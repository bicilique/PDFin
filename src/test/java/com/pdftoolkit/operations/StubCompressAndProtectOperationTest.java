package com.pdftoolkit.operations;

import com.pdftoolkit.operations.stub.StubCompressOperation;
import com.pdftoolkit.operations.stub.StubProtectOperation;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Compress and Protect operations.
 */
class StubCompressAndProtectOperationTest {

    @TempDir
    Path tempDir;

    @Test
    void testCompressOperationBasics() throws IOException {
        StubCompressOperation operation = new StubCompressOperation();
        assertEquals("Compress PDF", operation.getOperationName());
        
        List<File> files = new ArrayList<>();
        File file = tempDir.resolve("large.pdf").toFile();
        Files.writeString(file.toPath(), "Large PDF");
        files.add(file);
        
        File outputFolder = tempDir.resolve("output").toFile();
        outputFolder.mkdirs();
        
        Map<String, Object> params = new HashMap<>();
        params.put("compressionLevel", 2);
        
        String error = operation.validate(files, outputFolder, "compressed.pdf", params);
        assertNull(error);
    }

    @Test
    void testCompressValidationNoFiles() {
        StubCompressOperation operation = new StubCompressOperation();
        File outputFolder = tempDir.resolve("output").toFile();
        
        String error = operation.validate(new ArrayList<>(), outputFolder, "compressed.pdf", new HashMap<>());
        assertNotNull(error);
        assertTrue(error.contains("at least one"));
    }

    @Test
    void testProtectOperationBasics() throws IOException {
        StubProtectOperation operation = new StubProtectOperation();
        assertEquals("Protect PDF", operation.getOperationName());
        
        List<File> files = new ArrayList<>();
        File file = tempDir.resolve("confidential.pdf").toFile();
        Files.writeString(file.toPath(), "Confidential PDF");
        files.add(file);
        
        File outputFolder = tempDir.resolve("output").toFile();
        outputFolder.mkdirs();
        
        Map<String, Object> params = new HashMap<>();
        params.put("password", "secure123");
        params.put("confirmPassword", "secure123");
        
        String error = operation.validate(files, outputFolder, "protected.pdf", params);
        assertNull(error);
    }

    @Test
    void testProtectValidationPasswordTooShort() throws IOException {
        StubProtectOperation operation = new StubProtectOperation();
        
        List<File> files = new ArrayList<>();
        File file = tempDir.resolve("doc.pdf").toFile();
        Files.writeString(file.toPath(), "PDF");
        files.add(file);
        
        File outputFolder = tempDir.resolve("output").toFile();
        outputFolder.mkdirs();
        
        Map<String, Object> params = new HashMap<>();
        params.put("password", "123");
        params.put("confirmPassword", "123");
        
        String error = operation.validate(files, outputFolder, "protected.pdf", params);
        assertNotNull(error);
        assertTrue(error.contains("at least 6"));
    }

    @Test
    void testProtectValidationPasswordMismatch() throws IOException {
        StubProtectOperation operation = new StubProtectOperation();
        
        List<File> files = new ArrayList<>();
        File file = tempDir.resolve("doc.pdf").toFile();
        Files.writeString(file.toPath(), "PDF");
        files.add(file);
        
        File outputFolder = tempDir.resolve("output").toFile();
        outputFolder.mkdirs();
        
        Map<String, Object> params = new HashMap<>();
        params.put("password", "password123");
        params.put("confirmPassword", "different123");
        
        String error = operation.validate(files, outputFolder, "protected.pdf", params);
        assertNotNull(error);
        assertTrue(error.contains("do not match"));
    }

    @Test
    void testCompressExecuteSuccess() throws Exception {
        StubCompressOperation operation = new StubCompressOperation();
        
        List<File> files = new ArrayList<>();
        File file = tempDir.resolve("large.pdf").toFile();
        Files.writeString(file.toPath(), "Large PDF");
        files.add(file);
        
        File outputFolder = tempDir.resolve("output").toFile();
        outputFolder.mkdirs();
        
        Map<String, Object> params = new HashMap<>();
        params.put("compressionLevel", 2);
        
        Task<File> task = operation.execute(files, outputFolder, "compressed.pdf", params);
        
        // Execute task using helper
        File result = TaskTestHelper.executeTask(task);
        
        assertNotNull(result);
    }

    @Test
    void testProtectExecuteSuccess() throws Exception {
        StubProtectOperation operation = new StubProtectOperation();
        
        List<File> files = new ArrayList<>();
        File file = tempDir.resolve("doc.pdf").toFile();
        Files.writeString(file.toPath(), "PDF");
        files.add(file);
        
        File outputFolder = tempDir.resolve("output").toFile();
        outputFolder.mkdirs();
        
        Map<String, Object> params = new HashMap<>();
        params.put("password", "secure123");
        
        Task<File> task = operation.execute(files, outputFolder, "protected.pdf", params);
        
        // Execute task using helper
        File result = TaskTestHelper.executeTask(task);
        
        assertNotNull(result);
    }
}
