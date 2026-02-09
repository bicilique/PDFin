package com.pdftoolkit.utils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * LocaleManager - Handles language/locale management with persistence
 */
public class LocaleManager {
    
    private static final String PREF_KEY_LANGUAGE = "language";
    private static final Preferences prefs = Preferences.userNodeForPackage(LocaleManager.class);
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    
    private static final ObjectProperty<Locale> currentLocale = new SimpleObjectProperty<>();
    private static ResourceBundle currentBundle;
    
    // Available locales
    public static final Locale ENGLISH = new Locale("en");
    public static final Locale INDONESIAN = new Locale("id");
    
    static {
        // Initialize with saved locale or default to Indonesian
        String savedLang = prefs.get(PREF_KEY_LANGUAGE, "id");
        Locale locale = "en".equals(savedLang) ? ENGLISH : INDONESIAN;
        setLocale(locale);
    }
    
    /**
     * Set the current locale
     */
    public static void setLocale(Locale locale) {
        currentLocale.set(locale);
        currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
        saveLocalePreference(locale);
    }
    
    /**
     * Get the current locale
     */
    public static Locale getLocale() {
        return currentLocale.get();
    }
    
    /**
     * Get the current locale property (for binding)
     */
    public static ObjectProperty<Locale> localeProperty() {
        return currentLocale;
    }
    
    /**
     * Get the current resource bundle (for FXMLLoader)
     */
    public static ResourceBundle getBundle() {
        return currentBundle;
    }
    
    /**
     * Get a localized string
     */
    public static String getString(String key) {
        try {
            return currentBundle.getString(key);
        } catch (Exception e) {
            System.err.println("Missing translation key: " + key);
            return "!" + key + "!";
        }
    }
    
    /**
     * Get a localized string with parameters
     */
    public static String getString(String key, Object... params) {
        try {
            String pattern = currentBundle.getString(key);
            return String.format(pattern, params);
        } catch (Exception e) {
            System.err.println("Missing translation key: " + key);
            return "!" + key + "!";
        }
    }
    
    /**
     * Switch to the other language (toggle between English and Indonesian)
     */
    public static void toggleLanguage() {
        Locale newLocale = currentLocale.get().equals(ENGLISH) ? INDONESIAN : ENGLISH;
        setLocale(newLocale);
    }
    
    /**
     * Check if current locale is English
     */
    public static boolean isEnglish() {
        return currentLocale.get().equals(ENGLISH);
    }
    
    /**
     * Check if current locale is Indonesian
     */
    public static boolean isIndonesian() {
        return currentLocale.get().equals(INDONESIAN);
    }
    
    /**
     * Get the display name of current language
     */
    public static String getCurrentLanguageName() {
        return isEnglish() ? "English" : "Bahasa Indonesia";
    }
    
    /**
     * Save locale preference
     */
    private static void saveLocalePreference(Locale locale) {
        String langCode = locale.getLanguage();
        prefs.put(PREF_KEY_LANGUAGE, langCode);
        try {
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Failed to save locale preference: " + e.getMessage());
        }
    }
    
    /**
     * Reload the resource bundle (useful after locale change)
     */
    public static void reloadBundle() {
        currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale.get());
    }
}
