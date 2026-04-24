package controller;

import dao.UserDAO;
import service.AuthService;
import service.EmailSender;
import service.OTPGenerator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class ForgotPasswordController {

    @FXML private TextField emailField, otpField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label statusLabel;
    @FXML private Button sendOtpBtn, resetBtn;

    private UserDAO userDAO = new UserDAO();
    private EmailSender emailSender = new EmailSender();
    private OTPGenerator otpGenerator = new OTPGenerator();

    @FXML
    private void handleSendOTP(ActionEvent event) {
    String email = emailField.getText().trim();
    statusLabel.setStyle("-fx-text-fill: red;");

    if (email.isEmpty()) {
        statusLabel.setText("Enter email first!");
        return;
    }

    // 1. FORMAT CHECK (Regex)
    // Checks for: characters + @ + characters + . + characters
    String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
    if (!email.matches(emailRegex)) {
        statusLabel.setText("Please enter a valid email format (e.g., name@mail.com)");
        return;
    }

    // 2. REGISTERED CHECK (Database)
    if (!userDAO.emailExists(email)) {
        statusLabel.setText("This email is not registered in our system.");
        return;
    }

    // 3. SUCCESS PATH
    statusLabel.setStyle("-fx-text-fill: blue;");
    statusLabel.setText("Verifying and sending OTP...");

    String otp = otpGenerator.generateOTP();
    boolean sent = emailSender.sendOTP(email, otp);

    if (sent) {
        statusLabel.setStyle("-fx-text-fill: green;");
        statusLabel.setText("OTP sent to your registered email.");
        otpField.setVisible(true);
        newPasswordField.setVisible(true);
        resetBtn.setVisible(true);
        sendOtpBtn.setDisable(true);
        emailField.setEditable(false);
    } else {
        statusLabel.setText("Failed to send email. Check connection.");
    }
}

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String inputOtp = otpField.getText();
        String newPass = newPasswordField.getText();
        String email = emailField.getText().trim();

        if (newPass.length() < 6) {
            statusLabel.setText("Password must be at least 6 characters.");
            return;
        }

        if (otpGenerator.isOTPValid(inputOtp)) {
            String hashedPass = AuthService.hashPassword(newPass);
            if (userDAO.updatePassword(email, hashedPass)) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Password updated successfully!");
                resetBtn.setDisable(true);
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("Database update failed.");
            }
        } else {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Invalid or expired OTP.");
        }
    }

    @FXML
    private void backToLogin(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        // Since we likely used setFullScreen(true) in the LoginController,
        // we'll just set the root to keep the window state consistent.
        stage.getScene().setRoot(root);
    }
}
