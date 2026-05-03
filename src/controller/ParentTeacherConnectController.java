package controller;

import dao.UserDAO;
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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.CommunicationMessage;
import model.Student;
import model.TeacherContactRecord;
import model.User;

public class ParentTeacherConnectController implements ParentWardContextAware {

    @FXML private Label wardContextLabel;
    @FXML private Label selectedTeacherLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<TeacherContactRecord> teacherTable;
    @FXML private TableColumn<TeacherContactRecord, String> teacherNameCol;
    @FXML private TableColumn<TeacherContactRecord, String> subjectCol;
    @FXML private TableColumn<TeacherContactRecord, String> contactCol;
    @FXML private TableColumn<TeacherContactRecord, String> notifyCol;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextArea messageInput;

    private final UserDAO userDAO = new UserDAO();
    private User currentParent;
    private Student selectedWard;
    private TeacherContactRecord selectedTeacher;

    @FXML
    public void initialize() {
        teacherNameCol.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
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
        teacherTable.setItems(FXCollections.observableArrayList());

        teacherTable.getSelectionModel().selectedItemProperty().addListener((obs, oldTeacher, newTeacher) -> {
            selectedTeacher = newTeacher;
            loadChatHistory();
        });
    }

    @Override
    public void updateContext(User parentUser, Student ward) {
        this.currentParent = parentUser;
        this.selectedWard = ward;
        this.selectedTeacher = null;
        messageInput.clear();
        loadTeacherDirectory();
    }

    private void loadTeacherDirectory() {
        if (currentParent == null || selectedWard == null) {
            wardContextLabel.setText("No ward selected");
            selectedTeacherLabel.setText("Select a teacher");
            teacherTable.getItems().clear();
            chatMessagesBox.getChildren().clear();
            statusLabel.setText("Select a ward from the sidebar to connect with teachers.");
            return;
        }

        wardContextLabel.setText(selectedWard.getName() + " | " + selectedWard.getClassDisplay());
        ObservableList<TeacherContactRecord> teachers = userDAO.getWardTeachersForParent(currentParent.getUserId(), selectedWard.getStudentId());
        for (TeacherContactRecord teacher : teachers) {
            teacher.setUnreadCount(userDAO.getParentUnreadCountForTeacher(
                currentParent.getUserId(),
                teacher.getTeacherUserId(),
                selectedWard.getStudentId()
            ));
        }
        teacherTable.setItems(teachers);
        teacherTable.getSelectionModel().clearSelection();
        selectedTeacherLabel.setText("Select a teacher");
        chatMessagesBox.getChildren().clear();
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshTeacherConnectNotification();
        }
        statusLabel.setText(teachers.isEmpty()
            ? "No teachers mapped for this ward's class."
            : "Select a teacher to view discussion history.");
    }

    private void loadChatHistory() {
        chatMessagesBox.getChildren().clear();

        if (selectedTeacher == null || selectedWard == null || currentParent == null) {
            selectedTeacherLabel.setText("Select a teacher");
            return;
        }

        selectedTeacherLabel.setText(selectedTeacher.getTeacherName() + " (" + selectedTeacher.getSubjectName() + ")");
        ObservableList<CommunicationMessage> messages = userDAO.getChatHistory(
            currentParent.getUserId(),
            selectedTeacher.getTeacherUserId(),
            selectedWard.getStudentId()
        );

        userDAO.markParentConversationAsSeen(
            currentParent.getUserId(),
            selectedTeacher.getTeacherUserId(),
            selectedWard.getStudentId()
        );
        selectedTeacher.setUnreadCount(0);
        teacherTable.refresh();
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshTeacherConnectNotification();
        }

        if (messages.isEmpty()) {
            statusLabel.setText("No conversation yet. Start with a short query.");
            return;
        }

        for (CommunicationMessage message : messages) {
            boolean fromParent = message.getSenderId() == currentParent.getUserId();
            HBox row = new HBox();
            row.setAlignment(fromParent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox bubbleBox = new VBox();
            bubbleBox.setSpacing(4);
            bubbleBox.setMaxWidth(420);

            Label messageLabel = new Label(message.getMessageText());
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(420);
            if (fromParent) {
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
        if (currentParent == null || selectedWard == null) {
            statusLabel.setText("Select a ward first.");
            return;
        }
        if (selectedTeacher == null) {
            statusLabel.setText("Select a teacher before sending a message.");
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

        boolean sent = userDAO.sendMessageToTeacher(
            currentParent.getUserId(),
            selectedTeacher.getTeacherUserId(),
            selectedWard.getStudentId(),
            messageText
        );

        if (!sent) {
            statusLabel.setText("Message send failed. Check ward scope and teacher mapping.");
            return;
        }

        messageInput.clear();
        loadChatHistory();
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshTeacherConnectNotification();
        }
        statusLabel.setText("Message sent successfully.");
    }

    @FXML
    private void handleClearChat() {
        if (currentParent == null || selectedWard == null) {
            statusLabel.setText("Select a ward first.");
            return;
        }
        if (selectedTeacher == null) {
            statusLabel.setText("Select a teacher before clearing chat.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Chat");
        confirm.setHeaderText("Delete entire conversation?");
        confirm.setContentText("This will permanently delete all messages with this teacher for the selected ward.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean cleared = userDAO.clearChatHistoryForParent(
            currentParent.getUserId(),
            selectedTeacher.getTeacherUserId(),
            selectedWard.getStudentId()
        );

        if (!cleared) {
            statusLabel.setText("Failed to clear chat. Check scope/permissions.");
            return;
        }

        messageInput.clear();
        loadTeacherDirectory();
        selectedTeacherLabel.setText("Select a teacher");
        statusLabel.setText("Chat cleared successfully.");
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().refreshTeacherConnectNotification();
        }
    }

    @FXML
    private void backToDashboard() {
        if (ParentDashboardController.getInstance() != null) {
            ParentDashboardController.getInstance().scrollToTop();
        }
    }
}
