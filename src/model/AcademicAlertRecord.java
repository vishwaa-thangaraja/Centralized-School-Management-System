package model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AcademicAlertRecord {
    private final StringProperty subjectName;
    private final StringProperty examTitle;
    private final StringProperty examDate;
    private final DoubleProperty marksObtained;
    private final DoubleProperty maxMarks;

    public AcademicAlertRecord(String subjectName, String examTitle, String examDate, double marksObtained, double maxMarks) {
        this.subjectName = new SimpleStringProperty(subjectName);
        this.examTitle = new SimpleStringProperty(examTitle);
        this.examDate = new SimpleStringProperty(examDate);
        this.marksObtained = new SimpleDoubleProperty(marksObtained);
        this.maxMarks = new SimpleDoubleProperty(maxMarks);
    }

    public String getSubjectName() { return subjectName.get(); }
    public StringProperty subjectNameProperty() { return subjectName; }

    public String getExamTitle() { return examTitle.get(); }
    public StringProperty examTitleProperty() { return examTitle; }

    public String getExamDate() { return examDate.get(); }
    public StringProperty examDateProperty() { return examDate; }

    public double getMarksObtained() { return marksObtained.get(); }
    public DoubleProperty marksObtainedProperty() { return marksObtained; }

    public double getMaxMarks() { return maxMarks.get(); }
    public DoubleProperty maxMarksProperty() { return maxMarks; }

    public String getMarksDisplay() {
        return String.format("%.1f / %.1f", getMarksObtained(), getMaxMarks());
    }
}
