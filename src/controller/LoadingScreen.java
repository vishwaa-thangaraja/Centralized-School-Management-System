package controller;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;

final class LoadingScreen {
    private Stage stage;
    private MediaPlayer mediaPlayer;

    void show(Window owner) {
        if (stage != null && stage.isShowing()) {
            return;
        }

        VBox panel = new VBox(18);
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add("loading-panel");

        if (!addVideo(panel)) {
            addImage(panel);
        }

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(58, 58);
        Label title = new Label("Opening your portal");
        title.getStyleClass().add("loading-title");
        Label subtitle = new Label("Preparing your school workspace...");
        subtitle.getStyleClass().add("loading-subtitle");
        panel.getChildren().addAll(spinner, title, subtitle);

        StackPane root = new StackPane(panel);
        root.getStyleClass().add("loading-overlay");
        Scene scene = new Scene(root);
        URL css = getClass().getResource("/css/login.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage = new Stage(StageStyle.UNDECORATED);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setX(Screen.getPrimary().getBounds().getMinX());
        stage.setY(Screen.getPrimary().getBounds().getMinY());
        stage.setWidth(Screen.getPrimary().getBounds().getWidth());
        stage.setHeight(Screen.getPrimary().getBounds().getHeight());
        stage.show();

        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
        }
    }

    void close() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        if (stage != null) {
            stage.close();
            stage = null;
        }
    }

    private boolean addVideo(VBox panel) {
        URL videoUrl = firstResource("/assets/loading.mp4", "/loading.mp4");
        if (videoUrl == null) {
            return false;
        }
        try {
            mediaPlayer = new MediaPlayer(new Media(videoUrl.toExternalForm()));
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(true);
            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setFitWidth(460);
            mediaView.setFitHeight(260);
            mediaView.setPreserveRatio(true);
            panel.getChildren().add(mediaView);
            return true;
        } catch (RuntimeException ex) {
            mediaPlayer = null;
            return false;
        }
    }

    private void addImage(VBox panel) {
        URL imageUrl = firstResource("/assets/loading.gif", "/loading.gif", "/assets/loading.png", "/loading.png");
        if (imageUrl == null) {
            return;
        }
        ImageView imageView = new ImageView(new Image(imageUrl.toExternalForm(), false));
        imageView.setFitWidth(390);
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(true);
        panel.getChildren().add(imageView);
    }

    private URL firstResource(String... paths) {
        for (String path : paths) {
            URL resource = getClass().getResource(path);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }
}
