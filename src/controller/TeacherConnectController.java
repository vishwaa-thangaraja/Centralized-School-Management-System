package controller;

import dao.UserDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.CommunicationMessage;
import model.ParentChatThreadRecord;
import model.User;
import javafx.util.Duration;

public class TeacherConnectController implements LiveRefreshController {

    @FXML private Label teacherContextLabel;
    @FXML private Label selectedParentLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<ParentChatThreadRecord> parentThreadTable;
    @FXML private TableColumn<ParentChatThreadRecord, String> parentNameCol;
    @FXML private TableColumn<ParentChatThreadRecord, String> wardNameCol;
    @FXML private TableColumn<ParentChatThreadRecord, String> classCol;
    @FXML private TableColumn<ParentChatThreadRecord, String> contactCol;
    @FXML private TableColumn<ParentChatThreadRecord, String> notifyCol;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextArea messageInput;

    private final UserDAO userDAO = new UserDAO();
    private User currentTeacher;
    private ParentChatThreadRecord selectedThread;
    private boolean suppressSelectionListener = false;
    private Timeline liveRefreshTimeline;
    private static final int LIVE_REFRESH_SECONDS = 2;

    @FXML
    public void initialize() {
        parentNameCol.setCellValueFactory(new PropertyValueFactory<>("parentName"));
        wardNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
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
        parentThreadTable.setItems(FXCollections.observableArrayList());

        parentThreadTable.getSelectionModel().selectedItemProperty().addListener((obs, oldThread, newThread) -> {
            if (suppressSelectionListener) {
                return;
            }
            selectedThread = newThread;
            loadChatHistory();
        });
    }

    public void initData(User teacherUser) {
        this.currentTeacher = teacherUser;
        this.selectedThread = null;
        messageInput.clear();
        teacherContextLabel.setText("Teacher: " + teacherUser.getName());
        refreshSidebarNotification();
        loadInboxThreads();
        startLiveRefresh();
    }

    private void loadInboxThreads() {
        if (currentTeacher == null) {
            parentThreadTable.getItems().clear();
            chatMessagesBox.getChildren().clear();
            selectedParentLabel.setText("Select parent conversation");
            statusLabel.setText("Teacher context not loaded.");
            return;
        }

        int previousParentId = selectedThread != null ? selectedThread.getParentUserId() : -1;
        int previousStudentId = selectedThread != null ? selectedThread.getStudentId() : -1;
        ObservableList<ParentChatThreadRecord> threads = userDAO.getTeacherParentInboxThreads(currentTeacher.getUserId());
        suppressSelectionListener = true;
        parentThreadTable.setItems(threads);
        parentThreadTable.getSelectionModel().clearSelection();
        suppressSelectionListener = false;

        selectedThread = null;
        for (ParentChatThreadRecord thread : threads) {
            if (thread.getParentUserId() == previousParentId && thread.getStudentId() == previousStudentId) {
                selectedThread = thread;
                break;
            }
        }
        if (selectedThread != null) {
            parentThreadTable.getSelectionModel().select(selectedThread);
        } else if (!threads.isEmpty()) {
            parentThreadTable.getSelectionModel().selectFirst();
            selectedThread = parentThreadTable.getSelectionModel().getSelectedItem();
        } else {
            selectedParentLabel.setText("Select parent conversation");
            chatMessagesBox.getChildren().clear();
            statusLabel.setText("No parent threads available yet.");
        }
    }

    private void refreshInboxThreadsPreservingSelection() {
        if (currentTeacher == null) {
            return;
        }

        int previousParentId = selectedThread != null ? selectedThread.getParentUserId() : -1;
        int previousStudentId = selectedThread != null ? selectedThread.getStudentId() : -1;
        ObservableList<ParentChatThreadRecord> threads = userDAO.getTeacherParentInboxThreads(currentTeacher.getUserId());

        suppressSelectionListener = true;
        parentThreadTable.setItems(threads);
        parentThreadTable.getSelectionModel().clearSelection();
        selectedThread = null;
        for (ParentChatThreadRecord thread : threads) {
            if (thread.getParentUserId() == previousParentId && thread.getStudentId() == previousStudentId) {
                selectedThread = thread;
                parentThreadTable.getSelectionModel().select(thread);
                break;
            }
        }
        suppressSelectionListener = false;

        if (selectedThread != null) {
            loadChatHistory();
        } else {
            parentThreadTable.refresh();
        }
        refreshSidebarNotification();
    }

    private void startLiveRefresh() {
        stopLiveRefresh();
        liveRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(LIVE_REFRESH_SECONDS), event -> refreshInboxThreadsPreservingSelection()));
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

        if (currentTeacher == null || selectedThread == null) {
            selectedParentLabel.setText("Select parent conversation");
            return;
        }

        selectedParentLabel.setText(
            selectedThread.getParentName() + " | " +
            selectedThread.getStudentName() + " | " +
            selectedThread.getClassDisplay()
        );

        ObservableList<CommunicationMessage> messages = userDAO.getChatHistoryForTeacher(
            currentTeacher.getUserId(),
            selectedThread.getParentUserId(),
            selectedThread.getStudentId()
        );

        userDAO.markTeacherConversationAsSeen(
            currentTeacher.getUserId(),
            selectedThread.getParentUserId(),
            selectedThread.getStudentId()
        );
        selectedThread.setUnreadCount(0);
        parentThreadTable.refresh();
        refreshSidebarNotification();

        if (messages.isEmpty()) {
            statusLabel.setText("No conversation yet for this parent/ward.");
            return;
        }

        for (CommunicationMessage message : messages) {
            boolean fromTeacher = message.getSenderId() == currentTeacher.getUserId();
            HBox row = new HBox();
            row.setAlignment(fromTeacher ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubbleBox = new VBox();
            bubbleBox.setSpacing(4);
            bubbleBox.setMaxWidth(420);

            Label messageLabel = new Label(message.getMessageText());
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(420);
            if (fromTeacher) {
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
        if (currentTeacher == null) {
            statusLabel.setText("Teacher session not found.");
            return;
        }
        if (selectedThread == null) {
            statusLabel.setText("Select a parent thread before sending.");
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

        boolean sent = userDAO.sendMessageToParent(
            currentTeacher.getUserId(),
            selectedThread.getParentUserId(),
            selectedThread.getStudentId(),
            messageText
        );

        if (!sent) {
            statusLabel.setText("Message send failed. Check parent/ward scope.");
            return;
        }

        messageInput.clear();
        loadChatHistory();
        refreshSidebarNotification();
        statusLabel.setText("Message sent successfully.");
    }

    @FXML
    private void handleClearChat() {
        if (currentTeacher == null) {
            statusLabel.setText("Teacher session not found.");
            return;
        }
        if (selectedThread == null) {
            statusLabel.setText("Select a parent thread before clearing chat.");
            return;
        }

        if (DialogSupport.confirm(messageInput, "Clear Chat", "Delete entire conversation?\n\nThis will permanently delete all messages with this parent for the selected ward.")
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean cleared = userDAO.clearChatHistoryForTeacher(
            currentTeacher.getUserId(),
            selectedThread.getParentUserId(),
            selectedThread.getStudentId()
        );

        if (!cleared) {
            statusLabel.setText("Failed to clear chat. Check scope/permissions.");
            return;
        }

        messageInput.clear();
        loadInboxThreads();
        chatMessagesBox.getChildren().clear();
        selectedParentLabel.setText("Select parent conversation");
        statusLabel.setText("Chat cleared successfully.");
        refreshSidebarNotification();
    }

    private void refreshSidebarNotification() {
        if (TeacherDashboardController.getInstance() != null) {
            TeacherDashboardController.getInstance().refreshTeacherChatNotification();
        }
    }

    @FXML
    private void backToDashboard() {
        stopLiveRefresh();
        if (TeacherDashboardController.getInstance() != null) {
            TeacherDashboardController.getInstance().scrollToTop();
        }
    }
}
