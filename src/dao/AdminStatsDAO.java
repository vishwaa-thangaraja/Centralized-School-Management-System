package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminStatsDAO {
    private final LoginAuditDAO loginAuditDAO = new LoginAuditDAO();

    public Map<String, Integer> getDashboardStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        loginAuditDAO.cleanupStaleLoginAudits();
        stats.put("students", getScalarCount("SELECT COUNT(*) FROM STUDENTS"));
        stats.put("teachers", getScalarCount("SELECT COUNT(*) FROM TEACHERS"));
        stats.put("counsellors", getScalarCount(
            "SELECT COUNT(*) FROM USERS u JOIN ROLES r ON u.role_id = r.role_id WHERE UPPER(r.role_name) = 'COUNSELLOR'"
        ));
        stats.put("classes", getScalarCount("SELECT COUNT(*) FROM CLASSES"));
        stats.put("activeSessions", getScalarCount("SELECT COUNT(*) FROM LOGIN_AUDIT WHERE LOGOUT_TIME IS NULL"));
        stats.put("redFlags", getScalarCount(
            "SELECT COUNT(DISTINCT m.student_id) " +
            "FROM MARKS m JOIN QUESTION_PAPERS qp ON m.qp_id = qp.qp_id " +
            "WHERE m.marks_obtained IS NOT NULL AND qp.max_marks > 0 AND m.marks_obtained < (qp.max_marks * 0.5)"
        ));
        return stats;
    }

    private int getScalarCount(String query) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
