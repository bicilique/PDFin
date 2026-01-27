package com.pdftoolkit.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.File;
import java.io.IOException;

/**
 * Service for encrypting/locking PDF files with password protection.
 * Uses Apache PDFBox with AES encryption for strong security.
 */
public class PdfLockService {
    
    // Encryption key lengths
    private static final int AES_256_KEY_LENGTH = 256;
    private static final int AES_128_KEY_LENGTH = 128;
    
    /**
     * Result of a lock operation
     */
    public static class LockResult {
        private final boolean success;
        private final String errorMessage;
        private final File outputFile;
        
        private LockResult(boolean success, String errorMessage, File outputFile) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.outputFile = outputFile;
        }
        
        public static LockResult success(File outputFile) {
            return new LockResult(true, null, outputFile);
        }
        
        public static LockResult failure(String errorMessage) {
            return new LockResult(false, errorMessage, null);
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
    }
    
    /**
     * Locks a PDF file with password encryption.
     * 
     * @param inputFile Source PDF file
     * @param outputFile Destination file for locked PDF
     * @param password Password to encrypt with (used as both user and owner password)
     * @return LockResult with success status and details
     */
    public LockResult lockPdf(File inputFile, File outputFile, String password) {
        // Validate inputs
        if (inputFile == null || !inputFile.exists()) {
            return LockResult.failure("Input file does not exist");
        }
        
        if (!inputFile.canRead()) {
            return LockResult.failure("Cannot read input file");
        }
        
        if (outputFile == null) {
            return LockResult.failure("Output file not specified");
        }
        
        if (password == null || password.isEmpty()) {
            return LockResult.failure("Password cannot be empty");
        }
        
        // Ensure output directory exists
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                return LockResult.failure("Failed to create output directory");
            }
        }
        
        PDDocument document = null;
        try {
            // Load PDF document
            document = Loader.loadPDF(inputFile);
            
            // Create access permissions (full permissions - password only controls opening)
            AccessPermission accessPermission = new AccessPermission();
            accessPermission.setCanPrint(true);
            accessPermission.setCanModify(true);
            accessPermission.setCanExtractContent(true);
            accessPermission.setCanModifyAnnotations(true);
            accessPermission.setCanFillInForm(true);
            accessPermission.setCanExtractForAccessibility(true);
            accessPermission.setCanAssembleDocument(true);
            
            // Create protection policy with AES encryption
            StandardProtectionPolicy policy;
            try {
                // Try AES-256 first (strongest)
                policy = new StandardProtectionPolicy(password, password, accessPermission);
                policy.setEncryptionKeyLength(AES_256_KEY_LENGTH);
                policy.setPreferAES(true);
            } catch (Exception e) {
                // Fallback to AES-128
                policy = new StandardProtectionPolicy(password, password, accessPermission);
                policy.setEncryptionKeyLength(AES_128_KEY_LENGTH);
                policy.setPreferAES(true);
            }
            
            // Apply protection
            document.protect(policy);
            
            // Save encrypted document
            document.save(outputFile);
            
            return LockResult.success(outputFile);
            
        } catch (IOException e) {
            return LockResult.failure("Failed to lock PDF: " + e.getMessage());
        } catch (Exception e) {
            return LockResult.failure("Unexpected error: " + e.getMessage());
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
     * Gets the page count of a PDF file.
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
            return -1;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
    
    /**
     * Validates if a file is a readable PDF.
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
                    // Ignore
                }
            }
        }
    }
}
