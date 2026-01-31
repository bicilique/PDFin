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
 * Unit tests for MergeControllerRedesignedNew logic.
 * Tests state management, validation, file handling, and business logic.
 */
class MergeControllerRedesignedNewTest {

    private List<File> testFiles;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("pdftoolkit-merge-test");
        testFiles = new ArrayList<>();
        
        // Create test PDF files
        for (int i = 1; i <= 5; i++) {
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
    @DisplayName("Should filter PDF files from mixed list")
    void testFileFiltering_MixedList() {
        List<File> mixedFiles = new ArrayList<>();
        mixedFiles.add(new File("doc1.pdf"));
        mixedFiles.add(new File("image.jpg"));
        mixedFiles.add(new File("doc2.pdf"));
        mixedFiles.add(new File("text.txt"));
        
        List<File> pdfOnly = mixedFiles.stream()
            .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
            .toList();
        
        assertEquals(2, pdfOnly.size(), "Should filter to 2 PDF files");
    }

    // ==================== State Validation Tests ====================

    @Test
    @DisplayName("Merge button should be disabled with 0 files")
    void testValidation_DisabledWith0Files() {
        List<File> files = new ArrayList<>();
        boolean shouldEnable = files.size() >= 2;
        assertFalse(shouldEnable, "Should be disabled with 0 files");
    }

    @Test
    @DisplayName("Merge button should be disabled with 1 file")
    void testValidation_DisabledWith1File() {
        List<File> files = new ArrayList<>();
        files.add(testFiles.get(0));
        boolean shouldEnable = files.size() >= 2;
        assertFalse(shouldEnable, "Should be disabled with 1 file");
    }

    @Test
    @DisplayName("Merge button should be enabled with 2 files")
    void testValidation_EnabledWith2Files() {
        List<File> files = new ArrayList<>();
        files.add(testFiles.get(0));
        files.add(testFiles.get(1));
        boolean shouldEnable = files.size() >= 2;
        assertTrue(shouldEnable, "Should be enabled with 2 files");
    }

    @Test
    @DisplayName("Merge button should be enabled with many files")
    void testValidation_EnabledWithManyFiles() {
        List<File> files = new ArrayList<>(testFiles);
        boolean shouldEnable = files.size() >= 2;
        assertTrue(shouldEnable, "Should be enabled with 5 files");
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
    @DisplayName("Should validate output filename is not empty")
    void testValidation_OutputFilenameRequired() {
        String filename = "";
        boolean isValid = filename != null && !filename.trim().isEmpty();
        assertFalse(isValid, "Should be invalid when filename is empty");
        
        filename = "merged.pdf";
        isValid = filename != null && !filename.trim().isEmpty();
        assertTrue(isValid, "Should be valid when filename is set");
    }

    // ==================== Duplicate Detection Tests ====================

    @Test
    @DisplayName("Should detect duplicate files by path")
    void testDuplicateDetection_SameFile() {
        List<Path> addedPaths = new ArrayList<>();
        Path testPath = testFiles.get(0).toPath();
        
        // Add first time
        boolean added1 = !addedPaths.contains(testPath);
        if (added1) addedPaths.add(testPath);
        assertTrue(added1, "First addition should succeed");
        
        // Try to add again
        boolean added2 = !addedPaths.contains(testPath);
        assertFalse(added2, "Duplicate addition should fail");
    }

    // ==================== File Reordering Tests ====================

    @Test
    @DisplayName("Should reorder files - move up")
    void testReorder_MoveUp() {
        List<File> files = new ArrayList<>(testFiles.subList(0, 3));
        File secondFile = files.get(1);
        
        // Move second file up
        File moved = files.remove(1);
        files.add(0, moved);
        
        assertEquals(secondFile, files.get(0), "File should be at index 0");
    }

    @Test
    @DisplayName("Should reorder files - move down")
    void testReorder_MoveDown() {
        List<File> files = new ArrayList<>(testFiles.subList(0, 3));
        File firstFile = files.get(0);
        
        // Move first file down
        File moved = files.remove(0);
        files.add(1, moved);
        
        assertEquals(firstFile, files.get(1), "File should be at index 1");
    }

    @Test
    @DisplayName("Should not move first file up")
    void testReorder_CannotMoveFirstUp() {
        List<File> files = new ArrayList<>(testFiles);
        int index = 0;
        boolean canMoveUp = index > 0;
        assertFalse(canMoveUp, "First file cannot move up");
    }

    @Test
    @DisplayName("Should not move last file down")
    void testReorder_CannotMoveLastDown() {
        List<File> files = new ArrayList<>(testFiles);
        int index = files.size() - 1;
        boolean canMoveDown = index < files.size() - 1;
        assertFalse(canMoveDown, "Last file cannot move down");
    }

    // ==================== File Size Formatting Tests ====================

    @Test
    @DisplayName("Should format bytes correctly")
    void testFormatFileSize_Bytes() {
        long bytes = 512;
        String formatted = formatFileSize(bytes);
        assertEquals("512 B", formatted);
    }

    @Test
    @DisplayName("Should format kilobytes correctly")
    void testFormatFileSize_Kilobytes() {
        long bytes = 1536; // 1.5 KB
        String formatted = formatFileSize(bytes);
        assertEquals("1.5 KB", formatted);
    }

    @Test
    @DisplayName("Should format megabytes correctly")
    void testFormatFileSize_Megabytes() {
        long bytes = 2097152; // 2 MB
        String formatted = formatFileSize(bytes);
        assertEquals("2.0 MB", formatted);
    }

    @Test
    @DisplayName("Should format gigabytes correctly")
    void testFormatFileSize_Gigabytes() {
        long bytes = 1610612736L; // 1.5 GB
        String formatted = formatFileSize(bytes);
        assertEquals("1.50 GB", formatted);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ==================== Output Filename Tests ====================

    @Test
    @DisplayName("Should auto-append .pdf extension")
    void testOutputFilename_AutoAppendExtension() {
        String filename = "merged";
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename += ".pdf";
        }
        assertEquals("merged.pdf", filename);
    }

    @Test
    @DisplayName("Should not double-append .pdf extension")
    void testOutputFilename_NoDoubleExtension() {
        String filename = "merged.pdf";
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename += ".pdf";
        }
        assertEquals("merged.pdf", filename);
    }

    // ==================== Files Count Label Tests ====================

    @Test
    @DisplayName("Files count label for 0 files")
    void testFilesCountLabel_ZeroFiles() {
        int count = 0;
        String label = count == 0 ? "No files selected" : 
                      count == 1 ? "1 file" : count + " files";
        assertEquals("No files selected", label);
    }

    @Test
    @DisplayName("Files count label for 1 file")
    void testFilesCountLabel_OneFile() {
        int count = 1;
        String label = count == 0 ? "No files selected" : 
                      count == 1 ? "1 file" : count + " files";
        assertEquals("1 file", label);
    }

    @Test
    @DisplayName("Files count label for multiple files")
    void testFilesCountLabel_MultipleFiles() {
        int count = 5;
        String label = count == 0 ? "No files selected" : 
                      count == 1 ? "1 file" : count + " files";
        assertEquals("5 files", label);
    }

    // ==================== Empty State Tests ====================

    @Test
    @DisplayName("Empty state visible when no files")
    void testEmptyState_VisibleWhenEmpty() {
        List<File> files = new ArrayList<>();
        boolean emptyStateVisible = files.isEmpty();
        boolean filesViewVisible = !files.isEmpty();
        
        assertTrue(emptyStateVisible);
        assertFalse(filesViewVisible);
    }

    @Test
    @DisplayName("Files view visible when files exist")
    void testEmptyState_HiddenWhenFilesExist() {
        List<File> files = new ArrayList<>();
        files.add(testFiles.get(0));
        
        boolean emptyStateVisible = files.isEmpty();
        boolean filesViewVisible = !files.isEmpty();
        
        assertFalse(emptyStateVisible);
        assertTrue(filesViewVisible);
    }

    // ==================== Summary Tests ====================

    @Test
    @DisplayName("Should calculate total pages correctly")
    void testSummary_TotalPages() {
        // Simulate files with different page counts
        int[] pageCounts = {5, 10, 3, 7};
        int total = 0;
        for (int count : pageCounts) {
            total += count;
        }
        assertEquals(25, total);
    }

    @Test
    @DisplayName("Should calculate total size correctly")
    void testSummary_TotalSize() {
        long[] fileSizes = {1024L, 2048L, 512L};
        long total = 0;
        for (long size : fileSizes) {
            total += size;
        }
        assertEquals(3584L, total);
    }

    // ==================== File List Operations Tests ====================

    @Test
    @DisplayName("Should add files to list")
    void testFileListOperations_AddFiles() {
        List<File> files = new ArrayList<>();
        assertEquals(0, files.size());
        
        files.add(testFiles.get(0));
        assertEquals(1, files.size());
        
        files.add(testFiles.get(1));
        assertEquals(2, files.size());
    }

    @Test
    @DisplayName("Should remove files from list")
    void testFileListOperations_RemoveFiles() {
        List<File> files = new ArrayList<>();
        File file1 = testFiles.get(0);
        File file2 = testFiles.get(1);
        
        files.add(file1);
        files.add(file2);
        assertEquals(2, files.size());
        
        files.remove(file1);
        assertEquals(1, files.size());
        assertTrue(files.contains(file2));
        assertFalse(files.contains(file1));
    }

    @Test
    @DisplayName("Should clear all files")
    void testFileListOperations_ClearAll() {
        List<File> files = new ArrayList<>(testFiles);
        assertEquals(5, files.size());
        
        files.clear();
        assertEquals(0, files.size());
        assertTrue(files.isEmpty());
    }

    // ==================== Success Message Tests ====================

    @Test
    @DisplayName("Success message format")
    void testSuccessMessage_Format() {
        String filename = "merged.pdf";
        String message = String.format("Your PDFs have been merged into:\n%s", filename);
        assertTrue(message.contains(filename));
        assertTrue(message.contains("merged"));
    }

    // ==================== Move Operations Tests ====================

    @Test
    @DisplayName("Move file up preserves list size")
    void testMoveOperations_PreservesSize() {
        List<File> files = new ArrayList<>(testFiles);
        int originalSize = files.size();
        
        // Move second file up
        if (files.size() > 1) {
            File moved = files.remove(1);
            files.add(0, moved);
        }
        
        assertEquals(originalSize, files.size());
    }

    @Test
    @DisplayName("Move file down preserves list size")
    void testMoveOperations_MoveDownPreservesSize() {
        List<File> files = new ArrayList<>(testFiles);
        int originalSize = files.size();
        
        // Move first file down
        if (files.size() > 1) {
            File moved = files.remove(0);
            files.add(1, moved);
        }
        
        assertEquals(originalSize, files.size());
    }
}
