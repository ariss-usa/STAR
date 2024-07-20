package com.example.hello;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

public class onUpdateV2{
    private String newUpdate = "";
    private String editedDirEntry = "";
    private String aprsUpdate = "";
    public void call(){
        try(ZContext ctx = new ZContext()){
            Socket client = ctx.createSocket(SocketType.REP);
            Socket editSocket = ctx.createSocket(SocketType.REP);
            Socket aprsSocket = ctx.createSocket(SocketType.REP);
            assert (client != null);
            client.connect("tcp://127.0.0.1:5556");
            editSocket.connect("tcp://127.0.0.1:5557");

            aprsSocket.connect("tcp://127.0.0.1:5559");

            org.zeromq.ZMQ.Poller poller = ctx.createPoller(3);
            poller.register(client, ZMQ.Poller.POLLIN);
            poller.register(editSocket, ZMQ.Poller.POLLIN);
            poller.register(aprsSocket, ZMQ.Poller.POLLIN);

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
}
