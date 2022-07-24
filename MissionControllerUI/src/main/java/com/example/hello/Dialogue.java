package com.example.hello;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
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
    private Label dongleLabel;
    @FXML
    private Button enter;
    @FXML
    private TextField school;
    @FXML
    private Label schoolNameLabel;
    @FXML
    private ChoiceBox<Boolean> sdr;
    @FXML
    private ComboBox<String> state;
    @FXML
    private Label stateNameLabel;

    private String[] states = {"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware",
            "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
            "Maine", "Maryland", "Massachusetts", "Michigan", "Minesota", "Mississippi", "Missouri", "Montana", "Nebraska",
            "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
            "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas",
            "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming"};

    private Boolean[] sdrOptions = {true, false};
    private String stateName;
    private boolean sdrValue;
    private ArrayList<Object> list = new ArrayList<>();
    private boolean isSubmitPressed = false;
    private boolean newUser = true;

    @FXML
    void pressed(ActionEvent event) {
        isSubmitPressed = false;
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }

    @FXML
    private void inputData(ActionEvent event) throws IOException {
        if (school.getText().isEmpty()) {
            AlertBox.display("Enter your school's name in the Text Box");
        }
        else if (city.getText().isEmpty())  {
            AlertBox.display("Enter your city's name in the Text Box with the proper prompt");
        }
        else if (state.getSelectionModel().isEmpty())  {
            AlertBox.display("Enter a response in the states dropdown");
        }
        else if (sdr.getSelectionModel().isEmpty())  {
            AlertBox.display("Enter a response in the SDR Dongle present? dropdown");
        }
        else{
            File file = new File("important.txt");
            String generateID = "";
            if (file.exists()){
                Reader.read(list);
                generateID = list.get(0).toString();
                newUser = false;
            }
            else{
                //generateID = java.util.UUID.randomUUID().toString();
                for(int i = 0; i < 5; i++){
                    generateID += (int)Math.floor(Math.random() * 10);
                }
            }

            list.clear();
            list.add(generateID);
            list.add(school.getText());
            list.add(city.getText());
            list.add(state.getValue());
            list.add(sdr.getValue());
            Writer.write(list);

            ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
            isSubmitPressed = true;
        }
    }
    public void initialize() {
        sdr.getItems().addAll(sdrOptions);
        state.getItems().addAll(states);
        sdr.setOnAction(this::getSdr);
        state.setOnAction(this::getState);
        
        File file = new File("important.txt");
        if (file.isFile()){
            Reader.read(list);
            school.setText(list.get(1).toString());
            city.setText(list.get(2).toString());
            state.getSelectionModel().select(list.get(3).toString());
            sdr.setValue(Boolean.parseBoolean(list.get(4).toString()));
        }    
    }
    public void getSdr(ActionEvent event)  {
        setSdr(sdr.getValue());
    }

    private void setSdr(Boolean move) {
        this.sdrValue = move;
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
    public ArrayList<Object> getFileList(){
        return list;
    }
    public boolean isNewUser(){
        return newUser;
    }
}
