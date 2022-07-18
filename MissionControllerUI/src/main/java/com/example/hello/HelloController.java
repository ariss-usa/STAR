package com.example.hello;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

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

    private Stage parent;
    private Parent root;
    private ArrayList<String> possibleConnections = new ArrayList<String>();
    private ArrayList<String> possibleConnectionsAPRS = new ArrayList<String>();
    private ArrayList<String> possibleConnectionsBT = new ArrayList<String>();
    private ArrayList<Object> fileValues = new ArrayList<Object>();

    @FXML
    protected void configButtonPress(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("Dialogue.fxml"));
        root = loader.load();

        parent = (Stage) VBox.getScene().getWindow();
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Setup Dialog");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(parent);
        Scene scene = new Scene(root, 400, 250);
        dialogStage.setScene(scene);

        dialogStage.showAndWait();

        Dialogue controller = loader.getController();
        fileValues = controller.getFileList();
        if (controller.submitPressed()) {
            if (controller.isNewUser()) {
                returnEntries.checkfile(false);
            } else {
                returnEntries.checkfile(true);
            }

            availableRobots.getItems().removeAll(availableRobots.getItems());

            if (pairButton.getText().equals("Disconnect")) {
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
        } else if (Power.getText().isEmpty() || Integer.parseInt(Power.getText()) < 1 ||
                Integer.parseInt(Power.getText()) > 255) {
            AlertBox.display("Enter the power (from 1 to 255)");
        } else if (type.getSelectionModel().isEmpty() || type.getSelectionModel().getSelectedItem().equals("N/A")) {
            AlertBox.display("Select a direction");
        } else if (command.getText().isEmpty()) {
            AlertBox.display("Enter the amount of seconds you would like to run the robot");
        } else if (Double.parseDouble(command.getText()) < 1 || Double.parseDouble(command.getText()) > 120) {
            AlertBox.display("Enter values between 1 and 120");
        } else if (fileValues.size() != 0) {
            //Determine whether internet/APRS
            String str = availableRobots.getSelectionModel().getSelectedItem();
            String[] spl = str.split("\n");
            if (!medium.isSelected() && spl.length > 1) {
                try (ZContext ctx = new ZContext()) {
                    ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
                    socket.connect("tcp://127.0.0.1:5555");
                    String s = command.getText();
                    command.clear();
                    String selectedItem = availableRobots.getSelectionModel().getSelectedItem();
                    String selectedMCID = selectedItem.substring(0, selectedItem.indexOf("\n"));
                    String selectedDirection = type.getSelectionModel().getSelectedItem();
                    String leftPower;
                    String rightPower;
                    if (selectedDirection.equals("forward"))    {
                        leftPower = Power.getText();
                        rightPower = "-" + Power.getText();
                    }
                    else if (selectedDirection.equals("backward"))    {
                        leftPower = "-" + Power.getText();
                        rightPower = Power.getText();
                    }
                    else if (selectedDirection.equals("left"))    {
                        leftPower = Power.getText();
                        rightPower = Power.getText();
                    }
                    else    {
                        leftPower = "-" + Power.getText();
                        rightPower = "-" + Power.getText();
                    }
                    String command = "SEND " + leftPower + " " + rightPower + " "
                            + s + " Selected MCid: " + selectedMCID;
                    socket.send(command);
                    socket.recv();
                    sentListView.getItems().add(command);

                }
            } else {
                //for connection via APRS
            }
        }
    }

    @FXML
    void checkBoxClicked(MouseEvent event) {
        if (fileValues.size() != 0) {
            if (doNotDisturb.isSelected()) {
                recListView.getItems().clear();
                try (ZContext ctx = new ZContext()) {
                    ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
                    socket.connect("tcp://127.0.0.1:5555");
                    socket.send("editOnline, ChangeTo: No");
                    socket.recv();
                }
            } else {
                try (ZContext ctx = new ZContext()) {
                    ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
                    socket.connect("tcp://127.0.0.1:5555");
                    socket.send("editOnline, ChangeTo: Yes");
                    socket.recv();
                }
            }
        }
    }

    @FXML
    void pairPressed(MouseEvent event) {
        if (localRobotConnection.getSelectionModel().isEmpty()) {
            AlertBox.display("Choose a robot to pair with");
        } else {
            String pairText = pairButton.getText();
            if (pairText.equals("Pair")) {
                //Attempt to pair

                //
                availableRobots.getItems().add(0, localRobotConnection.getValue());
                localRobotConnection.setDisable(true);
                doNotDisturb.setDisable(false);
                pairButton.setText("Disconnect");
            } else {
                localRobotConnection.setValue(null);
                localRobotConnection.setDisable(false);
                doNotDisturb.setDisable(true);
                availableRobots.getItems().remove(0);
                pairButton.setText("Pair");
            }
        }
    }

    @FXML
    public void initialize() {
        type.getItems().addAll("N/A", "forward", "backward", "right", "left", "pause");
        possibleConnectionsAPRS.add("APRS Compatible Device 1");
        possibleConnectionsAPRS.add("APRS Compatible Device 2");

        possibleConnectionsBT.add("BT Compatible Device 1");
        possibleConnectionsBT.add("BT Compatible Device 2");
        possibleConnectionsBT.add("BT Compatible Device 3");

        localRobotConnection.getItems().addAll(possibleConnectionsBT);

        doNotDisturb.setSelected(true);
        doNotDisturb.setDisable(true);

        availableRobots.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                return;
            }

            String[] spl = newValue.split("\n");
            if (spl.length > 1) {
                if (newValue.split("\n")[2].equals("SDR Dongle: false")) {
                    medium.setSelected(false);
                    medium.setDisable(true);
                } else {
                    medium.setDisable(false);
                }
            } else {
                medium.setDisable(true);
            }
        });
        medium.setDisable(true);

        File tempFile = new File("important.txt");
        if (tempFile.exists()) {
            Reader.read(fileValues);
            if (fileValues.size() != 0) {
                returnEntries.checkfile(true);
                //String med = fileValues.get(5).toString();
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
                            if (addedEntry.contains("New Command:")) {
                                if (!doNotDisturb.isSelected()) {
                                    recListView.getItems().add(addedEntry.substring(13, addedEntry.length()));
                                }
                            } else if (addedEntry.contains("New Robot:")) {
                                if (!addedEntry.substring(11, 16).equals(fileValues.get(0).toString())) {
                                    possibleConnections.add(addedEntry);
                                    availableRobots.getItems().add(filter.filterEntry(addedEntry, fileValues.get(0).toString()));
                                }
                            } else if (!editedEntry.equals("")) {
                                String[] arr = editedEntry.split("\nIndex: ");
                                int index = Integer.parseInt(arr[1]);
                                possibleConnections.set(index, arr[0]);
                                availableRobots.getItems().setAll(filter.filterInternetInput(possibleConnections, fileValues.get(0).toString()));
                            }
                        });
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
}

class onUpdateV2 {
    private String newUpdate = "";
    private String editedDirEntry = "";

    public void call() {
        try (ZContext ctx = new ZContext()) {
            Socket client = ctx.createSocket(SocketType.REP);
            Socket editSocket = ctx.createSocket(SocketType.REP);
            assert (client != null);
            client.connect("tcp://127.0.0.1:5556");
            editSocket.connect("tcp://127.0.0.1:5557");

            org.zeromq.ZMQ.Poller poller = ctx.createPoller(2);
            poller.register(client, ZMQ.Poller.POLLIN);
            poller.register(editSocket, ZMQ.Poller.POLLIN);

            Object var = poller.poll(2);

            if (poller.pollin(0)) {
                String reply = client.recvStr(ZMQ.DONTWAIT);
                client.send("Recieved");
                newUpdate = reply;
            }
            if (poller.pollin(1)) {
                String reply = editSocket.recvStr(ZMQ.DONTWAIT);
                editSocket.send("Recieved");
                editedDirEntry = reply;
            }
        }
    }

    public String getNewUpdate() {
        return newUpdate;
    }

    public String getEditedEntry() {
        return editedDirEntry;
    }

    public void resetEditedEntry() {
        editedDirEntry = "";
    }

    public void reset() {
        newUpdate = "";
    }
}

class filter {
    public static ArrayList<String> filterInternetInput(ArrayList<String> list, String myMC) {
        ArrayList<String> filteredArray = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) {
            String[] split = list.get(i).split("\n|Online: |Connected: ");
            String online = split[6];
            String connected = split[8];
            if (online.equals("Yes")
                    && connected.equals("No") && !split[0].equals(myMC)) {
                String entry = split[0] + "\n" + split[1] + "\n" + split[2] + "\n" + split[4];
                filteredArray.add(entry);
            }
        }
        return filteredArray;
    }

    public static String filterEntry(String str, String myMC) {
        String[] split = str.split("\n|Online: |Connected: ");
        String mc = split[0].substring(11, 16);
        String online = split[6];
        String connected = split[8];
        if (online.equals("Yes")
                && connected.equals("No") && !mc.equals(myMC)) {
            String entry = mc + "\n" + split[1] + "\n" + split[2] + "\n" + split[4];
            return entry;
        }
        return "";
    }
}
