package controller;

import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import model.AssignmentRecord;
import model.User;
import service.AssignmentFileService;

import java.io.File;
import java.io.IOException;

public class StudentAssignmentsController {

    @FXML private Label statusLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private Label assignmentTitleLabel;
    @FXML private Label subjectLabel;
    @FXML private Label classLabel;
    @FXML private Label dueDateLabel;
    @FXML private Label submissionStatusLabel;
    @FXML private Label descriptionLabel;
    @FXML private TableView<AssignmentRecord> assignmentsTable;
    @FXML private TableColumn<AssignmentRecord, String> titleCol;
    @FXML private TableColumn<AssignmentRecord, String> subjectCol;
    @FXML private TableColumn<AssignmentRecord, String> dueDateCol;
    @FXML private TableColumn<AssignmentRecord, String> statusCol;
    @FXML private Button viewAssignmentButton;
    @FXML private Button uploadSolutionButton;
    @FXML private Button viewSubmissionButton;

    private final UserDAO userDAO = new UserDAO();
    private User currentUser;
    private AssignmentFileService fileService;

    @FXML
    public void initialize() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        assignmentsTable.setPlaceholder(new Label("No pending assignments"));

        assignmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, assignment) -> {
            populateAssignmentDetails(assignment);
        });

        try {
            fileService = new AssignmentFileService();
        } catch (IOException e) {
            showError("Storage Error", e.getMessage());
        }

        clearAssignmentDetails();
        statusLabel.setText("Select an assignment to view the PDF or upload your answer.");
    }

    public void initData(User user) {
        this.currentUser = user;
        loadAssignments();
    }

    private void loadAssignments() {
        ObservableList<AssignmentRecord> allAssignments = userDAO.getAssignmentsForStudent(currentUser.getUserId());
        ObservableList<AssignmentRecord> pendingAssignments = FXCollections.observableArrayList();

        for (AssignmentRecord assignment : allAssignments) {
            if ("Pending".equalsIgnoreCase(assignment.getStatus())) {
                pendingAssignments.add(assignment);
            }
        }

        assignmentsTable.setItems(pendingAssignments);
        assignmentsTable.getSelectionModel().clearSelection();
        clearAssignmentDetails();

        long pendingCount = pendingAssignments.size();
        long completedCount = allAssignments.size() - pendingCount;
        pendingCountLabel.setText(String.valueOf(pendingCount));
        completedCountLabel.setText(String.valueOf(completedCount));
        statusLabel.setText(pendingAssignments.isEmpty()
            ? "No pending assignments"
            : "Select a pending assignment to view the PDF or upload your answer.");
    }

    private void populateAssignmentDetails(AssignmentRecord assignment) {
        if (assignment == null) {
            clearAssignmentDetails();
            return;
        }

        assignmentTitleLabel.setText(assignment.getTitle());
        subjectLabel.setText(assignment.getSubjectName());
        classLabel.setText(assignment.getClassDisplay());
        dueDateLabel.setText(assignment.getDueDate());
        submissionStatusLabel.setText(assignment.getStatus());
        descriptionLabel.setText(assignment.getDescription());
        viewAssignmentButton.setDisable(fileService == null || !fileService.hasAssignmentPdf(assignment.getAssignmentId()));

        int studentId = userDAO.getStudentIdByUserId(currentUser.getUserId());
        viewSubmissionButton.setDisable(fileService == null || studentId <= 0 || !fileService.hasSubmissionPdf(assignment.getAssignmentId(), studentId));
        uploadSolutionButton.setDisable(false);
    }

    private void clearAssignmentDetails() {
        assignmentTitleLabel.setText("No assignment selected");
        subjectLabel.setText("-");
        classLabel.setText("-");
        dueDateLabel.setText("-");
        submissionStatusLabel.setText("-");
        descriptionLabel.setText("-");
        viewAssignmentButton.setDisable(true);
        uploadSolutionButton.setDisable(true);
        viewSubmissionButton.setDisable(true);
    }

    @FXML
    private void handleViewAssignmentPdf() {
        AssignmentRecord assignment = assignmentsTable.getSelectionModel().getSelectedItem();
        if (assignment == null || fileService == null) {
            statusLabel.setText("Select a pending assignment first.");
            return;
        }

        try {
            fileService.openPdf(fileService.getAssignmentPdf(assignment.getAssignmentId()));
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Unable to open the assignment PDF.");
            showError("Open PDF Error", e.getMessage());
        }
    }

    @FXML
    private void handleUploadSolutionPdf() {
        AssignmentRecord assignment = assignmentsTable.getSelectionModel().getSelectedItem();
        if (assignment == null || fileService == null) {
            statusLabel.setText("Select a pending assignment first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Solution PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(assignmentsTable.getScene().getWindow());

        if (file == null) {
            return;
        }

        int studentId = userDAO.getStudentIdByUserId(currentUser.getUserId());
        int submissionId = userDAO.getNextSubmissionId();
        if (studentId <= 0 || submissionId <= 0) {
            statusLabel.setText("Unable to prepare your submission.");
            return;
        }

        try {
            fileService.saveSubmissionPdf(file, assignment.getAssignmentId(), studentId);
            boolean submitted = userDAO.submitAssignment(currentUser.getUserId(), assignment.getAssignmentId(), submissionId);
            if (!submitted) {
                statusLabel.setText("Assignment submission failed.");
                return;
            }

            loadAssignments();
            if (StudentDashboardController.getInstance() != null) {
                StudentDashboardController.getInstance().refreshDashboardStats();
            }
            statusLabel.setText(assignmentsTable.getItems().isEmpty() ? "No pending assignments" : "Solution uploaded successfully.");
            showInfo("Submission Complete", "Your solution PDF was uploaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to upload your solution PDF.");
            showError("Upload Error", e.getMessage());
        }
    }

    @FXML
    private void handleViewSubmissionPdf() {
        AssignmentRecord assignment = assignmentsTable.getSelectionModel().getSelectedItem();
        if (assignment == null || fileService == null) {
            statusLabel.setText("Select a pending assignment first.");
            return;
        }

        int studentId = userDAO.getStudentIdByUserId(currentUser.getUserId());
        if (studentId <= 0) {
            statusLabel.setText("Student record not found.");
            return;
        }

        try {
            fileService.openPdf(fileService.getSubmissionPdf(assignment.getAssignmentId(), studentId));
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Unable to open your uploaded solution PDF.");
            showError("Open PDF Error", e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadAssignments();
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().refreshDashboardStats();
        }
    }

    @FXML
    private void backToDashboard() {
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().scrollToTop();
        }
    }

    private void showInfo(String title, String message) {
        DialogSupport.info(assignmentsTable, title, message);
    }

    private void showError(String title, String message) {
        DialogSupport.error(assignmentsTable, title, message);
    }
}
