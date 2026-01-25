package com.pdftoolkit.services;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating PDF thumbnails.
 * Provides caching to avoid regenerating thumbnails for the same files.
 */
public class PdfThumbnailService {
    
    private static final float DEFAULT_DPI = 72f; // Lower DPI for faster rendering
    private static final int THUMBNAIL_WIDTH = 150;
    
    // Cache thumbnails by file path + page index
    private final Map<String, Image> thumbnailCache = new ConcurrentHashMap<>();
    
    /**
     * Generates a thumbnail for the first page of a PDF.
     * 
     * @param pdfFile PDF file
     * @return JavaFX Image thumbnail, or null if generation fails
     */
    public Image generateThumbnail(File pdfFile) {
        return generateThumbnail(pdfFile, 0);
    }
    
    /**
     * Generates a thumbnail for a specific page of a PDF.
     * 
     * @param pdfFile PDF file
     * @param pageIndex Page index (0-based)
     * @return JavaFX Image thumbnail, or null if generation fails
     */
    public Image generateThumbnail(File pdfFile, int pageIndex) {
        return generateThumbnail(pdfFile, pageIndex, THUMBNAIL_WIDTH);
    }
    
    /**
     * Generates a thumbnail for a specific page with custom width.
     * 
     * @param pdfFile PDF file
     * @param pageIndex Page index (0-based)
     * @param thumbnailWidth Desired thumbnail width in pixels
     * @return JavaFX Image thumbnail, or null if generation fails
     */
    public Image generateThumbnail(File pdfFile, int pageIndex, int thumbnailWidth) {
        if (pdfFile == null || !pdfFile.exists()) {
            return null;
        }
        
        String cacheKey = pdfFile.getAbsolutePath() + ":" + pageIndex + ":" + thumbnailWidth;
        
        // Return cached thumbnail if available
        if (thumbnailCache.containsKey(cacheKey)) {
            return thumbnailCache.get(cacheKey);
        }
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                return null;
            }
            
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage bufferedImage = renderer.renderImageWithDPI(pageIndex, DEFAULT_DPI);
            
            // Scale to thumbnail size
            BufferedImage scaledImage = scaleImage(bufferedImage, thumbnailWidth);
            
            // Convert to JavaFX Image
            Image fxImage = SwingFXUtils.toFXImage(scaledImage, null);
            
            // Cache the thumbnail
            thumbnailCache.put(cacheKey, fxImage);
            
            return fxImage;
            
        } catch (IOException e) {
            System.err.println("Failed to generate thumbnail for: " + pdfFile.getName() + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the page count of a PDF file.
     * 
     * @param pdfFile PDF file
     * @return Number of pages, or 0 if file cannot be read
     */
    public int getPageCount(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            return 0;
        }
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            System.err.println("Failed to read page count for: " + pdfFile.getName());
            return 0;
        }
    }
    
    /**
     * Clears the thumbnail cache to free memory.
     */
    public void clearCache() {
        thumbnailCache.clear();
    }
    
    /**
     * Removes a specific file's thumbnails from cache.
     * 
     * @param pdfFile PDF file
     */
    public void removeCachedThumbnails(File pdfFile) {
        if (pdfFile == null) return;
        
        String pathPrefix = pdfFile.getAbsolutePath() + ":";
        thumbnailCache.keySet().removeIf(key -> key.startsWith(pathPrefix));
    }
    
    /**
     * Scales a BufferedImage to a target width while maintaining aspect ratio.
     * 
     * @param original Original image
     * @param targetWidth Target width in pixels
     * @return Scaled BufferedImage
     */
    private BufferedImage scaleImage(BufferedImage original, int targetWidth) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        if (originalWidth <= targetWidth) {
            return original; // No scaling needed
        }
        
        double scaleFactor = (double) targetWidth / originalWidth;
        int targetHeight = (int) (originalHeight * scaleFactor);
        
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        
        return scaled;
    }
}
