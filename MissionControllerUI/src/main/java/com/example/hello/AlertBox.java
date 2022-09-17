package com.example.hello;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AlertBox {
    public static void display(String message)    {
        Stage window = new Stage();
        window.setResizable(false);
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Alert");
        window.setMinWidth(250);

        Label label = new Label();
        label.setText(message);
        label.setFont(new Font("Verdana", 12.0));
        Button closeButton = new Button("Close");
        closeButton.setFont(new Font("Verdana", 12.0));
        closeButton.setOnAction(e -> window.close());

        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, closeButton);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(HelloApplication.class.getResource("style.css").toExternalForm());
        window.setScene(scene);
        window.showAndWait();
    }
}