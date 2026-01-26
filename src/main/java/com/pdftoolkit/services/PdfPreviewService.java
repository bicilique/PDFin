package com.pdftoolkit.services;

import com.pdftoolkit.models.PdfItem;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for asynchronously loading PDF metadata and thumbnails.
 * Used by Compress, Merge, and other features that display PDF file cards.
 * 
 * Features:
 * - Non-blocking async loading of file size, page count, and thumbnail
 * - Thumbnail caching to avoid re-rendering
 * - Automatic UI updates via JavaFX Platform.runLater
 * - Thread pool for parallel loading
 */
public class PdfPreviewService {
    
    private static final int THUMBNAIL_DPI = 72;
    private static final int THUMBNAIL_MAX_WIDTH = 64;
    private static final int THUMBNAIL_MAX_HEIGHT = 90;
    private static final int THREAD_POOL_SIZE = 3;
    
    private static volatile PdfPreviewService instance;
    
    private final ExecutorService executor;
    private final Map<Path, Image> thumbnailCache;
    
    private PdfPreviewService() {
        this.executor = Executors.newFixedThreadPool(
            THREAD_POOL_SIZE,
            r -> {
                Thread t = new Thread(r, "PDF-Preview-Loader");
                t.setDaemon(true);
                return t;
            }
        );
        this.thumbnailCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Get singleton instance (thread-safe).
     */
    public static PdfPreviewService getInstance() {
        if (instance == null) {
            synchronized (PdfPreviewService.class) {
                if (instance == null) {
                    instance = new PdfPreviewService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Load all metadata for a PdfItem asynchronously.
     * Updates the PdfItem properties on the JavaFX thread as data becomes available.
     * 
     * @param item The PdfItem to populate
     * @return CompletableFuture that completes when all metadata is loaded
     */
    public CompletableFuture<Void> loadMetadataAsync(PdfItem item) {
        return CompletableFuture.runAsync(() -> {
            Path path = item.getPath();
            
            try {
                // Mark as loading
                Platform.runLater(() -> item.setLoading(true));
                
                // Load file size
                long size = Files.size(path);
                Platform.runLater(() -> item.setFileSizeBytes(size));
                
                // Load page count and thumbnail from PDF
                try (PDDocument document = Loader.loadPDF(path.toFile())) {
                    int pageCount = document.getNumberOfPages();
                    Platform.runLater(() -> item.setPageCount(pageCount));
                    
                    // Load thumbnail (check cache first)
                    Image thumbnail = thumbnailCache.get(path);
                    if (thumbnail == null && pageCount > 0) {
                        thumbnail = renderThumbnail(document, 0);
                        if (thumbnail != null) {
                            thumbnailCache.put(path, thumbnail);
                        }
                    }
                    
                    final Image finalThumbnail = thumbnail;
                    Platform.runLater(() -> {
                        if (finalThumbnail != null) {
                            item.setThumbnail(finalThumbnail);
                        }
                        item.setLoading(false);
                        item.setError(null);
                    });
                    
                } catch (IOException e) {
                    String errorMsg = "Failed to read PDF: " + e.getMessage();
                    Platform.runLater(() -> {
                        item.setError(errorMsg);
                        item.setLoading(false);
                    });
                }
                
            } catch (IOException e) {
                String errorMsg = "Failed to read file: " + e.getMessage();
                Platform.runLater(() -> {
                    item.setError(errorMsg);
                    item.setLoading(false);
                });
            }
        }, executor);
    }
    
    /**
     * Render thumbnail from an already-open PDDocument.
     * Blocks until rendering completes (should be called from background thread).
     */
    private Image renderThumbnail(PDDocument document, int pageIndex) {
        try {
            if (pageIndex >= document.getNumberOfPages()) {
                return null;
            }
            
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage bufferedImage = renderer.renderImageWithDPI(
                pageIndex, 
                THUMBNAIL_DPI, 
                ImageType.RGB
            );
            
            // Scale to thumbnail size while maintaining aspect ratio
            BufferedImage scaled = scaleImage(bufferedImage, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
            
            return SwingFXUtils.toFXImage(scaled, null);
            
        } catch (IOException e) {
            System.err.println("Failed to render thumbnail: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Scale an image to fit within maxWidth x maxHeight while maintaining aspect ratio.
     */
    private BufferedImage scaleImage(BufferedImage original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        double aspectRatio = (double) width / height;
        int targetWidth, targetHeight;
        
        if (width > height) {
            targetWidth = Math.min(width, maxWidth);
            targetHeight = (int) (targetWidth / aspectRatio);
        } else {
            targetHeight = Math.min(height, maxHeight);
            targetWidth = (int) (targetHeight * aspectRatio);
        }
        
        // Ensure we don't exceed bounds
        if (targetWidth > maxWidth) {
            targetWidth = maxWidth;
            targetHeight = (int) (targetWidth / aspectRatio);
        }
        if (targetHeight > maxHeight) {
            targetHeight = maxHeight;
            targetWidth = (int) (targetHeight * aspectRatio);
        }
        
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION, 
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        
        return scaled;
    }
    
    /**
     * Clear thumbnail cache for a specific file.
     */
    public void clearCache(Path path) {
        thumbnailCache.remove(path);
    }
    
    /**
     * Clear entire thumbnail cache.
     */
    public void clearAllCache() {
        thumbnailCache.clear();
    }
    
    /**
     * Shutdown the executor service (call on application exit).
     */
    public void shutdown() {
        executor.shutdown();
    }
}
