package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.AuthService;
import service.SchoolSettingsService;

public class Main extends Application 
{
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        // Path adjusted to 'view' folder relative to this class
        Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
        
        SchoolSettingsService.applyStageTitle(primaryStage);
        
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setOnCloseRequest(event -> AuthService.clearCurrentUser());

        primaryStage.show();
    }

    public static void main(String[] args) 
    {
        launch(args);
    }
}
