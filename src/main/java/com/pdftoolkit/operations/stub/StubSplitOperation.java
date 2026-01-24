package com.pdftoolkit.operations.stub;

import com.pdftoolkit.operations.PdfOperation;
import javafx.concurrent.Task;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Stub implementation of split operation for UI testing.
 */
public class StubSplitOperation implements PdfOperation {

    @Override
    public Task<File> execute(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                String splitMode = (String) parameters.get("splitMode");
                String rangeInput = (String) parameters.get("rangeInput");
                
                updateMessage("Analyzing PDF structure...");
                updateProgress(0, 100);
                Thread.sleep(600);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Splitting pages (" + splitMode + ")...");
                updateProgress(50, 100);
                Thread.sleep(1200);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Generating output files...");
                updateProgress(90, 100);
                Thread.sleep(500);
                
                updateProgress(100, 100);
                updateMessage("Split complete!");
                
                // Simulate output folder (multiple files created)
                return outputFolder;
            }
        };
    }

    @Override
    public String getOperationName() {
        return "Split PDF";
    }

    @Override
    public String validate(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters) {
        if (inputFiles == null || inputFiles.isEmpty()) {
            return "Please select a PDF file to split";
        }
        if (inputFiles.size() > 1) {
            return "Please select only one PDF file to split";
        }
        if (outputFolder == null || !outputFolder.isDirectory()) {
            return "Please select a valid output folder";
        }
        
        String splitMode = (String) parameters.get("splitMode");
        String rangeInput = (String) parameters.get("rangeInput");
        
        if ("extractPages".equals(splitMode) || "splitByRanges".equals(splitMode)) {
            if (rangeInput == null || rangeInput.trim().isEmpty()) {
                return "Please enter page ranges";
            }
        }
        
        return null;
    }
}
