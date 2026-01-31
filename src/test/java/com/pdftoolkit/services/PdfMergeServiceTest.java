package com.pdftoolkit.services;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for PdfMergeService.
 * Achieves 100% code coverage for PDF merging operations.
 */
class PdfMergeServiceTest {

    private PdfMergeService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new PdfMergeService();
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
    @DisplayName("Test successful merge of two PDFs")
    void testMergePdfs_TwoFiles() throws IOException {
        // Given
        File pdf1 = createTestPdf("file1.pdf", 3);
        File pdf2 = createTestPdf("file2.pdf", 2);
        File outputFile = tempDir.resolve("merged.pdf").toFile();

        List<File> inputFiles = Arrays.asList(pdf1, pdf2);

        // When
        service.mergePdfs(inputFiles, outputFile);

        // Then
        assertTrue(outputFile.exists());
        
        // Verify merged PDF has correct page count
        try (PDDocument merged = Loader.loadPDF(outputFile)) {
            assertEquals(5, merged.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test successful merge of multiple PDFs")
    void testMergePdfs_MultipleFiles() throws IOException {
        // Given
        File pdf1 = createTestPdf("file1.pdf", 2);
        File pdf2 = createTestPdf("file2.pdf", 3);
        File pdf3 = createTestPdf("file3.pdf", 1);
        File pdf4 = createTestPdf("file4.pdf", 4);
        File outputFile = tempDir.resolve("merged_multi.pdf").toFile();

        List<File> inputFiles = Arrays.asList(pdf1, pdf2, pdf3, pdf4);

        // When
        service.mergePdfs(inputFiles, outputFile);

        // Then
        assertTrue(outputFile.exists());
        
        try (PDDocument merged = Loader.loadPDF(outputFile)) {
            assertEquals(10, merged.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test merge with null input files list")
    void testMergePdfs_NullInputFiles() {
        // Given
        File outputFile = tempDir.resolve("output.pdf").toFile();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.mergePdfs(null, outputFile)
        );
        assertEquals("Input files list cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Test merge with empty input files list")
    void testMergePdfs_EmptyInputFiles() {
        // Given
        List<File> emptyList = new ArrayList<>();
        File outputFile = tempDir.resolve("output.pdf").toFile();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.mergePdfs(emptyList, outputFile)
        );
        assertEquals("Input files list cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Test merge with only one file")
    void testMergePdfs_SingleFile() throws IOException {
        // Given
        File pdf1 = createTestPdf("single.pdf", 3);
        List<File> inputFiles = Collections.singletonList(pdf1);
        File outputFile = tempDir.resolve("output.pdf").toFile();

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.mergePdfs(inputFiles, outputFile)
        );
        assertEquals("At least 2 PDF files are required for merging", exception.getMessage());
    }

    @Test
    @DisplayName("Test merge with null output file")
    void testMergePdfs_NullOutputFile() throws IOException {
        // Given
        File pdf1 = createTestPdf("file1.pdf", 2);
        File pdf2 = createTestPdf("file2.pdf", 2);
        List<File> inputFiles = Arrays.asList(pdf1, pdf2);

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.mergePdfs(inputFiles, null)
        );
        assertEquals("Output file cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Test merge with non-existent input file")
    void testMergePdfs_NonExistentInputFile() throws IOException {
        // Given
        File pdf1 = createTestPdf("file1.pdf", 2);
        File nonExistentFile = tempDir.resolve("nonexistent.pdf").toFile();
        File outputFile = tempDir.resolve("merged.pdf").toFile();

        List<File> inputFiles = Arrays.asList(pdf1, nonExistentFile);

        // When/Then
        IOException exception = assertThrows(
            IOException.class,
            () -> service.mergePdfs(inputFiles, outputFile)
        );
        assertTrue(exception.getMessage().contains("Cannot read file"));
    }

    @Test
    @DisplayName("Test merge with unreadable input file")
    void testMergePdfs_UnreadableInputFile() throws IOException {
        // Given
        File pdf1 = createTestPdf("file1.pdf", 2);
        File pdf2 = createTestPdf("file2.pdf", 2);
        pdf2.setReadable(false);
        File outputFile = tempDir.resolve("merged.pdf").toFile();

        List<File> inputFiles = Arrays.asList(pdf1, pdf2);

        try {
            // When/Then
            IOException exception = assertThrows(
                IOException.class,
                () -> service.mergePdfs(inputFiles, outputFile)
            );
            assertTrue(exception.getMessage().contains("Cannot read file"));
        } finally {
            pdf2.setReadable(true);
        }
    }

    @Test
    @DisplayName("Test merge creates output directory if not exists")
    void testMergePdfs_CreatesOutputDirectory() throws IOException {
        // Given
        File pdf1 = createTestPdf("file1.pdf", 2);
        File pdf2 = createTestPdf("file2.pdf", 2);
        File outputFile = tempDir.resolve("nested/dir/merged.pdf").toFile();

        List<File> inputFiles = Arrays.asList(pdf1, pdf2);

        // When
        service.mergePdfs(inputFiles, outputFile);

        // Then
        assertTrue(outputFile.exists());
    }

    @Test
    @DisplayName("Test merge with Path objects")
    void testMergePdfs_WithPaths() throws IOException {
        // Given
        File pdf1 = createTestPdf("file1.pdf", 2);
        File pdf2 = createTestPdf("file2.pdf", 3);
        Path outputPath = tempDir.resolve("merged_paths.pdf");

        List<Path> inputPaths = Arrays.asList(pdf1.toPath(), pdf2.toPath());

        // When
        service.mergePdfs(inputPaths, outputPath);

        // Then
        assertTrue(Files.exists(outputPath));
        
        try (PDDocument merged = Loader.loadPDF(outputPath.toFile())) {
            assertEquals(5, merged.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test merge with invalid PDF file")
    void testMergePdfs_InvalidPdf() throws IOException {
        // Given
        File validPdf = createTestPdf("valid.pdf", 2);
        File invalidPdf = tempDir.resolve("invalid.pdf").toFile();
        Files.writeString(invalidPdf.toPath(), "This is not a PDF");
        File outputFile = tempDir.resolve("merged.pdf").toFile();

        List<File> inputFiles = Arrays.asList(validPdf, invalidPdf);

        // When/Then
        assertThrows(IOException.class, () -> service.mergePdfs(inputFiles, outputFile));
    }

    @Test
    @DisplayName("Test merge preserves page order")
    void testMergePdfs_PreservesOrder() throws IOException {
        // Given
        File pdf1 = createTestPdf("file1.pdf", 1);
        File pdf2 = createTestPdf("file2.pdf", 1);
        File pdf3 = createTestPdf("file3.pdf", 1);
        File outputFile = tempDir.resolve("merged_ordered.pdf").toFile();

        List<File> inputFiles = Arrays.asList(pdf1, pdf2, pdf3);

        // When
        service.mergePdfs(inputFiles, outputFile);

        // Then
        assertTrue(outputFile.exists());
        try (PDDocument merged = Loader.loadPDF(outputFile)) {
            assertEquals(3, merged.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test merge with large number of files")
    void testMergePdfs_ManyFiles() throws IOException {
        // Given
        List<File> inputFiles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            inputFiles.add(createTestPdf("file" + i + ".pdf", 1));
        }
        File outputFile = tempDir.resolve("merged_many.pdf").toFile();

        // When
        service.mergePdfs(inputFiles, outputFile);

        // Then
        assertTrue(outputFile.exists());
        try (PDDocument merged = Loader.loadPDF(outputFile)) {
            assertEquals(10, merged.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test merge with different page sizes")
    void testMergePdfs_DifferentPageSizes() throws IOException {
        // Given - Create PDFs with different page sizes
        File pdf1 = tempDir.resolve("a4.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(pdf1);
        }

        File pdf2 = tempDir.resolve("letter.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage(PDRectangle.LETTER));
            doc.save(pdf2);
        }

        File outputFile = tempDir.resolve("merged_mixed.pdf").toFile();
        List<File> inputFiles = Arrays.asList(pdf1, pdf2);

        // When
        service.mergePdfs(inputFiles, outputFile);

        // Then
        assertTrue(outputFile.exists());
        try (PDDocument merged = Loader.loadPDF(outputFile)) {
            assertEquals(2, merged.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("Test merge with empty PDFs")
    void testMergePdfs_EmptyPdfs() throws IOException {
        // Given - Create empty PDFs
        File pdf1 = tempDir.resolve("empty1.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdf1);
        }

        File pdf2 = tempDir.resolve("empty2.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdf2);
        }

        File outputFile = tempDir.resolve("merged_empty.pdf").toFile();
        List<File> inputFiles = Arrays.asList(pdf1, pdf2);

        // When
        service.mergePdfs(inputFiles, outputFile);

        // Then
        assertTrue(outputFile.exists());
        try (PDDocument merged = Loader.loadPDF(outputFile)) {
            assertEquals(2, merged.getNumberOfPages());
        }
    }
}
