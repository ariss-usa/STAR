package org.ariss.star;

import java.awt.Robot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

public class AvailableRobotsManager {
    private final ObservableList<RobotEntry> displayedRobots = FXCollections.observableArrayList();

    private RobotEntry localRobot;
    private RobotEntry aprsRobot;
    private ArrayList<RobotEntry> cache;
    private ComboBox<RobotEntry> boundComboBox;
    private final ExecutorService executor;
    private long lastRefresh = 0;
    private static final long COOLDOWN = TimeUnit.SECONDS.toMillis(5);

    public AvailableRobotsManager(ExecutorService executor, ComboBox<RobotEntry> comboBox) {
        this.executor = executor;
        this.boundComboBox = comboBox;
    }

    public ObservableList<RobotEntry> getDisplayedRobots() {
        return displayedRobots;
    }

    public void addLocalRobot(RobotEntry entry) {
        localRobot = entry;
        updateDisplay();
    }

    public void removeLocalRobot() {
        localRobot = null;
        updateDisplay();
    }

    public void addAprsRobot(RobotEntry entry) {
        aprsRobot = entry;
        updateDisplay();
    }

    public void removeAprsRobot() {
        aprsRobot = null;
        updateDisplay();
    }

    public void refreshRemoteRobots() {
        long currTime = System.currentTimeMillis();
        if((currTime - lastRefresh) < COOLDOWN && lastRefresh != 0){
            System.out.println("Rate limit: skipping remote robots refresh");
            return;
        }

        BackendDispatcher dispatcher = new BackendDispatcher(MessageStructure.GET_DIRECTORY, null);
        dispatcher.setOnSucceeded(e -> {
            JsonObject obj = dispatcher.getValue();
            if (obj != null && obj.get("status").getAsString().equals("ok")) {
                ArrayList<RobotEntry> remote = new ArrayList<>();
                for (JsonElement el : obj.getAsJsonArray("active_robots")) {
                    JsonObject robot = el.getAsJsonObject();
                    String id = robot.get("id").getAsString();
                    String school = robot.get("schoolName").getAsString();
                    String city = robot.get("city").getAsString();
                    String state = robot.get("state").getAsString();
                    remote.add(new RobotEntry(id, school, city, state));
                }
                cache = remote;
                lastRefresh = System.currentTimeMillis();
                updateDisplay();
            }
        });
        executor.submit(dispatcher);
    }

    private void updateDisplay() {
        Platform.runLater(() -> {
            List<RobotEntry> all = new ArrayList<>();
            if(localRobot != null) all.add(localRobot);
            if(aprsRobot != null) all.add(aprsRobot);
            if(cache != null) all.addAll(cache);

            RobotEntry previouslySelected = boundComboBox.getSelectionModel().getSelectedItem();
            previouslySelected = previouslySelected != null ? previouslySelected.get_copy() : null;
            displayedRobots.setAll(all);
            boundComboBox.getSelectionModel().select(previouslySelected);
        });
    }
}

