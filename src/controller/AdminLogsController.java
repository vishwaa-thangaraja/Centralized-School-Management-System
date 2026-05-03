package controller;

import dao.LoginAuditDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.AdminAuditRecord;
import model.User;

public class AdminLogsController {

    @FXML private Label adminContextLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<AdminAuditRecord> auditTable;
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
    private void backToDashboard() {
        if (AdminDashboardController.getInstance() != null) {
            AdminDashboardController.getInstance().scrollToTop();
        }
    }
}
