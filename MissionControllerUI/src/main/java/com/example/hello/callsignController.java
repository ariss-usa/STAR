package com.example.hello;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class callsignController {
    @FXML
    private TextField callsignText;
    @FXML
    private TextField callsignListener;
    @FXML
    protected void callSignSubmitPressed(ActionEvent event) throws IOException{
        String callsign = callsignText.getText();
        String listeningCall = callsignListener.getText();
        File file = new File("callsign.txt");
        PrintWriter pw = new PrintWriter(new FileWriter("callsign.txt", false));
        pw.write(callsign);
        pw.write("\n" + listeningCall);
        pw.close();
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }
    @FXML
    protected void callSignCancelPressed(ActionEvent event) throws IOException{
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }

    public void initialize() throws IOException{
        File file = new File("callsign.txt");
        if (file.isFile()){
            BufferedReader br = new BufferedReader(new FileReader(file));
            callsignText.setText(br.readLine());
            callsignListener.setText(br.readLine());
            br.close();
        }
    }
}
