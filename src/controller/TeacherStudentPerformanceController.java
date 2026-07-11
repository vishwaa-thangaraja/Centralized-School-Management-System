package controller;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import model.PerformanceRecord;
import model.Student;
import model.User;
import service.SchoolSettingsService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

public class TeacherStudentPerformanceController {

    @FXML private Label teacherInfoLabel;
    @FXML private Label studentCountLabel;
    @FXML private Label statusLabel;
    @FXML private Label studentNameLabel;
    @FXML private Label classLabel;
    @FXML private Label attendanceLabel;
    @FXML private Label averageMarksLabel;
    @FXML private Label conductLabel;
    @FXML private Label conductRemarksLabel;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, Number> studentIdCol;
    @FXML private TableColumn<Student, String> nameCol;
    @FXML private TableColumn<Student, String> classCol;
    @FXML private TableView<PerformanceRecord> marksTable;
    @FXML private TableColumn<PerformanceRecord, String> subjectCol;
    @FXML private TableColumn<PerformanceRecord, String> examCol;
    @FXML private TableColumn<PerformanceRecord, Double> marksCol;
    @FXML private Button downloadSelectedButton;
    @FXML private Button downloadAllButton;

    private final UserDAO userDAO = new UserDAO();
    private User currentTeacher;

    @FXML
    public void initialize() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        classCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));

        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));
        examCol.setCellValueFactory(new PropertyValueFactory<>("examType"));
        marksCol.setCellValueFactory(new PropertyValueFactory<>("marksObtained"));

        studentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selectedStudent) -> {
            loadStudentPerformance(selectedStudent);
        });

        setStudentContext(null, FXCollections.observableArrayList());
        downloadAllButton.setDisable(true);
        statusLabel.setText("Select a student to review academic performance and export reports.");
    }

    public void initData(User teacher) {
        this.currentTeacher = teacher;
        teacherInfoLabel.setText("Teacher: " + teacher.getName() + " | Reports include academics and conduct details only.");
        loadStudents();
    }

    private void loadStudents() {
        ObservableList<Student> students = userDAO.getStudentsForTeacher(currentTeacher.getUserId());
        studentsTable.setItems(students);
        studentsTable.getSelectionModel().clearSelection();
        studentCountLabel.setText(String.valueOf(students.size()));
        downloadAllButton.setDisable(students.isEmpty());
        setStudentContext(null, FXCollections.observableArrayList());

        if (students.isEmpty()) {
            statusLabel.setText("No mapped students are available for report generation.");
        } else {
            statusLabel.setText("Select a student to review academic performance and export reports.");
        }
    }

    private void loadStudentPerformance(Student student) {
        if (student == null) {
            setStudentContext(null, FXCollections.observableArrayList());
            statusLabel.setText("Select a student to review academic performance and export reports.");
            return;
        }

        ObservableList<PerformanceRecord> marksData = userDAO.getStudentMarks(student.getUserId());
        setStudentContext(student, marksData);
        statusLabel.setText("Loaded performance details for " + student.getName() + ".");
    }

    private void setStudentContext(Student student, ObservableList<PerformanceRecord> marksData) {
        boolean hasStudent = student != null;
        studentNameLabel.setText(hasStudent ? student.getName() : "No student selected");
        classLabel.setText(hasStudent ? student.getClassDisplay() : "-");
        attendanceLabel.setText(hasStudent ? String.format("%.1f%%", userDAO.getAttendancePercentage(student.getUserId())) : "-");
        averageMarksLabel.setText(hasStudent ? String.format("%.1f/100", userDAO.getAverageMarks(student.getUserId())) : "-");
        conductLabel.setText(hasStudent ? student.getConduct() : "-");
        conductRemarksLabel.setText(hasStudent ? normalizeDisplay(student.getConductRemarks()) : "-");
        marksTable.setItems(marksData);
        downloadSelectedButton.setDisable(!hasStudent);
    }

    private String normalizeDisplay(String value) {
        return value == null || value.isBlank() || "-".equals(value) ? "No remarks available" : value;
    }

    @FXML
    private void handleDownloadSelected() {
        Student selectedStudent = studentsTable.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            statusLabel.setText("Select a student before downloading a report.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Student Performance PDF");
        fileChooser.setInitialFileName("Student_Report_" + sanitizeFileName(selectedStudent.getName()) + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(studentsTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            createStudentReportPdf(file, selectedStudent);
            statusLabel.setText("Report saved for " + selectedStudent.getName() + ".");
            showInfo("PDF Generated", "The selected student's report was generated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Failed to generate the selected student's report.");
            showError("PDF Error", e.getMessage());
        }
    }

    @FXML
    private void handleDownloadAll() {
        ObservableList<Student> students = studentsTable.getItems();
        if (students.isEmpty()) {
            statusLabel.setText("No students are available for bulk export.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Folder for Student Reports");
        File directory = directoryChooser.showDialog(studentsTable.getScene().getWindow());
        if (directory == null) {
            return;
        }

        int generatedCount = 0;
        for (Student student : students) {
            File file = new File(directory, "Student_Report_" + sanitizeFileName(student.getName()) + ".pdf");
            try {
                createStudentReportPdf(file, student);
                generatedCount++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        statusLabel.setText("Generated " + generatedCount + " student performance reports.");
        showInfo("Bulk Export Complete", "Generated " + generatedCount + " PDF report(s) for mapped students.");
    }

    private void createStudentReportPdf(File file, Student student) throws Exception {
        ObservableList<PerformanceRecord> marksData = userDAO.getStudentMarks(student.getUserId());
        double attendance = userDAO.getAttendancePercentage(student.getUserId());
        double averageMarks = userDAO.getAverageMarks(student.getUserId());

        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(SchoolSettingsService.getSchoolName()).setFontSize(18));
        for (String line : SchoolSettingsService.getContactLines()) {
            document.add(new Paragraph(line).setFontSize(9));
        }
        document.add(new Paragraph("Student Performance and Conduct Report").setFontSize(13));
        document.add(new Paragraph("Generated by: " + currentTeacher.getName()));
        document.add(new Paragraph("Generated on: " + new Date()));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Student Name: " + student.getName()));
        document.add(new Paragraph("Student ID: " + student.getStudentId()));
        document.add(new Paragraph("Class: " + student.getClassDisplay()));
        document.add(new Paragraph(String.format("Attendance: %.1f%%", attendance)));
        document.add(new Paragraph(String.format("Average Marks: %.1f/100", averageMarks)));
        document.add(new Paragraph("Conduct: " + student.getConduct()));
        document.add(new Paragraph("Conduct Remarks: " + normalizeDisplay(student.getConductRemarks())));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Academic Performance"));

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2}));
        table.useAllAvailableWidth();
        table.addHeaderCell("Subject");
        table.addHeaderCell("Exam Type");
        table.addHeaderCell("Marks");

        if (marksData.isEmpty()) {
            table.addCell("No academic records");
            table.addCell("-");
            table.addCell("-");
        } else {
            for (PerformanceRecord record : marksData) {
                table.addCell(record.getSubject());
                table.addCell(record.getExamType());
                table.addCell(String.format("%.1f", record.getMarksObtained()));
            }
        }

        document.add(table);
        document.close();
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private void showInfo(String title, String message) {
        DialogSupport.info(studentsTable, title, message);
    }

    private void showError(String title, String message) {
        DialogSupport.error(studentsTable, title, message);
    }

    @FXML
    private void backToDashboard() {
        if (TeacherDashboardController.getInstance() != null) {
            TeacherDashboardController.getInstance().scrollToTop();
        }
    }
}
