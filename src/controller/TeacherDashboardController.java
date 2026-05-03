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
import javafx.scene.control.Button;
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

public class TeacherDashboardController {

    private static TeacherDashboardController instance;

    @FXML private VBox sidebar;
    @FXML private Pane overlayPane;
    @FXML private ScrollPane mainContentScroll;
    @FXML private VBox dashboardContent;
    @FXML private StackPane rootStack;
    @FXML private Label brandTitleLabel;
    @FXML private Label teacherNameLabel;
    @FXML private Label studentCountLabel;
    @FXML private Label classCountLabel;
    @FXML private Label assignmentCountLabel;
    @FXML private Label teacherProfileNameLabel;
    @FXML private Label teacherEmailLabel;
    @FXML private Label teacherQualificationLabel;
    @FXML private Label teacherExperienceLabel;
    @FXML private Label teacherScopeLabel;
    @FXML private Button teacherConnectInboxButton;
    @FXML private Label teacherConnectInboxBadgeLabel;
    @FXML private Label upcomingExamTitleLabel;
    @FXML private Label upcomingExamMetaLabel;
    @FXML private Label upcomingExamCounterLabel;

    private boolean isSidebarOpen = false;
    private static final double SIDEBAR_WIDTH = 300;
    private User currentUser;
    private final UserDAO userDAO = new UserDAO();
    private final ExamService examService = new ExamService();
    private final List<ExamRecord> upcomingExamNotifications = new ArrayList<>();
    private Timeline upcomingExamTimeline;
    private int upcomingExamIndex = 0;

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
        brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Teacher"));
    }

    public void initData(User user) {
        this.currentUser = user;
        brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Teacher"));
        teacherNameLabel.setText("Welcome, " + user.getName());
        refreshDashboard();
        refreshTeacherChatNotification();
    }

    public void refreshDashboard() {
        if (currentUser == null) {
            return;
        }

        Map<String, String> profile = userDAO.getTeacherProfile(currentUser.getUserId());
        int studentCount = userDAO.getTeacherStudentCount(currentUser.getUserId());
        int classCount = userDAO.getTeacherClassCount(currentUser.getUserId());
        int assignmentCount = userDAO.getTeacherAssignmentCount(currentUser.getUserId());

        String qualification = profile.getOrDefault("qualification", "Not Provided");
        String experience = profile.getOrDefault("experience", "0");

        teacherProfileNameLabel.setText(currentUser.getName());
        teacherEmailLabel.setText(currentUser.getEmail());
        teacherQualificationLabel.setText(qualification);
        teacherExperienceLabel.setText(experience + " years");
        teacherScopeLabel.setText(studentCount + " student(s) from " + classCount + " assigned class(s)");
        studentCountLabel.setText(String.valueOf(studentCount));
        classCountLabel.setText(String.valueOf(classCount));
        assignmentCountLabel.setText(String.valueOf(assignmentCount));
        refreshUpcomingExamNotifications();
        refreshTeacherChatNotification();
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
            } else if (controller instanceof TeacherQuestionBankController) {
                ((TeacherQuestionBankController) controller).initData(currentUser);
            } else if (controller instanceof TeacherConnectController) {
                ((TeacherConnectController) controller).initData(currentUser);
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
    public void openQuestionBankView(ActionEvent event) {
        loadView("/view/teacher_question_bank_view.fxml");
    }

    @FXML
    public void openAttendanceView(ActionEvent event) {
        loadView("/view/teacher_attendance_view.fxml");
    }

    @FXML
    public void openTeacherConnectInbox(ActionEvent event) {
        loadView("/view/teacher_connect_view.fxml");
    }

    public void refreshTeacherChatNotification() {
        if (teacherConnectInboxButton == null || teacherConnectInboxBadgeLabel == null) {
            return;
        }
        teacherConnectInboxButton.setText("Parent Chat");
        if (currentUser == null) {
            teacherConnectInboxBadgeLabel.setVisible(false);
            teacherConnectInboxBadgeLabel.setManaged(false);
            return;
        }
        int unreadCount = userDAO.getTeacherUnreadMessageCount(currentUser.getUserId());
        teacherConnectInboxBadgeLabel.setText(String.valueOf(unreadCount));
        teacherConnectInboxBadgeLabel.setVisible(unreadCount > 0);
        teacherConnectInboxBadgeLabel.setManaged(unreadCount > 0);
    }

    @FXML
    public void scrollToTop() {
        mainContentScroll.setContent(dashboardContent);
        mainContentScroll.setVvalue(0);
        refreshDashboard();
        refreshTeacherChatNotification();
        if (isSidebarOpen) {
            toggleSidebar();
        }
    }

    @FXML
    public void toggleSidebar() {
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), sidebar);
        if (!isSidebarOpen) {
            refreshTeacherChatNotification();
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

    private void refreshUpcomingExamNotifications() {
        if (upcomingExamTitleLabel == null || currentUser == null) {
            return;
        }

        upcomingExamNotifications.clear();
        int teacherId = userDAO.getTeacherIdByUserId(currentUser.getUserId());
        if (teacherId > 0) {
            ObservableList<ExamRecord> exams = examService.getTeacherExamRecords(teacherId);
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
