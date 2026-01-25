package com.pdftoolkit.operations.stub;

import com.pdftoolkit.operations.PdfOperation;
import javafx.concurrent.Task;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Stub implementation of merge operation for UI testing.
 * Simulates work with progress updates without actual PDF processing.
 */
public class StubMergeOperation implements PdfOperation {

    @Override
    public Task<File> execute(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Preparing to merge " + inputFiles.size() + " files...");
                updateProgress(0, inputFiles.size() + 1);
                
                Thread.sleep(500);
                
                for (int i = 0; i < inputFiles.size(); i++) {
                    if (isCancelled()) {
                        updateMessage("Cancelled");
                        return null;
                    }
                    
                    String fileName = inputFiles.get(i).getName();
                    updateMessage(String.format("Merging file %d of %d: %s", 
                                               i + 1, inputFiles.size(), fileName));
                    updateProgress(i + 1, inputFiles.size() + 1);
                    Thread.sleep(800);
                }
                
                updateMessage("Writing merged PDF to disk...");
                updateProgress(inputFiles.size() + 1, inputFiles.size() + 1);
                Thread.sleep(500);
                
                // Simulate output file
                File outputFile = new File(outputFolder, outputFileName);
                updateMessage("Merge complete!");
                
                return outputFile;
            }
        };
    }

    @Override
    public String getOperationName() {
        return "Merge PDF";
    }

    @Override
    public String validate(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters) {
        if (inputFiles == null || inputFiles.size() < 2) {
            return "Please select at least 2 PDF files to merge";
        }
        if (outputFolder == null || !outputFolder.isDirectory()) {
            return "Please select a valid output folder";
        }
        if (outputFileName == null || outputFileName.trim().isEmpty()) {
            return "Please enter an output file name";
        }
        return null;
    }
}
