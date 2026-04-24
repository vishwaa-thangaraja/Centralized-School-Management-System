package controller;

import dao.UserDAO;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
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

import java.io.IOException;

public class StudentDashboardController {

    private static StudentDashboardController instance;

    @FXML private VBox sidebar;
    @FXML private Pane overlayPane;
    @FXML private ScrollPane mainContentScroll;
    @FXML private VBox dashboardContent;
    @FXML private StackPane rootStack;
    @FXML private Label userNameLabel;
    @FXML private Label attendanceVal;
    @FXML private Label marksVal;
    @FXML private Label pendingTasks;
    @FXML private Label assignmentBadgeLabel;
    @FXML private Label insightMessage;
    @FXML private VBox insightCard;
    @FXML private LineChart<String, Number> performanceChart;

    private boolean isSidebarOpen = false;
    private static final double SIDEBAR_WIDTH = 300;
    private User currentUser;

    public StudentDashboardController() {
        instance = this;
    }

    public static StudentDashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        sidebar.setTranslateX(-SIDEBAR_WIDTH);
        overlayPane.setVisible(false);
    }

    public void initData(User user) {
        this.currentUser = user;
        userNameLabel.setText("Welcome, " + user.getName());
        refreshDashboardStats();
    }

    public void refreshDashboardStats() {
        UserDAO dao = new UserDAO();
        double att = dao.getAttendancePercentage(currentUser.getUserId());
        double avg = dao.getAverageMarks(currentUser.getUserId());
        int pending = dao.getPendingAssignments(currentUser.getUserId());

        attendanceVal.setText(String.format("%.1f%%", att));
        marksVal.setText(String.format("%.1f/100", avg));
        pendingTasks.setText(pending + " Pending");
        assignmentBadgeLabel.setText(String.valueOf(pending));
        assignmentBadgeLabel.setVisible(pending > 0);
        assignmentBadgeLabel.setManaged(pending > 0);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Marks Trend");
        series.getData().add(new XYChart.Data<>("Midterm Math", 85));
        series.getData().add(new XYChart.Data<>("Midterm CS", 92));
        performanceChart.getData().clear();
        performanceChart.getData().add(series);

        if (att < 75) {
            insightMessage.setText("Critical: Your attendance is low (" + String.format("%.1f%%", att) + ")");
            insightCard.setStyle("-fx-border-color: #e74c3c; -fx-background-color: #fdedec; -fx-border-width: 0 0 0 5px;");
        } else {
            insightMessage.setText("You are in good academic standing.");
            insightCard.setStyle("-fx-border-color: #2ecc71; -fx-background-color: #eafaf1; -fx-border-width: 0 0 0 5px;");
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AttendanceController) {
                ((AttendanceController) controller).initData(currentUser);
            } else if (controller instanceof StudentAssignmentsController) {
                ((StudentAssignmentsController) controller).initData(currentUser);
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
    private void openAttendanceView(ActionEvent event) {
        loadView("/view/attendance_view.fxml");
    }

    @FXML
    private void openPerformanceView(ActionEvent event) {
        loadView("/view/performance_view.fxml");
    }

    @FXML
    private void openAssignmentsView(ActionEvent event) {
        loadView("/view/student_assignments_view.fxml");
    }

    @FXML
    public void toggleSidebar() {
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
    public void scrollToTop() {
        mainContentScroll.setContent(dashboardContent);
        mainContentScroll.setVvalue(0);
        refreshDashboardStats();
        if (isSidebarOpen) {
            toggleSidebar();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            AuthService.clearCurrentUser();
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
