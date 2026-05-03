package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AdminParentLinkRecord {
    private final IntegerProperty parentId;
    private final IntegerProperty studentId;
    private final StringProperty parentName;
    private final StringProperty studentName;
    private final StringProperty relation;

    public AdminParentLinkRecord(int parentId, int studentId, String parentName, String studentName, String relation) {
        this.parentId = new SimpleIntegerProperty(parentId);
        this.studentId = new SimpleIntegerProperty(studentId);
        this.parentName = new SimpleStringProperty(parentName);
        this.studentName = new SimpleStringProperty(studentName);
        this.relation = new SimpleStringProperty(relation);
    }

    public int getParentId() { return parentId.get(); }
    public IntegerProperty parentIdProperty() { return parentId; }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public String getParentName() { return parentName.get(); }
    public StringProperty parentNameProperty() { return parentName; }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    public String getRelation() { return relation.get(); }
    public StringProperty relationProperty() { return relation; }
}
