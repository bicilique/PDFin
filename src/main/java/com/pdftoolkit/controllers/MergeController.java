package com.pdftoolkit.controllers;

import com.pdftoolkit.models.PdfItem;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.services.PdfMergeService;
import com.pdftoolkit.services.PdfPreviewService;
import com.pdftoolkit.ui.CustomDialog;
import com.pdftoolkit.utils.AppPaths;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redesigned Merge PDF Controller following the Protect feature's style.
 * Two-pane layout with improved file input UX:
 * - LEFT: File staging area with drag-drop and file cards
 * - RIGHT: Settings panel with output options and merge action
 * 
 * Key improvements:
 * - File cards instead of ListView for better visual hierarchy
 * - Large drop zone when empty
 * - Smooth transitions between empty and populated states
 * - Drag & drop support for both adding and reordering files
 * - Persistent state management
 * - Async metadata loading
 */
public class MergeController {

    // LEFT PANE: Files Area
    @FXML private Button clearFilesButton;
    @FXML private StackPane dropZonePane;
    @FXML private Button selectFilesButton;
    @FXML private ScrollPane filesScrollPane;
    @FXML private VBox filesContainer;
    @FXML private HBox addFilesToolbar;
    @FXML private Button addMoreFilesButton;
    @FXML private Label filesCountLabel;
    
    // RIGHT PANE: Settings
    @FXML private Label totalPagesLabel;
    @FXML private Label totalSizeLabel;
    @FXML private TextField outputFolderField;
    @FXML private Button browseFolderButton;
    @FXML private TextField outputFilenameField;
    @FXML private Button mergeButton;
    @FXML private Button resetButton;
    
    // OVERLAYS (unified like Split/Compress)
    @FXML private StackPane progressOverlay;
    @FXML private Label progressTitle;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressMessage;
    @FXML private Button cancelProcessButton;
    @FXML private VBox successPane;
    @FXML private Label successMessage;
    @FXML private Button openFolderButton;
    @FXML private Button mergeAnotherButton;
    @FXML private Button closeSuccessButton;
    
    // Data and Services
    private final ObservableList<PdfItem> selectedFiles = FXCollections.observableArrayList();
    private final PdfMergeService mergeService = new PdfMergeService();
    private final PdfPreviewService previewService = PdfPreviewService.getInstance();
    private final AppState.MergeToolState state = AppState.getInstance().getMergeToolState();
    
    private Task<File> mergeTask;
    private File lastOutputFile;
    private int draggedIndex = -1;

    @FXML
    public void initialize() {
        setupOutputFolder();
        setupDragAndDrop();
        setupValidationBinding();
        setupFileListListeners();
        updateFilesView();
        
        // Hide overlays initially
        if (progressOverlay != null) {
            progressOverlay.setVisible(false);
            progressOverlay.setManaged(false);
        }
        
        // Setup locale change listener
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> updateTexts());
        
        // Initial text update
        updateTexts();
    }
    
    /**
     * Setup output folder with defaults.
     */
    private void setupOutputFolder() {
        if (state.getOutputFolder() == null || state.getOutputFolder().isEmpty()) {
            state.setOutputFolder(AppPaths.getDefaultOutputPath());
        }
        if (state.getOutputFilename() == null || state.getOutputFilename().isEmpty()) {
            state.setOutputFilename("merged.pdf");
        }
        
        outputFolderField.setText(state.getOutputFolder());
        outputFilenameField.setText(state.getOutputFilename());
        
        // Bind to state
        outputFolderField.textProperty().addListener((obs, oldVal, newVal) -> 
            state.setOutputFolder(newVal));
        outputFilenameField.textProperty().addListener((obs, oldVal, newVal) -> 
            state.setOutputFilename(newVal));
    }
    
    /**
     * Setup drag and drop for file input.
     */
    private void setupDragAndDrop() {
        // Drop zone drag and drop
        dropZonePane.setOnDragOver(this::handleDragOver);
        dropZonePane.setOnDragDropped(this::handleDragDropped);
        
        // Files container drag and drop
        filesScrollPane.setOnDragOver(this::handleDragOver);
        filesScrollPane.setOnDragDropped(this::handleDragDropped);
    }
    
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            List<File> pdfFiles = event.getDragboard().getFiles().stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());
            if (!pdfFiles.isEmpty()) {
                event.acceptTransferModes(TransferMode.COPY);
                
                // Add visual feedback
                if (!dropZonePane.getStyleClass().contains("drag-over")) {
                    dropZonePane.getStyleClass().add("drag-over");
                }
            }
        }
        event.consume();
    }
    
    private void handleDragDropped(DragEvent event) {
        // Remove visual feedback
        dropZonePane.getStyleClass().remove("drag-over");
        
        boolean success = false;
        if (event.getDragboard().hasFiles()) {
            List<File> pdfFiles = event.getDragboard().getFiles().stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());
            
            if (!pdfFiles.isEmpty()) {
                // Filter duplicates
                List<File> newFiles = pdfFiles.stream()
                    .filter(f -> selectedFiles.stream()
                        .noneMatch(item -> item.getPath().toFile().getAbsolutePath()
                            .equals(f.getAbsolutePath())))
                    .collect(Collectors.toList());
                
                for (File file : newFiles) {
                    addPdfFile(file);
                }
                
                success = !newFiles.isEmpty();
                
                // Show feedback for duplicates
                if (newFiles.size() < pdfFiles.size()) {
                    showInfo(String.format(
                        LocaleManager.getString("merge.duplicatesIgnored"),
                        pdfFiles.size() - newFiles.size()
                    ));
                }
            }
        }
        
        event.setDropCompleted(success);
        event.consume();
    }
    
    /**
     * Setup validation binding for merge button.
     */
    private void setupValidationBinding() {
        BooleanBinding shouldDisable = Bindings.createBooleanBinding(
            () -> {
                // Need at least 2 files
                if (selectedFiles.size() < 2) {
                    return true;
                }
                
                // Check if any file is loading
                if (selectedFiles.stream().anyMatch(PdfItem::isLoading)) {
                    return true;
                }
                
                // Check if any file has error
                if (selectedFiles.stream().anyMatch(PdfItem::hasError)) {
                    return true;
                }
                
                // Check output folder
                String folder = outputFolderField.getText();
                if (folder == null || folder.trim().isEmpty()) {
                    return true;
                }
                
                // Check output filename
                String filename = outputFilenameField.getText();
                if (filename == null || filename.trim().isEmpty()) {
                    return true;
                }
                
                return false;
            },
            selectedFiles,
            outputFolderField.textProperty(),
            outputFilenameField.textProperty()
        );
        
        mergeButton.disableProperty().bind(shouldDisable);
        
        // Listen to item property changes
        selectedFiles.addListener((ListChangeListener<PdfItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (PdfItem item : change.getAddedSubList()) {
                        item.loadingProperty().addListener((obs, oldVal, newVal) -> 
                            shouldDisable.invalidate());
                        item.errorProperty().addListener((obs, oldVal, newVal) -> 
                            shouldDisable.invalidate());
                    }
                }
            }
        });
    }
    
    /**
     * Setup file list change listeners.
     */
    private void setupFileListListeners() {
        selectedFiles.addListener((ListChangeListener<PdfItem>) change -> {
            updateFilesView();
            updateSummary();
        });
    }
    
    /**
     * Update UI texts for localization.
     */
    private void updateTexts() {
        updateSummary();
        updateFilesCountLabel();
    }
    
    /**
     * Update files view - toggle between empty state and file cards.
     */
    private void updateFilesView() {
        boolean isEmpty = selectedFiles.isEmpty();
        
        // Toggle visibility
        dropZonePane.setVisible(isEmpty);
        dropZonePane.setManaged(isEmpty);
        filesScrollPane.setVisible(!isEmpty);
        filesScrollPane.setManaged(!isEmpty);
        addFilesToolbar.setVisible(!isEmpty);
        addFilesToolbar.setManaged(!isEmpty);
        clearFilesButton.setVisible(!isEmpty);
        clearFilesButton.setManaged(!isEmpty);
        
        if (!isEmpty) {
            renderFileCards();
            updateFilesCountLabel();
        }
    }
    
    /**
     * Render file cards in the container.
     */
    private void renderFileCards() {
        filesContainer.getChildren().clear();
        
        for (int i = 0; i < selectedFiles.size(); i++) {
            PdfItem item = selectedFiles.get(i);
            VBox card = createFileCard(item, i);
            filesContainer.getChildren().add(card);
        }
    }
    
    /**
     * Create a file card UI component.
     */
    private VBox createFileCard(PdfItem item, int index) {
        VBox card = new VBox(12);
        card.getStyleClass().add("file-card");
        card.setPadding(new Insets(16));
        
        // Top row: thumbnail, info, and remove button
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        // Thumbnail
        StackPane thumbnailContainer = new StackPane();
        thumbnailContainer.getStyleClass().add("file-card-thumbnail");
        thumbnailContainer.setPrefSize(60, 80);
        thumbnailContainer.setMinSize(60, 80);
        thumbnailContainer.setMaxSize(60, 80);
        
        if (item.isLoading()) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(30, 30);
            thumbnailContainer.getChildren().add(spinner);
        } else if (item.getThumbnail() != null) {
            ImageView thumbnail = new ImageView(item.getThumbnail());
            thumbnail.setFitWidth(60);
            thumbnail.setFitHeight(80);
            thumbnail.setPreserveRatio(true);
            thumbnailContainer.getChildren().add(thumbnail);
        } else {
            Label placeholder = new Label("📄");
            placeholder.setStyle("-fx-font-size: 32px;");
            thumbnailContainer.getChildren().add(placeholder);
        }
        
        // File info
        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label fileNameLabel = new Label(item.getFileName());
        fileNameLabel.getStyleClass().add("file-card-name");
        fileNameLabel.setWrapText(false);
        fileNameLabel.setMaxWidth(Double.MAX_VALUE);
        
        HBox detailsRow = new HBox(12);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        
        if (item.getPageCount() > 0) {
            Label pagesLabel = new Label(item.getPageCount() + " " + 
                LocaleManager.getString("common.pages"));
            pagesLabel.getStyleClass().add("file-card-detail");
            detailsRow.getChildren().add(pagesLabel);
        }
        
        if (item.getFileSizeBytes() > 0) {
            Label sizeLabel = new Label(formatFileSize(item.getFileSizeBytes()));
            sizeLabel.getStyleClass().add("file-card-detail");
            detailsRow.getChildren().add(sizeLabel);
        }
        
        infoBox.getChildren().addAll(fileNameLabel, detailsRow);
        
        // Error label if present
        if (item.hasError()) {
            Label errorLabel = new Label("⚠ " + item.getError());
            errorLabel.getStyleClass().add("file-card-error");
            errorLabel.setWrapText(true);
            infoBox.getChildren().add(errorLabel);
        }
        
        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        // Move up button
        if (index > 0) {
            Button moveUpBtn = new Button("↑");
            moveUpBtn.getStyleClass().add("file-card-action-btn");
            moveUpBtn.setOnAction(e -> moveFileUp(index));
            actions.getChildren().add(moveUpBtn);
        }
        
        // Move down button
        if (index < selectedFiles.size() - 1) {
            Button moveDownBtn = new Button("↓");
            moveDownBtn.getStyleClass().add("file-card-action-btn");
            moveDownBtn.setOnAction(e -> moveFileDown(index));
            actions.getChildren().add(moveDownBtn);
        }
        
        // Remove button
        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("file-card-remove-btn");
        removeBtn.setOnAction(e -> removeFile(item));
        actions.getChildren().add(removeBtn);
        
        topRow.getChildren().addAll(thumbnailContainer, infoBox, actions);
        card.getChildren().add(topRow);
        
        // Setup drag-and-drop for reordering
        setupCardDragAndDrop(card, index);
        
        return card;
    }
    
    /**
     * Setup drag-and-drop handlers for a file card to enable reordering.
     */
    private void setupCardDragAndDrop(VBox card, int cardIndex) {
        // Make card draggable
        card.setOnDragDetected(event -> {
            if (selectedFiles.size() <= 1) return; // No point dragging single item
            
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(cardIndex));
            db.setContent(content);
            
            draggedIndex = cardIndex;
            card.setOpacity(0.5);
            event.consume();
        });
        
        card.setOnDragOver(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        
        card.setOnDragEntered(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                card.setStyle("-fx-border-color: #0969da; -fx-border-width: 2px;");
            }
            event.consume();
        });
        
        card.setOnDragExited(event -> {
            card.setStyle("");
            event.consume();
        });
        
        card.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasString() && draggedIndex >= 0 && draggedIndex != cardIndex) {
                // Reorder items
                PdfItem draggedItem = selectedFiles.remove(draggedIndex);
                
                // Adjust target index if needed
                int targetIndex = cardIndex;
                if (draggedIndex < cardIndex) {
                    targetIndex--;
                }
                
                selectedFiles.add(targetIndex, draggedItem);
                renderFileCards();
                success = true;
            }
            
            event.setDropCompleted(success);
            event.consume();
        });
        
        card.setOnDragDone(event -> {
            card.setOpacity(1.0);
            card.setStyle("");
            draggedIndex = -1;
            event.consume();
        });
    }
    
    /**
     * Update files count label.
     */
    private void updateFilesCountLabel() {
        int count = selectedFiles.size();
        if (count == 0) {
            filesCountLabel.setText(LocaleManager.getString("merge.noFiles"));
        } else if (count == 1) {
            filesCountLabel.setText("1 " + LocaleManager.getString("merge.file"));
        } else {
            filesCountLabel.setText(count + " " + LocaleManager.getString("merge.files"));
        }
    }
    
    /**
     * Update summary (total pages and size).
     */
    private void updateSummary() {
        int totalPages = selectedFiles.stream()
            .mapToInt(PdfItem::getPageCount)
            .sum();
        
        long totalBytes = selectedFiles.stream()
            .mapToLong(PdfItem::getFileSizeBytes)
            .sum();
        
        totalPagesLabel.setText(totalPages + " " + LocaleManager.getString("common.pages"));
        totalSizeLabel.setText(formatFileSize(totalBytes));
    }
    
    /**
     * Format file size in human-readable format.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * Add a PDF file to the list.
     */
    private void addPdfFile(File file) {
        PdfItem item = new PdfItem(file.toPath());
        item.setLoading(true);
        selectedFiles.add(item);
        
        // Load metadata asynchronously
        previewService.loadMetadataAsync(item).thenRun(() -> {
            // Re-render file cards and update summary when thumbnail is loaded
            Platform.runLater(() -> {
                renderFileCards();
                updateSummary();
            });
        });
    }
    
    /**
     * Remove a file from the list.
     */
    private void removeFile(PdfItem item) {
        selectedFiles.remove(item);
    }
    
    /**
     * Move file up in the list.
     */
    private void moveFileUp(int index) {
        if (index > 0) {
            PdfItem item = selectedFiles.remove(index);
            selectedFiles.add(index - 1, item);
            renderFileCards();
        }
    }
    
    /**
     * Move file down in the list.
     */
    private void moveFileDown(int index) {
        if (index < selectedFiles.size() - 1) {
            PdfItem item = selectedFiles.remove(index);
            selectedFiles.add(index + 1, item);
            renderFileCards();
        }
    }
    
    /**
     * Handle select files button.
     */
    @FXML
    private void selectFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LocaleManager.getString("merge.selectFiles"));
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                LocaleManager.getString("merge.pdfFiles"), "*.pdf")
        );
        
        List<File> files = chooser.showOpenMultipleDialog(
            selectFilesButton.getScene().getWindow()
        );
        
        if (files != null && !files.isEmpty()) {
            // Filter duplicates
            List<File> newFiles = files.stream()
                .filter(f -> selectedFiles.stream()
                    .noneMatch(item -> item.getPath().toFile().getAbsolutePath()
                        .equals(f.getAbsolutePath())))
                .collect(Collectors.toList());
            
            for (File file : newFiles) {
                addPdfFile(file);
            }
            
            if (newFiles.size() < files.size()) {
                showInfo(String.format(
                    LocaleManager.getString("merge.duplicatesIgnored"),
                    files.size() - newFiles.size()
                ));
            }
        }
    }
    
    /**
     * Handle clear files button.
     */
    @FXML
    private void clearFiles() {
        if (!selectedFiles.isEmpty()) {
            boolean confirmed = CustomDialog.showConfirmation(
                LocaleManager.getString("merge.clearAll"),
                LocaleManager.getString("merge.clearAllConfirm") + "\n\n" + 
                LocaleManager.getString("merge.clearAllMessage")
            );
            
            if (confirmed) {
                selectedFiles.clear();
            }
        }
    }
    
    /**
     * Handle browse folder button.
     */
    @FXML
    private void browseFolderButton() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LocaleManager.getString("merge.chooseFolder"));
        
        File currentFolder = new File(outputFolderField.getText());
        if (currentFolder.exists() && currentFolder.isDirectory()) {
            chooser.setInitialDirectory(currentFolder);
        }
        
        File folder = chooser.showDialog(browseFolderButton.getScene().getWindow());
        if (folder != null) {
            outputFolderField.setText(folder.getAbsolutePath());
        }
    }
    
    /**
     * Handle merge button.
     */
    @FXML
    private void handleMerge() {
        // Validation
        if (selectedFiles.size() < 2) {
            showError(LocaleManager.getString("merge.error.minFiles"));
            return;
        }
        
        String outputPath = outputFolderField.getText();
        if (outputPath == null || outputPath.trim().isEmpty()) {
            showError(LocaleManager.getString("merge.error.noOutputFolder"));
            return;
        }
        
        String filename = outputFilenameField.getText();
        if (filename == null || filename.trim().isEmpty()) {
            filename = "merged.pdf";
        }
        
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename += ".pdf";
        }
        
        final File outputDir = new File(outputPath);
        File outputFile = new File(outputDir, filename);
        
        // Check if file exists and handle duplicate
        if (outputFile.exists()) {
            var result = CustomDialog.showTripleChoice(
                LocaleManager.getString("merge.warning.fileExists"),
                LocaleManager.getString("merge.warning.fileExistsHeader") + "\n\n" +
                String.format(LocaleManager.getString("merge.warning.fileExistsMessage"), filename),
                LocaleManager.getString("merge.warning.rename"),
                LocaleManager.getString("merge.warning.overwrite"),
                LocaleManager.getString("common.cancel")
            );
            
            if (result.isEmpty() || result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return; // User cancelled
            } else if (result.get().getButtonData() == ButtonBar.ButtonData.YES) {
                // Rename (YES = first button)
                outputFile = getUniqueOutputFile(outputDir, filename);
                outputFilenameField.setText(outputFile.getName());
            }
            // If overwrite (NO = second button), continue with existing outputFile
        }
        
        // Show progress overlay
        showProgressOverlay();
        
        final File finalOutputFile = outputFile;
        final List<File> files = selectedFiles.stream()
            .map(item -> item.getPath().toFile())
            .collect(Collectors.toList());
        
        mergeTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage(LocaleManager.getString("merge.progress.preparing"));
                updateProgress(0, files.size() + 1);
                
                // Process each file
                for (int i = 0; i < files.size(); i++) {
                    if (isCancelled()) {
                        updateMessage(LocaleManager.getString("merge.progress.cancelled"));
                        return null;
                    }
                    
                    String fileName = files.get(i).getName();
                    updateMessage(String.format(
                        LocaleManager.getString("merge.progress.processing"),
                        i + 1, files.size(), fileName
                    ));
                    updateProgress(i + 1, files.size() + 1);
                }
                
                // Perform merge
                updateMessage(LocaleManager.getString("merge.progress.merging"));
                mergeService.mergePdfs(files, finalOutputFile);
                
                updateMessage(LocaleManager.getString("merge.progress.writing"));
                updateProgress(files.size() + 1, files.size() + 1);
                
                updateMessage(LocaleManager.getString("merge.progress.complete"));
                return finalOutputFile;
            }
        };
        
        // Bind progress
        progressBar.progressProperty().bind(mergeTask.progressProperty());
        progressMessage.textProperty().bind(mergeTask.messageProperty());
        
        mergeTask.setOnSucceeded(e -> Platform.runLater(() -> {
            File result = mergeTask.getValue();
            if (result != null) {
                lastOutputFile = result;
                showSuccess(result.getName());
            }
        }));
        
        mergeTask.setOnFailed(e -> Platform.runLater(() -> {
            hideProgressOverlay();
            Throwable ex = mergeTask.getException();
            showError(LocaleManager.getString("merge.error.failed") + 
                     (ex != null ? ": " + ex.getMessage() : ""));
        }));
        
        mergeTask.setOnCancelled(e -> Platform.runLater(this::hideProgressOverlay));
        
        Thread thread = new Thread(mergeTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Handle reset button.
     */
    @FXML
    private void handleReset() {
        selectedFiles.clear();
        outputFilenameField.setText("merged.pdf");
    }
    
    /**
     * Handle open folder button.
     */
    @FXML
    private void handleOpenFolder() {
        if (lastOutputFile != null && lastOutputFile.getParentFile() != null) {
            try {
                java.awt.Desktop.getDesktop().open(lastOutputFile.getParentFile());
            } catch (Exception ex) {
                showError(LocaleManager.getString("merge.error.openFolder") + ": " + ex.getMessage());
            }
        }
    }
    
    /**
     * Handle merge another button.
     */
    @FXML
    private void handleMergeAnother() {
        hideProgressOverlay();
        selectedFiles.clear();
        outputFilenameField.setText("merged.pdf");
    }
    
    /**
     * Handle cancel process button.
     */
    @FXML
    private void handleCancelProcess() {
        if (mergeTask != null && mergeTask.isRunning()) {
            mergeTask.cancel();
        }
    }
    
    /**
     * Handle close success button.
     */
    @FXML
    private void handleCloseSuccess() {
        hideProgressOverlay();
    }
    
    /**
     * Get a unique output file by appending numbers if file exists.
     * Example: merged.pdf -> merged_1.pdf -> merged_2.pdf
     */
    private File getUniqueOutputFile(File directory, String originalFilename) {
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        
        int counter = 1;
        File file = new File(directory, originalFilename);
        
        while (file.exists()) {
            String newFilename = baseName + "_" + counter + extension;
            file = new File(directory, newFilename);
            counter++;
        }
        
        return file;
    }
    
    /**
     * Show progress overlay.
     */
    private void showProgressOverlay() {
        progressOverlay.setVisible(true);
        progressOverlay.setManaged(true);
        successPane.setVisible(false);
        successPane.setManaged(false);
        cancelProcessButton.setVisible(true);
        cancelProcessButton.setManaged(true);
    }
    
    /**
     * Hide progress overlay.
     */
    private void hideProgressOverlay() {
        // Unbind properties before hiding to prevent binding errors
        progressBar.progressProperty().unbind();
        progressMessage.textProperty().unbind();
        
        progressOverlay.setVisible(false);
        progressOverlay.setManaged(false);
    }
    
    /**
     * Show success state.
     */
    private void showSuccess(String filename) {
        successMessage.setText(String.format(
            LocaleManager.getString("merge.success.message"), 
            filename
        ));
        
        cancelProcessButton.setVisible(false);
        cancelProcessButton.setManaged(false);
        successPane.setVisible(true);
        successPane.setManaged(true);
    }
    
    /**
     * Show error alert.
     */
    private void showError(String message) {
        CustomDialog.showError(
            LocaleManager.getString("merge.error.title"),
            message
        );
    }
    
    /**
     * Show info alert.
     */
    private void showInfo(String message) {
        CustomDialog.showInfo(
            LocaleManager.getString("merge.title"),
            message
        );
    }
}
