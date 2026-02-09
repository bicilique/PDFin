package com.pdftoolkit.services;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Production-grade service for generating PDF thumbnails with async rendering and caching.
 * Features:
 * - Real PDFBox rendering with configurable DPI
 * - Async rendering with CompletableFuture
 * - Generation tokens for cancellation
 * - Integration with PdfThumbnailCache for LRU caching
 * - Thread pool for parallel rendering
 */
public class PdfThumbnailService {
    
    private static final int BASE_DPI = 120; // Base DPI at zoom 1.0
    private static final int MAX_DPI = 240;  // Max DPI to cap memory usage
    private static final int THREAD_POOL_SIZE = 2; // Reduced to 2 for better resource management
    
    private final PdfThumbnailCache cache;
    private final ExecutorService renderExecutor;
    private final AtomicLong renderGeneration = new AtomicLong(0);
    
    public PdfThumbnailService() {
        this.cache = new PdfThumbnailCache();
        this.renderExecutor = Executors.newFixedThreadPool(
            THREAD_POOL_SIZE, 
            r -> {
                Thread t = new Thread(r, "PDF-Thumbnail-Renderer");
                t.setDaemon(true);
                return t;
            }
        );
    }
    
    /**
     * Result of thumbnail rendering operation.
     */
    public record ThumbnailResult(Image image, boolean fromCache, long generation) {}
    
    /**
     * Generates a thumbnail asynchronously for a specific page at given zoom level.
     * 
     * @param pdfFile PDF file
     * @param pageIndex Page index (0-based)
     * @param zoom Zoom level (1.0 = 100%, 2.0 = 200%)
     * @return CompletableFuture containing thumbnail result
     */
    public CompletableFuture<ThumbnailResult> generateThumbnailAsync(File pdfFile, int pageIndex, double zoom) {
        return generateThumbnailAsync(pdfFile.toPath(), pageIndex, zoom);
    }
    
    /**
     * Generates a thumbnail asynchronously for a specific page at given zoom level.
     * 
     * @param pdfPath PDF file path
     * @param pageIndex Page index (0-based)
     * @param zoom Zoom level (1.0 = 100%, 2.0 = 200%)
     * @return CompletableFuture containing thumbnail result
     */
    public CompletableFuture<ThumbnailResult> generateThumbnailAsync(Path pdfPath, int pageIndex, double zoom) {
        final long generation = renderGeneration.get();
        
        // Check cache first
        Image cached = cache.get(pdfPath, pageIndex, zoom);
        if (cached != null) {
            return CompletableFuture.completedFuture(new ThumbnailResult(cached, true, generation));
        }
        
        // Render asynchronously
        return CompletableFuture.supplyAsync(() -> {
            // Check if this generation is still valid
            if (generation != renderGeneration.get()) {
                throw new CancellationException("Render generation invalidated");
            }
            
            try {
                Image rendered = renderThumbnail(pdfPath, pageIndex, zoom);
                if (rendered != null && generation == renderGeneration.get()) {
                    cache.put(pdfPath, pageIndex, zoom, rendered);
                }
                return new ThumbnailResult(rendered, false, generation);
            } catch (Exception e) {
                throw new CompletionException("Failed to render thumbnail", e);
            }
        }, renderExecutor);
    }
    
    /**
     * Renders a thumbnail synchronously (blocking call).
     * 
     * @param pdfPath PDF file path
     * @param pageIndex Page index (0-based)
     * @param zoom Zoom level (1.0 = 100%, 2.0 = 200%)
     * @return JavaFX Image thumbnail, or null if rendering fails
     */
    private Image renderThumbnail(Path pdfPath, int pageIndex, double zoom) {
        File pdfFile = pdfPath.toFile();
        if (!pdfFile.exists()) {
            return null;
        }
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                return null;
            }
            
            // Calculate DPI based on zoom level
            int dpi = calculateDPI(zoom);
            
            // Render at calculated DPI with RGB for proper colors
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage bufferedImage = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
            
            // Convert to JavaFX Image
            return SwingFXUtils.toFXImage(bufferedImage, null);
            
        } catch (IOException e) {
            System.err.println("Failed to render thumbnail: " + pdfFile.getName() 
                             + " page " + (pageIndex + 1) + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculates DPI based on zoom level.
     * 
     * @param zoom Zoom level (1.0 = 100%)
     * @return DPI value, capped at MAX_DPI
     */
    private int calculateDPI(double zoom) {
        int dpi = (int) (BASE_DPI * zoom);
        return Math.min(dpi, MAX_DPI);
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
     * Generates a thumbnail synchronously for backward compatibility.
     * 
     * @param pdfFile PDF file
     * @return JavaFX Image thumbnail, or null if generation fails
     */
    public Image generateThumbnail(File pdfFile) {
        return generateThumbnail(pdfFile, 0);
    }
    
    /**
     * Generates a thumbnail synchronously for backward compatibility.
     * 
     * @param pdfFile PDF file
     * @param pageIndex Page index (0-based)
     * @return JavaFX Image thumbnail, or null if generation fails
     */
    public Image generateThumbnail(File pdfFile, int pageIndex) {
        return generateThumbnail(pdfFile, pageIndex, 150);
    }
    
    /**
     * Generates a thumbnail synchronously for backward compatibility.
     * Uses zoom calculation to determine DPI based on target width.
     * 
     * @param pdfFile PDF file
     * @param pageIndex Page index (0-based)
     * @param thumbnailWidth Desired thumbnail width (used to calculate zoom)
     * @return JavaFX Image thumbnail, or null if generation fails
     */
    public Image generateThumbnail(File pdfFile, int pageIndex, int thumbnailWidth) {
        // Calculate approximate zoom based on thumbnail width
        // Assuming A4 page at 120 DPI is ~1000px wide
        double zoom = thumbnailWidth / 1000.0 * 8.5; // Approximate scaling
        zoom = Math.max(0.5, Math.min(zoom, 2.0)); // Clamp between 0.5 and 2.0
        
        return renderThumbnail(pdfFile.toPath(), pageIndex, zoom);
    }
    
    /**
     * Invalidates all current render operations by incrementing generation counter.
     * All in-progress renders will be cancelled.
     */
    public void cancelCurrentRenders() {
        renderGeneration.incrementAndGet();
    }
    
    /**
     * Clears the thumbnail cache and cancels all renders.
     */
    public void clearCache() {
        cancelCurrentRenders();
        cache.clear();
    }
    
    /**
     * Removes a specific file's thumbnails from cache and cancels renders.
     * 
     * @param pdfFile PDF file
     */
    public void removeCachedThumbnails(File pdfFile) {
        if (pdfFile == null) return;
        cancelCurrentRenders();
        cache.clearForFile(pdfFile.toPath());
    }
    
    /**
     * Gets the current cache size.
     * 
     * @return Number of cached thumbnails
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Shuts down the render executor service.
     * Should be called on application shutdown.
     */
    public void shutdown() {
        renderExecutor.shutdown();
        try {
            if (!renderExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                renderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            renderExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
