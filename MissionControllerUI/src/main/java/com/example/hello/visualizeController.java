package com.example.hello;

import java.io.IOException;

import javax.swing.Action;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.animation.PathTransition.OrientationType;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class visualizeController {
    @FXML
    private Pane content;
    public void initialize() {
        for(int i = 24; i < 600; i += 20){
            for(int j = 0; j < 800; j += 20){
                Rectangle rect = new Rectangle(j, i, 20, 20);
                rect.setFill(Color.TRANSPARENT);
                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(0.5);
                content.getChildren().add(rect);
            }
        }
    }
    @FXML
    protected void imageChange(ActionEvent event) throws IOException{
        
    }
}

