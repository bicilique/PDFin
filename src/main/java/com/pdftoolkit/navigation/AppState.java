package com.pdftoolkit.navigation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight application state holder.
 * Maintains recent files and shared data across views.
 * Extended to support tool-specific state persistence across navigation and language/theme changes.
 */
public class AppState {

    private static AppState instance;

    private final ObservableList<RecentFile> recentFiles;
    
    // Tool-specific state holders
    private SplitToolState splitToolState;
    private MergeToolState mergeToolState;

    private AppState() {
        recentFiles = FXCollections.observableArrayList();
        splitToolState = new SplitToolState();
        mergeToolState = new MergeToolState();
    }

    public static AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }
    
    /**
     * Get the split tool state holder.
     */
    public SplitToolState getSplitToolState() {
        return splitToolState;
    }
    
    /**
     * Get the merge tool state holder.
     */
    public MergeToolState getMergeToolState() {
        return mergeToolState;
    }

    /**
     * Add a file to recent files list.
     */
    public void addRecentFile(String operation, File outputFile) {
        recentFiles.add(0, new RecentFile(operation, outputFile.getName(), 
                                         outputFile.getParent(), LocalDateTime.now()));
        
        // Keep only last 10
        if (recentFiles.size() > 10) {
            recentFiles.remove(10, recentFiles.size());
        }
    }

    /**
     * Get the observable list of recent files.
     */
    public ObservableList<RecentFile> getRecentFiles() {
        return recentFiles;
    }

    /**
     * Recent file record.
     */
    public static class RecentFile {
        private final String operation;
        private final String fileName;
        private final String folderPath;
        private final LocalDateTime timestamp;

        public RecentFile(String operation, String fileName, String folderPath, LocalDateTime timestamp) {
            this.operation = operation;
            this.fileName = fileName;
            this.folderPath = folderPath;
            this.timestamp = timestamp;
        }

        public String getOperation() {
            return operation;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFolderPath() {
            return folderPath;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Split tool state - persists across navigation and language/theme changes.
     */
    public static class SplitToolState {
        private File selectedFile;
        private int totalPages;
        private double zoomLevel = 1.0;
        private String outputFolder;
        
        public File getSelectedFile() {
            return selectedFile;
        }
        
        public void setSelectedFile(File file) {
            this.selectedFile = file;
        }
        
        public int getTotalPages() {
            return totalPages;
        }
        
        public void setTotalPages(int pages) {
            this.totalPages = pages;
        }
        
        public double getZoomLevel() {
            return zoomLevel;
        }
        
        public void setZoomLevel(double zoom) {
            this.zoomLevel = zoom;
        }
        
        public String getOutputFolder() {
            return outputFolder;
        }
        
        public void setOutputFolder(String folder) {
            this.outputFolder = folder;
        }
        
        public void clear() {
            selectedFile = null;
            totalPages = 0;
            zoomLevel = 1.0;
            outputFolder = null;
        }
    }
    
    /**
     * Merge tool state - persists across navigation and language/theme changes.
     * Uses ObservableList to support reactive UI updates.
     */
    public static class MergeToolState {
        private final ObservableList<com.pdftoolkit.models.PdfItem> pdfItems;
        private String outputFolder;
        private String outputFilename;
        
        public MergeToolState() {
            this.pdfItems = FXCollections.observableArrayList();
            this.outputFilename = "merged.pdf";
        }
        
        public ObservableList<com.pdftoolkit.models.PdfItem> getPdfItems() {
            return pdfItems;
        }
        
        public void addPdfItem(com.pdftoolkit.models.PdfItem item) {
            // Prevent duplicates based on path
            if (pdfItems.stream().noneMatch(existing -> existing.getPath().equals(item.getPath()))) {
                pdfItems.add(item);
            }
        }
        
        public void removePdfItem(com.pdftoolkit.models.PdfItem item) {
            pdfItems.remove(item);
        }
        
        public void clearPdfItems() {
            pdfItems.clear();
        }
        
        public String getOutputFolder() {
            return outputFolder;
        }
        
        public void setOutputFolder(String folder) {
            this.outputFolder = folder;
        }
        
        public String getOutputFilename() {
            return outputFilename;
        }
        
        public void setOutputFilename(String filename) {
            this.outputFilename = filename;
        }
        
        /**
         * Get total page count from all PDF items.
         */
        public int getTotalPages() {
            return pdfItems.stream()
                    .mapToInt(com.pdftoolkit.models.PdfItem::getPageCount)
                    .sum();
        }
        
        /**
         * Check if any items are still loading.
         */
        public boolean hasLoadingItems() {
            return pdfItems.stream().anyMatch(com.pdftoolkit.models.PdfItem::isLoading);
        }
        
        /**
         * Check if any items have errors.
         */
        public boolean hasErrors() {
            return pdfItems.stream().anyMatch(com.pdftoolkit.models.PdfItem::hasError);
        }
        
        public void clear() {
            pdfItems.clear();
            outputFolder = null;
            outputFilename = "merged.pdf";
        }
    }
}
