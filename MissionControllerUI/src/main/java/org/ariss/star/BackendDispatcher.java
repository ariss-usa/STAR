package org.ariss.star;

import java.util.HashMap;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.concurrent.Task;

public class BackendDispatcher extends Task<JsonObject>{
    final String ZMQ_ENDPOINT = "tcp://127.0.0.1:5555";
    Gson gson = new Gson();
    JsonObject msg = new JsonObject();

    public BackendDispatcher(MessageStructure msg_type, HashMap<String, Object> params) {
        if (params != null) {
            JsonElement body = gson.toJsonTree(params);
            if (body.isJsonObject()) {
                msg = body.getAsJsonObject();
            }
            else{
                msg = new JsonObject();
            }
        }
        msg.addProperty("type", msg_type.name().toLowerCase());
    }

    @Override
    protected JsonObject call() throws Exception {
        JsonObject obj;
        try(ZContext ctx = new ZContext()){
            ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
            socket.connect(ZMQ_ENDPOINT);
            socket.setReceiveTimeOut(2000);
            socket.send(gson.toJson(msg));
            String returnStr = socket.recvStr();

            if (returnStr == null) {
                throw new RuntimeException("Backend did not respond in time");
            }
            obj = gson.fromJson(returnStr, JsonObject.class);
        }
        return obj;
    }

    public void attachDefaultErrorHandler(){
        this.setOnSucceeded(e -> {
            JsonObject recv = this.getValue();
            if(recv.get("status").getAsString().equals("error")){
                AlertBox.display("Error: " + recv.get("err_msg"));
            }
        });
    }
}

enum MessageStructure {
    PAIR_CONNECT,
    REMOTE_CONTROL,
    LOCAL_CONTROL,
    PAIR_DISCONNECT,
    USER_DATA_UPDATE,
    GET_DIRECTORY,
    RECEIVE_APRS,
    STOP_APRS_RECEIVE,
    SEND_APRS,
    GET_PORTS,
    END_PROGRAM
}