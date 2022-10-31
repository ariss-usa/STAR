package com.example.hello;

import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        }
    }
    @FXML
    void sendPressed(MouseEvent event) {
        if (availableRobots.getSelectionModel().isEmpty()) {
            AlertBox.display("Select a robot from the dropdown");
        }
        else if (Power.getText().isEmpty() || Integer.parseInt(Power.getText()) < 1 || 
                Integer.parseInt(Power.getText()) > 255 )  {
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

             
            if(!medium.isSelected()){
                String s = command.getText();
                
                String selectedItem = availableRobots.getSelectionModel().getSelectedItem();
                String selectedDirection = type.getSelectionModel().getSelectedItem();
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
                try(ZContext ctx = new ZContext()){
                    ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
                    socket.connect("tcp://127.0.0.1:5554");
                    String s = command.getText();
                    
                    String selectedItem = availableRobots.getSelectionModel().getSelectedItem();
                    String selectedDirection = type.getSelectionModel().getSelectedItem();
                    String command = "";
                    String selectedMCID = selectedItem.substring(0, selectedItem.indexOf("\n"));
                    
                    command = "APRS " + Power.getText() + " " + selectedDirection + " "
                    + s + " Selected MCid: " + selectedMCID;

                    socket.send(command);
                    String str1 = socket.recvStr();
                    sentListView.getItems().add(command);
                    ctx.destroy();
                }
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
                doNotDisturb.setDisable(true);
                availableRobots.getItems().remove(0);
                pairButton.setText("Pair");
                transfer tr = new transfer("Pair disconnect");
                threadExecutor.submit(tr);
                pairingStatus = false;
                otherFeatures.setDisable(true);
            }
        }
    }
    @FXML
    public void initialize() throws IOException{
        type.getItems().addAll("N/A", "forward", "backward", "right", "left", "pause");
        sentListView.getItems().add("~");
        recListView.getItems().add("~");
        doNotDisturb.setSelected(true);
        doNotDisturb.setDisable(true);
        otherFeatures.setDisable(true);
        
        hideLoadingAnimation();
        loadingAnimation();

        availableRobots.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            currRobot = newValue;
            if(newValue == null || newValue.isEmpty()){
                return;
            }

            String [] spl = newValue.split("\n");
            if(spl.length > 1){
                if(newValue.split("\n")[2].equals("SDR Dongle: false")){
                    medium.setSelected(false);
                    medium.setDisable(true);
                }
                else{
                    medium.setDisable(false);
                }
            }
            else{
                medium.setDisable(true);
            }
            otherFeatures.setDisable(false);
         });
        localRobotConnection.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if(isNowShowing){
                comList.restart();
            }
        });
        medium.setDisable(true);

        File tempFile = new File("important.txt");
        if (tempFile.exists()){
            Reader.read(fileValues);
            if (fileValues.size() != 0){
                returnEntries.checkfile(true);
                possibleConnections = returnEntries.getDirList();
                availableRobots.setVisibleRowCount(3);
                availableRobots.getItems().addAll(filter.filterInternetInput(possibleConnections, fileValues.get(0).toString()));
            }
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
        TranslateTransition dot1 = new TranslateTransition();
        dot1.setDuration(Duration.millis(300));
        dot1.setNode(circle1);
        dot1.setByY(-5);
        dot1.setCycleCount(Timeline.INDEFINITE);
        dot1.setAutoReverse(true);
        dot1.play();

        TranslateTransition dot2 = new TranslateTransition();
        dot2.setDelay(Duration.millis(100));
        dot2.setDuration(Duration.millis(300));
        dot2.setNode(circle2);
        dot2.setByY(-5);
        dot2.setCycleCount(Timeline.INDEFINITE);
        dot2.setAutoReverse(true);
        dot2.play();

        TranslateTransition dot3 = new TranslateTransition();
        dot3.setDelay(Duration.millis(200));
        dot3.setDuration(Duration.millis(300));
        dot3.setNode(circle3);
        dot3.setByY(-5);
        dot3.setCycleCount(Timeline.INDEFINITE);
        dot3.setAutoReverse(true);
        dot3.play();
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
                                    localRobotConnection.getItems().removeAll(localRobotConnection.getItems());
                                    if(split.length == 0){
                                        return;
                                    }
                                    localRobotConnection.getItems().addAll(split);
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

