package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ExamRecord {
    private final IntegerProperty examId;
    private final IntegerProperty classId;
    private final IntegerProperty subjectId;
    private final StringProperty examTitle;
    private final StringProperty description;
    private final StringProperty subjectName;
    private final StringProperty classDisplay;
    private final StringProperty examDate;
    private final IntegerProperty totalMarks;
    private final StringProperty teacherName;
    private final StringProperty status;
    private final ObjectProperty<Double> marksObtained;
    private final StringProperty grade;

    public ExamRecord(
            int examId,
            int classId,
            int subjectId,
            String examTitle,
            String description,
            String subjectName,
            String classDisplay,
            String examDate,
            int totalMarks,
            String teacherName,
            String status,
            Double marksObtained,
            String grade
    ) {
        this.examId = new SimpleIntegerProperty(examId);
        this.classId = new SimpleIntegerProperty(classId);
        this.subjectId = new SimpleIntegerProperty(subjectId);
        this.examTitle = new SimpleStringProperty(examTitle);
        this.description = new SimpleStringProperty(description);
        this.subjectName = new SimpleStringProperty(subjectName);
        this.classDisplay = new SimpleStringProperty(classDisplay);
        this.examDate = new SimpleStringProperty(examDate);
        this.totalMarks = new SimpleIntegerProperty(totalMarks);
        this.teacherName = new SimpleStringProperty(teacherName);
        this.status = new SimpleStringProperty(status);
        this.marksObtained = new SimpleObjectProperty<>(marksObtained);
        this.grade = new SimpleStringProperty(grade);
    }

    public int getExamId() { return examId.get(); }
    public IntegerProperty examIdProperty() { return examId; }

    public int getClassId() { return classId.get(); }
    public IntegerProperty classIdProperty() { return classId; }

    public int getSubjectId() { return subjectId.get(); }
    public IntegerProperty subjectIdProperty() { return subjectId; }

    public String getExamTitle() { return examTitle.get(); }
    public StringProperty examTitleProperty() { return examTitle; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }

    public String getSubjectName() { return subjectName.get(); }
    public StringProperty subjectNameProperty() { return subjectName; }

    public String getClassDisplay() { return classDisplay.get(); }
    public StringProperty classDisplayProperty() { return classDisplay; }

    public String getExamDate() { return examDate.get(); }
    public StringProperty examDateProperty() { return examDate; }

    public int getTotalMarks() { return totalMarks.get(); }
    public IntegerProperty totalMarksProperty() { return totalMarks; }

    public String getTeacherName() { return teacherName.get(); }
    public StringProperty teacherNameProperty() { return teacherName; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    public void setStatus(String value) { status.set(value); }

    public Double getMarksObtained() { return marksObtained.get(); }
    public ObjectProperty<Double> marksObtainedProperty() { return marksObtained; }

    public void setMarksObtained(Double value) { marksObtained.set(value); }

    public String getMarksDisplay() {
        Double marks = getMarksObtained();
        return marks == null ? "Not Evaluated" : String.format("%.1f", marks);
    }

    public String getGrade() { return grade.get(); }
    public StringProperty gradeProperty() { return grade; }

    public void setGrade(String value) { grade.set(value); }
}
