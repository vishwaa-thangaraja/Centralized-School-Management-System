package controller;

import dao.UserDAO;
import model.User;
import model.AttendanceRecord;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.util.List;
import java.util.Map;

public class AttendanceController {

    @FXML private Label nameLbl, idLbl, stdLbl, totalDaysLbl, presentDaysLbl, percentageLbl;
    @FXML private BarChart<String, Number> leaveChart;
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> dateCol, fnCol, anCol, remarkCol;

    private User currentUser;

    public void initData(User user) {
        this.currentUser = user;
        nameLbl.setText(user.getName());
        
        UserDAO dao = new UserDAO();
        Map<String, String> profile = dao.getStudentProfile(user.getUserId());
        
        if (!profile.isEmpty()) {
            idLbl.setText("ID: " + profile.get("student_id"));
            stdLbl.setText(profile.get("standard") + "th Standard - Section " + profile.get("section"));
        } else {
            idLbl.setText("ID: " + user.getUserId());
            stdLbl.setText("Standard: 10th (Default)");
        }
        
        setupTable();
        loadAttendanceData();
    }

    private void setupTable() {
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        fnCol.setCellValueFactory(new PropertyValueFactory<>("fnStatus"));
        anCol.setCellValueFactory(new PropertyValueFactory<>("anStatus"));
        remarkCol.setCellValueFactory(new PropertyValueFactory<>("remark"));
    }

    private void loadAttendanceData() {
    UserDAO dao = new UserDAO();
    List<AttendanceRecord> records = dao.getSchoolAttendance(currentUser.getUserId());
    attendanceTable.getItems().setAll(records);

    // We calculate based on SESSIONS (2 per day)
    int totalSessions = records.size() * 2; 
    int sessionsPresent = 0;
    int leaveSessions = 0, odSessions = 0, absentSessions = 0;

    for (AttendanceRecord r : records) {
        // Check FN and AN separately
        String[] daySessions = {r.getFnStatus(), r.getAnStatus()};
        
        for (String status : daySessions) {
            if (status == null || status.equals("-") || status.equals("Not Marked")) {
                // If a session isn't marked, we shouldn't count it in the total for percentage
                totalSessions--; 
                continue;
            }

            if (status.equalsIgnoreCase("Present")) sessionsPresent++;
            else if (status.equalsIgnoreCase("Leave")) leaveSessions++;
            else if (status.equalsIgnoreCase("OD")) odSessions++;
            else if (status.equalsIgnoreCase("Absent")) absentSessions++;
        }
    }

    // Update Summary Labels
    totalDaysLbl.setText(String.valueOf(totalSessions)); // Label now shows Total Sessions
    presentDaysLbl.setText(String.valueOf(sessionsPresent)); // Label now shows Sessions Present

    // Accurate Percentage: (Present Sessions / Total Marked Sessions) * 100
    double percent = totalSessions > 0 ? (sessionsPresent * 100.0 / totalSessions) : 0;
    percentageLbl.setText(String.format("%.2f%%", percent));

    // Update BarChart
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.setName("Attendance Sessions");
    series.getData().add(new XYChart.Data<>("Leave", leaveSessions));
    series.getData().add(new XYChart.Data<>("OD", odSessions));
    series.getData().add(new XYChart.Data<>("Absent", absentSessions));
    
    leaveChart.getData().clear();
    leaveChart.getData().add(series);
}

    @FXML
    private void backToDashboard(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/student_dashboard.fxml"));
        Parent root = loader.load();
        StudentDashboardController controller = loader.getController();
        controller.initData(currentUser);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }
}