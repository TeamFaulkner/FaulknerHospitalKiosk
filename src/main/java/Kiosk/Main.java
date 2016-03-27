package Kiosk;/**
 * Created by Matt on 3/26/2016.
 */

import AStar.Map;
import Hospital.Kiosk;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static Stage primaryStage;
    public static Map map = new Map("C:\\Users\\Matt\\Desktop\\graph.DGS");
    private Parent root;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {

        primaryStage = stage;

        root = FXMLLoader.load(getClass().getResource("Dashboard.fxml"));

        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Main");
        primaryStage.show();

    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}