package controller;

import dao.AppSettingsDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.User;
import service.SchoolSettingsService;
import service.ThemeService;

import java.util.Map;

public class AdminSettingsController {

    @FXML private Label adminContextLabel;
    @FXML private Label statusLabel;
    @FXML private TextField schoolNameField;
    @FXML private TextArea addressArea;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private MenuButton themeMenuButton;

    private final AppSettingsDAO settingsDAO = new AppSettingsDAO();
    private User currentAdmin;

    public void initData(User adminUser) {
        this.currentAdmin = adminUser;
        adminContextLabel.setText("Admin: " + adminUser.getName());
        loadSettings();
    }

    private void loadSettings() {
        Map<String, String> settings = settingsDAO.getSettings();
        schoolNameField.setText(settings.getOrDefault("SCHOOL_NAME", "CSMS School"));
        addressArea.setText(settings.getOrDefault("SCHOOL_ADDRESS", ""));
        phoneField.setText(settings.getOrDefault("SCHOOL_PHONE", ""));
        emailField.setText(settings.getOrDefault("SCHOOL_EMAIL", ""));
        refreshThemeMenuText();
    }

    @FXML
    private void handleSaveSettings() {
        if (currentAdmin == null) {
            statusLabel.setText("Admin session not found.");
            return;
        }
        String schoolName = text(schoolNameField);
        if (schoolName.isEmpty()) {
            statusLabel.setText("School name is required.");
            return;
        }
        boolean saved = settingsDAO.saveSettings(
            currentAdmin.getUserId(),
            schoolName,
            text(addressArea),
            text(phoneField),
            text(emailField)
        );
        if (saved) {
            SchoolSettingsService.refresh();
            if (AdminDashboardController.getInstance() != null) {
                AdminDashboardController.getInstance().applySchoolSettings();
            }
            SchoolSettingsService.applyStageTitle((Stage) schoolNameField.getScene().getWindow());
            statusLabel.setText("Settings saved.");
        } else {
            statusLabel.setText("Settings could not be saved.");
        }
    }

    @FXML
    private void handleThemeButton() {
        applyTheme(ThemeService.toggleTheme());
    }

    @FXML
    private void handleLightTheme() {
        applyTheme(ThemeService.LIGHT);
    }

    @FXML
    private void handleDarkTheme() {
        applyTheme(ThemeService.DARK);
    }

    @FXML
    private void backToDashboard() {
        if (AdminDashboardController.getInstance() != null) {
            AdminDashboardController.getInstance().scrollToTop();
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String text(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private void applyTheme(String themeName) {
        if (ThemeService.saveTheme(themeName)) {
            ThemeService.applyCurrentTheme(schoolNameField.getScene().getRoot());
            refreshThemeMenuText();
            statusLabel.setText("Theme set to " + ThemeService.currentTheme() + ".");
        } else {
            statusLabel.setText("Theme could not be changed.");
        }
    }

    private void refreshThemeMenuText() {
        if (themeMenuButton != null) {
            themeMenuButton.setText("Theme: " + ThemeService.currentTheme());
        }
    }
}
