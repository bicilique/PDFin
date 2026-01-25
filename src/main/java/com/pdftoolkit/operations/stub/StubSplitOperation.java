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
                updateProgress(0, 5);
                Thread.sleep(600);
                
                if (isCancelled()) {
                    return null;
                }
                
                // Count ranges to split
                int rangeCount = countRanges(rangeInput);
                
                updateMessage("Preparing to split into " + rangeCount + " ranges...");
                updateProgress(1, rangeCount + 2);
                Thread.sleep(400);
                
                // Simulate processing each range
                for (int i = 0; i < rangeCount; i++) {
                    if (isCancelled()) {
                        return null;
                    }
                    
                    updateMessage(String.format("Processing range %d of %d...", i + 1, rangeCount));
                    updateProgress(i + 2, rangeCount + 2);
                    Thread.sleep(800);
                }
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Writing output files to disk...");
                updateProgress(rangeCount + 1, rangeCount + 2);
                Thread.sleep(500);
                
                updateProgress(rangeCount + 2, rangeCount + 2);
                updateMessage("Split complete! Created " + rangeCount + " files.");
                
                // Simulate output folder (multiple files created)
                return outputFolder;
            }
            
            private int countRanges(String rangeInput) {
                if (rangeInput == null || rangeInput.trim().isEmpty()) {
                    return 1;
                }
                // Count commas + 1 for simple range counting
                return rangeInput.split(",").length;
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
