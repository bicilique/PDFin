package com.pdftoolkit.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.File;
import java.io.IOException;

/**
 * Service for adding password protection to PDF files.
 * Uses Apache PDFBox encryption with AES-256 for strong security.
 */
public class PdfProtectionService {
    
    // Standard 256-bit AES encryption key length (strongest available)
    private static final int KEY_LENGTH_256 = 256;
    
    // Standard 128-bit AES encryption key length (fallback)
    private static final int KEY_LENGTH_128 = 128;
    
    /**
     * Result class containing operation outcome details
     */
    public static class ProtectionResult {
        private final boolean success;
        private final String errorMessage;
        private final File outputFile;
        
        public ProtectionResult(boolean success, String errorMessage, File outputFile) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.outputFile = outputFile;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public File getOutputFile() {
            return outputFile;
        }
        
        public static ProtectionResult success(File outputFile) {
            return new ProtectionResult(true, null, outputFile);
        }
        
        public static ProtectionResult failure(String errorMessage) {
            return new ProtectionResult(false, errorMessage, null);
        }
    }
    
    /**
     * Protects a PDF file with password encryption.
     * 
     * @param inputFile Source PDF file to protect
     * @param outputFile Destination for the protected PDF
     * @param password Password to use for encryption (user and owner)
     * @return ProtectionResult containing success status and details
     */
    public ProtectionResult protectPdf(File inputFile, File outputFile, String password) {
        // Validate inputs
        if (inputFile == null || !inputFile.exists()) {
            return ProtectionResult.failure("Input file does not exist");
        }
        
        if (!inputFile.canRead()) {
            return ProtectionResult.failure("Cannot read input file");
        }
        
        if (outputFile == null) {
            return ProtectionResult.failure("Output file not specified");
        }
        
        if (password == null || password.isEmpty()) {
            return ProtectionResult.failure("Password cannot be empty");
        }
        
        // Ensure output directory exists
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                return ProtectionResult.failure("Failed to create output directory");
            }
        }
        
        PDDocument document = null;
        try {
            // Load the PDF document
            document = Loader.loadPDF(inputFile);
            
            // Create access permissions (full permissions by default)
            AccessPermission accessPermission = new AccessPermission();
            // Allow all operations - the password only controls opening the document
            accessPermission.setCanPrint(true);
            accessPermission.setCanModify(true);
            accessPermission.setCanExtractContent(true);
            accessPermission.setCanModifyAnnotations(true);
            accessPermission.setCanFillInForm(true);
            accessPermission.setCanExtractForAccessibility(true);
            accessPermission.setCanAssembleDocument(true);
            
            // Create protection policy with AES-256 encryption
            StandardProtectionPolicy protectionPolicy;
            try {
                // Try AES-256 first (strongest)
                protectionPolicy = new StandardProtectionPolicy(password, password, accessPermission);
                protectionPolicy.setEncryptionKeyLength(KEY_LENGTH_256);
            } catch (Exception e) {
                // Fallback to AES-128 if 256 is not available
                protectionPolicy = new StandardProtectionPolicy(password, password, accessPermission);
                protectionPolicy.setEncryptionKeyLength(KEY_LENGTH_128);
            }
            
            // Use AES encryption (more secure than RC4)
            protectionPolicy.setPreferAES(true);
            
            // Apply protection to document
            document.protect(protectionPolicy);
            
            // Save the protected document
            document.save(outputFile);
            
            return ProtectionResult.success(outputFile);
            
        } catch (IOException e) {
            String errorMsg = "Failed to protect PDF: " + e.getMessage();
            return ProtectionResult.failure(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error: " + e.getMessage();
            return ProtectionResult.failure(errorMsg);
        } finally {
            // Always close the document
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    System.err.println("Failed to close document: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Gets the page count of a PDF file.
     * Useful for displaying metadata in the UI.
     * 
     * @param pdfFile PDF file to analyze
     * @return Number of pages, or -1 if cannot be determined
     */
    public int getPageCount(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            return -1;
        }
        
        PDDocument document = null;
        try {
            document = Loader.loadPDF(pdfFile);
            return document.getNumberOfPages();
        } catch (IOException e) {
            System.err.println("Failed to get page count: " + e.getMessage());
            return -1;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    System.err.println("Failed to close document: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Validates if a file is a valid PDF.
     * 
     * @param file File to validate
     * @return true if valid PDF, false otherwise
     */
    public boolean isValidPdf(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return false;
        }
        
        PDDocument document = null;
        try {
            document = Loader.loadPDF(file);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
}
