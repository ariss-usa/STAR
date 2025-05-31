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
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
    private ComboBox<RobotEntry> availableRobots;
    @FXML
    private TextField command;
    @FXML
    private MenuItem configItem;
    @FXML
    private MenuItem configItem1;
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
    private MenuItem otherFeatures;
    @FXML
    private MenuItem commandBuilder;
    @FXML
    private CheckBox visualizerCheck;
    @FXML
    private CheckBox recAPRSCheckBox;
    @FXML
    private Circle circle1;
    @FXML
    private Circle circle2;
    @FXML
    private Circle circle3;

    private Stage parent;
    private Parent root;
    private ArrayList<Object> fileValues = new ArrayList<Object>();
    public static ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
    private static RobotEntry currRobot;
    private static boolean pairingStatus = false;
    final private String ARISS_URL = "https://www.ariss.org/";
    final private String STAR_URL = "https://sites.google.com/view/ariss-starproject/home";
    final private String LOCALHOST_URL = "http://localhost:8080/index.html";
    BackendDispatcher dispatcher;

    @FXML
    protected void onLinkPressed(ActionEvent event) throws IOException, URISyntaxException{
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            Desktop.getDesktop().browse(new URI(ARISS_URL));
        }
        else{
            //Default browser for rpi
            new ProcessBuilder("sh", "-c", "sensible-browser " + ARISS_URL).start();
        }
    }
    
    @FXML
    protected void onhelpPressed(ActionEvent event) throws IOException, URISyntaxException{
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            Desktop.getDesktop().browse(new URI(STAR_URL));
        }
        else{
            //Default browser for rpi
            new ProcessBuilder("sh", "-c", "sensible-browser " + STAR_URL).start();
        }
    }

    @FXML
    protected void gpredictMenuItemPressed(ActionEvent event) throws IOException {
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            new ProcessBuilder("..\\gpredict-win32-2.2.1\\gpredict-win32-2.2.1\\gpredict.exe").start();
        }
        else{
            new ProcessBuilder("gpredict").start();
        }
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
        RobotEntry entry = new RobotEntry(br.readLine(), br.readLine());
        br.close();
        
        if(pairingStatus){
            if(availableRobots.getItems().size() > 1 && availableRobots.getItems().get(1).getType() == EntryType.APRS){
                availableRobots.getItems().set(1, entry);
            }
            else{
                availableRobots.getItems().add(1, entry);
            }
        }
        else{
            if(availableRobots.getItems().size() > 0 && availableRobots.getItems().get(0).getType() == EntryType.APRS){
                availableRobots.getItems().set(0, entry);
            }
            else{
                availableRobots.getItems().add(0, entry);
            }
        }
    }
    Process process = null;
    Thread processThread = null;
    Server server = null;
    @FXML
    protected void visualize(ActionEvent event) throws Exception{
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            if(visualizerCheck.isSelected()){
                process = new ProcessBuilder(".\\MARS-SIM\\MARS-SIM.exe").start();
                processThread = new Thread(() -> {
                    try {
                        process.waitFor(); // Wait for the process to complete
                        visualizerCheck.setSelected(false);
                        process = null;
                        processThread = null;
                    } catch (InterruptedException e) {
                        // Handle any exceptions that may occur
                        e.printStackTrace();
                    }
                });
                processThread.start();
            }
            else{
                process.destroy();
            }
        }
        else{
            if(visualizerCheck.isSelected()){
                server = new Server(8080);
                ResourceHandler resourceHandler = new ResourceHandler();
                resourceHandler.setWelcomeFiles(new String[]{"index.html"});
                resourceHandler.setResourceBase("./webGL_Mars_Sim");
                server.setHandler(resourceHandler);
                try{
                    server.start();
                    new ProcessBuilder("sh", "-c", "sensible-browser " + LOCALHOST_URL).start();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            else if(server != null){
                server.stop();
            }
        }
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

            if(pairingStatus){
                doNotDisturb.setDisable(false);
            }
            
            availableRobots.getItems().removeAll(availableRobots.getItems());

            if(pairButton.getText().equals("Disconnect")){
                String s = localRobotConnection.getValue();
                availableRobots.getItems().add(new RobotEntry(s));
            }
        }
    }
    @FXML
    protected void receive(ActionEvent event) throws IOException{
        if(recAPRSCheckBox.isSelected()){
            dispatcher = new BackendDispatcher(MessageStructure.RECEIVE_APRS, null);
            dispatcher.setOnSucceeded(e -> {
                JsonObject recv = dispatcher.getValue();
                if(recv.get("status").getAsString().equals("error")){
                    AlertBox.display("Error: " + recv.get("err_msg"));
                    recAPRSCheckBox.setSelected(false);
                }
            });
            threadExecutor.submit(dispatcher);
        }
        else{
            recAPRSCheckBox.setDisable(true);
            dispatcher = new BackendDispatcher(MessageStructure.STOP_APRS_RECEIVE, null);
            dispatcher.setOnSucceeded(e -> {
                JsonObject recv = dispatcher.getValue();
                if(recv.get("status").getAsString().equals("error")){
                    AlertBox.display(recv.get("err_msg").getAsString());
                }
                recAPRSCheckBox.setDisable(false);
            });

            threadExecutor.submit(dispatcher);
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
        else if (Double.parseDouble(command.getText()) < 0 || Double.parseDouble(command.getText()) > 120){
            AlertBox.display("Enter values between 0 and 120");
        }
        else{
            //Determine whether internet/APRS
            String s = command.getText();
            RobotEntry selectedItem = availableRobots.getSelectionModel().getSelectedItem();
            String selectedDirection = type.getSelectionModel().getSelectedItem();
            String string_command = Power.getText() + " " + selectedDirection + " " + s;
            if(!medium.isSelected()){
                HashMap<String, Object> map = new HashMap<>();
                if(selectedItem.getType() == EntryType.REMOTE){
                    String selectedMCID = selectedItem.getId();
                    HashMap<String, Object> cmd = new HashMap<>();
                    cmd.put("power", Power.getText());
                    cmd.put("direction", selectedDirection);
                    cmd.put("time", s);
                    
                    map.put("receiver_id", selectedMCID);
                    map.put("commands", Arrays.asList(cmd));
                    dispatcher = new BackendDispatcher(MessageStructure.REMOTE_CONTROL, map);
                }
                else{
                    HashMap<String, Object> cmd = new HashMap<>();
                    cmd.put("power", Power.getText());
                    cmd.put("direction", selectedDirection);
                    cmd.put("time", s);

                    map.put("commands", Arrays.asList(cmd));
                    dispatcher = new BackendDispatcher(MessageStructure.LOCAL_CONTROL, map);
                }
                threadExecutor.submit(dispatcher);
                sentListView.getItems().add(string_command);
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

                HashMap<String, Object> params = new HashMap<>();
                params.put("callsign", mycallsign);
                HashMap<String, Object> cmd = new HashMap<>();
                ArrayList<Object> cmdList = new ArrayList<>();
                cmd.put("power", Power.getText());
                cmd.put("direction", selectedDirection);
                cmd.put("time", s);
                cmdList.add(cmd);
                params.put("commands", cmdList);
                params.put("destination", sendcall);
                dispatcher = new BackendDispatcher(MessageStructure.SEND_APRS, params);
                
                dispatcher.setOnSucceeded(e -> {
                    JsonObject recv = dispatcher.getValue();
                    if(recv.get("status").getAsString().equals("error")){
                        AlertBox.display("Error: " + recv.get("err_msg"));
                    }
                });
                threadExecutor.submit(dispatcher);
            }
            Power.clear();
            type.setValue("N/A");
            command.clear();
        }
    }
    @FXML
    void checkBoxClicked(MouseEvent event) {
        if(fileValues.size() != 0){
            HashMap<String, Object> map = new HashMap<>();
            if(doNotDisturb.isSelected()){
                doNotDisturb.setDisable(true);
                showLoadingAnimation();
                map.put("doNotDisturb", true);
                dispatcher = new BackendDispatcher(MessageStructure.USER_DATA_UPDATE, map);
                dispatcher.setOnSucceeded(e -> {
                    hideLoadingAnimation();
                    doNotDisturb.setDisable(false);
                });
                threadExecutor.submit(dispatcher);
            }
            else{
                doNotDisturb.setDisable(true);
                showLoadingAnimation();
                map.put("doNotDisturb", false);
                dispatcher = new BackendDispatcher(MessageStructure.USER_DATA_UPDATE, map);
                dispatcher.setOnSucceeded(e -> {
                    doNotDisturb.setDisable(false);
                    hideLoadingAnimation();
                });
                threadExecutor.submit(dispatcher);
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
                HashMap<String, Object> map = new HashMap<>();
                map.put("port", localRobotConnection.getSelectionModel().getSelectedItem());
                dispatcher = new BackendDispatcher(MessageStructure.PAIR_CONNECT, map);
                pairButton.setDisable(true);
                dispatcher.setOnSucceeded(e -> {
                    //String passfail = dispatcher.getValue();
                    JsonObject response = dispatcher.getValue();
                    if (response.get("status").getAsString().equals("ok")){
                        availableRobots.getItems().add(0, new RobotEntry(localRobotConnection.getValue()));
                        localRobotConnection.setDisable(true);
                        File f = new File("important.txt");
                        if(f.exists()){
                            doNotDisturb.setDisable(false);
                        }
                        recAPRSCheckBox.setDisable(false);
                        pairingStatus = true;
                        pairButton.setText("Disconnect");
                    }
                    else if(response.get("status").getAsString().equals("error")){
                        //Show user pairing failed
                        AlertBox.display(response.get("err_msg").getAsString());
                    }
                    pairButton.setDisable(false);
                });
                threadExecutor.submit(dispatcher);
            }
            else{
                localRobotConnection.setValue(null);
                localRobotConnection.setDisable(false);
                availableRobots.getItems().remove(0);
                recAPRSCheckBox.setSelected(false);
                recAPRSCheckBox.setDisable(true);
                
                //Unpair and turn status offline
                pairButton.setDisable(false);
                pairButton.setText("Pair");

                dispatcher = new BackendDispatcher(MessageStructure.PAIR_DISCONNECT, null);
                dispatcher.setOnSucceeded(e -> {
                    JsonObject recv = dispatcher.getValue();
                    if(recv.get("status").getAsString().equals("error")){
                        AlertBox.display(recv.get("err_msg").getAsString());
                    }
                    else{
                        pairButton.setDisable(false);
                    }
                });

                BackendDispatcher stop_rec = new BackendDispatcher(MessageStructure.STOP_APRS_RECEIVE, null);
                stop_rec.setOnSucceeded(e -> {
                    JsonObject recv = dispatcher.getValue();
                    if(recv.get("status").getAsString().equals("error")){
                        AlertBox.display(recv.get("err_msg").getAsString());
                    }
                });

                threadExecutor.submit(dispatcher);
                threadExecutor.submit(stop_rec);

                pairingStatus = false;
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
        medium.setDisable(true);
        recAPRSCheckBox.setDisable(true);
        recAPRSCheckBox.setSelected(false);

        
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
            protected void updateItem(RobotEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(availableRobots.getPromptText());
                } else {
                    setText(item.toString());
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

            if (newValue == null) {
                return;
            }
            
            if(newValue.getType() == EntryType.APRS){
                medium.setDisable(true);
                medium.setSelected(true);
            }
            else if(newValue.getType() == EntryType.LOCAL){
                medium.setDisable(true);
                medium.setSelected(false);
            }
            else if(newValue.getType() == EntryType.REMOTE){
                medium.setDisable(true);
                medium.setSelected(false);
            }
        });

        localRobotConnection.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if(isNowShowing){
                comList.restart();
            }
        });

        if(new File("important.txt").exists()){
            Reader.read(fileValues);
        }

        availableRobots.setVisibleRowCount(3);
        availableRobots.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                ArrayList<RobotEntry> localEntries = new ArrayList<>();
                for (RobotEntry item : availableRobots.getItems()) {
                    if (item != null && item.getType() == EntryType.LOCAL || item.getType() == EntryType.APRS) {
                        localEntries.add(item);
                    }
                }

                dispatcher = new BackendDispatcher(MessageStructure.GET_DIRECTORY, null);
                dispatcher.setOnSucceeded(event -> {
                    JsonObject obj = dispatcher.getValue();

                    if (obj != null && obj.get("status").getAsString().equals("ok")){
                        ArrayList<RobotEntry> fullList = new ArrayList<>(localEntries);

                        for (JsonElement el : obj.getAsJsonArray("active_robots")) {
                            JsonObject robot = el.getAsJsonObject();
                            String id = robot.get("id").getAsString();
                            String school = robot.get("schoolName").getAsString();
                            String city = robot.get("city").getAsString();
                            String state = robot.get("state").getAsString();
                            fullList.add(new RobotEntry(id, school, city, state));
                        }

                        RobotEntry selectedNow = availableRobots.getSelectionModel().getSelectedItem() == null ? 
                                                null : availableRobots.getSelectionModel().getSelectedItem().get_copy();
                        availableRobots.getItems().setAll(fullList);

                        if (selectedNow != null){
                            availableRobots.getSelectionModel().select(selectedNow);
                        }
                    }
                });
                threadExecutor.submit(dispatcher);
            }
        });

        
        File callsignFile = new File("callsign.txt");
        if(callsignFile.exists()){
            BufferedReader br = new BufferedReader(new FileReader(callsignFile));
            availableRobots.getItems().add(new RobotEntry(br.readLine(), br.readLine()));
            br.close();
        }

        onUpdateV3 updater = new onUpdateV3();
        updater.startListening(() -> {
            //We have received a usb disconnect
            if(pairingStatus){
                localRobotConnection.getSelectionModel().clearSelection();
                localRobotConnection.getItems().clear();
                pairButton.setText("Pair");

                if(availableRobots.getItems().size() > 0){
                    //the first one must be the local robot
                    availableRobots.getItems().remove(0);
                }

                pairButton.setDisable(false);
                doNotDisturb.setSelected(true);
                doNotDisturb.setDisable(true);
                recAPRSCheckBox.setSelected(false);
                recAPRSCheckBox.setDisable(true);
                localRobotConnection.setDisable(false);
                pairingStatus = false;
            }
        });
        
        // onUpdateV2 ouv = new onUpdateV2();
        // ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
        //     @Override
        //     public void run() {
        //         ouv.call();
        //         String aprsEntry = ouv.getAPRSEntry();
        //         ouv.reset();
        //         ouv.resetEditedEntry();
        //         ouv.resetAPRSEntry();
        //         Platform.runLater(
        //         () -> {
        //             if(!aprsEntry.equals("")){
        //                 recListView.getItems().add(aprsEntry);
        //             }
        //         });
        //     }
        // }, 0, 100, TimeUnit.MILLISECONDS);
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

    public static RobotEntry getSelectedRobot(){
        return currRobot;
    }
    public static boolean getPairingStatus(){
        return pairingStatus;
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
                        Gson gson = new Gson();
                        JsonObject msg = new JsonObject();
                        msg.addProperty("type", "get_ports");
                        socket.send(gson.toJson(msg));
                        String comports = socket.recvStr();
                        JsonObject obj = gson.fromJson(comports, JsonObject.class);
                        JsonArray portsArray = obj.getAsJsonArray("ports");
    
                        ctx.destroy();
                        final CountDownLatch latch = new CountDownLatch(1);
                        Platform.runLater(new Runnable() {                          
                            @Override
                            public void run() {
                                try{
                                    if(portsArray == null){
                                        return;
                                    }
                                    ArrayList<String> output = new ArrayList<String>();
                                    for(JsonElement port : portsArray){
                                        output.add(port.getAsString());
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

