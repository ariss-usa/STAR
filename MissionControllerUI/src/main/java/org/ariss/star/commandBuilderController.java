package org.ariss.star;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.ariss.star.MissionController.RobotType;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;


public class commandBuilderController {
    @FXML
    private TextArea CBTextBox;
    private MissionController baseController;
    @FXML
    public void initialize() throws IOException {
        if (MissionController.getRobotType() == RobotType.MBOT) {
            CBTextBox.appendText("100 forward 3\r\n");
            CBTextBox.appendText("50 backward 2\r\n");
            CBTextBox.appendText("0 delay 5\r\n");
            CBTextBox.appendText("150 left 4\r\n");
            CBTextBox.appendText("255 right 1\r\n");
        } else {
            CBTextBox.appendText("forward 30\r\n");
            CBTextBox.appendText("backward 10\r\n");
            CBTextBox.appendText("delay 5\r\n");
            CBTextBox.appendText("left 90\r\n");
            CBTextBox.appendText("right 180\r\n");
        }
    }
    @FXML
    void multiCommand(ActionEvent event) {
        String txt = CBTextBox.getText();
        boolean formatCheck = checkFormat(txt);
        RobotEntry currRobot = MissionController.getSelectedRobot();
        RobotType robotType = MissionController.getRobotType();
        if (currRobot == null) {
            AlertBox.display("Select a robot");
        } else {
            if (robotType == RobotType.MBOT) {
                if (formatCheck) {
                    String [] split = txt.split("\n| ");
                    HashMap<String, Object> params = new HashMap<>();
                    ArrayList<HashMap<String, Object>> cmds = new ArrayList<>();
                    String readableCommand = "";
                    for (int i = 0; i < split.length; i += 3) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("power", split[i]);
                        map.put("direction", split[i + 1]);
                        map.put("time", split[i + 2]);
                        cmds.add(map);

                        if (split.length == 1 || i == split.length - 1){
                            readableCommand += split[i] + " " + split[i+1] + " " + split[i+2];
                        }
                        else{
                            readableCommand += split[i] + " " + split[i+1] + " " + split[i+2] + ", ";
                        }
                    }
                    params.put("commands", cmds);
                    BackendDispatcher dispatcher;
                    if (currRobot.getType() == EntryType.REMOTE) {
                        //Multi-commands through discord
                        params.put("receiver_id", currRobot.getId());
                        dispatcher = new BackendDispatcher(MessageStructure.REMOTE_CONTROL, params);
                    } else if (currRobot.getType() == EntryType.LOCAL) {
                        //Multi-commands through BT
                        dispatcher = new BackendDispatcher(MessageStructure.LOCAL_CONTROL, params);
                    } else {
                        params.put("callsign", currRobot.myCallsign);
                        params.put("destination", currRobot.destinationCallsign);
                        dispatcher = new BackendDispatcher(MessageStructure.SEND_APRS, params);
                    }
                    dispatcher.attachDefaultErrorHandler();
                    baseController.addToSendList(readableCommand);
                    MissionController.threadExecutor.submit(dispatcher);
                }
            } else {
                if (formatCheck) {
                    String [] split = txt.split("\n| ");
                    HashMap<String, Object> params = new HashMap<>();
                    ArrayList<HashMap<String, Object>> cmds = new ArrayList<>();
                    String readableCommand = "";
                    for (int i = 0; i < split.length; i += 2) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("direction", split[i]);
                        map.put("amount", split[i + 1]);
                        cmds.add(map);

                        if (split.length == 1 || i == split.length - 1) {
                            readableCommand += split[i] + " " + split[i+1];
                        } else {
                            readableCommand += split[i] + " " + split[i+1] + ", ";
                        }
                    }
                    params.put("commands", cmds);
                    BackendDispatcher dispatcher = new BackendDispatcher(MessageStructure.LOCAL_CONTROL, params);
                    dispatcher.attachDefaultErrorHandler();
                    baseController.addToSendList(readableCommand);
                    MissionController.threadExecutor.submit(dispatcher);
                }
            }
        }
    }

    @FXML
    void onCancelPressed(ActionEvent event) {
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }
    public boolean checkFormat(String str){
        String [] arr = str.split("\n");
        RobotType currRobot = MissionController.getRobotType();
        if (currRobot == RobotType.MBOT) {
            for (int i = 0; i < arr.length; i++) {
                String [] split = arr[i].split(" ");
                if(split.length != 3){
                    AlertBox.display("Wrong format");
                    return false;
                }
                try {
                    int power = Integer.parseInt(split[0]);
                    if (power < 0 || power > 255)  {
                        AlertBox.display("Enter the power (from 0 to 255)");
                        return false;
                    }
                    String dir = split[1];
                    dir = dir.toLowerCase();
                    ArrayList<String> dirCheck = new ArrayList<String>(Arrays.asList("forward", "backward", "right", "left", "delay"));
                    if (!dirCheck.contains(dir)) {
                        AlertBox.display("Enter a valid direction");
                        return false;
                    }
                    try {
                        int time = Integer.parseInt(split[2]);
                        if (time < 0 || time > 100) {
                            AlertBox.display("Enter the time (from 0 to 100)");
                            return false;
                        }
                    } catch (Exception e) {
                        AlertBox.display("Time is in an incorrect format");
                    }
                } catch (Exception e) {
                    AlertBox.display("Power is in an incorrect format");
                    return false;
                }
            }
        } else {
            for (int i = 0; i < arr.length; i++) {
                String [] split = arr[i].split(" ");
                if(split.length != 2){
                    AlertBox.display("Wrong format");
                    return false;
                }
                try {
                    
                    String dir = split[0];
                    dir = dir.toLowerCase();
                    ArrayList<String> dirCheck = new ArrayList<String>(Arrays.asList("forward", "backward", "right", "left", "delay", "arm"));
                    if (!dirCheck.contains(dir)) {
                        AlertBox.display("Enter a valid direction");
                        return false;
                    }

                    int amount = Integer.parseInt(split[1]);
                    if (dir.equals("arm") && (amount < 0 || amount > 135)) {
                        AlertBox.display("Enter the position f the arm (from 0 to 135 degrees)");
                        return false;
                    }
                } catch (Exception e) {
                    AlertBox.display("Amount is in an incorrect format");
                    return false;
                }
            }
        }
        return true;
    }
    public void setBaseController(MissionController controller){
        baseController = controller;
    }
}
