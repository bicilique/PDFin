package com.pdftoolkit.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for generating PDF thumbnails and reading PDF metadata.
 * 
 * CURRENT IMPLEMENTATION: Simulated thumbnails using placeholders.
 * PRODUCTION READY: Interface designed for Apache PDFBox integration.
 * 
 * To enable real PDF processing:
 * 1. Add Apache PDFBox dependency to pom.xml
 * 2. Replace simulation methods with PDFBox calls
 * 3. Implementation examples provided in comments
 */
public class PdfMetadataUtil {
    
    private static final int THUMBNAIL_WIDTH = 48;
    private static final int THUMBNAIL_HEIGHT = 64;
    private static final Map<File, Image> thumbnailCache = new HashMap<>();
    private static final Map<File, Integer> pageCountCache = new HashMap<>();
    
    /**
     * Get the number of pages in a PDF file.
     * 
     * CURRENT: Returns simulated count (10-50 pages randomly).
     * PRODUCTION: Replace with PDFBox implementation.
     * 
     * @param pdfFile The PDF file to analyze
     * @return Number of pages, or 0 if error
     */
    public static int getPageCount(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            return 0;
        }
        
        // Check cache first
        if (pageCountCache.containsKey(pdfFile)) {
            return pageCountCache.get(pdfFile);
        }
        
        // SIMULATION: Random page count
        // TODO: Replace with actual PDFBox implementation
        int pageCount = simulatePageCount(pdfFile);
        
        /* PRODUCTION IMPLEMENTATION (requires Apache PDFBox):
        try (PDDocument document = PDDocument.load(pdfFile)) {
            pageCount = document.getNumberOfPages();
        } catch (IOException e) {
            System.err.println("Error reading PDF: " + e.getMessage());
            pageCount = 0;
        }
        */
        
        pageCountCache.put(pdfFile, pageCount);
        return pageCount;
    }
    
    /**
     * Generate a thumbnail image for the first page of a PDF.
     * 
     * CURRENT: Returns placeholder image with page icon.
     * PRODUCTION: Replace with PDFBox rendering.
     * 
     * @param pdfFile The PDF file to thumbnail
     * @return JavaFX Image of first page, or placeholder
     */
    public static Image generateThumbnail(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            return generatePlaceholderThumbnail();
        }
        
        // Check cache first
        if (thumbnailCache.containsKey(pdfFile)) {
            return thumbnailCache.get(pdfFile);
        }
        
        // SIMULATION: Generate placeholder
        // TODO: Replace with actual PDFBox rendering
        Image thumbnail = simulateThumbnail(pdfFile);
        
        /* PRODUCTION IMPLEMENTATION (requires Apache PDFBox):
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            if (document.getNumberOfPages() > 0) {
                BufferedImage bim = renderer.renderImageWithDPI(0, 72); // First page, 72 DPI
                BufferedImage scaled = scaleImage(bim, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
                thumbnail = SwingFXUtils.toFXImage(scaled, null);
            } else {
                thumbnail = generatePlaceholderThumbnail();
            }
        } catch (IOException e) {
            System.err.println("Error rendering PDF thumbnail: " + e.getMessage());
            thumbnail = generatePlaceholderThumbnail();
        }
        */
        
        thumbnailCache.put(pdfFile, thumbnail);
        return thumbnail;
    }
    
    /**
     * Generate a thumbnail for a specific page in a PDF.
     * Useful for range cards in split view.
     * 
     * @param pdfFile The PDF file
     * @param pageIndex The page index (0-based)
     * @return JavaFX Image of the page
     */
    public static Image generatePageThumbnail(File pdfFile, int pageIndex) {
        if (pdfFile == null || !pdfFile.exists() || pageIndex < 0) {
            return generatePlaceholderThumbnail();
        }
        
        // SIMULATION: Return placeholder with page number
        // TODO: Replace with actual PDFBox rendering
        return simulatePageThumbnail(pdfFile, pageIndex);
        
        /* PRODUCTION IMPLEMENTATION (requires Apache PDFBox):
        try (PDDocument document = PDDocument.load(pdfFile)) {
            if (pageIndex >= document.getNumberOfPages()) {
                return generatePlaceholderThumbnail();
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage bim = renderer.renderImageWithDPI(pageIndex, 72);
            BufferedImage scaled = scaleImage(bim, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
            return SwingFXUtils.toFXImage(scaled, null);
        } catch (IOException e) {
            System.err.println("Error rendering page thumbnail: " + e.getMessage());
            return generatePlaceholderThumbnail();
        }
        */
    }
    
    /**
     * Clear thumbnail cache for a specific file.
     * Call this when file is modified or removed.
     */
    public static void clearCache(File pdfFile) {
        thumbnailCache.remove(pdfFile);
        pageCountCache.remove(pdfFile);
    }
    
    /**
     * Clear all caches.
     */
    public static void clearAllCaches() {
        thumbnailCache.clear();
        pageCountCache.clear();
    }
    
    /**
     * Get cache statistics for monitoring.
     */
    public static String getCacheStats() {
        return String.format("Thumbnails cached: %d, Page counts cached: %d",
                           thumbnailCache.size(), pageCountCache.size());
    }
    
    // ========== SIMULATION METHODS (Remove when PDFBox is integrated) ==========
    
    private static int simulatePageCount(File pdfFile) {
        // Generate consistent page count based on file size
        long size = pdfFile.length();
        return (int) (10 + (size % 40)); // 10-50 pages
    }
    
    private static Image simulateThumbnail(File pdfFile) {
        BufferedImage bufferedImage = new BufferedImage(
            THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background
        g2d.setColor(new Color(241, 245, 249)); // Light gray
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        
        // Border
        g2d.setColor(new Color(226, 232, 240));
        g2d.drawRect(0, 0, THUMBNAIL_WIDTH - 1, THUMBNAIL_HEIGHT - 1);
        
        // Simple page icon simulation
        g2d.setColor(new Color(100, 116, 139));
        int centerX = THUMBNAIL_WIDTH / 2;
        int centerY = THUMBNAIL_HEIGHT / 2;
        
        // Draw simple document lines
        for (int i = 0; i < 4; i++) {
            int y = centerY - 12 + (i * 6);
            g2d.drawLine(centerX - 12, y, centerX + 12, y);
        }
        
        g2d.dispose();
        
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }
    
    private static Image simulatePageThumbnail(File pdfFile, int pageIndex) {
        BufferedImage bufferedImage = new BufferedImage(
            THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Background
        g2d.setColor(new Color(248, 250, 252));
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        
        // Border
        g2d.setColor(new Color(203, 213, 225));
        g2d.drawRect(0, 0, THUMBNAIL_WIDTH - 1, THUMBNAIL_HEIGHT - 1);
        
        // Page number
        g2d.setColor(new Color(71, 85, 105));
        g2d.setFont(g2d.getFont().deriveFont(10f));
        String pageNum = String.valueOf(pageIndex + 1);
        int strWidth = g2d.getFontMetrics().stringWidth(pageNum);
        g2d.drawString(pageNum, (THUMBNAIL_WIDTH - strWidth) / 2, THUMBNAIL_HEIGHT / 2 + 4);
        
        g2d.dispose();
        
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }
    
    private static Image generatePlaceholderThumbnail() {
        BufferedImage bufferedImage = new BufferedImage(
            THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Gray background
        g2d.setColor(new Color(229, 231, 235));
        g2d.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        
        // Border
        g2d.setColor(new Color(209, 213, 219));
        g2d.drawRect(0, 0, THUMBNAIL_WIDTH - 1, THUMBNAIL_HEIGHT - 1);
        
        g2d.dispose();
        
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }
    
    /**
     * Scale a BufferedImage to the specified dimensions while maintaining aspect ratio.
     * (Helper method for PDFBox integration)
     */
    private static BufferedImage scaleImage(BufferedImage original, int targetWidth, int targetHeight) {
        double aspectRatio = (double) original.getWidth() / original.getHeight();
        int scaledWidth = targetWidth;
        int scaledHeight = (int) (targetWidth / aspectRatio);
        
        if (scaledHeight > targetHeight) {
            scaledHeight = targetHeight;
            scaledWidth = (int) (targetHeight * aspectRatio);
        }
        
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // Center the image
        int x = (targetWidth - scaledWidth) / 2;
        int y = (targetHeight - scaledHeight) / 2;
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, targetWidth, targetHeight);
        g2d.drawImage(original, x, y, scaledWidth, scaledHeight, null);
        g2d.dispose();
        
        return scaled;
    }
}
