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
                try(ZContext ctx = new ZContext()){
                    ZMQ.Socket socket = ctx.createSocket(SocketType.REQ);
                    socket.connect("tcp://127.0.0.1:5555");
                    socket.send("END");
                    socket.recv();
                    ctx.destroy();
                }
                MissionController.threadExecutor.shutdown();
                try{
                    if (!MissionController.threadExecutor.awaitTermination(10, TimeUnit.SECONDS)){
                        MissionController.threadExecutor.shutdownNow();
                    }
                }
                catch(InterruptedException e){
                    MissionController.threadExecutor.shutdownNow();
                }
                System.exit(0);
            }
        });
    }
    public static void main(String[] args) throws IOException {
        launch();
    }
}
