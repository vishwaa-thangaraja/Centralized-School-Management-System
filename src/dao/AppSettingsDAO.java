package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppSettingsDAO {
    public Map<String, String> getSettings() {
        Map<String, String> settings = defaultSettings();
        try (Connection conn = DBConnection.getConnection()) {
            if (!ensureInfrastructure(conn)) {
                return settings;
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT setting_key, setting_value FROM APP_SETTINGS")) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settings;
    }

    public boolean saveSettings(int adminUserId, String schoolName, String address, String phone, String email) {
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId) || !ensureInfrastructure(conn)) {
                return false;
            }
            upsertSetting(conn, "SCHOOL_NAME", schoolName);
            upsertSetting(conn, "SCHOOL_ADDRESS", address);
            upsertSetting(conn, "SCHOOL_PHONE", phone);
            upsertSetting(conn, "SCHOOL_EMAIL", email);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean saveTheme(String themeName) {
        String normalizedTheme = "Dark".equalsIgnoreCase(themeName) ? "Dark" : "Light";
        try (Connection conn = DBConnection.getConnection()) {
            if (!ensureInfrastructure(conn)) {
                return false;
            }
            upsertSetting(conn, "THEME", normalizedTheme);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean ensureInfrastructure(Connection conn) {
        try {
            if (!tableExists(conn, "APP_SETTINGS")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE APP_SETTINGS (" +
                        "SETTING_KEY VARCHAR2(50) PRIMARY KEY, " +
                        "SETTING_VALUE VARCHAR2(500)" +
                        ")"
                    );
                }
            }
            for (Map.Entry<String, String> entry : defaultSettings().entrySet()) {
                insertDefaultSetting(conn, entry.getKey(), entry.getValue());
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, String> defaultSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("SCHOOL_NAME", "CSMS");
        settings.put("SCHOOL_ADDRESS", "");
        settings.put("SCHOOL_PHONE", "");
        settings.put("SCHOOL_EMAIL", "");
        settings.put("THEME", "Light");
        return settings;
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, tableName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private boolean isAdminUser(Connection conn, int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                       "WHERE u.user_id = ? AND UPPER(r.role_name) = 'ADMIN'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void insertDefaultSetting(Connection conn, String key, String value) throws SQLException {
        String query = "INSERT INTO APP_SETTINGS (setting_key, setting_value) " +
                       "SELECT ?, ? FROM dual WHERE NOT EXISTS (" +
                       "SELECT 1 FROM APP_SETTINGS WHERE setting_key = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value == null ? "" : value);
            pstmt.setString(3, key);
            pstmt.executeUpdate();
        }
    }

    private void upsertSetting(Connection conn, String key, String value) throws SQLException {
        String query = "MERGE INTO APP_SETTINGS target " +
                       "USING (SELECT ? AS setting_key, ? AS setting_value FROM dual) src " +
                       "ON (target.setting_key = src.setting_key) " +
                       "WHEN MATCHED THEN UPDATE SET target.setting_value = src.setting_value " +
                       "WHEN NOT MATCHED THEN INSERT (setting_key, setting_value) VALUES (src.setting_key, src.setting_value)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value == null ? "" : value);
            pstmt.executeUpdate();
        }
    }
}
