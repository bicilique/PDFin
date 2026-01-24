package com.pdftoolkit.operations;

import com.pdftoolkit.operations.stub.StubMergeOperation;
import com.pdftoolkit.test.TaskTestHelper;
import javafx.concurrent.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StubMergeOperation.
 */
class StubMergeOperationTest {

    private StubMergeOperation operation;
    private List<File> inputFiles;
    private File outputFolder;
    private Map<String, Object> parameters;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        operation = new StubMergeOperation();
        inputFiles = new ArrayList<>();
        parameters = new HashMap<>();
        
        // Create temp output folder
        outputFolder = tempDir.resolve("output").toFile();
        outputFolder.mkdirs();
        
        // Create dummy PDF files
        for (int i = 1; i <= 3; i++) {
            File file = tempDir.resolve("test" + i + ".pdf").toFile();
            Files.writeString(file.toPath(), "PDF content " + i);
            inputFiles.add(file);
        }
    }

    @Test
    void testOperationName() {
        assertEquals("Merge PDF", operation.getOperationName());
    }

    @Test
    void testValidateSuccess() {
        String error = operation.validate(inputFiles, outputFolder, "merged.pdf", parameters);
        assertNull(error, "Should be valid with 2+ files");
    }

    @Test
    void testValidateNoFiles() {
        String error = operation.validate(new ArrayList<>(), outputFolder, "merged.pdf", parameters);
        assertNotNull(error);
        assertTrue(error.contains("at least 2"));
    }

    @Test
    void testValidateOneFile() {
        List<File> oneFile = new ArrayList<>();
        oneFile.add(inputFiles.get(0));
        
        String error = operation.validate(oneFile, outputFolder, "merged.pdf", parameters);
        assertNotNull(error);
        assertTrue(error.contains("at least 2"));
    }

    @Test
    void testValidateNullFiles() {
        String error = operation.validate(null, outputFolder, "merged.pdf", parameters);
        assertNotNull(error);
    }

    @Test
    void testValidateNullOutputFolder() {
        String error = operation.validate(inputFiles, null, "merged.pdf", parameters);
        assertNotNull(error);
        assertTrue(error.contains("output folder"));
    }

    @Test
    void testValidateEmptyFileName() {
        String error = operation.validate(inputFiles, outputFolder, "", parameters);
        assertNotNull(error);
        assertTrue(error.contains("file name"));
    }

    @Test
    void testExecuteSuccess() throws Exception {
        Task<File> task = operation.execute(inputFiles, outputFolder, "merged.pdf", parameters);
        
        // Execute task using helper
        File result = TaskTestHelper.executeTask(task);
        
        assertNotNull(result);
        assertEquals("merged.pdf", result.getName());
    }

    @Test
    void testExecuteProgressUpdates() throws Exception {
        Task<File> task = operation.execute(inputFiles, outputFolder, "merged.pdf", parameters);
        
        List<Double> progressValues = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        
        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            progressValues.add(newVal.doubleValue());
        });
        
        task.messageProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                messages.add(newVal);
            }
        });
        
        // Execute task using helper
        File result = TaskTestHelper.executeTask(task);
        
        assertNotNull(result);
        assertFalse(progressValues.isEmpty(), "Should have progress updates");
        assertFalse(messages.isEmpty(), "Should have status messages");
        assertTrue(messages.stream().anyMatch(m -> m.contains("Preparing") || m.contains("Processing")));
    }

    @Test
    void testExecuteCancel() throws Exception {
        Task<File> task = operation.execute(inputFiles, outputFolder, "merged.pdf", parameters);
        
        // Start task in thread
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        
        // Cancel immediately
        Thread.sleep(50);
        boolean cancelled = task.cancel();
        
        assertTrue(cancelled || task.isCancelled(), "Task should be cancellable");
        
        // Wait a bit for cancellation to take effect
        Thread.sleep(200);
    }
}
