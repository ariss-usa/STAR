package com.example.hello;

import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.awt.*;

public class HelloController {
    @FXML
    private javafx.scene.layout.AnchorPane AnchorPane;
    @FXML
    private javafx.scene.control.MenuBar MenuBar;
    @FXML
    private TextField Power;
    @FXML
    private javafx.scene.layout.VBox VBox;
    @FXML
    private ComboBox<String> availableRobots;
    @FXML
    private TextField command;
    @FXML
    private MenuItem configItem;
    @FXML
    private CheckBox doNotDisturb;
    @FXML
    private CheckBox medium;
    @FXML
    private Button enter;
    @FXML
    private MenuItem helpItem;
    @FXML
    private Menu helpMenu;
    @FXML
    private ListView<String> recListView;
    @FXML
    private Label recText;
    @FXML
    private ListView<String> sentListView;
    @FXML
    private Label sentText;
    @FXML
    private Menu setupMenu;
    @FXML
    private ComboBox<String> type;
    @FXML
    private ComboBox<String> localRobotConnection;
    @FXML
    private Button pairButton;
    @FXML
    private MenuItem link;
    @FXML
    private Circle circle1;
    @FXML
    private Circle circle2;
    @FXML
    private Circle circle3;
    @FXML
    private MenuItem otherFeatures;
    @FXML
    private CheckBox visualizerCheck;
    @FXML
    private CheckBox recAPRSCheckBox;

    
    private Stage parent;
    private Parent root;
    private ArrayList<String> possibleConnections = new ArrayList<String>();
    private ArrayList<Object> fileValues = new ArrayList<Object>();
    public static ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
    private static String currRobot = "";
    private static boolean pairingStatus = false;

    @FXML
    protected void onLinkPressed(ActionEvent event) throws IOException, URISyntaxException{
        Desktop.getDesktop().browse(new URI("https://www.ariss.org/"));
    }
    
    @FXML
    protected void onhelpPressed(ActionEvent event) throws IOException, URISyntaxException{
        Desktop.getDesktop().browse(new URI("https://sites.google.com/view/ariss-starproject/home"));
    }

    @FXML
    protected void gpredictMenuItemPressed(ActionEvent event) throws IOException {
        Process process = new ProcessBuilder("..\\gpredict-win32-2.2.1\\gpredict-win32-2.2.1\\gpredict.exe").start();
    }

    @FXML
    protected void callsignEdit(ActionEvent event) throws IOException{
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("callsign.fxml"));
        root = loader.load();

        parent = (Stage) VBox.getScene().getWindow();
        Stage dialogStage = new Stage();
        dialogStage.setResizable(false);
        dialogStage.setTitle("Call sign");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(parent);
        Scene scene = new Scene(root, 313, 121);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        File callsignFile = new File("callsign.txt");
        if(!callsignFile.exists()) return;
        BufferedReader br = new BufferedReader(new FileReader(callsignFile));
        String format = "My Call: " + br.readLine() + ", Send to: " + br.readLine();
        br.close();
        if(pairingStatus){
            if(availableRobots.getItems().get(1).startsWith("My Call")){
                availableRobots.getItems().set(1, format);
            }
            else{
                availableRobots.getItems().add(1, format);
            }
        }
        else{
            if(availableRobots.getItems().get(0).startsWith("My Call")){
                availableRobots.getItems().set(0, format);
            }
            else{
                availableRobots.getItems().add(0, format);
            }
        }
    }
    
    @FXML
    protected void visualize(ActionEvent event) throws IOException{
        if(!visualizerCheck.isSelected()) return;
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("visualize.fxml"));
        
        root = loader.load();
        Stage dialogStage = new Stage();
        dialogStage.setResizable(false);
        dialogStage.setTitle("Visualizer");
        dialogStage.initOwner(parent);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogStage.setScene(scene);
        
        dialogStage.showAndWait();
        visualizerCheck.setSelected(false);
    }
    @FXML
    protected void onCBPressed(ActionEvent event) throws IOException{
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("commandBuilder.fxml"));
        root = loader.load();

        parent = (Stage) VBox.getScene().getWindow();
        Stage dialogStage = new Stage();
        dialogStage.setResizable(false);
        dialogStage.setTitle("Command Builder");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(parent);
        Scene scene = new Scene(root, 273, 207);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogStage.setScene(scene);

        dialogStage.showAndWait();
    }
    @FXML
    protected void configButtonPress(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("Dialogue.fxml"));
        root = loader.load();

        parent = (Stage) VBox.getScene().getWindow();
        Stage dialogStage = new Stage();
        dialogStage.setResizable(false);
        dialogStage.setTitle("Setup Dialog");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(parent);
        Scene scene = new Scene(root, 400, 210);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogStage.setScene(scene);

        dialogStage.showAndWait();

        Dialogue controller = loader.getController();
        fileValues = controller.getFileList();
        if(controller.submitPressed()){
            if(controller.isNewUser()){
                returnEntries.checkfile(false);
            }
            else{
                returnEntries.checkfile(true);
            }
            
            availableRobots.getItems().removeAll(availableRobots.getItems());

            if(pairButton.getText().equals("Disconnect")){
                String s = localRobotConnection.getValue();
                availableRobots.getItems().add(s);
            }
            
            possibleConnections = returnEntries.getDirList();
            availableRobots.getItems().addAll(filter.filterInternetInput(possibleConnections, fileValues.get(0).toString()));        
            
            availableRobots.setVisibleRowCount(3);

            if(!fileValues.get(4).toString().equals("true")){
                recAPRSCheckBox.setDisable(true);
                medium.setDisable(true);
                File callsignFile = new File("callsign.txt");
                callsignFile.delete();
            }
            else{
                recAPRSCheckBox.setDisable(false);
            }
        }
    }
    @FXML
    protected void receive(ActionEvent event) throws IOException{
        if(recAPRSCheckBox.isSelected()){
            transfer t = new transfer("recAPRS");
            t.setOnSucceeded(e -> {
                String recv = t.getValue();
                if(recv.equals("ACK")) return;
                if(recv.equals("callsign file missing")){
                    AlertBox.display("Call sign file missing");
                    recAPRSCheckBox.setSelected(false);
                }
                else if(recv.equals("rtl_fm stopped")){
                    AlertBox.display("RTL-SDR not running, check if the dongle is plugged in");
                    recAPRSCheckBox.setSelected(false);
                }
            });
            threadExecutor.submit(t);
            
        }
        else{
            transfer t = new transfer("stopReceivingAPRS");
            threadExecutor.submit(t);
        }
    }
    
    @FXML
    void sendPressed(MouseEvent event) throws IOException {
        if (availableRobots.getSelectionModel().isEmpty()) {
            AlertBox.display("Select a robot from the dropdown");
        }
        else if (Power.getText().isEmpty() || Double.parseDouble(Power.getText()) < 1 || 
                Double.parseDouble(Power.getText()) > 255 )  {
            AlertBox.display("Enter the power (from 1 to 255)");
        }
        else if (type.getSelectionModel().isEmpty() || type.getSelectionModel().getSelectedItem().equals("N/A"))  {
            AlertBox.display("Select a direction");
        }
        else if (command.getText().isEmpty()){
            AlertBox.display("Enter the amount of seconds you would like to run the robot");
        }
        else if (Double.parseDouble(command.getText()) < 1 || Double.parseDouble(command.getText()) > 120){
            AlertBox.display("Enter values between 1 and 120");
        }
        else{
            //Determine whether internet/APRS
            String str = availableRobots.getSelectionModel().getSelectedItem();
            String [] spl = str.split("\n");

            String s = command.getText();
            String selectedItem = availableRobots.getSelectionModel().getSelectedItem();
            String selectedDirection = type.getSelectionModel().getSelectedItem();
            if(!medium.isSelected()){
                String command = "";
                if(spl.length > 1){
                    String selectedMCID = selectedItem.substring(0, selectedItem.indexOf("\n"));
                    command = "SEND " + Power.getText() + " " + selectedDirection + " "
                    + s + " Selected MCid: " + selectedMCID;
                }
                else{
                    command = "Local " + Power.getText() + " " + selectedDirection + " "+ s;
                }
                transfer t = new transfer(command);
                threadExecutor.submit(t);
                sentListView.getItems().add(command);
            }
            else{
                File file = new File("callsign.txt");
                if(!file.exists()){
                    AlertBox.display("Add a call sign before sending via APRS");
                    return;
                }
                BufferedReader br = new BufferedReader(new FileReader(file));
                String mycallsign = br.readLine();
                String sendcall = br.readLine();
                br.close();
                String command = Power.getText() + " " + selectedDirection + " " + s;
                transfer t = new transfer("Transmit APRS " + mycallsign + " " + command + " " + sendcall);
                t.setOnSucceeded(e -> {
                    String recv = t.getValue();
                    if(recv.equals("callsign file missing")){
                        AlertBox.display("Call sign file missing");
                    }
                });
                threadExecutor.submit(t);
                
            }
            Power.clear();
            type.setValue("N/A");
            command.clear();
        }
    }
    @FXML
    void checkBoxClicked(MouseEvent event) {
        if(fileValues.size() != 0){
            if(doNotDisturb.isSelected()){
                doNotDisturb.setDisable(true);
                showLoadingAnimation();
                transfer tr = new transfer("editOnline, ChangeTo: No");
                tr.setOnSucceeded(e -> {
                    hideLoadingAnimation();
                    doNotDisturb.setDisable(false);
                });
                threadExecutor.submit(tr);
            }
            else{
                doNotDisturb.setDisable(true);
                showLoadingAnimation();
                transfer tr = new transfer("editOnline, ChangeTo: Yes");
                tr.setOnSucceeded(e -> {
                    doNotDisturb.setDisable(false);
                    hideLoadingAnimation();
                });
                threadExecutor.submit(tr);
            }
        }
    }
    @FXML
    void pairPressed(MouseEvent event) {
        if (localRobotConnection.getSelectionModel().isEmpty())  {
            AlertBox.display("Choose a robot to pair with");
        }
        else{
            String pairText = pairButton.getText();
            if(pairText.equals("Pair")){
                String send = "Pair connect " + localRobotConnection.getSelectionModel().getSelectedItem();
                transfer tr = new transfer(send);
                pairButton.setDisable(true);
                tr.setOnSucceeded(e -> {
                    String passfail = tr.getValue();
                    if (passfail.equals("pass")){
                        availableRobots.getItems().add(0, localRobotConnection.getValue());
                        localRobotConnection.setDisable(true);
                        doNotDisturb.setDisable(false);
                        recAPRSCheckBox.setDisable(false);
                        pairButton.setText("Disconnect");
                    }
                    else if(passfail.equals("fail")){
                        //Show user pairing failed
                        AlertBox.display("Pairing failed, try again");
                    }
                    pairButton.setDisable(false);
                });
                threadExecutor.submit(tr);
                pairingStatus = true;
                
            }
            else{
                localRobotConnection.setValue(null);
                localRobotConnection.setDisable(false);
                availableRobots.getItems().remove(0);
                recAPRSCheckBox.setDisable(true);

                //Unpair and turn status offline
                pairButton.setText("Pair");
                transfer tr = new transfer("Pair disconnect");
                threadExecutor.submit(tr);
                transfer tr1 = new transfer("changeTo: No");
                threadExecutor.submit(tr1);

                pairingStatus = false;
                otherFeatures.setDisable(true);
                doNotDisturb.setSelected(true);
                doNotDisturb.setDisable(true);
                Power.clear();
                command.clear();
                type.setValue(null);
            }
        }
    }
    @FXML
    public void initialize() throws IOException{
        type.getItems().addAll("forward", "backward", "right", "left");
        sentListView.getItems().add("~");
        recListView.getItems().add("~");
        doNotDisturb.setSelected(true);
        doNotDisturb.setDisable(true);
        otherFeatures.setDisable(true);
        
        hideLoadingAnimation();
        loadingAnimation();

        localRobotConnection.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(localRobotConnection.getPromptText());
                } else {
                    setText(item);
                }
            }
        });
        availableRobots.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(availableRobots.getPromptText());
                } else {
                    setText(item);
                }
            }
        });
        type.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(type.getPromptText());
                } else {
                    setText(item);
                }
            }
        });

        availableRobots.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            currRobot = newValue;
            if(newValue == null || newValue.isEmpty()){
                return;
            }
            if(currRobot.startsWith("My Call")){
                medium.setDisable(true);
                medium.setSelected(true);
            }
            else{
                medium.setDisable(true);
                medium.setSelected(false);
                threadExecutor.submit(new transfer("stopReceivingAPRS"));
            }
            otherFeatures.setDisable(false);
         });
        localRobotConnection.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if(isNowShowing){
                comList.restart();
            }
        });
        medium.setDisable(true);
        recAPRSCheckBox.setDisable(true);
        File tempFile = new File("important.txt");
        if (tempFile.exists()){
            Reader.read(fileValues);
            if (fileValues.size() != 0){
                returnEntries.checkfile(true);
                possibleConnections = returnEntries.getDirList();
                availableRobots.setVisibleRowCount(3);
                if(!fileValues.get(4).toString().equals("true")){
                    recAPRSCheckBox.setDisable(true);
                }
                availableRobots.getItems().addAll(filter.filterInternetInput(possibleConnections, fileValues.get(0).toString()));
            }
        }
        File callsignFile = new File("callsign.txt");
        if(callsignFile.exists()){
            BufferedReader br = new BufferedReader(new FileReader(callsignFile));
            availableRobots.getItems().add("My Call: " + br.readLine() + ", Send to: " + br.readLine());
            br.close();
        }
        onUpdateV2 ouv = new onUpdateV2();
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                ouv.call();
                String addedEntry = ouv.getNewUpdate();
                String editedEntry = ouv.getEditedEntry();
                ouv.reset();
                ouv.resetEditedEntry();
                Platform.runLater(
                () -> {
                    if(addedEntry.contains("New Command:")){
                        if(!doNotDisturb.isSelected()){
                            recListView.getItems().add(addedEntry.substring(13, addedEntry.length()));
                        }
                    }
                    else if(addedEntry.contains("New Robot:")){
                        if(!addedEntry.substring(11, 16).equals(fileValues.get(0).toString())){
                            possibleConnections.add(addedEntry);
                            availableRobots.getItems().add(filter.filterEntry(addedEntry, fileValues.get(0).toString()));
                        }
                    }
                    else if(!editedEntry.equals("")){
                        String [] arr = editedEntry.split("\nIndex: ");
                        int index = Integer.parseInt(arr[1]);
                        possibleConnections.set(index, arr[0]);
                        availableRobots.getItems().setAll(filter.filterInternetInput(possibleConnections, fileValues.get(0).toString()));
                    }
                });
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    public static String getSelectedRobot(){
        return currRobot;
    }
    public static boolean getPairingStatus(){
        return pairingStatus;
    }
    private void loadingAnimation(){
        TranslateTransition [] tta = {new TranslateTransition(), new TranslateTransition(), new TranslateTransition()};
        Node [] circles = {circle1, circle2, circle3};
        Duration [] delays = {Duration.millis(0), Duration.millis(100), Duration.millis(200)};
        for(int i = 0; i < tta.length; i++){
            tta[i].setDelay(delays[i]);
            tta[i].setDuration(Duration.millis(300));
            tta[i].setNode(circles[i]);
            tta[i].setByY(-5);
            tta[i].setCycleCount(Timeline.INDEFINITE);
            tta[i].setAutoReverse(true);
            tta[i].play();
        }
    }
    private void hideLoadingAnimation(){
        circle1.setVisible(false);
        circle2.setVisible(false);
        circle3.setVisible(false);
    }
    private void showLoadingAnimation(){
        circle1.setVisible(true);
        circle2.setVisible(true);
        circle3.setVisible(true);
    }
    Service<Void> comList = new Service<Void>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                public Void call() {
                    try(ZContext ctx = new ZContext()){
                        ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
                        socket.connect("tcp://127.0.0.1:5555");
                        socket.send("getCOMList");
                        String comports = socket.recvStr();
                        String [] split = comports.split(";");
                        ctx.destroy();
                        final CountDownLatch latch = new CountDownLatch(1);
                        Platform.runLater(new Runnable() {                          
                            @Override
                            public void run() {
                                try{
                                    ArrayList<String> output = new ArrayList<String>();
                                    for(int i = 0; i < split.length; i++){
                                        if(split[i].equals("")){
                                            continue;
                                        }
                                        output.add(split[i]);
                                    }
                                    localRobotConnection.getItems().removeAll(localRobotConnection.getItems());
                                    localRobotConnection.getItems().addAll(output);
                                }finally{
                                    latch.countDown();
                                }
                            }
                        });
                        latch.await();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        }
    };
}

