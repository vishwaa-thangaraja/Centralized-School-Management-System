package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class QuestionBankRecord {
    private final IntegerProperty questionId;
    private final IntegerProperty classId;
    private final IntegerProperty subjectId;
    private final StringProperty title;
    private final StringProperty subjectName;
    private final StringProperty classDisplay;
    private final StringProperty academicYear;
    private final StringProperty teacherName;
    private final StringProperty uploadedAt;
    private final StringProperty originalFileName;

    public QuestionBankRecord(
            int questionId,
            int classId,
            int subjectId,
            String title,
            String subjectName,
            String classDisplay,
            String academicYear,
            String teacherName,
            String uploadedAt,
            String originalFileName
    ) {
        this.questionId = new SimpleIntegerProperty(questionId);
        this.classId = new SimpleIntegerProperty(classId);
        this.subjectId = new SimpleIntegerProperty(subjectId);
        this.title = new SimpleStringProperty(title);
        this.subjectName = new SimpleStringProperty(subjectName);
        this.classDisplay = new SimpleStringProperty(classDisplay);
        this.academicYear = new SimpleStringProperty(academicYear);
        this.teacherName = new SimpleStringProperty(teacherName);
        this.uploadedAt = new SimpleStringProperty(uploadedAt);
        this.originalFileName = new SimpleStringProperty(originalFileName);
    }

    public int getQuestionId() { return questionId.get(); }
    public IntegerProperty questionIdProperty() { return questionId; }

    public int getClassId() { return classId.get(); }
    public IntegerProperty classIdProperty() { return classId; }

    public int getSubjectId() { return subjectId.get(); }
    public IntegerProperty subjectIdProperty() { return subjectId; }

    public String getTitle() { return title.get(); }
    public StringProperty titleProperty() { return title; }

    public String getSubjectName() { return subjectName.get(); }
    public StringProperty subjectNameProperty() { return subjectName; }

    public String getClassDisplay() { return classDisplay.get(); }
    public StringProperty classDisplayProperty() { return classDisplay; }

    public String getAcademicYear() { return academicYear.get(); }
    public StringProperty academicYearProperty() { return academicYear; }

    public String getTeacherName() { return teacherName.get(); }
    public StringProperty teacherNameProperty() { return teacherName; }

    public String getUploadedAt() { return uploadedAt.get(); }
    public StringProperty uploadedAtProperty() { return uploadedAt; }

    public String getOriginalFileName() { return originalFileName.get(); }
    public StringProperty originalFileNameProperty() { return originalFileName; }
}
