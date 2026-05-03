package controller;

import dao.UserDAO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import model.CounsellorContactRecord;
import model.Student;
import model.User;

import java.sql.Date;
import java.time.LocalDate;

public class CounsellingRequestController implements ParentWardContextAware {

    @FXML private Label contextLabel;
    @FXML private Label counsellorContactLabel;
    @FXML private DatePicker preferredDatePicker;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextArea concernArea;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();
    private User requesterUser;
    private int targetStudentId = -1;
    private boolean parentMode = false;

    @FXML
    public void initialize() {
        categoryCombo.getItems().setAll("Academic", "Personal", "Career");
        categoryCombo.getSelectionModel().selectFirst();
        preferredDatePicker.setValue(LocalDate.now().plusDays(1));
    }

    public void initForStudent(User studentUser) {
        this.requesterUser = studentUser;
        this.parentMode = false;
        this.targetStudentId = userDAO.getStudentIdByUserId(studentUser.getUserId());
        contextLabel.setText("Student: " + studentUser.getName());
        loadCounsellorContactForStudent();
    }

    @Override
    public void updateContext(User parentUser, Student selectedWard) {
        this.requesterUser = parentUser;
        this.parentMode = true;
        this.targetStudentId = selectedWard != null ? selectedWard.getStudentId() : -1;
        contextLabel.setText(selectedWard == null
            ? "Parent: " + parentUser.getName()
            : "Parent: " + parentUser.getName() + " | Ward: " + selectedWard.getName());
        loadCounsellorContactForParent(selectedWard);
    }

    private void loadCounsellorContactForStudent() {
        if (requesterUser == null || targetStudentId <= 0) {
            counsellorContactLabel.setText("Counsellor Contact: Not available");
            return;
        }
        ObservableList<CounsellorContactRecord> contacts = userDAO.getCounsellorContactsForStudent(requesterUser.getUserId(), targetStudentId);
        if (contacts.isEmpty()) {
            counsellorContactLabel.setText("Counsellor Contact: Not assigned");
            return;
        }
        CounsellorContactRecord contact = contacts.get(0);
        counsellorContactLabel.setText("Counsellor: " + contact.getCounsellorName() + " | " + contact.getEmail() + " | " + contact.getPhone());
    }

    private void loadCounsellorContactForParent(Student ward) {
        if (requesterUser == null || ward == null) {
            counsellorContactLabel.setText("Counsellor Contact: Select ward first");
            return;
        }
        ObservableList<CounsellorContactRecord> contacts = userDAO.getCounsellorContactsForParent(requesterUser.getUserId(), ward.getStudentId());
        if (contacts.isEmpty()) {
            counsellorContactLabel.setText("Counsellor Contact: Not assigned");
            return;
        }
        CounsellorContactRecord contact = contacts.get(0);
        counsellorContactLabel.setText("Counsellor: " + contact.getCounsellorName() + " | " + contact.getEmail() + " | " + contact.getPhone());
    }

    @FXML
    private void handleSubmitRequest() {
        if (requesterUser == null || targetStudentId <= 0) {
            statusLabel.setText("Requester context not available.");
            return;
        }
        LocalDate preferredDate = preferredDatePicker.getValue();
        if (preferredDate == null) {
            statusLabel.setText("Select preferred date.");
            return;
        }
        if (preferredDate.isBefore(LocalDate.now())) {
            statusLabel.setText("Preferred date cannot be in the past.");
            return;
        }
        String category = categoryCombo.getValue();
        if (category == null || category.isBlank()) {
            statusLabel.setText("Select category.");
            return;
        }
        String concern = concernArea.getText() == null ? "" : concernArea.getText().trim();
        if (concern.isEmpty()) {
            statusLabel.setText("Primary concern cannot be empty.");
            return;
        }
        if (concern.length() > 500) {
            statusLabel.setText("Primary concern must be 500 characters or less.");
            return;
        }

        boolean created = userDAO.createCounsellingRequest(
            requesterUser.getUserId(),
            targetStudentId,
            Date.valueOf(preferredDate),
            category,
            concern
        );
        if (!created) {
            statusLabel.setText("Failed to create counselling request.");
            return;
        }

        concernArea.clear();
        preferredDatePicker.setValue(LocalDate.now().plusDays(1));
        categoryCombo.getSelectionModel().selectFirst();
        statusLabel.setText("Counselling request submitted successfully.");
    }

    @FXML
    private void backToDashboard() {
        if (parentMode) {
            if (ParentDashboardController.getInstance() != null) {
                ParentDashboardController.getInstance().scrollToTop();
            }
            return;
        }
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().scrollToTop();
        }
    }
}
