package dao;

import java.sql.*;
import java.util.*;
import model.AssignmentRecord;
import model.AttendanceRecord;
import model.PerformanceRecord; // Added for Performance Module
import model.Student;
import model.User;
import javafx.collections.FXCollections; // Added for TableView compatibility
import javafx.collections.ObservableList; // Added for TableView compatibility

public class UserDAO {

    public User validateUser(String email, String passwordHash) {
        String query = "SELECT u.user_id, u.name, u.email, r.role_name " +
                       "FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                       "WHERE u.email = ? AND u.password_hash = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, passwordHash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setRoleName(rs.getString("role_name"));
                return user;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Map<String, String> getStudentProfile(int userId) {
        Map<String, String> profile = new HashMap<>();
        String query = "SELECT s.student_id, c.class_name, c.section " +
                       "FROM STUDENTS s " +
                       "LEFT JOIN STUDENT_CLASS sc ON s.student_id = sc.student_id " +
                       "LEFT JOIN CLASSES c ON sc.class_id = c.class_id " +
                       "WHERE s.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                profile.put("student_id", rs.getString("student_id"));
                profile.put("standard", rs.getString("class_name") != null ? rs.getString("class_name") : "N/A");
                profile.put("section", rs.getString("section") != null ? rs.getString("section") : "-");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return profile;
    }

    public Map<String, String> getTeacherProfile(int userId) {
        Map<String, String> profile = new HashMap<>();
        String query = "SELECT t.teacher_id, NVL(t.qualification, 'Not Provided') AS qualification, " +
                       "NVL(TO_CHAR(t.experience), '0') AS experience " +
                       "FROM TEACHERS t WHERE t.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                profile.put("teacher_id", rs.getString("teacher_id"));
                profile.put("qualification", rs.getString("qualification"));
                profile.put("experience", rs.getString("experience"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return profile;
    }

    public List<AttendanceRecord> getSchoolAttendance(int userId) {
        List<AttendanceRecord> list = new ArrayList<>();
        String query = "SELECT TO_CHAR(TRUNC(attendance_date), 'DD-Mon-YYYY') as a_date, " +
                       "MAX(CASE WHEN session_type = 'FN' THEN status END) as FN, " +
                       "MAX(CASE WHEN session_type = 'AN' THEN status END) as AN " +
                       "FROM ATTENDANCE WHERE user_id = ? " +
                       "GROUP BY TRUNC(attendance_date) " +
                       "ORDER BY TRUNC(attendance_date) DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new AttendanceRecord(
                    rs.getString("a_date"),
                    rs.getString("FN") != null ? rs.getString("FN") : "-",
                    rs.getString("AN") != null ? rs.getString("AN") : "-",
                    "Regular"
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public ObservableList<PerformanceRecord> getStudentMarks(int userId) {
        ObservableList<PerformanceRecord> marksList = FXCollections.observableArrayList();

        // The Triple Join: Marks to Papers, Papers to Subjects, Students to Users
        String query = "SELECT s.subject_name, m.marks_obtained, qp.exam_type " +
                       "FROM MARKS m " +
                       "JOIN QUESTION_PAPERS qp ON m.qp_id = qp.qp_id " +
                       "JOIN SUBJECTS s ON qp.subject_id = s.subject_id " +
                       "JOIN STUDENTS stu ON m.student_id = stu.student_id " +
                       "WHERE stu.user_id = ? " +
                       "ORDER BY qp.exam_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                marksList.add(new PerformanceRecord(
                    rs.getString("subject_name"),
                    rs.getDouble("marks_obtained"),
                    rs.getString("exam_type")
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error in getStudentMarks: " + e.getMessage());
            e.printStackTrace();
        }
        return marksList;
    }

    // -------------------------------------

    public int getTeacherStudentCount(int userId) {
        String query = "SELECT COUNT(DISTINCT sc.student_id) " +
                       "FROM TEACHERS t " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "JOIN STUDENT_CLASS sc ON cst.class_id = sc.class_id " +
                       "WHERE t.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTeacherClassCount(int userId) {
        String query = "SELECT COUNT(DISTINCT cst.class_id) " +
                       "FROM TEACHERS t " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "WHERE t.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTeacherSubjectCount(int userId) {
        String query = "SELECT COUNT(DISTINCT cst.subject_id) " +
                       "FROM TEACHERS t " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "WHERE t.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getTeacherAssignmentCount(int userId) {
        String query = "SELECT COUNT(*) " +
                       "FROM ASSIGNMENTS a " +
                       "WHERE EXISTS (" +
                       "    SELECT 1 FROM TEACHERS t " +
                       "    JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "    WHERE t.user_id = ? AND cst.class_id = a.class_id AND cst.subject_id = a.subject_id" +
                       ")";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public LinkedHashMap<Integer, String> getTeacherClasses(int userId) {
        LinkedHashMap<Integer, String> classes = new LinkedHashMap<>();
        String query = "SELECT DISTINCT c.class_id, " +
                       "c.class_name || '-' || c.section || ' (' || c.academic_year || ')' AS class_display " +
                       "FROM TEACHERS t " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "JOIN CLASSES c ON cst.class_id = c.class_id " +
                       "WHERE t.user_id = ? " +
                       "ORDER BY class_display";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                classes.put(rs.getInt("class_id"), rs.getString("class_display"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classes;
    }

    public LinkedHashMap<Integer, String> getTeacherSubjectsForClass(int userId, int classId) {
        LinkedHashMap<Integer, String> subjects = new LinkedHashMap<>();
        String query = "SELECT DISTINCT s.subject_id, s.subject_name " +
                       "FROM TEACHERS t " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "JOIN SUBJECTS s ON cst.subject_id = s.subject_id " +
                       "WHERE t.user_id = ? AND cst.class_id = ? " +
                       "ORDER BY s.subject_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, classId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                subjects.put(rs.getInt("subject_id"), rs.getString("subject_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return subjects;
    }

    public int getNextAssignmentId() {
        String query = "SELECT NVL(MAX(assignment_id), 0) + 1 FROM ASSIGNMENTS";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public int getNextSubmissionId() {
        String query = "SELECT NVL(MAX(submission_id), 0) + 1 FROM SUBMISSIONS";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public int getStudentIdByUserId(int userId) {
        String query = "SELECT student_id FROM STUDENTS WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public ObservableList<AssignmentRecord> getAssignmentsForTeacher(int userId) {
        ObservableList<AssignmentRecord> assignments = FXCollections.observableArrayList();
        String query = "SELECT a.assignment_id, a.class_id, a.subject_id, a.title, NVL(a.description, '-') AS description, " +
                       "TO_CHAR(a.due_date, 'YYYY-MM-DD') AS due_date, s.subject_name, " +
                       "c.class_name || '-' || c.section || ' (' || c.academic_year || ')' AS class_display, " +
                       "(SELECT COUNT(DISTINCT sub.student_id) FROM SUBMISSIONS sub WHERE sub.assignment_id = a.assignment_id) AS submitted_count, " +
                       "((SELECT COUNT(DISTINCT sc.student_id) FROM STUDENT_CLASS sc WHERE sc.class_id = a.class_id) - " +
                       "(SELECT COUNT(DISTINCT sub.student_id) FROM SUBMISSIONS sub WHERE sub.assignment_id = a.assignment_id)) AS pending_count " +
                       "FROM ASSIGNMENTS a " +
                       "JOIN SUBJECTS s ON a.subject_id = s.subject_id " +
                       "JOIN CLASSES c ON a.class_id = c.class_id " +
                       "WHERE EXISTS (" +
                       "    SELECT 1 FROM TEACHERS t " +
                       "    JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "    WHERE t.user_id = ? AND cst.class_id = a.class_id AND cst.subject_id = a.subject_id" +
                       ") " +
                       "ORDER BY a.due_date DESC, a.assignment_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                assignments.add(new AssignmentRecord(
                    rs.getInt("assignment_id"),
                    rs.getInt("class_id"),
                    rs.getInt("subject_id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("due_date"),
                    rs.getString("subject_name"),
                    rs.getString("class_display"),
                    "Published",
                    rs.getInt("submitted_count"),
                    Math.max(rs.getInt("pending_count"), 0)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return assignments;
    }

    public ObservableList<AssignmentRecord> getAssignmentsForStudent(int userId) {
        ObservableList<AssignmentRecord> assignments = FXCollections.observableArrayList();
        String query = "SELECT a.assignment_id, a.class_id, a.subject_id, a.title, NVL(a.description, '-') AS description, " +
                       "TO_CHAR(a.due_date, 'YYYY-MM-DD') AS due_date, s.subject_name, " +
                       "c.class_name || '-' || c.section || ' (' || c.academic_year || ')' AS class_display, " +
                       "CASE WHEN sub.submission_id IS NULL THEN 'Pending' ELSE 'Submitted' END AS assignment_status " +
                       "FROM STUDENTS stu " +
                       "JOIN STUDENT_CLASS sc ON stu.student_id = sc.student_id " +
                       "JOIN ASSIGNMENTS a ON sc.class_id = a.class_id " +
                       "JOIN SUBJECTS s ON a.subject_id = s.subject_id " +
                       "JOIN CLASSES c ON a.class_id = c.class_id " +
                       "LEFT JOIN SUBMISSIONS sub ON sub.assignment_id = a.assignment_id AND sub.student_id = stu.student_id " +
                       "WHERE stu.user_id = ? " +
                       "ORDER BY a.due_date ASC, a.assignment_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                assignments.add(new AssignmentRecord(
                    rs.getInt("assignment_id"),
                    rs.getInt("class_id"),
                    rs.getInt("subject_id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("due_date"),
                    rs.getString("subject_name"),
                    rs.getString("class_display"),
                    rs.getString("assignment_status"),
                    0,
                    0
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return assignments;
    }

    public LinkedHashMap<Integer, String> getSubmittedStudentsForAssignment(int assignmentId) {
        LinkedHashMap<Integer, String> submittedStudents = new LinkedHashMap<>();
        String query = "SELECT stu.student_id, u.name " +
                       "FROM SUBMISSIONS sub " +
                       "JOIN STUDENTS stu ON sub.student_id = stu.student_id " +
                       "JOIN USERS u ON stu.user_id = u.user_id " +
                       "WHERE sub.assignment_id = ? " +
                       "ORDER BY u.name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, assignmentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                submittedStudents.put(rs.getInt("student_id"), rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return submittedStudents;
    }

    public boolean createAssignmentForTeacher(int teacherUserId, int assignmentId, int classId, int subjectId, String title, String description, String dueDate) {
        String scopeQuery = "SELECT COUNT(*) " +
                            "FROM TEACHERS t " +
                            "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                            "WHERE t.user_id = ? AND cst.class_id = ? AND cst.subject_id = ?";
        String insertQuery = "INSERT INTO ASSIGNMENTS (assignment_id, class_id, subject_id, title, description, due_date) " +
                             "VALUES (?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'))";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement scopeStmt = conn.prepareStatement(scopeQuery)) {
                scopeStmt.setInt(1, teacherUserId);
                scopeStmt.setInt(2, classId);
                scopeStmt.setInt(3, subjectId);
                ResultSet scopeRs = scopeStmt.executeQuery();
                if (!scopeRs.next() || scopeRs.getInt(1) == 0) {
                    return false;
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, assignmentId);
                insertStmt.setInt(2, classId);
                insertStmt.setInt(3, subjectId);
                insertStmt.setString(4, title);
                insertStmt.setString(5, description);
                insertStmt.setString(6, dueDate);
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean submitAssignment(int studentUserId, int assignmentId, int submissionId) {
        String studentQuery = "SELECT student_id FROM STUDENTS WHERE user_id = ?";
        String scopeQuery = "SELECT COUNT(*) " +
                            "FROM STUDENTS stu " +
                            "JOIN STUDENT_CLASS sc ON stu.student_id = sc.student_id " +
                            "JOIN ASSIGNMENTS a ON sc.class_id = a.class_id " +
                            "WHERE stu.user_id = ? AND a.assignment_id = ?";
        String existingQuery = "SELECT submission_id FROM SUBMISSIONS WHERE assignment_id = ? AND student_id = ?";
        String insertQuery = "INSERT INTO SUBMISSIONS (submission_id, assignment_id, student_id, submitted_on, marks) VALUES (?, ?, ?, SYSDATE, NULL)";
        String updateQuery = "UPDATE SUBMISSIONS SET submitted_on = SYSDATE WHERE submission_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            int studentId;
            try (PreparedStatement studentStmt = conn.prepareStatement(studentQuery)) {
                studentStmt.setInt(1, studentUserId);
                ResultSet studentRs = studentStmt.executeQuery();
                if (!studentRs.next()) {
                    return false;
                }
                studentId = studentRs.getInt(1);
            }

            try (PreparedStatement scopeStmt = conn.prepareStatement(scopeQuery)) {
                scopeStmt.setInt(1, studentUserId);
                scopeStmt.setInt(2, assignmentId);
                ResultSet scopeRs = scopeStmt.executeQuery();
                if (!scopeRs.next() || scopeRs.getInt(1) == 0) {
                    return false;
                }
            }

            int existingSubmissionId = -1;
            try (PreparedStatement existingStmt = conn.prepareStatement(existingQuery)) {
                existingStmt.setInt(1, assignmentId);
                existingStmt.setInt(2, studentId);
                ResultSet existingRs = existingStmt.executeQuery();
                if (existingRs.next()) {
                    existingSubmissionId = existingRs.getInt(1);
                }
            }

            if (existingSubmissionId > 0) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, existingSubmissionId);
                    return updateStmt.executeUpdate() > 0;
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, submissionId);
                insertStmt.setInt(2, assignmentId);
                insertStmt.setInt(3, studentId);
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<Student> getStudentsForTeacher(int userId) {
        ObservableList<Student> students = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT s.student_id, u.user_id, NVL(c.class_id, 0) AS class_id, " +
                       "u.name, u.email, NVL(u.phone, '-') AS phone, " +
                       "NVL(c.class_name || '-' || c.section || ' (' || c.academic_year || ')', 'Not Assigned') AS class_display, " +
                       "TO_CHAR(s.dob, 'YYYY-MM-DD') AS dob, NVL(s.gender, '-') AS gender, " +
                       "NVL(s.conduct, '-') AS conduct, NVL(s.conduct_remarks, '-') AS conduct_remarks " +
                       "FROM TEACHERS t " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "JOIN STUDENT_CLASS sc ON cst.class_id = sc.class_id " +
                       "JOIN STUDENTS s ON sc.student_id = s.student_id " +
                       "JOIN USERS u ON s.user_id = u.user_id " +
                       "LEFT JOIN CLASSES c ON sc.class_id = c.class_id " +
                       "WHERE t.user_id = ? " +
                       "ORDER BY u.name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                students.add(new Student(
                    rs.getInt("student_id"),
                    rs.getInt("user_id"),
                    rs.getInt("class_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("class_display"),
                    rs.getString("dob"),
                    rs.getString("gender"),
                    rs.getString("conduct"),
                    rs.getString("conduct_remarks")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public boolean updateStudentDetailsForTeacher(int teacherUserId, Student student, int newClassId) {
        String scopeQuery = "SELECT COUNT(*) " +
                            "FROM TEACHERS t " +
                            "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                            "JOIN STUDENT_CLASS sc ON cst.class_id = sc.class_id " +
                            "WHERE t.user_id = ? AND sc.student_id = ?";
        String targetClassScopeQuery = "SELECT COUNT(*) " +
                                       "FROM TEACHERS t " +
                                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                                       "WHERE t.user_id = ? AND cst.class_id = ?";
        String duplicateEmailQuery = "SELECT COUNT(*) FROM USERS WHERE email = ? AND user_id <> ?";
        String updateUserQuery = "UPDATE USERS SET name = ?, email = ?, phone = ? WHERE user_id = ?";
        String updateStudentQuery = "UPDATE STUDENTS SET dob = TO_DATE(?, 'YYYY-MM-DD'), gender = ?, conduct = ?, conduct_remarks = ? WHERE student_id = ?";
        String updateStudentClassQuery = "UPDATE STUDENT_CLASS SET class_id = ? WHERE student_id = ? AND class_id = ?";
        String insertStudentClassQuery = "INSERT INTO STUDENT_CLASS (student_id, class_id) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement scopeStmt = conn.prepareStatement(scopeQuery)) {
                    scopeStmt.setInt(1, teacherUserId);
                    scopeStmt.setInt(2, student.getStudentId());
                    ResultSet scopeRs = scopeStmt.executeQuery();
                    if (!scopeRs.next() || scopeRs.getInt(1) == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                try (PreparedStatement targetClassStmt = conn.prepareStatement(targetClassScopeQuery)) {
                    targetClassStmt.setInt(1, teacherUserId);
                    targetClassStmt.setInt(2, newClassId);
                    ResultSet targetClassRs = targetClassStmt.executeQuery();
                    if (!targetClassRs.next() || targetClassRs.getInt(1) == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                try (PreparedStatement duplicateStmt = conn.prepareStatement(duplicateEmailQuery)) {
                    duplicateStmt.setString(1, student.getEmail());
                    duplicateStmt.setInt(2, student.getUserId());
                    ResultSet duplicateRs = duplicateStmt.executeQuery();
                    if (duplicateRs.next() && duplicateRs.getInt(1) > 0) {
                        conn.rollback();
                        return false;
                    }
                }

                try (PreparedStatement userStmt = conn.prepareStatement(updateUserQuery);
                     PreparedStatement studentStmt = conn.prepareStatement(updateStudentQuery)) {
                    userStmt.setString(1, student.getName());
                    userStmt.setString(2, student.getEmail());
                    userStmt.setString(3, student.getPhone());
                    userStmt.setInt(4, student.getUserId());
                    userStmt.executeUpdate();

                    studentStmt.setString(1, student.getDob());
                    studentStmt.setString(2, student.getGender());
                    studentStmt.setString(3, student.getConduct());
                    studentStmt.setString(4, student.getConductRemarks());
                    studentStmt.setInt(5, student.getStudentId());
                    studentStmt.executeUpdate();
                }

                if (newClassId != student.getClassId()) {
                    if (student.getClassId() > 0) {
                        try (PreparedStatement classStmt = conn.prepareStatement(updateStudentClassQuery)) {
                            classStmt.setInt(1, newClassId);
                            classStmt.setInt(2, student.getStudentId());
                            classStmt.setInt(3, student.getClassId());
                            int updatedRows = classStmt.executeUpdate();
                            if (updatedRows == 0) {
                                conn.rollback();
                                return false;
                            }
                        }
                    } else {
                        try (PreparedStatement classStmt = conn.prepareStatement(insertStudentClassQuery)) {
                            classStmt.setInt(1, student.getStudentId());
                            classStmt.setInt(2, newClassId);
                            classStmt.executeUpdate();
                        }
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public double getAttendancePercentage(int userId) {
        String query = "SELECT (COUNT(CASE WHEN status = 'Present' THEN 1 END) * 100.0 / COUNT(*)) " +
                       "FROM ATTENDANCE WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public double getAverageMarks(int userId) {
        String query = "SELECT AVG(m.marks_obtained) FROM MARKS m " +
                       "JOIN STUDENTS s ON m.student_id = s.student_id " +
                       "WHERE s.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public int getPendingAssignments(int userId) {
        String query = "SELECT COUNT(*) FROM ASSIGNMENTS a " +
                       "JOIN STUDENT_CLASS sc ON a.class_id = sc.class_id " +
                       "JOIN STUDENTS s ON sc.student_id = s.student_id " +
                       "WHERE s.user_id = ? AND a.assignment_id NOT IN " +
                       "(SELECT assignment_id FROM SUBMISSIONS WHERE student_id = s.student_id)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM USERS WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean updatePassword(String email, String newPasswordHash) {
        String query = "UPDATE USERS SET password_hash = ? WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, email);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}
