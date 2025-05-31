package com.example.hello;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;


public class commandBuilderController {
    @FXML
    private TextArea CBTextBox;
    @FXML
    public void initialize() throws IOException{
        CBTextBox.appendText("100 forward 3\r\n");
        CBTextBox.appendText("50 backward 2\r\n");
        CBTextBox.appendText("0 delay 5\r\n");
        CBTextBox.appendText("150 left 4\r\n");
        CBTextBox.appendText("255 right 1\r\n");
    }
    @FXML
    void multiCommand(ActionEvent event) {
        String txt = CBTextBox.getText();
        boolean formatCheck = checkFormat(txt);
        RobotEntry currRobot = HelloController.getSelectedRobot();
        if(currRobot == null){
            AlertBox.display("Select a robot");
        }
        else{
            if(!HelloController.getPairingStatus()){
                AlertBox.display("Pair to a robot");
            }
            else if(formatCheck){
                String [] split = txt.split("\n| ");
                HashMap<String, Object> params = new HashMap<>();
                ArrayList<HashMap<String, Object>> cmds = new ArrayList<>();
                for(int i = 0; i < split.length; i+=3){
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("power", split[i]);
                    map.put("direction", split[i+1]);
                    map.put("time", split[i+2]);
                    cmds.add(map);
                }
                params.put("commands", cmds);
                BackendDispatcher dispatcher;
                if(currRobot.getType() == EntryType.REMOTE){
                    //Multi-commands through discord
                    params.put("receiver_id", currRobot.getId());
                    dispatcher = new BackendDispatcher(MessageStructure.REMOTE_CONTROL, params);
                }
                else if(currRobot.getType() == EntryType.LOCAL){
                    //Multi-commands through BT
                    dispatcher = new BackendDispatcher(MessageStructure.LOCAL_CONTROL, params);
                }
                else{
                    params.put("callsign", currRobot.myCallsign);
                    params.put("destination", currRobot.callsignToAccept);
                    dispatcher = new BackendDispatcher(MessageStructure.SEND_APRS, params);
                }
                HelloController.threadExecutor.submit(dispatcher);
            }
        }
    }

    @FXML
    void onCancelPressed(ActionEvent event) {
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }
    public boolean checkFormat(String str){
        String [] arr = str.split("\n");
        for(int i = 0; i < arr.length; i++){
            String [] split = arr[i].split(" ");
            if(split.length != 3){
                AlertBox.display("Wrong format");
                return false;
            }
            try{
                int power = Integer.parseInt(split[0]);
                if (power < 0 || power > 255 )  {
                    AlertBox.display("Enter the power (from 0 to 255)");
                    return false;
                }
                String dir = split[1];
                dir = dir.toLowerCase();
                ArrayList<String> dirCheck = new ArrayList<String>(Arrays.asList("forward", "backward", "right", "left", "delay"));
                if (!dirCheck.contains(dir)){
                    AlertBox.display("Enter a valid direction");
                    return false;
                }
                try{
                    int time = Integer.parseInt(split[2]);
                    if(time < 0 || time > 100){
                        AlertBox.display("Enter the time (from 0 to 100)");
                        return false;
                    }
                }
                catch(Exception e){
                    AlertBox.display("Time is in an incorrect format");
                }
            }
            catch(Exception e){
                AlertBox.display("Power is in an incorrect format");
                return false;
            }
        }
        return true;
    }
}
