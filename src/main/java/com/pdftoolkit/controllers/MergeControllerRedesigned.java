package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.services.PdfMergeService;
import com.pdftoolkit.services.PdfThumbnailService;
import com.pdftoolkit.ui.PdfFileCard;
import com.pdftoolkit.utils.AppPaths;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redesigned Merge PDF Controller with modern two-panel UI.
 * LEFT: Draggable PDF card list
 * RIGHT: Configuration + Primary CTA
 * 
 * Now uses real PDF services (PdfMergeService, PdfThumbnailService).
 */
public class MergeControllerRedesigned {

    @FXML private VBox cardListBox;
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

    private final ObservableList<File> pdfFiles = FXCollections.observableArrayList();
    private final PdfMergeService mergeService = new PdfMergeService();
    private final PdfThumbnailService thumbnailService = new PdfThumbnailService();
    private Task<File> currentTask;
    private File lastOutputFile;
    private PdfFileCard draggedCard;
    private int draggedIndex = -1;
    private Label nonPdfFeedbackLabel;

    @FXML
    private void initialize() {
        // Set default output folder using AppPaths
        outputFolderField.setText(AppPaths.getDefaultOutputPath());
        
        // Setup listeners
        pdfFiles.addListener((ListChangeListener<File>) c -> updateUI());
        
        // Setup drag & drop on empty state and card list
        setupDragAndDrop();
        
        // Setup locale change listener
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> updateUI());
        
        // Initial UI update
        updateUI();
    }
    
    private void setupDragAndDrop() {
        // External drag from OS
        cardListBox.setOnDragOver(this::handleExternalDragOver);
        cardListBox.setOnDragDropped(this::handleExternalDragDropped);
        emptyStatePane.setOnDragOver(this::handleExternalDragOver);
        emptyStatePane.setOnDragDropped(this::handleExternalDragDropped);
    }
    
    private void updateUI() {
        boolean hasPdfs = !pdfFiles.isEmpty();
        
        // Toggle empty state
        emptyStatePane.setVisible(!hasPdfs);
        emptyStatePane.setManaged(!hasPdfs);
        
        // Update summary
        int totalPages = pdfFiles.size() * 10; // Placeholder (would read from PDF metadata)
        filesCountLabel.setText(pdfFiles.size() + " " + LocaleManager.getString("merge.files"));
        totalPagesLabel.setText(totalPages + " pages"); // Would use actual page count
        
        // Enable/disable merge button
        mergeButton.setDisable(pdfFiles.size() < 2);
        
        // Rebuild card list
        rebuildCardList();
    }
    
    private void rebuildCardList() {
        cardListBox.getChildren().clear();
        
        for (int i = 0; i < pdfFiles.size(); i++) {
            File file = pdfFiles.get(i);
            PdfFileCard card = new PdfFileCard(file);
            
            // Load page count and thumbnail asynchronously
            loadCardDataAsync(card, file);
            
            // Setup internal drag to reorder
            final int index = i;
            setupCardDragAndDrop(card, index);
            
            cardListBox.getChildren().add(card);
        }
    }
    
    private void loadCardDataAsync(PdfFileCard card, File file) {
        Task<Void> loadTask = new Task<>() {
            private int pageCount;
            private javafx.scene.image.Image thumbnail;
            
            @Override
            protected Void call() throws Exception {
                // Load page count
                pageCount = thumbnailService.getPageCount(file);
                
                // Load thumbnail
                thumbnail = thumbnailService.generateThumbnail(file);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                card.setPageCount(pageCount);
                if (thumbnail != null) {
                    card.setThumbnail(thumbnail);
                }
            }
            
            @Override
            protected void failed() {
                System.err.println("Failed to load data for: " + file.getName());
                card.setPageCount(0);
            }
        };
        
        // Run async
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void setupCardDragAndDrop(PdfFileCard card, int index) {
        // Drag detected - start internal reorder
        card.setOnDragDetected(event -> {
            if (pdfFiles.size() <= 1) return;
            
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(index));
            db.setContent(content);
            
            draggedCard = card;
            draggedIndex = index;
            card.getStyleClass().add("dragging");
            event.consume();
        });
        
        // Drag over - show drop indicator
        card.setOnDragOver(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        
        // Drop - reorder list
        card.setOnDragDropped(event -> {
            if (draggedCard != null && draggedIndex != -1) {
                int targetIndex = cardListBox.getChildren().indexOf(card);
                if (targetIndex != -1 && targetIndex != draggedIndex) {
                    // Reorder in ObservableList
                    File draggedFile = pdfFiles.remove(draggedIndex);
                    
                    // Adjust target index if dragging down
                    int insertIndex = draggedIndex < targetIndex ? targetIndex : targetIndex;
                    pdfFiles.add(insertIndex, draggedFile);
                    
                    event.setDropCompleted(true);
                }
            }
            event.consume();
        });
        
        // Drag done - cleanup
        card.setOnDragDone(event -> {
            if (draggedCard != null) {
                draggedCard.getStyleClass().remove("dragging");
            }
            draggedCard = null;
            draggedIndex = -1;
            event.consume();
        });
    }
    
    // External drag & drop from OS
    private void handleExternalDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            List<File> pdfFiles = db.getFiles().stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());
            if (!pdfFiles.isEmpty()) {
                event.acceptTransferModes(TransferMode.COPY);
                // Add visual feedback
                Node target = (Node) event.getSource();
                if (!target.getStyleClass().contains("drag-over")) {
                    target.getStyleClass().add("drag-over");
                }
            }
        }
        event.consume();
    }
    
    private void handleExternalDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        
        if (db.hasFiles()) {
            List<File> allFiles = db.getFiles();
            List<File> pdfOnly = allFiles.stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());
            
            pdfFiles.addAll(pdfOnly);
            success = !pdfOnly.isEmpty();
            
            // Show feedback if non-PDF files were dropped
            if (pdfOnly.size() < allFiles.size()) {
                showNonPdfFeedback();
            }
        }
        
        event.setDropCompleted(success);
        
        // Remove visual feedback
        Node target = (Node) event.getSource();
        target.getStyleClass().remove("drag-over");
        event.consume();
    }
    
    private void showNonPdfFeedback() {
        if (nonPdfFeedbackLabel == null) {
            nonPdfFeedbackLabel = new Label(LocaleManager.getString("merge.nonPdfIgnored"));
            nonPdfFeedbackLabel.getStyleClass().add("validation-label");
            nonPdfFeedbackLabel.getStyleClass().add("warning");
        }
        
        // Would show this in UI - for now just log
        System.out.println("Non-PDF files were ignored");
    }
    
    @FXML
    private void handleAddPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LocaleManager.getString("merge.addFiles"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(
            cardListBox.getScene().getWindow()
        );
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            pdfFiles.addAll(selectedFiles);
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
        if (pdfFiles.size() < 2) {
            showAlert(LocaleManager.getString("validation.noFiles"), Alert.AlertType.WARNING);
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
        
        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Preparing to merge " + pdfFiles.size() + " files...");
                updateProgress(0, pdfFiles.size() + 1);
                
                Thread.sleep(300); // Brief pause for UI feedback
                
                // Process each file
                for (int i = 0; i < pdfFiles.size(); i++) {
                    if (isCancelled()) {
                        updateMessage("Cancelled");
                        return null;
                    }
                    
                    String fileName = pdfFiles.get(i).getName();
                    updateMessage(String.format("Processing file %d of %d: %s", 
                                               i + 1, pdfFiles.size(), fileName));
                    updateProgress(i + 1, pdfFiles.size() + 1);
                    Thread.sleep(200); // Simulate processing time
                }
                
                // Perform actual merge using PdfMergeService
                updateMessage("Merging PDFs...");
                mergeService.mergePdfs(pdfFiles, outputFile);
                
                updateMessage("Writing merged PDF to disk...");
                updateProgress(pdfFiles.size() + 1, pdfFiles.size() + 1);
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
        pdfFiles.clear();
        outputFilenameField.setText("merged.pdf");
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
