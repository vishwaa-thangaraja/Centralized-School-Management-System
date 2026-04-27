package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AssignmentRecord {
    private final IntegerProperty assignmentId;
    private final IntegerProperty classId;
    private final IntegerProperty subjectId;
    private final StringProperty title;
    private final StringProperty description;
    private final StringProperty dueDate;
    private final StringProperty subjectName;
    private final StringProperty classDisplay;
    private final StringProperty status;
    private final IntegerProperty submittedCount;
    private final IntegerProperty pendingCount;

    public AssignmentRecord(
            int assignmentId,
            int classId,
            int subjectId,
            String title,
            String description,
            String dueDate,
            String subjectName,
            String classDisplay,
            String status,
            int submittedCount,
            int pendingCount
    ) {
        this.assignmentId = new SimpleIntegerProperty(assignmentId);
        this.classId = new SimpleIntegerProperty(classId);
        this.subjectId = new SimpleIntegerProperty(subjectId);
        this.title = new SimpleStringProperty(title);
        this.description = new SimpleStringProperty(description);
        this.dueDate = new SimpleStringProperty(dueDate);
        this.subjectName = new SimpleStringProperty(subjectName);
        this.classDisplay = new SimpleStringProperty(classDisplay);
        this.status = new SimpleStringProperty(status);
        this.submittedCount = new SimpleIntegerProperty(submittedCount);
        this.pendingCount = new SimpleIntegerProperty(pendingCount);
    }

    public int getAssignmentId() { return assignmentId.get(); }
    public IntegerProperty assignmentIdProperty() { return assignmentId; }

    public int getClassId() { return classId.get(); }
    public IntegerProperty classIdProperty() { return classId; }

    public int getSubjectId() { return subjectId.get(); }
    public IntegerProperty subjectIdProperty() { return subjectId; }

    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }

    public String getDueDate() { return dueDate.get(); }
    public StringProperty dueDateProperty() { return dueDate; }

    public String getSubjectName() { return subjectName.get(); }
    public StringProperty subjectNameProperty() { return subjectName; }

    public String getClassDisplay() { return classDisplay.get(); }
    public StringProperty classDisplayProperty() { return classDisplay; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    public int getSubmittedCount() { return submittedCount.get(); }
    public IntegerProperty submittedCountProperty() { return submittedCount; }

    public int getPendingCount() { return pendingCount.get(); }
    public IntegerProperty pendingCountProperty() { return pendingCount; }
}
