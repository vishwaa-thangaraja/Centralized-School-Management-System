package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ParentChatThreadRecord {
    private final IntegerProperty parentUserId;
    private final IntegerProperty studentId;
    private final StringProperty parentName;
    private final StringProperty studentName;
    private final StringProperty classDisplay;
    private final StringProperty contact;
    private final IntegerProperty unreadCount;

    public ParentChatThreadRecord(
        int parentUserId,
        int studentId,
        String parentName,
        String studentName,
        String classDisplay,
        String contact,
        int unreadCount
    ) {
        this.parentUserId = new SimpleIntegerProperty(parentUserId);
        this.studentId = new SimpleIntegerProperty(studentId);
        this.parentName = new SimpleStringProperty(parentName);
        this.studentName = new SimpleStringProperty(studentName);
        this.classDisplay = new SimpleStringProperty(classDisplay);
        this.contact = new SimpleStringProperty(contact);
        this.unreadCount = new SimpleIntegerProperty(unreadCount);
    }

    public int getParentUserId() { return parentUserId.get(); }
    public IntegerProperty parentUserIdProperty() { return parentUserId; }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public String getParentName() { return parentName.get(); }
    public StringProperty parentNameProperty() { return parentName; }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    public String getClassDisplay() { return classDisplay.get(); }
    public StringProperty classDisplayProperty() { return classDisplay; }

    public String getContact() { return contact.get(); }
    public StringProperty contactProperty() { return contact; }

    public int getUnreadCount() { return unreadCount.get(); }
    public void setUnreadCount(int value) { unreadCount.set(value); }
    public IntegerProperty unreadCountProperty() { return unreadCount; }

    public String getUnreadDisplay() {
        return getUnreadCount() <= 0 ? "0" : String.valueOf(getUnreadCount());
    }
}
