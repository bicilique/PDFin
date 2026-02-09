package com.pdftoolkit.utils;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

/**
 * ThemeManager - Handles light/dark mode theme switching with persistent storage
 */
public class ThemeManager {
    
    private static final String DARK_MODE_CLASS = "dark-mode";
    private static final String PREF_KEY_DARK_MODE = "darkMode";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    
    private static boolean isDarkMode = false;
    private static Scene currentScene;
    
    /**
     * Initialize the theme manager with a scene
     */
    public static void initialize(Scene scene) {
        currentScene = scene;
        // Load saved preference from persistent storage
        loadThemePreference();
    }
    
    /**
     * Toggle between light and dark mode
     */
    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme();
        saveThemePreference();
    }
    
    /**
     * Set dark mode explicitly
     */
    public static void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
        applyTheme();
        saveThemePreference();
    }
    
    /**
     * Check if dark mode is currently active
     */
    public static boolean isDarkMode() {
        return isDarkMode;
    }
    
    /**
     * Apply the current theme to the scene
     */
    private static void applyTheme() {
        if (currentScene != null) {
            if (isDarkMode) {
                if (!currentScene.getRoot().getStyleClass().contains(DARK_MODE_CLASS)) {
                    currentScene.getRoot().getStyleClass().add(DARK_MODE_CLASS);
                }
            } else {
                currentScene.getRoot().getStyleClass().remove(DARK_MODE_CLASS);
            }
        }
    }
    
    /**
     * Load theme preference from persistent storage
     */
    private static void loadThemePreference() {
        // Load from Java Preferences API (persists across app restarts)
        isDarkMode = prefs.getBoolean(PREF_KEY_DARK_MODE, false);
        applyTheme();
    }
    
    /**
     * Save theme preference to persistent storage
     */
    private static void saveThemePreference() {
        // Save to Java Preferences API
        prefs.putBoolean(PREF_KEY_DARK_MODE, isDarkMode);
        try {
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Failed to save theme preference: " + e.getMessage());
        }
    }
    
    /**
     * Get the theme icon (for toggle button)
     */
    public static String getThemeIcon() {
        return isDarkMode ? "☀" : "🌙";
    }
    
    /**
     * Get the theme tooltip text
     */
    public static String getThemeTooltip() {
        return isDarkMode ? 
            LocaleManager.getString("theme.light") : 
            LocaleManager.getString("theme.dark");
    }
}
