package controller;

import dao.UserDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
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
import model.ExamRecord;
import model.User;
import service.AuthService;
import service.ExamService;
import service.SchoolSettingsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentDashboardController {

    private static StudentDashboardController instance;

    @FXML private VBox sidebar;
    @FXML private Pane overlayPane;
    @FXML private ScrollPane mainContentScroll;
    @FXML private VBox dashboardContent;
    @FXML private StackPane rootStack;
    @FXML private Label brandTitleLabel;
    @FXML private Label userNameLabel;
    @FXML private Label attendanceVal;
    @FXML private Label marksVal;
    @FXML private Label pendingTasks;
    @FXML private Label assignmentBadgeLabel;
    @FXML private Label counsellorBadgeLabel;
    @FXML private Label insightMessage;
    @FXML private VBox insightCard;
    @FXML private Label studentProfileNameLabel;
    @FXML private Label studentEmailLabel;
    @FXML private Label studentGenderLabel;
    @FXML private Label studentDobLabel;
    @FXML private Label studentIdLabel;
    @FXML private Label studentClassLabel;
    @FXML private LineChart<String, Number> performanceChart;
    @FXML private Label upcomingExamTitleLabel;
    @FXML private Label upcomingExamMetaLabel;
    @FXML private Label upcomingExamCounterLabel;

    private boolean isSidebarOpen = false;
    private static final double SIDEBAR_WIDTH = 300;
    private User currentUser;
    private final ExamService examService = new ExamService();
    private final List<ExamRecord> upcomingExamNotifications = new ArrayList<>();
    private Timeline upcomingExamTimeline;
    private int upcomingExamIndex = 0;

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
        brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Student"));
    }

    public void initData(User user) {
        this.currentUser = user;
        brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Student"));
        userNameLabel.setText("Welcome, " + user.getName());
        refreshDashboardStats();
    }

    public void refreshDashboardStats() {
        UserDAO dao = new UserDAO();
        double att = dao.getAttendancePercentage(currentUser.getUserId());
        double avg = dao.getAverageMarks(currentUser.getUserId());
        int pending = dao.getPendingAssignments(currentUser.getUserId());
        Map<String, String> profile = dao.getStudentProfile(currentUser.getUserId());

        studentProfileNameLabel.setText(currentUser.getName());
        studentEmailLabel.setText(currentUser.getEmail());
        studentGenderLabel.setText(profile.getOrDefault("gender", "-"));
        studentDobLabel.setText(profile.getOrDefault("dob", "-"));
        studentIdLabel.setText(profile.getOrDefault("student_id", "-"));
        studentClassLabel.setText(profile.getOrDefault("class_display", "N/A"));
        attendanceVal.setText(String.format("%.1f%%", att));
        marksVal.setText(String.format("%.1f/100", avg));
        pendingTasks.setText(pending + " Pending");
        assignmentBadgeLabel.setText(String.valueOf(pending));
        assignmentBadgeLabel.setVisible(pending > 0);
        assignmentBadgeLabel.setManaged(pending > 0);

        int studentId = -1;
        try {
            studentId = Integer.parseInt(profile.getOrDefault("student_id", "-1"));
        } catch (NumberFormatException ignored) {
        }
        int counsellorUnread = studentId > 0 ? dao.getStudentCounsellorUnreadMessageCount(currentUser.getUserId(), studentId) : 0;
        counsellorBadgeLabel.setText(String.valueOf(counsellorUnread));
        counsellorBadgeLabel.setVisible(counsellorUnread > 0);
        counsellorBadgeLabel.setManaged(counsellorUnread > 0);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Marks Trend");
        series.getData().add(new XYChart.Data<>("Midterm Math", 85));
        series.getData().add(new XYChart.Data<>("Midterm CS", 92));
        performanceChart.getData().clear();
        performanceChart.getData().add(series);

        if (att < 75) {
            insightMessage.setText("Critical: Your attendance is low (" + String.format("%.1f%%", att) + ")");
            insightCard.setStyle("-fx-border-color: #e74c3c; -fx-background-color: #ffffff; -fx-border-width: 0 0 0 5px;");
        } else {
            insightMessage.setText("You are in good academic standing.");
            insightCard.setStyle("-fx-border-color: #2ecc71; -fx-background-color: #ffffff; -fx-border-width: 0 0 0 5px;");
        }
        refreshUpcomingExamNotifications(studentId);
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
            } else if (controller instanceof StudentQuestionBankController) {
                ((StudentQuestionBankController) controller).initData(currentUser);
            } else if (controller instanceof StudentCounsellorConnectController) {
                ((StudentCounsellorConnectController) controller).initData(currentUser);
            } else if (controller instanceof CounsellingRequestController) {
                ((CounsellingRequestController) controller).initForStudent(currentUser);
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
    private void openQuestionBankView(ActionEvent event) {
        loadView("/view/student_question_bank_view.fxml");
    }

    @FXML
    private void openCounsellorConnectView(ActionEvent event) {
        loadView("/view/student_counsellor_connect_view.fxml");
    }

    @FXML
    private void openCounsellingRequestView(ActionEvent event) {
        loadView("/view/counselling_request_view.fxml");
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

    private void refreshUpcomingExamNotifications(int studentId) {
        if (upcomingExamTitleLabel == null) {
            return;
        }

        upcomingExamNotifications.clear();
        if (studentId > 0) {
            ObservableList<ExamRecord> exams = examService.getStudentExamRecords(studentId);
            for (ExamRecord exam : exams) {
                if ("Upcoming".equalsIgnoreCase(exam.getStatus())) {
                    upcomingExamNotifications.add(exam);
                }
            }
        }

        upcomingExamIndex = 0;
        if (upcomingExamNotifications.isEmpty()) {
            stopUpcomingExamTimeline();
            upcomingExamTitleLabel.setText("No upcoming exams");
            upcomingExamMetaLabel.setText("-");
            upcomingExamCounterLabel.setText("0 / 0");
            return;
        }

        showUpcomingExam(0);
        if (upcomingExamNotifications.size() > 1) {
            stopUpcomingExamTimeline();
            upcomingExamTimeline = new Timeline(new KeyFrame(Duration.seconds(4), event -> showNextUpcomingExam()));
            upcomingExamTimeline.setCycleCount(Timeline.INDEFINITE);
            upcomingExamTimeline.play();
        } else {
            stopUpcomingExamTimeline();
        }
    }

    private void showNextUpcomingExam() {
        if (upcomingExamNotifications.isEmpty()) {
            return;
        }
        upcomingExamIndex = (upcomingExamIndex + 1) % upcomingExamNotifications.size();
        showUpcomingExam(upcomingExamIndex);
    }

    private void showUpcomingExam(int index) {
        ExamRecord exam = upcomingExamNotifications.get(index);
        upcomingExamTitleLabel.setText(exam.getExamTitle());
        upcomingExamMetaLabel.setText(exam.getClassDisplay() + " | " + exam.getSubjectName() + " | " + exam.getExamDate());
        upcomingExamCounterLabel.setText((index + 1) + " / " + upcomingExamNotifications.size());
    }

    private void stopUpcomingExamTimeline() {
        if (upcomingExamTimeline != null) {
            upcomingExamTimeline.stop();
            upcomingExamTimeline = null;
        }
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
            SchoolSettingsService.applyStageTitle(stage);
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
