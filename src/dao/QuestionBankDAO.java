package dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.QuestionBankRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class QuestionBankDAO {
    public int getNextQuestionId() {
        try (Connection conn = DBConnection.getConnection()) {
            ensureInfrastructure(conn);
            return SequenceService.nextVal(conn, "SEQ_QUESTION_BANK", "QUESTION_BANK", "QUESTION_ID");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean createQuestionPaper(
            int teacherUserId,
            int questionId,
            int classId,
            int subjectId,
            String title,
            String originalFileName
    ) {
        String insertQuery = "INSERT INTO QUESTION_BANK " +
                             "(question_id, class_id, subject_id, teacher_id, title, academic_year, original_file_name, uploaded_at) " +
                             "SELECT ?, ?, ?, t.teacher_id, ?, c.academic_year, ?, SYSDATE " +
                             "FROM TEACHERS t JOIN CLASSES c ON c.class_id = ? " +
                             "WHERE t.user_id = ? AND EXISTS (" +
                             "SELECT 1 FROM CLASS_SUBJECT_TEACHER cst " +
                             "WHERE cst.teacher_id = t.teacher_id AND cst.class_id = ? AND cst.subject_id = ?)";
        try (Connection conn = DBConnection.getConnection()) {
            ensureInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setInt(1, questionId);
                pstmt.setInt(2, classId);
                pstmt.setInt(3, subjectId);
                pstmt.setString(4, title);
                pstmt.setString(5, originalFileName);
                pstmt.setInt(6, classId);
                pstmt.setInt(7, teacherUserId);
                pstmt.setInt(8, classId);
                pstmt.setInt(9, subjectId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<QuestionBankRecord> getTeacherQuestionPapers(int teacherUserId) {
        ObservableList<QuestionBankRecord> records = FXCollections.observableArrayList();
        String query = baseSelect() +
                       "WHERE t.user_id = ? " +
                       "ORDER BY qb.uploaded_at DESC, qb.question_id DESC";
        try (Connection conn = DBConnection.getConnection()) {
            ensureInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, teacherUserId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    records.add(toRecord(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public ObservableList<QuestionBankRecord> getStudentQuestionPapers(int studentUserId) {
        ObservableList<QuestionBankRecord> records = FXCollections.observableArrayList();
        String query = baseSelect() +
                       "WHERE qb.class_id IN (" +
                       "SELECT sc.class_id FROM STUDENT_CLASS sc " +
                       "JOIN STUDENTS stu ON sc.student_id = stu.student_id " +
                       "WHERE stu.user_id = ?) " +
                       "ORDER BY qb.uploaded_at DESC, qb.question_id DESC";
        try (Connection conn = DBConnection.getConnection()) {
            ensureInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, studentUserId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    records.add(toRecord(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    private String baseSelect() {
        return "SELECT qb.question_id, qb.class_id, qb.subject_id, qb.title, " +
               "s.subject_name, c.class_name || '-' || c.section || ' (' || c.academic_year || ')' AS class_display, " +
               "c.academic_year, NVL(u.name, '-') AS teacher_name, " +
               "TO_CHAR(qb.uploaded_at, 'YYYY-MM-DD HH24:MI') AS uploaded_at, " +
               "NVL(qb.original_file_name, '-') AS original_file_name " +
               "FROM QUESTION_BANK qb " +
               "JOIN CLASSES c ON qb.class_id = c.class_id " +
               "JOIN SUBJECTS s ON qb.subject_id = s.subject_id " +
               "LEFT JOIN TEACHERS t ON qb.teacher_id = t.teacher_id " +
               "LEFT JOIN USERS u ON t.user_id = u.user_id ";
    }

    private QuestionBankRecord toRecord(ResultSet rs) throws SQLException {
        return new QuestionBankRecord(
            rs.getInt("question_id"),
            rs.getInt("class_id"),
            rs.getInt("subject_id"),
            rs.getString("title"),
            rs.getString("subject_name"),
            rs.getString("class_display"),
            rs.getString("academic_year"),
            rs.getString("teacher_name"),
            rs.getString("uploaded_at"),
            rs.getString("original_file_name")
        );
    }

    private void ensureInfrastructure(Connection conn) throws SQLException {
        if (!tableExists(conn, "QUESTION_BANK")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE QUESTION_BANK (" +
                    "QUESTION_ID NUMBER PRIMARY KEY, " +
                    "CLASS_ID NUMBER NOT NULL, " +
                    "SUBJECT_ID NUMBER NOT NULL, " +
                    "TEACHER_ID NUMBER, " +
                    "TITLE VARCHAR2(200) NOT NULL, " +
                    "ACADEMIC_YEAR VARCHAR2(10) NOT NULL, " +
                    "ORIGINAL_FILE_NAME VARCHAR2(255), " +
                    "UPLOADED_AT DATE DEFAULT SYSDATE, " +
                    "CONSTRAINT FK_QB_CLASS FOREIGN KEY (CLASS_ID) REFERENCES CLASSES(CLASS_ID), " +
                    "CONSTRAINT FK_QB_SUBJECT FOREIGN KEY (SUBJECT_ID) REFERENCES SUBJECTS(SUBJECT_ID), " +
                    "CONSTRAINT FK_QB_TEACHER FOREIGN KEY (TEACHER_ID) REFERENCES TEACHERS(TEACHER_ID)" +
                    ")"
                );
            }
        }
        SequenceService.ensureSequence(conn, "SEQ_QUESTION_BANK", "QUESTION_BANK", "QUESTION_ID");
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, tableName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
