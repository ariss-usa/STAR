package org.ariss.star;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.JsonObject;

public class StarApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.getIcons().add(new Image(getClass().getResource("/org/ariss/images/arissLogo.jpg").toExternalForm()));
        System.out.println(StarApp.class.getResource("newGUI.fxml"));
        FXMLLoader fxmlLoader = new FXMLLoader(StarApp.class.getResource("newGUI.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 665, 466);

        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle("Mission Controller");
        stage.setResizable(false);
        stage.setScene(scene);

        Platform.runLater(()-> {
            stage.setWidth(665);
            stage.setHeight(466);
            stage.centerOnScreen();
        });
        stage.show();
        
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                we.consume();
                BackendDispatcher dispatcher = new BackendDispatcher(MessageStructure.END_PROGRAM, null);
                dispatcher.setOnSucceeded(e -> {
                    JsonObject obj = dispatcher.getValue();
                    if(obj.get("status").getAsString().equals("error")){
                        AlertBox.display(obj.get("err_msg").getAsString());
                    }
                    else{
                        MissionController.threadExecutor.shutdown();
                        try {
                            if (!MissionController.threadExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                                MissionController.threadExecutor.shutdownNow();
                            }
                        } catch (InterruptedException ex) {
                            MissionController.threadExecutor.shutdownNow();
                        }
                        Platform.exit();
                        System.exit(0);
                    }
                });
                MissionController.threadExecutor.submit(dispatcher);
            }
        });
    }
    public static void main(String[] args) throws IOException {
        launch();
    }
}
