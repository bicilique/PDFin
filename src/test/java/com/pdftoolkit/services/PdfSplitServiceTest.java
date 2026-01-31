package com.pdftoolkit.services;

import com.pdftoolkit.services.PdfSplitService.PageRange;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for PdfSplitService.
 * Achieves 100% code coverage for PDF splitting operations.
 */
class PdfSplitServiceTest {

    private PdfSplitService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new PdfSplitService();
    }

    private File createTestPdf(String filename, int pages) throws IOException {
        File pdfFile = tempDir.resolve(filename).toFile();
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            doc.save(pdfFile);
        }
        return pdfFile;
    }

    @Test
    @DisplayName("Test PageRange creation with valid values")
    void testPageRange_Valid() {
        // When
        PageRange range = new PageRange(1, 5);

        // Then
        assertEquals(1, range.startPage());
        assertEquals(5, range.endPage());
        assertEquals(5, range.getPageCount());
    }

    @Test
    @DisplayName("Test PageRange with start page less than 1")
    void testPageRange_InvalidStartPage() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PageRange(0, 5)
        );
        assertEquals("Start page must be >= 1", exception.getMessage());
    }

    @Test
    @DisplayName("Test PageRange with end page less than start page")
    void testPageRange_InvalidEndPage() {
        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PageRange(5, 3)
        );
        assertEquals("End page must be >= start page", exception.getMessage());
    }

    @Test
    @DisplayName("Test PageRange with single page")
    void testPageRange_SinglePage() {
        // When
        PageRange range = new PageRange(3, 3);

        // Then
        assertEquals(1, range.getPageCount());
    }

    @Test
    @DisplayName("Test split PDF by single range")
    void testSplitPdfByRanges_SingleRange() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputDir = tempDir.resolve("output").toFile();
        outputDir.mkdirs();

        PageRange range = new PageRange(1, 5);
        List<PageRange> ranges = Collections.singletonList(range);

        // When
        List<File> results = service.splitPdfByRanges(inputFile, ranges, outputDir, "split");

        // Then
        assertEquals(1, results.size());
        assertTrue(results.get(0).exists());
        
        try (PDDocument doc = Loader.loadPDF(results.get(0))) {
            assertEquals(5, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test split PDF by multiple ranges")
    void testSplitPdfByRanges_MultipleRanges() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 20);
        File outputDir = tempDir.resolve("output").toFile();
        outputDir.mkdirs();

        List<PageRange> ranges = Arrays.asList(
            new PageRange(1, 5),
            new PageRange(6, 10),
            new PageRange(11, 20)
        );

        // When
        List<File> results = service.splitPdfByRanges(inputFile, ranges, outputDir, "split");

        // Then
        assertEquals(3, results.size());
        
        try (PDDocument doc1 = Loader.loadPDF(results.get(0))) {
            assertEquals(5, doc1.getNumberOfPages());
        }
        
        try (PDDocument doc2 = Loader.loadPDF(results.get(1))) {
            assertEquals(5, doc2.getNumberOfPages());
        }
        
        try (PDDocument doc3 = Loader.loadPDF(results.get(2))) {
            assertEquals(10, doc3.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test split with null input file")
    void testSplitPdfByRanges_NullInputFile() {
        // Given
        File outputDir = tempDir.toFile();
        List<PageRange> ranges = Collections.singletonList(new PageRange(1, 2));

        // When/Then
        IOException exception = assertThrows(
            IOException.class,
            () -> service.splitPdfByRanges(null, ranges, outputDir, "split")
        );
        assertTrue(exception.getMessage().contains("Input file does not exist"));
    }

    @Test
    @DisplayName("Test split with non-existent input file")
    void testSplitPdfByRanges_NonExistentFile() {
        // Given
        File nonExistent = tempDir.resolve("nonexistent.pdf").toFile();
        File outputDir = tempDir.toFile();
        List<PageRange> ranges = Collections.singletonList(new PageRange(1, 2));

        // When/Then
        IOException exception = assertThrows(
            IOException.class,
            () -> service.splitPdfByRanges(nonExistent, ranges, outputDir, "split")
        );
        assertTrue(exception.getMessage().contains("Input file does not exist"));
    }

    @Test
    @DisplayName("Test split with null ranges")
    void testSplitPdfByRanges_NullRanges() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputDir = tempDir.toFile();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.splitPdfByRanges(inputFile, null, outputDir, "split")
        );
        assertEquals("At least one page range is required", exception.getMessage());
    }

    @Test
    @DisplayName("Test split with empty ranges")
    void testSplitPdfByRanges_EmptyRanges() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputDir = tempDir.toFile();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.splitPdfByRanges(inputFile, Collections.emptyList(), outputDir, "split")
        );
        assertEquals("At least one page range is required", exception.getMessage());
    }

    @Test
    @DisplayName("Test split with null output directory")
    void testSplitPdfByRanges_NullOutputDir() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        List<PageRange> ranges = Collections.singletonList(new PageRange(1, 5));

        // When/Then
        IOException exception = assertThrows(
            IOException.class,
            () -> service.splitPdfByRanges(inputFile, ranges, null, "split")
        );
        assertTrue(exception.getMessage().contains("Output directory does not exist"));
    }

    @Test
    @DisplayName("Test split with non-existent output directory")
    void testSplitPdfByRanges_NonExistentOutputDir() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File nonExistentDir = tempDir.resolve("nonexistent").toFile();
        List<PageRange> ranges = Collections.singletonList(new PageRange(1, 5));

        // When/Then
        IOException exception = assertThrows(
            IOException.class,
            () -> service.splitPdfByRanges(inputFile, ranges, nonExistentDir, "split")
        );
        assertTrue(exception.getMessage().contains("Output directory does not exist"));
    }

    @Test
    @DisplayName("Test split with null base filename")
    void testSplitPdfByRanges_NullBaseFilename() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputDir = tempDir.toFile();
        List<PageRange> ranges = Collections.singletonList(new PageRange(1, 5));

        // When
        List<File> results = service.splitPdfByRanges(inputFile, ranges, outputDir, null);

        // Then
        assertEquals(1, results.size());
        assertTrue(results.get(0).getName().startsWith("input"));
    }

    @Test
    @DisplayName("Test split with empty base filename")
    void testSplitPdfByRanges_EmptyBaseFilename() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputDir = tempDir.toFile();
        List<PageRange> ranges = Collections.singletonList(new PageRange(1, 5));

        // When
        List<File> results = service.splitPdfByRanges(inputFile, ranges, outputDir, "");

        // Then
        assertEquals(1, results.size());
        assertTrue(results.get(0).getName().startsWith("input"));
    }

    @Test
    @DisplayName("Test split with range exceeding document pages")
    void testSplitPdfByRanges_RangeExceedsPages() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 5);
        File outputDir = tempDir.toFile();
        List<PageRange> ranges = Collections.singletonList(new PageRange(1, 10));

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.splitPdfByRanges(inputFile, ranges, outputDir, "split")
        );
        assertTrue(exception.getMessage().contains("exceeds document pages"));
    }

    @Test
    @DisplayName("Test split PDF by pages (one page per file)")
    void testSplitPdfByPages() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 5);
        File outputDir = tempDir.resolve("pages_output").toFile();
        outputDir.mkdirs();

        // When
        List<File> results = service.splitPdfByPages(inputFile, outputDir, "page");

        // Then
        assertEquals(5, results.size());
        
        for (File resultFile : results) {
            assertTrue(resultFile.exists());
            try (PDDocument doc = Loader.loadPDF(resultFile)) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }
    }

    @Test
    @DisplayName("Test split PDF by pages with null base filename")
    void testSplitPdfByPages_NullBaseFilename() throws IOException {
        // Given
        File inputFile = createTestPdf("test.pdf", 3);
        File outputDir = tempDir.toFile();

        // When
        List<File> results = service.splitPdfByPages(inputFile, outputDir, null);

        // Then
        assertEquals(3, results.size());
        assertTrue(results.get(0).getName().startsWith("test"));
    }

    @Test
    @DisplayName("Test split PDF by ranges with Path objects")
    void testSplitPdfByRanges_WithPaths() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        Path outputDirPath = tempDir.resolve("output_paths");
        Files.createDirectories(outputDirPath);

        List<PageRange> ranges = Arrays.asList(
            new PageRange(1, 3),
            new PageRange(4, 7)
        );

        // When
        List<Path> results = service.splitPdfByRanges(inputFile.toPath(), ranges, outputDirPath, "split");

        // Then
        assertEquals(2, results.size());
        
        for (Path resultPath : results) {
            assertTrue(Files.exists(resultPath));
        }
    }

    @Test
    @DisplayName("Test extract specific pages")
    void testExtractPages() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputDir = tempDir.resolve("extracted").toFile();
        outputDir.mkdirs();

        List<Integer> pageNumbers = Arrays.asList(1, 3, 5, 7);

        // When
        List<File> results = service.extractPages(inputFile, pageNumbers, outputDir, "extracted");

        // Then
        assertEquals(4, results.size());
        
        for (File resultFile : results) {
            assertTrue(resultFile.exists());
            try (PDDocument doc = Loader.loadPDF(resultFile)) {
                assertEquals(1, doc.getNumberOfPages());
            }
        }
    }

    @Test
    @DisplayName("Test extract pages with null input file")
    void testExtractPages_NullInputFile() {
        // Given
        File outputDir = tempDir.toFile();
        List<Integer> pageNumbers = Collections.singletonList(1);

        // When/Then
        IOException exception = assertThrows(
            IOException.class,
            () -> service.extractPages(null, pageNumbers, outputDir, "extracted")
        );
        assertTrue(exception.getMessage().contains("Input file does not exist"));
    }

    @Test
    @DisplayName("Test extract pages with null page numbers")
    void testExtractPages_NullPageNumbers() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputDir = tempDir.toFile();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.extractPages(inputFile, null, outputDir, "extracted")
        );
        assertEquals("At least one page number is required", exception.getMessage());
    }

    @Test
    @DisplayName("Test extract pages with empty page numbers")
    void testExtractPages_EmptyPageNumbers() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputDir = tempDir.toFile();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.extractPages(inputFile, Collections.emptyList(), outputDir, "extracted")
        );
        assertEquals("At least one page number is required", exception.getMessage());
    }

    @Test
    @DisplayName("Test extract pages with null output directory")
    void testExtractPages_NullOutputDir() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        List<Integer> pageNumbers = Collections.singletonList(1);

        // When/Then
        IOException exception = assertThrows(
            IOException.class,
            () -> service.extractPages(inputFile, pageNumbers, null, "extracted")
        );
        assertTrue(exception.getMessage().contains("Output directory does not exist"));
    }

    @Test
    @DisplayName("Test extract pages with page number out of bounds")
    void testExtractPages_PageOutOfBounds() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 5);
        File outputDir = tempDir.toFile();
        List<Integer> pageNumbers = Arrays.asList(1, 10);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.extractPages(inputFile, pageNumbers, outputDir, "extracted")
        );
        assertTrue(exception.getMessage().contains("out of bounds"));
    }

    @Test
    @DisplayName("Test extract pages with page number less than 1")
    void testExtractPages_PageLessThanOne() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 5);
        File outputDir = tempDir.toFile();
        List<Integer> pageNumbers = Arrays.asList(0, 1);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.extractPages(inputFile, pageNumbers, outputDir, "extracted")
        );
        assertTrue(exception.getMessage().contains("out of bounds"));
    }

    @Test
    @DisplayName("Test extract pages as single file")
    void testExtractPagesAsSingleFile() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputFile = tempDir.resolve("extracted_single.pdf").toFile();

        List<Integer> pageNumbers = Arrays.asList(2, 4, 6, 8);

        // When
        List<File> results = service.extractPagesAsSingleFile(inputFile, pageNumbers, outputFile);

        // Then
        assertEquals(1, results.size());
        assertTrue(outputFile.exists());
        
        try (PDDocument doc = Loader.loadPDF(outputFile)) {
            assertEquals(4, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test extract pages as single file with unsorted page numbers")
    void testExtractPagesAsSingleFile_UnsortedPages() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputFile = tempDir.resolve("extracted_unsorted.pdf").toFile();

        List<Integer> pageNumbers = Arrays.asList(7, 2, 9, 4);

        // When
        List<File> results = service.extractPagesAsSingleFile(inputFile, pageNumbers, outputFile);

        // Then
        assertEquals(1, results.size());
        assertTrue(outputFile.exists());
        
        try (PDDocument doc = Loader.loadPDF(outputFile)) {
            assertEquals(4, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test extract pages as single file with null input file")
    void testExtractPagesAsSingleFile_NullInputFile() {
        // Given
        File outputFile = tempDir.resolve("output.pdf").toFile();
        List<Integer> pageNumbers = Collections.singletonList(1);

        // When/Then
        IOException exception = assertThrows(
            IOException.class,
            () -> service.extractPagesAsSingleFile(null, pageNumbers, outputFile)
        );
        assertTrue(exception.getMessage().contains("Input file does not exist"));
    }

    @Test
    @DisplayName("Test extract pages as single file with null page numbers")
    void testExtractPagesAsSingleFile_NullPageNumbers() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputFile = tempDir.resolve("output.pdf").toFile();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.extractPagesAsSingleFile(inputFile, null, outputFile)
        );
        assertEquals("At least one page number is required", exception.getMessage());
    }

    @Test
    @DisplayName("Test extract pages as single file with null output file")
    void testExtractPagesAsSingleFile_NullOutputFile() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        List<Integer> pageNumbers = Collections.singletonList(1);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.extractPagesAsSingleFile(inputFile, pageNumbers, null)
        );
        assertEquals("Output file cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Test extract pages as single file creates output directory")
    void testExtractPagesAsSingleFile_CreatesDirectory() throws IOException {
        // Given
        File inputFile = createTestPdf("input.pdf", 10);
        File outputFile = tempDir.resolve("nested/dir/extracted.pdf").toFile();
        List<Integer> pageNumbers = Arrays.asList(1, 2, 3);

        // When
        List<File> results = service.extractPagesAsSingleFile(inputFile, pageNumbers, outputFile);

        // Then
        assertEquals(1, results.size());
        assertTrue(outputFile.exists());
        assertTrue(outputFile.getParentFile().exists());
    }
}
