package com.pdftoolkit.services;

import javafx.scene.image.Image;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LRU cache for PDF page thumbnails.
 * Cache key: (PDF path, page index, zoom bucket)
 * Zoom bucket is rounded to nearest 0.1 to avoid endless variants.
 */
public class PdfThumbnailCache {
    
    private static final int MAX_ENTRIES = 200; // Keep last 200 thumbnails
    private final Map<CacheKey, Image> cache;
    
    public PdfThumbnailCache() {
        // LinkedHashMap with access-order for LRU
        this.cache = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, Image> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }
    
    /**
     * Get cached thumbnail if available.
     * @param pdfPath Path to PDF file
     * @param pageIndex Zero-based page index
     * @param zoom Current zoom level
     * @return Cached image or null if not found
     */
    public synchronized Image get(Path pdfPath, int pageIndex, double zoom) {
        double zoomBucket = roundZoom(zoom);
        CacheKey key = new CacheKey(pdfPath, pageIndex, zoomBucket);
        return cache.get(key);
    }
    
    /**
     * Put thumbnail in cache.
     * @param pdfPath Path to PDF file
     * @param pageIndex Zero-based page index
     * @param zoom Current zoom level
     * @param image Rendered image
     */
    public synchronized void put(Path pdfPath, int pageIndex, double zoom, Image image) {
        double zoomBucket = roundZoom(zoom);
        CacheKey key = new CacheKey(pdfPath, pageIndex, zoomBucket);
        cache.put(key, image);
    }
    
    /**
     * Clear all cached thumbnails.
     */
    public synchronized void clear() {
        cache.clear();
    }
    
    /**
     * Clear cache for specific PDF file.
     * @param pdfPath Path to PDF file
     */
    public synchronized void clearForFile(Path pdfPath) {
        cache.entrySet().removeIf(entry -> entry.getKey().pdfPath.equals(pdfPath));
    }
    
    /**
     * Get current cache size.
     * @return Number of cached thumbnails
     */
    public synchronized int size() {
        return cache.size();
    }
    
    /**
     * Round zoom to nearest 0.1 to create buckets.
     * This prevents cache explosion from slightly different zoom values.
     */
    private double roundZoom(double zoom) {
        return Math.round(zoom * 10.0) / 10.0;
    }
    
    /**
     * Cache key composed of PDF path, page index, and zoom bucket.
     */
    private static class CacheKey {
        final Path pdfPath;
        final int pageIndex;
        final double zoomBucket;
        
        CacheKey(Path pdfPath, int pageIndex, double zoomBucket) {
            this.pdfPath = pdfPath;
            this.pageIndex = pageIndex;
            this.zoomBucket = zoomBucket;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return pageIndex == cacheKey.pageIndex &&
                   Double.compare(cacheKey.zoomBucket, zoomBucket) == 0 &&
                   Objects.equals(pdfPath, cacheKey.pdfPath);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(pdfPath, pageIndex, zoomBucket);
        }
    }
}
