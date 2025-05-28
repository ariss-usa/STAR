package com.example.hello;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class returnEntries {
    private static ArrayList<String> connections = new ArrayList<String>();

    public static ArrayList<String> getDirList(){
        final ExecutorService service;
        final Future<String> dirTask;

        service = Executors.newFixedThreadPool(1);        
        dirTask = service.submit(new dirV1());
        try{
            JsonObject obj = JsonParser.parseString(dirTask.get()).getAsJsonObject();
            JsonArray robots = obj.getAsJsonArray("active_robots");
            connections.clear();
            for(int i = 0; i < robots.size(); i++){
                JsonObject robot = robots.get(i).getAsJsonObject();
                String summary = robot.get("id").getAsString() + " - " +
                                robot.get("city").getAsString() + ", " +
                                robot.get("state").getAsString();
                connections.add(summary);
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
                Gson gson = new Gson();
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "user_data_update");
                boolean b1 = socket.send(gson.toJson(msg));
                String str = socket.recvStr();
            }
            else{
                // Send nothing since websocket establishes connection when file is present

                //boolean b1 = socket.send("Check file false");
                //String str = socket.recvStr();
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
            Gson gson = new Gson();

            JsonObject msg = new JsonObject();
            msg.addProperty("type", "get_directory");

            socket.send(gson.toJson(msg));
            String str = socket.recvStr();
            ctx.destroy();
            return str;
        }
    }
}
