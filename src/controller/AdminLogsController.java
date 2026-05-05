package controller;

import dao.LoginAuditDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.AdminAuditRecord;
import model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminLogsController {

    @FXML private Label adminContextLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<AdminAuditRecord> auditTable;
    @FXML private TableColumn<AdminAuditRecord, AdminAuditRecord> profilePicCol;
    @FXML private TableColumn<AdminAuditRecord, Number> logIdCol;
    @FXML private TableColumn<AdminAuditRecord, String> userCol;
    @FXML private TableColumn<AdminAuditRecord, String> roleCol;
    @FXML private TableColumn<AdminAuditRecord, String> loginCol;
    @FXML private TableColumn<AdminAuditRecord, String> logoutCol;
    @FXML private TableColumn<AdminAuditRecord, String> ipCol;

    private final LoginAuditDAO loginAuditDAO = new LoginAuditDAO();
    private User currentAdmin;

    @FXML
    public void initialize() {
        auditTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        profilePicCol.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        profilePicCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(AdminAuditRecord record, boolean empty) {
                super.updateItem(record, empty);
                if (empty || record == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setText(null);
                setGraphic(ProfileImageSupport.createAvatarNode(
                    record.getProfileImageData(),
                    record.getFallbackInitial(),
                    34.0
                ));
            }
        });
        logIdCol.setCellValueFactory(new PropertyValueFactory<>("logId"));
        userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        loginCol.setCellValueFactory(new PropertyValueFactory<>("loginTime"));
        logoutCol.setCellValueFactory(new PropertyValueFactory<>("logoutTime"));
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
    }

    public void initData(User adminUser) {
        this.currentAdmin = adminUser;
        adminContextLabel.setText("Admin: " + adminUser.getName());
        loadLogs();
    }

    private void loadLogs() {
        auditTable.setItems(loginAuditDAO.getLoginAuditRecords());
        statusLabel.setText("Showing latest login audit entries.");
    }

    @FXML
    private void handleRefresh() {
        loadLogs();
    }

    @FXML
    private void handleDeleteSelected() {
        ObservableList<AdminAuditRecord> selectedRecords = auditTable.getSelectionModel().getSelectedItems();
        if (selectedRecords == null || selectedRecords.isEmpty()) {
            statusLabel.setText("Select one or more logs to delete.");
            return;
        }

        if (!confirm("Delete selected logs?", "This will remove only the selected login audit entries.")) {
            return;
        }

        List<Integer> selectedLogIds = new ArrayList<>();
        for (AdminAuditRecord record : selectedRecords) {
            selectedLogIds.add(record.getLogId());
        }

        int deletedCount = loginAuditDAO.deleteLoginAudits(selectedLogIds);
        loadLogs();
        statusLabel.setText("Deleted " + deletedCount + " selected log(s).");
    }

    @FXML
    private void handleClearAll() {
        if (!confirm("Clear entire login log?", "This will remove every login audit entry.")) {
            return;
        }

        int deletedCount = loginAuditDAO.clearLoginAudits();
        loadLogs();
        statusLabel.setText("Cleared " + deletedCount + " login audit log(s).");
    }

    @FXML
    private void backToDashboard() {
        if (AdminDashboardController.getInstance() != null) {
            AdminDashboardController.getInstance().scrollToTop();
        }
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
