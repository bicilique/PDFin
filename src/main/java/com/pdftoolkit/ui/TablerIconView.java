package com.pdftoolkit.ui;

import javafx.beans.property.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reliable icon component using Unicode text symbols.
 * NO SVG DEPENDENCY - uses Text node with Unicode characters.
 * Stable and performant for JavaFX.
 */
public class TablerIconView extends Text {
    
    private static final Properties ICON_MAP = new Properties();
    private static boolean mapLoaded = false;
    
    private final StringProperty iconName = new SimpleStringProperty();
    private final DoubleProperty iconSize = new SimpleDoubleProperty(16);
    
    static {
        loadIconMapping();
    }
    
    private static void loadIconMapping() {
        if (mapLoaded) return;
        
        try (InputStream is = TablerIconView.class.getResourceAsStream("/icons/tabler-icons.properties")) {
            if (is != null) {
                ICON_MAP.load(is);
                mapLoaded = true;
                System.out.println("✅ Loaded " + ICON_MAP.size() + " icon mappings");
            } else {
                System.err.println("⚠️ Icon mapping file not found, using fallback icons");
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error loading icon mapping: " + e.getMessage());
        }
    }
    
    public TablerIconView() {
        this("", 16);
    }
    
    public TablerIconView(String iconName) {
        this(iconName, 16);
    }
    
    public TablerIconView(String iconName, double size) {
        getStyleClass().add("tabler-icon");
        
        // Bind properties
        this.iconName.addListener((obs, old, newVal) -> updateIcon(newVal));
        this.iconSize.addListener((obs, old, newVal) -> updateSize(newVal.doubleValue()));
        
        // Set initial values
        setIconName(iconName);
        setIconSize(size);
    }
    
    private void updateIcon(String name) {
        if (name == null || name.isEmpty()) {
            setText("");
            return;
        }
        
        String unicode = ICON_MAP.getProperty(name);
        if (unicode != null) {
            setText(unicode);
        } else {
            // Fallback: use first letter or generic symbol
            System.err.println("⚠️ Icon not found: " + name + ", using fallback");
            setText(getFallbackIcon(name));
        }
    }
    
    private String getFallbackIcon(String name) {
        // Provide sensible fallbacks
        switch (name.toLowerCase()) {
            case "home": return "\u2302";
            case "merge": return "\u2B0C";
            case "split": return "\u2702";
            case "compress": return "\u2B0A";
            case "lock": case "protect": return "\u1F512";
            case "folder": return "\u1F4C1";
            case "folder-open": return "\u1F4C2";
            case "file": case "file-pdf": return "\u1F4C4";
            case "plus": case "add": return "\u002B";
            case "trash": case "delete": case "remove": return "\u1F5D1";
            case "check": case "success": return "\u2713";
            case "x": case "close": case "error": return "\u00D7";
            case "alert": case "warning": return "\u26A0";
            case "info": return "\u2139";
            case "eye": return "\u1F441";
            case "eye-off": return "\u1F576";
            case "play": case "start": return "\u25B6";
            case "up": case "arrow-up": return "\u2191";
            case "down": case "arrow-down": return "\u2193";
            case "settings": return "\u2699";
            default: return "\u25A0"; // Generic square
        }
    }
    
    private void updateSize(double size) {
        setFont(Font.font("Segoe UI Symbol", size)); // Use system font with good symbol support
        setStyle("-fx-font-size: " + size + "px;");
    }
    
    // Property accessors
    public String getIconName() {
        return iconName.get();
    }
    
    public void setIconName(String iconName) {
        this.iconName.set(iconName);
    }
    
    public StringProperty iconNameProperty() {
        return iconName;
    }
    
    public double getIconSize() {
        return iconSize.get();
    }
    
    public void setIconSize(double size) {
        this.iconSize.set(size);
    }
    
    public DoubleProperty iconSizeProperty() {
        return iconSize;
    }
}
