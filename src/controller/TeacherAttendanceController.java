package controller;

import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import model.AttendanceRecord;
import model.Student;
import model.User;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TeacherAttendanceController {

    @FXML private ComboBox<String> studentSelector;
    @FXML private Label selectedStudentLabel;
    @FXML private Label nameLbl;
    @FXML private Label idLbl;
    @FXML private Label qualificationLbl;
    @FXML private Label totalDaysLbl;
    @FXML private Label presentDaysLbl;
    @FXML private Label percentageLbl;
    @FXML private Label editStatusLabel;
    @FXML private BarChart<String, Number> attendanceChart;
    @FXML private DatePicker attendanceDatePicker;
    @FXML private ComboBox<String> addFnCombo;
    @FXML private ComboBox<String> addAnCombo;
    @FXML private TextField addRemarkField;
    @FXML private Button queueAttendanceButton;
    @FXML private Button editModeToggleButton;
    @FXML private Button saveChangesButton;
    @FXML private Button cancelButton;
    @FXML private TableView<AttendanceRecord> attendanceTable;
    @FXML private TableColumn<AttendanceRecord, String> dateCol;
    @FXML private TableColumn<AttendanceRecord, String> fnCol;
    @FXML private TableColumn<AttendanceRecord, String> anCol;
    @FXML private TableColumn<AttendanceRecord, String> remarkCol;

    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<AttendanceRecord> attendanceRows = FXCollections.observableArrayList();
    private final Map<String, Student> studentDisplayMap = new LinkedHashMap<>();
    private final Map<String, AttendanceSnapshot> originalRowsByDate = new HashMap<>();
    private final Map<String, AttendanceSnapshot> pendingChangesByDate = new HashMap<>();

    private User currentUser;
    private Student selectedStudent;
    private boolean editModeEnabled = false;
    private boolean selectedStudentInScope = false;
    private int teacherId = -1;

    public void initData(User user) {
        this.currentUser = user;
        nameLbl.setText(user.getName());

        Map<String, String> profile = userDAO.getTeacherProfile(user.getUserId());
        idLbl.setText("Teacher ID: " + profile.getOrDefault("teacher_id", String.valueOf(user.getUserId())));
        qualificationLbl.setText(profile.getOrDefault("qualification", "Not Provided") + " | Experience: "
            + profile.getOrDefault("experience", "0") + " years");
        teacherId = userDAO.getTeacherIdByUserId(currentUser.getUserId());

        setupTable();
        configureAttendanceEntryControls();
        loadStudentsForScope();
    }

    private void setupTable() {
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        fnCol.setCellValueFactory(new PropertyValueFactory<>("fnStatus"));
        anCol.setCellValueFactory(new PropertyValueFactory<>("anStatus"));
        remarkCol.setCellValueFactory(new PropertyValueFactory<>("remark"));

        fnCol.setCellFactory(column -> createAttendanceStatusCell());
        anCol.setCellFactory(column -> createAttendanceStatusCell());
        remarkCol.setCellFactory(TextFieldTableCell.forTableColumn());

        fnCol.setOnEditCommit(event -> {
            AttendanceRecord row = event.getRowValue();
            if (!canEditRow(row)) {
                attendanceTable.refresh();
                editStatusLabel.setText("This row cannot be edited.");
                return;
            }
            row.setFnStatus(event.getNewValue());
            markRowAsChanged(row);
        });

        anCol.setOnEditCommit(event -> {
            AttendanceRecord row = event.getRowValue();
            if (!canEditRow(row)) {
                attendanceTable.refresh();
                editStatusLabel.setText("This row cannot be edited.");
                return;
            }
            row.setAnStatus(event.getNewValue());
            markRowAsChanged(row);
        });

        remarkCol.setOnEditCommit(event -> {
            AttendanceRecord row = event.getRowValue();
            if (!canEditRow(row)) {
                attendanceTable.refresh();
                editStatusLabel.setText("This row cannot be edited.");
                return;
            }
            row.setRemark(displayRemark(event.getNewValue()));
            markRowAsChanged(row);
        });

        attendanceTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(AttendanceRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    return;
                }
                if (pendingChangesByDate.containsKey(item.getDate())) {
                    setStyle("-fx-background-color: #fff3cd;");
                } else {
                    setStyle("");
                }
            }
        });

        attendanceTable.setEditable(false);
        attendanceTable.setItems(attendanceRows);
    }

    private void configureAttendanceEntryControls() {
        addFnCombo.getItems().setAll("Present", "Absent");
        addAnCombo.getItems().setAll("Present", "Absent");
        resetAttendanceEntryForm();
        attendanceChart.setTitle("Attendance Sessions");
        updateEntryControls();
    }

    private ComboBoxTableCell<AttendanceRecord, String> createAttendanceStatusCell() {
        return new ComboBoxTableCell<>("Present", "Absent") {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                applyAttendanceStatusStyle(this, item, empty);
            }
        };
    }

    private void applyAttendanceStatusStyle(TableCell<?, String> cell, String value, boolean empty) {
        if (empty || value == null) {
            cell.setStyle("-fx-alignment: CENTER;");
            return;
        }

        String textColor = "#2c3e50";
        if ("Present".equalsIgnoreCase(value)) {
            textColor = "#1f9d55";
        } else if ("Absent".equalsIgnoreCase(value)) {
            textColor = "#dc3545";
        }

        cell.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
    }

    private void loadStudentsForScope() {
        studentDisplayMap.clear();
        studentSelector.getItems().clear();

        ObservableList<Student> students = userDAO.getStudentsForTeacher(currentUser.getUserId());
        for (Student student : students) {
            String display = student.getName() + " (ID: " + student.getStudentId() + ") - " + student.getClassDisplay();
            studentDisplayMap.put(display, student);
            studentSelector.getItems().add(display);
        }

        studentSelector.valueProperty().addListener((obs, oldValue, newValue) -> handleStudentSelection(newValue));

        if (studentSelector.getItems().isEmpty()) {
            selectedStudentLabel.setText("No mapped students");
            editStatusLabel.setText("No students available in teacher scope.");
            setEditMode(false);
            updateEditActionButtons();
            updateEntryControls();
            clearAttendanceState();
            return;
        }

        studentSelector.setValue(studentSelector.getItems().get(0));
    }

    private void handleStudentSelection(String studentDisplay) {
        selectedStudent = studentDisplayMap.get(studentDisplay);
        if (selectedStudent == null) {
            clearAttendanceState();
            return;
        }

        selectedStudentLabel.setText(selectedStudent.getName() + " | " + selectedStudent.getClassDisplay());
        // Hard scope gate: teachers can edit only students mapped through their classes.
        selectedStudentInScope = userDAO.validateTeacherScopeForStudent(teacherId, selectedStudent.getStudentId());
        setEditMode(false);
        updateEditActionButtons();
        updateEntryControls();
        resetAttendanceEntryForm();

        if (!selectedStudentInScope) {
            clearAttendanceState();
            editStatusLabel.setText("Editing disabled: selected student is outside your scope.");
            return;
        }

        loadAttendanceDataForSelectedStudent();
        editStatusLabel.setText("Read-only mode. Enable edit mode to update table rows, or add a new day below.");
    }

    private void clearAttendanceState() {
        attendanceRows.clear();
        originalRowsByDate.clear();
        pendingChangesByDate.clear();
        recalculateSummaryAndChart();
    }

    private void loadAttendanceDataForSelectedStudent() {
        clearAttendanceState();
        if (selectedStudent == null) {
            return;
        }

        int studentUserId = userDAO.getStudentUserIdByStudentId(selectedStudent.getStudentId());
        if (studentUserId <= 0) {
            editStatusLabel.setText("Unable to resolve selected student attendance mapping.");
            return;
        }

        List<AttendanceRecord> records = userDAO.getSchoolAttendance(studentUserId);
        for (AttendanceRecord record : records) {
            record.setEditable(!isFutureDate(record.getDate()));
            attendanceRows.add(record);
            originalRowsByDate.put(record.getDate(), new AttendanceSnapshot(
                record.getFnStatus(), record.getAnStatus(), normalizeRemark(record.getRemark())
            ));
        }

        sortAttendanceRows();
        recalculateSummaryAndChart();
        attendanceTable.refresh();
    }

    private void recalculateSummaryAndChart() {
        int totalSessions = attendanceRows.size() * 2;
        int sessionsPresent = 0;
        int absentSessions = 0;

        for (AttendanceRecord record : attendanceRows) {
            String[] daySessions = {record.getFnStatus(), record.getAnStatus()};
            for (String status : daySessions) {
                if (status == null || status.equals("-") || status.equalsIgnoreCase("Not Marked")) {
                    totalSessions--;
                    continue;
                }

                if (status.equalsIgnoreCase("Present")) {
                    sessionsPresent++;
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
        series.getData().add(new XYChart.Data<>("Present", sessionsPresent));
        series.getData().add(new XYChart.Data<>("Absent", absentSessions));

        attendanceChart.getData().clear();
        attendanceChart.getData().add(series);
    }

    private boolean canEditRow(AttendanceRecord row) {
        return editModeEnabled
            && selectedStudentInScope
            && row != null
            && row.isEditable()
            && !isFutureDate(row.getDate());
    }

    private void markRowAsChanged(AttendanceRecord row) {
        String date = row.getDate();
        AttendanceSnapshot original = originalRowsByDate.get(date);
        AttendanceSnapshot current = new AttendanceSnapshot(
            row.getFnStatus(),
            row.getAnStatus(),
            normalizeRemark(row.getRemark())
        );

        if (original != null && original.matches(current)) {
            pendingChangesByDate.remove(date);
        } else {
            pendingChangesByDate.put(date, current);
        }

        recalculateSummaryAndChart();
        attendanceTable.refresh();
        updateEditActionButtons();
        if (pendingChangesByDate.isEmpty()) {
            editStatusLabel.setText("No pending changes.");
        } else {
            editStatusLabel.setText(pendingChangesByDate.size() + " edited day(s) pending save.");
        }
    }

    private String normalizeRemark(String remark) {
        if (remark == null) {
            return "";
        }
        String trimmed = remark.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("Regular")) {
            return "";
        }
        return trimmed;
    }

    private String displayRemark(String remark) {
        String normalized = normalizeRemark(remark);
        return normalized.isEmpty() ? "Regular" : normalized;
    }

    private boolean isFutureDate(String dateText) {
        try {
            return LocalDate.parse(dateText).isAfter(LocalDate.now());
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private void setEditMode(boolean enabled) {
        editModeEnabled = enabled && selectedStudentInScope;
        attendanceTable.setEditable(editModeEnabled);
        editModeToggleButton.setText(editModeEnabled ? "Disable Edit Mode" : "Enable Edit Mode");
        attendanceTable.refresh();
    }

    private void updateEditActionButtons() {
        boolean hasStudent = selectedStudent != null;
        boolean hasPending = !pendingChangesByDate.isEmpty();
        boolean canEdit = hasStudent && selectedStudentInScope;

        editModeToggleButton.setDisable(!canEdit);
        saveChangesButton.setDisable(!(canEdit && hasPending));
        cancelButton.setDisable(!(canEdit && hasPending));
    }

    private void updateEntryControls() {
        boolean canEdit = selectedStudent != null && selectedStudentInScope;
        attendanceDatePicker.setDisable(!canEdit);
        addFnCombo.setDisable(!canEdit);
        addAnCombo.setDisable(!canEdit);
        addRemarkField.setDisable(!canEdit);
        queueAttendanceButton.setDisable(!canEdit);
    }

    private void resetAttendanceEntryForm() {
        attendanceDatePicker.setValue(LocalDate.now());
        addFnCombo.getSelectionModel().select("Present");
        addAnCombo.getSelectionModel().select("Present");
        addRemarkField.clear();
    }

    private void sortAttendanceRows() {
        FXCollections.sort(attendanceRows, Comparator.comparing(AttendanceRecord::getDate).reversed());
    }

    private AttendanceRecord findAttendanceRecord(String dateText) {
        for (AttendanceRecord record : attendanceRows) {
            if (record.getDate().equals(dateText)) {
                return record;
            }
        }
        return null;
    }

    @FXML
    private void toggleEditMode() {
        if (selectedStudent == null) {
            editStatusLabel.setText("Select a student first.");
            return;
        }
        if (!selectedStudentInScope) {
            editStatusLabel.setText("Editing disabled for this student due to scope restrictions.");
            return;
        }
        setEditMode(!editModeEnabled);
        updateEditActionButtons();
        if (editModeEnabled) {
            editStatusLabel.setText("Edit mode enabled. FN/AN and remarks are now editable.");
        } else if (!pendingChangesByDate.isEmpty()) {
            editStatusLabel.setText("Edit mode disabled. Pending attendance changes are still queued for save.");
        } else {
            editStatusLabel.setText("Edit mode disabled.");
        }
    }

    @FXML
    private void handleQueueAttendanceDay() {
        if (selectedStudent == null) {
            editStatusLabel.setText("Select a student first.");
            return;
        }
        if (!selectedStudentInScope) {
            editStatusLabel.setText("This student is outside your teaching scope.");
            return;
        }

        LocalDate selectedDate = attendanceDatePicker.getValue();
        if (selectedDate == null) {
            editStatusLabel.setText("Choose a date for the attendance entry.");
            return;
        }
        if (selectedDate.isAfter(LocalDate.now())) {
            editStatusLabel.setText("Future dates cannot be added.");
            return;
        }

        String fnStatus = addFnCombo.getValue();
        String anStatus = addAnCombo.getValue();
        if (fnStatus == null || anStatus == null) {
            editStatusLabel.setText("Choose both FN and AN statuses.");
            return;
        }

        String dateText = selectedDate.toString();
        AttendanceRecord record = findAttendanceRecord(dateText);
        if (record == null) {
            record = new AttendanceRecord(dateText, fnStatus, anStatus, displayRemark(addRemarkField.getText()));
            record.setEditable(true);
            attendanceRows.add(record);
        } else {
            record.setFnStatus(fnStatus);
            record.setAnStatus(anStatus);
            record.setRemark(displayRemark(addRemarkField.getText()));
        }

        record.setEditable(true);
        markRowAsChanged(record);
        sortAttendanceRows();
        attendanceTable.refresh();
        updateEditActionButtons();
        resetAttendanceEntryForm();
        if (pendingChangesByDate.containsKey(dateText)) {
            editStatusLabel.setText("Queued attendance for " + dateText + ". Click Save Changes to persist it.");
        } else {
            editStatusLabel.setText("No new attendance changes detected for " + dateText + ".");
        }
    }

    @FXML
    private void saveChanges() {
        if (pendingChangesByDate.isEmpty() || selectedStudent == null) {
            editStatusLabel.setText("No attendance changes to save.");
            return;
        }

        // Re-validate scope at commit time to avoid stale/forged UI state updates.
        if (!userDAO.validateTeacherScopeForStudent(teacherId, selectedStudent.getStudentId())) {
            editStatusLabel.setText("Save blocked: selected student is outside your teaching scope.");
            setEditMode(false);
            updateEditActionButtons();
            return;
        }

        for (Map.Entry<String, AttendanceSnapshot> entry : pendingChangesByDate.entrySet()) {
            String date = entry.getKey();
            AttendanceSnapshot change = entry.getValue();

            if (isFutureDate(date)) {
                editStatusLabel.setText("Future dates cannot be edited.");
                return;
            }

            boolean updated = userDAO.updateAttendanceRecord(
                selectedStudent.getStudentId(),
                date,
                change.fnStatus,
                change.anStatus,
                change.remark
            );
            if (!updated) {
                editStatusLabel.setText("Save failed for date " + date + ". Validate data and retry.");
                return;
            }

            // Placeholder hook for future admin audit integration.
            userDAO.logAttendanceAlterationPlaceholder(currentUser.getUserId(), selectedStudent.getStudentId(), date);
        }

        loadAttendanceDataForSelectedStudent();
        setEditMode(false);
        updateEditActionButtons();
        resetAttendanceEntryForm();
        editStatusLabel.setText("Attendance changes saved successfully.");
    }

    @FXML
    private void cancelChanges() {
        if (selectedStudent == null) {
            editStatusLabel.setText("Select a student first.");
            return;
        }
        loadAttendanceDataForSelectedStudent();
        setEditMode(false);
        updateEditActionButtons();
        editStatusLabel.setText("Pending changes discarded.");
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

    private static class AttendanceSnapshot {
        private final String fnStatus;
        private final String anStatus;
        private final String remark;

        private AttendanceSnapshot(String fnStatus, String anStatus, String remark) {
            this.fnStatus = fnStatus == null ? "" : fnStatus;
            this.anStatus = anStatus == null ? "" : anStatus;
            this.remark = remark == null ? "" : remark;
        }

        private boolean matches(AttendanceSnapshot other) {
            return this.fnStatus.equals(other.fnStatus)
                && this.anStatus.equals(other.anStatus)
                && this.remark.equals(other.remark);
        }
    }
}
