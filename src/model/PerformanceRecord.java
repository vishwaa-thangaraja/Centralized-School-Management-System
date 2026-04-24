package model;

import javafx.beans.property.*;

public class PerformanceRecord {
    private final StringProperty subject;
    private final DoubleProperty marksObtained;
    private final StringProperty examType;

    public PerformanceRecord(String subject, double marksObtained, String examType) {
        this.subject = new SimpleStringProperty(subject);
        this.marksObtained = new SimpleDoubleProperty(marksObtained);
        this.examType = new SimpleStringProperty(examType);
    }

    public String getSubject() { return subject.get(); }
    public StringProperty subjectProperty() { return subject; }

    public double getMarksObtained() { return marksObtained.get(); }
    public DoubleProperty marksObtainedProperty() { return marksObtained; }

    public String getExamType() { return examType.get(); }
    public StringProperty examTypeProperty() { return examType; }
}