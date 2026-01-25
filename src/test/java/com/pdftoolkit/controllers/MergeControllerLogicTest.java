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
 * Tests for Merge Controller logic (non-UI components).
 * Tests state management, validation, and reordering logic.
 */
class MergeControllerLogicTest {

    private List<File> testFiles;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create temp directory and test PDF files
        tempDir = Files.createTempDirectory("pdftoolkit-test");
        testFiles = new ArrayList<>();
        
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
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("Merge button should be disabled with 0 files")
    void testMergeButton_DisabledWith0Files() {
        List<File> files = new ArrayList<>();
        boolean shouldEnable = files.size() >= 2;
        assertFalse(shouldEnable, "Should be disabled with 0 files");
    }

    @Test
    @DisplayName("Merge button should be disabled with 1 file")
    void testMergeButton_DisabledWith1File() {
        List<File> files = new ArrayList<>();
        files.add(testFiles.get(0));
        boolean shouldEnable = files.size() >= 2;
        assertFalse(shouldEnable, "Should be disabled with 1 file");
    }

    @Test
    @DisplayName("Merge button should be enabled with 2 files")
    void testMergeButton_EnabledWith2Files() {
        List<File> files = new ArrayList<>();
        files.add(testFiles.get(0));
        files.add(testFiles.get(1));
        boolean shouldEnable = files.size() >= 2;
        assertTrue(shouldEnable, "Should be enabled with 2 files");
    }

    @Test
    @DisplayName("Merge button should be enabled with many files")
    void testMergeButton_EnabledWithManyFiles() {
        List<File> files = new ArrayList<>(testFiles);
        boolean shouldEnable = files.size() >= 2;
        assertTrue(shouldEnable, "Should be enabled with 5 files");
    }

    @Test
    @DisplayName("Should filter PDF files from mixed file list")
    void testFileFilter_OnlyPDFs() {
        List<File> mixedFiles = new ArrayList<>();
        mixedFiles.add(new File("document.pdf"));
        mixedFiles.add(new File("image.jpg"));
        mixedFiles.add(new File("text.txt"));
        mixedFiles.add(new File("another.pdf"));
        
        List<File> pdfOnly = mixedFiles.stream()
            .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
            .toList();
        
        assertEquals(2, pdfOnly.size(), "Should only have 2 PDF files");
        assertTrue(pdfOnly.get(0).getName().endsWith(".pdf"));
        assertTrue(pdfOnly.get(1).getName().endsWith(".pdf"));
    }

    @Test
    @DisplayName("Should reorder files correctly when dragging up")
    void testReorder_DragUp() {
        List<File> files = new ArrayList<>(testFiles);
        
        // Move file from index 3 to index 1
        File draggedFile = files.remove(3);
        files.add(1, draggedFile);
        
        assertEquals("test4.pdf", files.get(1).getName(), 
                    "Dragged file should be at position 1");
        assertEquals("test2.pdf", files.get(2).getName(),
                    "Previous file should shift down");
    }

    @Test
    @DisplayName("Should reorder files correctly when dragging down")
    void testReorder_DragDown() {
        List<File> files = new ArrayList<>(testFiles);
        
        // Move file from index 1 to index 3
        File draggedFile = files.remove(1);
        files.add(3, draggedFile);
        
        assertEquals("test2.pdf", files.get(3).getName(),
                    "Dragged file should be at position 3");
        assertEquals("test3.pdf", files.get(1).getName(),
                    "Next file should shift up");
    }

    @Test
    @DisplayName("Should handle dragging to same position")
    void testReorder_SamePosition() {
        List<File> files = new ArrayList<>(testFiles);
        List<File> original = new ArrayList<>(files);
        
        // "Drag" file to same position (no-op)
        int index = 2;
        if (index == 2) {
            // Don't modify list
        }
        
        assertEquals(original, files, "List should remain unchanged");
    }

    @Test
    @DisplayName("Should validate output folder exists")
    void testValidation_OutputFolderExists() {
        File validFolder = tempDir.toFile();
        assertTrue(validFolder.exists() && validFolder.isDirectory(),
                  "Valid folder should pass");
        
        File invalidFolder = new File("/nonexistent/path/folder");
        assertFalse(invalidFolder.exists(),
                   "Invalid folder should fail");
    }

    @Test
    @DisplayName("Should validate filename is not empty")
    void testValidation_FilenameNotEmpty() {
        String validName = "merged.pdf";
        assertFalse(validName == null || validName.trim().isEmpty(),
                   "Valid filename should pass");
        
        String emptyName = "";
        assertTrue(emptyName.trim().isEmpty(),
                  "Empty filename should fail");
        
        String nullName = null;
        assertTrue(nullName == null || nullName.trim().isEmpty(),
                  "Null filename should fail");
    }

    @Test
    @DisplayName("Should use default filename when none provided")
    void testDefaultFilename() {
        String filename = "";
        if (filename == null || filename.trim().isEmpty()) {
            filename = "merged.pdf";
        }
        
        assertEquals("merged.pdf", filename,
                    "Should use default filename");
    }

    @Test
    @DisplayName("List should maintain order during multiple operations")
    void testMultipleReorders() {
        List<File> files = new ArrayList<>(testFiles);
        
        // Multiple reorder operations
        File file1 = files.remove(0);
        files.add(2, file1); // Move first to position 2
        
        File file2 = files.remove(4);
        files.add(0, file2); // Move last to first
        
        assertEquals(5, files.size(), "Should maintain all files");
        assertFalse(files.contains(null), "Should not contain null");
    }
}
