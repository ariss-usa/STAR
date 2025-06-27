package org.ariss.star;

import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
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
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.*;

public class MissionController {
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
    @FXML
    private ImageView sstv_image;
    @FXML
    private StackPane stackpane;
    @FXML
    private CheckBox qsstv_checkbox;

    private Stage parent;
    private Parent root;
    public static ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
    private static RobotEntry currRobot;
    private static boolean pairingStatus = false;
    final private String ARISS_URL = "https://www.ariss.org/";
    final private String STAR_URL = "https://sites.google.com/view/ariss-starproject/home";
    final private String LOCALHOST_URL = "http://localhost:8080/index.html";
    private AvailableRobotsManager robotsManager;
    static ConfigManager configManager;
    private WritableImage sstvWritable;
    private PixelWriter pixelWriter;
    private Process qsstv;

    @FXML
    protected void qsstvCheckboxClicked(MouseEvent event) throws IOException {
        if (qsstv_checkbox.isSelected()) {
            qsstv = new ProcessBuilder("./qsstv").start();
        }
        else {
            if (qsstv != null && qsstv.isAlive()) {
                qsstv.destroy();
                qsstv = null;
            }
        }
    }

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
            //new ProcessBuilder("..\\gpredict-win32-2.2.1\\gpredict-win32-2.2.1\\gpredict.exe").start();
            new ProcessBuilder(".\\gpredict-win32-2.2.1\\gpredict-win32-2.2.1\\gpredict.exe").start();
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

        if(ConfigManager.hasCallsignConfig()){
            RobotEntry bot = ConfigManager.readCallsignConfig();
            if(bot != null){
                robotsManager.addAprsRobot(ConfigManager.callsignEntry);
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

        commandBuilderController cbController = loader.getController();
        cbController.setBaseController(this);

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
        if(controller.submitPressed() && ConfigManager.isGlobalModeEnabled()){
            //New/Entry may have been changed - push to server
            HashMap<String, Object> params = new HashMap<>();
            //Client backend will reread these new values from file
            params.put("doNotDisturb", doNotDisturb.isSelected());
            BackendDispatcher dispatcher = new BackendDispatcher(MessageStructure.USER_DATA_UPDATE, params);
            dispatcher.attachDefaultErrorHandler();
            threadExecutor.submit(dispatcher);
        }
    }
    
    protected void receive(){
        MessageStructure structure = recAPRSCheckBox.isSelected() ? MessageStructure.RECEIVE_APRS : MessageStructure.STOP_APRS_RECEIVE;
        BackendDispatcher dispatcher = new BackendDispatcher(structure, null);
        dispatcher.setOnSucceeded(e -> {
            JsonObject recv = dispatcher.getValue();
            if(recv.get("status").getAsString().equals("error")){
                if (recAPRSCheckBox.isSelected())
                    recAPRSCheckBox.setSelected(false);
                AlertBox.display("Error: " + recv.get("err_msg"));
            }
        });
        threadExecutor.submit(dispatcher);
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
            BackendDispatcher dispatcher;
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
                    dispatcher.attachDefaultErrorHandler();
                }
                else{
                    HashMap<String, Object> cmd = new HashMap<>();
                    cmd.put("power", Power.getText());
                    cmd.put("direction", selectedDirection);
                    cmd.put("time", s);

                    map.put("commands", Arrays.asList(cmd));
                    dispatcher = new BackendDispatcher(MessageStructure.LOCAL_CONTROL, map);
                    dispatcher.attachDefaultErrorHandler();
                }
                threadExecutor.submit(dispatcher);
            }
            else{
                if(!ConfigManager.hasCallsignConfig() || ConfigManager.callsignEntry == null){
                    AlertBox.display("Setup your callsign");
                }

                HashMap<String, Object> params = new HashMap<>();
                params.put("callsign", ConfigManager.callsignEntry.myCallsign);
                HashMap<String, Object> cmd = new HashMap<>();
                ArrayList<Object> cmdList = new ArrayList<>();
                cmd.put("power", Power.getText());
                cmd.put("direction", selectedDirection);
                cmd.put("time", s);
                cmdList.add(cmd);
                params.put("commands", cmdList);
                params.put("destination", ConfigManager.callsignEntry.destinationCallsign);
                dispatcher = new BackendDispatcher(MessageStructure.SEND_APRS, params);
                
                dispatcher.attachDefaultErrorHandler();
                threadExecutor.submit(dispatcher);
            }
            sentListView.getItems().add(string_command);
            Power.clear();
            type.setValue("N/A");
            command.clear();
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
                BackendDispatcher dispatcher = new BackendDispatcher(MessageStructure.PAIR_CONNECT, map);
                pairButton.setDisable(true);
                dispatcher.setOnSucceeded(e -> {
                    JsonObject response = dispatcher.getValue();
                    if (response.get("status").getAsString().equals("ok")){
                        robotsManager.addLocalRobot(new RobotEntry(localRobotConnection.getValue()));
                        localRobotConnection.setDisable(true);
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
                robotsManager.removeLocalRobot();
                
                //Unpair and turn status offline
                pairButton.setDisable(true);
                pairingStatus = false;
                
                BackendDispatcher dispatcher = new BackendDispatcher(MessageStructure.PAIR_DISCONNECT, null);
                dispatcher.setOnSucceeded(e -> {
                    JsonObject recv = dispatcher.getValue();
                    if(recv.get("status").getAsString().equals("error")){
                        AlertBox.display(recv.get("err_msg").getAsString());
                    }
                    else{
                        pairButton.setText("Pair");
                        pairButton.setDisable(false);
                    }
                });

                BackendDispatcher stop_rec = new BackendDispatcher(MessageStructure.STOP_APRS_RECEIVE, null);
                stop_rec.attachDefaultErrorHandler();

                threadExecutor.submit(dispatcher);
                threadExecutor.submit(stop_rec);

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
        medium.setDisable(true);
        robotsManager = new AvailableRobotsManager(threadExecutor, availableRobots);
        
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

        availableRobots.setVisibleRowCount(3);
        availableRobots.setItems(robotsManager.getDisplayedRobots());
        availableRobots.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if(isShowing && ConfigManager.isGlobalModeEnabled())
                robotsManager.refreshRemoteRobots();
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

        Service<JsonObject> comList = new Service<JsonObject>() {
            @Override
            protected Task<JsonObject> createTask() {
                return new BackendDispatcher(MessageStructure.GET_PORTS, null);
            }
        };

        comList.setOnSucceeded(event -> {
            JsonObject obj = comList.getValue();
            if(obj.get("status").getAsString().equals("ok")){
                JsonArray ports = obj.getAsJsonArray("ports");
                ArrayList<String> portsList = new ArrayList<>();
                for(JsonElement item : ports){
                    portsList.add(item.getAsString());
                }

                String prev_selection = null;
                if(localRobotConnection.getSelectionModel().getSelectedItem() != null){
                    prev_selection = new String(localRobotConnection.getSelectionModel().getSelectedItem());
                }
                localRobotConnection.getItems().setAll(portsList);
                localRobotConnection.getSelectionModel().select(prev_selection);
            }
        });

        localRobotConnection.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if(isNowShowing){
                comList.restart();
            }
        });

        if(ConfigManager.isGlobalModeEnabled()){
            ConfigManager.readConfig();
        }

        if(ConfigManager.hasCallsignConfig()){
            RobotEntry entry = ConfigManager.readCallsignConfig();
            if(entry != null){
                robotsManager.addAprsRobot(entry);
            }
        }

        doNotDisturb.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if(!ConfigManager.isGlobalModeEnabled() || !pairingStatus){
                event.consume();
                AlertBox.display("User must be configured and robot must be paired");
                return;
            }
            doNotDisturb.setSelected(!doNotDisturb.isSelected());
            handleDoNotDisturbUpdate();
            event.consume();
        });

        recAPRSCheckBox.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if(!pairingStatus && !recAPRSCheckBox.isSelected()){
                event.consume();
                AlertBox.display("Robot must be paired");
                return;
            }
            recAPRSCheckBox.setSelected(!recAPRSCheckBox.isSelected());
            receive();
            event.consume();
        });

        onUpdateV3 updater = new onUpdateV3();
        updater.startListening(update -> {
            String type = update.get("type").getAsString();
            switch (type) {
                case "usb_disconnect":
                    //We have received a usb disconnect
                    if(pairingStatus){
                        localRobotConnection.getSelectionModel().clearSelection();
                        localRobotConnection.getItems().clear();
                        pairButton.setText("Pair");

                        robotsManager.removeLocalRobot();

                        pairButton.setDisable(false);
                        localRobotConnection.setDisable(false);
                        pairingStatus = false;
                    }
                    break;
                case "command":
                    //Someone sent us a command
                    JsonArray commands = update.get("commands").getAsJsonArray();
                    String readableEntry = "";

                    for(int i = 0; i < commands.size(); i++){
                        JsonObject obj = commands.get(i).getAsJsonObject();
                        
                        readableEntry += obj.get("power").getAsString() + " " +
                                        obj.get("direction").getAsString() + " " +
                                        obj.get("time").getAsString();

                        if(commands.size() > 0 && i != commands.size() - 1){
                            readableEntry += ", ";
                        }
                    }

                    if (!readableEntry.equals(""))
                        recListView.getItems().add(readableEntry);
                    break;
            }
        });

        //sstvImage = new WritableImage(240, 320);
        //pixelWriter = sstvImage.getPixelWriter();

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            qsstv_checkbox.setDisable(true);
        }
        
        sstvWritable = new WritableImage(320, 256);
        sstv_image.setImage(sstvWritable);
        updater.startListeningQSSTV(sstvWritable);
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
    public void addToSendList(String item){
        sentListView.getItems().add(item);
    }

    private void handleDoNotDisturbUpdate(){
        HashMap<String, Object> map = new HashMap<>();
        doNotDisturb.setDisable(true);
        showLoadingAnimation();
        map.put("doNotDisturb", doNotDisturb.isSelected());
        BackendDispatcher dispatcher = new BackendDispatcher(MessageStructure.USER_DATA_UPDATE, map);
        dispatcher.setOnSucceeded(e -> {
            JsonObject obj = dispatcher.getValue();
            if(obj.get("status").getAsString().equals("error")){
                AlertBox.display(obj.get("err_msg").getAsString());
            }
            hideLoadingAnimation();
            doNotDisturb.setDisable(false);
        });
        threadExecutor.submit(dispatcher);
    }
}

