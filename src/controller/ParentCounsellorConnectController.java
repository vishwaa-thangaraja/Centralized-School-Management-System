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
import model.Student;
import model.User;
import javafx.util.Duration;

public class ParentCounsellorConnectController implements ParentWardContextAware, LiveRefreshController {

    @FXML private Label wardContextLabel;
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
    private User currentParent;
    private Student selectedWard;
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

    @Override
    public void updateContext(User parentUser, Student ward) {
        this.currentParent = parentUser;
        this.selectedWard = ward;
        this.selectedCounsellor = null;
        messageInput.clear();
        loadCounsellorDirectory();
        startLiveRefresh();
    }

    private void loadCounsellorDirectory() {
        if (currentParent == null || selectedWard == null) {
            wardContextLabel.setText("No ward selected");
            selectedCounsellorLabel.setText("Select counsellor");
            counsellorInfoLabel.setText("Email: - | Phone: -");
            statusLabel.setText("Select ward in sidebar.");
            counsellorTable.getItems().clear();
            chatMessagesBox.getChildren().clear();
            return;
        }

        wardContextLabel.setText(selectedWard.getName() + " | " + selectedWard.getClassDisplay());
        ObservableList<CounsellorContactRecord> list = userDAO.getCounsellorContactsForParent(currentParent.getUserId(), selectedWard.getStudentId());
        counsellorTable.setItems(list);
        counsellorTable.getSelectionModel().clearSelection();
        selectedCounsellorLabel.setText("Select counsellor");
        counsellorInfoLabel.setText("Email: - | Phone: -");
        chatMessagesBox.getChildren().clear();
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshCounsellorConnectNotification();
        }
        statusLabel.setText(list.isEmpty() ? "No counsellors available." : "Select counsellor to chat.");
    }

    private void refreshCounsellorDirectoryPreservingSelection() {
        if (currentParent == null || selectedWard == null) {
            return;
        }

        int previousCounsellorId = selectedCounsellor != null ? selectedCounsellor.getCounsellorUserId() : -1;
        ObservableList<CounsellorContactRecord> list = userDAO.getCounsellorContactsForParent(currentParent.getUserId(), selectedWard.getStudentId());

        suppressSelectionListener = true;
        counsellorTable.setItems(list);
        counsellorTable.getSelectionModel().clearSelection();
        selectedCounsellor = null;
        for (CounsellorContactRecord counsellor : list) {
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
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshCounsellorConnectNotification();
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
        if (currentParent == null || selectedWard == null || selectedCounsellor == null) {
            selectedCounsellorLabel.setText("Select counsellor");
            counsellorInfoLabel.setText("Email: - | Phone: -");
            return;
        }

        selectedCounsellorLabel.setText(selectedCounsellor.getCounsellorName());
        counsellorInfoLabel.setText("Email: " + selectedCounsellor.getEmail() + " | Phone: " + selectedCounsellor.getPhone());

        ObservableList<CommunicationMessage> messages = userDAO.getCounsellorChatHistoryForParent(
            currentParent.getUserId(),
            selectedCounsellor.getCounsellorUserId(),
            selectedWard.getStudentId()
        );
        userDAO.markParentCounsellorConversationAsSeen(
            currentParent.getUserId(),
            selectedCounsellor.getCounsellorUserId(),
            selectedWard.getStudentId()
        );
        selectedCounsellor.setUnreadCount(0);
        counsellorTable.refresh();
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshCounsellorConnectNotification();
        }

        if (messages.isEmpty()) {
            statusLabel.setText("No conversation yet.");
            return;
        }

        for (CommunicationMessage message : messages) {
            boolean fromParent = message.getSenderId() == currentParent.getUserId();
            HBox row = new HBox();
            row.setAlignment(fromParent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubble = new VBox();
            bubble.setSpacing(4);
            bubble.setMaxWidth(430);

            Label textLabel = new Label(message.getMessageText());
            textLabel.setWrapText(true);
            textLabel.setMaxWidth(430);
            if (fromParent) {
                textLabel.setStyle("-fx-background-color: #d1ecf1; -fx-padding: 10 12 10 12; -fx-background-radius: 10;");
            } else {
                textLabel.setStyle("-fx-background-color: #f8d7da; -fx-padding: 10 12 10 12; -fx-background-radius: 10;");
            }
            Label timeLabel = new Label(message.getSentAt());
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
            bubble.getChildren().addAll(textLabel, timeLabel);
            row.getChildren().add(bubble);
            chatMessagesBox.getChildren().add(row);
        }

        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        statusLabel.setText("Loaded " + messages.size() + " message(s).");
    }

    @FXML
    private void handleSendMessage() {
        if (currentParent == null || selectedWard == null || selectedCounsellor == null) {
            statusLabel.setText("Select ward and counsellor first.");
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

        boolean sent = userDAO.sendMessageToCounsellorFromParent(
            currentParent.getUserId(),
            selectedCounsellor.getCounsellorUserId(),
            selectedWard.getStudentId(),
            messageText
        );
        if (!sent) {
            statusLabel.setText("Failed to send message.");
            return;
        }

        messageInput.clear();
        loadChatHistory();
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshCounsellorConnectNotification();
        }
        statusLabel.setText("Message sent successfully.");
    }

    @FXML
    private void handleClearChat() {
        if (currentParent == null || selectedWard == null || selectedCounsellor == null) {
            statusLabel.setText("Select ward and counsellor first.");
            return;
        }
        if (DialogSupport.confirm(messageInput, "Clear Chat", "Delete full conversation?\n\nThis will permanently delete counsellor chat history for this ward.")
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean cleared = userDAO.clearChatHistoryForParentCounsellor(
            currentParent.getUserId(),
            selectedCounsellor.getCounsellorUserId(),
            selectedWard.getStudentId()
        );
        if (!cleared) {
            statusLabel.setText("Failed to clear chat.");
            return;
        }

        messageInput.clear();
        loadCounsellorDirectory();
        statusLabel.setText("Chat cleared successfully.");
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshCounsellorConnectNotification();
        }
    }

    @FXML
    private void backToDashboard() {
        stopLiveRefresh();
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().scrollToTop();
        }
    }
}
