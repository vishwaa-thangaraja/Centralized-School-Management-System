package dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.AdminAuditRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginAuditDAO {
    public ObservableList<AdminAuditRecord> getLoginAuditRecords() {
        ObservableList<AdminAuditRecord> records = FXCollections.observableArrayList();
        cleanupStaleLoginAudits();
        String query = "SELECT * FROM (" +
                       "SELECT la.log_id, NVL(u.name, 'Unknown') AS user_name, NVL(r.role_name, '-') AS role_name, " +
                       "TO_CHAR(la.login_time, 'YYYY-MM-DD HH24:MI:SS') AS login_time, " +
                       "NVL(TO_CHAR(la.logout_time, 'YYYY-MM-DD HH24:MI:SS'), 'Active') AS logout_time, " +
                       "NVL(la.ip_address, '-') AS ip_address " +
                       "FROM LOGIN_AUDIT la " +
                       "LEFT JOIN USERS u ON la.user_id = u.user_id " +
                       "LEFT JOIN ROLES r ON u.role_id = r.role_id " +
                       "ORDER BY la.login_time DESC, la.log_id DESC) " +
                       "WHERE ROWNUM <= 200";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new AdminAuditRecord(
                    rs.getInt("log_id"),
                    rs.getString("user_name"),
                    rs.getString("role_name"),
                    rs.getString("login_time"),
                    rs.getString("logout_time"),
                    rs.getString("ip_address")
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
