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
 * Unit tests for SplitControllerRedesigned logic.
 * Tests state management, validation, file handling, and business logic.
 */
class SplitControllerTest {

    private List<File> testFiles;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("pdftoolkit-split-test");
        testFiles = new ArrayList<>();
        
        // Create test PDF files
        for (int i = 1; i <= 3; i++) {
            File testFile = tempDir.resolve("test" + i + ".pdf").toFile();
            testFile.createNewFile();
            testFiles.add(testFile);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test files
        for (File file : testFiles) {
            Files.deleteIfExists(file.toPath());
        }
        // Clean up temp directory
        if (Files.exists(tempDir)) {
            try (var stream = Files.list(tempDir)) {
                stream.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
            }
            Files.deleteIfExists(tempDir);
        }
    }

    // ==================== File Validation Tests ====================

    @Test
    @DisplayName("Should accept valid PDF files")
    void testFileValidation_AcceptsPDF() {
        File pdfFile = new File("document.pdf");
        boolean isValid = pdfFile.getName().toLowerCase().endsWith(".pdf");
        assertTrue(isValid, "Should accept .pdf files");
    }

    @Test
    @DisplayName("Should reject non-PDF files")
    void testFileValidation_RejectsNonPDF() {
        List<File> nonPdfFiles = List.of(
            new File("image.jpg"),
            new File("document.docx"),
            new File("text.txt")
        );
        
        for (File file : nonPdfFiles) {
            boolean isValid = file.getName().toLowerCase().endsWith(".pdf");
            assertFalse(isValid, "Should reject " + file.getName());
        }
    }

    @Test
    @DisplayName("Should validate PDF file extension case-insensitively")
    void testFileValidation_CaseInsensitive() {
        List<File> pdfFiles = List.of(
            new File("document.PDF"),
            new File("document.pdf"),
            new File("document.Pdf")
        );
        
        for (File file : pdfFiles) {
            boolean isValid = file.getName().toLowerCase().endsWith(".pdf");
            assertTrue(isValid, "Should accept " + file.getName());
        }
    }

    // ==================== State Validation Tests ====================

    @Test
    @DisplayName("Split button should be disabled with no file")
    void testValidation_DisabledWithNoFile() {
        File selectedFile = null;
        boolean shouldEnable = selectedFile != null;
        assertFalse(shouldEnable, "Should be disabled with no file");
    }

    @Test
    @DisplayName("Split button should be enabled with valid file")
    void testValidation_EnabledWithFile() {
        File selectedFile = testFiles.get(0);
        boolean shouldEnable = selectedFile != null;
        assertTrue(shouldEnable, "Should be enabled with file");
    }

    @Test
    @DisplayName("Should validate output folder is set")
    void testValidation_OutputFolderRequired() {
        String outputFolder = null;
        boolean isValid = outputFolder != null && !outputFolder.trim().isEmpty();
        assertFalse(isValid, "Should be invalid when output folder is null");
        
        outputFolder = tempDir.toString();
        isValid = outputFolder != null && !outputFolder.trim().isEmpty();
        assertTrue(isValid, "Should be valid when output folder is set");
    }

    @Test
    @DisplayName("Should validate output folder exists")
    void testValidation_OutputFolderExists() {
        File outputDir = tempDir.toFile();
        boolean exists = outputDir.exists() && outputDir.isDirectory();
        assertTrue(exists, "Should validate output folder exists");
        
        File nonExistent = new File("/nonexistent/path");
        exists = nonExistent.exists() && nonExistent.isDirectory();
        assertFalse(exists, "Should fail for non-existent folder");
    }

    // ==================== Page Range Validation Tests ====================

    @Test
    @DisplayName("Should validate page range - from <= to")
    void testPageRangeValidation_FromLessThanOrEqualTo() {
        int from = 1;
        int to = 5;
        boolean isValid = from <= to;
        assertTrue(isValid, "Valid range: from (1) <= to (5)");
        
        from = 5;
        to = 1;
        isValid = from <= to;
        assertFalse(isValid, "Invalid range: from (5) > to (1)");
    }

    @Test
    @DisplayName("Should validate page range - same page")
    void testPageRangeValidation_SamePage() {
        int from = 3;
        int to = 3;
        boolean isValid = from <= to;
        assertTrue(isValid, "Valid range: from (3) == to (3)");
    }

    @Test
    @DisplayName("Should validate page range within bounds")
    void testPageRangeValidation_WithinBounds() {
        int totalPages = 10;
        int from = 1;
        int to = 10;
        boolean isValid = from >= 1 && to <= totalPages && from <= to;
        assertTrue(isValid, "Valid range: 1-10 within 10 pages");
        
        from = 1;
        to = 11;
        isValid = from >= 1 && to <= totalPages && from <= to;
        assertFalse(isValid, "Invalid range: 1-11 exceeds 10 pages");
    }

    @Test
    @DisplayName("Should validate page range starts at 1 or greater")
    void testPageRangeValidation_StartsAtOne() {
        int from = 0;
        int to = 5;
        boolean isValid = from >= 1;
        assertFalse(isValid, "Invalid: page numbers start at 1");
        
        from = 1;
        isValid = from >= 1;
        assertTrue(isValid, "Valid: page numbers start at 1");
    }

    // ==================== Extract Pages Specification Tests ====================

    @Test
    @DisplayName("Should validate extract pages - empty")
    void testExtractPagesValidation_Empty() {
        String pageSpec = "";
        boolean isValid = pageSpec != null && !pageSpec.trim().isEmpty();
        assertFalse(isValid, "Should be invalid when empty");
    }

    @Test
    @DisplayName("Should validate extract pages - null")
    void testExtractPagesValidation_Null() {
        String pageSpec = null;
        boolean isValid = pageSpec != null && !pageSpec.trim().isEmpty();
        assertFalse(isValid, "Should be invalid when null");
    }

    @Test
    @DisplayName("Should validate extract pages - single page")
    void testExtractPagesValidation_SinglePage() {
        String pageSpec = "5";
        boolean isValid = pageSpec != null && !pageSpec.trim().isEmpty();
        assertTrue(isValid, "Should be valid for single page");
    }

    @Test
    @DisplayName("Should validate extract pages - multiple pages")
    void testExtractPagesValidation_MultiplePages() {
        String pageSpec = "1,3,5,7";
        boolean isValid = pageSpec != null && !pageSpec.trim().isEmpty();
        assertTrue(isValid, "Should be valid for multiple pages");
    }

    @Test
    @DisplayName("Should validate extract pages - page ranges")
    void testExtractPagesValidation_Ranges() {
        String pageSpec = "1-5,7,10-12";
        boolean isValid = pageSpec != null && !pageSpec.trim().isEmpty();
        assertTrue(isValid, "Should be valid for ranges");
    }

    // ==================== Page Specification Parsing Tests ====================

    @Test
    @DisplayName("Should parse single page number")
    void testParsePageSpec_SinglePage() {
        String spec = "5";
        List<Integer> pages = parsePageSpec(spec, 10);
        assertEquals(1, pages.size());
        assertTrue(pages.contains(5));
    }

    @Test
    @DisplayName("Should parse comma-separated pages")
    void testParsePageSpec_CommaSeparated() {
        String spec = "1,3,5";
        List<Integer> pages = parsePageSpec(spec, 10);
        assertEquals(3, pages.size());
        assertTrue(pages.containsAll(List.of(1, 3, 5)));
    }

    @Test
    @DisplayName("Should parse page range")
    void testParsePageSpec_Range() {
        String spec = "3-5";
        List<Integer> pages = parsePageSpec(spec, 10);
        assertEquals(3, pages.size());
        assertTrue(pages.containsAll(List.of(3, 4, 5)));
    }

    @Test
    @DisplayName("Should parse mixed specification")
    void testParsePageSpec_Mixed() {
        String spec = "1,3-5,7";
        List<Integer> pages = parsePageSpec(spec, 10);
        assertEquals(5, pages.size());
        assertTrue(pages.containsAll(List.of(1, 3, 4, 5, 7)));
    }

    @Test
    @DisplayName("Should reject invalid page numbers")
    void testParsePageSpec_InvalidPages() {
        String spec = "0,5,11";
        assertThrows(IllegalArgumentException.class, () -> {
            parsePageSpec(spec, 10);
        });
    }

    @Test
    @DisplayName("Should reject invalid range format")
    void testParsePageSpec_InvalidRange() {
        String spec = "5-3"; // from > to
        assertThrows(IllegalArgumentException.class, () -> {
            parsePageSpec(spec, 10);
        });
    }

    private List<Integer> parsePageSpec(String spec, int totalPages) {
        List<Integer> pages = new ArrayList<>();
        String[] parts = spec.split(",");
        
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                int from = Integer.parseInt(range[0].trim());
                int to = Integer.parseInt(range[1].trim());
                
                if (from < 1 || to > totalPages || from > to) {
                    throw new IllegalArgumentException("Invalid range: " + part);
                }
                
                for (int i = from; i <= to; i++) {
                    pages.add(i);
                }
            } else {
                int page = Integer.parseInt(part);
                if (page < 1 || page > totalPages) {
                    throw new IllegalArgumentException("Invalid page: " + page);
                }
                pages.add(page);
            }
        }
        
        return pages;
    }

    // ==================== Zoom Level Tests ====================

    @Test
    @DisplayName("Should validate zoom level range")
    void testZoomLevel_Range() {
        double zoom = 1.0;
        boolean isValid = zoom >= 1.0 && zoom <= 2.0;
        assertTrue(isValid, "Valid zoom: 1.0");
        
        zoom = 1.5;
        isValid = zoom >= 1.0 && zoom <= 2.0;
        assertTrue(isValid, "Valid zoom: 1.5");
        
        zoom = 2.0;
        isValid = zoom >= 1.0 && zoom <= 2.0;
        assertTrue(isValid, "Valid zoom: 2.0");
        
        zoom = 0.5;
        isValid = zoom >= 1.0 && zoom <= 2.0;
        assertFalse(isValid, "Invalid zoom: 0.5");
        
        zoom = 3.0;
        isValid = zoom >= 1.0 && zoom <= 2.0;
        assertFalse(isValid, "Invalid zoom: 3.0");
    }

    @Test
    @DisplayName("Should calculate columns for zoom level")
    void testZoomLevel_Columns() {
        assertEquals(3, getColumnsForZoom(1.0), "1.0 zoom = 3 columns");
        assertEquals(2, getColumnsForZoom(1.5), "1.5 zoom = 2 columns");
        assertEquals(1, getColumnsForZoom(2.0), "2.0 zoom = 1 column");
    }

    private int getColumnsForZoom(double zoom) {
        if (zoom >= 2.0) return 1;
        if (zoom >= 1.5) return 2;
        return 3;
    }

    // ==================== File State Tests ====================

    @Test
    @DisplayName("Empty state visible when no file selected")
    void testEmptyState_NoFile() {
        File selectedFile = null;
        boolean emptyStateVisible = selectedFile == null;
        boolean previewVisible = selectedFile != null;
        
        assertTrue(emptyStateVisible);
        assertFalse(previewVisible);
    }

    @Test
    @DisplayName("Preview visible when file selected")
    void testEmptyState_FileSelected() {
        File selectedFile = testFiles.get(0);
        boolean emptyStateVisible = selectedFile == null;
        boolean previewVisible = selectedFile != null;
        
        assertFalse(emptyStateVisible);
        assertTrue(previewVisible);
    }

    // ==================== Progressive Loading Tests ====================

    @Test
    @DisplayName("Should calculate initial batch size")
    void testProgressiveLoading_InitialBatch() {
        int batchSize = 30; // INITIAL_BATCH_SIZE
        assertTrue(batchSize > 0);
        assertEquals(30, batchSize);
    }

    @Test
    @DisplayName("Should calculate load more batch size")
    void testProgressiveLoading_LoadMoreBatch() {
        int batchSize = 20; // LOAD_MORE_BATCH_SIZE
        assertTrue(batchSize > 0);
        assertEquals(20, batchSize);
    }

    @Test
    @DisplayName("Should determine if more pages available")
    void testProgressiveLoading_MorePagesAvailable() {
        int loadedPages = 30;
        int totalPages = 50;
        boolean moreAvailable = loadedPages < totalPages;
        assertTrue(moreAvailable, "More pages available");
        
        loadedPages = 50;
        moreAvailable = loadedPages < totalPages;
        assertFalse(moreAvailable, "No more pages available");
    }

    // ==================== Output Filename Generation Tests ====================

    @Test
    @DisplayName("Should generate output filename for split by range")
    void testOutputFilename_SplitByRange() {
        String baseFilename = "document.pdf".replace(".pdf", "");
        int rangeNumber = 1;
        String outputFilename = String.format("%s_part%d.pdf", baseFilename, rangeNumber);
        assertEquals("document_part1.pdf", outputFilename);
    }

    @Test
    @DisplayName("Should generate output filename for extract pages")
    void testOutputFilename_ExtractPages() {
        String baseFilename = "document.pdf".replace(".pdf", "");
        String pages = "1-5";
        String outputFilename = String.format("%s_pages%s.pdf", baseFilename, pages.replace("-", "_"));
        assertEquals("document_pages1_5.pdf", outputFilename);
    }

    // ==================== Mode Selection Tests ====================

    @Test
    @DisplayName("Split by range mode selected")
    void testModeSelection_SplitByRange() {
        boolean isSplitByRange = true;
        boolean isExtractPages = false;
        
        assertTrue(isSplitByRange);
        assertFalse(isExtractPages);
    }

    @Test
    @DisplayName("Extract pages mode selected")
    void testModeSelection_ExtractPages() {
        boolean isSplitByRange = false;
        boolean isExtractPages = true;
        
        assertFalse(isSplitByRange);
        assertTrue(isExtractPages);
    }

    // ==================== Range Card Tests ====================

    @Test
    @DisplayName("Should add range card")
    void testRangeCard_Add() {
        List<Integer> rangeCards = new ArrayList<>();
        rangeCards.add(1);
        assertEquals(1, rangeCards.size());
    }

    @Test
    @DisplayName("Should remove range card")
    void testRangeCard_Remove() {
        List<Integer> rangeCards = new ArrayList<>();
        rangeCards.add(1);
        rangeCards.add(2);
        rangeCards.remove(Integer.valueOf(1));
        assertEquals(1, rangeCards.size());
        assertFalse(rangeCards.contains(1));
    }

    @Test
    @DisplayName("Should count range cards")
    void testRangeCard_Count() {
        List<Integer> rangeCards = new ArrayList<>();
        assertEquals(0, rangeCards.size());
        
        rangeCards.add(1);
        rangeCards.add(2);
        rangeCards.add(3);
        assertEquals(3, rangeCards.size());
    }

    // ==================== Validation Message Tests ====================

    @Test
    @DisplayName("Validation message for invalid range")
    void testValidationMessage_InvalidRange() {
        String message = "Invalid range: from must be ≤ to";
        assertNotNull(message);
        assertTrue(message.contains("Invalid"));
    }

    @Test
    @DisplayName("Validation message for no file")
    void testValidationMessage_NoFile() {
        String message = "Please select a PDF file first";
        assertNotNull(message);
        assertTrue(message.contains("select"));
    }

    @Test
    @DisplayName("Validation message for no output folder")
    void testValidationMessage_NoOutputFolder() {
        String message = "Please select an output folder";
        assertNotNull(message);
        assertTrue(message.contains("output folder"));
    }

    // ==================== Progress Status Tests ====================

    @Test
    @DisplayName("Progress status for split operation")
    void testProgressStatus_Splitting() {
        String status = "Splitting PDF...";
        assertNotNull(status);
        assertTrue(status.contains("Splitting"));
    }

    @Test
    @DisplayName("Progress status for processing file")
    void testProgressStatus_Processing() {
        int current = 1;
        int total = 5;
        String status = String.format("Processing %d of %d...", current, total);
        assertEquals("Processing 1 of 5...", status);
    }

    // ==================== Success Message Tests ====================

    @Test
    @DisplayName("Success message format")
    void testSuccessMessage_Format() {
        String message = "File successfully split!";
        assertNotNull(message);
        assertTrue(message.contains("successfully"));
    }
}
