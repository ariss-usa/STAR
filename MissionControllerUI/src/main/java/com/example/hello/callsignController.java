package com.example.hello;

import java.io.File;
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
    protected void callSignSubmitPressed(ActionEvent event) throws IOException{
        String str = callsignText.getText();
        File file = new File("callsign.txt");
        PrintWriter pw = new PrintWriter(new FileWriter("callsign.txt", false));
        pw.write(str);
        pw.close();
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }
    @FXML
    protected void callSignCancelPressed(ActionEvent event) throws IOException{
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }
}
