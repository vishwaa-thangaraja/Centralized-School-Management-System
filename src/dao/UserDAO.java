package dao;

import java.sql.*;
import java.util.*;
import model.AdminParentLinkRecord;
import model.AdminStudentClassRecord;
import model.AdminTeacherMappingRecord;
import model.AdminUserRecord;
import model.AcademicAlertRecord;
import model.AssignmentRecord;
import model.AttendanceRecord;
import model.CommunicationMessage;
import model.CounsellingCaseRecord;
import model.CounsellorContactRecord;
import model.CounsellorInboxRecord;
import model.CounsellingRequestRecord;
import model.ExamRecord;
import model.ParentChatThreadRecord;
import model.PerformanceRecord; // Added for Performance Module
import model.Student;
import model.TeacherContactRecord;
import model.User;
import javafx.collections.FXCollections; // Added for TableView compatibility
import javafx.collections.ObservableList; // Added for TableView compatibility

public class UserDAO {
    private static final Map<String, Integer> MEMORY_CHAT_LAST_SEEN = new HashMap<>();

    private String memoryChatKey(int viewerUserId, int partnerUserId, int studentId) {
        return viewerUserId + "|" + partnerUserId + "|" + studentId;
    }

    private int getMemoryLastSeenMessageId(int viewerUserId, int partnerUserId, int studentId) {
        synchronized (MEMORY_CHAT_LAST_SEEN) {
            return MEMORY_CHAT_LAST_SEEN.getOrDefault(memoryChatKey(viewerUserId, partnerUserId, studentId), 0);
        }
    }

    private void setMemoryLastSeenMessageId(int viewerUserId, int partnerUserId, int studentId, int lastSeenMessageId) {
        synchronized (MEMORY_CHAT_LAST_SEEN) {
            MEMORY_CHAT_LAST_SEEN.put(memoryChatKey(viewerUserId, partnerUserId, studentId), Math.max(lastSeenMessageId, 0));
        }
    }

    public User validateUser(String email, String passwordHash) {
        try (Connection conn = DBConnection.getConnection()) {
            String activeClause = columnExists(conn, "USERS", "IS_ACTIVE") ? " AND u.is_active = 1" : "";
            String query = "SELECT u.user_id, u.name, u.email, r.role_name " +
                           "FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                           "WHERE u.email = ? AND u.password_hash = ?" + activeClause;
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
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
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Map<String, String> getStudentProfile(int userId) {
        Map<String, String> profile = new HashMap<>();
        String query = "SELECT s.student_id, TO_CHAR(s.dob, 'DD/MM/YYYY') AS dob, NVL(s.gender, '-') AS gender, " +
                       "c.class_name, c.section, " +
                       "NVL(c.class_name || '-' || c.section || ' (' || c.academic_year || ')', 'N/A') AS class_display " +
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
                profile.put("dob", rs.getString("dob") != null ? rs.getString("dob") : "-");
                profile.put("gender", rs.getString("gender") != null ? rs.getString("gender") : "-");
                profile.put("class_display", rs.getString("class_display"));
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

    public int getStudentIdByUserId(int userId) {
        String query = "SELECT student_id FROM STUDENTS WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("student_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<AttendanceRecord> getSchoolAttendance(int userId) {
        List<AttendanceRecord> list = new ArrayList<>();
        String query = "SELECT TO_CHAR(TRUNC(attendance_date), 'YYYY-MM-DD') as a_date, " +
                       "MAX(CASE WHEN session_type = 'FN' THEN status END) as FN, " +
                       "MAX(CASE WHEN session_type = 'AN' THEN status END) as AN, " +
                       "MAX(CASE WHEN leave_reason IS NOT NULL THEN leave_reason END) as day_remark " +
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
                    rs.getString("day_remark") != null ? rs.getString("day_remark") : "Regular"
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
        try (Connection conn = DBConnection.getConnection()) {
            return SequenceService.nextVal(conn, "SEQ_ASSIGNMENTS", "ASSIGNMENTS", "ASSIGNMENT_ID");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public int getNextSubmissionId() {
        try (Connection conn = DBConnection.getConnection()) {
            return SequenceService.nextVal(conn, "SEQ_SUBMISSIONS", "SUBMISSIONS", "SUBMISSION_ID");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public int getTeacherIdByUserId(int userId) {
        String query = "SELECT teacher_id FROM TEACHERS WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public int getStudentUserIdByStudentId(int studentId) {
        String query = "SELECT user_id FROM STUDENTS WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public boolean validateTeacherScopeForStudent(int teacherId, int studentId) {
        // Scope is derived strictly from CLASS_SUBJECT_TEACHER -> STUDENT_CLASS mappings.
        String query = "SELECT COUNT(*) " +
                       "FROM CLASS_SUBJECT_TEACHER cst " +
                       "JOIN STUDENT_CLASS sc ON cst.class_id = sc.class_id " +
                       "WHERE cst.teacher_id = ? AND sc.student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, teacherId);
            pstmt.setInt(2, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateAttendanceRecord(int studentId, String date, String fnStatus, String anStatus, String remarks) {
        Set<String> allowedStatuses = new HashSet<>(Arrays.asList("Present", "Absent"));
        if (!allowedStatuses.contains(fnStatus) || !allowedStatuses.contains(anStatus)) {
            return false;
        }

        String studentScopeQuery = "SELECT stu.user_id, MIN(sc.class_id) AS class_id " +
                                   "FROM STUDENTS stu " +
                                   "JOIN STUDENT_CLASS sc ON stu.student_id = sc.student_id " +
                                   "WHERE stu.student_id = ? " +
                                   "GROUP BY stu.user_id";
        String futureDateQuery = "SELECT CASE WHEN TO_DATE(?, 'YYYY-MM-DD') > TRUNC(SYSDATE) THEN 1 ELSE 0 END FROM dual";
        String attendanceCheckQuery = "SELECT COUNT(*) AS record_count, MIN(attendance_id) AS min_attendance_id " +
                                      "FROM ATTENDANCE " +
                                      "WHERE user_id = ? AND class_id = ? " +
                                      "AND TRUNC(attendance_date) = TO_DATE(?, 'YYYY-MM-DD') AND session_type = ?";
        String updateAttendanceQuery = "UPDATE ATTENDANCE SET status = ?, leave_reason = ? WHERE attendance_id = ?";
        String insertAttendanceQuery = "INSERT INTO ATTENDANCE " +
                                       "(attendance_id, user_id, class_id, attendance_date, session_type, status, leave_reason, approval_status, approved_by) " +
                                       "VALUES (?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, NULL, NULL)";

        try (Connection conn = DBConnection.getConnection()) {
            SequenceService.ensureSequence(conn, "SEQ_ATTENDANCE", "ATTENDANCE", "ATTENDANCE_ID");
            conn.setAutoCommit(false);
            try {
                int studentUserId;
                int classId;
                try (PreparedStatement scopeStmt = conn.prepareStatement(studentScopeQuery)) {
                    scopeStmt.setInt(1, studentId);
                    ResultSet scopeRs = scopeStmt.executeQuery();
                    if (!scopeRs.next()) {
                        conn.rollback();
                        return false;
                    }
                    studentUserId = scopeRs.getInt("user_id");
                    classId = scopeRs.getInt("class_id");
                }

                try (PreparedStatement futureStmt = conn.prepareStatement(futureDateQuery)) {
                    futureStmt.setString(1, date);
                    ResultSet futureRs = futureStmt.executeQuery();
                    if (futureRs.next() && futureRs.getInt(1) == 1) {
                        conn.rollback();
                        return false;
                    }
                }

                String normalizedRemark = (remarks == null || remarks.isBlank()) ? null : remarks.trim();
                String[] sessions = {"FN", "AN"};
                String[] statuses = {fnStatus, anStatus};

                for (int index = 0; index < sessions.length; index++) {
                    String session = sessions[index];
                    String sessionStatus = statuses[index];

                    int recordCount = 0;
                    int existingAttendanceId = -1;
                    try (PreparedStatement checkStmt = conn.prepareStatement(attendanceCheckQuery)) {
                        checkStmt.setInt(1, studentUserId);
                        checkStmt.setInt(2, classId);
                        checkStmt.setString(3, date);
                        checkStmt.setString(4, session);
                        ResultSet checkRs = checkStmt.executeQuery();
                        if (checkRs.next()) {
                            recordCount = checkRs.getInt("record_count");
                            existingAttendanceId = checkRs.getInt("min_attendance_id");
                        }
                    }

                    if (recordCount > 1) {
                        conn.rollback();
                        return false;
                    }

                    if (recordCount == 1) {
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateAttendanceQuery)) {
                            updateStmt.setString(1, sessionStatus);
                            updateStmt.setString(2, normalizedRemark);
                            updateStmt.setInt(3, existingAttendanceId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        int nextAttendanceId = SequenceService.nextVal(conn, "SEQ_ATTENDANCE", "ATTENDANCE", "ATTENDANCE_ID");

                        try (PreparedStatement insertStmt = conn.prepareStatement(insertAttendanceQuery)) {
                            insertStmt.setInt(1, nextAttendanceId);
                            insertStmt.setInt(2, studentUserId);
                            insertStmt.setInt(3, classId);
                            insertStmt.setString(4, date);
                            insertStmt.setString(5, session);
                            insertStmt.setString(6, sessionStatus);
                            insertStmt.setString(7, normalizedRemark);
                            insertStmt.executeUpdate();
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

    public void logAttendanceAlterationPlaceholder(int teacherUserId, int studentId, String attendanceDate) {
        // Placeholder for future admin-grade audit trail table.
        System.out.println(
            "[AUDIT-PLACEHOLDER] Teacher " + teacherUserId +
            " altered attendance for student " + studentId +
            " on " + attendanceDate
        );
    }

    public ObservableList<ExamRecord> getExamsForStudent(int studentId) {
        ObservableList<ExamRecord> exams = FXCollections.observableArrayList();
        String query = "SELECT qp.qp_id, qp.class_id, qp.subject_id, " +
                       "NVL(qp.exam_type, 'Exam') AS exam_title, " +
                       "NVL(qp.exam_description, '-') AS exam_description, " +
                       "s.subject_name, " +
                       "c.class_name || '-' || c.section || ' (' || c.academic_year || ')' AS class_display, " +
                       "TO_CHAR(qp.exam_date, 'YYYY-MM-DD') AS exam_date, " +
                       "qp.max_marks, " +
                       "NVL(u.name, 'Not Assigned') AS teacher_name, " +
                       "CASE WHEN TRUNC(qp.exam_date) > TRUNC(SYSDATE) THEN 'Upcoming' ELSE 'Completed' END AS exam_status " +
                       "FROM QUESTION_PAPERS qp " +
                       "JOIN SUBJECTS s ON qp.subject_id = s.subject_id " +
                       "JOIN CLASSES c ON qp.class_id = c.class_id " +
                       "JOIN STUDENT_CLASS sc ON qp.class_id = sc.class_id " +
                       "LEFT JOIN TEACHERS t ON qp.created_by_teacher_id = t.teacher_id " +
                       "LEFT JOIN USERS u ON t.user_id = u.user_id " +
                       "WHERE sc.student_id = ? " +
                       "ORDER BY qp.exam_date ASC, qp.qp_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                exams.add(new ExamRecord(
                    rs.getInt("qp_id"),
                    rs.getInt("class_id"),
                    rs.getInt("subject_id"),
                    rs.getString("exam_title"),
                    rs.getString("exam_description"),
                    rs.getString("subject_name"),
                    rs.getString("class_display"),
                    rs.getString("exam_date"),
                    rs.getInt("max_marks"),
                    rs.getString("teacher_name"),
                    rs.getString("exam_status"),
                    null,
                    "Not Evaluated"
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exams;
    }

    public Map<Integer, Double> getMarksForStudent(int studentId) {
        Map<Integer, Double> marksByExam = new HashMap<>();
        String query = "SELECT qp_id, marks_obtained FROM MARKS WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                marksByExam.put(rs.getInt("qp_id"), rs.getDouble("marks_obtained"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return marksByExam;
    }

    public ObservableList<ExamRecord> getExamsForTeacher(int teacherId) {
        ObservableList<ExamRecord> exams = FXCollections.observableArrayList();
        String query = "SELECT qp.qp_id, qp.class_id, qp.subject_id, " +
                       "NVL(qp.exam_type, 'Exam') AS exam_title, " +
                       "NVL(qp.exam_description, '-') AS exam_description, " +
                       "s.subject_name, " +
                       "c.class_name || '-' || c.section || ' (' || c.academic_year || ')' AS class_display, " +
                       "TO_CHAR(qp.exam_date, 'YYYY-MM-DD') AS exam_date, " +
                       "qp.max_marks, " +
                       "NVL(creator_user.name, '-') AS teacher_name, " +
                       "CASE WHEN TRUNC(qp.exam_date) > TRUNC(SYSDATE) THEN 'Upcoming' ELSE 'Completed' END AS exam_status " +
                       "FROM QUESTION_PAPERS qp " +
                       "JOIN SUBJECTS s ON qp.subject_id = s.subject_id " +
                       "JOIN CLASSES c ON qp.class_id = c.class_id " +
                       "LEFT JOIN TEACHERS creator_teacher ON qp.created_by_teacher_id = creator_teacher.teacher_id " +
                       "LEFT JOIN USERS creator_user ON creator_teacher.user_id = creator_user.user_id " +
                       "WHERE EXISTS (" +
                       "    SELECT 1 FROM CLASS_SUBJECT_TEACHER cst " +
                       "    WHERE cst.teacher_id = ? AND cst.class_id = qp.class_id AND cst.subject_id = qp.subject_id" +
                       ") " +
                       "ORDER BY qp.exam_date DESC, qp.qp_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                exams.add(new ExamRecord(
                    rs.getInt("qp_id"),
                    rs.getInt("class_id"),
                    rs.getInt("subject_id"),
                    rs.getString("exam_title"),
                    rs.getString("exam_description"),
                    rs.getString("subject_name"),
                    rs.getString("class_display"),
                    rs.getString("exam_date"),
                    rs.getInt("max_marks"),
                    rs.getString("teacher_name"),
                    rs.getString("exam_status"),
                    null,
                    "Not Evaluated"
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exams;
    }

    public boolean createExam(int teacherId, int classId, int subjectId, String examTitle, String description, String examDate, int totalMarks) {
        String scopeQuery = "SELECT COUNT(*) FROM CLASS_SUBJECT_TEACHER " +
                            "WHERE teacher_id = ? AND class_id = ? AND subject_id = ?";
        // The duplicate guard follows the business rule: one exam per class-subject-date.
        String duplicateQuery = "SELECT COUNT(*) FROM QUESTION_PAPERS " +
                                "WHERE class_id = ? AND subject_id = ? AND TRUNC(exam_date) = TO_DATE(?, 'YYYY-MM-DD')";
        String insertQuery = "INSERT INTO QUESTION_PAPERS " +
                             "(qp_id, class_id, subject_id, exam_type, exam_description, exam_date, max_marks, created_by_teacher_id) " +
                             "VALUES (?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement scopeStmt = conn.prepareStatement(scopeQuery)) {
                scopeStmt.setInt(1, teacherId);
                scopeStmt.setInt(2, classId);
                scopeStmt.setInt(3, subjectId);
                ResultSet scopeRs = scopeStmt.executeQuery();
                if (!scopeRs.next() || scopeRs.getInt(1) == 0) {
                    return false;
                }
            }

            try (PreparedStatement duplicateStmt = conn.prepareStatement(duplicateQuery)) {
                duplicateStmt.setInt(1, classId);
                duplicateStmt.setInt(2, subjectId);
                duplicateStmt.setString(3, examDate);
                ResultSet duplicateRs = duplicateStmt.executeQuery();
                if (duplicateRs.next() && duplicateRs.getInt(1) > 0) {
                    return false;
                }
            }

            int nextExamId = SequenceService.nextVal(conn, "SEQ_QUESTION_PAPERS", "QUESTION_PAPERS", "QP_ID");

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, nextExamId);
                insertStmt.setInt(2, classId);
                insertStmt.setInt(3, subjectId);
                insertStmt.setString(4, examTitle);
                insertStmt.setString(5, description);
                insertStmt.setString(6, examDate);
                insertStmt.setInt(7, totalMarks);
                insertStmt.setInt(8, teacherId);
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean saveStudentMarks(int examId, Map<Integer, Double> studentMarks) {
        String studentScopeQuery = "SELECT COUNT(*) FROM QUESTION_PAPERS qp " +
                                   "JOIN STUDENT_CLASS sc ON qp.class_id = sc.class_id " +
                                   "WHERE qp.qp_id = ? AND sc.student_id = ?";
        String existingQuery = "SELECT mark_id FROM MARKS WHERE student_id = ? AND qp_id = ?";
        String updateQuery = "UPDATE MARKS SET marks_obtained = ? WHERE mark_id = ?";
        String insertQuery = "INSERT INTO MARKS (mark_id, student_id, qp_id, marks_obtained) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            SequenceService.ensureSequence(conn, "SEQ_MARKS", "MARKS", "MARK_ID");
            conn.setAutoCommit(false);
            try {
                for (Map.Entry<Integer, Double> entry : studentMarks.entrySet()) {
                    int studentId = entry.getKey();
                    double marks = entry.getValue();

                    try (PreparedStatement scopeStmt = conn.prepareStatement(studentScopeQuery)) {
                        scopeStmt.setInt(1, examId);
                        scopeStmt.setInt(2, studentId);
                        ResultSet scopeRs = scopeStmt.executeQuery();
                        if (!scopeRs.next() || scopeRs.getInt(1) == 0) {
                            conn.rollback();
                            return false;
                        }
                    }

                    int existingMarkId = -1;
                    try (PreparedStatement existingStmt = conn.prepareStatement(existingQuery)) {
                        existingStmt.setInt(1, studentId);
                        existingStmt.setInt(2, examId);
                        ResultSet existingRs = existingStmt.executeQuery();
                        if (existingRs.next()) {
                            existingMarkId = existingRs.getInt(1);
                        }
                    }

                    if (existingMarkId > 0) {
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                            updateStmt.setDouble(1, marks);
                            updateStmt.setInt(2, existingMarkId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        int nextMarkId = SequenceService.nextVal(conn, "SEQ_MARKS", "MARKS", "MARK_ID");

                        try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                            insertStmt.setInt(1, nextMarkId);
                            insertStmt.setInt(2, studentId);
                            insertStmt.setInt(3, examId);
                            insertStmt.setDouble(4, marks);
                            insertStmt.executeUpdate();
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

    public boolean validateTeacherScopeForClassSubject(int teacherId, int classId, int subjectId) {
        String query = "SELECT COUNT(*) FROM CLASS_SUBJECT_TEACHER " +
                       "WHERE teacher_id = ? AND class_id = ? AND subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, teacherId);
            pstmt.setInt(2, classId);
            pstmt.setInt(3, subjectId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean validateTeacherScopeForExam(int teacherId, int examId, int classId, int subjectId) {
        String query = "SELECT COUNT(*) FROM QUESTION_PAPERS qp " +
                       "JOIN CLASS_SUBJECT_TEACHER cst " +
                       "ON qp.class_id = cst.class_id AND qp.subject_id = cst.subject_id " +
                       "WHERE qp.qp_id = ? AND cst.teacher_id = ? AND qp.class_id = ? AND qp.subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, examId);
            pstmt.setInt(2, teacherId);
            pstmt.setInt(3, classId);
            pstmt.setInt(4, subjectId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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

    private boolean isParentWardMapped(Connection conn, int parentUserId, int studentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM PARENT_STUDENT WHERE parent_id = ? AND student_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, parentUserId);
            pstmt.setInt(2, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public ObservableList<Student> getParentWards(int parentUserId) {
        ObservableList<Student> wards = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT s.student_id, s.user_id, NVL(sc.class_id, 0) AS class_id, " +
                       "u.name, u.email, NVL(u.phone, '-') AS phone, " +
                       "NVL(c.class_name || '-' || c.section || ' (' || c.academic_year || ')', 'Not Assigned') AS class_display, " +
                       "TO_CHAR(s.dob, 'YYYY-MM-DD') AS dob, NVL(s.gender, '-') AS gender, " +
                       "NVL(s.conduct, '-') AS conduct, NVL(s.conduct_remarks, '-') AS conduct_remarks " +
                       "FROM PARENT_STUDENT ps " +
                       "JOIN STUDENTS s ON ps.student_id = s.student_id " +
                       "JOIN USERS u ON s.user_id = u.user_id " +
                       "LEFT JOIN STUDENT_CLASS sc ON s.student_id = sc.student_id " +
                       "LEFT JOIN CLASSES c ON sc.class_id = c.class_id " +
                       "WHERE ps.parent_id = ? " +
                       "ORDER BY u.name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, parentUserId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                wards.add(new Student(
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
        return wards;
    }

    public AttendanceRecord getTodayAttendance(int studentId) {
        String query = "SELECT TO_CHAR(TRUNC(SYSDATE), 'YYYY-MM-DD') AS attendance_day, " +
                       "NVL(MAX(CASE WHEN a.session_type = 'FN' THEN a.status END), 'Not Marked') AS fn_status, " +
                       "NVL(MAX(CASE WHEN a.session_type = 'AN' THEN a.status END), 'Not Marked') AS an_status, " +
                       "NVL(MAX(a.leave_reason), 'Regular') AS remark " +
                       "FROM STUDENTS s " +
                       "LEFT JOIN ATTENDANCE a ON a.user_id = s.user_id AND TRUNC(a.attendance_date) = TRUNC(SYSDATE) " +
                       "WHERE s.student_id = ? " +
                       "GROUP BY TRUNC(SYSDATE)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new AttendanceRecord(
                    rs.getString("attendance_day"),
                    rs.getString("fn_status"),
                    rs.getString("an_status"),
                    rs.getString("remark")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new AttendanceRecord("", "Not Marked", "Not Marked", "No record for today");
    }

    public AttendanceRecord getTodayAttendanceForParent(int parentUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentWardMapped(conn, parentUserId, studentId)) {
                return new AttendanceRecord("", "Access Denied", "Access Denied", "Ward mapping not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new AttendanceRecord("", "Error", "Error", "Unable to verify ward scope");
        }
        return getTodayAttendance(studentId);
    }

    public ObservableList<AcademicAlertRecord> getAcademicAlerts(int studentId) {
        ObservableList<AcademicAlertRecord> alerts = FXCollections.observableArrayList();
        String query = "SELECT sub.subject_name, NVL(qp.exam_type, 'Exam') AS exam_title, " +
                       "TO_CHAR(qp.exam_date, 'YYYY-MM-DD') AS exam_date, " +
                       "m.marks_obtained, qp.max_marks " +
                       "FROM MARKS m " +
                       "JOIN QUESTION_PAPERS qp ON m.qp_id = qp.qp_id " +
                       "JOIN SUBJECTS sub ON qp.subject_id = sub.subject_id " +
                       "WHERE m.student_id = ? AND m.marks_obtained < (0.5 * qp.max_marks) " +
                       "ORDER BY qp.exam_date DESC, m.mark_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                alerts.add(new AcademicAlertRecord(
                    rs.getString("subject_name"),
                    rs.getString("exam_title"),
                    rs.getString("exam_date"),
                    rs.getDouble("marks_obtained"),
                    rs.getDouble("max_marks")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public ObservableList<AcademicAlertRecord> getAcademicAlertsForParent(int parentUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentWardMapped(conn, parentUserId, studentId)) {
                return FXCollections.observableArrayList();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return FXCollections.observableArrayList();
        }
        return getAcademicAlerts(studentId);
    }

    public ObservableList<AttendanceRecord> getAttendanceHistoryForParent(int parentUserId, int studentId) {
        ObservableList<AttendanceRecord> history = FXCollections.observableArrayList();
        String query = "SELECT TO_CHAR(TRUNC(a.attendance_date), 'YYYY-MM-DD') AS attendance_day, " +
                       "NVL(MAX(CASE WHEN a.session_type = 'FN' THEN a.status END), 'Not Marked') AS fn_status, " +
                       "NVL(MAX(CASE WHEN a.session_type = 'AN' THEN a.status END), 'Not Marked') AS an_status, " +
                       "NVL(MAX(a.leave_reason), 'Regular') AS day_remark " +
                       "FROM STUDENTS s " +
                       "LEFT JOIN ATTENDANCE a ON a.user_id = s.user_id " +
                       "WHERE s.student_id = ? " +
                       "GROUP BY TRUNC(a.attendance_date) " +
                       "ORDER BY TRUNC(a.attendance_date) DESC";

        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentWardMapped(conn, parentUserId, studentId)) {
                return history;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, studentId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String attendanceDay = rs.getString("attendance_day");
                    if (attendanceDay == null) {
                        continue;
                    }
                    history.add(new AttendanceRecord(
                        attendanceDay,
                        rs.getString("fn_status"),
                        rs.getString("an_status"),
                        rs.getString("day_remark")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public ObservableList<TeacherContactRecord> getWardTeachers(int studentId) {
        ObservableList<TeacherContactRecord> teachers = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT u.user_id AS teacher_user_id, t.teacher_id, u.name AS teacher_name, " +
                       "sub.subject_name, NVL(u.phone, u.email) AS contact " +
                       "FROM STUDENT_CLASS sc " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON sc.class_id = cst.class_id " +
                       "JOIN TEACHERS t ON cst.teacher_id = t.teacher_id " +
                       "JOIN USERS u ON t.user_id = u.user_id " +
                       "JOIN SUBJECTS sub ON cst.subject_id = sub.subject_id " +
                       "WHERE sc.student_id = ? " +
                       "ORDER BY teacher_name, sub.subject_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                teachers.add(new TeacherContactRecord(
                    rs.getInt("teacher_user_id"),
                    rs.getInt("teacher_id"),
                    rs.getString("teacher_name"),
                    rs.getString("subject_name"),
                    rs.getString("contact")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teachers;
    }

    public ObservableList<TeacherContactRecord> getWardTeachersForParent(int parentUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentWardMapped(conn, parentUserId, studentId)) {
                return FXCollections.observableArrayList();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return FXCollections.observableArrayList();
        }
        return getWardTeachers(studentId);
    }

    public ObservableList<CommunicationMessage> getChatHistory(int parentId, int teacherId, int studentId) {
        ObservableList<CommunicationMessage> messages = FXCollections.observableArrayList();
        String query = "SELECT message_id, sender_id, receiver_id, student_id, message_text, " +
                       "TO_CHAR(sent_at, 'YYYY-MM-DD HH24:MI:SS') AS sent_at " +
                       "FROM (" +
                       "    SELECT c.* FROM COMMUNICATION c " +
                       "    WHERE c.student_id = ? " +
                       "      AND ((c.sender_id = ? AND c.receiver_id = ?) OR (c.sender_id = ? AND c.receiver_id = ?)) " +
                       "    ORDER BY c.sent_at DESC, c.message_id DESC" +
                       ") WHERE ROWNUM <= 50 " +
                       "ORDER BY sent_at ASC";

        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentWardMapped(conn, parentId, studentId)) {
                return messages;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, studentId);
                pstmt.setInt(2, parentId);
                pstmt.setInt(3, teacherId);
                pstmt.setInt(4, teacherId);
                pstmt.setInt(5, parentId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    messages.add(new CommunicationMessage(
                        rs.getInt("message_id"),
                        rs.getInt("sender_id"),
                        rs.getInt("receiver_id"),
                        rs.getInt("student_id"),
                        rs.getString("message_text"),
                        rs.getString("sent_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    private boolean isTeacherMappedToStudent(Connection conn, int teacherUserId, int studentId) throws SQLException {
        String query = "SELECT COUNT(*) " +
                       "FROM TEACHERS t " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "JOIN STUDENT_CLASS sc ON sc.class_id = cst.class_id " +
                       "WHERE t.user_id = ? AND sc.student_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, teacherUserId);
            pstmt.setInt(2, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean isParentMappedToStudent(Connection conn, int parentUserId, int studentId) throws SQLException {
        return isParentWardMapped(conn, parentUserId, studentId);
    }

    private boolean isUserInRole(Connection conn, int userId, String roleName) throws SQLException {
        String query = "SELECT COUNT(*) FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                       "WHERE u.user_id = ? AND UPPER(r.role_name) = UPPER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, roleName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean isCounsellorUser(Connection conn, int userId) throws SQLException {
        return isUserInRole(conn, userId, "Counsellor");
    }

    private boolean isStudentOwner(Connection conn, int userId, int studentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM STUDENTS WHERE USER_ID = ? AND STUDENT_ID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private int getStudentIdForUser(Connection conn, int userId) throws SQLException {
        String query = "SELECT student_id FROM STUDENTS WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("student_id");
            }
        }
        return -1;
    }

    private int getDefaultCounsellorUserId(Connection conn) throws SQLException {
        String query = "SELECT u.user_id FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                       "WHERE UPPER(r.role_name) = 'COUNSELLOR' ORDER BY u.user_id";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
        }
        return -1;
    }

    private int getNextCounsellingSessionId(Connection conn) throws SQLException {
        return SequenceService.nextVal(conn, "SEQ_COUNSELLING", "COUNSELLING", "SESSION_ID");
    }

    private boolean isTeacherParentStudentMapped(Connection conn, int teacherUserId, int parentUserId, int studentId) throws SQLException {
        String query = "SELECT COUNT(*) " +
                       "FROM TEACHERS t " +
                       "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "JOIN STUDENT_CLASS sc ON sc.class_id = cst.class_id " +
                       "JOIN PARENT_STUDENT ps ON ps.student_id = sc.student_id " +
                       "WHERE t.user_id = ? AND ps.parent_id = ? AND sc.student_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, teacherUserId);
            pstmt.setInt(2, parentUserId);
            pstmt.setInt(3, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, tableName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String query = "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, tableName.toUpperCase());
            pstmt.setString(2, columnName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean indexExists(Connection conn, String indexName) throws SQLException {
        String query = "SELECT COUNT(*) FROM USER_INDEXES WHERE INDEX_NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, indexName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void executeDdlIgnoreAlreadyExists(Connection conn, String ddl) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(ddl);
        } catch (SQLException e) {
            if (e.getErrorCode() != 955) {
                throw e;
            }
        }
    }

    private boolean ensureCommunicationReadStateInfrastructure(Connection conn) {
        try {
            if (!tableExists(conn, "COMMUNICATION_READ_STATE")) {
                executeDdlIgnoreAlreadyExists(conn,
                    "CREATE TABLE COMMUNICATION_READ_STATE (" +
                    "VIEWER_ID NUMBER NOT NULL, " +
                    "PARTNER_ID NUMBER NOT NULL, " +
                    "STUDENT_ID NUMBER NOT NULL, " +
                    "LAST_SEEN_MESSAGE_ID NUMBER DEFAULT 0 NOT NULL, " +
                    "LAST_SEEN_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                    "CONSTRAINT PK_COMM_READ_STATE PRIMARY KEY (VIEWER_ID, PARTNER_ID, STUDENT_ID), " +
                    "CONSTRAINT FK_COMM_READ_VIEWER FOREIGN KEY (VIEWER_ID) REFERENCES USERS(USER_ID), " +
                    "CONSTRAINT FK_COMM_READ_PARTNER FOREIGN KEY (PARTNER_ID) REFERENCES USERS(USER_ID), " +
                    "CONSTRAINT FK_COMM_READ_STUDENT FOREIGN KEY (STUDENT_ID) REFERENCES STUDENTS(STUDENT_ID), " +
                    "CONSTRAINT CHK_COMM_READ_LAST_ID CHECK (LAST_SEEN_MESSAGE_ID >= 0)" +
                    ")"
                );
            }

            if (tableExists(conn, "COMMUNICATION_READ_STATE") && !indexExists(conn, "IDX_COMM_READ_VIEWER")) {
                executeDdlIgnoreAlreadyExists(
                    conn,
                    "CREATE INDEX IDX_COMM_READ_VIEWER ON COMMUNICATION_READ_STATE(VIEWER_ID)"
                );
            }

            return tableExists(conn, "COMMUNICATION_READ_STATE");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getNextCommunicationMessageId(Connection conn) throws SQLException {
        return SequenceService.nextVal(conn, "SEQ_COMMUNICATION", "COMMUNICATION", "MESSAGE_ID");
    }

    private boolean upsertReadState(Connection conn, int viewerUserId, int partnerUserId, int studentId, int lastSeenMessageId) throws SQLException {
        String mergeQuery =
            "MERGE INTO COMMUNICATION_READ_STATE target " +
            "USING (SELECT ? AS viewer_id, ? AS partner_id, ? AS student_id, ? AS last_seen_message_id FROM dual) src " +
            "ON (target.viewer_id = src.viewer_id AND target.partner_id = src.partner_id AND target.student_id = src.student_id) " +
            "WHEN MATCHED THEN UPDATE SET target.last_seen_message_id = src.last_seen_message_id, target.last_seen_at = CURRENT_TIMESTAMP " +
            "WHEN NOT MATCHED THEN INSERT (viewer_id, partner_id, student_id, last_seen_message_id, last_seen_at) " +
            "VALUES (src.viewer_id, src.partner_id, src.student_id, src.last_seen_message_id, CURRENT_TIMESTAMP)";

        try (PreparedStatement pstmt = conn.prepareStatement(mergeQuery)) {
            pstmt.setInt(1, viewerUserId);
            pstmt.setInt(2, partnerUserId);
            pstmt.setInt(3, studentId);
            pstmt.setInt(4, Math.max(lastSeenMessageId, 0));
            return pstmt.executeUpdate() > 0;
        }
    }

    private ObservableList<CommunicationMessage> getScopedConversation(Connection conn, int userAId, int userBId, int studentId) throws SQLException {
        ObservableList<CommunicationMessage> messages = FXCollections.observableArrayList();
        String query = "SELECT message_id, sender_id, receiver_id, student_id, message_text, " +
                       "TO_CHAR(sent_at, 'YYYY-MM-DD HH24:MI:SS') AS sent_at " +
                       "FROM (" +
                       "    SELECT c.* FROM COMMUNICATION c " +
                       "    WHERE c.student_id = ? " +
                       "      AND ((c.sender_id = ? AND c.receiver_id = ?) OR (c.sender_id = ? AND c.receiver_id = ?)) " +
                       "    ORDER BY c.sent_at DESC, c.message_id DESC" +
                       ") WHERE ROWNUM <= 50 " +
                       "ORDER BY sent_at ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, userAId);
            pstmt.setInt(3, userBId);
            pstmt.setInt(4, userBId);
            pstmt.setInt(5, userAId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new CommunicationMessage(
                    rs.getInt("message_id"),
                    rs.getInt("sender_id"),
                    rs.getInt("receiver_id"),
                    rs.getInt("student_id"),
                    rs.getString("message_text"),
                    rs.getString("sent_at")
                ));
            }
        }
        return messages;
    }

    private int getConversationUnreadCount(
        Connection conn,
        int viewerUserId,
        int partnerUserId,
        int studentId,
        boolean readStateAvailable
    ) throws SQLException {
        if (readStateAvailable) {
            String query = "SELECT COUNT(*) AS unread_count " +
                           "FROM COMMUNICATION c " +
                           "WHERE c.receiver_id = ? " +
                           "  AND c.sender_id = ? " +
                           "  AND c.student_id = ? " +
                           "  AND c.message_id > NVL((" +
                           "      SELECT rs.last_seen_message_id " +
                           "      FROM COMMUNICATION_READ_STATE rs " +
                           "      WHERE rs.viewer_id = ? " +
                           "        AND rs.partner_id = ? " +
                           "        AND rs.student_id = ?" +
                           "  ), 0)";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, viewerUserId);
                pstmt.setInt(2, partnerUserId);
                pstmt.setInt(3, studentId);
                pstmt.setInt(4, viewerUserId);
                pstmt.setInt(5, partnerUserId);
                pstmt.setInt(6, studentId);
                ResultSet rs = pstmt.executeQuery();
                return rs.next() ? rs.getInt("unread_count") : 0;
            }
        }

        int lastSeenMessageId = getMemoryLastSeenMessageId(viewerUserId, partnerUserId, studentId);
        String query = "SELECT COUNT(*) AS unread_count " +
                       "FROM COMMUNICATION " +
                       "WHERE receiver_id = ? " +
                       "  AND sender_id = ? " +
                       "  AND student_id = ? " +
                       "  AND message_id > ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, viewerUserId);
            pstmt.setInt(2, partnerUserId);
            pstmt.setInt(3, studentId);
            pstmt.setInt(4, lastSeenMessageId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("unread_count") : 0;
        }
    }

    private void clearMemoryConversationState(int userAId, int userBId, int studentId) {
        synchronized (MEMORY_CHAT_LAST_SEEN) {
            MEMORY_CHAT_LAST_SEEN.remove(memoryChatKey(userAId, userBId, studentId));
            MEMORY_CHAT_LAST_SEEN.remove(memoryChatKey(userBId, userAId, studentId));
        }
    }

    private void clearConversationData(Connection conn, int userAId, int userBId, int studentId) throws SQLException {
        String deleteMessagesQuery = "DELETE FROM COMMUNICATION " +
                                     "WHERE student_id = ? " +
                                     "  AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))";
        String deleteReadStateQuery = "DELETE FROM COMMUNICATION_READ_STATE " +
                                      "WHERE student_id = ? " +
                                      "  AND ((viewer_id = ? AND partner_id = ?) OR (viewer_id = ? AND partner_id = ?))";

        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            if (tableExists(conn, "COMMUNICATION_READ_STATE")) {
                try (PreparedStatement readStateStmt = conn.prepareStatement(deleteReadStateQuery)) {
                    readStateStmt.setInt(1, studentId);
                    readStateStmt.setInt(2, userAId);
                    readStateStmt.setInt(3, userBId);
                    readStateStmt.setInt(4, userBId);
                    readStateStmt.setInt(5, userAId);
                    readStateStmt.executeUpdate();
                }
            }

            try (PreparedStatement messageStmt = conn.prepareStatement(deleteMessagesQuery)) {
                messageStmt.setInt(1, studentId);
                messageStmt.setInt(2, userAId);
                messageStmt.setInt(3, userBId);
                messageStmt.setInt(4, userBId);
                messageStmt.setInt(5, userAId);
                messageStmt.executeUpdate();
            }

            conn.commit();
            clearMemoryConversationState(userAId, userBId, studentId);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    private boolean markConversationSeenForUsers(
        Connection conn,
        int viewerUserId,
        int partnerUserId,
        int studentId,
        boolean readStateAvailable
    ) throws SQLException {
        String query = "SELECT NVL(MAX(message_id), 0) AS last_message_id " +
                       "FROM COMMUNICATION " +
                       "WHERE receiver_id = ? AND sender_id = ? AND student_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, viewerUserId);
            pstmt.setInt(2, partnerUserId);
            pstmt.setInt(3, studentId);
            ResultSet rs = pstmt.executeQuery();
            int lastMessageId = rs.next() ? rs.getInt("last_message_id") : 0;
            if (readStateAvailable) {
                return upsertReadState(conn, viewerUserId, partnerUserId, studentId, lastMessageId);
            }
            setMemoryLastSeenMessageId(viewerUserId, partnerUserId, studentId, lastMessageId);
            return true;
        }
    }

    public int getParentUnreadMessageCount(int parentUserId, int studentId) {
        String teacherPartnerQuery = "SELECT DISTINCT c.sender_id AS teacher_user_id " +
                                     "FROM COMMUNICATION c " +
                                     "WHERE c.receiver_id = ? " +
                                     "  AND c.student_id = ? " +
                                     "  AND EXISTS (" +
                                     "      SELECT 1 FROM TEACHERS t " +
                                     "      JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                                     "      JOIN STUDENT_CLASS sc ON sc.class_id = cst.class_id " +
                                     "      WHERE t.user_id = c.sender_id AND sc.student_id = c.student_id" +
                                     "  )";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId)) {
                return 0;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            int unreadCount = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(teacherPartnerQuery)) {
                pstmt.setInt(1, parentUserId);
                pstmt.setInt(2, studentId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int teacherUserId = rs.getInt("teacher_user_id");
                    unreadCount += getConversationUnreadCount(conn, parentUserId, teacherUserId, studentId, readStateAvailable);
                }
            }
            return unreadCount;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getParentUnreadCountForTeacher(int parentUserId, int teacherUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId)) {
                return 0;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            return getConversationUnreadCount(conn, parentUserId, teacherUserId, studentId, readStateAvailable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int markParentWardMessagesAsSeen(int parentUserId, int studentId) {
        String query = "SELECT sender_id, MAX(message_id) AS last_message_id " +
                       "FROM COMMUNICATION " +
                       "WHERE receiver_id = ? AND student_id = ? " +
                       "GROUP BY sender_id";
        int updated = 0;
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId)) {
                return 0;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, parentUserId);
                pstmt.setInt(2, studentId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int partnerId = rs.getInt("sender_id");
                    int lastMessageId = rs.getInt("last_message_id");
                    boolean success = readStateAvailable
                        ? upsertReadState(conn, parentUserId, partnerId, studentId, lastMessageId)
                        : true;
                    if (!readStateAvailable) {
                        setMemoryLastSeenMessageId(parentUserId, partnerId, studentId, lastMessageId);
                    }
                    if (success) {
                        updated++;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updated;
    }

    public boolean markParentConversationAsSeen(int parentUserId, int teacherUserId, int studentId) {
        String query = "SELECT NVL(MAX(message_id), 0) AS last_message_id " +
                       "FROM COMMUNICATION " +
                       "WHERE receiver_id = ? AND sender_id = ? AND student_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId)) {
                return false;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, parentUserId);
                pstmt.setInt(2, teacherUserId);
                pstmt.setInt(3, studentId);
                ResultSet rs = pstmt.executeQuery();
                int lastMessageId = rs.next() ? rs.getInt("last_message_id") : 0;
                if (readStateAvailable) {
                    return upsertReadState(conn, parentUserId, teacherUserId, studentId, lastMessageId);
                }
                setMemoryLastSeenMessageId(parentUserId, teacherUserId, studentId, lastMessageId);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<ParentChatThreadRecord> getTeacherParentInboxThreads(int teacherUserId) {
        ObservableList<ParentChatThreadRecord> threads = FXCollections.observableArrayList();
        String queryWithReadState = "SELECT x.parent_user_id, x.parent_name, x.student_id, x.student_name, x.class_display, x.contact, " +
                                    "       NVL((SELECT COUNT(*) " +
                                    "            FROM COMMUNICATION c " +
                                    "            WHERE c.receiver_id = ? " +
                                    "              AND c.sender_id = x.parent_user_id " +
                                    "              AND c.student_id = x.student_id " +
                                    "              AND c.message_id > NVL((SELECT rs.last_seen_message_id " +
                                    "                                      FROM COMMUNICATION_READ_STATE rs " +
                                    "                                      WHERE rs.viewer_id = ? " +
                                    "                                        AND rs.partner_id = x.parent_user_id " +
                                    "                                        AND rs.student_id = x.student_id), 0)), 0) AS unread_count " +
                                    "FROM (" +
                                    "    SELECT DISTINCT ps.parent_id AS parent_user_id, up.name AS parent_name, " +
                                    "           s.student_id, us.name AS student_name, " +
                                    "           NVL(c.class_name || '-' || c.section || ' (' || c.academic_year || ')', '-') AS class_display, " +
                                    "           NVL(up.phone, up.email) AS contact " +
                                    "    FROM TEACHERS t " +
                                    "    JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                                    "    JOIN STUDENT_CLASS sc ON sc.class_id = cst.class_id " +
                                    "    JOIN STUDENTS s ON s.student_id = sc.student_id " +
                                    "    JOIN USERS us ON us.user_id = s.user_id " +
                                    "    JOIN PARENT_STUDENT ps ON ps.student_id = s.student_id " +
                                    "    JOIN USERS up ON up.user_id = ps.parent_id " +
                                    "    LEFT JOIN CLASSES c ON c.class_id = sc.class_id " +
                                    "    WHERE t.user_id = ?" +
                                    ") x " +
                                    "ORDER BY unread_count DESC, x.parent_name, x.student_name";
        String queryWithoutReadState = "SELECT DISTINCT ps.parent_id AS parent_user_id, up.name AS parent_name, " +
                                      "       s.student_id, us.name AS student_name, " +
                                      "       NVL(c.class_name || '-' || c.section || ' (' || c.academic_year || ')', '-') AS class_display, " +
                                      "       NVL(up.phone, up.email) AS contact, 0 AS unread_count " +
                                      "FROM TEACHERS t " +
                                      "JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                                      "JOIN STUDENT_CLASS sc ON sc.class_id = cst.class_id " +
                                      "JOIN STUDENTS s ON s.student_id = sc.student_id " +
                                      "JOIN USERS us ON us.user_id = s.user_id " +
                                      "JOIN PARENT_STUDENT ps ON ps.student_id = s.student_id " +
                                      "JOIN USERS up ON up.user_id = ps.parent_id " +
                                      "LEFT JOIN CLASSES c ON c.class_id = sc.class_id " +
                                      "WHERE t.user_id = ? " +
                                      "ORDER BY parent_name, student_name";
        try (Connection conn = DBConnection.getConnection()) {
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            String query = readStateAvailable ? queryWithReadState : queryWithoutReadState;
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                if (readStateAvailable) {
                    pstmt.setInt(1, teacherUserId);
                    pstmt.setInt(2, teacherUserId);
                    pstmt.setInt(3, teacherUserId);
                } else {
                    pstmt.setInt(1, teacherUserId);
                }
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    ParentChatThreadRecord thread = new ParentChatThreadRecord(
                        rs.getInt("parent_user_id"),
                        rs.getInt("student_id"),
                        rs.getString("parent_name"),
                        rs.getString("student_name"),
                        rs.getString("class_display"),
                        rs.getString("contact"),
                        rs.getInt("unread_count")
                    );
                    if (!readStateAvailable) {
                        int unread = getConversationUnreadCount(
                            conn,
                            teacherUserId,
                            thread.getParentUserId(),
                            thread.getStudentId(),
                            false
                        );
                        thread.setUnreadCount(unread);
                    }
                    threads.add(thread);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        FXCollections.sort(
            threads,
            Comparator.comparingInt(ParentChatThreadRecord::getUnreadCount)
                      .reversed()
                      .thenComparing(ParentChatThreadRecord::getParentName)
                      .thenComparing(ParentChatThreadRecord::getStudentName)
        );
        return threads;
    }

    public int getTeacherUnreadMessageCount(int teacherUserId) {
        String query = "SELECT COUNT(*) AS unread_count " +
                       "FROM COMMUNICATION c " +
                       "WHERE c.receiver_id = ? " +
                       "  AND EXISTS (SELECT 1 FROM PARENT_STUDENT ps WHERE ps.parent_id = c.sender_id AND ps.student_id = c.student_id) " +
                       "  AND EXISTS (" +
                       "      SELECT 1 FROM TEACHERS t " +
                       "      JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                       "      JOIN STUDENT_CLASS sc ON sc.class_id = cst.class_id " +
                       "      WHERE t.user_id = ? AND sc.student_id = c.student_id" +
                       "  ) " +
                       "  AND c.message_id > NVL((" +
                       "      SELECT rs.last_seen_message_id FROM COMMUNICATION_READ_STATE rs " +
                       "      WHERE rs.viewer_id = ? " +
                       "        AND rs.partner_id = c.sender_id " +
                       "        AND rs.student_id = c.student_id" +
                       "  ), 0)";
        String partnerQuery = "SELECT DISTINCT c.sender_id, c.student_id " +
                              "FROM COMMUNICATION c " +
                              "WHERE c.receiver_id = ? " +
                              "  AND EXISTS (SELECT 1 FROM PARENT_STUDENT ps WHERE ps.parent_id = c.sender_id AND ps.student_id = c.student_id) " +
                              "  AND EXISTS (" +
                              "      SELECT 1 FROM TEACHERS t " +
                              "      JOIN CLASS_SUBJECT_TEACHER cst ON t.teacher_id = cst.teacher_id " +
                              "      JOIN STUDENT_CLASS sc ON sc.class_id = cst.class_id " +
                              "      WHERE t.user_id = ? AND sc.student_id = c.student_id" +
                              "  )";
        try (Connection conn = DBConnection.getConnection()) {
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            if (!readStateAvailable) {
                int unreadCount = 0;
                try (PreparedStatement partnerStmt = conn.prepareStatement(partnerQuery)) {
                    partnerStmt.setInt(1, teacherUserId);
                    partnerStmt.setInt(2, teacherUserId);
                    ResultSet partnerRs = partnerStmt.executeQuery();
                    while (partnerRs.next()) {
                        int parentUserId = partnerRs.getInt("sender_id");
                        int studentId = partnerRs.getInt("student_id");
                        unreadCount += getConversationUnreadCount(conn, teacherUserId, parentUserId, studentId, false);
                    }
                }
                return unreadCount;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, teacherUserId);
                pstmt.setInt(2, teacherUserId);
                pstmt.setInt(3, teacherUserId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("unread_count");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int markTeacherMessagesAsSeen(int teacherUserId) {
        String query = "SELECT sender_id, student_id, MAX(message_id) AS last_message_id " +
                       "FROM COMMUNICATION " +
                       "WHERE receiver_id = ? " +
                       "GROUP BY sender_id, student_id";
        int updated = 0;
        try (Connection conn = DBConnection.getConnection()) {
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, teacherUserId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int parentUserId = rs.getInt("sender_id");
                    int studentId = rs.getInt("student_id");
                    int lastMessageId = rs.getInt("last_message_id");
                    if (!isTeacherParentStudentMapped(conn, teacherUserId, parentUserId, studentId)) {
                        continue;
                    }
                    boolean success = readStateAvailable
                        ? upsertReadState(conn, teacherUserId, parentUserId, studentId, lastMessageId)
                        : true;
                    if (!readStateAvailable) {
                        setMemoryLastSeenMessageId(teacherUserId, parentUserId, studentId, lastMessageId);
                    }
                    if (success) {
                        updated++;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return updated;
    }

    public boolean markTeacherConversationAsSeen(int teacherUserId, int parentUserId, int studentId) {
        String query = "SELECT NVL(MAX(message_id), 0) AS last_message_id " +
                       "FROM COMMUNICATION " +
                       "WHERE receiver_id = ? AND sender_id = ? AND student_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isTeacherParentStudentMapped(conn, teacherUserId, parentUserId, studentId)) {
                return false;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, teacherUserId);
                pstmt.setInt(2, parentUserId);
                pstmt.setInt(3, studentId);
                ResultSet rs = pstmt.executeQuery();
                int lastMessageId = rs.next() ? rs.getInt("last_message_id") : 0;
                if (readStateAvailable) {
                    return upsertReadState(conn, teacherUserId, parentUserId, studentId, lastMessageId);
                }
                setMemoryLastSeenMessageId(teacherUserId, parentUserId, studentId, lastMessageId);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<CommunicationMessage> getChatHistoryForTeacher(int teacherUserId, int parentUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isTeacherParentStudentMapped(conn, teacherUserId, parentUserId, studentId)) {
                return FXCollections.observableArrayList();
            }
            return getScopedConversation(conn, teacherUserId, parentUserId, studentId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return FXCollections.observableArrayList();
    }

    public boolean sendMessageToParent(int teacherUserId, int parentUserId, int studentId, String messageText) {
        String insertQuery = "INSERT INTO COMMUNICATION " +
                             "(message_id, sender_id, receiver_id, student_id, message_text, sent_at) " +
                             "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DBConnection.getConnection()) {
            if (!isTeacherParentStudentMapped(conn, teacherUserId, parentUserId, studentId)) {
                return false;
            }

            int nextMessageId = getNextCommunicationMessageId(conn);
            if (nextMessageId <= 0) {
                return false;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, nextMessageId);
                insertStmt.setInt(2, teacherUserId);
                insertStmt.setInt(3, parentUserId);
                insertStmt.setInt(4, studentId);
                insertStmt.setString(5, messageText);
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendMessageToTeacher(int parentId, int teacherUserId, int studentId, String messageText) {
        String teacherScopeQuery = "SELECT COUNT(*) " +
                                   "FROM STUDENT_CLASS sc " +
                                   "JOIN CLASS_SUBJECT_TEACHER cst ON sc.class_id = cst.class_id " +
                                   "JOIN TEACHERS t ON cst.teacher_id = t.teacher_id " +
                                   "WHERE sc.student_id = ? AND t.user_id = ?";
        String insertQuery = "INSERT INTO COMMUNICATION " +
                             "(message_id, sender_id, receiver_id, student_id, message_text, sent_at) " +
                             "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentWardMapped(conn, parentId, studentId)) {
                return false;
            }

            try (PreparedStatement scopeStmt = conn.prepareStatement(teacherScopeQuery)) {
                scopeStmt.setInt(1, studentId);
                scopeStmt.setInt(2, teacherUserId);
                ResultSet scopeRs = scopeStmt.executeQuery();
                if (!scopeRs.next() || scopeRs.getInt(1) == 0) {
                    return false;
                }
            }

            int nextMessageId = getNextCommunicationMessageId(conn);
            if (nextMessageId <= 0) {
                return false;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, nextMessageId);
                insertStmt.setInt(2, parentId);
                insertStmt.setInt(3, teacherUserId);
                insertStmt.setInt(4, studentId);
                insertStmt.setString(5, messageText);
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearChatHistoryForParent(int parentUserId, int teacherUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId)) {
                return false;
            }
            if (!isTeacherParentStudentMapped(conn, teacherUserId, parentUserId, studentId)) {
                return false;
            }
            clearConversationData(conn, parentUserId, teacherUserId, studentId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearChatHistoryForTeacher(int teacherUserId, int parentUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isTeacherParentStudentMapped(conn, teacherUserId, parentUserId, studentId)) {
                return false;
            }
            clearConversationData(conn, teacherUserId, parentUserId, studentId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<CounsellorContactRecord> getCounsellorContactsForParent(int parentUserId, int studentId) {
        ObservableList<CounsellorContactRecord> contacts = FXCollections.observableArrayList();
        String query = "SELECT u.user_id, u.name, u.email, NVL(u.phone, '-') AS phone " +
                       "FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                       "WHERE UPPER(r.role_name) = 'COUNSELLOR' " +
                       "ORDER BY u.name";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId)) {
                return contacts;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int counsellorUserId = rs.getInt("user_id");
                    int unread = getConversationUnreadCount(conn, parentUserId, counsellorUserId, studentId, readStateAvailable);
                    contacts.add(new CounsellorContactRecord(
                        counsellorUserId,
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        unread
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        FXCollections.sort(contacts, Comparator.comparingInt(CounsellorContactRecord::getUnreadCount).reversed().thenComparing(CounsellorContactRecord::getCounsellorName));
        return contacts;
    }

    public ObservableList<CounsellorContactRecord> getCounsellorContactsForStudent(int studentUserId, int studentId) {
        ObservableList<CounsellorContactRecord> contacts = FXCollections.observableArrayList();
        String query = "SELECT u.user_id, u.name, u.email, NVL(u.phone, '-') AS phone " +
                       "FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                       "WHERE UPPER(r.role_name) = 'COUNSELLOR' " +
                       "ORDER BY u.name";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isStudentOwner(conn, studentUserId, studentId)) {
                return contacts;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int counsellorUserId = rs.getInt("user_id");
                    int unread = getConversationUnreadCount(conn, studentUserId, counsellorUserId, studentId, readStateAvailable);
                    contacts.add(new CounsellorContactRecord(
                        counsellorUserId,
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        unread
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        FXCollections.sort(contacts, Comparator.comparingInt(CounsellorContactRecord::getUnreadCount).reversed().thenComparing(CounsellorContactRecord::getCounsellorName));
        return contacts;
    }

    public int getParentCounsellorUnreadMessageCount(int parentUserId, int studentId) {
        int unreadCount = 0;
        ObservableList<CounsellorContactRecord> contacts = getCounsellorContactsForParent(parentUserId, studentId);
        for (CounsellorContactRecord contact : contacts) {
            unreadCount += contact.getUnreadCount();
        }
        return unreadCount;
    }

    public int getStudentCounsellorUnreadMessageCount(int studentUserId, int studentId) {
        int unreadCount = 0;
        ObservableList<CounsellorContactRecord> contacts = getCounsellorContactsForStudent(studentUserId, studentId);
        for (CounsellorContactRecord contact : contacts) {
            unreadCount += contact.getUnreadCount();
        }
        return unreadCount;
    }

    public ObservableList<CounsellorInboxRecord> getCounsellorInboxForCounsellor(int counsellorUserId) {
        ObservableList<CounsellorInboxRecord> records = FXCollections.observableArrayList();
        String query = "SELECT target_user_id, student_id, participant_name, participant_type, student_name, class_display, contact " +
                       "FROM (" +
                       "  SELECT p.user_id AS target_user_id, stu.student_id, p.name AS participant_name, 'Parent' AS participant_type, " +
                       "         su.name AS student_name, NVL(c.class_name || '-' || c.section || ' (' || c.academic_year || ')', '-') AS class_display, " +
                       "         NVL(p.phone, p.email) AS contact " +
                       "  FROM PARENT_STUDENT ps " +
                       "  JOIN USERS p ON ps.parent_id = p.user_id " +
                       "  JOIN STUDENTS stu ON ps.student_id = stu.student_id " +
                       "  JOIN USERS su ON stu.user_id = su.user_id " +
                       "  LEFT JOIN STUDENT_CLASS sc ON stu.student_id = sc.student_id " +
                       "  LEFT JOIN CLASSES c ON sc.class_id = c.class_id " +
                       "  UNION " +
                       "  SELECT su.user_id AS target_user_id, stu.student_id, su.name AS participant_name, 'Student' AS participant_type, " +
                       "         su.name AS student_name, NVL(c.class_name || '-' || c.section || ' (' || c.academic_year || ')', '-') AS class_display, " +
                       "         NVL(su.phone, su.email) AS contact " +
                       "  FROM STUDENTS stu " +
                       "  JOIN USERS su ON stu.user_id = su.user_id " +
                       "  LEFT JOIN STUDENT_CLASS sc ON stu.student_id = sc.student_id " +
                       "  LEFT JOIN CLASSES c ON sc.class_id = c.class_id " +
                       ") x " +
                       "ORDER BY participant_type, participant_name";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isCounsellorUser(conn, counsellorUserId)) {
                return records;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int targetUserId = rs.getInt("target_user_id");
                    int studentId = rs.getInt("student_id");
                    int unread = getConversationUnreadCount(conn, counsellorUserId, targetUserId, studentId, readStateAvailable);
                    records.add(new CounsellorInboxRecord(
                        targetUserId,
                        studentId,
                        rs.getString("participant_name"),
                        rs.getString("participant_type"),
                        rs.getString("student_name"),
                        rs.getString("class_display"),
                        rs.getString("contact"),
                        unread
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        FXCollections.sort(
            records,
            Comparator.comparingInt(CounsellorInboxRecord::getUnreadCount).reversed()
                      .thenComparing(CounsellorInboxRecord::getParticipantType)
                      .thenComparing(CounsellorInboxRecord::getParticipantName)
        );
        return records;
    }

    public int getCounsellorUnreadMessageCount(int counsellorUserId) {
        int unreadCount = 0;
        ObservableList<CounsellorInboxRecord> records = getCounsellorInboxForCounsellor(counsellorUserId);
        for (CounsellorInboxRecord record : records) {
            unreadCount += record.getUnreadCount();
        }
        return unreadCount;
    }

    public ObservableList<CommunicationMessage> getCounsellorChatHistoryForParent(int parentUserId, int counsellorUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId) || !isCounsellorUser(conn, counsellorUserId)) {
                return FXCollections.observableArrayList();
            }
            return getScopedConversation(conn, parentUserId, counsellorUserId, studentId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return FXCollections.observableArrayList();
    }

    public ObservableList<CommunicationMessage> getCounsellorChatHistoryForStudent(int studentUserId, int counsellorUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isStudentOwner(conn, studentUserId, studentId) || !isCounsellorUser(conn, counsellorUserId)) {
                return FXCollections.observableArrayList();
            }
            return getScopedConversation(conn, studentUserId, counsellorUserId, studentId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return FXCollections.observableArrayList();
    }

    public ObservableList<CommunicationMessage> getCounsellorChatHistoryForCounsellor(int counsellorUserId, int targetUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isCounsellorUser(conn, counsellorUserId)) {
                return FXCollections.observableArrayList();
            }
            return getScopedConversation(conn, counsellorUserId, targetUserId, studentId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return FXCollections.observableArrayList();
    }

    public boolean markParentCounsellorConversationAsSeen(int parentUserId, int counsellorUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId) || !isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            return markConversationSeenForUsers(conn, parentUserId, counsellorUserId, studentId, readStateAvailable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markStudentCounsellorConversationAsSeen(int studentUserId, int counsellorUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isStudentOwner(conn, studentUserId, studentId) || !isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            return markConversationSeenForUsers(conn, studentUserId, counsellorUserId, studentId, readStateAvailable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean markCounsellorConversationAsSeen(int counsellorUserId, int targetUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            boolean readStateAvailable = ensureCommunicationReadStateInfrastructure(conn);
            return markConversationSeenForUsers(conn, counsellorUserId, targetUserId, studentId, readStateAvailable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendMessageToCounsellorFromParent(int parentUserId, int counsellorUserId, int studentId, String messageText) {
        String insertQuery = "INSERT INTO COMMUNICATION (message_id, sender_id, receiver_id, student_id, message_text, sent_at) " +
                             "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId) || !isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            int nextMessageId = getNextCommunicationMessageId(conn);
            if (nextMessageId <= 0) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setInt(1, nextMessageId);
                pstmt.setInt(2, parentUserId);
                pstmt.setInt(3, counsellorUserId);
                pstmt.setInt(4, studentId);
                pstmt.setString(5, messageText);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendMessageToCounsellorFromStudent(int studentUserId, int counsellorUserId, int studentId, String messageText) {
        String insertQuery = "INSERT INTO COMMUNICATION (message_id, sender_id, receiver_id, student_id, message_text, sent_at) " +
                             "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isStudentOwner(conn, studentUserId, studentId) || !isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            int nextMessageId = getNextCommunicationMessageId(conn);
            if (nextMessageId <= 0) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setInt(1, nextMessageId);
                pstmt.setInt(2, studentUserId);
                pstmt.setInt(3, counsellorUserId);
                pstmt.setInt(4, studentId);
                pstmt.setString(5, messageText);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendMessageFromCounsellor(int counsellorUserId, int targetUserId, int studentId, String messageText) {
        String insertQuery = "INSERT INTO COMMUNICATION (message_id, sender_id, receiver_id, student_id, message_text, sent_at) " +
                             "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            int nextMessageId = getNextCommunicationMessageId(conn);
            if (nextMessageId <= 0) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setInt(1, nextMessageId);
                pstmt.setInt(2, counsellorUserId);
                pstmt.setInt(3, targetUserId);
                pstmt.setInt(4, studentId);
                pstmt.setString(5, messageText);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearChatHistoryForParentCounsellor(int parentUserId, int counsellorUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isParentMappedToStudent(conn, parentUserId, studentId) || !isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            clearConversationData(conn, parentUserId, counsellorUserId, studentId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearChatHistoryForStudentCounsellor(int studentUserId, int counsellorUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isStudentOwner(conn, studentUserId, studentId) || !isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            clearConversationData(conn, studentUserId, counsellorUserId, studentId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearChatHistoryForCounsellor(int counsellorUserId, int targetUserId, int studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            clearConversationData(conn, counsellorUserId, targetUserId, studentId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createCounsellingRequest(int requesterUserId, int studentId, java.sql.Date preferredDate, String category, String primaryConcern) {
        String insertQuery = "INSERT INTO COUNSELLING " +
                             "(session_id, student_id, counsellor_id, session_date, notes, status, category) " +
                             "VALUES (?, ?, ?, ?, ?, 'Pending', ?)";
        try (Connection conn = DBConnection.getConnection()) {
            boolean requesterAllowed = isStudentOwner(conn, requesterUserId, studentId)
                || isParentMappedToStudent(conn, requesterUserId, studentId);
            if (!requesterAllowed) {
                return false;
            }
            int counsellorUserId = getDefaultCounsellorUserId(conn);
            if (counsellorUserId <= 0) {
                return false;
            }
            int nextSessionId = getNextCounsellingSessionId(conn);
            if (nextSessionId <= 0) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setInt(1, nextSessionId);
                pstmt.setInt(2, studentId);
                pstmt.setInt(3, counsellorUserId);
                pstmt.setDate(4, preferredDate);
                pstmt.setString(5, primaryConcern);
                pstmt.setString(6, category);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getCounsellorPendingRequestCount(int counsellorUserId) {
        String query = "SELECT COUNT(*) AS total FROM COUNSELLING WHERE COUNSELLOR_ID = ? AND STATUS = 'Pending'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, counsellorUserId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getCounsellorActiveCaseCount(int counsellorUserId) {
        String query = "SELECT COUNT(*) AS total FROM COUNSELLING WHERE COUNSELLOR_ID = ? AND STATUS IN ('Pending', 'Scheduled')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, counsellorUserId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public ObservableList<CounsellingCaseRecord> getCounsellingCasesForCounsellor(int counsellorUserId) {
        ObservableList<CounsellingCaseRecord> cases = FXCollections.observableArrayList();
        String query = "SELECT c.session_id, c.student_id, u.name AS student_name, " +
                       "NVL(cls.class_name || '-' || cls.section || ' (' || cls.academic_year || ')', '-') AS class_display, " +
                       "TO_CHAR(c.session_date, 'YYYY-MM-DD') AS session_date, " +
                       "NVL(c.status, 'Pending') AS status, NVL(c.category, 'Academic') AS category, NVL(c.notes, '') AS notes " +
                       "FROM COUNSELLING c " +
                       "JOIN STUDENTS s ON c.student_id = s.student_id " +
                       "JOIN USERS u ON s.user_id = u.user_id " +
                       "LEFT JOIN STUDENT_CLASS sc ON s.student_id = sc.student_id " +
                       "LEFT JOIN CLASSES cls ON sc.class_id = cls.class_id " +
                       "WHERE c.counsellor_id = ? " +
                       "ORDER BY CASE NVL(c.status, 'Pending') " +
                       "    WHEN 'Pending' THEN 1 " +
                       "    WHEN 'Scheduled' THEN 2 " +
                       "    WHEN 'Completed' THEN 3 " +
                       "    ELSE 4 END, c.session_date DESC, c.session_id DESC";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isCounsellorUser(conn, counsellorUserId)) {
                return cases;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, counsellorUserId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    cases.add(new CounsellingCaseRecord(
                        rs.getInt("session_id"),
                        rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("class_display"),
                        rs.getString("session_date"),
                        rs.getString("status"),
                        rs.getString("category"),
                        rs.getString("notes")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cases;
    }

    public boolean updateCounsellingRequestStatus(int counsellorUserId, int sessionId, String newStatus) {
        if (!"Pending".equals(newStatus) && !"Scheduled".equals(newStatus) && !"Completed".equals(newStatus)) {
            return false;
        }
        String query = "UPDATE COUNSELLING SET STATUS = ? WHERE SESSION_ID = ? AND COUNSELLOR_ID = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isCounsellorUser(conn, counsellorUserId)) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, newStatus);
                pstmt.setInt(2, sessionId);
                pstmt.setInt(3, counsellorUserId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<CounsellingRequestRecord> getPrivateCounsellingNotes(int loggedInUserId, int studentId) {
        ObservableList<CounsellingRequestRecord> notes = FXCollections.observableArrayList();
        String query = "SELECT session_id, student_id, counsellor_id, TO_CHAR(session_date, 'YYYY-MM-DD') AS session_date, " +
                       "NVL(status, 'Pending') AS status, NVL(category, 'Academic') AS category, NVL(notes, '') AS notes " +
                       "FROM COUNSELLING " +
                       "WHERE student_id = ? " +
                       "  AND (? = counsellor_id OR EXISTS (" +
                       "      SELECT 1 FROM PARENT_STUDENT ps WHERE ps.student_id = COUNSELLING.student_id AND ps.parent_id = ?" +
                       "  )) " +
                       "ORDER BY session_date DESC, session_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            boolean isCounsellor = isCounsellorUser(conn, loggedInUserId);
            boolean isParent = isParentMappedToStudent(conn, loggedInUserId, studentId);
            if (!isCounsellor && !isParent) {
                return notes;
            }

            pstmt.setInt(1, studentId);
            pstmt.setInt(2, loggedInUserId);
            pstmt.setInt(3, loggedInUserId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notes.add(new CounsellingRequestRecord(
                    rs.getInt("session_id"),
                    rs.getInt("student_id"),
                    rs.getInt("counsellor_id"),
                    rs.getString("session_date"),
                    rs.getString("status"),
                    rs.getString("category"),
                    rs.getString("notes")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    private boolean isAdminUser(Connection conn, int userId) throws SQLException {
        return isUserInRole(conn, userId, "Admin");
    }

    public boolean canEditUser(int loggedInAdminId, int targetUserId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, loggedInAdminId)) {
                return false;
            }
            boolean targetIsAdmin = isAdminUser(conn, targetUserId);
            return !targetIsAdmin || loggedInAdminId == targetUserId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<AdminUserRecord> getAdminUsers() {
        ObservableList<AdminUserRecord> users = FXCollections.observableArrayList();
        try (Connection conn = DBConnection.getConnection()) {
            boolean hasActiveColumn = columnExists(conn, "USERS", "IS_ACTIVE");
            String activeSelect = hasActiveColumn ? "u.is_active" : "1";
            String query = "SELECT u.user_id, u.name, u.email, r.role_name, NVL(u.phone, '-') AS phone, " +
                           activeSelect + " AS is_active " +
                           "FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                           "ORDER BY r.role_name, u.name";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    users.add(new AdminUserRecord(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role_name"),
                        rs.getString("phone"),
                        rs.getInt("is_active")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public LinkedHashMap<Integer, String> getAdminDeletableUserOptions(int adminUserId) {
        LinkedHashMap<Integer, String> options = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId)) {
                return options;
            }
            boolean hasActiveColumn = columnExists(conn, "USERS", "IS_ACTIVE");
            String displaySelect = hasActiveColumn
                ? "r.role_name || ': ' || u.name || ' (' || u.email || ') - ' || CASE u.is_active WHEN 1 THEN 'Active' ELSE 'Inactive' END AS display_name "
                : "r.role_name || ': ' || u.name || ' (' || u.email || ')' AS display_name ";
            String query = "SELECT u.user_id, " + displaySelect +
                           "FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                           "WHERE u.user_id <> ? AND UPPER(r.role_name) <> 'ADMIN' " +
                           "ORDER BY r.role_name, u.name";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, adminUserId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    options.put(rs.getInt("user_id"), rs.getString("display_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return options;
    }

    public LinkedHashMap<Integer, String> getAdminRoleOptions() {
        return getIdNameOptions("SELECT role_id, role_name FROM ROLES ORDER BY role_id", "role_id", "role_name");
    }

    public LinkedHashMap<Integer, String> getAdminClassOptions() {
        return getIdNameOptions(
            "SELECT class_id, class_name || '-' || section || ' (' || academic_year || ')' AS display_name FROM CLASSES ORDER BY class_name, section",
            "class_id",
            "display_name"
        );
    }

    public boolean createClassAsAdmin(int adminUserId, String className, String section, String academicYear) {
        String query = "INSERT INTO CLASSES (class_id, class_name, section, academic_year) " +
                       "SELECT ?, ?, ?, ? FROM dual WHERE NOT EXISTS (" +
                       "SELECT 1 FROM CLASSES WHERE UPPER(class_name) = UPPER(?) " +
                       "AND UPPER(section) = UPPER(?) AND academic_year = ?)";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId)) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, getNextId(conn, "CLASSES", "CLASS_ID"));
                pstmt.setString(2, className);
                pstmt.setString(3, section);
                pstmt.setString(4, academicYear);
                pstmt.setString(5, className);
                pstmt.setString(6, section);
                pstmt.setString(7, academicYear);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public LinkedHashMap<Integer, String> getAdminSubjectOptions() {
        return getIdNameOptions("SELECT subject_id, subject_name FROM SUBJECTS ORDER BY subject_name", "subject_id", "subject_name");
    }

    public LinkedHashMap<Integer, String> getAdminTeacherOptions() {
        return getIdNameOptions(
            "SELECT t.teacher_id, u.name || ' (' || u.email || ')' AS display_name FROM TEACHERS t JOIN USERS u ON t.user_id = u.user_id ORDER BY u.name",
            "teacher_id",
            "display_name"
        );
    }

    public LinkedHashMap<Integer, String> getAdminParentOptions() {
        return getIdNameOptions(
            "SELECT u.user_id, u.name || ' (' || u.email || ')' AS display_name FROM USERS u JOIN ROLES r ON u.role_id = r.role_id WHERE UPPER(r.role_name) = 'PARENT' ORDER BY u.name",
            "user_id",
            "display_name"
        );
    }

    public LinkedHashMap<Integer, String> getAdminStudentOptions() {
        return getIdNameOptions(
            "SELECT s.student_id, u.name || ' (' || u.email || ')' AS display_name FROM STUDENTS s JOIN USERS u ON s.user_id = u.user_id ORDER BY u.name",
            "student_id",
            "display_name"
        );
    }

    private LinkedHashMap<Integer, String> getIdNameOptions(String query, String idColumn, String nameColumn) {
        LinkedHashMap<Integer, String> options = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                options.put(rs.getInt(idColumn), rs.getString(nameColumn));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return options;
    }

    private boolean userExists(Connection conn, int userId) throws SQLException {
        String query = "SELECT COUNT(*) AS user_count FROM USERS WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt("user_count") > 0;
        }
    }

    private int getOptionalId(Connection conn, String query, int id) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    private int executeUpdate(Connection conn, String query, Object... params) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int index = 0; index < params.length; index++) {
                pstmt.setObject(index + 1, params[index]);
            }
            return pstmt.executeUpdate();
        }
    }

    private int getRoleIdByName(Connection conn, String roleName) throws SQLException {
        String query = "SELECT role_id FROM ROLES WHERE UPPER(role_name) = UPPER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, roleName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("role_id");
            }
        }
        return -1;
    }

    private int getNextId(Connection conn, String tableName, String idColumn) throws SQLException {
        return SequenceService.nextVal(conn, sequenceNameFor(tableName), tableName, idColumn);
    }

    private String sequenceNameFor(String tableName) {
        switch (tableName.toUpperCase()) {
            case "ASSIGNMENTS":
                return "SEQ_ASSIGNMENTS";
            case "ATTENDANCE":
                return "SEQ_ATTENDANCE";
            case "CLASSES":
                return "SEQ_CLASSES";
            case "COMMUNICATION":
                return "SEQ_COMMUNICATION";
            case "COUNSELLING":
                return "SEQ_COUNSELLING";
            case "LOGIN_AUDIT":
                return "SEQ_LOGIN_AUDIT";
            case "MARKS":
                return "SEQ_MARKS";
            case "QUESTION_PAPERS":
                return "SEQ_QUESTION_PAPERS";
            case "STUDENTS":
                return "SEQ_STUDENTS";
            case "SUBMISSIONS":
                return "SEQ_SUBMISSIONS";
            case "TEACHERS":
                return "SEQ_TEACHERS";
            case "USERS":
                return "SEQ_USERS";
            default:
                throw new IllegalArgumentException("No sequence configured for table " + tableName);
        }
    }

    private int getClassStudentCount(Connection conn, int classId) throws SQLException {
        String query = "SELECT COUNT(DISTINCT student_id) AS student_count FROM STUDENT_CLASS WHERE class_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, classId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("student_count");
            }
        }
        return 0;
    }

    private boolean studentAlreadyInClass(Connection conn, int studentId, int classId) throws SQLException {
        String query = "SELECT COUNT(*) AS match_count FROM STUDENT_CLASS WHERE student_id = ? AND class_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, classId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt("match_count") > 0;
        }
    }

    private int getClassTeacherCount(Connection conn, int classId) throws SQLException {
        String query = "SELECT COUNT(DISTINCT teacher_id) AS teacher_count FROM CLASS_SUBJECT_TEACHER WHERE class_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, classId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("teacher_count");
            }
        }
        return 0;
    }

    private boolean teacherAlreadyInClass(Connection conn, int classId, int teacherId) throws SQLException {
        String query = "SELECT COUNT(*) AS match_count FROM CLASS_SUBJECT_TEACHER WHERE class_id = ? AND teacher_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, classId);
            pstmt.setInt(2, teacherId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt("match_count") > 0;
        }
    }

    public boolean createUserAsAdmin(
        int adminUserId,
        String roleName,
        String name,
        String email,
        String passwordHash,
        String phone,
        String qualification,
        int experience,
        java.sql.Date dob,
        String gender,
        String conduct,
        String conductRemarks,
        int classId,
        int parentUserId,
        String relation
    ) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId)) {
                return false;
            }
            int roleId = getRoleIdByName(conn, roleName);
            if (roleId <= 0) {
                return false;
            }
            if ("Student".equalsIgnoreCase(roleName) && getClassStudentCount(conn, classId) >= 60) {
                return false;
            }
            ensureUserCreationSequences(conn, roleName);
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int nextUserId = getNextId(conn, "USERS", "USER_ID");
                boolean hasActiveColumn = columnExists(conn, "USERS", "IS_ACTIVE");
                String userInsert = hasActiveColumn
                    ? "INSERT INTO USERS (user_id, name, email, password_hash, role_id, phone, created_at, is_active) VALUES (?, ?, ?, ?, ?, ?, SYSDATE, 1)"
                    : "INSERT INTO USERS (user_id, name, email, password_hash, role_id, phone, created_at) VALUES (?, ?, ?, ?, ?, ?, SYSDATE)";
                try (PreparedStatement pstmt = conn.prepareStatement(userInsert)) {
                    pstmt.setInt(1, nextUserId);
                    pstmt.setString(2, name);
                    pstmt.setString(3, email);
                    pstmt.setString(4, passwordHash);
                    pstmt.setInt(5, roleId);
                    pstmt.setString(6, phone);
                    pstmt.executeUpdate();
                }

                if ("Teacher".equalsIgnoreCase(roleName)) {
                    int nextTeacherId = getNextId(conn, "TEACHERS", "TEACHER_ID");
                    try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO TEACHERS (teacher_id, user_id, qualification, experience) VALUES (?, ?, ?, ?)"
                    )) {
                        pstmt.setInt(1, nextTeacherId);
                        pstmt.setInt(2, nextUserId);
                        pstmt.setString(3, qualification);
                        pstmt.setInt(4, experience);
                        pstmt.executeUpdate();
                    }
                } else if ("Student".equalsIgnoreCase(roleName)) {
                    int nextStudentId = getNextId(conn, "STUDENTS", "STUDENT_ID");
                    try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO STUDENTS (student_id, user_id, dob, gender, conduct, conduct_remarks) VALUES (?, ?, ?, ?, ?, ?)"
                    )) {
                        pstmt.setInt(1, nextStudentId);
                        pstmt.setInt(2, nextUserId);
                        pstmt.setDate(3, dob);
                        pstmt.setString(4, gender);
                        pstmt.setString(5, conduct);
                        pstmt.setString(6, conductRemarks);
                        pstmt.executeUpdate();
                    }
                    try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO STUDENT_CLASS (student_id, class_id) VALUES (?, ?)"
                    )) {
                        pstmt.setInt(1, nextStudentId);
                        pstmt.setInt(2, classId);
                        pstmt.executeUpdate();
                    }
                    try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO PARENT_STUDENT (parent_id, student_id, relation) VALUES (?, ?, ?)"
                    )) {
                        pstmt.setInt(1, parentUserId);
                        pstmt.setInt(2, nextStudentId);
                        pstmt.setString(3, relation);
                        pstmt.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void ensureUserCreationSequences(Connection conn, String roleName) throws SQLException {
        if ("Teacher".equalsIgnoreCase(roleName)) {
            SequenceService.ensureSequences(conn, new String[][] {
                {"SEQ_USERS", "USERS", "USER_ID"},
                {"SEQ_TEACHERS", "TEACHERS", "TEACHER_ID"}
            });
        } else if ("Student".equalsIgnoreCase(roleName)) {
            SequenceService.ensureSequences(conn, new String[][] {
                {"SEQ_USERS", "USERS", "USER_ID"},
                {"SEQ_STUDENTS", "STUDENTS", "STUDENT_ID"}
            });
        } else {
            SequenceService.ensureSequence(conn, "SEQ_USERS", "USERS", "USER_ID");
        }
    }

    public boolean updateAdminManagedUserBasic(int adminUserId, int targetUserId, String name, String email, String phone) {
        if (!canEditUser(adminUserId, targetUserId)) {
            return false;
        }
        String query = "UPDATE USERS SET name = ?, email = ?, phone = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, phone);
            pstmt.setInt(4, targetUserId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setUserActiveStatus(int adminUserId, int targetUserId, boolean active) {
        if (!canEditUser(adminUserId, targetUserId)) {
            return false;
        }
        if (adminUserId == targetUserId && !active) {
            return false;
        }
        try (Connection conn = DBConnection.getConnection()) {
            if (!columnExists(conn, "USERS", "IS_ACTIVE")) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE USERS SET is_active = ? WHERE user_id = ?")) {
                pstmt.setInt(1, active ? 1 : 0);
                pstmt.setInt(2, targetUserId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteUserAsAdmin(int adminUserId, int targetUserId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId) || adminUserId == targetUserId || isAdminUser(conn, targetUserId)) {
                return false;
            }
            if (!userExists(conn, targetUserId)) {
                return false;
            }

            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int studentId = getOptionalId(conn, "SELECT student_id FROM STUDENTS WHERE user_id = ?", targetUserId);
                int teacherId = getOptionalId(conn, "SELECT teacher_id FROM TEACHERS WHERE user_id = ?", targetUserId);

                if (tableExists(conn, "COMMUNICATION_READ_STATE")) {
                    if (studentId > 0) {
                        executeUpdate(conn, "DELETE FROM COMMUNICATION_READ_STATE WHERE student_id = ?", studentId);
                    }
                    executeUpdate(conn, "DELETE FROM COMMUNICATION_READ_STATE WHERE viewer_id = ? OR partner_id = ?", targetUserId, targetUserId);
                }

                if (tableExists(conn, "COMMUNICATION")) {
                    if (studentId > 0) {
                        executeUpdate(conn, "DELETE FROM COMMUNICATION WHERE student_id = ?", studentId);
                    }
                    executeUpdate(conn, "DELETE FROM COMMUNICATION WHERE sender_id = ? OR receiver_id = ?", targetUserId, targetUserId);
                }

                if (studentId > 0) {
                    executeUpdate(conn, "DELETE FROM COUNSELLING WHERE student_id = ?", studentId);
                    executeUpdate(conn, "DELETE FROM MARKS WHERE student_id = ?", studentId);
                    executeUpdate(conn, "DELETE FROM SUBMISSIONS WHERE student_id = ?", studentId);
                    executeUpdate(conn, "DELETE FROM PARENT_STUDENT WHERE student_id = ?", studentId);
                    executeUpdate(conn, "DELETE FROM STUDENT_CLASS WHERE student_id = ?", studentId);
                }

                executeUpdate(conn, "UPDATE COUNSELLING SET counsellor_id = NULL WHERE counsellor_id = ?", targetUserId);
                executeUpdate(conn, "DELETE FROM PARENT_STUDENT WHERE parent_id = ?", targetUserId);
                executeUpdate(conn, "UPDATE ATTENDANCE SET approved_by = NULL WHERE approved_by = ?", targetUserId);
                executeUpdate(conn, "DELETE FROM ATTENDANCE WHERE user_id = ?", targetUserId);
                executeUpdate(conn, "DELETE FROM LOGIN_AUDIT WHERE user_id = ?", targetUserId);

                if (teacherId > 0) {
                    if (tableExists(conn, "QUESTION_BANK")) {
                        executeUpdate(conn, "UPDATE QUESTION_BANK SET teacher_id = NULL WHERE teacher_id = ?", teacherId);
                    }
                    if (columnExists(conn, "QUESTION_PAPERS", "CREATED_BY_TEACHER_ID")) {
                        executeUpdate(conn, "UPDATE QUESTION_PAPERS SET created_by_teacher_id = NULL WHERE created_by_teacher_id = ?", teacherId);
                    }
                    executeUpdate(conn, "DELETE FROM CLASS_SUBJECT_TEACHER WHERE teacher_id = ?", teacherId);
                    executeUpdate(conn, "DELETE FROM TEACHERS WHERE teacher_id = ?", teacherId);
                }

                if (studentId > 0) {
                    executeUpdate(conn, "DELETE FROM STUDENTS WHERE student_id = ?", studentId);
                }

                int deleted = executeUpdate(conn, "DELETE FROM USERS WHERE user_id = ?", targetUserId);
                conn.commit();
                return deleted > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<AdminTeacherMappingRecord> getTeacherSubjectClassMappings() {
        ObservableList<AdminTeacherMappingRecord> records = FXCollections.observableArrayList();
        String query = "SELECT cst.class_id, cst.subject_id, cst.teacher_id, " +
                       "c.class_name || '-' || c.section || ' (' || c.academic_year || ')' AS class_display, " +
                       "s.subject_name, u.name AS teacher_name " +
                       "FROM CLASS_SUBJECT_TEACHER cst " +
                       "JOIN CLASSES c ON cst.class_id = c.class_id " +
                       "JOIN SUBJECTS s ON cst.subject_id = s.subject_id " +
                       "JOIN TEACHERS t ON cst.teacher_id = t.teacher_id " +
                       "JOIN USERS u ON t.user_id = u.user_id " +
                       "ORDER BY c.class_name, c.section, s.subject_name, u.name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new AdminTeacherMappingRecord(
                    rs.getInt("class_id"),
                    rs.getInt("subject_id"),
                    rs.getInt("teacher_id"),
                    rs.getString("class_display"),
                    rs.getString("subject_name"),
                    rs.getString("teacher_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public boolean addTeacherSubjectClassMapping(int adminUserId, int classId, int subjectId, int teacherId) {
        String query = "INSERT INTO CLASS_SUBJECT_TEACHER (class_id, subject_id, teacher_id) " +
                       "SELECT ?, ?, ? FROM dual WHERE NOT EXISTS (" +
                       "SELECT 1 FROM CLASS_SUBJECT_TEACHER WHERE class_id = ? AND subject_id = ? AND teacher_id = ?)";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId)) {
                return false;
            }
            if (!teacherAlreadyInClass(conn, classId, teacherId) && getClassTeacherCount(conn, classId) >= 2) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, classId);
                pstmt.setInt(2, subjectId);
                pstmt.setInt(3, teacherId);
                pstmt.setInt(4, classId);
                pstmt.setInt(5, subjectId);
                pstmt.setInt(6, teacherId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<AdminStudentClassRecord> getStudentClassMappings() {
        ObservableList<AdminStudentClassRecord> records = FXCollections.observableArrayList();
        String query = "SELECT s.student_id, u.name AS student_name, " +
                       "NVL(c.class_name || '-' || c.section || ' (' || c.academic_year || ')', '-') AS class_display " +
                       "FROM STUDENTS s JOIN USERS u ON s.user_id = u.user_id " +
                       "LEFT JOIN STUDENT_CLASS sc ON s.student_id = sc.student_id " +
                       "LEFT JOIN CLASSES c ON sc.class_id = c.class_id " +
                       "ORDER BY u.name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new AdminStudentClassRecord(
                    rs.getInt("student_id"),
                    rs.getString("student_name"),
                    rs.getString("class_display")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public boolean moveStudentToClass(int adminUserId, int studentId, int classId) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId)) {
                return false;
            }
            if (!studentAlreadyInClass(conn, studentId, classId) && getClassStudentCount(conn, classId) >= 60) {
                return false;
            }
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM STUDENT_CLASS WHERE student_id = ?")) {
                    deleteStmt.setInt(1, studentId);
                    deleteStmt.executeUpdate();
                }
                try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO STUDENT_CLASS (student_id, class_id) VALUES (?, ?)")) {
                    insertStmt.setInt(1, studentId);
                    insertStmt.setInt(2, classId);
                    insertStmt.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ObservableList<AdminParentLinkRecord> getParentStudentLinks() {
        ObservableList<AdminParentLinkRecord> records = FXCollections.observableArrayList();
        String query = "SELECT ps.parent_id, ps.student_id, p.name AS parent_name, su.name AS student_name, ps.relation " +
                       "FROM PARENT_STUDENT ps " +
                       "JOIN USERS p ON ps.parent_id = p.user_id " +
                       "JOIN STUDENTS s ON ps.student_id = s.student_id " +
                       "JOIN USERS su ON s.user_id = su.user_id " +
                       "ORDER BY p.name, su.name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new AdminParentLinkRecord(
                    rs.getInt("parent_id"),
                    rs.getInt("student_id"),
                    rs.getString("parent_name"),
                    rs.getString("student_name"),
                    rs.getString("relation")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public boolean linkParentToStudent(int adminUserId, int parentUserId, int studentId, String relation) {
        String query = "MERGE INTO PARENT_STUDENT target " +
                       "USING (SELECT ? AS parent_id, ? AS student_id, ? AS relation FROM dual) src " +
                       "ON (target.parent_id = src.parent_id AND target.student_id = src.student_id) " +
                       "WHEN MATCHED THEN UPDATE SET target.relation = src.relation " +
                       "WHEN NOT MATCHED THEN INSERT (parent_id, student_id, relation) VALUES (src.parent_id, src.student_id, src.relation)";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId)) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, parentUserId);
                pstmt.setInt(2, studentId);
                pstmt.setString(3, relation);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
