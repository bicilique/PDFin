package com.pdftoolkit.models;

import javafx.beans.property.*;

import java.io.File;

/**
 * Model class representing a PDF file to be protected/locked.
 * Contains observable properties for binding to JavaFX UI components.
 */
public class ProtectedPdfItem {
    
    /**
     * Status enum for tracking processing state
     */
    public enum Status {
        READY("protect.status.ready"),
        PROCESSING("protect.status.processing"),
        COMPLETED("protect.status.completed"),
        FAILED("protect.status.failed");
        
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
    private final StringProperty fileSize;
    private final IntegerProperty pageCount;
    private final StringProperty outputName;
    private final ObjectProperty<Status> status;
    private final StringProperty errorMessage;
    
    /**
     * Creates a new ProtectedPdfItem from a source file.
     * 
     * @param sourceFile The source PDF file to protect
     */
    public ProtectedPdfItem(File sourceFile) {
        this.sourceFile = sourceFile;
        this.fileName = new SimpleStringProperty(sourceFile.getName());
        this.fileSize = new SimpleStringProperty(formatFileSize(sourceFile.length()));
        this.pageCount = new SimpleIntegerProperty(-1); // -1 = not yet loaded
        
        // Generate default output name: original-locked.pdf
        String defaultOutputName = generateDefaultOutputName(sourceFile.getName());
        this.outputName = new SimpleStringProperty(defaultOutputName);
        
        this.status = new SimpleObjectProperty<>(Status.READY);
        this.errorMessage = new SimpleStringProperty("");
    }
    
    /**
     * Generates default output name by inserting "-locked" before extension.
     * Example: "document.pdf" -> "document-locked.pdf"
     */
    private String generateDefaultOutputName(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "locked.pdf";
        }
        
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String nameWithoutExt = originalName.substring(0, lastDotIndex);
            String extension = originalName.substring(lastDotIndex);
            return nameWithoutExt + "-locked" + extension;
        }
        
        return originalName + "-locked";
    }
    
    /**
     * Formats file size in human-readable format (KB, MB, GB)
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    // Getters for non-property values
    public File getSourceFile() {
        return sourceFile;
    }
    
    // Property getters
    public StringProperty fileNameProperty() {
        return fileName;
    }
    
    public String getFileName() {
        return fileName.get();
    }
    
    public StringProperty fileSizeProperty() {
        return fileSize;
    }
    
    public String getFileSize() {
        return fileSize.get();
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
    
    @Override
    public String toString() {
        return "ProtectedPdfItem{" +
                "fileName=" + getFileName() +
                ", status=" + getStatus() +
                ", outputName=" + getOutputName() +
                '}';
    }
}
