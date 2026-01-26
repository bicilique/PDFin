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
 * Controller for Compress PDF view - iLovePDF-inspired redesign.
 * 
 * Features:
 * - Clean 2-column layout (files left, settings right)
 * - Radio card compression options instead of slider
 * - Collapsible "More options" panel
 * - Persistent state across language switches (via StateStore)
 * - Async loading of PDF metadata and thumbnails
 * - Drag & drop support for adding files
 * - Duplicate detection (by normalized absolute path)
 * - Per-item remove button
 * - Real PDFBox compression with configurable levels
 */
public class CompressController {

    // Top bar
    @FXML
    private Button backButton;

    // File workspace (left column)
    @FXML
    private StackPane dropZone;

    @FXML
    private VBox emptyStateContainer;

    @FXML
    private VBox fileListContainer;

    @FXML
    private ListView<PdfItem> fileListView;

    @FXML
    private Button addFilesButton;

    @FXML
    private Button addMoreButton;

    @FXML
    private Button clearButton;

    // Settings panel (right column) - Radio Cards
    @FXML
    private RadioButton extremeRadio;

    @FXML
    private RadioButton recommendedRadio;

    @FXML
    private RadioButton lowRadio;
    
    @FXML
    private javafx.scene.layout.HBox extremeCard;
    
    @FXML
    private javafx.scene.layout.HBox recommendedCard;
    
    @FXML
    private javafx.scene.layout.HBox lowCard;

    @FXML
    private Button moreOptionsToggle;

    @FXML
    private VBox moreOptionsPanel;

    @FXML
    private CheckBox keepBestQualityCheckbox;

    @FXML
    private TextField outputFolderField;

    @FXML
    private Button browseOutputButton;

    @FXML
    private TextField outputFileNameField;

    @FXML
    private Button processButton;

    // Progress overlay
    @FXML
    private StackPane progressOverlay;

    @FXML
    private VBox processingPane;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private VBox completionPane;

    @FXML
    private Label completionMessage;

    @FXML
    private Button openFolderButton;

    @FXML
    private Button processAnotherButton;

    // Hidden fields for backwards compatibility
    @FXML
    private Slider compressionSlider;

    @FXML
    private Label compressionLevelLabel;

    // Persistent state
    private CompressPdfState state;
    
    // Services
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
        
        setupRadioButtons();
        setupListView();
        setupDragAndDrop();
        setupOutputSettings();
        setupButtons();
        setupValidation();
        setupKeyboardShortcuts();
        
        progressOverlay.setVisible(false);
        progressOverlay.setManaged(false);
        completionPane.setVisible(false);
        processingPane.setVisible(true);
        
        // Setup locale listener for live language switching
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> {
            bundle = LocaleManager.getBundle();
            updateTexts();
        });
        
        // Initial text update
        updateTexts();
    }
    
    /**
     * Setup radio button toggle group for compression levels.
     * Each card is fully clickable and updates the RadioButton selection.
     */
    private void setupRadioButtons() {
        compressionGroup = new ToggleGroup();
        extremeRadio.setToggleGroup(compressionGroup);
        recommendedRadio.setToggleGroup(compressionGroup);
        lowRadio.setToggleGroup(compressionGroup);
        
        // Set initial selection based on state
        switch (state.getCompressionLevel()) {
            case LOW -> lowRadio.setSelected(true);
            case EXTREME -> extremeRadio.setSelected(true);
            default -> recommendedRadio.setSelected(true);
        }
        
        // Make entire card clickable
        extremeCard.setOnMouseClicked(e -> extremeRadio.setSelected(true));
        recommendedCard.setOnMouseClicked(e -> recommendedRadio.setSelected(true));
        lowCard.setOnMouseClicked(e -> lowRadio.setSelected(true));
        
        // Add visual feedback on card hover
        setupCardHoverEffect(extremeCard, extremeRadio);
        setupCardHoverEffect(recommendedCard, recommendedRadio);
        setupCardHoverEffect(lowCard, lowRadio);
        
        // Sync state with radio button selection
        compressionGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == extremeRadio) {
                state.setCompressionLevel(CompressionLevel.EXTREME);
                updateCardSelection(extremeCard, true);
                updateCardSelection(recommendedCard, false);
                updateCardSelection(lowCard, false);
            } else if (newToggle == lowRadio) {
                state.setCompressionLevel(CompressionLevel.LOW);
                updateCardSelection(extremeCard, false);
                updateCardSelection(recommendedCard, false);
                updateCardSelection(lowCard, true);
            } else {
                state.setCompressionLevel(CompressionLevel.RECOMMENDED);
                updateCardSelection(extremeCard, false);
                updateCardSelection(recommendedCard, true);
                updateCardSelection(lowCard, false);
            }
            updateProcessButtonText();
        });
        
        // Initial card selection styling
        updateCardSelection(recommendedCard, true);
        
        // Bind checkbox to state
        keepBestQualityCheckbox.selectedProperty().bindBidirectional(state.keepBestQualityProperty());
        
        // More options toggle
        moreOptionsToggle.setOnAction(e -> toggleMoreOptions());
    }
    
    /**
     * Setup hover effects for radio cards.
     */
    private void setupCardHoverEffect(javafx.scene.layout.HBox card, RadioButton radio) {
        card.setOnMouseEntered(e -> {
            if (!radio.isSelected()) {
                card.setStyle(card.getStyle() + "-fx-background-color: rgba(59, 130, 246, 0.08);");
            }
        });
        card.setOnMouseExited(e -> {
            if (!radio.isSelected()) {
                card.setStyle("");
            }
        });
    }
    
    /**
     * Update visual selection state of a radio card.
     */
    private void updateCardSelection(javafx.scene.layout.HBox card, boolean selected) {
        if (selected) {
            if (!card.getStyleClass().contains("radio-card-selected")) {
                card.getStyleClass().add("radio-card-selected");
            }
        } else {
            card.getStyleClass().remove("radio-card-selected");
        }
    }
    
    /**
     * Toggle visibility of more options panel.
     */
    private void toggleMoreOptions() {
        boolean isVisible = moreOptionsPanel.isVisible();
        moreOptionsPanel.setVisible(!isVisible);
        moreOptionsPanel.setManaged(!isVisible);
        moreOptionsToggle.setText(isVisible ? "More options ▾" : "More options ▴");
    }
    
    /**
     * Setup ListView with custom cell factory and bind to persistent state.
     */
    private void setupListView() {
        // Bind ListView to persistent state
        fileListView.setItems(state.getItems());
        
        // Custom cell factory
        fileListView.setCellFactory(lv -> new PdfItemCell(this::handleRemoveItem));
        
        // Show/hide empty state vs file list based on content
        state.getItems().addListener((ListChangeListener<PdfItem>) change -> {
            updateEmptyState();
            updateProcessButtonText();
        });
        
        updateEmptyState();
        
        // Placeholder when list is empty
        fileListView.setPlaceholder(new Label()); // Empty, we use custom overlay
    }
    
    private void updateEmptyState() {
        boolean isEmpty = state.getItems().isEmpty();
        
        // Show/hide empty state
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(isEmpty);
            emptyStateContainer.setManaged(isEmpty);
        }
        
        // Show/hide file list container
        if (fileListContainer != null) {
            fileListContainer.setVisible(!isEmpty);
            fileListContainer.setManaged(!isEmpty);
        }
    }
    
    /**
     * Update the process button text based on file count.
     * "Compress 1 PDF" / "Compress 2 PDFs"
     */
    private void updateProcessButtonText() {
        int count = state.getItems().size();
        if (count == 0) {
            processButton.setText(bundle.getString("compress.compressNow"));
        } else if (count == 1) {
            processButton.setText("Compress 1 PDF");
        } else {
            processButton.setText(String.format("Compress %d PDFs", count));
        }
    }
    
    /**
     * Setup drag and drop for the drop zone.
     */
    private void setupDragAndDrop() {
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleDragDropped);
        
        // Also enable drag-drop on the ListView itself
        fileListView.setOnDragOver(this::handleDragOver);
        fileListView.setOnDragDropped(this::handleDragDropped);
    }

    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();
        if (files != null) {
            List<Path> pdfPaths = files.stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .map(File::toPath)
                .collect(Collectors.toList());
            
            addFiles(pdfPaths);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    /**
     * Setup output folder and filename fields with state binding.
     */
    private void setupOutputSettings() {
        // Initialize default output folder if not set
        if (state.getOutputFolder() == null) {
            Path defaultFolder = Paths.get(System.getProperty("user.home"), "Desktop");
            if (!Files.exists(defaultFolder)) {
                defaultFolder = Paths.get(System.getProperty("user.home"));
            }
            state.setOutputFolder(defaultFolder);
        }
        
        // Bind output folder field to state
        outputFolderField.textProperty().bind(
            Bindings.createStringBinding(
                () -> state.getOutputFolder() != null ? state.getOutputFolder().toString() : "",
                state.outputFolderProperty()
            )
        );
        
        // Bind output filename field to state (bidirectional)
        outputFileNameField.textProperty().bindBidirectional(state.outputFileNameProperty());
    }

    private void setupButtons() {
        backButton.setOnAction(e -> AppNavigator.navigateToHome());
        addFilesButton.setOnAction(e -> handleAddFiles());
        if (addMoreButton != null) {
            addMoreButton.setOnAction(e -> handleAddFiles());
        }
        clearButton.setOnAction(e -> handleClear());
        browseOutputButton.setOnAction(e -> handleBrowseOutput());
        processButton.setOnAction(e -> handleProcess());
        cancelButton.setOnAction(e -> handleCancelProcess());
        openFolderButton.setOnAction(e -> handleOpenFolder());
        processAnotherButton.setOnAction(e -> handleProcessAnother());
    }

    /**
     * Setup validation rules - disable process button when state is invalid.
     * Uses pure binding approach - no imperative setDisable() calls.
     */
    private void setupValidation() {
        // Create a BooleanBinding that tracks all validation conditions
        javafx.beans.binding.BooleanBinding shouldDisable = Bindings.createBooleanBinding(
            () -> {
                // Disable if no items
                if (state.getItems().isEmpty()) {
                    return true;
                }
                
                // Disable if any item is loading
                if (state.getItems().stream().anyMatch(PdfItem::isLoading)) {
                    return true;
                }
                
                // Disable if any item has error
                if (state.getItems().stream().anyMatch(PdfItem::hasError)) {
                    return true;
                }
                
                // Disable if output folder is null or invalid
                if (state.getOutputFolder() == null) {
                    return true;
                }
                
                // Disable if output filename is empty or invalid
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
        
        // Bind once and never call setDisable manually
        processButton.disableProperty().bind(shouldDisable);
        
        // Listen to individual item changes to trigger binding re-evaluation
        state.getItems().addListener((ListChangeListener<PdfItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (PdfItem item : change.getAddedSubList()) {
                        // Invalidate binding when loading or error state changes
                        item.loadingProperty().addListener((obs, oldVal, newVal) -> {
                            shouldDisable.invalidate();
                        });
                        item.errorProperty().addListener((obs, oldVal, newVal) -> {
                            shouldDisable.invalidate();
                        });
                    }
                }
            }
        });
    }
    
    /**
     * Setup keyboard shortcuts (Delete key to remove selected items).
     */
    private void setupKeyboardShortcuts() {
        fileListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                PdfItem selected = fileListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleRemoveItem(selected);
                }
            }
        });
    }
    
    private void updateTexts() {
        // Update process button text based on file count
        updateProcessButtonText();
    }

    /**
     * Handle add files button click.
     */
    private void handleAddFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(bundle.getString("compress.selectFiles"));
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        List<File> files = chooser.showOpenMultipleDialog(AppNavigator.getPrimaryStage());
        if (files != null && !files.isEmpty()) {
            List<Path> paths = files.stream().map(File::toPath).collect(Collectors.toList());
            addFiles(paths);
        }
    }
    
    /**
     * Add files to the list, ignoring duplicates and loading metadata asynchronously.
     */
    private void addFiles(List<Path> paths) {
        for (Path path : paths) {
            // Create PdfItem
            PdfItem item = new PdfItem(path);
            
            // Try to add to state (will reject duplicates)
            boolean added = state.addItem(item);
            
            if (added) {
                // Load metadata asynchronously
                previewService.loadMetadataAsync(item);
            }
        }
    }
    
    /**
     * Handle remove item from list.
     */
    private void handleRemoveItem(PdfItem item) {
        state.removeItem(item);
    }

    /**
     * Handle clear all files.
     */
    private void handleClear() {
        state.clearItems();
    }

    /**
     * Handle browse output folder.
     */
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
     * Handle compress now button - start compression task.
     */
    private void handleProcess() {
        if (!state.isValid()) {
            return;
        }

        // Prepare paths
        List<Path> inputPaths = state.getItems().stream()
            .map(PdfItem::getPath)
            .collect(Collectors.toList());
        
        Path outputDir = state.getOutputFolder();
        String outputFileName = state.getOutputFileName();
        CompressionLevel level = state.getCompressionLevel();
        boolean keepBestQuality = state.isKeepBestQuality();
        
        // Ensure output filename has .pdf extension
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

        // Bind progress UI
        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressLabel.textProperty().bind(currentTask.messageProperty());

        currentTask.setOnSucceeded(e -> Platform.runLater(() -> {
            showCompletion();
            AppState.getInstance().addRecentFile("Compress", state.getOutputFolder().toFile());
        }));

        currentTask.setOnFailed(e -> Platform.runLater(() -> {
            hideProgressOverlay();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(bundle.getString("progress.error"));
            alert.setHeaderText(bundle.getString("compress.title"));
            Throwable ex = currentTask.getException();
            alert.setContentText(ex != null ? ex.getMessage() : "Unknown error occurred");
            alert.showAndWait();
        }));

        currentTask.setOnCancelled(e -> Platform.runLater(() -> {
            hideProgressOverlay();
        }));

        showProgressOverlay(bundle.getString("compress.processing"));
        
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Show progress overlay with scrim background.
     */
    private void showProgressOverlay(String message) {
        processingPane.setVisible(true);
        processingPane.setManaged(true);
        completionPane.setVisible(false);
        completionPane.setManaged(false);
        
        progressLabel.setText(message);
        progressBar.setProgress(0);
        
        progressOverlay.setVisible(true);
        progressOverlay.setManaged(true);
    }
    
    /**
     * Update progress overlay with new value and message.
     */
    private void updateProgress(double progress, String message) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            progressLabel.setText(message);
        });
    }
    
    /**
     * Hide progress overlay.
     */
    private void hideProgressOverlay() {
        progressOverlay.setVisible(false);
        progressOverlay.setManaged(false);
    }

    /**
     * Handle cancel button during processing.
     */
    private void handleCancelProcess() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
    }

    /**
     * Show completion UI.
     */
    private void showCompletion() {
        int fileCount = state.getItems().size();
        String message = String.format(
            bundle.getString("compress.success") + "\n%d file(s) compressed",
            fileCount
        );
        completionMessage.setText(message);
        
        processingPane.setVisible(false);
        processingPane.setManaged(false);
        completionPane.setVisible(true);
        completionPane.setManaged(true);
    }

    /**
     * Handle open folder button.
     */
    private void handleOpenFolder() {
        try {
            if (state.getOutputFolder() != null && Files.exists(state.getOutputFolder())) {
                java.awt.Desktop.getDesktop().open(state.getOutputFolder().toFile());
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Cannot open folder: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Handle process another button - close overlay and reset for next compression.
     */
    private void handleProcessAnother() {
        progressOverlay.setVisible(false);
        progressOverlay.setManaged(false);
        
        processingPane.setVisible(true);
        processingPane.setManaged(true);
        completionPane.setVisible(false);
        completionPane.setManaged(false);
        
        // Optionally clear items
        // state.clearItems();
    }
}


