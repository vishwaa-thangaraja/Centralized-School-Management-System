package service;

import dao.LoginAuditDAO;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import model.User;

public class AuthService {
    // This variable will hold the logged-in user's data (the "Session")
    private static User currentUser;

    // Method to set the user after successful login
    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) {
            new LoginAuditDAO().logSuccessfulLogin(user.getUserId(), getLocalIpAddress());
        }
    }

    // Method to get the logged-in user's data
    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clearCurrentUser() {
        if (currentUser != null) {
            new LoginAuditDAO().logLogout(currentUser.getUserId());
        }
        currentUser = null;
    }

    private static String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "LOCAL";
        }
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
