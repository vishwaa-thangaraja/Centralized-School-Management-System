package controller;

import dao.QuestionBankDAO;
import dao.UserDAO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import model.QuestionBankRecord;
import model.User;
import service.QuestionBankFileService;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TeacherQuestionBankController {
    @FXML private Label teacherInfoLabel;
    @FXML private Label statusLabel;
    @FXML private Label selectedPdfLabel;
    @FXML private Label questionCountLabel;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private Label academicYearLabel;
    @FXML private TextField titleField;
    @FXML private TableView<QuestionBankRecord> questionTable;
    @FXML private TableColumn<QuestionBankRecord, String> titleCol;
    @FXML private TableColumn<QuestionBankRecord, String> classCol;
    @FXML private TableColumn<QuestionBankRecord, String> subjectCol;
    @FXML private TableColumn<QuestionBankRecord, String> yearCol;
    @FXML private TableColumn<QuestionBankRecord, String> uploadedCol;
    @FXML private Button openSelectedButton;

    private final UserDAO userDAO = new UserDAO();
    private final QuestionBankDAO questionBankDAO = new QuestionBankDAO();
    private final Map<String, Integer> classDisplayToId = new LinkedHashMap<>();
    private final Map<String, String> classDisplayToYear = new LinkedHashMap<>();
    private final Map<String, Integer> subjectDisplayToId = new LinkedHashMap<>();

    private User currentTeacher;
    private File selectedPdfFile;
    private QuestionBankFileService fileService;

    @FXML
    public void initialize() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        classCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("academicYear"));
        uploadedCol.setCellValueFactory(new PropertyValueFactory<>("uploadedAt"));
        questionTable.setPlaceholder(new Label("No question papers uploaded yet."));
        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, record) -> {
            openSelectedButton.setDisable(record == null || fileService == null || !fileService.hasQuestionPdf(record.getQuestionId()));
        });

        classCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadSubjectsForSelectedClass());
        selectedPdfLabel.setText("No PDF selected");
        openSelectedButton.setDisable(true);
        statusLabel.setText("Choose a class, subject, title, and PDF.");

        try {
            fileService = new QuestionBankFileService();
        } catch (IOException e) {
            showError("Storage Error", e.getMessage());
        }
    }

    public void initData(User teacher) {
        this.currentTeacher = teacher;
        teacherInfoLabel.setText("Teacher: " + teacher.getName());
        loadTeacherClasses();
        loadQuestionPapers();
    }

    private void loadTeacherClasses() {
        classDisplayToId.clear();
        classDisplayToYear.clear();
        subjectDisplayToId.clear();
        classCombo.getItems().clear();
        subjectCombo.getItems().clear();

        LinkedHashMap<Integer, String> classes = userDAO.getTeacherClasses(currentTeacher.getUserId());
        for (Map.Entry<Integer, String> entry : classes.entrySet()) {
            classDisplayToId.put(entry.getValue(), entry.getKey());
            classDisplayToYear.put(entry.getValue(), extractAcademicYear(entry.getValue()));
            classCombo.getItems().add(entry.getValue());
        }

        if (classCombo.getItems().isEmpty()) {
            statusLabel.setText("No assigned classes are available.");
            return;
        }

        classCombo.setValue(classCombo.getItems().get(0));
        loadSubjectsForSelectedClass();
    }

    private void loadSubjectsForSelectedClass() {
        subjectDisplayToId.clear();
        subjectCombo.getItems().clear();

        String classDisplay = classCombo.getValue();
        Integer classId = classDisplayToId.get(classDisplay);
        academicYearLabel.setText(classDisplayToYear.getOrDefault(classDisplay, "-"));
        if (currentTeacher == null || classId == null) {
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

    private void loadQuestionPapers() {
        if (currentTeacher == null) {
            return;
        }
        ObservableList<QuestionBankRecord> records = questionBankDAO.getTeacherQuestionPapers(currentTeacher.getUserId());
        questionTable.setItems(records);
        questionCountLabel.setText(String.valueOf(records.size()));
        questionTable.getSelectionModel().clearSelection();
        openSelectedButton.setDisable(true);
    }

    @FXML
    private void handleChoosePdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Question Paper PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(questionTable.getScene().getWindow());
        if (file != null) {
            selectedPdfFile = file;
            selectedPdfLabel.setText(file.getName());
            statusLabel.setText("Selected PDF: " + file.getName());
        }
    }

    @FXML
    private void handlePublishQuestionPaper() {
        if (fileService == null) {
            statusLabel.setText("PDF storage is not available.");
            return;
        }

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String classDisplay = classCombo.getValue();
        String subjectDisplay = subjectCombo.getValue();

        if (title.isEmpty() || classDisplay == null || subjectDisplay == null || selectedPdfFile == null) {
            statusLabel.setText("Class, subject, title, and PDF are required.");
            return;
        }

        Integer classId = classDisplayToId.get(classDisplay);
        Integer subjectId = subjectDisplayToId.get(subjectDisplay);
        if (classId == null || subjectId == null) {
            statusLabel.setText("Choose valid class and subject values.");
            return;
        }

        int questionId = questionBankDAO.getNextQuestionId();
        if (questionId <= 0) {
            statusLabel.setText("Unable to allocate a question paper ID.");
            return;
        }

        File savedPdf = fileService.getQuestionPdf(questionId);
        try {
            fileService.saveQuestionPdf(selectedPdfFile, questionId);
            boolean created = questionBankDAO.createQuestionPaper(
                currentTeacher.getUserId(),
                questionId,
                classId,
                subjectId,
                title,
                selectedPdfFile.getName()
            );
            if (!created) {
                savedPdf.delete();
                statusLabel.setText("Upload failed. Check your class-subject mapping.");
                return;
            }

            clearForm();
            loadQuestionPapers();
            statusLabel.setText("Question paper uploaded successfully.");
            showInfo("Question Paper Uploaded", "The PDF is available in the student question bank.");
        } catch (Exception e) {
            savedPdf.delete();
            e.printStackTrace();
            statusLabel.setText("Failed to upload the PDF.");
            showError("Upload Error", e.getMessage());
        }
    }

    @FXML
    private void handleOpenSelectedPdf() {
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
    private void handleRefresh() {
        loadTeacherClasses();
        loadQuestionPapers();
        statusLabel.setText("Question bank refreshed.");
    }

    private void clearForm() {
        titleField.clear();
        selectedPdfFile = null;
        selectedPdfLabel.setText("No PDF selected");
        if (!classCombo.getItems().isEmpty()) {
            classCombo.setValue(classCombo.getItems().get(0));
            loadSubjectsForSelectedClass();
        }
    }

    @FXML
    private void backToDashboard() {
        if (TeacherDashboardController.getInstance() != null) {
            TeacherDashboardController.getInstance().scrollToTop();
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

    private String extractAcademicYear(String classDisplay) {
        if (classDisplay == null) {
            return "-";
        }
        int start = classDisplay.lastIndexOf('(');
        int end = classDisplay.lastIndexOf(')');
        if (start >= 0 && end > start) {
            return classDisplay.substring(start + 1, end);
        }
        return "-";
    }
}
