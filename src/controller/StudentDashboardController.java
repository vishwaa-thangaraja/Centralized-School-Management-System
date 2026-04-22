package controller;

import dao.UserDAO;
import model.User;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.util.Duration;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class StudentDashboardController {

    @FXML private VBox sidebar;
    @FXML private Pane overlayPane;
    @FXML private ScrollPane mainContentScroll;
    @FXML private StackPane rootStack;
    @FXML private Label userNameLabel;
    @FXML private Label attendanceVal, marksVal, pendingTasks, insightMessage;
    @FXML private VBox insightCard;
    @FXML private LineChart<String, Number> performanceChart;

    private boolean isSidebarOpen = false;
    private final double SIDEBAR_WIDTH = 300;

    @FXML
    public void initialize() {
        sidebar.setTranslateX(-SIDEBAR_WIDTH);
        overlayPane.setVisible(false);
    }

    public void initData(User user) {
        userNameLabel.setText("Welcome, " + user.getName());
        
        UserDAO dao = new UserDAO();
        
        // Fetch data from DAO
        double att = dao.getAttendancePercentage(user.getUserId());
        double avg = dao.getAverageMarks(user.getUserId());
        int pending = dao.getPendingAssignments(user.getUserId());

        // Update UI Labels
        attendanceVal.setText(String.format("%.1f%%", att));
        marksVal.setText(String.format("%.1f/100", avg));
        pendingTasks.setText(pending + " Pending");

        // Populate Chart with Sample Data (You can replace this with a DAO call later)
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Marks Trend");
        series.getData().add(new XYChart.Data<>("Midterm Math", 85));
        series.getData().add(new XYChart.Data<>("Midterm CS", 92));
        performanceChart.getData().clear();
        performanceChart.getData().add(series);

        // State Engine UI Logic (Using Unicode for emojis to avoid encoding errors)
        if (att < 75) {
            insightMessage.setText("\u26A0\uFE0F Critical: Your attendance is " + String.format("%.1f%%", att) + ". Contact your coordinator.");
            insightCard.setStyle("-fx-border-color: #e74c3c; -fx-background-color: #fdedec; -fx-border-width: 0 0 0 5px;");
        } else {
            insightMessage.setText("\u2705 You are in good academic standing.");
            insightCard.setStyle("-fx-border-color: #2ecc71; -fx-background-color: #eafaf1; -fx-border-width: 0 0 0 5px;");
        }
    }

    @FXML
    private void toggleSidebar() {
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), sidebar);
        if (!isSidebarOpen) {
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
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML 
    private void scrollToTop() {
        mainContentScroll.setVvalue(0);
        if (isSidebarOpen) toggleSidebar();
    }
}