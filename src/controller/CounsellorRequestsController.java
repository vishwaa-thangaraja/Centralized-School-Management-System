package controller;

import dao.UserDAO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.CounsellingCaseRecord;
import model.User;

public class CounsellorRequestsController {

    @FXML private Label counsellorContextLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<CounsellingCaseRecord> requestsTable;
    @FXML private TableColumn<CounsellingCaseRecord, Number> sessionIdCol;
    @FXML private TableColumn<CounsellingCaseRecord, String> studentNameCol;
    @FXML private TableColumn<CounsellingCaseRecord, String> classCol;
    @FXML private TableColumn<CounsellingCaseRecord, String> dateCol;
    @FXML private TableColumn<CounsellingCaseRecord, String> categoryCol;
    @FXML private TableColumn<CounsellingCaseRecord, String> statusCol;
    @FXML private TableColumn<CounsellingCaseRecord, String> notesCol;
    @FXML private Button acceptButton;
    @FXML private Button completeButton;

    private final UserDAO userDAO = new UserDAO();
    private User currentCounsellor;

    @FXML
    public void initialize() {
        sessionIdCol.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        classCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("sessionDate"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        requestsTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(CounsellingCaseRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if ("Pending".equalsIgnoreCase(item.getStatus())) {
                    setStyle("-fx-background-color: #fff8e1;");
                } else if ("Scheduled".equalsIgnoreCase(item.getStatus())) {
                    setStyle("-fx-background-color: #e8f4fd;");
                } else {
                    setStyle("-fx-background-color: #eef8ee;");
                }
            }
        });

        requestsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> updateActionState(selected));
        updateActionState(null);
    }

    public void initData(User counsellorUser) {
        this.currentCounsellor = counsellorUser;
        counsellorContextLabel.setText("Counsellor: " + counsellorUser.getName());
        loadRequests();
    }

    private void loadRequests() {
        if (currentCounsellor == null) {
            requestsTable.getItems().clear();
            statusLabel.setText("Counsellor context not found.");
            updateActionState(null);
            return;
        }

        ObservableList<CounsellingCaseRecord> requests = userDAO.getCounsellingCasesForCounsellor(currentCounsellor.getUserId());
        requestsTable.setItems(requests);
        requestsTable.getSelectionModel().clearSelection();
        updateActionState(null);
        statusLabel.setText(requests.isEmpty()
            ? "No counselling requests available."
            : "Select a request to accept or complete it.");

        if (CounsellorDashboardController.getInstance() != null) {
            CounsellorDashboardController.getInstance().refreshDashboard();
        }
    }

    private void updateActionState(CounsellingCaseRecord selected) {
        boolean isPending = selected != null && "Pending".equalsIgnoreCase(selected.getStatus());
        boolean isScheduled = selected != null && "Scheduled".equalsIgnoreCase(selected.getStatus());
        acceptButton.setDisable(!isPending);
        completeButton.setDisable(!isScheduled);
    }

    @FXML
    private void handleAcceptRequest() {
        updateSelectedStatus("Scheduled", "Request accepted and scheduled.");
    }

    @FXML
    private void handleCompleteRequest() {
        updateSelectedStatus("Completed", "Counselling case marked completed.");
    }

    private void updateSelectedStatus(String newStatus, String successMessage) {
        CounsellingCaseRecord selected = requestsTable.getSelectionModel().getSelectedItem();
        if (currentCounsellor == null || selected == null) {
            statusLabel.setText("Select a request first.");
            return;
        }

        boolean updated = userDAO.updateCounsellingRequestStatus(
            currentCounsellor.getUserId(),
            selected.getSessionId(),
            newStatus
        );
        if (!updated) {
            statusLabel.setText("Could not update this request.");
            return;
        }

        loadRequests();
        statusLabel.setText(successMessage);
    }

    @FXML
    private void handleRefresh() {
        loadRequests();
    }

    @FXML
    private void backToDashboard() {
        if (CounsellorDashboardController.getInstance() != null) {
            CounsellorDashboardController.getInstance().scrollToTop();
        }
    }
}
