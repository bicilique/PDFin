package com.pdftoolkit.operations.stub;

import com.pdftoolkit.operations.PdfOperation;
import javafx.concurrent.Task;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Stub implementation of protect operation for UI testing.
 */
public class StubProtectOperation implements PdfOperation {

    @Override
    public Task<File> execute(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                String password = (String) parameters.get("password");
                
                updateMessage("Reading PDF structure...");
                updateProgress(0, 100);
                Thread.sleep(500);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Applying encryption...");
                updateProgress(50, 100);
                Thread.sleep(1000);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Writing protected file...");
                updateProgress(90, 100);
                Thread.sleep(500);
                
                updateProgress(100, 100);
                updateMessage("Password protection applied!");
                
                File outputFile = new File(outputFolder, outputFileName);
                return outputFile;
            }
        };
    }

    @Override
    public String getOperationName() {
        return "Protect PDF";
    }

    @Override
    public String validate(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters) {
        if (inputFiles == null || inputFiles.isEmpty()) {
            return "Please select at least one PDF file to protect";
        }
        if (outputFolder == null || !outputFolder.isDirectory()) {
            return "Please select a valid output folder";
        }
        if (outputFileName == null || outputFileName.trim().isEmpty()) {
            return "Please enter an output file name";
        }
        
        String password = (String) parameters.get("password");
        String confirmPassword = (String) parameters.get("confirmPassword");
        
        if (password == null || password.isEmpty()) {
            return "Please enter a password";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match";
        }
        
        return null;
    }
}
