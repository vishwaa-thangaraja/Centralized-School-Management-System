package service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class QuestionBankFileService {
    private final Path questionBankDir = Path.of("folders", "question_bank");

    public QuestionBankFileService() throws IOException {
        Files.createDirectories(questionBankDir);
    }

    public File getQuestionPdf(int questionId) {
        return questionBankDir.resolve("question_" + questionId + ".pdf").toFile();
    }

    public void saveQuestionPdf(File sourceFile, int questionId) throws IOException {
        Files.copy(sourceFile.toPath(), getQuestionPdf(questionId).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean hasQuestionPdf(int questionId) {
        return getQuestionPdf(questionId).exists();
    }

    public void openPdf(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("PDF file not found.");
        }
        Desktop.getDesktop().open(file);
    }

    public void copyQuestionPdf(int questionId, File targetFile) throws IOException {
        File source = getQuestionPdf(questionId);
        if (!source.exists()) {
            throw new IOException("PDF file not found.");
        }
        Files.copy(source.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
