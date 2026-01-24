package com.pdftoolkit.operations;

import com.pdftoolkit.operations.stub.StubSplitOperation;
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
 * Unit tests for StubSplitOperation.
 */
class StubSplitOperationTest {

    private StubSplitOperation operation;
    private List<File> inputFiles;
    private File outputFolder;
    private Map<String, Object> parameters;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        operation = new StubSplitOperation();
        inputFiles = new ArrayList<>();
        parameters = new HashMap<>();
        
        outputFolder = tempDir.resolve("output").toFile();
        outputFolder.mkdirs();
        
        File file = tempDir.resolve("document.pdf").toFile();
        Files.writeString(file.toPath(), "PDF content for splitting");
        inputFiles.add(file);
        
        parameters.put("splitMode", "extractPages");
        parameters.put("rangeInput", "1-3");
    }

    @Test
    void testOperationName() {
        assertEquals("Split PDF", operation.getOperationName());
    }

    @Test
    void testValidateSuccess() {
        String error = operation.validate(inputFiles, outputFolder, "split", parameters);
        assertNull(error);
    }

    @Test
    void testValidateNoFiles() {
        String error = operation.validate(new ArrayList<>(), outputFolder, "split", parameters);
        assertNotNull(error);
        assertTrue(error.contains("select a PDF"));
    }

    @Test
    void testValidateMultipleFiles() {
        inputFiles.add(new File("extra.pdf"));
        String error = operation.validate(inputFiles, outputFolder, "split", parameters);
        assertNotNull(error);
        assertTrue(error.contains("only one"));
    }

    @Test
    void testValidateMissingRangeInput() {
        parameters.put("rangeInput", "");
        String error = operation.validate(inputFiles, outputFolder, "split", parameters);
        assertNotNull(error);
        assertTrue(error.contains("page ranges"));
    }

    @Test
    void testExecuteSuccess() throws Exception {
        Task<File> task = operation.execute(inputFiles, outputFolder, "split", parameters);
        
        // Execute task using helper
        File result = TaskTestHelper.executeTask(task);
        
        assertNotNull(result);
    }
}
