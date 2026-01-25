package com.pdftoolkit.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.multipdf.Splitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for splitting PDF files into multiple documents.
 * Uses Apache PDFBox for PDF operations.
 */
public class PdfSplitService {
    
    /**
     * Represents a page range for splitting.
     */
    public record PageRange(int startPage, int endPage) {
        public PageRange {
            if (startPage < 1) {
                throw new IllegalArgumentException("Start page must be >= 1");
            }
            if (endPage < startPage) {
                throw new IllegalArgumentException("End page must be >= start page");
            }
        }
        
        public int getPageCount() {
            return endPage - startPage + 1;
        }
    }
    
    /**
     * Splits a PDF file into multiple files based on page ranges.
     * 
     * @param inputFile Source PDF file
     * @param ranges List of page ranges to extract
     * @param outputDir Directory for output files
     * @param baseFileName Base name for output files (will append _range_01, _range_02, etc.)
     * @return List of created output files
     * @throws IOException if PDF operations fail
     */
    public List<File> splitPdfByRanges(File inputFile, List<PageRange> ranges, File outputDir, String baseFileName) throws IOException {
        if (inputFile == null || !inputFile.exists()) {
            throw new IOException("Input file does not exist: " + inputFile);
        }
        
        if (ranges == null || ranges.isEmpty()) {
            throw new IllegalArgumentException("At least one page range is required");
        }
        
        if (outputDir == null || !outputDir.exists()) {
            throw new IOException("Output directory does not exist: " + outputDir);
        }
        
        if (baseFileName == null || baseFileName.trim().isEmpty()) {
            baseFileName = inputFile.getName().replace(".pdf", "");
        }
        
        List<File> outputFiles = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(inputFile)) {
            int totalPages = document.getNumberOfPages();
            
            // Validate ranges
            for (PageRange range : ranges) {
                if (range.endPage() > totalPages) {
                    throw new IllegalArgumentException(
                        String.format("Range %d-%d exceeds document pages (total: %d)", 
                            range.startPage(), range.endPage(), totalPages)
                    );
                }
            }
            
            // Split for each range
            int rangeIndex = 1;
            for (PageRange range : ranges) {
                PDDocument rangeDoc = new PDDocument();
                
                // Copy pages for this range (PDFBox uses 0-based index)
                for (int i = range.startPage() - 1; i < range.endPage(); i++) {
                    rangeDoc.addPage(document.getPage(i));
                }
                
                // Save output file
                String outputFileName = String.format("%s_range_%02d.pdf", baseFileName, rangeIndex++);
                File outputFile = new File(outputDir, outputFileName);
                rangeDoc.save(outputFile);
                rangeDoc.close();
                
                outputFiles.add(outputFile);
            }
        }
        
        return outputFiles;
    }
    
    /**
     * Splits PDF into separate files, one page per file.
     * 
     * @param inputFile Source PDF
     * @param outputDir Output directory
     * @param baseFileName Base name for output files
     * @return List of created files
     * @throws IOException if operations fail
     */
    public List<File> splitPdfByPages(File inputFile, File outputDir, String baseFileName) throws IOException {
        if (baseFileName == null || baseFileName.trim().isEmpty()) {
            baseFileName = inputFile.getName().replace(".pdf", "");
        }
        
        List<File> outputFiles = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(inputFile)) {
            Splitter splitter = new Splitter();
            splitter.setSplitAtPage(1); // One page per document
            
            List<PDDocument> splitDocs = splitter.split(document);
            
            int pageNum = 1;
            for (PDDocument pageDoc : splitDocs) {
                String outputFileName = String.format("%s_page_%03d.pdf", baseFileName, pageNum++);
                File outputFile = new File(outputDir, outputFileName);
                pageDoc.save(outputFile);
                pageDoc.close();
                outputFiles.add(outputFile);
            }
        }
        
        return outputFiles;
    }
    
    /**
     * Splits PDF with Path objects.
     * 
     * @param inputPath Input PDF path
     * @param ranges Page ranges
     * @param outputDirPath Output directory path
     * @param baseFileName Base output filename
     * @return List of created file paths
     * @throws IOException if split fails
     */
    public List<Path> splitPdfByRanges(Path inputPath, List<PageRange> ranges, Path outputDirPath, String baseFileName) throws IOException {
        List<File> files = splitPdfByRanges(inputPath.toFile(), ranges, outputDirPath.toFile(), baseFileName);
        return files.stream().map(File::toPath).toList();
    }
    
    /**
     * Extracts specific pages from a PDF and creates individual PDFs for each page.
     * 
     * @param inputFile Source PDF file
     * @param pageNumbers List of page numbers to extract (1-indexed)
     * @param outputDir Directory for output files
     * @param baseFileName Base name for output files
     * @return List of created output files
     * @throws IOException if PDF operations fail
     */
    public List<File> extractPages(File inputFile, List<Integer> pageNumbers, File outputDir, String baseFileName) throws IOException {
        if (inputFile == null || !inputFile.exists()) {
            throw new IOException("Input file does not exist: " + inputFile);
        }
        
        if (pageNumbers == null || pageNumbers.isEmpty()) {
            throw new IllegalArgumentException("At least one page number is required");
        }
        
        if (outputDir == null || !outputDir.exists()) {
            throw new IOException("Output directory does not exist: " + outputDir);
        }
        
        if (baseFileName == null || baseFileName.trim().isEmpty()) {
            baseFileName = inputFile.getName().replace(".pdf", "");
        }
        
        List<File> outputFiles = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(inputFile)) {
            int totalPages = document.getNumberOfPages();
            
            // Validate page numbers
            for (int pageNum : pageNumbers) {
                if (pageNum < 1 || pageNum > totalPages) {
                    throw new IllegalArgumentException(
                        String.format("Page %d is out of bounds (total: %d)", pageNum, totalPages)
                    );
                }
            }
            
            // Extract each page
            for (int pageNum : pageNumbers) {
                PDDocument pageDoc = new PDDocument();
                
                // Copy the specific page (PDFBox uses 0-based index)
                pageDoc.addPage(document.getPage(pageNum - 1));
                
                // Save output file
                String outputFileName = String.format("%s_page_%03d.pdf", baseFileName, pageNum);
                File outputFile = new File(outputDir, outputFileName);
                pageDoc.save(outputFile);
                pageDoc.close();
                
                outputFiles.add(outputFile);
            }
        }
        
        return outputFiles;
    }
}
