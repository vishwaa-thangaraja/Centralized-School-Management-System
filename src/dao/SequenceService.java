package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SequenceService {
    private SequenceService() {
    }

    public static int nextVal(Connection conn, String sequenceName, String tableName, String idColumn) throws SQLException {
        validateIdentifier(sequenceName);
        validateIdentifier(tableName);
        validateIdentifier(idColumn);
        ensureSequence(conn, sequenceName, tableName, idColumn);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT " + sequenceName + ".NEXTVAL FROM dual")) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    public static void ensureSequence(Connection conn, String sequenceName, String tableName, String idColumn) throws SQLException {
        validateIdentifier(sequenceName);
        validateIdentifier(tableName);
        validateIdentifier(idColumn);
        if (sequenceExists(conn, sequenceName)) {
            return;
        }

        int startWith = getCurrentMaxId(conn, tableName, idColumn) + 1;
        String ddl = "CREATE SEQUENCE " + sequenceName + " START WITH " + Math.max(startWith, 1) + " INCREMENT BY 1 NOCACHE";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(ddl);
        } catch (SQLException e) {
            if (e.getErrorCode() != 955) {
                throw e;
            }
        }
    }

    public static void ensureSequences(Connection conn, String[][] definitions) throws SQLException {
        for (String[] definition : definitions) {
            if (definition.length != 3) {
                throw new IllegalArgumentException("Sequence definition must contain sequence, table, and id column.");
            }
            ensureSequence(conn, definition[0], definition[1], definition[2]);
        }
    }

    private static boolean sequenceExists(Connection conn, String sequenceName) throws SQLException {
        String query = "SELECT COUNT(*) FROM USER_SEQUENCES WHERE SEQUENCE_NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sequenceName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static int getCurrentMaxId(Connection conn, String tableName, String idColumn) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT NVL(MAX(" + idColumn + "), 0) FROM " + tableName)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private static void validateIdentifier(String value) {
        if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid database identifier: " + value);
        }
    }
}
