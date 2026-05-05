package dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.AdminAuditRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class LoginAuditDAO {
    public ObservableList<AdminAuditRecord> getLoginAuditRecords() {
        ObservableList<AdminAuditRecord> records = FXCollections.observableArrayList();
        cleanupStaleLoginAudits();
        ProfileImageDAO profileImageDAO = new ProfileImageDAO();
        profileImageDAO.ensureInfrastructure();
        String query = "SELECT * FROM (" +
                       "SELECT la.log_id, la.user_id, NVL(u.name, 'Unknown') AS user_name, NVL(r.role_name, '-') AS role_name, " +
                       "TO_CHAR(la.login_time, 'YYYY-MM-DD HH24:MI:SS') AS login_time, " +
                       "NVL(TO_CHAR(la.logout_time, 'YYYY-MM-DD HH24:MI:SS'), 'Active') AS logout_time, " +
                       "NVL(la.ip_address, '-') AS ip_address, " +
                       "upi.image_data AS user_profile_image, spi.image_data AS school_profile_image " +
                       "FROM LOGIN_AUDIT la " +
                       "LEFT JOIN USERS u ON la.user_id = u.user_id " +
                       "LEFT JOIN ROLES r ON u.role_id = r.role_id " +
                       "LEFT JOIN USER_PROFILE_IMAGES upi ON la.user_id = upi.user_id " +
                       "LEFT JOIN SCHOOL_PROFILE_IMAGES spi ON spi.image_id = 1 " +
                       "ORDER BY la.login_time DESC, la.log_id DESC) " +
                       "WHERE ROWNUM <= 200";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String roleName = rs.getString("role_name");
                byte[] profileImageData = roleName != null && roleName.equalsIgnoreCase("Admin")
                    ? rs.getBytes("school_profile_image")
                    : rs.getBytes("user_profile_image");
                records.add(new AdminAuditRecord(
                    rs.getInt("log_id"),
                    rs.getString("user_name"),
                    roleName,
                    rs.getString("login_time"),
                    rs.getString("logout_time"),
                    rs.getString("ip_address"),
                    profileImageData
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public void logSuccessfulLogin(int userId, String ipAddress) {
        String query = "INSERT INTO LOGIN_AUDIT (log_id, user_id, login_time, ip_address) VALUES (?, ?, SYSDATE, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            SequenceService.ensureSequence(conn, "SEQ_LOGIN_AUDIT", "LOGIN_AUDIT", "LOG_ID");
            closeStaleLoginAudits(conn);
            closeOpenLoginAuditsForUser(conn, userId);
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, SequenceService.nextVal(conn, "SEQ_LOGIN_AUDIT", "LOGIN_AUDIT", "LOG_ID"));
                pstmt.setInt(2, userId);
                pstmt.setString(3, ipAddress);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logLogout(int userId) {
        String query = "UPDATE LOGIN_AUDIT " +
                       "SET logout_time = SYSDATE " +
                       "WHERE user_id = ? AND logout_time IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void cleanupStaleLoginAudits() {
        try (Connection conn = DBConnection.getConnection()) {
            closeStaleLoginAudits(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int deleteLoginAudits(Collection<Integer> logIds) {
        if (logIds == null || logIds.isEmpty()) {
            return 0;
        }
        String query = "DELETE FROM LOGIN_AUDIT WHERE log_id = ?";
        int deletedCount = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (Integer logId : logIds) {
                if (logId == null) {
                    continue;
                }
                pstmt.setInt(1, logId);
                deletedCount += pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deletedCount;
    }

    public int clearLoginAudits() {
        String query = "DELETE FROM LOGIN_AUDIT";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void closeStaleLoginAudits(Connection conn) throws SQLException {
        String query = "UPDATE LOGIN_AUDIT " +
                       "SET logout_time = login_time + (12 / 24) " +
                       "WHERE logout_time IS NULL AND login_time < SYSDATE - (12 / 24)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.executeUpdate();
        }
    }

    private void closeOpenLoginAuditsForUser(Connection conn, int userId) throws SQLException {
        String query = "UPDATE LOGIN_AUDIT SET logout_time = SYSDATE " +
                       "WHERE user_id = ? AND logout_time IS NULL";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }
}
