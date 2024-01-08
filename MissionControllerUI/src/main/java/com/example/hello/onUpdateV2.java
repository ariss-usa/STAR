package com.example.hello;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class onUpdateV2{
    private String newUpdate = "";
    private String editedDirEntry = "";
    public void call(){
        try(ZContext ctx = new ZContext()){
            Socket client = ctx.createSocket(SocketType.REP);
            Socket editSocket = ctx.createSocket(SocketType.REP);
            assert (client != null);
            client.connect("tcp://127.0.0.1:5556");
            editSocket.connect("tcp://127.0.0.1:5557");

            org.zeromq.ZMQ.Poller poller = ctx.createPoller(2);
            poller.register(client, ZMQ.Poller.POLLIN);
            poller.register(editSocket, ZMQ.Poller.POLLIN);

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
        }
    }
    public String getNewUpdate(){
        return newUpdate;
    }
    public String getEditedEntry(){
        return editedDirEntry;
    }
    public void resetEditedEntry(){
        editedDirEntry = "";
    }
    public void reset(){
        newUpdate = "";
    }
}
