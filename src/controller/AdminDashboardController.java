package controller;

import dao.AdminStatsDAO;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.User;
import service.AuthService;
import service.SchoolSettingsService;

import java.io.IOException;
import java.util.Map;

public class AdminDashboardController {

    private static AdminDashboardController instance;

    @FXML private VBox sidebar;
    @FXML private Pane overlayPane;
    @FXML private ScrollPane mainContentScroll;
    @FXML private VBox dashboardContent;
    @FXML private StackPane rootStack;
    @FXML private Label brandTitleLabel;
    @FXML private Label adminNameLabel;
    @FXML private Label studentsCountLabel;
    @FXML private Label teachersCountLabel;
    @FXML private Label counsellorsCountLabel;
    @FXML private Label classesCountLabel;
    @FXML private Label activeSessionsLabel;
    @FXML private Label redFlagsLabel;

    private boolean isSidebarOpen = false;
    private static final double SIDEBAR_WIDTH = 300;
    private User currentUser;
    private final AdminStatsDAO statsDAO = new AdminStatsDAO();

    public AdminDashboardController() {
        instance = this;
    }

    public static AdminDashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        if (sidebar != null) {
            sidebar.setTranslateX(-SIDEBAR_WIDTH);
        }
        if (overlayPane != null) {
            overlayPane.setVisible(false);
        }
        applySchoolSettings();
    }

    public void initData(User user) {
        this.currentUser = user;
        applySchoolSettings();
        adminNameLabel.setText("Welcome, " + user.getName());
        refreshDashboard();
    }

    public void applySchoolSettings() {
        if (brandTitleLabel != null) {
            brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Admin"));
        }
    }

    public void refreshDashboard() {
        Map<String, Integer> stats = statsDAO.getDashboardStats();
        studentsCountLabel.setText(String.valueOf(stats.getOrDefault("students", 0)));
        teachersCountLabel.setText(String.valueOf(stats.getOrDefault("teachers", 0)));
        counsellorsCountLabel.setText(String.valueOf(stats.getOrDefault("counsellors", 0)));
        classesCountLabel.setText(String.valueOf(stats.getOrDefault("classes", 0)));
        activeSessionsLabel.setText(String.valueOf(stats.getOrDefault("activeSessions", 0)));
        redFlagsLabel.setText(String.valueOf(stats.getOrDefault("redFlags", 0)));
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();
            Object controller = loader.getController();

            if (controller instanceof AdminUserManagementController) {
                ((AdminUserManagementController) controller).initData(currentUser);
            } else if (controller instanceof AdminAcademicMappingController) {
                ((AdminAcademicMappingController) controller).initData(currentUser);
            } else if (controller instanceof AdminLogsController) {
                ((AdminLogsController) controller).initData(currentUser);
            } else if (controller instanceof AdminSettingsController) {
                ((AdminSettingsController) controller).initData(currentUser);
            }

            mainContentScroll.setContent(newContent);
            if (isSidebarOpen) {
                toggleSidebar();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void scrollToTop() {
        mainContentScroll.setContent(dashboardContent);
        mainContentScroll.setVvalue(0);
        applySchoolSettings();
        refreshDashboard();
        if (isSidebarOpen) {
            toggleSidebar();
        }
    }

    @FXML
    private void openUserManagement(ActionEvent event) {
        loadView("/view/admin_user_management.fxml");
    }

    @FXML
    private void openAcademicMapping(ActionEvent event) {
        loadView("/view/admin_academic_mapping.fxml");
    }

    @FXML
    private void openSystemLogs(ActionEvent event) {
        loadView("/view/admin_logs.fxml");
    }

    @FXML
    private void openSettings(ActionEvent event) {
        loadView("/view/admin_settings.fxml");
    }

    @FXML
    public void toggleSidebar() {
        if (sidebar == null || overlayPane == null) {
            refreshDashboard();
            return;
        }
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), sidebar);
        if (!isSidebarOpen) {
            refreshDashboard();
            overlayPane.setVisible(true);
            transition.setToX(0);
            mainContentScroll.setEffect(new GaussianBlur(15));
            isSidebarOpen = true;
        } else {
            transition.setToX(-SIDEBAR_WIDTH);
            transition.setOnFinished(e -> {
                overlayPane.setVisible(false);
                mainContentScroll.setEffect(null);
            });
            isSidebarOpen = false;
        }
        transition.play();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            AuthService.clearCurrentUser();
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            SchoolSettingsService.applyStageTitle(stage);
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
