package controller;

import dao.ProfileImageDAO;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.User;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

public final class ProfileImageSupport {
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final double HEADER_AVATAR_SIZE = 42.0;
    private static final ProfileImageDAO profileImageDAO = new ProfileImageDAO();

    private ProfileImageSupport() {
    }

    public static void configureUserProfileButton(Button button, User user) {
        if (button == null || user == null) {
            return;
        }
        prepareHeaderButton(button);
        refreshUserProfileButton(button, user);
        button.setTooltip(new Tooltip("Profile picture"));
        button.setOnAction(event -> showUserProfileMenu(button, user));
    }

    public static void configureSchoolProfileButton(Button button, User user) {
        if (button == null || user == null) {
            return;
        }
        prepareHeaderButton(button);
        refreshSchoolProfileButton(button);
        boolean editable = "Admin".equalsIgnoreCase(user.getRoleName());
        button.setTooltip(new Tooltip(editable ? "School profile picture" : "School profile picture"));
        button.setFocusTraversable(editable);
        if (!editable && !button.getStyleClass().contains("avatar-readonly")) {
            button.getStyleClass().add("avatar-readonly");
        }
        button.setOnAction(editable ? event -> showSchoolProfileMenu(button, user) : null);
    }

    public static void refreshUserProfileButton(Button button, User user) {
        if (button == null || user == null) {
            return;
        }
        applyAvatar(button, profileImageDAO.getUserProfileImage(user.getUserId()), initialFor(user.getName()), HEADER_AVATAR_SIZE);
    }

    public static void refreshSchoolProfileButton(Button button) {
        if (button == null) {
            return;
        }
        applyAvatar(button, profileImageDAO.getSchoolProfileImage(), "S", HEADER_AVATAR_SIZE);
    }

    public static Node createAvatarNode(byte[] imageData, String fallbackText, double size) {
        ImageView imageView = createImageView(imageData, size);
        if (imageView != null) {
            return imageView;
        }

        Label fallback = new Label(normalizeFallback(fallbackText));
        fallback.getStyleClass().add("avatar-fallback");
        fallback.setMinSize(size, size);
        fallback.setPrefSize(size, size);
        fallback.setMaxSize(size, size);
        fallback.setAlignment(Pos.CENTER);

        StackPane wrapper = new StackPane(fallback);
        wrapper.setMinSize(size, size);
        wrapper.setPrefSize(size, size);
        wrapper.setMaxSize(size, size);
        return wrapper;
    }

    private static void showUserProfileMenu(Button button, User user) {
        byte[] existingImage = profileImageDAO.getUserProfileImage(user.getUserId());
        ContextMenu menu = new ContextMenu();

        if (existingImage == null) {
            MenuItem insertItem = new MenuItem("Insert Picture");
            insertItem.setOnAction(event -> chooseAndSaveUserImage(button, user));
            menu.getItems().add(insertItem);
        } else {
            MenuItem updateItem = new MenuItem("Update Picture");
            updateItem.setOnAction(event -> chooseAndSaveUserImage(button, user));
            MenuItem removeItem = new MenuItem("Remove Picture");
            removeItem.setOnAction(event -> {
                if (profileImageDAO.removeUserProfileImage(user.getUserId())) {
                    refreshUserProfileButton(button, user);
                } else {
                    showAlert("Profile Picture", "Unable to remove the profile picture.");
                }
            });
            menu.getItems().addAll(updateItem, removeItem);
        }

        menu.show(button, javafx.geometry.Side.BOTTOM, 0, 6);
    }

    private static void showSchoolProfileMenu(Button button, User user) {
        byte[] existingImage = profileImageDAO.getSchoolProfileImage();
        ContextMenu menu = new ContextMenu();

        if (existingImage == null) {
            MenuItem insertItem = new MenuItem("Insert Picture");
            insertItem.setOnAction(event -> chooseAndSaveSchoolImage(button, user));
            menu.getItems().add(insertItem);
        } else {
            MenuItem updateItem = new MenuItem("Update Picture");
            updateItem.setOnAction(event -> chooseAndSaveSchoolImage(button, user));
            MenuItem removeItem = new MenuItem("Remove Picture");
            removeItem.setOnAction(event -> {
                if (profileImageDAO.removeSchoolProfileImage(user.getUserId())) {
                    refreshSchoolProfileButton(button);
                } else {
                    showAlert("School Profile Picture", "Unable to remove the school profile picture.");
                }
            });
            menu.getItems().addAll(updateItem, removeItem);
        }

        menu.show(button, javafx.geometry.Side.BOTTOM, 0, 6);
    }

    private static void chooseAndSaveUserImage(Button button, User user) {
        ImageSelection selection = chooseImage(button.getScene() == null ? null : button.getScene().getWindow());
        if (selection == null) {
            return;
        }
        if (profileImageDAO.saveUserProfileImage(user.getUserId(), selection.data, selection.mimeType)) {
            refreshUserProfileButton(button, user);
        } else {
            showAlert("Profile Picture", "Unable to save the selected profile picture.");
        }
    }

    private static void chooseAndSaveSchoolImage(Button button, User user) {
        ImageSelection selection = chooseImage(button.getScene() == null ? null : button.getScene().getWindow());
        if (selection == null) {
            return;
        }
        if (profileImageDAO.saveSchoolProfileImage(user.getUserId(), selection.data, selection.mimeType)) {
            refreshSchoolProfileButton(button);
        } else {
            showAlert("School Profile Picture", "Unable to save the selected school profile picture.");
        }
    }

    private static ImageSelection chooseImage(Window owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(owner);
        if (selectedFile == null) {
            return null;
        }
        if (selectedFile.length() > MAX_IMAGE_BYTES) {
            showAlert("Profile Picture", "Please choose an image smaller than 5 MB.");
            return null;
        }
        try {
            byte[] data = Files.readAllBytes(selectedFile.toPath());
            if (!isLoadableImage(data)) {
                showAlert("Profile Picture", "Please choose a valid image file.");
                return null;
            }
            return new ImageSelection(data, detectMimeType(selectedFile));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Profile Picture", "Unable to read the selected image.");
            return null;
        }
    }

    private static void prepareHeaderButton(Button button) {
        if (!button.getStyleClass().contains("avatar-button")) {
            button.getStyleClass().add("avatar-button");
        }
        button.setMinSize(HEADER_AVATAR_SIZE, HEADER_AVATAR_SIZE);
        button.setPrefSize(HEADER_AVATAR_SIZE, HEADER_AVATAR_SIZE);
        button.setMaxSize(HEADER_AVATAR_SIZE, HEADER_AVATAR_SIZE);
    }

    private static void applyAvatar(Button button, byte[] imageData, String fallbackText, double size) {
        ImageView imageView = createImageView(imageData, size);
        if (imageView != null) {
            button.setText(null);
            button.setGraphic(imageView);
            return;
        }
        button.setGraphic(null);
        button.setText(normalizeFallback(fallbackText));
    }

    private static ImageView createImageView(byte[] imageData, double size) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        Image image = new Image(new ByteArrayInputStream(imageData), size, size, true, true);
        if (image.isError()) {
            return null;
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);
        imageView.setClip(new Circle(size / 2, size / 2, size / 2));
        return imageView;
    }

    private static boolean isLoadableImage(byte[] data) {
        Image image = new Image(new ByteArrayInputStream(data), 1, 1, true, true);
        return !image.isError();
    }

    private static String detectMimeType(File file) throws IOException {
        String detected = Files.probeContentType(file.toPath());
        if (detected != null && detected.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return detected;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/*";
    }

    private static String initialFor(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        return String.valueOf(Character.toUpperCase(name.trim().charAt(0)));
    }

    private static String normalizeFallback(String fallbackText) {
        if (fallbackText == null || fallbackText.isBlank()) {
            return "U";
        }
        return fallbackText.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class ImageSelection {
        private final byte[] data;
        private final String mimeType;

        private ImageSelection(byte[] data, String mimeType) {
            this.data = data;
            this.mimeType = mimeType;
        }
    }
}
