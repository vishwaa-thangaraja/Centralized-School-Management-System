package controller;

import dao.UserDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.CommunicationMessage;
import model.CounsellorContactRecord;
import model.User;
import javafx.util.Duration;

public class StudentCounsellorConnectController implements LiveRefreshController {

    @FXML private Label studentContextLabel;
    @FXML private Label selectedCounsellorLabel;
    @FXML private Label counsellorInfoLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<CounsellorContactRecord> counsellorTable;
    @FXML private TableColumn<CounsellorContactRecord, String> counsellorNameCol;
    @FXML private TableColumn<CounsellorContactRecord, String> emailCol;
    @FXML private TableColumn<CounsellorContactRecord, String> phoneCol;
    @FXML private TableColumn<CounsellorContactRecord, String> notifyCol;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextArea messageInput;

    private final UserDAO userDAO = new UserDAO();
    private User currentStudentUser;
    private int currentStudentId = -1;
    private CounsellorContactRecord selectedCounsellor;
    private Timeline liveRefreshTimeline;
    private boolean suppressSelectionListener = false;
    private static final int LIVE_REFRESH_SECONDS = 2;

    @FXML
    public void initialize() {
        counsellorNameCol.setCellValueFactory(new PropertyValueFactory<>("counsellorName"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        notifyCol.setCellValueFactory(new PropertyValueFactory<>("unreadDisplay"));
        notifyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: CENTER; -fx-background-radius: 14;");
                }
            }
        });
        counsellorTable.setItems(FXCollections.observableArrayList());
        counsellorTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressSelectionListener) {
                return;
            }
            selectedCounsellor = newVal;
            loadChatHistory();
        });
    }

    public void initData(User studentUser) {
        this.currentStudentUser = studentUser;
        this.currentStudentId = userDAO.getStudentIdByUserId(studentUser.getUserId());
        this.selectedCounsellor = null;
        messageInput.clear();
        studentContextLabel.setText("Student: " + studentUser.getName());
        loadCounsellorDirectory();
        startLiveRefresh();
    }

    private void loadCounsellorDirectory() {
        if (currentStudentUser == null || currentStudentId <= 0) {
            studentContextLabel.setText("Student context missing");
            selectedCounsellorLabel.setText("Select counsellor");
            counsellorInfoLabel.setText("Email: - | Phone: -");
            statusLabel.setText("Student context unavailable.");
            counsellorTable.getItems().clear();
            chatMessagesBox.getChildren().clear();
            return;
        }

        ObservableList<CounsellorContactRecord> contacts = userDAO.getCounsellorContactsForStudent(currentStudentUser.getUserId(), currentStudentId);
        counsellorTable.setItems(contacts);
        counsellorTable.getSelectionModel().clearSelection();
        selectedCounsellorLabel.setText("Select counsellor");
        counsellorInfoLabel.setText("Email: - | Phone: -");
        chatMessagesBox.getChildren().clear();
        statusLabel.setText(contacts.isEmpty() ? "No counsellors available." : "Select counsellor to chat.");
    }

    private void refreshCounsellorDirectoryPreservingSelection() {
        if (currentStudentUser == null || currentStudentId <= 0) {
            return;
        }

        int previousCounsellorId = selectedCounsellor != null ? selectedCounsellor.getCounsellorUserId() : -1;
        ObservableList<CounsellorContactRecord> contacts = userDAO.getCounsellorContactsForStudent(currentStudentUser.getUserId(), currentStudentId);

        suppressSelectionListener = true;
        counsellorTable.setItems(contacts);
        counsellorTable.getSelectionModel().clearSelection();
        selectedCounsellor = null;
        for (CounsellorContactRecord counsellor : contacts) {
            if (counsellor.getCounsellorUserId() == previousCounsellorId) {
                selectedCounsellor = counsellor;
                counsellorTable.getSelectionModel().select(counsellor);
                break;
            }
        }
        suppressSelectionListener = false;

        if (selectedCounsellor != null) {
            loadChatHistory();
        } else {
            counsellorTable.refresh();
        }
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().refreshMessageNotifications();
        }
    }

    private void startLiveRefresh() {
        stopLiveRefresh();
        liveRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(LIVE_REFRESH_SECONDS), event -> refreshCounsellorDirectoryPreservingSelection()));
        liveRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        liveRefreshTimeline.play();
    }

    @Override
    public void stopLiveRefresh() {
        if (liveRefreshTimeline != null) {
            liveRefreshTimeline.stop();
            liveRefreshTimeline = null;
        }
    }

    private void loadChatHistory() {
        chatMessagesBox.getChildren().clear();
        if (currentStudentUser == null || currentStudentId <= 0 || selectedCounsellor == null) {
            selectedCounsellorLabel.setText("Select counsellor");
            counsellorInfoLabel.setText("Email: - | Phone: -");
            return;
        }

        selectedCounsellorLabel.setText(selectedCounsellor.getCounsellorName());
        counsellorInfoLabel.setText("Email: " + selectedCounsellor.getEmail() + " | Phone: " + selectedCounsellor.getPhone());

        ObservableList<CommunicationMessage> messages = userDAO.getCounsellorChatHistoryForStudent(
            currentStudentUser.getUserId(),
            selectedCounsellor.getCounsellorUserId(),
            currentStudentId
        );
        userDAO.markStudentCounsellorConversationAsSeen(
            currentStudentUser.getUserId(),
            selectedCounsellor.getCounsellorUserId(),
            currentStudentId
        );
        selectedCounsellor.setUnreadCount(0);
        counsellorTable.refresh();
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().refreshMessageNotifications();
        }

        if (messages.isEmpty()) {
            statusLabel.setText("No conversation yet.");
            return;
        }

        for (CommunicationMessage message : messages) {
            boolean fromStudent = message.getSenderId() == currentStudentUser.getUserId();
            HBox row = new HBox();
            row.setAlignment(fromStudent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubble = new VBox();
            bubble.setSpacing(4);
            bubble.setMaxWidth(430);

            Label msgLabel = new Label(message.getMessageText());
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(430);
            if (fromStudent) {
                msgLabel.setStyle("-fx-background-color: #d1ecf1; -fx-padding: 10 12 10 12; -fx-background-radius: 10;");
            } else {
                msgLabel.setStyle("-fx-background-color: #f8d7da; -fx-padding: 10 12 10 12; -fx-background-radius: 10;");
            }
            Label timeLabel = new Label(message.getSentAt());
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
            bubble.getChildren().addAll(msgLabel, timeLabel);
            row.getChildren().add(bubble);
            chatMessagesBox.getChildren().add(row);
        }

        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        statusLabel.setText("Loaded " + messages.size() + " message(s).");
    }

    @FXML
    private void handleSendMessage() {
        if (currentStudentUser == null || currentStudentId <= 0 || selectedCounsellor == null) {
            statusLabel.setText("Select counsellor first.");
            return;
        }
        String messageText = messageInput.getText() == null ? "" : messageInput.getText().trim();
        if (messageText.isEmpty()) {
            statusLabel.setText("Message cannot be empty.");
            return;
        }
        if (messageText.length() > 1000) {
            statusLabel.setText("Message must be 1000 characters or less.");
            return;
        }

        boolean sent = userDAO.sendMessageToCounsellorFromStudent(
            currentStudentUser.getUserId(),
            selectedCounsellor.getCounsellorUserId(),
            currentStudentId,
            messageText
        );
        if (!sent) {
            statusLabel.setText("Failed to send message.");
            return;
        }
        messageInput.clear();
        loadChatHistory();
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().refreshMessageNotifications();
        }
        statusLabel.setText("Message sent successfully.");
    }

    @FXML
    private void handleClearChat() {
        if (currentStudentUser == null || currentStudentId <= 0 || selectedCounsellor == null) {
            statusLabel.setText("Select counsellor first.");
            return;
        }
        if (DialogSupport.confirm(messageInput, "Clear Chat", "Delete full conversation?\n\nThis will permanently delete all counsellor chat messages for your profile.")
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        boolean cleared = userDAO.clearChatHistoryForStudentCounsellor(
            currentStudentUser.getUserId(),
            selectedCounsellor.getCounsellorUserId(),
            currentStudentId
        );
        if (!cleared) {
            statusLabel.setText("Failed to clear chat.");
            return;
        }
        messageInput.clear();
        loadCounsellorDirectory();
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().refreshMessageNotifications();
        }
        statusLabel.setText("Chat cleared successfully.");
    }

    @FXML
    private void backToDashboard() {
        stopLiveRefresh();
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().scrollToTop();
        }
    }
}
