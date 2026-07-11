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
import model.CounsellorInboxRecord;
import model.User;
import javafx.util.Duration;

public class CounsellorChatController implements LiveRefreshController {

    @FXML private Label counsellorContextLabel;
    @FXML private Label selectedParticipantLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<CounsellorInboxRecord> participantTable;
    @FXML private TableColumn<CounsellorInboxRecord, String> participantNameCol;
    @FXML private TableColumn<CounsellorInboxRecord, String> typeCol;
    @FXML private TableColumn<CounsellorInboxRecord, String> studentCol;
    @FXML private TableColumn<CounsellorInboxRecord, String> classCol;
    @FXML private TableColumn<CounsellorInboxRecord, String> contactCol;
    @FXML private TableColumn<CounsellorInboxRecord, String> notifyCol;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextArea messageInput;

    private final UserDAO userDAO = new UserDAO();
    private User currentCounsellor;
    private CounsellorInboxRecord selectedThread;
    private Timeline liveRefreshTimeline;
    private boolean suppressSelectionListener = false;
    private static final int LIVE_REFRESH_SECONDS = 2;

    @FXML
    public void initialize() {
        participantNameCol.setCellValueFactory(new PropertyValueFactory<>("participantName"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("participantType"));
        studentCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        classCol.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));
        contactCol.setCellValueFactory(new PropertyValueFactory<>("contact"));
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

        participantTable.setItems(FXCollections.observableArrayList());
        participantTable.getSelectionModel().selectedItemProperty().addListener((obs, oldThread, newThread) -> {
            if (suppressSelectionListener) {
                return;
            }
            selectedThread = newThread;
            loadChatHistory();
        });
    }

    public void initData(User counsellorUser) {
        this.currentCounsellor = counsellorUser;
        this.selectedThread = null;
        messageInput.clear();
        counsellorContextLabel.setText("Counsellor: " + counsellorUser.getName());
        loadDirectory();
        startLiveRefresh();
    }

    private void loadDirectory() {
        if (currentCounsellor == null) {
            participantTable.getItems().clear();
            chatMessagesBox.getChildren().clear();
            selectedParticipantLabel.setText("Select a participant");
            statusLabel.setText("Counsellor context not found.");
            return;
        }

        ObservableList<CounsellorInboxRecord> participants = userDAO.getCounsellorInboxForCounsellor(currentCounsellor.getUserId());
        participantTable.setItems(participants);
        selectedParticipantLabel.setText("Select a participant");
        chatMessagesBox.getChildren().clear();

        if (participants.isEmpty()) {
            statusLabel.setText("No parent/student participants available.");
        } else {
            statusLabel.setText("Select a participant to view conversation.");
        }

        if (CounsellorDashboardController.getInstance() != null) {
            CounsellorDashboardController.getInstance().refreshDashboard();
        }
    }

    private void refreshDirectoryPreservingSelection() {
        if (currentCounsellor == null) {
            return;
        }

        int previousTargetId = selectedThread != null ? selectedThread.getTargetUserId() : -1;
        int previousStudentId = selectedThread != null ? selectedThread.getStudentId() : -1;
        ObservableList<CounsellorInboxRecord> participants = userDAO.getCounsellorInboxForCounsellor(currentCounsellor.getUserId());

        suppressSelectionListener = true;
        participantTable.setItems(participants);
        participantTable.getSelectionModel().clearSelection();
        selectedThread = null;
        for (CounsellorInboxRecord participant : participants) {
            if (participant.getTargetUserId() == previousTargetId && participant.getStudentId() == previousStudentId) {
                selectedThread = participant;
                participantTable.getSelectionModel().select(participant);
                break;
            }
        }
        suppressSelectionListener = false;

        if (selectedThread != null) {
            loadChatHistory();
        } else {
            participantTable.refresh();
        }
        if (CounsellorDashboardController.getInstance() != null) {
            CounsellorDashboardController.getInstance().refreshDashboard();
        }
    }

    private void startLiveRefresh() {
        stopLiveRefresh();
        liveRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(LIVE_REFRESH_SECONDS), event -> refreshDirectoryPreservingSelection()));
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

        if (currentCounsellor == null || selectedThread == null) {
            selectedParticipantLabel.setText("Select a participant");
            return;
        }

        selectedParticipantLabel.setText(
            selectedThread.getParticipantName() + " (" + selectedThread.getParticipantType() + ") | " +
            selectedThread.getStudentName()
        );

        ObservableList<CommunicationMessage> messages = userDAO.getCounsellorChatHistoryForCounsellor(
            currentCounsellor.getUserId(),
            selectedThread.getTargetUserId(),
            selectedThread.getStudentId()
        );

        userDAO.markCounsellorConversationAsSeen(
            currentCounsellor.getUserId(),
            selectedThread.getTargetUserId(),
            selectedThread.getStudentId()
        );
        selectedThread.setUnreadCount(0);
        participantTable.refresh();
        if (CounsellorDashboardController.getInstance() != null) {
            CounsellorDashboardController.getInstance().refreshDashboard();
        }

        if (messages.isEmpty()) {
            statusLabel.setText("No messages yet in this conversation.");
            return;
        }

        for (CommunicationMessage message : messages) {
            boolean fromCounsellor = message.getSenderId() == currentCounsellor.getUserId();
            HBox row = new HBox();
            row.setAlignment(fromCounsellor ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubbleBox = new VBox();
            bubbleBox.setSpacing(4);
            bubbleBox.setMaxWidth(430);

            Label messageLabel = new Label(message.getMessageText());
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(430);
            if (fromCounsellor) {
                messageLabel.setStyle("-fx-background-color: #d1ecf1; -fx-padding: 10 12 10 12; -fx-background-radius: 10;");
            } else {
                messageLabel.setStyle("-fx-background-color: #f8d7da; -fx-padding: 10 12 10 12; -fx-background-radius: 10;");
            }

            Label timeLabel = new Label(message.getSentAt());
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");

            bubbleBox.getChildren().addAll(messageLabel, timeLabel);
            row.getChildren().add(bubbleBox);
            chatMessagesBox.getChildren().add(row);
        }

        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        statusLabel.setText("Loaded " + messages.size() + " message(s).");
    }

    @FXML
    private void handleSendMessage() {
        if (currentCounsellor == null) {
            statusLabel.setText("Counsellor session not found.");
            return;
        }
        if (selectedThread == null) {
            statusLabel.setText("Select a participant first.");
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

        boolean sent = userDAO.sendMessageFromCounsellor(
            currentCounsellor.getUserId(),
            selectedThread.getTargetUserId(),
            selectedThread.getStudentId(),
            messageText
        );
        if (!sent) {
            statusLabel.setText("Failed to send message.");
            return;
        }

        messageInput.clear();
        loadChatHistory();
        statusLabel.setText("Message sent successfully.");
    }

    @FXML
    private void handleClearChat() {
        if (currentCounsellor == null || selectedThread == null) {
            statusLabel.setText("Select a participant first.");
            return;
        }
        if (DialogSupport.confirm(messageInput, "Clear Chat", "Delete full conversation?\n\nThis will permanently delete all messages for this participant and student context.")
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean cleared = userDAO.clearChatHistoryForCounsellor(
            currentCounsellor.getUserId(),
            selectedThread.getTargetUserId(),
            selectedThread.getStudentId()
        );
        if (!cleared) {
            statusLabel.setText("Failed to clear chat.");
            return;
        }

        messageInput.clear();
        loadDirectory();
        selectedThread = null;
        statusLabel.setText("Chat cleared successfully.");
    }

    @FXML
    private void backToDashboard() {
        stopLiveRefresh();
        if (CounsellorDashboardController.getInstance() != null) {
            CounsellorDashboardController.getInstance().scrollToTop();
        }
    }
}
