package com.pdftoolkit.ui;

/**
 * Factory class for creating TablerIconView instances.
 * Uses SVG-based icons with proper CSS theming support.
 */
public class Icons {
    
    // Standard icon sizes
    public static final double SIZE_SMALL = 14;
    public static final double SIZE_MEDIUM = 18;
    public static final double SIZE_LARGE = 24;
    public static final double SIZE_XLARGE = 64;
    
    /**
     * Create an icon with default size (16px)
     */
    public static TablerIconView create(String iconName) {
        return new TablerIconView(iconName, 16);
    }
    
    /**
     * Create an icon with specific size
     */
    public static TablerIconView create(String iconName, double size) {
        return new TablerIconView(iconName, size);
    }
    
    /**
     * Create a small icon (14px)
     */
    public static TablerIconView small(String iconName) {
        return new TablerIconView(iconName, SIZE_SMALL);
    }
    
    /**
     * Create a medium icon (18px)
     */
    public static TablerIconView medium(String iconName) {
        return new TablerIconView(iconName, SIZE_MEDIUM);
    }
    
    /**
     * Create a large icon (24px)
     */
    public static TablerIconView large(String iconName) {
        return new TablerIconView(iconName, SIZE_LARGE);
    }
    
    /**
     * Create an extra-large icon (48px)
     */
    public static TablerIconView xlarge(String iconName) {
        return new TablerIconView(iconName, SIZE_XLARGE);
    }
    
    // Convenience methods for common icons
    public static TablerIconView home() {
        return create("home");
    }
    
    public static TablerIconView merge() {
        return create("merge");
    }
    
    public static TablerIconView split() {
        return create("split");
    }
    
    public static TablerIconView compress() {
        return create("compress");
    }
    
    public static TablerIconView resize() {
        return create("resize");
    }
    
    public static TablerIconView lock() {
        return create("lock");
    }
    
    public static TablerIconView lockBolt() {
        return create("lock-bolt");
    }
    
    public static TablerIconView folder() {
        return create("folder");
    }
    
    public static TablerIconView folderOpen() {
        return create("folder-open");
    }
    
    public static TablerIconView folders() {
        return create("folders");
    }
    
    public static TablerIconView fileInfo() {
        return create("file-info");
    }
    
    public static TablerIconView deviceUsb() {
        return create("device-usb");
    }
    
    public static TablerIconView plus() {
        return create("plus");
    }
    
    public static TablerIconView trash() {
        return create("trash");
    }
    
    public static TablerIconView play() {
        return create("play");
    }
    
    public static TablerIconView check() {
        return create("check");
    }
    
    public static TablerIconView circleCheck() {
        return create("circle-check");
    }
    
    public static TablerIconView alertTriangle() {
        return create("alert-triangle");
    }
    
    public static TablerIconView circleX() {
        return create("circle-x");
    }
    
    public static TablerIconView x() {
        return create("x");
    }
    
    public static TablerIconView settings() {
        return create("settings");
    }
    
    public static TablerIconView info() {
        return create("info-circle");
    }
    
    public static TablerIconView infoCircle() {
        return create("info-circle");
    }
    
    public static TablerIconView eye() {
        return create("eye");
    }
    
    public static TablerIconView eyeOff() {
        return create("eye-off");
    }
    
    public static TablerIconView loader() {
        return create("loader");
    }
    
    public static TablerIconView sun() {
        return create("sun");
    }
    
    public static TablerIconView moon() {
        return create("moon");
    }
}

