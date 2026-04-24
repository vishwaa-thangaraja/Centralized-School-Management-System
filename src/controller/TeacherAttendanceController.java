package controller;

import dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.AttendanceRecord;
import model.User;

import java.util.List;
import java.util.Map;

public class TeacherAttendanceController {

    @FXML private Label nameLbl;
    @FXML private Label idLbl;
    @FXML private Label qualificationLbl;
    @FXML private Label totalDaysLbl;
    @FXML private Label presentDaysLbl;
    @FXML private Label percentageLbl;
    @FXML private BarChart<String, Number> leaveChart;
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> dateCol;
    @FXML private TableColumn<AttendanceRecord, String> fnCol;
    @FXML private TableColumn<AttendanceRecord, String> anCol;
    @FXML private TableColumn<AttendanceRecord, String> remarkCol;

    private User currentUser;

    public void initData(User user) {
        this.currentUser = user;
        nameLbl.setText(user.getName());

        UserDAO dao = new UserDAO();
        Map<String, String> profile = dao.getTeacherProfile(user.getUserId());
        idLbl.setText("Teacher ID: " + profile.getOrDefault("teacher_id", String.valueOf(user.getUserId())));
        qualificationLbl.setText(profile.getOrDefault("qualification", "Not Provided") + " | Experience: "
            + profile.getOrDefault("experience", "0") + " years");

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

        int totalSessions = records.size() * 2;
        int sessionsPresent = 0;
        int leaveSessions = 0;
        int absentSessions = 0;

        for (AttendanceRecord record : records) {
            String[] daySessions = {record.getFnStatus(), record.getAnStatus()};
            for (String status : daySessions) {
                if (status == null || status.equals("-") || status.equals("Not Marked")) {
                    totalSessions--;
                    continue;
                }

                if (status.equalsIgnoreCase("Present")) {
                    sessionsPresent++;
                } else if (status.equalsIgnoreCase("Leave")) {
                    leaveSessions++;
                } else if (status.equalsIgnoreCase("Absent")) {
                    absentSessions++;
                }
            }
        }

        totalDaysLbl.setText(String.valueOf(Math.max(totalSessions, 0)));
        presentDaysLbl.setText(String.valueOf(sessionsPresent));
        double percent = totalSessions > 0 ? (sessionsPresent * 100.0 / totalSessions) : 0;
        percentageLbl.setText(String.format("%.2f%%", percent));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendance Sessions");
        series.getData().add(new XYChart.Data<>("Leave", leaveSessions));
        series.getData().add(new XYChart.Data<>("Absent", absentSessions));

        leaveChart.getData().clear();
        leaveChart.getData().add(series);
    }

    @FXML
    private void backToDashboard(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/teacher_dashboard.fxml"));
        Parent root = loader.load();
        TeacherDashboardController controller = loader.getController();
        controller.initData(currentUser);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }
}
