package controller;

import dao.UserDAO;
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

import java.io.IOException;
import java.util.Map;

public class TeacherDashboardController {

    private static TeacherDashboardController instance;

    @FXML private VBox sidebar;
    @FXML private Pane overlayPane;
    @FXML private ScrollPane mainContentScroll;
    @FXML private VBox dashboardContent;
    @FXML private StackPane rootStack;
    @FXML private Label teacherNameLabel;
    @FXML private Label teacherMetaLabel;
    @FXML private Label studentCountLabel;
    @FXML private Label classCountLabel;
    @FXML private Label assignmentCountLabel;
    @FXML private Label teacherInsightLabel;

    private boolean isSidebarOpen = false;
    private static final double SIDEBAR_WIDTH = 300;
    private User currentUser;

    public TeacherDashboardController() {
        instance = this;
    }

    public static TeacherDashboardController getInstance() {
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
        teacherNameLabel.setText("Welcome, " + user.getName());
        refreshDashboard();
    }

    public void refreshDashboard() {
        if (currentUser == null) {
            return;
        }

        UserDAO dao = new UserDAO();
        Map<String, String> profile = dao.getTeacherProfile(currentUser.getUserId());
        int studentCount = dao.getTeacherStudentCount(currentUser.getUserId());
        int classCount = dao.getTeacherClassCount(currentUser.getUserId());
        int assignmentCount = dao.getTeacherAssignmentCount(currentUser.getUserId());

        String qualification = profile.getOrDefault("qualification", "Not Provided");
        String experience = profile.getOrDefault("experience", "0");

        teacherMetaLabel.setText("Qualification: " + qualification + " | Experience: " + experience + " years");
        studentCountLabel.setText(String.valueOf(studentCount));
        classCountLabel.setText(String.valueOf(classCount));
        assignmentCountLabel.setText(String.valueOf(assignmentCount));

        if (studentCount == 0) {
            teacherInsightLabel.setText("No students are available under your assigned sections yet. Once students are mapped, you can manage profiles, performance, and assignments here.");
        } else {
            teacherInsightLabel.setText("You currently oversee " + studentCount + " student(s) from " + classCount + " assigned section(s). Use the modules below to manage profiles, performance, and assignments.");
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();

            Object controller = loader.getController();
            if (controller instanceof TeacherStudentManagementController) {
                ((TeacherStudentManagementController) controller).initData(currentUser);
            } else if (controller instanceof TeacherStudentPerformanceController) {
                ((TeacherStudentPerformanceController) controller).initData(currentUser);
            } else if (controller instanceof TeacherAssignmentsController) {
                ((TeacherAssignmentsController) controller).initData(currentUser);
            } else if (controller instanceof TeacherAttendanceController) {
                ((TeacherAttendanceController) controller).initData(currentUser);
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
    public void openStudentManagement(ActionEvent event) {
        loadView("/view/teacher_students_view.fxml");
    }

    @FXML
    public void openStudentPerformance(ActionEvent event) {
        loadView("/view/teacher_performance_view.fxml");
    }

    @FXML
    public void openAssignmentsView(ActionEvent event) {
        loadView("/view/teacher_assignments_view.fxml");
    }

    @FXML
    public void openAttendanceView(ActionEvent event) {
        loadView("/view/teacher_attendance_view.fxml");
    }

    @FXML
    public void scrollToTop() {
        mainContentScroll.setContent(dashboardContent);
        mainContentScroll.setVvalue(0);
        refreshDashboard();
        if (isSidebarOpen) {
            toggleSidebar();
        }
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
