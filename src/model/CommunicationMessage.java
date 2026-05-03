package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CommunicationMessage {
    private final IntegerProperty messageId;
    private final IntegerProperty senderId;
    private final IntegerProperty receiverId;
    private final IntegerProperty studentId;
    private final StringProperty messageText;
    private final StringProperty sentAt;

    public CommunicationMessage(int messageId, int senderId, int receiverId, int studentId, String messageText, String sentAt) {
        this.messageId = new SimpleIntegerProperty(messageId);
        this.senderId = new SimpleIntegerProperty(senderId);
        this.receiverId = new SimpleIntegerProperty(receiverId);
        this.studentId = new SimpleIntegerProperty(studentId);
        this.messageText = new SimpleStringProperty(messageText);
        this.sentAt = new SimpleStringProperty(sentAt);
    }

    public int getMessageId() { return messageId.get(); }
    public IntegerProperty messageIdProperty() { return messageId; }

    public int getSenderId() { return senderId.get(); }
    public IntegerProperty senderIdProperty() { return senderId; }

    public int getReceiverId() { return receiverId.get(); }
    public IntegerProperty receiverIdProperty() { return receiverId; }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public String getMessageText() { return messageText.get(); }
    public StringProperty messageTextProperty() { return messageText; }

    public String getSentAt() { return sentAt.get(); }
    public StringProperty sentAtProperty() { return sentAt; }
}
