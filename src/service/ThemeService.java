package service;

import dao.AppSettingsDAO;
import javafx.scene.Node;
import javafx.scene.control.Button;

public final class ThemeService {
    public static final String LIGHT = "Light";
    public static final String DARK = "Dark";
    private static final String LIGHT_CLASS = "light-theme";
    private static final String DARK_CLASS = "dark-theme";

    private ThemeService() {
    }

    public static String currentTheme() {
        return normalizeTheme(SchoolSettingsService.getThemeName());
    }

    public static boolean isDarkTheme() {
        return DARK.equals(currentTheme());
    }

    public static String toggleTheme() {
        return isDarkTheme() ? LIGHT : DARK;
    }

    public static boolean saveTheme(String themeName) {
        boolean saved = new AppSettingsDAO().saveTheme(normalizeTheme(themeName));
        if (saved) {
            SchoolSettingsService.refresh();
        }
        return saved;
    }

    public static void applyCurrentTheme(Node rootNode) {
        applyTheme(rootNode, currentTheme());
    }

    public static void applyTheme(Node rootNode, String themeName) {
        if (rootNode == null) {
            return;
        }
        rootNode.getStyleClass().remove(LIGHT_CLASS);
        rootNode.getStyleClass().remove(DARK_CLASS);
        rootNode.getStyleClass().add(DARK.equals(normalizeTheme(themeName)) ? DARK_CLASS : LIGHT_CLASS);
    }

    public static void updateThemeButton(Button button) {
        if (button == null) {
            return;
        }
        button.setText(isDarkTheme() ? "☀ Light" : "◐ Dark");
    }

    public static String normalizeTheme(String themeName) {
        return DARK.equalsIgnoreCase(themeName) ? DARK : LIGHT;
    }
}
