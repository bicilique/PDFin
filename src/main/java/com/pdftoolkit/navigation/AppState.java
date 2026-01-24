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
 */
public class AppState {

    private static AppState instance;

    private final ObservableList<RecentFile> recentFiles;

    private AppState() {
        recentFiles = FXCollections.observableArrayList();
    }

    public static AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
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
}
