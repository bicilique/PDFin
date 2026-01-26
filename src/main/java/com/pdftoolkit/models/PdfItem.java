package com.pdftoolkit.models;

import javafx.beans.property.*;
import javafx.scene.image.Image;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Model class representing a PDF file in the merge list.
 * Uses JavaFX properties for reactive UI binding.
 * Designed to be loaded asynchronously without blocking the UI thread.
 */
public class PdfItem {
    
    private final ObjectProperty<Path> path;
    private final StringProperty fileName;
    private final LongProperty fileSizeBytes;
    private final IntegerProperty pageCount;
    private final ObjectProperty<Image> thumbnail;
    private final BooleanProperty loading;
    private final StringProperty error;
    
    /**
     * Create a new PdfItem with the given file path.
     * Metadata (size, page count, thumbnail) should be loaded asynchronously.
     */
    public PdfItem(Path path) {
        this.path = new SimpleObjectProperty<>(path);
        this.fileName = new SimpleStringProperty(path.getFileName().toString());
        this.fileSizeBytes = new SimpleLongProperty(0L);
        this.pageCount = new SimpleIntegerProperty(0);
        this.thumbnail = new SimpleObjectProperty<>(null);
        this.loading = new SimpleBooleanProperty(true);
        this.error = new SimpleStringProperty(null);
    }
    
    // Path property
    public ObjectProperty<Path> pathProperty() {
        return path;
    }
    
    public Path getPath() {
        return path.get();
    }
    
    public void setPath(Path path) {
        this.path.set(path);
        // Update file name when path changes
        if (path != null) {
            this.fileName.set(path.getFileName().toString());
        }
    }
    
    // File name property
    public StringProperty fileNameProperty() {
        return fileName;
    }
    
    public String getFileName() {
        return fileName.get();
    }
    
    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }
    
    // File size property
    public LongProperty fileSizeBytesProperty() {
        return fileSizeBytes;
    }
    
    public long getFileSizeBytes() {
        return fileSizeBytes.get();
    }
    
    public void setFileSizeBytes(long size) {
        this.fileSizeBytes.set(size);
    }
    
    // Page count property
    public IntegerProperty pageCountProperty() {
        return pageCount;
    }
    
    public int getPageCount() {
        return pageCount.get();
    }
    
    public void setPageCount(int count) {
        this.pageCount.set(count);
    }
    
    // Thumbnail property
    public ObjectProperty<Image> thumbnailProperty() {
        return thumbnail;
    }
    
    public Image getThumbnail() {
        return thumbnail.get();
    }
    
    public void setThumbnail(Image image) {
        this.thumbnail.set(image);
    }
    
    // Loading property
    public BooleanProperty loadingProperty() {
        return loading;
    }
    
    public boolean isLoading() {
        return loading.get();
    }
    
    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }
    
    // Error property
    public StringProperty errorProperty() {
        return error;
    }
    
    public String getError() {
        return error.get();
    }
    
    public void setError(String error) {
        this.error.set(error);
    }
    
    public boolean hasError() {
        return error.get() != null && !error.get().isEmpty();
    }
    
    /**
     * Helper to format file size in human-readable format
     */
    public String getFormattedFileSize() {
        long bytes = fileSizeBytes.get();
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PdfItem pdfItem = (PdfItem) o;
        return Objects.equals(getPath(), pdfItem.getPath());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getPath());
    }
    
    @Override
    public String toString() {
        return "PdfItem{" +
                "fileName=" + getFileName() +
                ", pageCount=" + getPageCount() +
                ", size=" + getFormattedFileSize() +
                ", loading=" + isLoading() +
                ", hasError=" + hasError() +
                '}';
    }
}
