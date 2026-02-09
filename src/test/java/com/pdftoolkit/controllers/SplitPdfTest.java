package com.pdftoolkit.controllers;

import com.pdftoolkit.services.PdfSplitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Split PDF functionality.
 * Tests range validation and split service operations.
 */
class SplitPdfTest {
    
    private PdfSplitService splitService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        splitService = new PdfSplitService();
    }
    
    @Test
    void testPageRangeValidation() {
        // Test valid range
        assertDoesNotThrow(() -> new PdfSplitService.PageRange(1, 5));
        
        // Test single page range
        assertDoesNotThrow(() -> new PdfSplitService.PageRange(1, 1));
        
        // Test invalid range: start < 1
        assertThrows(IllegalArgumentException.class, () -> new PdfSplitService.PageRange(0, 5));
        
        // Test invalid range: end < start
        assertThrows(IllegalArgumentException.class, () -> new PdfSplitService.PageRange(5, 3));
    }
    
    @Test
    void testPageRangePageCount() {
        PdfSplitService.PageRange range1 = new PdfSplitService.PageRange(1, 5);
        assertEquals(5, range1.getPageCount());
        
        PdfSplitService.PageRange range2 = new PdfSplitService.PageRange(3, 3);
        assertEquals(1, range2.getPageCount());
        
        PdfSplitService.PageRange range3 = new PdfSplitService.PageRange(10, 20);
        assertEquals(11, range3.getPageCount());
    }
    
    @Test
    void testSplitServiceValidation() {
        File nonExistentFile = new File("nonexistent.pdf");
        File outputDir = tempDir.toFile();
        
        List<PdfSplitService.PageRange> ranges = List.of(
            new PdfSplitService.PageRange(1, 2)
        );
        
        // Should throw IOException for non-existent file
        assertThrows(Exception.class, () -> 
            splitService.splitPdfByRanges(nonExistentFile, ranges, outputDir, "test")
        );
    }
    
    @Test
    void testEmptyRangesValidation() throws Exception {
        File outputDir = tempDir.toFile();
        File dummyFile = new File(outputDir, "dummy.pdf");
        
        // Create a dummy file so file existence check passes
        Files.createFile(dummyFile.toPath());
        
        // Empty ranges should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> 
            splitService.splitPdfByRanges(dummyFile, List.of(), outputDir, "test")
        );
        
        // Null ranges should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> 
            splitService.splitPdfByRanges(dummyFile, null, outputDir, "test")
        );
    }
    
    @Test
    void testOutputDirectoryValidation() {
        File nonExistentDir = new File(tempDir.toFile(), "nonexistent");
        File dummyFile = new File(tempDir.toFile(), "dummy.pdf");
        
        List<PdfSplitService.PageRange> ranges = List.of(
            new PdfSplitService.PageRange(1, 2)
        );
        
        // Should throw IOException for non-existent output directory
        assertThrows(Exception.class, () -> 
            splitService.splitPdfByRanges(dummyFile, ranges, nonExistentDir, "test")
        );
    }
    
    /**
     * Test that button state logic would be correct.
     * Simulates controller validation logic.
     */
    @Test
    void testSplitButtonStateLogic() {
        // Simulate controller state
        File selectedFile = null;
        int rangeCount = 0;
        String outputPath = "";
        
        // No file, no ranges - button should be disabled
        assertFalse(isValidState(selectedFile, rangeCount, outputPath));
        
        // File selected, no ranges - button should be disabled
        selectedFile = new File("test.pdf");
        assertFalse(isValidState(selectedFile, rangeCount, outputPath));
        
        // File selected, has ranges, no output path - button should be disabled
        rangeCount = 1;
        assertFalse(isValidState(selectedFile, rangeCount, outputPath));
        
        // File selected, has ranges, has output path - button should be enabled
        outputPath = "/tmp/output";
        assertTrue(isValidState(selectedFile, rangeCount, outputPath));
        
        // File selected, no ranges, has output path - button should be disabled
        rangeCount = 0;
        assertFalse(isValidState(selectedFile, rangeCount, outputPath));
    }
    
    /**
     * Simulates controller validation logic
     */
    private boolean isValidState(File file, int rangeCount, String outputPath) {
        if (file == null) return false;
        if (rangeCount == 0) return false;
        if (outputPath == null || outputPath.isEmpty()) return false;
        return true;
    }
    
    @Test
    void testRangeValidationLogic() {
        int totalPages = 10;
        
        // Valid range
        assertTrue(isValidRange(1, 5, totalPages));
        assertTrue(isValidRange(1, 10, totalPages));
        assertTrue(isValidRange(5, 5, totalPages));
        
        // Invalid: from > to
        assertFalse(isValidRange(5, 3, totalPages));
        
        // Invalid: exceeds total pages
        assertFalse(isValidRange(1, 11, totalPages));
        assertFalse(isValidRange(11, 12, totalPages));
    }
    
    private boolean isValidRange(int from, int to, int totalPages) {
        if (from > to) return false;
        if (from > totalPages || to > totalPages) return false;
        if (from < 1) return false;
        return true;
    }
}
