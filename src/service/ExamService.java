package service;

import dao.UserDAO;
import javafx.collections.ObservableList;
import model.ExamRecord;

import java.util.Map;

public class ExamService {

    private final UserDAO userDAO = new UserDAO();

    public ObservableList<ExamRecord> getStudentExamRecords(int studentId) {
        ObservableList<ExamRecord> exams = userDAO.getExamsForStudent(studentId);
        Map<Integer, Double> marksByExam = userDAO.getMarksForStudent(studentId);

        // Merge exam metadata with evaluated marks to drive details and chart UI.
        for (ExamRecord exam : exams) {
            Double marks = marksByExam.get(exam.getExamId());
            if (marks != null) {
                exam.setMarksObtained(marks);
                exam.setGrade(calculateGrade(marks, exam.getTotalMarks()));
            } else {
                exam.setGrade("Not Evaluated");
            }
        }
        return exams;
    }

    public ObservableList<ExamRecord> getTeacherExamRecords(int teacherId) {
        return userDAO.getExamsForTeacher(teacherId);
    }

    private String calculateGrade(double marksObtained, int totalMarks) {
        if (totalMarks <= 0) {
            return "-";
        }

        double percentage = (marksObtained / totalMarks) * 100.0;
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }
}
