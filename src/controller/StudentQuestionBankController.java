package controller;

import dao.QuestionBankDAO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import model.QuestionBankRecord;
import model.User;
import service.QuestionBankFileService;

import java.io.File;
import java.io.IOException;

public class StudentQuestionBankController {
    @FXML private Label statusLabel;
    @FXML private Label totalQuestionsLabel;
    @FXML private Label selectedTitleLabel;
    @FXML private Label selectedSubjectLabel;
    @FXML private Label selectedTeacherLabel;
    @FXML private TableView<QuestionBankRecord> questionTable;
    @FXML private TableColumn<QuestionBankRecord, String> titleCol;
    @FXML private TableColumn<QuestionBankRecord, String> subjectCol;
    @FXML private TableColumn<QuestionBankRecord, String> yearCol;
    @FXML private TableColumn<QuestionBankRecord, String> teacherCol;
    @FXML private TableColumn<QuestionBankRecord, String> uploadedCol;
    @FXML private Button openButton;
    @FXML private Button downloadButton;

    private final QuestionBankDAO questionBankDAO = new QuestionBankDAO();
    private User currentStudent;
    private QuestionBankFileService fileService;

    @FXML
    public void initialize() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("academicYear"));
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        uploadedCol.setCellValueFactory(new PropertyValueFactory<>("uploadedAt"));
        questionTable.setPlaceholder(new Label("No question papers are available yet."));
        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, record) -> populateDetails(record));

        try {
            fileService = new QuestionBankFileService();
        } catch (IOException e) {
            showError("Storage Error", e.getMessage());
        }

        clearDetails();
        statusLabel.setText("Select a question paper to open or download.");
    }

    public void initData(User student) {
        this.currentStudent = student;
        loadQuestions();
    }

    private void loadQuestions() {
        if (currentStudent == null) {
            return;
        }
        ObservableList<QuestionBankRecord> records = questionBankDAO.getStudentQuestionPapers(currentStudent.getUserId());
        questionTable.setItems(records);
        totalQuestionsLabel.setText(String.valueOf(records.size()));
        questionTable.getSelectionModel().clearSelection();
        clearDetails();
        statusLabel.setText(records.isEmpty()
            ? "No question papers are available for your class yet."
            : "Select a question paper to open or download.");
    }

    private void populateDetails(QuestionBankRecord record) {
        if (record == null) {
            clearDetails();
            return;
        }

        selectedTitleLabel.setText(record.getTitle());
        selectedSubjectLabel.setText(record.getSubjectName());
        selectedTeacherLabel.setText(record.getTeacherName());
        boolean hasPdf = fileService != null && fileService.hasQuestionPdf(record.getQuestionId());
        openButton.setDisable(!hasPdf);
        downloadButton.setDisable(!hasPdf);
        statusLabel.setText(hasPdf ? "PDF ready." : "PDF file is missing on this machine.");
    }

    private void clearDetails() {
        selectedTitleLabel.setText("No question paper selected");
        selectedSubjectLabel.setText("-");
        selectedTeacherLabel.setText("-");
        openButton.setDisable(true);
        downloadButton.setDisable(true);
    }

    @FXML
    private void handleOpenPdf() {
        QuestionBankRecord selected = questionTable.getSelectionModel().getSelectedItem();
        if (selected == null || fileService == null) {
            statusLabel.setText("Select a question paper first.");
            return;
        }

        try {
            fileService.openPdf(fileService.getQuestionPdf(selected.getQuestionId()));
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Unable to open the PDF.");
            showError("Open PDF Error", e.getMessage());
        }
    }

    @FXML
    private void handleDownloadPdf() {
        QuestionBankRecord selected = questionTable.getSelectionModel().getSelectedItem();
        if (selected == null || fileService == null) {
            statusLabel.setText("Select a question paper first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Question Paper PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(sanitizeFileName(selected.getTitle()) + ".pdf");
        File target = fileChooser.showSaveDialog(questionTable.getScene().getWindow());
        if (target == null) {
            return;
        }

        try {
            fileService.copyQuestionPdf(selected.getQuestionId(), target);
            statusLabel.setText("Downloaded: " + target.getName());
            showInfo("Download Complete", "The question paper PDF was saved.");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Unable to download the PDF.");
            showError("Download Error", e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadQuestions();
    }

    @FXML
    private void backToDashboard() {
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().scrollToTop();
        }
    }

    private String sanitizeFileName(String value) {
        return value == null ? "question_paper" : value.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private void showInfo(String title, String message) {
        DialogSupport.info(questionTable, title, message);
    }

    private void showError(String title, String message) {
        DialogSupport.error(questionTable, title, message);
    }
}
