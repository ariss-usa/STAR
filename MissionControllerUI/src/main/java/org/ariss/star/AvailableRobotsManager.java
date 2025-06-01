package org.ariss.star;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AvailableRobotsManager {
    private final ObservableList<RobotEntry> displayedRobots = FXCollections.observableArrayList();

    private RobotEntry localRobot;
    private RobotEntry aprsRobot;
    private ArrayList<RobotEntry> cache;
    private final ExecutorService executor;

    public AvailableRobotsManager(ExecutorService executor) {
        this.executor = executor;
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
            if(!all.equals(displayedRobots)) displayedRobots.setAll(all);
        });
    }
}

