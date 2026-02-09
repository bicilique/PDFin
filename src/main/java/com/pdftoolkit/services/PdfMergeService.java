package com.pdftoolkit.services;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service for merging multiple PDF files into a single PDF.
 * Uses Apache PDFBox for PDF operations.
 */
public class PdfMergeService {
    
    /**
     * Merges multiple PDF files into a single output file.
     * 
     * @param inputFiles List of PDF files to merge (in order)
     * @param outputFile Target output file path
     * @throws IOException if PDF reading/writing fails
     * @throws IllegalArgumentException if inputs are invalid
     */
    public void mergePdfs(List<File> inputFiles, File outputFile) throws IOException {
        if (inputFiles == null || inputFiles.isEmpty()) {
            throw new IllegalArgumentException("Input files list cannot be null or empty");
        }
        
        if (inputFiles.size() < 2) {
            throw new IllegalArgumentException("At least 2 PDF files are required for merging");
        }
        
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }
        
        // Validate all input files exist and are readable
        for (File file : inputFiles) {
            if (!file.exists() || !file.canRead()) {
                throw new IOException("Cannot read file: " + file.getAbsolutePath());
            }
        }
        
        // Ensure output directory exists
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
            }
        }
        
        // Perform merge using PDFBox
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(outputFile.getAbsolutePath());
        
        for (File file : inputFiles) {
            merger.addSource(file);
        }
        
        // Merge documents
        merger.mergeDocuments(null); // Use default memory settings
    }
    
    /**
     * Merges PDFs with Path objects.
     * 
     * @param inputPaths List of input PDF paths
     * @param outputPath Output PDF path
     * @throws IOException if merge fails
     */
    public void mergePdfs(List<Path> inputPaths, Path outputPath) throws IOException {
        List<File> inputFiles = inputPaths.stream().map(Path::toFile).toList();
        mergePdfs(inputFiles, outputPath.toFile());
    }
}
