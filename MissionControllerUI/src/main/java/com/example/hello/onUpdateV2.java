package com.example.hello;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import javafx.scene.paint.Color;

public class onUpdateV2{
    private String newUpdate = "";
    private String editedDirEntry = "";
    private String aprsUpdate = "";

    private ArrayList<Color> lineColors = null;
    private int width = -1;
    private int lineNum = -1;

    public void call(){
        try(ZContext ctx = new ZContext()){
            Socket client = ctx.createSocket(SocketType.REP);
            Socket editSocket = ctx.createSocket(SocketType.REP);
            Socket aprsSocket = ctx.createSocket(SocketType.REP);
            Socket sstvSocket = ctx.createSocket(SocketType.PULL);
            assert (client != null);
            client.connect("tcp://127.0.0.1:5556");
            editSocket.connect("tcp://127.0.0.1:5557");

            aprsSocket.connect("tcp://127.0.0.1:5559");
            sstvSocket.connect("tcp://127.0.0.1:5560");


            org.zeromq.ZMQ.Poller poller = ctx.createPoller(4);
            poller.register(client, ZMQ.Poller.POLLIN);
            poller.register(editSocket, ZMQ.Poller.POLLIN);
            poller.register(aprsSocket, ZMQ.Poller.POLLIN);
            poller.register(sstvSocket, ZMQ.Poller.POLLIN);

            Object var = poller.poll(100);

            if(poller.pollin(0)){
                String reply = client.recvStr(ZMQ.DONTWAIT);
                client.send("Recieved");
                newUpdate = reply;
            }
            if (poller.pollin(1)) {
                String reply = editSocket.recvStr(ZMQ.DONTWAIT);
                editSocket.send("Recieved");
                editedDirEntry = reply;
            }
            if(poller.pollin(2)){
                String reply = aprsSocket.recvStr(ZMQ.DONTWAIT);
                aprsSocket.send("Recieved");
                aprsUpdate = reply;
            }
            if(poller.pollin(3)){
                byte[] message = sstvSocket.recv(ZMQ.DONTWAIT);
                ByteBuffer buffer = ByteBuffer.wrap(message);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                lineNum = buffer.getInt();
                width = buffer.getInt();

                byte[] red = new byte[width];
                byte[] green = new byte[width];
                byte[] blue = new byte[width];

                buffer.get(red);
                buffer.get(green);
                buffer.get(blue);

                lineColors = new ArrayList<>();
                for(int i = 0; i < width; i++){
                    int r = Byte.toUnsignedInt(red[i]);
                    int g = Byte.toUnsignedInt(green[i]);
                    int b = Byte.toUnsignedInt(blue[i]);

                    Color color = Color.rgb(r, g, b);
                    lineColors.add(color);
                }
            }
        }
    }
    public String getNewUpdate(){
        return newUpdate;
    }
    public String getEditedEntry(){
        return editedDirEntry;
    }
    public String getAPRSEntry(){
        return aprsUpdate;
    }
    public void resetEditedEntry(){
        editedDirEntry = "";
    }
    public void reset(){
        newUpdate = "";
    }
    public void resetAPRSEntry(){
        aprsUpdate = "";
    }

    public ArrayList<Color> getLine(){
        return lineColors;
    }
    public int getWidth(){
        return width;
    }
    public int getLineNumber(){
        return lineNum;
    }
    public void resetSSTV(){
        lineColors = null;
        width = -1;
        lineNum = -1;
    }
}
