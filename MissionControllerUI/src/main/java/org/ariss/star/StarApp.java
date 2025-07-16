package org.ariss.star;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class StarApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.getIcons().add(new Image(getClass().getResource("/org/ariss/images/arissLogo.jpg").toExternalForm()));
        System.out.println(StarApp.class.getResource("newGUI.fxml"));
        FXMLLoader fxmlLoader = new FXMLLoader(StarApp.class.getResource("newGUI.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 990, 493);

        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle("Mission Controller");

        stage.setScene(scene);
        stage.setMinWidth(990);
        stage.setMinHeight(493);

        Platform.runLater(()-> {
            stage.setWidth(990);
            stage.setHeight(493);
            stage.centerOnScreen();
        });
        stage.show();
        
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                we.consume();
                BackendDispatcher dispatcher = new BackendDispatcher(MessageStructure.END_PROGRAM, null);
                
                Runnable shutdown = () -> {
                    MissionController.threadExecutor.shutdownNow();
                    Platform.exit();
                    System.exit(0);
                };
                dispatcher.setOnSucceeded(e -> shutdown.run());
                dispatcher.setOnFailed(e -> shutdown.run());

                MissionController.threadExecutor.submit(dispatcher);
            }
        });
    }
    public static void main(String[] args) throws IOException {
        launch();
    }
}
