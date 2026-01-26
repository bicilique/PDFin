package com.pdftoolkit.controllers;

import com.pdftoolkit.models.PdfItem;
import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.services.PdfMergeService;
import com.pdftoolkit.services.PdfThumbnailService;
import com.pdftoolkit.ui.PdfItemCell;
import com.pdftoolkit.utils.AppPaths;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redesigned Merge PDF Controller with modern two-panel UI.
 * LEFT: Draggable PDF list (ListView with custom cells)
 * RIGHT: Configuration + Primary CTA
 * 
 * Key improvements:
 * - Uses AppState.MergeToolState to persist data across language changes
 * - ListView with custom PdfItemCell (no full re-render on add)
 * - Thumbnail caching (existing items don't reload)
 * - Remove button per item
 * - Drag & drop: reorder within list + drag from OS
 * - Async metadata loading (non-blocking UI)
 */
public class MergeControllerRedesigned {

    @FXML private ListView<PdfItem> pdfListView;
    @FXML private StackPane emptyStatePane;
    @FXML private Label instructionLabel;
    @FXML private Label filesCountLabel;
    @FXML private Label totalPagesLabel;
    @FXML private TextField outputFolderField;
    @FXML private TextField outputFilenameField;
    @FXML private Button addPdfButton;
    @FXML private Button mergeButton;
    @FXML private Button backButton;
    
    // Progress overlay elements
    @FXML private StackPane progressOverlay;
    @FXML private Label progressTitle;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressMessage;
    @FXML private Button cancelProcessButton;
    @FXML private VBox successPane;
    @FXML private Label successMessage;
    @FXML private Button openFolderButton;
    @FXML private Button processAnotherButton;

    // Use AppState to persist data across language changes
    private final AppState.MergeToolState state = AppState.getInstance().getMergeToolState();
    private final ObservableList<PdfItem> pdfItems = state.getPdfItems();
    
    private final PdfMergeService mergeService = new PdfMergeService();
    private final PdfThumbnailService thumbnailService = new PdfThumbnailService();
    
    private Task<File> currentTask;
    private File lastOutputFile;
    private int draggedIndex = -1;

    @FXML
    private void initialize() {
        // Set default output folder using AppPaths (if not already set)
        if (state.getOutputFolder() == null || state.getOutputFolder().isEmpty()) {
            state.setOutputFolder(AppPaths.getDefaultOutputPath());
        }
        if (state.getOutputFilename() == null || state.getOutputFilename().isEmpty()) {
            state.setOutputFilename("merged.pdf");
        }
        
        // Bind output fields to state (persists across language changes)
        outputFolderField.setText(state.getOutputFolder());
        outputFilenameField.setText(state.getOutputFilename());
        
        outputFolderField.textProperty().addListener((obs, oldVal, newVal) -> 
            state.setOutputFolder(newVal));
        outputFilenameField.textProperty().addListener((obs, oldVal, newVal) -> 
            state.setOutputFilename(newVal));
        
        // Setup ListView with custom cell factory
        pdfListView.setItems(pdfItems);
        pdfListView.setCellFactory(lv -> new PdfItemCell(this::handleRemoveItem));
        
        // Setup listeners
        pdfItems.addListener((ListChangeListener<PdfItem>) c -> updateUI());
        
        // Setup drag & drop
        setupDragAndDrop();
        
        // Setup locale change listener (only update UI text, don't clear data)
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> updateUIText());
        
        // Setup ListView drag and drop for reordering
        setupListViewReorder();
        
        // Initial UI update
        updateUI();
    }
    
    /**
     * Setup drag & drop from OS (external files).
     */
    private void setupDragAndDrop() {
        // External drag from OS onto ListView
        pdfListView.setOnDragOver(this::handleExternalDragOver);
        pdfListView.setOnDragDropped(this::handleExternalDragDropped);
        
        // External drag from OS onto empty state
        emptyStatePane.setOnDragOver(this::handleExternalDragOver);
        emptyStatePane.setOnDragDropped(this::handleExternalDragDropped);
    }
    
    /**
     * Setup ListView drag and drop for internal reordering.
     */
    private void setupListViewReorder() {
        pdfListView.setOnDragDetected(event -> {
            if (pdfListView.getSelectionModel().getSelectedItem() == null) return;
            
            draggedIndex = pdfListView.getSelectionModel().getSelectedIndex();
            Dragboard db = pdfListView.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(draggedIndex));
            db.setContent(content);
            
            event.consume();
        });
        
        pdfListView.setOnDragOver(event -> {
            if (event.getGestureSource() == pdfListView && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        
        pdfListView.setOnDragDropped(event -> {
            if (!event.getDragboard().hasString()) {
                event.setDropCompleted(false);
                event.consume();
                return;
            }
            
            // Find the drop target index
            int dropIndex = getDropIndex(event.getY());
            if (dropIndex == -1 || dropIndex == draggedIndex) {
                event.setDropCompleted(false);
                event.consume();
                return;
            }
            
            // Perform reorder
            PdfItem draggedItem = pdfItems.remove(draggedIndex);
            int targetIndex = draggedIndex < dropIndex ? dropIndex - 1 : dropIndex;
            pdfItems.add(targetIndex, draggedItem);
            
            // Restore selection
            pdfListView.getSelectionModel().select(targetIndex);
            
            event.setDropCompleted(true);
            event.consume();
        });
        
        pdfListView.setOnDragDone(DragEvent::consume);
    }
    
    /**
     * Calculate drop index based on Y coordinate.
     */
    private int getDropIndex(double y) {
        int size = pdfItems.size();
        if (size == 0) return -1;
        
        // Estimate cell height (should match cell height in ListView)
        double estimatedCellHeight = 90; // Adjust based on actual cell height
        int index = (int) (y / estimatedCellHeight);
        return Math.min(Math.max(0, index), size);
    }
    
    /**
     * Update UI (visibility, summary, button states).
     * This does NOT rebuild the list - ListView handles that automatically.
     */
    private void updateUI() {
        boolean hasPdfs = !pdfItems.isEmpty();
        
        // Toggle empty state
        emptyStatePane.setVisible(!hasPdfs);
        emptyStatePane.setManaged(!hasPdfs);
        pdfListView.setVisible(hasPdfs);
        pdfListView.setManaged(hasPdfs);
        
        // Update summary
        int totalPages = pdfItems.stream()
            .mapToInt(PdfItem::getPageCount)
            .sum();
        
        filesCountLabel.setText(pdfItems.size() + " " + LocaleManager.getString("merge.files"));
        totalPagesLabel.setText(totalPages + " " + LocaleManager.getString("common.pages"));
        
        // Enable/disable merge button
        boolean hasErrors = pdfItems.stream().anyMatch(PdfItem::hasError);
        boolean isLoading = pdfItems.stream().anyMatch(PdfItem::isLoading);
        mergeButton.setDisable(pdfItems.size() < 2 || hasErrors || isLoading);
    }
    
    /**
     * Update only UI text (on locale change). Does NOT touch data.
     */
    private void updateUIText() {
        // Update static labels via binding or direct call
        int totalPages = pdfItems.stream()
            .mapToInt(PdfItem::getPageCount)
            .sum();
        
        filesCountLabel.setText(pdfItems.size() + " " + LocaleManager.getString("merge.files"));
        totalPagesLabel.setText(totalPages + " " + LocaleManager.getString("common.pages"));
        
        // Update button/label text if needed
        addPdfButton.setText(LocaleManager.getString("merge.addFiles"));
        mergeButton.setText(LocaleManager.getString("merge.merge"));
        backButton.setText(LocaleManager.getString("common.back"));
        
        // Instruction label
        instructionLabel.setText(LocaleManager.getString("merge.dropInstruction"));
        
        // ListView cells will update automatically via their bindings
    }
    
    /**
     * Handle drag over from OS (external files).
     */
    private void handleExternalDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            List<File> pdfFiles = db.getFiles().stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());
            if (!pdfFiles.isEmpty()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
        }
        event.consume();
    }
    
    /**
     * Handle drop from OS (external files).
     */
    private void handleExternalDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        
        if (db.hasFiles()) {
            List<File> allFiles = db.getFiles();
            List<File> pdfOnly = allFiles.stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());
            
            if (!pdfOnly.isEmpty()) {
                // Filter out duplicates
                List<File> newFiles = pdfOnly.stream()
                    .filter(f -> pdfItems.stream()
                        .noneMatch(item -> item.getPath().toFile().getAbsolutePath()
                            .equals(f.getAbsolutePath())))
                    .collect(Collectors.toList());
                
                // Add new items and load metadata async
                for (File file : newFiles) {
                    addPdfItem(file);
                }
                
                success = !newFiles.isEmpty();
                
                // Show feedback if non-PDF or duplicates were dropped
                if (pdfOnly.size() < allFiles.size()) {
                    System.out.println("Some non-PDF files were ignored");
                }
                if (newFiles.size() < pdfOnly.size()) {
                    System.out.println("Some duplicate files were ignored");
                }
            }
        }
        
        event.setDropCompleted(success);
        event.consume();
    }
    
    /**
     * Add a PDF file to the list and load metadata asynchronously.
     * This does NOT trigger full list re-render - only appends the new item.
     */
    private void addPdfItem(File file) {
        // Create PdfItem with initial values
        PdfItem item = new PdfItem(file.toPath());
        item.setLoading(true);
        
        // Add to list immediately (ListView will render it)
        pdfItems.add(item);
        
        // Load metadata asynchronously (non-blocking)
        loadPdfMetadataAsync(item);
    }
    
    /**
     * Load PDF metadata (size, page count, thumbnail) asynchronously.
     * Uses cached thumbnails if available.
     */
    private void loadPdfMetadataAsync(PdfItem item) {
        Task<Void> loadTask = new Task<>() {
            private long fileSize;
            private int pageCount;
            private Image thumbnail;
            private String error;
            
            @Override
            protected Void call() {
                try {
                    File file = item.getPath().toFile();
                    
                    // Load file size
                    fileSize = file.length();
                    
                    // Load page count
                    pageCount = thumbnailService.getPageCount(file);
                    
                    // Load thumbnail (will use cache if available)
                    thumbnail = thumbnailService.generateThumbnail(file, 0);
                    
                } catch (Exception e) {
                    error = "Failed to read PDF: " + e.getMessage();
                    System.err.println("Error loading PDF metadata for: " + 
                        item.getFileName() + " - " + e.getMessage());
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    item.setFileSizeBytes(fileSize);
                    item.setPageCount(pageCount);
                    item.setThumbnail(thumbnail);
                    item.setLoading(false);
                    if (error != null) {
                        item.setError(error);
                    }
                    
                    // Update summary
                    updateUI();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    item.setError("Failed to load PDF");
                    item.setLoading(false);
                    updateUI();
                });
            }
        };
        
        // Run in background thread
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Handle remove button click from PdfItemCell.
     */
    private void handleRemoveItem(PdfItem item) {
        if (item != null) {
            pdfItems.remove(item);
            
            // Optional: clear cached thumbnail
            thumbnailService.removeCachedThumbnails(item.getPath().toFile());
        }
    }
    
    @FXML
    private void handleAddPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LocaleManager.getString("merge.addFiles"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(
            pdfListView.getScene().getWindow()
        );
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            // Filter out duplicates
            List<File> newFiles = selectedFiles.stream()
                .filter(f -> pdfItems.stream()
                    .noneMatch(item -> item.getPath().toFile().getAbsolutePath()
                        .equals(f.getAbsolutePath())))
                .collect(Collectors.toList());
            
            // Add each file
            for (File file : newFiles) {
                addPdfItem(file);
            }
        }
    }
    
    @FXML
    private void handleBrowseOutputFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LocaleManager.getString("merge.chooseFolder"));
        
        File currentFolder = new File(outputFolderField.getText());
        if (currentFolder.exists() && currentFolder.isDirectory()) {
            chooser.setInitialDirectory(currentFolder);
        }
        
        File selectedDir = chooser.showDialog(outputFolderField.getScene().getWindow());
        if (selectedDir != null) {
            outputFolderField.setText(selectedDir.getAbsolutePath());
        }
    }
    
    @FXML
    private void handleMerge() {
        // Validation
        if (pdfItems.size() < 2) {
            showAlert(LocaleManager.getString("validation.noFiles"), Alert.AlertType.WARNING);
            return;
        }
        
        // Check for errors or loading items
        if (pdfItems.stream().anyMatch(PdfItem::hasError)) {
            showAlert("Some PDF files have errors. Please remove them and try again.", Alert.AlertType.WARNING);
            return;
        }
        
        if (pdfItems.stream().anyMatch(PdfItem::isLoading)) {
            showAlert("Please wait for all files to finish loading.", Alert.AlertType.WARNING);
            return;
        }
        
        String outputPath = outputFolderField.getText();
        if (outputPath == null || outputPath.isEmpty()) {
            showAlert(LocaleManager.getString("validation.invalidFolder"), Alert.AlertType.WARNING);
            return;
        }
        
        String filename = outputFilenameField.getText();
        if (filename == null || filename.trim().isEmpty()) {
            filename = "merged.pdf";
        }
        
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename += ".pdf";
        }
        
        // Show progress overlay
        showProgressOverlay();
        
        // Create and execute real merge task
        final String finalFilename = filename;
        final File outputDir = new File(outputPath);
        final File outputFile = new File(outputDir, finalFilename);
        
        // Convert PdfItems to Files
        final List<File> files = pdfItems.stream()
            .map(item -> item.getPath().toFile())
            .collect(Collectors.toList());
        
        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Preparing to merge " + files.size() + " files...");
                updateProgress(0, files.size() + 1);
                
                Thread.sleep(300); // Brief pause for UI feedback
                
                // Process each file
                for (int i = 0; i < files.size(); i++) {
                    if (isCancelled()) {
                        updateMessage("Cancelled");
                        return null;
                    }
                    
                    String fileName = files.get(i).getName();
                    updateMessage(String.format("Processing file %d of %d: %s", 
                                               i + 1, files.size(), fileName));
                    updateProgress(i + 1, files.size() + 1);
                    Thread.sleep(200); // Simulate processing time
                }
                
                // Perform actual merge using PdfMergeService
                updateMessage("Merging PDFs...");
                mergeService.mergePdfs(files, outputFile);
                
                updateMessage("Writing merged PDF to disk...");
                updateProgress(files.size() + 1, files.size() + 1);
                Thread.sleep(200);
                
                updateMessage("Merge complete!");
                return outputFile;
            }
        };
        
        // Bind progress
        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressMessage.textProperty().bind(currentTask.messageProperty());
        
        currentTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                File result = currentTask.getValue();
                if (result != null) {
                    lastOutputFile = result;
                    showSuccess(LocaleManager.getString("merge.success") + "\n\n" + result.getName());
                }
            });
        });
        
        currentTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                hideProgressOverlay();
                Throwable ex = currentTask.getException();
                showAlert("Merge Error: " + (ex != null ? ex.getMessage() : "Unknown error"), 
                         Alert.AlertType.ERROR);
            });
        });
        
        currentTask.setOnCancelled(e -> {
            Platform.runLater(() -> {
                hideProgressOverlay();
                showAlert(LocaleManager.getString("progress.canceling"), Alert.AlertType.INFORMATION);
            });
        });
        
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    @FXML
    private void handleCancelProcess() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
    }
    
    @FXML
    private void handleOpenFolder() {
        if (lastOutputFile != null && lastOutputFile.getParentFile() != null) {
            try {
                java.awt.Desktop.getDesktop().open(lastOutputFile.getParentFile());
            } catch (Exception ex) {
                showAlert("Cannot open folder: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    @FXML
    private void handleProcessAnother() {
        hideProgressOverlay();
        pdfItems.clear();
        state.setOutputFilename("merged.pdf");
        outputFilenameField.setText("merged.pdf");
        
        // Clear thumbnail cache
        thumbnailService.clearCache();
    }
    
    @FXML
    private void handleBack() {
        AppNavigator.navigateToHome();
    }
    
    private void showProgressOverlay() {
        progressOverlay.setVisible(true);
        progressOverlay.setManaged(true);
        successPane.setVisible(false);
        successPane.setManaged(false);
        cancelProcessButton.setVisible(true);
        cancelProcessButton.setManaged(true);
    }
    
    private void hideProgressOverlay() {
        progressOverlay.setVisible(false);
        progressOverlay.setManaged(false);
    }
    
    private void showSuccess(String message) {
        cancelProcessButton.setVisible(false);
        cancelProcessButton.setManaged(false);
        successPane.setVisible(true);
        successPane.setManaged(true);
        successMessage.setText(message);
    }
    
    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
