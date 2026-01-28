package com.pdftoolkit.controllers;

import com.pdftoolkit.models.PdfItem;
import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.services.CompressPdfService;
import com.pdftoolkit.services.PdfPreviewService;
import com.pdftoolkit.state.CompressPdfState;
import com.pdftoolkit.state.CompressionLevel;
import com.pdftoolkit.state.StateStore;
import com.pdftoolkit.ui.PdfItemCell;
import com.pdftoolkit.utils.AppPaths;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Redesigned Compress PDF Controller with modern two-panel UI.
 * LEFT: File workspace with drag & drop support
 * RIGHT: Compression settings + Primary CTA
 * 
 * Key improvements over original CompressController:
 * - Two-panel layout for better visual hierarchy
 * - Improved file input with ListView for better scalability
 * - Enhanced drag & drop with visual feedback
 * - Better empty state design
 * - Cleaner settings panel with radio cards
 * - Persistent state management via StateStore
 * - Async metadata loading for responsive UI
 * - Proper validation with reactive bindings
 */
public class CompressControllerRedesigned {

    // Left Panel: File Staging Area (Following Protect Style)
    @FXML private StackPane emptyStatePane;  // Drop zone (large)
    @FXML private ListView<PdfItem> fileListView;
    @FXML private VBox fileListContainer;
    @FXML private Button addFilesButton;
    @FXML private Label filesCountLabel;
    @FXML private Button clearAllButton;

    // Right Panel: Settings
    @FXML private RadioButton extremeRadio;
    @FXML private RadioButton recommendedRadio;
    @FXML private RadioButton lowRadio;
    @FXML private HBox extremeCard;
    @FXML private HBox recommendedCard;
    @FXML private HBox lowCard;
    @FXML private CheckBox keepBestQualityCheckbox;
    @FXML private TextField outputFolderField;
    @FXML private Button browseOutputButton;
    @FXML private Button compressButton;

    // Progress overlay elements (unified like Split)
    @FXML private StackPane progressOverlay;
    @FXML private Label progressTitle;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressMessage;
    @FXML private Button cancelProcessButton;
    @FXML private VBox successPane;
    @FXML private Label successMessage;
    @FXML private Button openFolderButton;
    @FXML private Button processAnotherButton;
    @FXML private Button closeSuccessButton;

    // State and Services
    private CompressPdfState state;
    private final PdfPreviewService previewService = PdfPreviewService.getInstance();
    private final CompressPdfService compressService = new CompressPdfService();
    private Task<?> currentTask;
    private ResourceBundle bundle;
    private ToggleGroup compressionGroup;

    @FXML
    private void initialize() {
        // Get persistent state from store
        state = StateStore.getInstance().getCompressPdfState();
        
        // Get resource bundle for i18n
        bundle = LocaleManager.getBundle();
        
        // Initialize UI components
        setupCompressionRadioCards();
        setupFileListView();
        setupDragAndDrop();
        setupOutputSettings();
        setupButtons();
        setupValidation();
        setupKeyboardShortcuts();
        setupLocaleListener();
        
        // Initialize visibility
        updateEmptyState();
        progressOverlay.setVisible(false);
        progressOverlay.setManaged(false);
        
        // Update UI text
        updateTexts();
    }

    /**
     * Setup compression radio cards with toggle group and click handlers.
     */
    private void setupCompressionRadioCards() {
        // Create toggle group
        compressionGroup = new ToggleGroup();
        extremeRadio.setToggleGroup(compressionGroup);
        recommendedRadio.setToggleGroup(compressionGroup);
        lowRadio.setToggleGroup(compressionGroup);
        
        // Set initial selection from state
        selectCompressionLevel(state.getCompressionLevel());
        
        // Make entire card clickable
        extremeCard.setOnMouseClicked(e -> extremeRadio.setSelected(true));
        recommendedCard.setOnMouseClicked(e -> recommendedRadio.setSelected(true));
        lowCard.setOnMouseClicked(e -> lowRadio.setSelected(true));
        
        // Add hover effects
        setupCardHoverEffect(extremeCard, extremeRadio);
        setupCardHoverEffect(recommendedCard, recommendedRadio);
        setupCardHoverEffect(lowCard, lowRadio);
        
        // Sync state with selection
        compressionGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            CompressionLevel level = getSelectedCompressionLevel();
            state.setCompressionLevel(level);
            updateCardSelection();
            updateCompressButtonText();
        });
        
        // Initial card selection styling
        updateCardSelection();
        
        // Bind keep best quality checkbox
        keepBestQualityCheckbox.selectedProperty().bindBidirectional(state.keepBestQualityProperty());
    }

    /**
     * Get the currently selected compression level from radio buttons.
     */
    private CompressionLevel getSelectedCompressionLevel() {
        if (extremeRadio.isSelected()) {
            return CompressionLevel.EXTREME;
        } else if (lowRadio.isSelected()) {
            return CompressionLevel.LOW;
        } else {
            return CompressionLevel.RECOMMENDED;
        }
    }

    /**
     * Set the radio button selection based on compression level.
     */
    private void selectCompressionLevel(CompressionLevel level) {
        switch (level) {
            case EXTREME -> extremeRadio.setSelected(true);
            case LOW -> lowRadio.setSelected(true);
            default -> recommendedRadio.setSelected(true);
        }
    }

    /**
     * Setup hover effects for radio card.
     */
    private void setupCardHoverEffect(HBox card, RadioButton radio) {
        card.setOnMouseEntered(e -> {
            if (!radio.isSelected()) {
                if (!card.getStyleClass().contains("radio-card-hover")) {
                    card.getStyleClass().add("radio-card-hover");
                }
            }
        });
        card.setOnMouseExited(e -> {
            card.getStyleClass().remove("radio-card-hover");
        });
    }

    /**
     * Update visual selection state of all radio cards.
     */
    private void updateCardSelection() {
        updateSingleCardSelection(extremeCard, extremeRadio);
        updateSingleCardSelection(recommendedCard, recommendedRadio);
        updateSingleCardSelection(lowCard, lowRadio);
    }

    /**
     * Update visual selection state of a single radio card.
     */
    private void updateSingleCardSelection(HBox card, RadioButton radio) {
        if (radio.isSelected()) {
            if (!card.getStyleClass().contains("radio-card-selected")) {
                card.getStyleClass().add("radio-card-selected");
            }
        } else {
            card.getStyleClass().remove("radio-card-selected");
        }
    }

    /**
     * Setup file ListView with custom cell factory.
     */
    private void setupFileListView() {
        // Bind ListView to state
        fileListView.setItems(state.getItems());
        
        // Custom cell factory with remove button
        fileListView.setCellFactory(lv -> new PdfItemCell(this::handleRemoveFile));
        
        // Update UI when list changes
        state.getItems().addListener((ListChangeListener<PdfItem>) change -> {
            updateEmptyState();
            updateFilesCountLabel();
            updateCompressButtonText();
        });
        
        // Set empty placeholder
        fileListView.setPlaceholder(new Label());
    }

    /**
     * Setup drag and drop support for file input.
     */
    private void setupDragAndDrop() {
        // Apply drag-drop to empty state pane (drop zone)
        emptyStatePane.setOnDragOver(this::handleDragOver);
        emptyStatePane.setOnDragDropped(this::handleDragDropped);
        
        // Also enable on file list container (when files exist)
        fileListContainer.setOnDragOver(this::handleDragOver);
        fileListContainer.setOnDragDropped(this::handleDragDropped);
    }

    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        
        List<File> files = event.getDragboard().getFiles();
        if (files != null && !files.isEmpty()) {
            List<Path> pdfPaths = files.stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .map(File::toPath)
                .collect(Collectors.toList());
            
            if (!pdfPaths.isEmpty()) {
                addFiles(pdfPaths);
            } else {
                showError(bundle.getString("compress.error.noPdfFiles"));
            }
        }
        
        event.setDropCompleted(true);
        event.consume();
    }

    /**
     * Setup output folder and filename with state binding.
     */
    private void setupOutputSettings() {
        // Set default output folder if not set
        if (state.getOutputFolder() == null) {
            Path defaultFolder = Paths.get(AppPaths.getDefaultOutputPath());
            state.setOutputFolder(defaultFolder);
        }
        
        // Set default output filename if not set
        if (state.getOutputFileName() == null || state.getOutputFileName().isEmpty()) {
            state.setOutputFileName("compressed.pdf");
        }
        
        // Bind output folder field to state
        outputFolderField.textProperty().bind(
            Bindings.createStringBinding(
                () -> state.getOutputFolder() != null ? state.getOutputFolder().toString() : "",
                state.outputFolderProperty()
            )
        );
    }

    /**
     * Setup all button handlers.
     */
    private void setupButtons() {
        addFilesButton.setOnAction(e -> handleAddFiles());
        clearAllButton.setOnAction(e -> handleClearAll());
        browseOutputButton.setOnAction(e -> handleBrowseOutput());
        compressButton.setOnAction(e -> handleCompress());
        openFolderButton.setOnAction(e -> handleOpenFolder());
        processAnotherButton.setOnAction(e -> handleProcessAnother());
    }

    /**
     * Setup validation - disable compress button when invalid state.
     */
    private void setupValidation() {
        javafx.beans.binding.BooleanBinding shouldDisable = Bindings.createBooleanBinding(
            () -> {
                // Disable if no files
                if (state.getItems().isEmpty()) {
                    return true;
                }
                
                // Disable if any file is loading
                if (state.getItems().stream().anyMatch(PdfItem::isLoading)) {
                    return true;
                }
                
                // Disable if any file has error
                if (state.getItems().stream().anyMatch(PdfItem::hasError)) {
                    return true;
                }
                
                // Disable if output folder is invalid
                if (state.getOutputFolder() == null) {
                    return true;
                }
                
                // Disable if output filename is empty
                String fileName = state.getOutputFileName();
                if (fileName == null || fileName.trim().isEmpty()) {
                    return true;
                }
                
                return false;
            },
            state.getItems(),
            state.outputFolderProperty(),
            state.outputFileNameProperty()
        );
        
        compressButton.disableProperty().bind(shouldDisable);
        
        // Listen to item property changes
        state.getItems().addListener((ListChangeListener<PdfItem>) change -> {
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
     * Setup keyboard shortcuts.
     */
    private void setupKeyboardShortcuts() {
        fileListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                PdfItem selected = fileListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleRemoveFile(selected);
                }
            }
        });
    }

    /**
     * Setup locale change listener for live language switching.
     */
    private void setupLocaleListener() {
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> {
            bundle = LocaleManager.getBundle();
            updateTexts();
        });
    }

    /**
     * Update all UI texts based on current locale.
     */
    private void updateTexts() {
        updateCompressButtonText();
        updateFilesCountLabel();
    }

    /**
     * Update empty state visibility based on file list.
     */
    private void updateEmptyState() {
        boolean isEmpty = state.getItems().isEmpty();
        
        if (emptyStatePane != null) {
            emptyStatePane.setVisible(isEmpty);
            emptyStatePane.setManaged(isEmpty);
        }
        
        if (fileListContainer != null) {
            fileListContainer.setVisible(!isEmpty);
            fileListContainer.setManaged(!isEmpty);
        }
    }

    /**
     * Update files count label.
     */
    private void updateFilesCountLabel() {
        int count = state.getItems().size();
        if (count == 0) {
            filesCountLabel.setText(bundle.getString("compress.noFiles"));
        } else if (count == 1) {
            filesCountLabel.setText("1 " + bundle.getString("compress.file"));
        } else {
            filesCountLabel.setText(count + " " + bundle.getString("compress.files"));
        }
    }

    /**
     * Update compress button text based on file count.
     */
    private void updateCompressButtonText() {
        int count = state.getItems().size();
        if (count == 0) {
            compressButton.setText(bundle.getString("compress.compressNow"));
        } else if (count == 1) {
            compressButton.setText(bundle.getString("compress.compressFile"));
        } else {
            compressButton.setText(String.format(bundle.getString("compress.compressFiles"), count));
        }
    }

    /**
     * Handle add files button click.
     */
    @FXML
    private void handleAddFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(bundle.getString("compress.selectFiles"));
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(bundle.getString("compress.pdfFiles"), "*.pdf")
        );
        
        // Set initial directory from state or default
        if (state.getOutputFolder() != null && Files.exists(state.getOutputFolder())) {
            chooser.setInitialDirectory(state.getOutputFolder().toFile());
        }
        
        List<File> files = chooser.showOpenMultipleDialog(AppNavigator.getPrimaryStage());
        if (files != null && !files.isEmpty()) {
            List<Path> paths = files.stream().map(File::toPath).collect(Collectors.toList());
            addFiles(paths);
        }
    }

    /**
     * Add files to the list with duplicate detection and async metadata loading.
     */
    private void addFiles(List<Path> paths) {
        int addedCount = 0;
        int duplicateCount = 0;
        
        for (Path path : paths) {
            // Validate file exists and is readable
            if (!Files.exists(path) || !Files.isReadable(path)) {
                continue;
            }
            
            // Create PDF item
            PdfItem item = new PdfItem(path);
            
            // Try to add (will reject duplicates)
            boolean added = state.addItem(item);
            
            if (added) {
                addedCount++;
                // Load metadata asynchronously
                previewService.loadMetadataAsync(item);
            } else {
                duplicateCount++;
            }
        }
        
        // Show feedback if duplicates were ignored
        if (duplicateCount > 0) {
            String message = String.format(
                bundle.getString("compress.duplicatesIgnored"), 
                duplicateCount
            );
            showInfo(message);
        }
    }

    /**
     * Handle remove file from list.
     */
    private void handleRemoveFile(PdfItem item) {
        state.removeItem(item);
    }

    /**
     * Handle clear all files.
     */
    @FXML
    private void handleClearAll() {
        if (!state.getItems().isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(bundle.getString("compress.clearAll"));
            confirm.setHeaderText(bundle.getString("compress.clearAllConfirm"));
            confirm.setContentText(bundle.getString("compress.clearAllMessage"));
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    state.clearItems();
                }
            });
        }
    }

    /**
     * Handle browse output folder.
     */
    @FXML
    private void handleBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(bundle.getString("compress.chooseFolder"));
        
        if (state.getOutputFolder() != null && Files.exists(state.getOutputFolder())) {
            chooser.setInitialDirectory(state.getOutputFolder().toFile());
        }
        
        File folder = chooser.showDialog(AppNavigator.getPrimaryStage());
        if (folder != null) {
            state.setOutputFolder(folder.toPath());
        }
    }

    /**
     * Handle compress button - start compression process.
     */
    @FXML
    private void handleCompress() {
        if (!state.isValid()) {
            return;
        }

        // Prepare compression parameters
        List<Path> inputPaths = state.getItems().stream()
            .map(PdfItem::getPath)
            .collect(Collectors.toList());
        
        Path outputDir = state.getOutputFolder();
        String outputFileName = state.getOutputFileName();
        CompressionLevel level = state.getCompressionLevel();
        boolean keepBestQuality = state.isKeepBestQuality();
        
        // Ensure .pdf extension
        if (!outputFileName.toLowerCase().endsWith(".pdf")) {
            outputFileName += ".pdf";
        }
        
        // Create compression task
        if (inputPaths.size() == 1) {
            // Single file compression
            Path outputPath = outputDir.resolve(outputFileName);
            
            // Handle duplicate filename
            int counter = 1;
            while (Files.exists(outputPath)) {
                String baseName = outputFileName.substring(0, outputFileName.length() - 4);
                outputPath = outputDir.resolve(baseName + "_(" + counter + ").pdf");
                counter++;
            }
            
            currentTask = compressService.compressSingleFile(
                inputPaths.get(0),
                outputPath,
                level,
                keepBestQuality
            );
        } else {
            // Multiple files compression
            currentTask = compressService.compressMultipleFiles(
                inputPaths,
                outputDir,
                level,
                keepBestQuality
            );
        }

        // Bind progress
        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressMessage.textProperty().bind(currentTask.messageProperty());

        // Handle task completion
        currentTask.setOnSucceeded(e -> Platform.runLater(() -> {
            showSuccess();
            AppState.getInstance().addRecentFile("Compress", state.getOutputFolder().toFile());
        }));

        currentTask.setOnFailed(e -> Platform.runLater(() -> {
            hideProgressOverlay();
            Throwable ex = currentTask.getException();
            String errorMsg = ex != null ? ex.getMessage() : bundle.getString("compress.error.unknown");
            showError(errorMsg);
        }));

        currentTask.setOnCancelled(e -> Platform.runLater(this::hideProgressOverlay));

        // Show progress overlay
        showProgressOverlay();
        
        // Start task
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }
    /**
     * Handle open folder button.
     */
    @FXML
    private void handleOpenFolder() {
        try {
            if (state.getOutputFolder() != null && Files.exists(state.getOutputFolder())) {
                java.awt.Desktop.getDesktop().open(state.getOutputFolder().toFile());
            }
        } catch (Exception e) {
            showError(bundle.getString("compress.error.openFolder") + ": " + e.getMessage());
        }
    }

    /**
     * Handle process another button.
     */
    @FXML
    private void handleProcessAnother() {
        hideProgressOverlay();
        // Optionally clear files for next operation
        // state.clearItems();
    }

    /**
     * Handle cancel process button.
     */
    @FXML
    private void handleCancelProcess() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
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
     * Show success completion state.
     */
    private void showSuccess() {
        int fileCount = state.getItems().size();
        String message;
        if (fileCount == 1) {
            message = bundle.getString("compress.success.single");
        } else {
            message = String.format(bundle.getString("compress.success.multiple"), fileCount);
        }
        
        cancelProcessButton.setVisible(false);
        cancelProcessButton.setManaged(false);
        successPane.setVisible(true);
        successPane.setManaged(true);
        successMessage.setText(message);
    }

    /**
     * Show error alert.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(bundle.getString("compress.error.title"));
        alert.setHeaderText(bundle.getString("compress.title"));
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info alert.
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(bundle.getString("compress.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
