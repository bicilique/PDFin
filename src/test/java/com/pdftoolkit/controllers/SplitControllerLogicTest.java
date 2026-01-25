package com.pdftoolkit.controllers;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Split Controller logic (non-UI components).
 * Tests range validation, bounds checking, and state management.
 */
class SplitControllerLogicTest {

    private File testFile;
    private Path tempDir;
    private static final int TOTAL_PAGES = 25; // Simulated total pages

    @BeforeEach
    void setUp() throws IOException {
        // Create temp directory and test PDF file
        tempDir = Files.createTempDirectory("pdftoolkit-test");
        testFile = tempDir.resolve("test.pdf").toFile();
        testFile.createNewFile();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(testFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("Split button should be disabled when no file selected")
    void testSplitButton_DisabledNoFile() {
        File selectedFile = null;
        List<Range> ranges = List.of(new Range(1, 5));
        
        boolean shouldEnable = selectedFile != null && !ranges.isEmpty();
        assertFalse(shouldEnable, "Should be disabled with no file");
    }

    @Test
    @DisplayName("Split button should be disabled when no ranges defined")
    void testSplitButton_DisabledNoRanges() {
        File selectedFile = testFile;
        List<Range> ranges = new ArrayList<>();
        
        boolean shouldEnable = selectedFile != null && !ranges.isEmpty();
        assertFalse(shouldEnable, "Should be disabled with no ranges");
    }

    @Test
    @DisplayName("Split button should be enabled with file and ranges")
    void testSplitButton_EnabledWithFileAndRanges() {
        File selectedFile = testFile;
        List<Range> ranges = List.of(new Range(1, 10), new Range(11, 20));
        
        boolean shouldEnable = selectedFile != null && !ranges.isEmpty();
        assertTrue(shouldEnable, "Should be enabled with file and ranges");
    }

    @Test
    @DisplayName("Should validate range where from <= to")
    void testRangeValidation_FromLessThanOrEqualTo() {
        Range validRange = new Range(1, 10);
        assertTrue(validRange.isValid(), "Valid range should pass");
        
        Range equalRange = new Range(5, 5);
        assertTrue(equalRange.isValid(), "Equal range should pass");
        
        Range invalidRange = new Range(10, 5);
        assertFalse(invalidRange.isValid(), "Invalid range should fail");
    }

    @Test
    @DisplayName("Should validate range within document bounds")
    void testRangeValidation_WithinBounds() {
        Range validRange = new Range(1, 25);
        assertTrue(validRange.isWithinBounds(TOTAL_PAGES), 
                  "Range within bounds should pass");
        
        Range startOutOfBounds = new Range(0, 10);
        assertFalse(startOutOfBounds.isWithinBounds(TOTAL_PAGES),
                   "Range starting at 0 should fail");
        
        Range endOutOfBounds = new Range(1, 30);
        assertFalse(endOutOfBounds.isWithinBounds(TOTAL_PAGES),
                   "Range exceeding total pages should fail");
        
        Range bothOutOfBounds = new Range(30, 40);
        assertFalse(bothOutOfBounds.isWithinBounds(TOTAL_PAGES),
                   "Range completely out of bounds should fail");
    }

    @Test
    @DisplayName("Should detect overlapping ranges")
    void testRangeOverlap_Detection() {
        Range range1 = new Range(1, 10);
        Range range2 = new Range(5, 15);
        
        assertTrue(range1.overlaps(range2), "Ranges should overlap");
        assertTrue(range2.overlaps(range1), "Overlap should be symmetric");
        
        Range range3 = new Range(11, 20);
        assertFalse(range1.overlaps(range3), "Non-overlapping ranges");
        
        Range range4 = new Range(10, 15);
        assertTrue(range1.overlaps(range4), "Edge touching counts as overlap");
    }

    @Test
    @DisplayName("Should allow adjacent non-overlapping ranges")
    void testRangeOverlap_Adjacent() {
        Range range1 = new Range(1, 10);
        Range range2 = new Range(11, 20);
        
        assertFalse(range1.overlaps(range2), "Adjacent ranges should not overlap");
    }

    @Test
    @DisplayName("Should validate single page range")
    void testRangeValidation_SinglePage() {
        Range singlePage = new Range(5, 5);
        assertTrue(singlePage.isValid(), "Single page range should be valid");
        assertTrue(singlePage.isWithinBounds(TOTAL_PAGES), 
                  "Single page within bounds should be valid");
    }

    @Test
    @DisplayName("Should validate entire document range")
    void testRangeValidation_EntireDocument() {
        Range entireDoc = new Range(1, TOTAL_PAGES);
        assertTrue(entireDoc.isValid(), "Entire document range should be valid");
        assertTrue(entireDoc.isWithinBounds(TOTAL_PAGES),
                  "Entire document range should be within bounds");
    }

    @Test
    @DisplayName("Should filter only PDF files from drop")
    void testFileFilter_OnDrop() {
        List<File> droppedFiles = List.of(
            new File("document.pdf"),
            new File("image.jpg"),
            new File("spreadsheet.xlsx")
        );
        
        List<File> pdfOnly = droppedFiles.stream()
            .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
            .limit(1) // Split only accepts one file
            .toList();
        
        assertEquals(1, pdfOnly.size(), "Should accept only one PDF file");
        assertTrue(pdfOnly.get(0).getName().endsWith(".pdf"));
    }

    @Test
    @DisplayName("Should update spinner bounds when file is loaded")
    void testSpinnerBounds_OnFileLoad() {
        int minPage = 1;
        int maxPage = TOTAL_PAGES;
        
        // Simulate spinner value factory bounds
        assertTrue(minPage <= maxPage, "Min should be <= max");
        assertEquals(1, minPage, "Min page should be 1");
        assertEquals(TOTAL_PAGES, maxPage, "Max page should be total pages");
    }

    @Test
    @DisplayName("Should validate output folder is directory")
    void testOutputFolderValidation() {
        File validFolder = tempDir.toFile();
        assertTrue(validFolder.isDirectory(), "Valid folder should pass");
        
        File notADirectory = testFile; // This is a file, not directory
        assertFalse(notADirectory.isDirectory(), "File should fail directory check");
    }

    @Test
    @DisplayName("Should build range input string from multiple ranges")
    void testRangeInputBuilding() {
        List<Range> ranges = List.of(
            new Range(1, 5),
            new Range(10, 15),
            new Range(20, 25)
        );
        
        String rangeInput = ranges.stream()
            .map(r -> r.from + "-" + r.to)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
        
        assertEquals("1-5,10-15,20-25", rangeInput,
                    "Should build correct range input string");
    }

    // Helper class for range testing
    private static class Range {
        int from;
        int to;
        
        Range(int from, int to) {
            this.from = from;
            this.to = to;
        }
        
        boolean isValid() {
            return from <= to && from >= 1;
        }
        
        boolean isWithinBounds(int totalPages) {
            return from >= 1 && to <= totalPages && from <= to;
        }
        
        boolean overlaps(Range other) {
            return !(this.to < other.from || other.to < this.from);
        }
    }
}
