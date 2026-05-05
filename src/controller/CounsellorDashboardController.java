package controller;

import dao.UserDAO;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import service.ThemeService;

import java.io.IOException;

public class CounsellorDashboardController {

    private static CounsellorDashboardController instance;

    @FXML private VBox sidebar;
    @FXML private Pane overlayPane;
    @FXML private ScrollPane mainContentScroll;
    @FXML private VBox dashboardContent;
    @FXML private StackPane rootStack;
    @FXML private Label brandTitleLabel;
    @FXML private Label counsellorNameLabel;
    @FXML private Button profilePictureButton;
    @FXML private Button schoolProfileButton;
    @FXML private Button themeToggleButton;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label activeCasesLabel;
    @FXML private Label pendingRequestsLabel;
    @FXML private Label chatInboxBadgeLabel;
    @FXML private javafx.scene.control.Button chatInboxButton;

    private boolean isSidebarOpen = false;
    private static final double SIDEBAR_WIDTH = 300;
    private User currentUser;
    private final UserDAO userDAO = new UserDAO();

    public CounsellorDashboardController() {
        instance = this;
    }

    public static CounsellorDashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        sidebar.setTranslateX(-SIDEBAR_WIDTH);
        overlayPane.setVisible(false);
        brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Counsellor"));
    }

    public void initData(User user) {
        this.currentUser = user;
        brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Counsellor"));
        ThemeService.applyCurrentTheme(rootStack);
        ThemeService.updateThemeButton(themeToggleButton);
        counsellorNameLabel.setText("Welcome, " + user.getName());
        ProfileImageSupport.configureUserProfileButton(profilePictureButton, user);
        ProfileImageSupport.configureSchoolProfileButton(schoolProfileButton, user);
        profileNameLabel.setText(user.getName());
        profileEmailLabel.setText(user.getEmail());
        refreshDashboard();
    }

    public void refreshDashboard() {
        if (currentUser == null) {
            return;
        }
        int activeCases = userDAO.getCounsellorActiveCaseCount(currentUser.getUserId());
        int pendingRequests = userDAO.getCounsellorPendingRequestCount(currentUser.getUserId());
        int unreadInbox = userDAO.getCounsellorUnreadMessageCount(currentUser.getUserId());

        activeCasesLabel.setText(String.valueOf(activeCases));
        pendingRequestsLabel.setText(String.valueOf(pendingRequests));
        chatInboxBadgeLabel.setText(String.valueOf(unreadInbox));
        chatInboxBadgeLabel.setVisible(unreadInbox > 0);
        chatInboxBadgeLabel.setManaged(unreadInbox > 0);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();

            Object controller = loader.getController();
            if (controller instanceof CounsellorChatController) {
                ((CounsellorChatController) controller).initData(currentUser);
            } else if (controller instanceof CounsellorRequestsController) {
                ((CounsellorRequestsController) controller).initData(currentUser);
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
    private void openChatInbox(ActionEvent event) {
        loadView("/view/counsellor_chat_view.fxml");
    }

    @FXML
    private void openRequests(ActionEvent event) {
        loadView("/view/counsellor_requests_view.fxml");
    }

    @FXML
    public void scrollToTop() {
        mainContentScroll.setContent(dashboardContent);
        mainContentScroll.setVvalue(0);
        if (currentUser != null) {
            ProfileImageSupport.refreshUserProfileButton(profilePictureButton, currentUser);
            ProfileImageSupport.refreshSchoolProfileButton(schoolProfileButton);
        }
        ThemeService.applyCurrentTheme(rootStack);
        ThemeService.updateThemeButton(themeToggleButton);
        refreshDashboard();
        if (isSidebarOpen) {
            toggleSidebar();
        }
    }

    @FXML
    private void handleToggleTheme() {
        if (ThemeService.saveTheme(ThemeService.toggleTheme())) {
            ThemeService.applyCurrentTheme(rootStack);
            ThemeService.updateThemeButton(themeToggleButton);
        }
    }

    @FXML
    public void toggleSidebar() {
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
