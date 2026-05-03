package controller;

import dao.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import model.AdminParentLinkRecord;
import model.AdminStudentClassRecord;
import model.AdminTeacherMappingRecord;
import model.User;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class AdminAcademicMappingController {

    @FXML private Label adminContextLabel;
    @FXML private Label statusLabel;
    @FXML private TextField classNameField;
    @FXML private TextField sectionField;
    @FXML private TextField academicYearField;
    @FXML private ComboBox<String> teacherCombo;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private TableView<AdminTeacherMappingRecord> teacherMappingsTable;
    @FXML private TableColumn<AdminTeacherMappingRecord, String> tmTeacherCol;
    @FXML private TableColumn<AdminTeacherMappingRecord, String> tmClassCol;
    @FXML private TableColumn<AdminTeacherMappingRecord, String> tmSubjectCol;
    @FXML private ComboBox<String> studentCombo;
    @FXML private ComboBox<String> moveClassCombo;
    @FXML private TableView<AdminStudentClassRecord> studentClassTable;
    @FXML private TableColumn<AdminStudentClassRecord, String> scStudentCol;
    @FXML private TableColumn<AdminStudentClassRecord, String> scClassCol;
    @FXML private ComboBox<String> parentCombo;
    @FXML private ComboBox<String> linkStudentCombo;
    @FXML private TextField relationField;
    @FXML private TableView<AdminParentLinkRecord> parentLinksTable;
    @FXML private TableColumn<AdminParentLinkRecord, String> plParentCol;
    @FXML private TableColumn<AdminParentLinkRecord, String> plStudentCol;
    @FXML private TableColumn<AdminParentLinkRecord, String> plRelationCol;
    @FXML private ComboBox<String> deleteUserCombo;

    private final UserDAO userDAO = new UserDAO();
    private User currentAdmin;
    private final LinkedHashMap<Integer, String> teacherOptions = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, String> classOptions = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, String> subjectOptions = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, String> parentOptions = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, String> studentOptions = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, String> deleteUserOptions = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        tmTeacherCol.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        tmClassCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));
        tmSubjectCol.setCellValueFactory(new PropertyValueFactory<>("subjectName"));

        scStudentCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        scClassCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));

        plParentCol.setCellValueFactory(new PropertyValueFactory<>("parentName"));
        plStudentCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        plRelationCol.setCellValueFactory(new PropertyValueFactory<>("relation"));
    }

    public void initData(User adminUser) {
        this.currentAdmin = adminUser;
        adminContextLabel.setText("Admin: " + adminUser.getName());
        loadOptions();
        loadTables();
    }

    private void loadOptions() {
        teacherOptions.clear();
        teacherOptions.putAll(userDAO.getAdminTeacherOptions());
        teacherCombo.getItems().setAll(teacherOptions.values());
        selectFirst(teacherCombo);

        classOptions.clear();
        classOptions.putAll(userDAO.getAdminClassOptions());
        classCombo.getItems().setAll(classOptions.values());
        moveClassCombo.getItems().setAll(classOptions.values());
        selectFirst(classCombo);
        selectFirst(moveClassCombo);

        subjectOptions.clear();
        subjectOptions.putAll(userDAO.getAdminSubjectOptions());
        subjectCombo.getItems().setAll(subjectOptions.values());
        selectFirst(subjectCombo);

        parentOptions.clear();
        parentOptions.putAll(userDAO.getAdminParentOptions());
        parentCombo.getItems().setAll(parentOptions.values());
        parentCombo.setEditable(true);
        selectFirst(parentCombo);

        studentOptions.clear();
        studentOptions.putAll(userDAO.getAdminStudentOptions());
        studentCombo.getItems().setAll(studentOptions.values());
        linkStudentCombo.getItems().setAll(studentOptions.values());
        studentCombo.setEditable(true);
        linkStudentCombo.setEditable(true);
        selectFirst(studentCombo);
        selectFirst(linkStudentCombo);

        deleteUserOptions.clear();
        deleteUserOptions.putAll(userDAO.getAdminDeletableUserOptions(currentAdmin.getUserId()));
        deleteUserCombo.getItems().setAll(deleteUserOptions.values());
        deleteUserCombo.setEditable(true);
        selectFirst(deleteUserCombo);
    }

    private void loadTables() {
        teacherMappingsTable.setItems(userDAO.getTeacherSubjectClassMappings());
        studentClassTable.setItems(userDAO.getStudentClassMappings());
        parentLinksTable.setItems(userDAO.getParentStudentLinks());
        if (AdminDashboardController.getInstance() != null) {
            AdminDashboardController.getInstance().refreshDashboard();
        }
    }

    @FXML
    private void handleCreateClass() {
        String className = text(classNameField);
        String section = text(sectionField);
        String academicYear = text(academicYearField);
        if (className.isEmpty() || section.isEmpty() || academicYear.isEmpty()) {
            statusLabel.setText("Class name, section, and academic year are required.");
            return;
        }
        boolean created = userDAO.createClassAsAdmin(currentAdmin.getUserId(), className, section, academicYear);
        if (!created) {
            statusLabel.setText("Class already exists or could not be created.");
            return;
        }
        classNameField.clear();
        sectionField.clear();
        academicYearField.clear();
        loadOptions();
        loadTables();
        statusLabel.setText("Class created.");
    }

    @FXML
    private void handleAddTeacherMapping() {
        int teacherId = selectedId(teacherOptions, value(teacherCombo));
        int classId = selectedId(classOptions, value(classCombo));
        int subjectId = selectedId(subjectOptions, value(subjectCombo));
        if (teacherId <= 0 || classId <= 0 || subjectId <= 0) {
            statusLabel.setText("Select teacher, class, and subject.");
            return;
        }
        boolean added = userDAO.addTeacherSubjectClassMapping(currentAdmin.getUserId(), classId, subjectId, teacherId);
        loadTables();
        statusLabel.setText(added ? "Teacher assigned." : "Class limit reached or assignment already exists.");
    }

    @FXML
    private void handleMoveStudent() {
        int studentId = selectedId(studentOptions, value(studentCombo));
        int classId = selectedId(classOptions, value(moveClassCombo));
        if (studentId <= 0 || classId <= 0) {
            statusLabel.setText("Select student and target class.");
            return;
        }
        boolean moved = userDAO.moveStudentToClass(currentAdmin.getUserId(), studentId, classId);
        loadTables();
        statusLabel.setText(moved ? "Student class updated." : "Class limit reached or student could not be moved.");
    }

    @FXML
    private void handleLinkParent() {
        int parentUserId = selectedId(parentOptions, value(parentCombo));
        int studentId = selectedId(studentOptions, value(linkStudentCombo));
        String relation = relationField.getText() == null ? "" : relationField.getText().trim();
        if (parentUserId <= 0 || studentId <= 0 || relation.isEmpty()) {
            statusLabel.setText("Select parent, student, and relation.");
            return;
        }
        boolean linked = userDAO.linkParentToStudent(currentAdmin.getUserId(), parentUserId, studentId, relation);
        loadTables();
        statusLabel.setText(linked ? "Parent saved." : "Could not save parent.");
    }

    @FXML
    private void handleDeleteUser() {
        int targetUserId = selectedId(deleteUserOptions, value(deleteUserCombo));
        if (targetUserId <= 0) {
            statusLabel.setText("Select a user to delete.");
            return;
        }

        String selectedUser = value(deleteUserCombo);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete selected user?");
        confirm.setContentText(selectedUser);
        Optional<ButtonType> response = confirm.showAndWait();
        if (response.isEmpty() || response.get() != ButtonType.OK) {
            return;
        }

        boolean deleted = userDAO.deleteUserAsAdmin(currentAdmin.getUserId(), targetUserId);
        if (!deleted) {
            statusLabel.setText("Delete blocked. Admin users cannot be deleted here.");
            return;
        }
        loadOptions();
        loadTables();
        statusLabel.setText("User deleted.");
    }

    @FXML
    private void handleRefresh() {
        loadOptions();
        loadTables();
        statusLabel.setText("Academic setup refreshed.");
    }

    @FXML
    private void backToDashboard() {
        if (AdminDashboardController.getInstance() != null) {
            AdminDashboardController.getInstance().scrollToTop();
        }
    }

    private void selectFirst(ComboBox<String> comboBox) {
        if (!comboBox.getItems().isEmpty()) {
            comboBox.getSelectionModel().selectFirst();
        }
    }

    private String value(ComboBox<String> comboBox) {
        return comboBox.getValue() == null ? "" : comboBox.getValue().trim();
    }

    private String text(TextField textField) {
        return textField.getText() == null ? "" : textField.getText().trim();
    }

    private int selectedId(LinkedHashMap<Integer, String> map, String selectedDisplay) {
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getValue().equals(selectedDisplay)) {
                return entry.getKey();
            }
        }
        return -1;
    }
}
