package org.ariss.star;

import java.io.IOException;

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
    final private int CALLSIGN_LENGTH = 6;
    @FXML
    protected void callSignSubmitPressed(ActionEvent event) throws IOException{
        String callsign = callsignText.getText();
        String destinationCall = callsignListener.getText();
        if(callsign.length() != CALLSIGN_LENGTH){
            AlertBox.display("Malformed callsign - Must be 6 characters in length");
        }
        else{
            ConfigManager.dumpCallsignConfig(callsign, destinationCall);
            ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
        }
    }
    @FXML
    protected void callSignCancelPressed(ActionEvent event) throws IOException{
        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
    }

    public void initialize() throws IOException{
        if(ConfigManager.hasCallsignConfig()){
            ConfigManager.readCallsignConfig();
            if(ConfigManager.callsignEntry.myCallsign != null && ConfigManager.callsignEntry.destinationCallsign != null){
                callsignText.setText(ConfigManager.callsignEntry.myCallsign);
                callsignListener.setText(ConfigManager.callsignEntry.destinationCallsign);
            }
        }
    }
}
