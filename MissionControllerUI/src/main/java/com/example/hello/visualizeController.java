package com.example.hello;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.scene.control.MenuItem;

public class visualizeController {
    @FXML
    private Pane content;
    @FXML
    private Button submit;
    @FXML
    private TextArea commandBuilder;
    @FXML
    private ImageView imgView;
    private Rectangle robot;
    private double heading = 0;
    private String [] arr;
    private int counter=0;
    public void initialize() {
        robot = new Rectangle(400, 300, 20, 20);
        robot.setFill(Color.RED);
        robot.setStroke(Color.BLACK);
        robot.setStrokeWidth(0.5);
        content.getChildren().add(robot);
        robot.setManaged(false);

        commandBuilder.appendText("100 forward 3\r\n");
        commandBuilder.appendText("50 backward 2\r\n");
        commandBuilder.appendText("150 left 4\r\n");
        commandBuilder.appendText("255 right 1\r\n");
    }
    private void move(double time, double power, boolean forwardOrBack){
        TranslateTransition tt = new TranslateTransition();
        tt.setNode(robot);
        tt.setDuration(Duration.seconds(time));
        double byY;
        double byX;
        if(forwardOrBack){
            byY = -Math.cos(heading * Math.PI/180) * time * power/2;
            byX = Math.sin(heading * Math.PI/180) * time * power/2;
        }
        else{
            byY = Math.cos(heading * Math.PI/180) * time * power/2;
            byX = -Math.sin(heading * Math.PI/180) * time * power/2; 
        }
        tt.setByX(byX);
        tt.setByY(byY);
        tt.play();

        tt.setOnFinished(e -> {
            counter++;
            if(counter >= arr.length){ counter = 0; return;}
            String [] newArr = arr[counter].split(" ");
            transitionCaller(newArr);
        });
    }
    private void rotate(double time, double power, boolean leftOrRight){
        RotateTransition rt = new RotateTransition(Duration.seconds(time));
        rt.setNode(robot);
        //true for left
        //rt.setRate(Math.min(power/177.5, 2));
        if(leftOrRight){
            rt.setByAngle(-180 * time);
        }
        else{
            rt.setByAngle(180 * time);
        }
        heading += rt.getByAngle();
        rt.play();

        rt.setOnFinished(e -> {
            counter++;
            if(counter >= arr.length){ counter = 0; return;}
            String [] newArr = arr[counter].split(" ");
            transitionCaller(newArr);
        });
    }
    @FXML
    protected void imageChange(ActionEvent event) throws IOException{
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open image file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*jpg"));
        File file = fileChooser.showOpenDialog(((MenuItem)event.getTarget()).getParentPopup().getOwnerWindow());
        if(file == null) return;
        Image image = new Image(file.toURI().toString());
        imgView.setImage(image);
    }
    @FXML
    protected void pathSave(ActionEvent event) throws IOException{
        String str = commandBuilder.getText();
        PrintWriter pw = new PrintWriter(new FileWriter("commands.txt", false));
        pw.write(str);
        pw.close();
        AlertBox.display("File successfully saved!");
    }
    @FXML
    protected void submit(ActionEvent event) throws IOException{
        TranslateTransition tt = new TranslateTransition();
        tt.setNode(robot);
        tt.setDuration(Duration.millis(10));
        tt.setByX(-robot.getTranslateX());
        tt.setByY(-robot.getTranslateY());
        tt.play();
        RotateTransition rt = new RotateTransition(Duration.millis(1));
        rt.setNode(robot);
        if(heading < 0){
            rt.setByAngle(heading);
        }else{
            rt.setByAngle(360-heading);
        }
        heading = 0;
        rt.play();
        tt.setOnFinished(e -> {
            String str = commandBuilder.getText();
            checkFormat(str);
            arr = str.split("\n");
            String [] splitArr = arr[0].split(" ");
            transitionCaller(splitArr);
        });
    }
    private void transitionCaller(String [] splitArr){
        if(splitArr[1].equals("forward")){
            move(Double.parseDouble(splitArr[2]), Double.parseDouble(splitArr[0]), true);        
        }
        else if(splitArr[1].equals("backward")){
            move(Double.parseDouble(splitArr[2]), Double.parseDouble(splitArr[0]), false);
        }
        else if(splitArr[1].equals("left")){
            rotate(Double.parseDouble(splitArr[2]), Double.parseDouble(splitArr[0]), true);
        }
        else{
            rotate(Double.parseDouble(splitArr[2]), Double.parseDouble(splitArr[0]), false);
        }
    }
    public boolean checkFormat(String str){
        String [] arr = str.split("\n");
        for(int i = 0; i < arr.length; i++){
            String [] split = arr[i].split(" ");
            if(split.length != 3){
                AlertBox.display("Wrong format");
                return false;
            }
            try{
                double power = Double.parseDouble(split[0]);
                if (power < 1 || power > 255 )  {
                    AlertBox.display("Enter the power (from 1 to 255)");
                    return false;
                }
                String dir = split[1];
                dir = dir.toLowerCase();
                ArrayList<String> dirCheck = new ArrayList<String>(Arrays.asList("forward", "backward", "right", "left", "pause"));
                if (!dirCheck.contains(dir)){
                    AlertBox.display("Enter a valid direction");
                    return false;
                }
                try{
                    double time = Double.parseDouble(split[2]);
                    if(time < 0 || time > 100){
                        AlertBox.display("Enter the time (from 0 to 100)");
                        return false;
                    }
                }
                catch(Exception e){
                    AlertBox.display("Time is in an incorrect format");
                }
            }
            catch(Exception e){
                AlertBox.display("Power is in an incorrect format");
                return false;
            }
        }
        return true;
    }
}

