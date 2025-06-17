package org.ariss.star;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class Dialogue {

    @FXML
    private Button cancel;
    @FXML
    private TextField city;
    @FXML
    private Label cityNameLabel;
    @FXML
    private Button enter;
    @FXML
    private TextField school;
    @FXML
    private Label schoolNameLabel;
    @FXML
    private ComboBox<String> state;
    @FXML
    private Label stateNameLabel;

    private String[] states = {"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware",
            "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
            "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska",
            "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
            "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas",
            "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming"};

    private String stateName;
    private boolean isSubmitPressed = false;

    @FXML
    void pressed(ActionEvent event) {
        isSubmitPressed = false;
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }

    @FXML
    private void inputData(ActionEvent event) throws IOException {
        if (school.getText().isEmpty()) {
            AlertBox.display("Enter your school's name in the textbox");
        }
        else if (city.getText().isEmpty())  {
            AlertBox.display("Enter your city's name in the textbox");
        }
        else if (state.getSelectionModel().isEmpty())  {
            AlertBox.display("Enter a response in the states dropdown");
        }
        else{
            String id = "";
            if(ConfigManager.isGlobalModeEnabled()){
                ConfigManager.readConfig();
                id = ConfigManager.getConfig().id;
            }
            else{
                //id = java.util.UUID.randomUUID().toString();
                for(int i = 0; i < 5; i++){
                    id += (int)Math.floor(Math.random() * 10);
                }
            }

            ConfigManager.dumpConfig(id, school.getText(), city.getText(), state.getValue());
            ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
            isSubmitPressed = true;
        }
    }
    public void initialize() {
        state.getItems().addAll(states);
        state.setOnAction(this::getState);
        
        if(ConfigManager.isGlobalModeEnabled() && ConfigManager.readConfig()){
            UserConfig config = ConfigManager.getConfig();
            school.setText(config.school);
            city.setText(config.city);
            state.getSelectionModel().select(config.state);
        }
    }
    public void getState(ActionEvent event)  {
        setState(state.getValue());
    }

    private void setState(String move) {
        this.stateName = move;
    }
    public boolean submitPressed(){
        return isSubmitPressed;
    }
}
