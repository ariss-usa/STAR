package com.example.hello;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import javafx.concurrent.Task;

public class transfer extends Task<String>{
    private String str;
    public transfer(String str){
        this.str = str;
    }
    @Override
    protected String call() throws Exception {
        try(ZContext ctx = new ZContext()){
            ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
            socket.connect("tcp://127.0.0.1:5555");
            socket.send(str);
            socket.recvStr();
            ctx.destroy();
        }
        return null;
    }
}
