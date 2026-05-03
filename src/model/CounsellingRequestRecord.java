package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CounsellingRequestRecord {
    private final IntegerProperty sessionId;
    private final IntegerProperty studentId;
    private final IntegerProperty counsellorId;
    private final StringProperty sessionDate;
    private final StringProperty status;
    private final StringProperty category;
    private final StringProperty notes;

    public CounsellingRequestRecord(
        int sessionId,
        int studentId,
        int counsellorId,
        String sessionDate,
        String status,
        String category,
        String notes
    ) {
        this.sessionId = new SimpleIntegerProperty(sessionId);
        this.studentId = new SimpleIntegerProperty(studentId);
        this.counsellorId = new SimpleIntegerProperty(counsellorId);
        this.sessionDate = new SimpleStringProperty(sessionDate);
        this.status = new SimpleStringProperty(status);
        this.category = new SimpleStringProperty(category);
        this.notes = new SimpleStringProperty(notes);
    }

    public int getSessionId() { return sessionId.get(); }
    public IntegerProperty sessionIdProperty() { return sessionId; }

    public int getStudentId() { return studentId.get(); }
    public IntegerProperty studentIdProperty() { return studentId; }

    public int getCounsellorId() { return counsellorId.get(); }
    public IntegerProperty counsellorIdProperty() { return counsellorId; }

    public String getSessionDate() { return sessionDate.get(); }
    public StringProperty sessionDateProperty() { return sessionDate; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    public String getCategory() { return category.get(); }
    public StringProperty categoryProperty() { return category; }

    public String getNotes() { return notes.get(); }
    public StringProperty notesProperty() { return notes; }
}
