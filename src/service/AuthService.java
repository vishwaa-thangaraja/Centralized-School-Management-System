package service;

import dao.LoginAuditDAO;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import model.User;

public class AuthService {
    // This variable will hold the logged-in user's data (the "Session")
    private static User currentUser;
    private static final ExecutorService AUDIT_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "csms-audit");
        thread.setDaemon(true);
        return thread;
    });

    // Method to set the user after successful login
    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) {
            int userId = user.getUserId();
            AUDIT_EXECUTOR.submit(() -> new LoginAuditDAO().logSuccessfulLogin(userId, getLocalIpAddress()));
        }
    }

    // Method to get the logged-in user's data
    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clearCurrentUser() {
        if (currentUser != null) {
            int userId = currentUser.getUserId();
            AUDIT_EXECUTOR.submit(() -> new LoginAuditDAO().logLogout(userId));
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
