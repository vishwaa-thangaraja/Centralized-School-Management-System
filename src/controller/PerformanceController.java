package controller;

import dao.UserDAO;
import model.PerformanceRecord;
import service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;

public class PerformanceController {

    @FXML private TableView<PerformanceRecord> performanceTable;
    @FXML private TableColumn<PerformanceRecord, String> colSubject;
    @FXML private TableColumn<PerformanceRecord, Double> colMarks;
    @FXML private TableColumn<PerformanceRecord, String> colExam;
    @FXML private LineChart<String, Number> performanceChart;

    private UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colMarks.setCellValueFactory(new PropertyValueFactory<>("marksObtained"));
        colExam.setCellValueFactory(new PropertyValueFactory<>("examType"));

        if (AuthService.getCurrentUser() != null) {
            int currentUserId = AuthService.getCurrentUser().getUserId();
            ObservableList<PerformanceRecord> marksData = userDAO.getStudentMarks(currentUserId);
            performanceTable.setItems(marksData);
            loadChartData(marksData);
        }
    }

    @FXML
    private void handleBackToDashboard() {
        // Returns to main dashboard and refreshes the summary stats
        if (StudentDashboardController.getInstance() != null) {
            StudentDashboardController.getInstance().scrollToTop();
        }
    }

    private void loadChartData(ObservableList<PerformanceRecord> data) {
        performanceChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Marks by Subject");

        for (PerformanceRecord record : data) {
            series.getData().add(new XYChart.Data<>(record.getSubject(), record.getMarksObtained()));
        }
        performanceChart.getData().add(series);
    }

    @FXML
    private void handleDownloadPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName("Report_" + AuthService.getCurrentUser().getName() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File file = fileChooser.showSaveDialog(performanceTable.getScene().getWindow());

        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(new FileOutputStream(file));
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                document.add(new Paragraph("CENTRALISED SCHOOL MANAGEMENT SYSTEM").setFontSize(20));
                document.add(new Paragraph("Academic Performance Report").setFontSize(14));
                document.add(new Paragraph("------------------------------------------------------------------"));
                document.add(new Paragraph("Student Name: " + AuthService.getCurrentUser().getName()));
                document.add(new Paragraph("Student ID: " + AuthService.getCurrentUser().getUserId()));
                document.add(new Paragraph("Date: " + new java.util.Date().toString()));
                document.add(new Paragraph("\n")); 

                Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2}));
                table.useAllAvailableWidth();
                table.addHeaderCell("Subject");
                table.addHeaderCell("Exam Type");
                table.addHeaderCell("Marks Obtained");

                for (PerformanceRecord record : performanceTable.getItems()) {
                    table.addCell(record.getSubject());
                    table.addCell(record.getExamType());
                    table.addCell(String.valueOf(record.getMarksObtained()));
                }

                document.add(table);
                document.close();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("PDF Report generated successfully!");
                alert.show();

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("PDF Error: " + e.getMessage());
                alert.show();
            }
        }
    }
}