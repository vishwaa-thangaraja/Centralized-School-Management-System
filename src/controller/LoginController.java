package controller;

import dao.UserDAO;
import model.User;
import service.AuthService;
import service.SchoolSettingsService;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label loginTitleLabel;
    @FXML private Button loginButton;
    @FXML private StackPane loadingOverlay;

    private UserDAO userDAO = new UserDAO();
    private final LoadingScreen loadingScreen = new LoadingScreen();

    @FXML
    public void initialize() {
        loginTitleLabel.setText(SchoolSettingsService.getLoginTitle());
        loadingOverlay.setVisible(false);
        loginButton.setDefaultButton(true);
        passwordField.setOnAction(event -> loginButton.fire());
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        setLoading(true);
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() {
                String hashedPassword = AuthService.hashPassword(password);
                return userDAO.validateUser(email, hashedPassword);
            }
        };

        loginTask.setOnSucceeded(workerState -> {
            User user = loginTask.getValue();
            if (user == null) {
                setLoading(false);
                errorLabel.setText("Invalid email or password.");
                return;
            }

            String dashboardPath = dashboardPathFor(user);
            if (dashboardPath == null) {
                setLoading(false);
                errorLabel.setText("This role does not have a dashboard yet.");
                return;
            }
            AuthService.setCurrentUser(user);
            PauseTransition renderLoadingScreen = new PauseTransition(Duration.seconds(2));
            renderLoadingScreen.setOnFinished(delayEvent -> {
                try {
                    switchToDashboard(event, dashboardPath, user);
                } catch (IOException e) {
                    e.printStackTrace();
                    setLoading(false);
                    errorLabel.setText("Error loading dashboard.");
                }
            });
            renderLoadingScreen.play();
        });

        loginTask.setOnFailed(workerState -> {
            loginTask.getException().printStackTrace();
            setLoading(false);
            errorLabel.setText("Unable to login. Please try again.");
        });

        Thread loginThread = new Thread(loginTask, "csms-login");
        loginThread.setDaemon(true);
        loginThread.start();
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            switchToDashboard(event, "/view/forgot_password.fxml", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchToDashboard(ActionEvent event, String fxmlPath, User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        
        if (user != null) {
            Object controller = loader.getController();
            if (controller instanceof StudentDashboardController) {
                ((StudentDashboardController) controller).initData(user);
            } else if (controller instanceof TeacherDashboardController) {
                ((TeacherDashboardController) controller).initData(user);
            } else if (controller instanceof ParentDashboardController) {
                ((ParentDashboardController) controller).initData(user);
            } else if (controller instanceof CounsellorDashboardController) {
                ((CounsellorDashboardController) controller).initData(user);
            } else if (controller instanceof AdminDashboardController) {
                ((AdminDashboardController) controller).initData(user);
            }
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SchoolSettingsService.applyStageTitle(stage);
        stage.setScene(new Scene(root));
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();
        loadingScreen.close();
    }

    private String dashboardPathFor(User user) {
        if (user.getRoleName().equalsIgnoreCase("Student")) {
            return "/view/student_dashboard.fxml";
        }
        if (user.getRoleName().equalsIgnoreCase("Teacher")) {
            return "/view/teacher_dashboard.fxml";
        }
        if (user.getRoleName().equalsIgnoreCase("Parent")) {
            return "/view/parent_dashboard.fxml";
        }
        if (user.getRoleName().equalsIgnoreCase("Counsellor")) {
            return "/view/counsellor_dashboard.fxml";
        }
        if (user.getRoleName().equalsIgnoreCase("Admin")) {
            return "/view/admin_dashboard.fxml";
        }
        return null;
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisible(loading);
        loginButton.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        if (loading) {
            loadingScreen.show(loginButton.getScene() == null ? null : loginButton.getScene().getWindow());
        } else {
            loadingScreen.close();
        }
        if (loading) {
            errorLabel.setText("");
        }
    }
}
