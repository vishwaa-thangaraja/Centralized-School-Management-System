package service;

import dao.AppSettingsDAO;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SchoolSettingsService {
    private static final String DEFAULT_SCHOOL_NAME = "CSMS";
    private static Map<String, String> cachedSettings;

    private SchoolSettingsService() {
    }

    public static synchronized void refresh() {
        cachedSettings = new AppSettingsDAO().getSettings();
    }

    public static synchronized Map<String, String> getSettings() {
        if (cachedSettings == null) {
            refresh();
        }
        return new LinkedHashMap<>(cachedSettings);
    }

    public static String getSchoolName() {
        String value = getSettings().getOrDefault("SCHOOL_NAME", DEFAULT_SCHOOL_NAME);
        return value == null || value.isBlank() ? DEFAULT_SCHOOL_NAME : value.trim();
    }

    public static String getSchoolAddress() {
        return normalize(getSettings().get("SCHOOL_ADDRESS"));
    }

    public static String getSchoolPhone() {
        return normalize(getSettings().get("SCHOOL_PHONE"));
    }

    public static String getSchoolEmail() {
        return normalize(getSettings().get("SCHOOL_EMAIL"));
    }

    public static String getPortalTitle(String roleName) {
        String role = roleName == null || roleName.isBlank() ? "" : " " + roleName.trim().toUpperCase() + " PORTAL";
        return getSchoolName() + role;
    }

    public static String getLoginTitle() {
        return getSchoolName() + " Login";
    }

    public static String getWindowTitle() {
        return getSchoolName() + " - School Management System";
    }

    public static void applyStageTitle(Stage stage) {
        if (stage != null) {
            stage.setTitle(getWindowTitle());
        }
    }

    public static List<String> getContactLines() {
        List<String> lines = new ArrayList<>();
        String address = getSchoolAddress();
        String phone = getSchoolPhone();
        String email = getSchoolEmail();
        if (!address.isEmpty()) {
            lines.add(address);
        }
        if (!phone.isEmpty()) {
            lines.add("Phone: " + phone);
        }
        if (!email.isEmpty()) {
            lines.add("Email: " + email);
        }
        return lines;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
