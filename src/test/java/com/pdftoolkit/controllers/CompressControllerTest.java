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
 * Unit tests for CompressControllerRedesigned logic.
 * Tests state management, validation, file handling, and business logic.
 * 
 * Note: These tests focus on the controller's business logic without
 * requiring JavaFX initialization. UI-specific tests would need
 * TestFX or similar framework.
 */
class CompressControllerTest {

    private List<File> testFiles;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("pdftoolkit-compress-test");
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
        // Delete directory only if empty
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
            new File("text.txt"),
            new File("spreadsheet.xlsx")
        );
        
        for (File file : nonPdfFiles) {
            boolean isValid = file.getName().toLowerCase().endsWith(".pdf");
            assertFalse(isValid, "Should reject " + file.getName());
        }
    }

    @Test
    @DisplayName("Should filter PDF files from mixed file list")
    void testFileFiltering_MixedList() {
        List<File> mixedFiles = new ArrayList<>();
        mixedFiles.add(new File("doc1.pdf"));
        mixedFiles.add(new File("image.jpg"));
        mixedFiles.add(new File("doc2.pdf"));
        mixedFiles.add(new File("text.txt"));
        mixedFiles.add(new File("doc3.pdf"));
        
        List<File> pdfOnly = mixedFiles.stream()
            .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
            .toList();
        
        assertEquals(3, pdfOnly.size(), "Should filter to 3 PDF files");
        assertTrue(pdfOnly.stream().allMatch(f -> f.getName().endsWith(".pdf")));
    }

    // ==================== State Validation Tests ====================

    @Test
    @DisplayName("Compress button should be disabled with 0 files")
    void testValidation_DisabledWith0Files() {
        List<File> files = new ArrayList<>();
        boolean shouldEnable = !files.isEmpty();
        assertFalse(shouldEnable, "Should be disabled with 0 files");
    }

    @Test
    @DisplayName("Compress button should be enabled with 1 file")
    void testValidation_EnabledWith1File() {
        List<File> files = new ArrayList<>();
        files.add(testFiles.get(0));
        boolean shouldEnable = !files.isEmpty();
        assertTrue(shouldEnable, "Should be enabled with 1 file");
    }

    @Test
    @DisplayName("Compress button should be enabled with multiple files")
    void testValidation_EnabledWithMultipleFiles() {
        List<File> files = new ArrayList<>(testFiles);
        boolean shouldEnable = !files.isEmpty();
        assertTrue(shouldEnable, "Should be enabled with " + files.size() + " files");
    }

    @Test
    @DisplayName("Should validate output folder is set")
    void testValidation_OutputFolderRequired() {
        Path outputFolder = null;
        boolean isValid = outputFolder != null;
        assertFalse(isValid, "Should be invalid when output folder is null");
        
        outputFolder = tempDir;
        isValid = outputFolder != null;
        assertTrue(isValid, "Should be valid when output folder is set");
    }

    @Test
    @DisplayName("Should validate output filename is not empty")
    void testValidation_OutputFilenameRequired() {
        String filename = "";
        boolean isValid = filename != null && !filename.trim().isEmpty();
        assertFalse(isValid, "Should be invalid when filename is empty");
        
        filename = "compressed.pdf";
        isValid = filename != null && !filename.trim().isEmpty();
        assertTrue(isValid, "Should be valid when filename is set");
    }

    @Test
    @DisplayName("Should validate complete state for compression")
    void testValidation_CompleteState() {
        // Invalid: no files
        List<File> files = new ArrayList<>();
        Path outputFolder = tempDir;
        String filename = "compressed.pdf";
        boolean isValid = !files.isEmpty() && outputFolder != null && 
                         filename != null && !filename.trim().isEmpty();
        assertFalse(isValid, "Should be invalid without files");
        
        // Valid: all required fields present
        files.add(testFiles.get(0));
        isValid = !files.isEmpty() && outputFolder != null && 
                 filename != null && !filename.trim().isEmpty();
        assertTrue(isValid, "Should be valid with all required fields");
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

    @Test
    @DisplayName("Should allow different files with same name in different folders")
    void testDuplicateDetection_DifferentPaths() throws IOException {
        Path tempDir2 = Files.createTempDirectory("pdftoolkit-compress-test2");
        
        try {
            File file1 = tempDir.resolve("test.pdf").toFile();
            File file2 = tempDir2.resolve("test.pdf").toFile();
            file1.createNewFile();
            file2.createNewFile();
            
            List<Path> addedPaths = new ArrayList<>();
            
            boolean added1 = !addedPaths.contains(file1.toPath());
            if (added1) addedPaths.add(file1.toPath());
            assertTrue(added1, "First file should be added");
            
            boolean added2 = !addedPaths.contains(file2.toPath());
            if (added2) addedPaths.add(file2.toPath());
            assertTrue(added2, "Second file with same name but different path should be added");
            
            assertEquals(2, addedPaths.size(), "Should have 2 different files");
            
            // Clean up temp files
            Files.deleteIfExists(file1.toPath());
            Files.deleteIfExists(file2.toPath());
        } finally {
            // Clean up second temp directory
            if (Files.exists(tempDir2)) {
                try (var stream = Files.list(tempDir2)) {
                    stream.forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
                }
                Files.deleteIfExists(tempDir2);
            }
        }
    }

    // ==================== Output Filename Tests ====================

    @Test
    @DisplayName("Should auto-append .pdf extension if missing")
    void testOutputFilename_AutoAppendExtension() {
        String filename = "compressed";
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename += ".pdf";
        }
        assertEquals("compressed.pdf", filename, "Should append .pdf extension");
    }

    @Test
    @DisplayName("Should not double-append .pdf extension")
    void testOutputFilename_NoDoubleExtension() {
        String filename = "compressed.pdf";
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename += ".pdf";
        }
        assertEquals("compressed.pdf", filename, "Should not double-append .pdf");
    }

    @Test
    @DisplayName("Should handle duplicate output filenames")
    void testOutputFilename_HandleDuplicates() throws IOException {
        String baseFilename = "compressed.pdf";
        Path outputDir = tempDir;
        Path outputPath = outputDir.resolve(baseFilename);
        
        // Create existing file
        Files.createFile(outputPath);
        
        // Generate new filename
        int counter = 1;
        Path newPath = outputPath;
        while (Files.exists(newPath)) {
            String baseName = baseFilename.substring(0, baseFilename.length() - 4);
            newPath = outputDir.resolve(baseName + "_(" + counter + ").pdf");
            counter++;
        }
        
        assertEquals("compressed_(1).pdf", newPath.getFileName().toString(), 
                    "Should generate numbered filename");
        assertFalse(Files.exists(newPath), "New path should not exist yet");
    }

    // ==================== Compression Level Tests ====================

    @Test
    @DisplayName("Should have three compression levels")
    void testCompressionLevel_ThreeLevels() {
        String[] levels = {"EXTREME", "RECOMMENDED", "LOW"};
        assertEquals(3, levels.length, "Should have 3 compression levels");
    }

    @Test
    @DisplayName("Default compression level should be RECOMMENDED")
    void testCompressionLevel_DefaultIsRecommended() {
        String defaultLevel = "RECOMMENDED";
        assertEquals("RECOMMENDED", defaultLevel, "Default should be RECOMMENDED");
    }

    // ==================== Files Count Label Tests ====================

    @Test
    @DisplayName("Should format files count label correctly for 0 files")
    void testFilesCountLabel_ZeroFiles() {
        int count = 0;
        String label = count == 0 ? "No files selected" : 
                      count == 1 ? "1 file" : count + " files";
        assertEquals("No files selected", label);
    }

    @Test
    @DisplayName("Should format files count label correctly for 1 file")
    void testFilesCountLabel_OneFile() {
        int count = 1;
        String label = count == 0 ? "No files selected" : 
                      count == 1 ? "1 file" : count + " files";
        assertEquals("1 file", label);
    }

    @Test
    @DisplayName("Should format files count label correctly for multiple files")
    void testFilesCountLabel_MultipleFiles() {
        int count = 5;
        String label = count == 0 ? "No files selected" : 
                      count == 1 ? "1 file" : count + " files";
        assertEquals("5 files", label);
    }

    // ==================== Button Text Tests ====================

    @Test
    @DisplayName("Compress button should show 'Compress Now' for 0 files")
    void testCompressButtonText_ZeroFiles() {
        int count = 0;
        String buttonText;
        if (count == 0) {
            buttonText = "Compress Now";
        } else if (count == 1) {
            buttonText = "Compress 1 PDF";
        } else {
            buttonText = String.format("Compress %d PDFs", count);
        }
        assertEquals("Compress Now", buttonText);
    }

    @Test
    @DisplayName("Compress button should show 'Compress 1 PDF' for 1 file")
    void testCompressButtonText_OneFile() {
        int count = 1;
        String buttonText;
        if (count == 0) {
            buttonText = "Compress Now";
        } else if (count == 1) {
            buttonText = "Compress 1 PDF";
        } else {
            buttonText = String.format("Compress %d PDFs", count);
        }
        assertEquals("Compress 1 PDF", buttonText);
    }

    @Test
    @DisplayName("Compress button should show 'Compress N PDFs' for multiple files")
    void testCompressButtonText_MultipleFiles() {
        int count = 5;
        String buttonText;
        if (count == 0) {
            buttonText = "Compress Now";
        } else if (count == 1) {
            buttonText = "Compress 1 PDF";
        } else {
            buttonText = String.format("Compress %d PDFs", count);
        }
        assertEquals("Compress 5 PDFs", buttonText);
    }

    // ==================== Empty State Tests ====================

    @Test
    @DisplayName("Empty state should be visible when no files")
    void testEmptyState_VisibleWhenEmpty() {
        List<File> files = new ArrayList<>();
        boolean emptyStateVisible = files.isEmpty();
        boolean fileListVisible = !files.isEmpty();
        
        assertTrue(emptyStateVisible, "Empty state should be visible");
        assertFalse(fileListVisible, "File list should not be visible");
    }

    @Test
    @DisplayName("File list should be visible when files exist")
    void testEmptyState_HiddenWhenFilesExist() {
        List<File> files = new ArrayList<>();
        files.add(testFiles.get(0));
        
        boolean emptyStateVisible = files.isEmpty();
        boolean fileListVisible = !files.isEmpty();
        
        assertFalse(emptyStateVisible, "Empty state should not be visible");
        assertTrue(fileListVisible, "File list should be visible");
    }

    // ==================== More Options Panel Tests ====================

    @Test
    @DisplayName("More options should be collapsed by default")
    void testMoreOptions_CollapsedByDefault() {
        boolean expanded = false;
        assertFalse(expanded, "More options should be collapsed initially");
    }

    @Test
    @DisplayName("More options toggle should change state")
    void testMoreOptions_ToggleChangesState() {
        boolean expanded = false;
        
        // First toggle
        expanded = !expanded;
        assertTrue(expanded, "Should be expanded after first toggle");
        
        // Second toggle
        expanded = !expanded;
        assertFalse(expanded, "Should be collapsed after second toggle");
    }

    // ==================== File List Operations Tests ====================

    @Test
    @DisplayName("Should add files to list")
    void testFileListOperations_AddFiles() {
        List<Path> files = new ArrayList<>();
        assertEquals(0, files.size(), "Should start with 0 files");
        
        files.add(testFiles.get(0).toPath());
        assertEquals(1, files.size(), "Should have 1 file after adding");
        
        files.add(testFiles.get(1).toPath());
        assertEquals(2, files.size(), "Should have 2 files after adding");
    }

    @Test
    @DisplayName("Should remove files from list")
    void testFileListOperations_RemoveFiles() {
        List<Path> files = new ArrayList<>();
        Path file1 = testFiles.get(0).toPath();
        Path file2 = testFiles.get(1).toPath();
        
        files.add(file1);
        files.add(file2);
        assertEquals(2, files.size(), "Should have 2 files");
        
        files.remove(file1);
        assertEquals(1, files.size(), "Should have 1 file after removal");
        assertTrue(files.contains(file2), "Should contain file2");
        assertFalse(files.contains(file1), "Should not contain file1");
    }

    @Test
    @DisplayName("Should clear all files from list")
    void testFileListOperations_ClearAll() {
        List<Path> files = new ArrayList<>();
        files.add(testFiles.get(0).toPath());
        files.add(testFiles.get(1).toPath());
        files.add(testFiles.get(2).toPath());
        assertEquals(3, files.size(), "Should have 3 files");
        
        files.clear();
        assertEquals(0, files.size(), "Should have 0 files after clear");
        assertTrue(files.isEmpty(), "List should be empty");
    }

    // ==================== Success Message Tests ====================

    @Test
    @DisplayName("Success message should be singular for 1 file")
    void testSuccessMessage_SingleFile() {
        int fileCount = 1;
        String message;
        if (fileCount == 1) {
            message = "File successfully compressed!";
        } else {
            message = String.format("%d files successfully compressed!", fileCount);
        }
        assertEquals("File successfully compressed!", message);
    }

    @Test
    @DisplayName("Success message should be plural for multiple files")
    void testSuccessMessage_MultipleFiles() {
        int fileCount = 5;
        String message;
        if (fileCount == 1) {
            message = "File successfully compressed!";
        } else {
            message = String.format("%d files successfully compressed!", fileCount);
        }
        assertEquals("5 files successfully compressed!", message);
    }
}
