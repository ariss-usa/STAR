package org.ariss.star;

import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.zeromq.ZContext;
import javafx.application.Platform;

import java.util.function.Consumer;

import org.zeromq.SocketType;

public class onUpdateV3 {
    private Thread listenerThread;
    private ZContext context;
    private ZMQ.Socket pullSocket;

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

    public void stopListening() {
        if (listenerThread != null) listenerThread.interrupt();
        if (context != null) context.close();
    }
}
