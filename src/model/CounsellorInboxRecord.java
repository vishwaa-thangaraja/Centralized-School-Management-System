package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CounsellorInboxRecord {
    private final IntegerProperty targetUserId;
    private final IntegerProperty studentId;
    private final StringProperty participantName;
    private final StringProperty participantType;
    private final StringProperty studentName;
    private final StringProperty classDisplay;
    private final StringProperty contact;
    private final IntegerProperty unreadCount;

    public CounsellorInboxRecord(
        int targetUserId,
        int studentId,
        String participantName,
        String participantType,
        String studentName,
        String classDisplay,
        String contact,
        int unreadCount
    ) {
        this.targetUserId = new SimpleIntegerProperty(targetUserId);
        this.studentId = new SimpleIntegerProperty(studentId);
        this.participantName = new SimpleStringProperty(participantName);
        this.participantType = new SimpleStringProperty(participantType);
        this.studentName = new SimpleStringProperty(studentName);
        this.classDisplay = new SimpleStringProperty(classDisplay);
        this.contact = new SimpleStringProperty(contact);
        this.unreadCount = new SimpleIntegerProperty(unreadCount);
    }

    public int getTargetUserId() { return targetUserId.get(); }
    public IntegerProperty targetUserIdProperty() { return targetUserId; }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public String getParticipantName() { return participantName.get(); }
    public StringProperty participantNameProperty() { return participantName; }

    public String getParticipantType() { return participantType.get(); }
    public StringProperty participantTypeProperty() { return participantType; }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    public String getClassDisplay() { return classDisplay.get(); }
    public StringProperty classDisplayProperty() { return classDisplay; }

    public String getContact() { return contact.get(); }
    public StringProperty contactProperty() { return contact; }

    public int getUnreadCount() { return unreadCount.get(); }
    public void setUnreadCount(int unreadCount) { this.unreadCount.set(unreadCount); }
    public IntegerProperty unreadCountProperty() { return unreadCount; }
    public String getUnreadDisplay() { return getUnreadCount() <= 0 ? "" : String.valueOf(getUnreadCount()); }
}
