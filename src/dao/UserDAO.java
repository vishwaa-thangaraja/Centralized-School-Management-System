package dao;

import model.User;
import java.sql.*;

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

    public boolean updatePassword(String email, String newPasswordHash) {
        String query = "UPDATE USERS SET password_hash = ? WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, email);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}