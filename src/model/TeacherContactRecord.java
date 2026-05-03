package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TeacherContactRecord {
    private final IntegerProperty teacherUserId;
    private final IntegerProperty teacherId;
    private final StringProperty teacherName;
    private final StringProperty subjectName;
    private final StringProperty contact;
    private final IntegerProperty unreadCount;

    public TeacherContactRecord(int teacherUserId, int teacherId, String teacherName, String subjectName, String contact) {
        this(teacherUserId, teacherId, teacherName, subjectName, contact, 0);
    }

    public TeacherContactRecord(int teacherUserId, int teacherId, String teacherName, String subjectName, String contact, int unreadCount) {
        this.teacherUserId = new SimpleIntegerProperty(teacherUserId);
        this.teacherId = new SimpleIntegerProperty(teacherId);
        this.teacherName = new SimpleStringProperty(teacherName);
        this.subjectName = new SimpleStringProperty(subjectName);
        this.contact = new SimpleStringProperty(contact);
        this.unreadCount = new SimpleIntegerProperty(unreadCount);
    }

    public int getTeacherUserId() { return teacherUserId.get(); }
    public IntegerProperty teacherUserIdProperty() { return teacherUserId; }

    public int getTeacherId() { return teacherId.get(); }
    public IntegerProperty teacherIdProperty() { return teacherId; }

    public String getTeacherName() { return teacherName.get(); }
    public StringProperty teacherNameProperty() { return teacherName; }

    public String getSubjectName() { return subjectName.get(); }
    public StringProperty subjectNameProperty() { return subjectName; }

    public String getContact() { return contact.get(); }
    public StringProperty contactProperty() { return contact; }

    public int getUnreadCount() { return unreadCount.get(); }
    public void setUnreadCount(int value) { unreadCount.set(value); }
    public IntegerProperty unreadCountProperty() { return unreadCount; }
    public String getUnreadDisplay() { return getUnreadCount() <= 0 ? "" : String.valueOf(getUnreadCount()); }
}
