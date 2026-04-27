package service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AssignmentFileService {
    private final Path assignmentDir = Path.of("folders", "assignments");
    private final Path submissionDir = Path.of("folders", "submissions");

    public AssignmentFileService() throws IOException {
        Files.createDirectories(assignmentDir);
        Files.createDirectories(submissionDir);
    }

    public File getAssignmentPdf(int assignmentId) {
        return assignmentDir.resolve("assignment_" + assignmentId + ".pdf").toFile();
    }

    public File getSubmissionPdf(int assignmentId, int studentId) {
        return submissionDir.resolve("submission_" + assignmentId + "_" + studentId + ".pdf").toFile();
    }

    public void saveAssignmentPdf(File sourceFile, int assignmentId) throws IOException {
        Files.copy(sourceFile.toPath(), getAssignmentPdf(assignmentId).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void saveSubmissionPdf(File sourceFile, int assignmentId, int studentId) throws IOException {
        Files.copy(sourceFile.toPath(), getSubmissionPdf(assignmentId, studentId).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean hasAssignmentPdf(int assignmentId) {
        return getAssignmentPdf(assignmentId).exists();
    }

    public boolean hasSubmissionPdf(int assignmentId, int studentId) {
        return getSubmissionPdf(assignmentId, studentId).exists();
    }

    public void openPdf(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("PDF file not found.");
        }
        Desktop.getDesktop().open(file);
    }
}
