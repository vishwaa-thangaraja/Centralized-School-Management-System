package controller;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.net.URL;
import java.util.Optional;

public final class DialogSupport {
    private DialogSupport() {
    }

    public static Optional<ButtonType> confirm(Node ownerNode, String title, String message) {
        Alert alert = create(ownerNode, Alert.AlertType.CONFIRMATION, title, title, message);
        return alert.showAndWait();
    }

    public static void info(Node ownerNode, String title, String message) {
        create(ownerNode, Alert.AlertType.INFORMATION, title, title, message).showAndWait();
    }

    public static void warning(Node ownerNode, String title, String message) {
        create(ownerNode, Alert.AlertType.WARNING, title, title, message).showAndWait();
    }

    public static void error(Node ownerNode, String title, String message) {
        create(ownerNode, Alert.AlertType.ERROR, title, title, message).showAndWait();
    }

    public static void info(Window owner, String title, String message) {
        create(owner, Alert.AlertType.INFORMATION, title, title, message).showAndWait();
    }

    private static Alert create(Node ownerNode, Alert.AlertType type, String title, String header, String message) {
        Window owner = ownerNode == null || ownerNode.getScene() == null ? null : ownerNode.getScene().getWindow();
        return create(owner, type, title, header, message);
    }

    private static Alert create(Window owner, Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        applyStyles(alert);
        return alert;
    }

    private static void applyStyles(Alert alert) {
        addStylesheet(alert, "/css/dashboard.css");
        addStylesheet(alert, "/css/login.css");
        alert.getDialogPane().getStyleClass().add("app-dialog");
    }

    private static void addStylesheet(Alert alert, String path) {
        URL css = DialogSupport.class.getResource(path);
        if (css != null) {
            alert.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
    }
}
