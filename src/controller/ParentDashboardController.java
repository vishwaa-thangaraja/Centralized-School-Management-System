package controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.AcademicAlertRecord;
import model.AttendanceRecord;
import model.Student;
import model.User;
import service.AuthService;
import service.SchoolSettingsService;
import service.ThemeService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class ParentDashboardController {

    private static ParentDashboardController instance;

    @FXML private VBox sidebar;
    @FXML private Pane overlayPane;
    @FXML private ScrollPane mainContentScroll;
    @FXML private VBox dashboardContent;
    @FXML private StackPane rootStack;
    @FXML private Label brandTitleLabel;
    @FXML private Label parentNameLabel;
    @FXML private Button profilePictureButton;
    @FXML private Button schoolProfileButton;
    @FXML private Button themeToggleButton;
    @FXML private ComboBox<Student> wardSelector;
    @FXML private Label wardNameLabel;
    @FXML private Label wardClassLabel;
    @FXML private Label wardEmailLabel;
    @FXML private Label wardGenderLabel;
    @FXML private Label wardDobLabel;
    @FXML private Label wardConductLabel;
    @FXML private Label todayFnLabel;
    @FXML private Label todayAnLabel;
    @FXML private Label overallSessionsLabel;
    @FXML private Label overallPresentLabel;
    @FXML private Label overallPercentLabel;
    @FXML private Label alertCountLabel;
    @FXML private Label homeStatusLabel;
    @FXML private Button downloadPerformancePdfButton;
    @FXML private Button teacherConnectButton;
    @FXML private Label teacherConnectBadgeLabel;
    @FXML private Button counsellorConnectButton;
    @FXML private Label counsellorConnectBadgeLabel;
    @FXML private TableView<AttendanceRecord> attendanceHistoryTable;
    @FXML private TableColumn<AttendanceRecord, String> attendanceDateCol;
    @FXML private TableColumn<AttendanceRecord, String> attendanceFnCol;
    @FXML private TableColumn<AttendanceRecord, String> attendanceAnCol;
    @FXML private TableColumn<AttendanceRecord, String> attendanceRemarkCol;
    @FXML private TableView<AcademicAlertRecord> alertsTable;
    @FXML private TableColumn<AcademicAlertRecord, String> alertSubjectCol;
    @FXML private TableColumn<AcademicAlertRecord, String> alertExamCol;
    @FXML private TableColumn<AcademicAlertRecord, String> alertDateCol;
    @FXML private TableColumn<AcademicAlertRecord, String> alertMarksCol;

    private static final double SIDEBAR_WIDTH = 300;
    private boolean isSidebarOpen = false;
    private User currentParent;
    private Student selectedWard;
    private Object activeSubviewController;
    private ObservableList<AcademicAlertRecord> currentAlerts;
    private ObservableList<AttendanceRecord> currentAttendanceHistory;
    private final UserDAO userDAO = new UserDAO();
    private Timeline notificationTimeline;
    private static final int NOTIFICATION_REFRESH_SECONDS = 2;

    public ParentDashboardController() {
        instance = this;
    }

    public static ParentDashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        sidebar.setTranslateX(-SIDEBAR_WIDTH);
        overlayPane.setVisible(false);
        brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Parent"));

        attendanceDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        attendanceFnCol.setCellValueFactory(new PropertyValueFactory<>("fnStatus"));
        attendanceAnCol.setCellValueFactory(new PropertyValueFactory<>("anStatus"));
        attendanceRemarkCol.setCellValueFactory(new PropertyValueFactory<>("remark"));
        alertSubjectCol.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        alertExamCol.setCellValueFactory(new PropertyValueFactory<>("examTitle"));
        alertDateCol.setCellValueFactory(new PropertyValueFactory<>("examDate"));
        alertMarksCol.setCellValueFactory(new PropertyValueFactory<>("marksDisplay"));

        wardSelector.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getClassDisplay());
                }
            }
        });
        wardSelector.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select Ward");
                } else {
                    setText(item.getName() + " - " + item.getClassDisplay());
                }
            }
        });

        wardSelector.valueProperty().addListener((obs, oldWard, newWard) -> {
            selectedWard = newWard;
            refreshHomeView();
            refreshTeacherConnectNotification();
            refreshCounsellorConnectNotification();
            refreshActiveSubview();
        });
    }

    public void initData(User parent) {
        this.currentParent = parent;
        brandTitleLabel.setText(SchoolSettingsService.getPortalTitle("Parent"));
        ThemeService.applyCurrentTheme(rootStack);
        ThemeService.updateThemeButton(themeToggleButton);
        parentNameLabel.setText("Welcome, " + parent.getName());
        ProfileImageSupport.configureUserProfileButton(profilePictureButton, parent);
        ProfileImageSupport.configureSchoolProfileButton(schoolProfileButton, parent);
        loadParentWards();
        refreshTeacherConnectNotification();
        refreshCounsellorConnectNotification();
        startNotificationRefresh();
    }

    private void loadParentWards() {
        ObservableList<Student> wards = userDAO.getParentWards(currentParent.getUserId());
        wardSelector.setItems(wards);

        if (wards.isEmpty()) {
            selectedWard = null;
            clearHomeView();
            homeStatusLabel.setText("No wards are linked to this parent account.");
            return;
        }

        wardSelector.getSelectionModel().selectFirst();
    }

    private void refreshHomeView() {
        if (selectedWard == null) {
            clearHomeView();
            return;
        }

        wardNameLabel.setText(selectedWard.getName());
        wardClassLabel.setText(selectedWard.getClassDisplay());
        wardEmailLabel.setText(selectedWard.getEmail());
        wardGenderLabel.setText(selectedWard.getGender());
        wardDobLabel.setText(selectedWard.getDob());
        wardConductLabel.setText(selectedWard.getConduct());

        AttendanceRecord todayAttendance = userDAO.getTodayAttendanceForParent(currentParent.getUserId(), selectedWard.getStudentId());
        todayFnLabel.setText(todayAttendance.getFnStatus());
        todayAnLabel.setText(todayAttendance.getAnStatus());

        currentAttendanceHistory = userDAO.getAttendanceHistoryForParent(currentParent.getUserId(), selectedWard.getStudentId());
        attendanceHistoryTable.setItems(currentAttendanceHistory);

        int totalSessions = currentAttendanceHistory.size() * 2;
        int presentSessions = 0;
        for (AttendanceRecord record : currentAttendanceHistory) {
            String[] sessions = {record.getFnStatus(), record.getAnStatus()};
            for (String sessionStatus : sessions) {
                if (sessionStatus == null || sessionStatus.equals("-") || sessionStatus.equalsIgnoreCase("Not Marked")) {
                    totalSessions--;
                    continue;
                }
                if (sessionStatus.equalsIgnoreCase("Present")) {
                    presentSessions++;
                }
            }
        }
        totalSessions = Math.max(totalSessions, 0);
        overallSessionsLabel.setText(String.valueOf(totalSessions));
        overallPresentLabel.setText(String.valueOf(presentSessions));
        double attendancePercent = totalSessions > 0 ? (presentSessions * 100.0 / totalSessions) : 0;
        overallPercentLabel.setText(String.format("%.2f%%", attendancePercent));

        currentAlerts = userDAO.getAcademicAlertsForParent(currentParent.getUserId(), selectedWard.getStudentId());
        alertsTable.setItems(currentAlerts);
        alertCountLabel.setText(String.valueOf(currentAlerts.size()));
        homeStatusLabel.setText(currentAlerts.isEmpty()
            ? "No academic red flags for this ward. Attendance and performance are up-to-date."
            : currentAlerts.size() + " red flag(s) found below 50% threshold.");
    }

    private void clearHomeView() {
        wardNameLabel.setText("-");
        wardClassLabel.setText("-");
        wardEmailLabel.setText("-");
        wardGenderLabel.setText("-");
        wardDobLabel.setText("-");
        wardConductLabel.setText("-");
        todayFnLabel.setText("Not Marked");
        todayAnLabel.setText("Not Marked");
        overallSessionsLabel.setText("0");
        overallPresentLabel.setText("0");
        overallPercentLabel.setText("0.00%");
        alertCountLabel.setText("0");
        attendanceHistoryTable.getItems().clear();
        alertsTable.getItems().clear();
        currentAttendanceHistory = null;
        currentAlerts = null;
    }

    private void loadView(String fxmlPath) {
        try {
            stopActiveSubviewRefresh();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newContent = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ParentWardContextAware) {
                ((ParentWardContextAware) controller).updateContext(currentParent, selectedWard);
            }
            activeSubviewController = controller;
            mainContentScroll.setContent(newContent);

            if (isSidebarOpen) {
                toggleSidebar();
            }
        } catch (IOException e) {
            e.printStackTrace();
            homeStatusLabel.setText("Unable to open the selected view.");
        }
    }

    private void refreshActiveSubview() {
        if (activeSubviewController instanceof ParentWardContextAware) {
            ((ParentWardContextAware) activeSubviewController).updateContext(currentParent, selectedWard);
        }
    }

    @FXML
    private void openTeacherConnect(ActionEvent event) {
        refreshTeacherConnectNotification();
        loadView("/view/parent_teacher_connect.fxml");
    }

    @FXML
    private void openCounsellorConnect(ActionEvent event) {
        refreshCounsellorConnectNotification();
        loadView("/view/parent_counsellor_connect_view.fxml");
    }

    @FXML
    private void openCounsellingRequest(ActionEvent event) {
        loadView("/view/counselling_request_view.fxml");
    }

    public void refreshTeacherConnectNotification() {
        if (teacherConnectButton == null || teacherConnectBadgeLabel == null) {
            return;
        }
        teacherConnectButton.setText("Teacher Connect");
        if (currentParent == null || selectedWard == null) {
            teacherConnectBadgeLabel.setVisible(false);
            teacherConnectBadgeLabel.setManaged(false);
            return;
        }
        int unreadCount = userDAO.getParentUnreadMessageCount(currentParent.getUserId(), selectedWard.getStudentId());
        teacherConnectBadgeLabel.setText(String.valueOf(unreadCount));
        teacherConnectBadgeLabel.setVisible(unreadCount > 0);
        teacherConnectBadgeLabel.setManaged(unreadCount > 0);
    }

    public void refreshCounsellorConnectNotification() {
        if (counsellorConnectButton == null || counsellorConnectBadgeLabel == null) {
            return;
        }
        counsellorConnectButton.setText("Counsellor Connect");
        if (currentParent == null || selectedWard == null) {
            counsellorConnectBadgeLabel.setVisible(false);
            counsellorConnectBadgeLabel.setManaged(false);
            return;
        }
        int unreadCount = userDAO.getParentCounsellorUnreadMessageCount(currentParent.getUserId(), selectedWard.getStudentId());
        counsellorConnectBadgeLabel.setText(String.valueOf(unreadCount));
        counsellorConnectBadgeLabel.setVisible(unreadCount > 0);
        counsellorConnectBadgeLabel.setManaged(unreadCount > 0);
    }

    private void refreshMessageNotifications() {
        refreshTeacherConnectNotification();
        refreshCounsellorConnectNotification();
    }

    private void startNotificationRefresh() {
        stopNotificationRefresh();
        notificationTimeline = new Timeline(new KeyFrame(Duration.seconds(NOTIFICATION_REFRESH_SECONDS), event -> refreshMessageNotifications()));
        notificationTimeline.setCycleCount(Timeline.INDEFINITE);
        notificationTimeline.play();
    }

    private void stopNotificationRefresh() {
        if (notificationTimeline != null) {
            notificationTimeline.stop();
            notificationTimeline = null;
        }
    }

    private void stopActiveSubviewRefresh() {
        if (activeSubviewController instanceof LiveRefreshController) {
            ((LiveRefreshController) activeSubviewController).stopLiveRefresh();
        }
    }

    @FXML
    private void handleDownloadPerformancePdf() {
        if (currentParent == null || selectedWard == null) {
            homeStatusLabel.setText("Select a ward before downloading performance PDF.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Parent Performance PDF");
        fileChooser.setInitialFileName("Parent_Performance_" + selectedWard.getName().replaceAll("[^a-zA-Z0-9-_]", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(mainContentScroll.getScene().getWindow());

        if (file == null) {
            return;
        }

        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph(SchoolSettingsService.getSchoolName()).setFontSize(18));
            for (String line : SchoolSettingsService.getContactLines()) {
                document.add(new Paragraph(line).setFontSize(9));
            }
            document.add(new Paragraph("Parent Ward Performance Report").setFontSize(13));
            document.add(new Paragraph("Generated by Parent: " + currentParent.getName()));
            document.add(new Paragraph("Generated on: " + new Date()));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Ward Name: " + selectedWard.getName()));
            document.add(new Paragraph("Class: " + selectedWard.getClassDisplay()));
            document.add(new Paragraph("Today's FN: " + todayFnLabel.getText() + " | Today's AN: " + todayAnLabel.getText()));
            document.add(new Paragraph("Overall Attendance: " + overallPresentLabel.getText() + "/" + overallSessionsLabel.getText() + " (" + overallPercentLabel.getText() + ")"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Academic Alerts (Below 50%)"));

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2, 2}));
            table.useAllAvailableWidth();
            table.addHeaderCell("Subject");
            table.addHeaderCell("Exam");
            table.addHeaderCell("Date");
            table.addHeaderCell("Marks");

            if (currentAlerts == null || currentAlerts.isEmpty()) {
                table.addCell("No red flag alerts");
                table.addCell("-");
                table.addCell("-");
                table.addCell("-");
            } else {
                for (AcademicAlertRecord alert : currentAlerts) {
                    table.addCell(alert.getSubjectName());
                    table.addCell(alert.getExamTitle());
                    table.addCell(alert.getExamDate());
                    table.addCell(alert.getMarksDisplay());
                }
            }

            document.add(table);
            document.close();
            homeStatusLabel.setText("Performance PDF generated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            homeStatusLabel.setText("Failed to generate performance PDF.");
        }
    }

    @FXML
    public void scrollToTop() {
        stopActiveSubviewRefresh();
        activeSubviewController = null;
        mainContentScroll.setContent(dashboardContent);
        mainContentScroll.setVvalue(0);
        if (currentParent != null) {
            ProfileImageSupport.refreshUserProfileButton(profilePictureButton, currentParent);
            ProfileImageSupport.refreshSchoolProfileButton(schoolProfileButton);
        }
        ThemeService.applyCurrentTheme(rootStack);
        ThemeService.updateThemeButton(themeToggleButton);
        refreshHomeView();
        refreshTeacherConnectNotification();
        refreshCounsellorConnectNotification();
        if (isSidebarOpen) {
            toggleSidebar();
        }
    }

    @FXML
    private void handleToggleTheme() {
        if (ThemeService.saveTheme(ThemeService.toggleTheme())) {
            ThemeService.applyCurrentTheme(rootStack);
            ThemeService.updateThemeButton(themeToggleButton);
        }
    }

    @FXML
    public void toggleSidebar() {
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), sidebar);
        if (!isSidebarOpen) {
            refreshTeacherConnectNotification();
            refreshCounsellorConnectNotification();
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
            stopNotificationRefresh();
            stopActiveSubviewRefresh();
            AuthService.clearCurrentUser();
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            SchoolSettingsService.applyStageTitle(stage);
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
