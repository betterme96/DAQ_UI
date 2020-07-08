package com.wzb;

import com.wzb.controller.BtnVboxController;
import com.wzb.models.Status;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("views/MainWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 500, 400);
        BtnVboxController controller = fxmlLoader.getController();//获取controller实例对象

       // controller.setStatus(1);
        primaryStage.setScene(scene);
        primaryStage.setTitle("DAQ");
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
