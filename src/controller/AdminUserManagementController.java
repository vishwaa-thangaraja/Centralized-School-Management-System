package controller;

import dao.UserDAO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import model.AdminUserRecord;
import model.User;
import service.AuthService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminUserManagementController {

    @FXML private Label adminContextLabel;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private VBox teacherFieldsBox;
    @FXML private TextField qualificationField;
    @FXML private TextField experienceField;
    @FXML private VBox studentFieldsBox;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<String> genderCombo;
    @FXML private ComboBox<String> conductCombo;
    @FXML private TextArea conductRemarksArea;
    @FXML private ComboBox<String> studentClassCombo;
    @FXML private ComboBox<String> parentCombo;
    @FXML private TextField relationField;
    @FXML private TableView<AdminUserRecord> usersTable;
    @FXML private TableColumn<AdminUserRecord, Number> userIdCol;
    @FXML private TableColumn<AdminUserRecord, String> userNameCol;
    @FXML private TableColumn<AdminUserRecord, String> userEmailCol;
    @FXML private TableColumn<AdminUserRecord, String> userRoleCol;
    @FXML private TableColumn<AdminUserRecord, String> userPhoneCol;
    @FXML private TableColumn<AdminUserRecord, String> userActiveCol;
    @FXML private TextField editNameField;
    @FXML private TextField editEmailField;
    @FXML private TextField editPhoneField;
    @FXML private Button activeToggleButton;

    private final UserDAO userDAO = new UserDAO();
    private User currentAdmin;
    private final LinkedHashMap<Integer, String> classOptions = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, String> parentOptions = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        userIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        userNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        userEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        userRoleCol.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        userPhoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        userActiveCol.setCellValueFactory(new PropertyValueFactory<>("activeText"));

        genderCombo.getItems().setAll("Male", "Female", "Other");
        conductCombo.getItems().setAll("Excellent", "Good", "Average", "Poor");
        conductCombo.getSelectionModel().select("Good");
        dobPicker.setValue(LocalDate.now().minusYears(15));

        roleCombo.valueProperty().addListener((obs, oldRole, newRole) -> updateRoleFields());
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, selectedUser) -> populateSelectedUser(selectedUser));
        updateRoleFields();
        updateActiveButton(null);
    }

    public void initData(User adminUser) {
        this.currentAdmin = adminUser;
        adminContextLabel.setText("Admin: " + adminUser.getName());
        loadOptions();
        loadUsers();
    }

    private void loadOptions() {
        roleCombo.getItems().clear();
        for (String roleName : userDAO.getAdminRoleOptions().values()) {
            roleCombo.getItems().add(roleName);
        }
        roleCombo.getSelectionModel().select("Student");

        classOptions.clear();
        classOptions.putAll(userDAO.getAdminClassOptions());
        studentClassCombo.getItems().setAll(classOptions.values());
        if (!studentClassCombo.getItems().isEmpty()) {
            studentClassCombo.getSelectionModel().selectFirst();
        }

        parentOptions.clear();
        parentOptions.putAll(userDAO.getAdminParentOptions());
        parentCombo.getItems().setAll(parentOptions.values());
        parentCombo.setEditable(true);
        if (!parentCombo.getItems().isEmpty()) {
            parentCombo.getSelectionModel().selectFirst();
        }
    }

    private void loadUsers() {
        ObservableList<AdminUserRecord> users = userDAO.getAdminUsers();
        usersTable.setItems(users);
        updateActiveButton(usersTable.getSelectionModel().getSelectedItem());
        if (AdminDashboardController.getInstance() != null) {
            AdminDashboardController.getInstance().refreshDashboard();
        }
    }

    private void updateRoleFields() {
        String role = roleCombo.getValue();
        boolean teacher = "Teacher".equalsIgnoreCase(role);
        boolean student = "Student".equalsIgnoreCase(role);
        teacherFieldsBox.setVisible(teacher);
        teacherFieldsBox.setManaged(teacher);
        studentFieldsBox.setVisible(student);
        studentFieldsBox.setManaged(student);
    }

    private void populateSelectedUser(AdminUserRecord selectedUser) {
        if (selectedUser == null) {
            editNameField.clear();
            editEmailField.clear();
            editPhoneField.clear();
            statusLabel.setText("");
            updateActiveButton(null);
            return;
        }
        editNameField.setText(selectedUser.getName());
        editEmailField.setText(selectedUser.getEmail());
        editPhoneField.setText("-".equals(selectedUser.getPhone()) ? "" : selectedUser.getPhone());
        statusLabel.setText("");
        updateActiveButton(selectedUser);
    }

    @FXML
    private void handleCreateUser() {
        if (currentAdmin == null) {
            statusLabel.setText("Admin session not found.");
            return;
        }
        String role = value(roleCombo);
        String name = text(nameField);
        String email = text(emailField);
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String phone = text(phoneField);

        if (role.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Role, name, valid email, and password are mandatory.");
            return;
        }
        if (!looksLikeEmail(email)) {
            statusLabel.setText("Email must be 100% valid for OTP verification.");
            return;
        }

        String qualification = text(qualificationField);
        int experience = parseInt(text(experienceField), -1);
        Date dob = null;
        String gender = value(genderCombo);
        String conduct = value(conductCombo);
        String remarks = text(conductRemarksArea);
        int classId = selectedId(classOptions, value(studentClassCombo));
        int parentUserId = selectedId(parentOptions, value(parentCombo));
        String relation = text(relationField);

        if ("Teacher".equalsIgnoreCase(role) && (qualification.isEmpty() || experience < 0)) {
            statusLabel.setText("Teacher creation requires qualification and non-negative experience.");
            return;
        }
        if ("Student".equalsIgnoreCase(role)) {
            if (dobPicker.getValue() == null || classId <= 0 || parentUserId <= 0 || relation.isEmpty()) {
                statusLabel.setText("Student creation requires DOB, class, parent, and relation.");
                return;
            }
            dob = Date.valueOf(dobPicker.getValue());
        }

        boolean created = userDAO.createUserAsAdmin(
            currentAdmin.getUserId(),
            role,
            name,
            email,
            AuthService.hashPassword(password),
            phone,
            qualification,
            Math.max(experience, 0),
            dob,
            gender.isEmpty() ? null : gender,
            conduct.isEmpty() ? "Good" : conduct,
            remarks,
            classId,
            parentUserId,
            relation
        );

        if (!created) {
            statusLabel.setText("Failed to create user. Check duplicates and mandatory role data.");
            return;
        }

        clearCreateForm();
        loadOptions();
        loadUsers();
        statusLabel.setText("User created successfully.");
    }

    @FXML
    private void handleSaveSelectedUser() {
        AdminUserRecord selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            statusLabel.setText("Select a user first.");
            return;
        }
        String name = text(editNameField);
        String email = text(editEmailField);
        if (name.isEmpty() || !looksLikeEmail(email)) {
            statusLabel.setText("Selected user needs a name and valid email.");
            return;
        }
        boolean updated = userDAO.updateAdminManagedUserBasic(
            currentAdmin.getUserId(),
            selectedUser.getUserId(),
            name,
            email,
            text(editPhoneField)
        );
        if (!updated) {
            statusLabel.setText("Update blocked.");
            return;
        }
        loadUsers();
        statusLabel.setText("User updated successfully.");
    }

    @FXML
    private void handleToggleActive() {
        AdminUserRecord selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            statusLabel.setText("Select a user first.");
            return;
        }
        boolean newState = selectedUser.getIsActive() == 0;
        boolean updated = userDAO.setUserActiveStatus(currentAdmin.getUserId(), selectedUser.getUserId(), newState);
        if (!updated) {
            statusLabel.setText("Status change blocked.");
            return;
        }
        loadUsers();
        statusLabel.setText(newState ? "User reactivated." : "User deactivated.");
    }

    @FXML
    private void handleRefresh() {
        loadOptions();
        loadUsers();
        statusLabel.setText("User list refreshed.");
    }

    @FXML
    private void backToDashboard() {
        if (AdminDashboardController.getInstance() != null) {
            AdminDashboardController.getInstance().scrollToTop();
        }
    }

    private void clearCreateForm() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        phoneField.clear();
        qualificationField.clear();
        experienceField.clear();
        relationField.clear();
        conductRemarksArea.clear();
        roleCombo.getSelectionModel().select("Student");
    }

    private void updateActiveButton(AdminUserRecord selectedUser) {
        if (activeToggleButton == null) {
            return;
        }
        if (selectedUser == null) {
            activeToggleButton.setDisable(true);
            activeToggleButton.setText("Set Status");
            return;
        }
        if (currentAdmin != null
            && "Admin".equalsIgnoreCase(selectedUser.getRoleName())
            && selectedUser.getUserId() != currentAdmin.getUserId()) {
            activeToggleButton.setDisable(true);
            activeToggleButton.setText("Set Status");
            return;
        }
        activeToggleButton.setDisable(false);
        activeToggleButton.setText(selectedUser.getIsActive() == 1 ? "Deactivate User" : "Reactivate User");
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String text(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private String value(ComboBox<String> comboBox) {
        return comboBox.getValue() == null ? "" : comboBox.getValue().trim();
    }

    private boolean looksLikeEmail(String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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
