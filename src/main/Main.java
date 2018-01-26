package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.text.Font;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Font.loadFont(getClass().getResource("resources/fonts/FiraSans-Regular.otf").toExternalForm(), 10);

        Parent root = FXMLLoader.load(getClass().getResource("view/mainUI.fxml"));
        primaryStage.setTitle("GarlicGUI");
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Starts the FX toolkit, instantiates this class,
        // and calls start(...) on the FX Application thread:
        launch(args);
    }

}
