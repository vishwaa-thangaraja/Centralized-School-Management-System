package controller;

import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import model.AssignmentRecord;
import model.User;
import service.AssignmentFileService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class TeacherAssignmentsController {

    @FXML private Label teacherInfoLabel;
    @FXML private Label statusLabel;
    @FXML private Label assignmentCountLabel;
    @FXML private Label selectedPdfLabel;
    @FXML private Label selectedAssignmentLabel;
    @FXML private Label submittedCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label completedStatusLabel;
    @FXML private TableView<AssignmentRecord> assignmentsTable;
    @FXML private TableColumn<AssignmentRecord, String> titleCol;
    @FXML private TableColumn<AssignmentRecord, String> classCol;
    @FXML private TableColumn<AssignmentRecord, String> dueDateCol;
    @FXML private TableColumn<AssignmentRecord, Number> submittedCol;
    @FXML private TableColumn<AssignmentRecord, Number> pendingCol;
    @FXML private TableView<AssignmentRecord> completedAssignmentsTable;
    @FXML private TableColumn<AssignmentRecord, String> completedTitleCol;
    @FXML private TableColumn<AssignmentRecord, String> completedClassCol;
    @FXML private TableColumn<AssignmentRecord, String> completedDueDateCol;
    @FXML private TableColumn<AssignmentRecord, Number> completedSubmittedCol;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private TextField titleField;
    @FXML private TextField dueDateField;
    @FXML private TextArea descriptionArea;
    @FXML private Button publishButton;
    @FXML private Button viewPdfButton;
    @FXML private Button downloadSelectedCompletedButton;
    @FXML private Button downloadAllCompletedButton;

    private final UserDAO userDAO = new UserDAO();
    private final Map<String, Integer> classDisplayToId = new LinkedHashMap<>();
    private final Map<String, Integer> subjectDisplayToId = new LinkedHashMap<>();
    private User currentTeacher;
    private File selectedPdfFile;
    private AssignmentFileService fileService;

    @FXML
    public void initialize() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        classCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        submittedCol.setCellValueFactory(new PropertyValueFactory<>("submittedCount"));
        pendingCol.setCellValueFactory(new PropertyValueFactory<>("pendingCount"));
        assignmentsTable.setPlaceholder(new Label("No assignments waiting for submission"));

        completedTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        completedClassCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));
        completedDueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        completedSubmittedCol.setCellValueFactory(new PropertyValueFactory<>("submittedCount"));
        completedAssignmentsTable.setPlaceholder(new Label("No assignments are fully submitted yet."));

        classCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadSubjectsForSelectedClass());
        assignmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, assignment) -> populateAssignmentSummary(assignment));
        completedAssignmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, assignment) -> updateCompletedDownloadState());

        publishButton.setDisable(false);
        viewPdfButton.setDisable(true);
        downloadSelectedCompletedButton.setDisable(true);
        downloadAllCompletedButton.setDisable(true);
        selectedPdfLabel.setText("No PDF selected");
        selectedAssignmentLabel.setText("No assignment selected");
        submittedCountLabel.setText("-");
        pendingCountLabel.setText("-");
        completedStatusLabel.setText("No assignments are fully submitted yet.");
        statusLabel.setText("Choose a PDF and publish an assignment for one of your assigned section-subject pairs.");

        try {
            fileService = new AssignmentFileService();
        } catch (IOException e) {
            showError("Storage Error", e.getMessage());
        }
    }

    public void initData(User teacher) {
        this.currentTeacher = teacher;
        teacherInfoLabel.setText("Teacher: " + teacher.getName() + " | Upload assignment PDFs for your assigned section-subject pairs.");
        loadTeacherClasses();
        loadAssignments();
    }

    private void loadTeacherClasses() {
        classDisplayToId.clear();
        classCombo.getItems().clear();
        subjectDisplayToId.clear();
        subjectCombo.getItems().clear();

        LinkedHashMap<Integer, String> classes = userDAO.getTeacherClasses(currentTeacher.getUserId());
        for (Map.Entry<Integer, String> entry : classes.entrySet()) {
            classDisplayToId.put(entry.getValue(), entry.getKey());
            classCombo.getItems().add(entry.getValue());
        }

        if (classCombo.getItems().isEmpty()) {
            statusLabel.setText("No assigned sections are available for this teacher yet. Add CLASS_SUBJECT_TEACHER mappings first.");
            return;
        }

        classCombo.setValue(classCombo.getItems().get(0));
        loadSubjectsForSelectedClass();
    }

    private void loadSubjectsForSelectedClass() {
        subjectDisplayToId.clear();
        subjectCombo.getItems().clear();

        String classDisplay = classCombo.getValue();
        if (classDisplay == null) {
            return;
        }

        Integer classId = classDisplayToId.get(classDisplay);
        if (classId == null) {
            return;
        }

        LinkedHashMap<Integer, String> subjects = userDAO.getTeacherSubjectsForClass(currentTeacher.getUserId(), classId);
        for (Map.Entry<Integer, String> entry : subjects.entrySet()) {
            subjectDisplayToId.put(entry.getValue(), entry.getKey());
            subjectCombo.getItems().add(entry.getValue());
        }

        if (!subjectCombo.getItems().isEmpty()) {
            subjectCombo.setValue(subjectCombo.getItems().get(0));
        }
    }

    private void loadAssignments() {
        ObservableList<AssignmentRecord> allAssignments = userDAO.getAssignmentsForTeacher(currentTeacher.getUserId());
        ObservableList<AssignmentRecord> activeAssignments = FXCollections.observableArrayList();
        ObservableList<AssignmentRecord> completedAssignments = FXCollections.observableArrayList();

        for (AssignmentRecord assignment : allAssignments) {
            if (assignment.getPendingCount() > 0) {
                activeAssignments.add(assignment);
            } else {
                completedAssignments.add(assignment);
            }
        }

        assignmentsTable.setItems(activeAssignments);
        completedAssignmentsTable.setItems(completedAssignments);
        assignmentCountLabel.setText(String.valueOf(allAssignments.size()));
        assignmentsTable.getSelectionModel().clearSelection();
        completedAssignmentsTable.getSelectionModel().clearSelection();
        populateAssignmentSummary(null);
        updateCompletedDownloadState();

        completedStatusLabel.setText(completedAssignments.isEmpty()
            ? "No assignments are fully submitted yet."
            : "These assignments have been submitted by all assigned students.");
    }

    private void populateAssignmentSummary(AssignmentRecord assignment) {
        if (assignment == null) {
            selectedAssignmentLabel.setText("No assignment selected");
            submittedCountLabel.setText("-");
            pendingCountLabel.setText("-");
            viewPdfButton.setDisable(true);
            return;
        }

        selectedAssignmentLabel.setText(assignment.getTitle() + " | " + assignment.getClassDisplay());
        submittedCountLabel.setText(String.valueOf(assignment.getSubmittedCount()));
        pendingCountLabel.setText(String.valueOf(assignment.getPendingCount()));
        viewPdfButton.setDisable(fileService == null || !fileService.hasAssignmentPdf(assignment.getAssignmentId()));
    }

    private void updateCompletedDownloadState() {
        boolean hasCompleted = !completedAssignmentsTable.getItems().isEmpty();
        boolean hasSelectedCompleted = completedAssignmentsTable.getSelectionModel().getSelectedItem() != null;
        downloadSelectedCompletedButton.setDisable(!hasSelectedCompleted);
        downloadAllCompletedButton.setDisable(!hasCompleted);
    }

    @FXML
    private void handleChoosePdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Assignment PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showOpenDialog(assignmentsTable.getScene().getWindow());
        if (file != null) {
            selectedPdfFile = file;
            selectedPdfLabel.setText(file.getName());
            statusLabel.setText("Selected PDF: " + file.getName());
        }
    }

    @FXML
    private void handlePublishAssignment() {
        if (fileService == null) {
            statusLabel.setText("PDF storage is not available.");
            return;
        }

        String classDisplay = classCombo.getValue();
        String subjectDisplay = subjectCombo.getValue();
        String title = titleField.getText().trim();
        String dueDate = dueDateField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (classDisplay == null || subjectDisplay == null || title.isEmpty() || dueDate.isEmpty() || selectedPdfFile == null) {
            statusLabel.setText("Class, subject, title, due date, and PDF are required.");
            return;
        }

        if (!dueDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            statusLabel.setText("Due date must use YYYY-MM-DD format.");
            return;
        }

        Integer classId = classDisplayToId.get(classDisplay);
        Integer subjectId = subjectDisplayToId.get(subjectDisplay);
        if (classId == null || subjectId == null) {
            statusLabel.setText("Choose valid class and subject values.");
            return;
        }

        int assignmentId = userDAO.getNextAssignmentId();
        if (assignmentId <= 0) {
            statusLabel.setText("Failed to allocate a new assignment ID.");
            return;
        }

        try {
            fileService.saveAssignmentPdf(selectedPdfFile, assignmentId);
            boolean created = userDAO.createAssignmentForTeacher(
                currentTeacher.getUserId(),
                assignmentId,
                classId,
                subjectId,
                title,
                description.isBlank() ? "-" : description,
                dueDate
            );

            if (!created) {
                statusLabel.setText("Assignment creation failed. Check teacher mapping for the chosen class and subject.");
                return;
            }

            clearPublishForm();
            loadAssignments();
            if (TeacherDashboardController.getInstance() != null) {
                TeacherDashboardController.getInstance().refreshDashboard();
            }
            statusLabel.setText("Assignment published successfully.");
            showInfo("Assignment Published", "The assignment PDF is now available to assigned students.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to publish the assignment PDF.");
            showError("Assignment Error", e.getMessage());
        }
    }

    private void clearPublishForm() {
        titleField.clear();
        dueDateField.clear();
        descriptionArea.clear();
        selectedPdfFile = null;
        selectedPdfLabel.setText("No PDF selected");
        if (!classCombo.getItems().isEmpty()) {
            classCombo.setValue(classCombo.getItems().get(0));
            loadSubjectsForSelectedClass();
        }
    }

    @FXML
    private void handleViewSelectedPdf() {
        AssignmentRecord selectedAssignment = assignmentsTable.getSelectionModel().getSelectedItem();
        if (selectedAssignment == null || fileService == null) {
            statusLabel.setText("Select a pending assignment first.");
            return;
        }

        try {
            fileService.openPdf(fileService.getAssignmentPdf(selectedAssignment.getAssignmentId()));
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Unable to open the assignment PDF.");
            showError("Open PDF Error", e.getMessage());
        }
    }

    @FXML
    private void handleDownloadSelectedCompleted() {
        AssignmentRecord selectedAssignment = completedAssignmentsTable.getSelectionModel().getSelectedItem();
        if (selectedAssignment == null || fileService == null) {
            completedStatusLabel.setText("Select a fully submitted assignment first.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Folder for Selected Assignment Submissions");
        File directory = directoryChooser.showDialog(completedAssignmentsTable.getScene().getWindow());
        if (directory == null) {
            return;
        }

        int copied = copySubmissionPdfsForAssignment(selectedAssignment, directory);
        completedStatusLabel.setText("Downloaded " + copied + " submission PDF(s) for " + selectedAssignment.getTitle() + ".");
    }

    @FXML
    private void handleDownloadAllCompleted() {
        if (completedAssignmentsTable.getItems().isEmpty() || fileService == null) {
            completedStatusLabel.setText("No fully submitted assignments are available.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Folder for All Completed Assignment Submissions");
        File directory = directoryChooser.showDialog(completedAssignmentsTable.getScene().getWindow());
        if (directory == null) {
            return;
        }

        int copied = 0;
        for (AssignmentRecord assignment : completedAssignmentsTable.getItems()) {
            copied += copySubmissionPdfsForAssignment(assignment, directory);
        }
        completedStatusLabel.setText("Downloaded " + copied + " submission PDF(s) from all fully submitted assignments.");
    }

    private int copySubmissionPdfsForAssignment(AssignmentRecord assignment, File directory) {
        int copied = 0;
        LinkedHashMap<Integer, String> submittedStudents = userDAO.getSubmittedStudentsForAssignment(assignment.getAssignmentId());
        for (Map.Entry<Integer, String> entry : submittedStudents.entrySet()) {
            File source = fileService.getSubmissionPdf(assignment.getAssignmentId(), entry.getKey());
            if (!source.exists()) {
                continue;
            }

            String targetName = sanitizeFileName(assignment.getTitle()) + "_" + sanitizeFileName(entry.getValue()) + ".pdf";
            File target = new File(directory, targetName);
            try {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return copied;
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    @FXML
    private void handleRefresh() {
        loadTeacherClasses();
        loadAssignments();
        if (!classCombo.getItems().isEmpty()) {
            statusLabel.setText("Assignments refreshed.");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }

    @FXML
    private void backToDashboard() {
        if (TeacherDashboardController.getInstance() != null) {
            TeacherDashboardController.getInstance().scrollToTop();
        }
    }
}
