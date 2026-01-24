package com.pdftoolkit.operations.stub;

import com.pdftoolkit.operations.PdfOperation;
import javafx.concurrent.Task;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Stub implementation of compress operation for UI testing.
 */
public class StubCompressOperation implements PdfOperation {

    @Override
    public Task<File> execute(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters) {
        return new Task<>() {
            @Override
            protected File call() throws Exception {
                int compressionLevel = (int) parameters.get("compressionLevel");
                
                updateMessage("Analyzing file size...");
                updateProgress(0, 100);
                Thread.sleep(500);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Compressing images (Level: " + compressionLevel + ")...");
                updateProgress(30, 100);
                Thread.sleep(1000);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Optimizing fonts and resources...");
                updateProgress(60, 100);
                Thread.sleep(800);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Writing compressed file...");
                updateProgress(90, 100);
                Thread.sleep(500);
                
                updateProgress(100, 100);
                updateMessage("Compression complete! Reduced by ~" + (20 + compressionLevel * 10) + "%");
                
                File outputFile = new File(outputFolder, outputFileName);
                return outputFile;
            }
        };
    }

    @Override
    public String getOperationName() {
        return "Compress PDF";
    }

    @Override
    public String validate(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters) {
        if (inputFiles == null || inputFiles.isEmpty()) {
            return "Please select at least one PDF file to compress";
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
