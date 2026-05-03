package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AdminTeacherMappingRecord {
    private final IntegerProperty classId;
    private final IntegerProperty subjectId;
    private final IntegerProperty teacherId;
    private final StringProperty classDisplay;
    private final StringProperty subjectName;
    private final StringProperty teacherName;

    public AdminTeacherMappingRecord(int classId, int subjectId, int teacherId, String classDisplay, String subjectName, String teacherName) {
        this.classId = new SimpleIntegerProperty(classId);
        this.subjectId = new SimpleIntegerProperty(subjectId);
        this.teacherId = new SimpleIntegerProperty(teacherId);
        this.classDisplay = new SimpleStringProperty(classDisplay);
        this.subjectName = new SimpleStringProperty(subjectName);
        this.teacherName = new SimpleStringProperty(teacherName);
    }

    public int getClassId() { return classId.get(); }
    public IntegerProperty classIdProperty() { return classId; }

    public int getSubjectId() { return subjectId.get(); }
    public IntegerProperty subjectIdProperty() { return subjectId; }

    public int getTeacherId() { return teacherId.get(); }
    public IntegerProperty teacherIdProperty() { return teacherId; }

    public String getClassDisplay() { return classDisplay.get(); }
    public StringProperty classDisplayProperty() { return classDisplay; }

    public String getSubjectName() { return subjectName.get(); }
    public StringProperty subjectNameProperty() { return subjectName; }

    public String getTeacherName() { return teacherName.get(); }
    public StringProperty teacherNameProperty() { return teacherName; }
}
