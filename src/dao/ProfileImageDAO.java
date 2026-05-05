package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ProfileImageDAO {
    private static final int SCHOOL_IMAGE_ID = 1;

    public boolean ensureInfrastructure() {
        try (Connection conn = DBConnection.getConnection()) {
            return ensureInfrastructure(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public byte[] getUserProfileImage(int userId) {
        if (userId <= 0) {
            return null;
        }
        String query = "SELECT image_data FROM USER_PROFILE_IMAGES WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (!ensureInfrastructure(conn)) {
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getBytes("image_data");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveUserProfileImage(int userId, byte[] imageData, String mimeType) {
        if (userId <= 0 || imageData == null || imageData.length == 0) {
            return false;
        }
        try (Connection conn = DBConnection.getConnection()) {
            if (!ensureInfrastructure(conn)) {
                return false;
            }
            String updateQuery = "UPDATE USER_PROFILE_IMAGES " +
                                 "SET image_data = ?, mime_type = ?, updated_at = SYSDATE " +
                                 "WHERE user_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setBytes(1, imageData);
                updateStmt.setString(2, normalizeMimeType(mimeType));
                updateStmt.setInt(3, userId);
                if (updateStmt.executeUpdate() > 0) {
                    return true;
                }
            }

            String insertQuery = "INSERT INTO USER_PROFILE_IMAGES (user_id, image_data, mime_type, updated_at) " +
                                 "VALUES (?, ?, ?, SYSDATE)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, userId);
                insertStmt.setBytes(2, imageData);
                insertStmt.setString(3, normalizeMimeType(mimeType));
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeUserProfileImage(int userId) {
        if (userId <= 0) {
            return false;
        }
        String query = "DELETE FROM USER_PROFILE_IMAGES WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (!ensureInfrastructure(conn)) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public byte[] getSchoolProfileImage() {
        String query = "SELECT image_data FROM SCHOOL_PROFILE_IMAGES WHERE image_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (!ensureInfrastructure(conn)) {
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, SCHOOL_IMAGE_ID);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getBytes("image_data");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveSchoolProfileImage(int adminUserId, byte[] imageData, String mimeType) {
        if (adminUserId <= 0 || imageData == null || imageData.length == 0) {
            return false;
        }
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId) || !ensureInfrastructure(conn)) {
                return false;
            }
            String updateQuery = "UPDATE SCHOOL_PROFILE_IMAGES " +
                                 "SET image_data = ?, mime_type = ?, updated_at = SYSDATE " +
                                 "WHERE image_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setBytes(1, imageData);
                updateStmt.setString(2, normalizeMimeType(mimeType));
                updateStmt.setInt(3, SCHOOL_IMAGE_ID);
                if (updateStmt.executeUpdate() > 0) {
                    return true;
                }
            }

            String insertQuery = "INSERT INTO SCHOOL_PROFILE_IMAGES (image_id, image_data, mime_type, updated_at) " +
                                 "VALUES (?, ?, ?, SYSDATE)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, SCHOOL_IMAGE_ID);
                insertStmt.setBytes(2, imageData);
                insertStmt.setString(3, normalizeMimeType(mimeType));
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeSchoolProfileImage(int adminUserId) {
        if (adminUserId <= 0) {
            return false;
        }
        String query = "DELETE FROM SCHOOL_PROFILE_IMAGES WHERE image_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            if (!isAdminUser(conn, adminUserId) || !ensureInfrastructure(conn)) {
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, SCHOOL_IMAGE_ID);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    boolean ensureInfrastructure(Connection conn) {
        try {
            if (!tableExists(conn, "USER_PROFILE_IMAGES")) {
                executeDdlIgnoreAlreadyExists(conn,
                    "CREATE TABLE USER_PROFILE_IMAGES (" +
                    "USER_ID NUMBER PRIMARY KEY, " +
                    "IMAGE_DATA BLOB NOT NULL, " +
                    "MIME_TYPE VARCHAR2(80), " +
                    "UPDATED_AT DATE DEFAULT SYSDATE NOT NULL, " +
                    "CONSTRAINT FK_PROFILE_IMAGE_USER FOREIGN KEY (USER_ID) " +
                    "REFERENCES USERS(USER_ID) ON DELETE CASCADE" +
                    ")"
                );
            }

            if (!tableExists(conn, "SCHOOL_PROFILE_IMAGES")) {
                executeDdlIgnoreAlreadyExists(conn,
                    "CREATE TABLE SCHOOL_PROFILE_IMAGES (" +
                    "IMAGE_ID NUMBER PRIMARY KEY, " +
                    "IMAGE_DATA BLOB NOT NULL, " +
                    "MIME_TYPE VARCHAR2(80), " +
                    "UPDATED_AT DATE DEFAULT SYSDATE NOT NULL, " +
                    "CONSTRAINT CHK_SCHOOL_PROFILE_IMAGE_ID CHECK (IMAGE_ID = 1)" +
                    ")"
                );
            }

            return tableExists(conn, "USER_PROFILE_IMAGES") && tableExists(conn, "SCHOOL_PROFILE_IMAGES");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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

    private boolean isAdminUser(Connection conn, int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM USERS u JOIN ROLES r ON u.role_id = r.role_id " +
                       "WHERE u.user_id = ? AND UPPER(r.role_name) = 'ADMIN'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
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

    private String normalizeMimeType(String mimeType) {
        return mimeType == null || mimeType.isBlank() ? "image/*" : mimeType.trim();
    }
}
