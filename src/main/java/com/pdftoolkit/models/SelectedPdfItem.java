package com.pdftoolkit.models;

import javafx.beans.property.*;
import javafx.scene.image.Image;

import java.io.File;

/**
 * Model class representing a PDF file selected for lock/encryption.
 * Contains observable properties for binding to JavaFX UI components.
 */
public class SelectedPdfItem {
    
    /**
     * Processing status for a PDF file
     */
    public enum Status {
        READY("lock.status.ready"),
        PROCESSING("lock.status.processing"),
        DONE("lock.status.done"),
        FAILED("lock.status.failed");
        
        private final String resourceKey;
        
        Status(String resourceKey) {
            this.resourceKey = resourceKey;
        }
        
        public String getResourceKey() {
            return resourceKey;
        }
    }
    
    private final File sourceFile;
    private final StringProperty fileName;
    private final LongProperty sizeBytes;
    private final IntegerProperty pageCount;
    private final StringProperty outputName;
    private final ObjectProperty<Status> status;
    private final StringProperty errorMessage;
    private final ObjectProperty<Image> thumbnail;
    
    /**
     * Creates a new SelectedPdfItem from a source file.
     * 
     * @param sourceFile The source PDF file
     */
    public SelectedPdfItem(File sourceFile) {
        this.sourceFile = sourceFile;
        this.fileName = new SimpleStringProperty(sourceFile.getName());
        this.sizeBytes = new SimpleLongProperty(sourceFile.length());
        this.pageCount = new SimpleIntegerProperty(-1); // -1 = not loaded yet
        
        // Generate default output name
        this.outputName = new SimpleStringProperty(generateOutputName(sourceFile.getName()));
        
        this.status = new SimpleObjectProperty<>(Status.READY);
        this.errorMessage = new SimpleStringProperty("");
        this.thumbnail = new SimpleObjectProperty<>(null); // null = not loaded yet
    }
    
    /**
     * Generates output filename by inserting "-locked" before extension.
     * Example: "document.pdf" -> "document-locked.pdf"
     */
    private String generateOutputName(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "locked.pdf";
        }
        
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot > 0) {
            String nameWithoutExt = originalName.substring(0, lastDot);
            String extension = originalName.substring(lastDot);
            return nameWithoutExt + "-locked" + extension;
        }
        
        return originalName + "-locked.pdf";
    }
    
    /**
     * Formats file size in human-readable format.
     */
    public String getFormattedSize() {
        long bytes = sizeBytes.get();
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    // Getters for properties and values
    
    public File getSourceFile() {
        return sourceFile;
    }
    
    public StringProperty fileNameProperty() {
        return fileName;
    }
    
    public String getFileName() {
        return fileName.get();
    }
    
    public LongProperty sizeBytesProperty() {
        return sizeBytes;
    }
    
    public long getSizeBytes() {
        return sizeBytes.get();
    }
    
    public IntegerProperty pageCountProperty() {
        return pageCount;
    }
    
    public int getPageCount() {
        return pageCount.get();
    }
    
    public void setPageCount(int count) {
        this.pageCount.set(count);
    }
    
    public StringProperty outputNameProperty() {
        return outputName;
    }
    
    public String getOutputName() {
        return outputName.get();
    }
    
    public void setOutputName(String name) {
        this.outputName.set(name);
    }
    
    public ObjectProperty<Status> statusProperty() {
        return status;
    }
    
    public Status getStatus() {
        return status.get();
    }
    
    public void setStatus(Status status) {
        this.status.set(status);
    }
    
    public StringProperty errorMessageProperty() {
        return errorMessage;
    }
    
    public String getErrorMessage() {
        return errorMessage.get();
    }
    
    public void setErrorMessage(String message) {
        this.errorMessage.set(message);
    }
    
    public ObjectProperty<Image> thumbnailProperty() {
        return thumbnail;
    }
    
    public Image getThumbnail() {
        return thumbnail.get();
    }
    
    public void setThumbnail(Image image) {
        this.thumbnail.set(image);
    }
    
    @Override
    public String toString() {
        return "SelectedPdfItem{" +
                "fileName='" + getFileName() + '\'' +
                ", status=" + getStatus() +
                ", outputName='" + getOutputName() + '\'' +
                '}';
    }
}
