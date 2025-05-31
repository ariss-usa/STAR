package com.example.hello;

import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.zeromq.ZContext;
import javafx.application.Platform;
import org.zeromq.SocketType;

public class onUpdateV3 {
    private Thread listenerThread;
    private ZContext context;
    private ZMQ.Socket pullSocket;

    public void startListening(Runnable onDisconnect) {
        context = new ZContext();
        pullSocket = context.createSocket(SocketType.PULL);
        pullSocket.connect("tcp://127.0.0.1:5556");
        Gson gson = new Gson();
        listenerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String message = pullSocket.recvStr();
                    JsonObject obj = gson.fromJson(message, JsonObject.class);
                    if (obj != null && obj.get("type").getAsString().equals("usb_disconnect")) {
                        Platform.runLater(onDisconnect);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
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
