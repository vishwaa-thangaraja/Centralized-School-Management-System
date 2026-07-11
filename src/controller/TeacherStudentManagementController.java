package controller;

import dao.UserDAO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Student;
import model.User;

import java.util.LinkedHashMap;
import java.util.Map;

public class TeacherStudentManagementController {

    @FXML private Label teacherInfoLabel;
    @FXML private Label studentCountLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, Number> studentIdCol;
    @FXML private TableColumn<Student, String> nameCol;
    @FXML private TableColumn<Student, String> classCol;
    @FXML private TableColumn<Student, String> conductCol;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField dobField;
    @FXML private TextField genderField;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> conductCombo;
    @FXML private TextArea remarksArea;
    @FXML private Button saveButton;

    private final UserDAO userDAO = new UserDAO();
    private final Map<String, Integer> classDisplayToId = new LinkedHashMap<>();
    private User currentTeacher;

    @FXML
    public void initialize() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        classCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));
        conductCol.setCellValueFactory(new PropertyValueFactory<>("conduct"));

        conductCombo.getItems().addAll("Excellent", "Good", "Average", "Poor");
        configureReadOnlyFields();

        studentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selectedStudent) -> {
            populateForm(selectedStudent);
            if (selectedStudent != null) {
                statusLabel.setText("Viewing " + selectedStudent.getName() + ". Only class, conduct, and remarks can be edited.");
            }
        });

        setFormDisabled(true);
        statusLabel.setText("Select a student from the left to view details. Only class, conduct, and remarks are editable.");
    }

    public void initData(User teacher) {
        this.currentTeacher = teacher;
        teacherInfoLabel.setText("Teacher: " + teacher.getName() + " | Only class, conduct, and remarks can be updated for students in your scope.");
        loadTeacherClasses();
        loadStudents();
    }

    private void loadTeacherClasses() {
        classDisplayToId.clear();
        classCombo.getItems().clear();

        LinkedHashMap<Integer, String> teacherClasses = userDAO.getTeacherClasses(currentTeacher.getUserId());
        for (Map.Entry<Integer, String> entry : teacherClasses.entrySet()) {
            classDisplayToId.put(entry.getValue(), entry.getKey());
            classCombo.getItems().add(entry.getValue());
        }
    }

    private void loadStudents() {
        if (currentTeacher == null) {
            return;
        }

        ObservableList<Student> students = userDAO.getStudentsForTeacher(currentTeacher.getUserId());
        studentsTable.setItems(students);
        studentsTable.getSelectionModel().clearSelection();
        studentCountLabel.setText(String.valueOf(students.size()));
        clearForm();

        if (students.isEmpty()) {
            statusLabel.setText("No students are mapped to your classes yet.");
        } else {
            statusLabel.setText("Select a student from the left to view details. Only class, conduct, and remarks are editable.");
        }
    }

    private void populateForm(Student student) {
        if (student == null) {
            clearForm();
            return;
        }

        setFormDisabled(false);
        nameField.setText(student.getName());
        emailField.setText(student.getEmail());
        phoneField.setText("-".equals(student.getPhone()) ? "" : student.getPhone());
        dobField.setText(student.getDob());
        classCombo.setValue(student.getClassDisplay());
        genderField.setText("-".equals(student.getGender()) ? "" : student.getGender());
        conductCombo.setValue("-".equals(student.getConduct()) ? null : student.getConduct());
        remarksArea.setText("-".equals(student.getConductRemarks()) ? "" : student.getConductRemarks());
    }

    private void clearForm() {
        setFormDisabled(true);
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        dobField.clear();
        genderField.clear();
        classCombo.setValue(null);
        conductCombo.setValue(null);
        remarksArea.clear();
    }

    private void setFormDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        emailField.setDisable(disabled);
        phoneField.setDisable(disabled);
        dobField.setDisable(disabled);
        genderField.setDisable(disabled);
        classCombo.setDisable(disabled);
        conductCombo.setDisable(disabled);
        remarksArea.setDisable(disabled);
        saveButton.setDisable(disabled);
    }

    private void configureReadOnlyFields() {
        for (TextField field : new TextField[]{nameField, emailField, phoneField, dobField, genderField}) {
            field.setEditable(false);
            field.setFocusTraversable(false);
            if (!field.getStyleClass().contains("readonly-field")) {
                field.getStyleClass().add("readonly-field");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadTeacherClasses();
        loadStudents();
    }

    @FXML
    private void handleSaveChanges() {
        Student selectedStudent = studentsTable.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            statusLabel.setText("Select a student first.");
            return;
        }

        String classDisplay = classCombo.getValue();
        String conduct = conductCombo.getValue();
        String remarks = remarksArea.getText().trim();

        if (classDisplay == null || conduct == null) {
            statusLabel.setText("Class and conduct are required.");
            return;
        }

        Integer selectedClassId = classDisplayToId.get(classDisplay);
        if (selectedClassId == null) {
            statusLabel.setText("Choose a class from the teacher's mapped classes.");
            return;
        }

        selectedStudent.setConduct(conduct);
        selectedStudent.setConductRemarks(remarks);

        boolean updated = userDAO.updateStudentClassAndConductForTeacher(
            currentTeacher.getUserId(),
            selectedStudent.getStudentId(),
            selectedStudent.getClassId(),
            selectedClassId,
            conduct,
            remarks
        );
        if (updated) {
            statusLabel.setText("Student class/conduct details updated successfully.");
            loadStudents();
        } else {
            statusLabel.setText("Update failed. Check class access or student scope.");
            DialogSupport.warning(studentsTable, "Student update blocked", "This teacher can edit only class, conduct, and remarks for mapped students, and can assign only classes already mapped to the teacher.");
        }
    }

    @FXML
    private void backToDashboard() {
        if (TeacherDashboardController.getInstance() != null) {
            TeacherDashboardController.getInstance().scrollToTop();
        }
    }
}
