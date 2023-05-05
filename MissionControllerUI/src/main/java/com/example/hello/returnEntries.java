package com.example.hello;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class returnEntries {
    private static ArrayList<String> connections = new ArrayList<String>();

    public static ArrayList<String> getDirList(){
        final ExecutorService service;
        final Future<String>  dirTask;

        service = Executors.newFixedThreadPool(1);        
        dirTask = service.submit(new dirV1());
        try{
            connections.clear();
            String [] arr = dirTask.get().split(";");
            for(String traverse : arr){
                connections.add(traverse);
            }
        }catch(final InterruptedException ex) {
            ex.printStackTrace();
        } catch(final ExecutionException ex) {
            ex.printStackTrace();
        }
        service.shutdownNow();
        return connections;
    }
    public static void checkfile(boolean editMessage){
        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
            socket.connect("tcp://127.0.0.1:5555");
            if(editMessage){
                boolean b1 = socket.send("Check file true");
                String str = socket.recvStr();
            }
            else{
                boolean b1 = socket.send("Check file false");
                String str = socket.recvStr();
            }
            ctx.destroy();
        }
    }
}
class dirV1 implements Callable<String>{
    public String call(){
        try(ZContext ctx = new ZContext()){
            ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
            boolean bool = socket.connect("tcp://127.0.0.1:5555");
            socket.send("getDIRList");
            String str = socket.recvStr();
            ctx.destroy();
            return str;
        }
    }
}
