package com.example.hello;

import java.util.ArrayList;
import java.util.Arrays;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;


public class commandBuilderController {
    @FXML
    private TextArea CBTextBox;

    @FXML
    void multiCommand(ActionEvent event) {
        String txt = CBTextBox.getText();
        boolean formatCheck = checkFormat(txt);
        String currRobot = HelloController.getSelectedRobot();
        if(currRobot == null){
            AlertBox.display("pair to a robot");
        }
        else{
            String [] spl = currRobot.split("\n");
            if(currRobot.equals("")){
                AlertBox.display("Select a robot");
            }
            else if(!HelloController.getPairingStatus()){
                AlertBox.display("Pair to a robot");
            }
            else if(formatCheck){
                String command = "";
                if(spl.length > 1){
                    //Multi-commands through discord
                }
                else{
                    //Multi-commands through BT
                    command = "Local " + txt;
                }
                transfer t = new transfer(command);
                HelloController.threadExecutor.submit(t);
            }
        }
    }

    @FXML
    void onCancelPressed(ActionEvent event) {
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }
    private boolean checkFormat(String str){
        String [] arr = str.split("\n");
        for(int i = 0; i < arr.length; i++){
            String [] split = arr[i].split(" ");
            if(split.length != 3){
                AlertBox.display("Wrong format");
                return false;
            }
            try{
                int power = Integer.parseInt(split[0]);
                if (power < 1 || power > 255 )  {
                    AlertBox.display("Enter the power (from 1 to 255)");
                    return false;
                }
                String dir = split[1];
                dir = dir.toLowerCase();
                ArrayList<String> dirCheck = new ArrayList<String>(Arrays.asList("forward", "backward", "right", "left", "pause"));
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
