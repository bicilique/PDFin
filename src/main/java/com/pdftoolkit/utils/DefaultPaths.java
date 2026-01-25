package com.pdftoolkit.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for managing default application paths.
 * Provides consistent default output folder location across the application.
 */
public class DefaultPaths {
    
    private static final String APP_FOLDER_NAME = "PDFin";
    private static File appOutputDir;
    
    /**
     * Get the default application output directory.
     * Default location: ${user.home}/PDFin
     * Creates the directory if it doesn't exist.
     * 
     * @return File representing the default output directory
     */
    public static File getAppOutputDir() {
        if (appOutputDir == null) {
            String userHome = System.getProperty("user.home");
            Path outputPath = Paths.get(userHome, APP_FOLDER_NAME);
            appOutputDir = outputPath.toFile();
            
            // Create directory if it doesn't exist
            if (!appOutputDir.exists()) {
                try {
                    Files.createDirectories(outputPath);
                } catch (IOException e) {
                    // Fallback to user home if creation fails
                    System.err.println("Failed to create PDFin output directory: " + e.getMessage());
                    appOutputDir = new File(userHome);
                }
            }
        }
        
        return appOutputDir;
    }
    
    /**
     * Get the absolute path to the default output directory as a String.
     * 
     * @return String path to the default output directory
     */
    public static String getAppOutputPath() {
        return getAppOutputDir().getAbsolutePath();
    }
    
    /**
     * Check if the default output directory exists and is writable.
     * 
     * @return true if directory exists and is writable, false otherwise
     */
    public static boolean isAppOutputDirValid() {
        File dir = getAppOutputDir();
        return dir.exists() && dir.isDirectory() && dir.canWrite();
    }
    
    /**
     * Reset the cached output directory (useful for testing).
     */
    public static void resetCache() {
        appOutputDir = null;
    }
}
