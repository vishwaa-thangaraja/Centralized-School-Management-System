package controller;

import dao.UserDAO;
import model.User;
import service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        String hashedPassword = AuthService.hashPassword(password);
        User user = userDAO.validateUser(email, hashedPassword);

        if (user != null) {
            AuthService.setCurrentUser(user);

            try {
                if (user.getRoleName().equalsIgnoreCase("Student")) {
                    switchToDashboard(event, "/view/student_dashboard.fxml", user);
                } else if (user.getRoleName().equalsIgnoreCase("Teacher")) {
                    switchToDashboard(event, "/view/teacher_dashboard.fxml", user);
                } else {
                    errorLabel.setText("This role does not have a dashboard yet.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Error loading dashboard.");
            }
        } else {
            errorLabel.setText("Invalid email or password.");
        }
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
            }
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setFullScreen(true);
        stage.show();
    }
}
