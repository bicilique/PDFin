package com.pdftoolkit.operations;

import javafx.concurrent.Task;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Interface for PDF operations.
 * Each operation returns a JavaFX Task that can be executed on a background thread.
 * 
 * Implementations should:
 * - Update task progress and message
 * - Handle cancellation
 * - Return output file on success
 * - Throw exceptions on failure
 */
public interface PdfOperation {

    /**
     * Execute the operation with given inputs and parameters.
     * 
     * @param inputFiles List of input PDF files
     * @param outputFolder Destination folder for output
     * @param outputFileName Name pattern for output file(s)
     * @param parameters Operation-specific parameters (compression level, page ranges, password, etc.)
     * @return Task that performs the operation and returns the output file
     */
    Task<File> execute(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters);

    /**
     * Get the display name of this operation.
     */
    String getOperationName();

    /**
     * Validate inputs before execution.
     * 
     * @return Validation error message, or null if valid
     */
    String validate(List<File> inputFiles, File outputFolder, String outputFileName, Map<String, Object> parameters);
}
