package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AdminStudentClassRecord {
    private final IntegerProperty studentId;
    private final StringProperty studentName;
    private final StringProperty classDisplay;

    public AdminStudentClassRecord(int studentId, String studentName, String classDisplay) {
        this.studentId = new SimpleIntegerProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.classDisplay = new SimpleStringProperty(classDisplay);
    }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    public String getClassDisplay() { return classDisplay.get(); }
    public StringProperty classDisplayProperty() { return classDisplay; }
}
