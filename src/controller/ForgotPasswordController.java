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
        if (email.isEmpty()) {
            statusLabel.setText("Enter email first!");
            return;
        }

        // Generate and Send
        String otp = otpGenerator.generateOTP();
        boolean sent = emailSender.sendOTP(email, otp);

        if (sent) {
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("OTP sent to your email.");
            // Reveal OTP and Password fields
            otpField.setVisible(true);
            newPasswordField.setVisible(true);
            resetBtn.setVisible(true);
            sendOtpBtn.setDisable(true);
        } else {
            statusLabel.setText("Failed to send email.");
        }
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String inputOtp = otpField.getText();
        String newPass = newPasswordField.getText();
        String email = emailField.getText().trim();

        if (otpGenerator.isOTPValid(inputOtp)) {
            String hashedPass = AuthService.hashPassword(newPass);
            if (userDAO.updatePassword(email, hashedPass)) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Password updated successfully!");
            } else {
                statusLabel.setText("Database update failed.");
            }
        } else {
            statusLabel.setText("Invalid or expired OTP.");
        }
    }

    @FXML
    private void backToLogin(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }
}