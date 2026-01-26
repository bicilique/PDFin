package com.pdftoolkit.state;

/**
 * Compression levels for PDF compression.
 * Each level has specific DPI and JPEG quality parameters.
 */
public enum CompressionLevel {
    
    /**
     * Low compression - best quality, least file size reduction.
     * Render at ~180 DPI, JPEG quality ~0.90
     */
    LOW(180, 0.90f, "Low Compression"),
    
    /**
     * Recommended compression - balanced quality and size.
     * Render at ~140 DPI, JPEG quality ~0.75
     */
    RECOMMENDED(140, 0.75f, "Recommended"),
    
    /**
     * Extreme compression - smallest file, most quality loss.
     * Render at ~100 DPI, JPEG quality ~0.60
     */
    EXTREME(100, 0.60f, "Extreme Compression");
    
    private final int dpi;
    private final float jpegQuality;
    private final String displayName;
    
    CompressionLevel(int dpi, float jpegQuality, String displayName) {
        this.dpi = dpi;
        this.jpegQuality = jpegQuality;
        this.displayName = displayName;
    }
    
    /**
     * Get the DPI to render pages at for this compression level.
     */
    public int getDpi() {
        return dpi;
    }
    
    /**
     * Get the JPEG quality (0.0 to 1.0) for this compression level.
     */
    public float getJpegQuality() {
        return jpegQuality;
    }
    
    /**
     * Get display name for UI.
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Apply "keep best quality" modifier.
     * Increases DPI by ~20% and quality by ~0.10
     */
    public int getModifiedDpi(boolean keepBestQuality) {
        if (keepBestQuality) {
            return (int) (dpi * 1.2);
        }
        return dpi;
    }
    
    public float getModifiedJpegQuality(boolean keepBestQuality) {
        if (keepBestQuality) {
            return Math.min(1.0f, jpegQuality + 0.10f);
        }
        return jpegQuality;
    }
    
    /**
     * Get compression level from slider value (1, 2, 3).
     */
    public static CompressionLevel fromSliderValue(int value) {
        return switch (value) {
            case 1 -> LOW;
            case 3 -> EXTREME;
            default -> RECOMMENDED;
        };
    }
    
    /**
     * Get slider value (1, 2, 3) from compression level.
     */
    public int toSliderValue() {
        return switch (this) {
            case LOW -> 1;
            case EXTREME -> 3;
            default -> 2;
        };
    }
}
