package org.ariss.star;

import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.zeromq.ZContext;
import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

import org.zeromq.SocketType;

public class onUpdateV3 {
    private Thread listenerThread;
    private ZContext context;
    private ZMQ.Socket pullSocket;

    private Thread qsstvThread;
    private ZMQ.Socket pullSocket1;
    private int lastLineNum;

    public void startListening(Consumer<JsonObject> onUpdate) {
        context = new ZContext();
        pullSocket = context.createSocket(SocketType.PULL);
        pullSocket.connect("tcp://127.0.0.1:5556");
        Gson gson = new Gson();
        listenerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String message = pullSocket.recvStr();
                    JsonObject obj = gson.fromJson(message, JsonObject.class);
                    if (obj != null && obj.has("type")) {
                        Platform.runLater(() -> onUpdate.accept(obj));
                    }
                } catch (Exception e) {
                    System.out.println("[DEBUG]: there was an error handling pushed updates from client backend " + e.toString());
                }
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void startListeningQSSTV(WritableImage pix) {
        pullSocket1 = context.createSocket(SocketType.PULL);
        pullSocket1.bind("tcp://127.0.0.1:5557");
        qsstvThread = new Thread(() -> {
            while (true) {
                byte[] message = pullSocket1.recv(0);
                ByteBuffer buffer = ByteBuffer.wrap(message);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                int lineNum = buffer.getInt();
                int width = buffer.getInt();

                byte[] red = new byte[width];
                byte[] green = new byte[width];
                byte[] blue = new byte[width];

                buffer.get(red);
                buffer.get(green);
                buffer.get(blue);

                if (lineNum < lastLineNum) {
                    Platform.runLater(() -> {
                        for (int i = 0; i < 280; i++) {
                            for (int j = 0; j < 270; j++) {
                                pix.getPixelWriter().setColor(i, j, Color.TRANSPARENT);
                            }
                        }
                    });
                }
                lastLineNum = lineNum;

                Platform.runLater(() -> {
                    if (lineNum >= 0 && lineNum < 270) {
                        for(int x = 0; x < Math.min(280, width); x++) {
                            int r = red[x] & 0xFF;
                            int g = green[x] & 0xFF;
                            int b = blue[x] & 0xFF;
                            pix.getPixelWriter().setColor(x, lineNum, Color.rgb(r, g, b));
                        }
                    }
                });
            }
        });

        qsstvThread.setDaemon(true);
        qsstvThread.start();
    }

    public void stopListening() {
        if (listenerThread != null) listenerThread.interrupt();
        if (context != null) context.close();
    }
}
