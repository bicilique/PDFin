package com.pdftoolkit.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for managing application paths and directories.
 * Provides access to the default output directory for PDF operations.
 */
public class AppPaths {
    
    private static final String APP_FOLDER_NAME = "PDFin";
    private static Path defaultOutputDir;
    
    /**
     * Gets the default output directory for PDF operations.
     * Creates the directory if it doesn't exist.
     * 
     * @return Path to ${user.home}/PDFin
     */
    public static Path getDefaultOutputDir() {
        if (defaultOutputDir == null) {
            String userHome = System.getProperty("user.home");
            defaultOutputDir = Paths.get(userHome, APP_FOLDER_NAME);
            
            // Ensure directory exists
            if (!Files.exists(defaultOutputDir)) {
                try {
                    Files.createDirectories(defaultOutputDir);
                } catch (IOException e) {
                    System.err.println("Failed to create default output directory: " + e.getMessage());
                    // Fallback to user home
                    defaultOutputDir = Paths.get(userHome);
                }
            }
        }
        
        return defaultOutputDir;
    }
    
    /**
     * Gets the default output directory as a string path.
     * 
     * @return String path to default output directory
     */
    public static String getDefaultOutputPath() {
        return getDefaultOutputDir().toString();
    }
    
    /**
     * Checks if the default output directory exists and is writable.
     * 
     * @return true if directory exists and is writable
     */
    public static boolean isDefaultOutputDirValid() {
        Path dir = getDefaultOutputDir();
        return Files.exists(dir) && Files.isDirectory(dir) && Files.isWritable(dir);
    }
}
