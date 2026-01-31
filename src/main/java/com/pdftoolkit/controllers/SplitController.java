package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.services.PdfSplitService;
import com.pdftoolkit.services.PdfThumbnailService;
import com.pdftoolkit.ui.CustomDialog;
import com.pdftoolkit.ui.PageThumbnailCard;
import com.pdftoolkit.utils.AppPaths;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redesigned Split PDF Controller with modern two-panel UI.
 * LEFT: Page thumbnail previews (all pages)
 * RIGHT: Configuration + Primary CTA
 * 
 * Now uses real PDF services (PdfSplitService, PdfThumbnailService).
 * Focuses on processing ONE file at a time with all page previews visible.
 * 
 * MODES:
 * - Split by Ranges > Custom Ranges (Manual): User manually defines ranges via Range Editor
 * - Split by Ranges > Fixed Interval (Auto): Automatically split into equal chunks
 * - Extract Pages: User specifies pages via text input OR selects pages from preview
 * 
 * IMPORTANT: Page preview selection is ONLY used in "Extract Pages" mode.
 * In "Split by Ranges" modes, page clicks only highlight/focus the page for visual reference.
 * The Range Editor remains independent of page preview interactions.
 */
public class SplitController {

    // LEFT PANEL: File staging elements (following protect style)
    @FXML private StackPane dropZonePane;  // Large drop zone when empty
    @FXML private VBox pagePreviewContainer;  // Container for page thumbnails when file loaded
    @FXML private TilePane pageGridContainer;
    @FXML private Label selectedFileInfoLabel;
    @FXML private Button removePdfButton;
    @FXML private Button deselectAllButton;
    @FXML private Button selectFilesButton;  // Button in drop zone
    
    // Zoom control elements
    @FXML private javafx.scene.layout.HBox zoomControlsBox;
    @FXML private Button zoomOutButton;
    @FXML private Slider zoomSlider;
    @FXML private Button zoomInButton;
    @FXML private Label zoomPercentLabel;
    
    // Progressive loading control elements
    @FXML private javafx.scene.layout.HBox loadControlsBox;
    @FXML private Label pageLoadStatusLabel;
    @FXML private Button loadMoreButton;
    @FXML private Button loadAllButton;
    @FXML private ProgressIndicator loadingProgressIndicator;
    
    // RIGHT PANEL: Configuration elements
    @FXML private VBox modeSection;
    @FXML private ToggleButton splitByRangeToggle;
    @FXML private ToggleButton extractPagesToggle;
    @FXML private VBox rangeTypeSection;
    @FXML private ToggleButton customRangesToggle;
    @FXML private ToggleButton fixedRangesToggle;
    
    @FXML private VBox rangeEditorSection;
    @FXML private Label rangeEditorTitle;
    @FXML private Spinner<Integer> fromPageSpinner;
    @FXML private Spinner<Integer> toPageSpinner;
    @FXML private Label validationLabel;
    
    // Extract Pages section elements
    @FXML private VBox extractPagesSection;
    @FXML private TextField extractPagesField;
    @FXML private Label extractValidationLabel;
    
    // Fixed Interval section elements
    @FXML private VBox fixedIntervalSection;
    @FXML private Spinner<Integer> pagesPerFileSpinner;
    @FXML private Label fixedIntervalValidationLabel;
    
    @FXML private VBox outputSection;
    @FXML private TextField outputFolderField;
    @FXML private Label outputInfoLabel;
    @FXML private Button browseFolderButton;
    @FXML private Button splitButton;
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
    @FXML private Button closeSuccessButton;
    
    // Delete confirmation modal elements
    @FXML private StackPane deleteConfirmOverlay;
    @FXML private Label deleteConfirmTitle;
    @FXML private Label deleteConfirmMessage;
    @FXML private Button cancelDeleteButton;
    @FXML private Button confirmDeleteButton;

    private File selectedFile;
    private int totalPages = 0;
    private final ObservableList<PageThumbnailCard> pageThumbnails = FXCollections.observableArrayList();
    private PageThumbnailCard pageToDelete; // Track page/range pending deletion
    private final PdfSplitService splitService = new PdfSplitService();
    private final PdfThumbnailService thumbnailService = new PdfThumbnailService();
    private Task<File> currentTask;
    
    // Progressive loading state
    private int loadedPageCount = 0;
    private int totalPageCount = 0;
    private static final int INITIAL_BATCH_SIZE = 30;
    private static final int LOAD_MORE_BATCH_SIZE = 20;
    private Task<Void> currentLoadTask;
    private File lastOutputFolder;
    private ToggleGroup modeToggleGroup;
    
    // Properties for empty-state binding
    private final BooleanProperty noContentProperty = new SimpleBooleanProperty(true);
    private final IntegerProperty loadedThumbnailsCountProperty = new SimpleIntegerProperty(0);
    private final BooleanProperty isLoadingProperty = new SimpleBooleanProperty(false);
    private final IntegerProperty selectedPagesCountProperty = new SimpleIntegerProperty(0);
    
    // TASK 2: Drag-to-select state
    private boolean isDragging = false;
    private boolean dragSelectionMode = true; // true = select, false = deselect
    
    // State persistence
    private final AppState.SplitToolState toolState = AppState.getInstance().getSplitToolState();
    
    // CTA binding - will be initialized in initialize()
    private BooleanBinding canSplitBinding;

    @FXML
    private void initialize() {
        // Restore state from AppState if available
        restoreToolState();
        
        // Set default output folder if not restored
        if (outputFolderField.getText() == null || outputFolderField.getText().isEmpty()) {
            outputFolderField.setText(AppPaths.getDefaultOutputPath());
        }
        
        // Bind empty-state visibility to noContent property
        // We manually update noContentProperty when state changes
        dropZonePane.visibleProperty().bind(noContentProperty);
        dropZonePane.managedProperty().bind(noContentProperty);
        
        // Bind page preview container visibility to inverse of empty state
        pagePreviewContainer.visibleProperty().bind(noContentProperty.not());
        pagePreviewContainer.managedProperty().bind(noContentProperty.not());
        
        // Initialize empty state to true
        updateEmptyState();
        
        // Setup drag & drop on the left empty-state panel
        setupDragAndDrop();
        setupLeftPanelDragDrop();
        
        // Setup mode toggle group
        modeToggleGroup = new ToggleGroup();
        splitByRangeToggle.setToggleGroup(modeToggleGroup);
        extractPagesToggle.setToggleGroup(modeToggleGroup);
        splitByRangeToggle.setSelected(true);
        
        // Mode change listener to toggle between range editor and extract pages section
        modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == splitByRangeToggle) {
                // Show range editor, hide extract pages
                rangeEditorSection.setVisible(true);
                rangeEditorSection.setManaged(true);
                extractPagesSection.setVisible(false);
                extractPagesSection.setManaged(false);
                rangeTypeSection.setVisible(true);
                rangeTypeSection.setManaged(true);
                // Enable range editor if file is loaded (allow adding first range)
                if (selectedFile != null) {
                    // Only enable if in custom ranges mode (not fixed interval)
                    rangeEditorSection.setDisable(fixedRangesToggle.isSelected());
                }
                // Clear page selections when leaving Extract mode
                clearPageSelections();
            } else if (newToggle == extractPagesToggle) {
                // Show extract pages, hide range editor
                rangeEditorSection.setVisible(false);
                rangeEditorSection.setManaged(false);
                extractPagesSection.setVisible(true);
                extractPagesSection.setManaged(true);
                rangeTypeSection.setVisible(false);
                rangeTypeSection.setManaged(false);
                // Enable extract pages section if file is loaded
                if (selectedFile != null) {
                    extractPagesSection.setDisable(false);
                }
            }
            updateUI();
        });
        
        // Setup range type toggle group
        ToggleGroup rangeTypeGroup = new ToggleGroup();
        customRangesToggle.setToggleGroup(rangeTypeGroup);
        fixedRangesToggle.setToggleGroup(rangeTypeGroup);
        customRangesToggle.setSelected(true);
        
        // Range type change listener to toggle between custom ranges and fixed interval
        rangeTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == customRangesToggle) {
                // Show custom range editor, hide fixed interval
                rangeEditorSection.setVisible(true);
                rangeEditorSection.setManaged(true);
                fixedIntervalSection.setVisible(false);
                fixedIntervalSection.setManaged(false);
                // Always enable range editor in custom ranges mode (allow adding first range)
                if (selectedFile != null) {
                    rangeEditorSection.setDisable(false);
                }
            } else if (newToggle == fixedRangesToggle) {
                // Show fixed interval, hide custom range editor
                rangeEditorSection.setVisible(false);
                rangeEditorSection.setManaged(false);
                fixedIntervalSection.setVisible(true);
                fixedIntervalSection.setManaged(true);
                if (selectedFile != null) {
                    fixedIntervalSection.setDisable(false);
                }
            }
            updateUI();
        });
        
        // Setup spinners (will be configured when file is loaded)
        fromPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        toPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        pagesPerFileSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 5));
        
        // Spinner listeners for validation
        fromPageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateCurrentRange();
        });
        toPageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateCurrentRange();
        });
        
        // Extract pages field listener for validation
        extractPagesField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedFile != null && extractPagesToggle.isSelected()) {
                validateExtractPages();
                
                // If user types in extract field, clear page selections
                if (newVal != null && !newVal.trim().isEmpty()) {
                    clearPageSelections();
                }
            }
            updateUI();
        });
        
        // Setup drag & drop
        setupDragAndDrop();
        
        // Setup zoom slider listener
        if (zoomSlider != null) {
            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateZoomPercentLabel(newVal.doubleValue());
                // Save zoom level to state (TASK A)
                toolState.setZoomLevel(newVal.doubleValue());
                // Re-render thumbnails with new zoom level
                if (selectedFile != null && !pageThumbnails.isEmpty()) {
                    reRenderLoadedThumbnails(newVal.doubleValue());
                }
            });
            // Initialize zoom percent label
            updateZoomPercentLabel(1.0);
        }
        
        // Setup output folder listener to save state
        outputFolderField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                toolState.setOutputFolder(newVal);
            }
        });
        
        // Setup locale change listener - IMPORTANT: only update labels, NOT state
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> refreshLabels());
        
        // Setup canSplit binding (TASK C)
        setupCanSplitBinding();
        
        // Initial UI update
        updateUI();
    }
    
    /**
     * Restore tool state from AppState if a PDF was previously loaded.
     * This enables state persistence across navigation and language/theme changes.
     */
    private void restoreToolState() {
        File savedFile = toolState.getSelectedFile();
        if (savedFile != null && savedFile.exists()) {
            // Restore the file without clearing state
            setSelectedFile(savedFile);
        }
        
        // Restore zoom level
        if (zoomSlider != null && toolState.getZoomLevel() > 0) {
            zoomSlider.setValue(toolState.getZoomLevel());
        }
        
        // Restore output folder
        String savedOutput = toolState.getOutputFolder();
        if (savedOutput != null && !savedOutput.isEmpty()) {
            outputFolderField.setText(savedOutput);
        }
    }
    
    /**
     * Setup the canSplit binding that determines when the Split button is enabled.
     * This binding observes all relevant state and re-evaluates automatically.
     */
    private void setupCanSplitBinding() {
        // Create boolean binding that checks all conditions
        canSplitBinding = Bindings.createBooleanBinding(() -> {
            // Must have a selected file
            if (selectedFile == null || totalPages == 0) {
                return false;
            }
            
            // Must have valid output folder
            String outputPath = outputFolderField.getText();
            if (outputPath == null || outputPath.isEmpty()) {
                return false;
            }
            File outputDir = new File(outputPath);
            if (!outputDir.exists() || !outputDir.isDirectory()) {
                return false;
            }
            
            // Mode-specific validation
            if (extractPagesToggle.isSelected()) {
                // Extract mode: must have page selection OR valid page spec (not both)
                String pageSpec = extractPagesField.getText();
                boolean hasPageSpec = pageSpec != null && !pageSpec.trim().isEmpty();
                boolean hasSelectedPages = selectedPagesCountProperty.get() > 0;
                
                if (hasPageSpec) {
                    // Validate page spec
                    try {
                        com.pdftoolkit.utils.PageRangeParser.parse(pageSpec.trim(), totalPages);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                } else if (hasSelectedPages) {
                    // Has page selection
                    return true;
                }
                return false;
            } else if (fixedRangesToggle.isSelected()) {
                // Fixed interval mode: validate pages per file
                int pagesPerFile = pagesPerFileSpinner.getValue() != null ? pagesPerFileSpinner.getValue() : 0;
                return pagesPerFile > 0 && pagesPerFile <= totalPages;
            } else {
                // Custom ranges mode: 
                // TASK 1: If pages are selected, use them (page selection takes priority)
                // Otherwise, validate range in spinners
                if (selectedPagesCountProperty.get() > 0) {
                    return true; // Has page selection
                }
                
                // Validate range in spinners
                Integer from = fromPageSpinner.getValue();
                Integer to = toPageSpinner.getValue();
                
                if (from == null || to == null) {
                    return false;
                }
                
                // Check if range is valid (from <= to and within bounds)
                return from <= to && from >= 1 && to <= totalPages;
            }
        },
        // Observable dependencies
        outputFolderField.textProperty(),
        extractPagesField.textProperty(),
        fromPageSpinner.valueProperty(),
        toPageSpinner.valueProperty(),
        pagesPerFileSpinner.valueProperty(),
        modeToggleGroup.selectedToggleProperty(),
        selectedPagesCountProperty  // Listen to page selection changes
        );
        
        // Bind the split button's disable property (disable when canSplit is false)
        splitButton.disableProperty().bind(canSplitBinding.not());
    }
    
    /**
     * Refresh UI labels without clearing state (for locale/theme changes).
     */
    private void refreshLabels() {
        // Update file info label if file is loaded
        if (selectedFile != null) {
            String fileInfo = String.format(LocaleManager.getString("split.filesPane.fileInfo"), 
                                           selectedFile.getName(), totalPages);
            selectedFileInfoLabel.setText(fileInfo);
        } else {
            selectedFileInfoLabel.setText(LocaleManager.getString("split.filesPane.emptyState"));
        }
        
        // Update load status if applicable
        if (loadedPageCount > 0) {
            updateLoadStatus();
        }
        
        // Refresh other dynamic labels as needed
        updateUI();
    }
    
    private void validateExtractPages() {
        String pageSpec = extractPagesField.getText();
        if (pageSpec == null || pageSpec.trim().isEmpty()) {
            extractValidationLabel.setVisible(false);
            extractValidationLabel.setManaged(false);
            return;
        }
        
        try {
            com.pdftoolkit.utils.PageRangeParser.parse(pageSpec.trim(), totalPages);
            extractValidationLabel.setVisible(false);
            extractValidationLabel.setManaged(false);
        } catch (IllegalArgumentException e) {
            extractValidationLabel.setText(e.getMessage());
            extractValidationLabel.setVisible(true);
            extractValidationLabel.setManaged(true);
        }
    }
    
    private void setupDragAndDrop() {
        // Right panel drag-drop removed (TASK F)
        // Only left panel drop zone is active
    }
    
    /**
     * Update the noContent property based on current state.
     * Call this whenever selectedFile, totalPages, or loading state changes.
     */
    private void updateEmptyState() {
        boolean noContent = selectedFile == null || totalPages == 0 || 
                           (isLoadingProperty.get() && loadedThumbnailsCountProperty.get() == 0) || 
                           (!isLoadingProperty.get() && pageThumbnails.isEmpty());
        noContentProperty.set(noContent);
    }
    
    /**
     * Setup drag-and-drop for the left panel empty-state.
     * This makes the entire empty-state area the primary drop target.
     */
    private void setupLeftPanelDragDrop() {
        dropZonePane.setOnDragOver(this::handleLeftPanelDragOver);
        dropZonePane.setOnDragDropped(this::handleLeftPanelDragDropped);
        dropZonePane.setOnDragExited(event -> {
            dropZonePane.getStyleClass().remove("drag-over");
            event.consume();
        });
    }
    
    private void handleLeftPanelDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles() && selectedFile == null) {
            // Check if it's a PDF file
            File file = db.getFiles().get(0);
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                event.acceptTransferModes(TransferMode.COPY);
                if (!dropZonePane.getStyleClass().contains("drag-over")) {
                    dropZonePane.getStyleClass().add("drag-over");
                }
            }
        }
        event.consume();
    }
    
    private void handleLeftPanelDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        
        if (db.hasFiles()) {
            File file = db.getFiles().get(0);
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                setSelectedFile(file);
                success = true;
            } else {
                // Show gentle feedback for invalid drops
                showAlert(LocaleManager.getString("split.dropZone.invalidFile"), Alert.AlertType.WARNING);
            }
        }
        
        event.setDropCompleted(success);
        dropZonePane.getStyleClass().remove("drag-over");
        event.consume();
    }
    
    // Right panel drag-drop handlers removed (TASK F - only left panel accepts drops)
    
    @FXML
    private void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LocaleManager.getString("split.selectFile"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showOpenDialog(selectFilesButton.getScene().getWindow());
        if (file != null) {
            setSelectedFile(file);
        }
    }
    
    private void setSelectedFile(File file) {
        selectedFile = file;
        
        // Save to AppState for persistence across navigation (TASK A, B)
        toolState.setSelectedFile(file);
        
        // Clear existing thumbnails
        pageThumbnails.clear();
        pageGridContainer.getChildren().clear();
        loadedThumbnailsCountProperty.set(0);
        isLoadingProperty.set(true);
        
        // Load actual page count using thumbnail service
        Task<Integer> loadPageCountTask = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return thumbnailService.getPageCount(file);
            }
        };
        
        loadPageCountTask.setOnSucceeded(e -> {
            totalPages = loadPageCountTask.getValue();
            totalPageCount = totalPages; // Set progressive loading total
            loadedPageCount = 0; // Reset loaded count
            
            // Save total pages to state
            toolState.setTotalPages(totalPages);
            
            // Update UI
            String fileInfo = String.format(LocaleManager.getString("split.filesPane.fileInfo"), 
                                           file.getName(), totalPages);
            selectedFileInfoLabel.setText(fileInfo);
            
            // Show removePdfButton
            if (removePdfButton != null) {
                removePdfButton.setVisible(true);
                removePdfButton.setManaged(true);
            }
            
            // Show page grid container and zoom controls
            if (zoomControlsBox != null) {
                zoomControlsBox.setVisible(true);
                zoomControlsBox.setManaged(true);
            }
            if (loadControlsBox != null && totalPageCount > INITIAL_BATCH_SIZE) {
                loadControlsBox.setVisible(true);
                loadControlsBox.setManaged(true);
            }
            
            // Configure spinners with actual page range
            fromPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, totalPages, 1));
            toPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, totalPages, totalPages));
            
            // Enable sections based on current mode
            modeSection.setDisable(false);
            outputSection.setDisable(false);
            
            // Enable appropriate sections based on selected mode
            if (extractPagesToggle.isSelected()) {
                extractPagesSection.setDisable(false);
                rangeEditorSection.setDisable(true);
                fixedIntervalSection.setDisable(true);
            } else if (fixedRangesToggle.isSelected()) {
                fixedIntervalSection.setDisable(false);
                rangeEditorSection.setDisable(true);
                extractPagesSection.setDisable(true);
            } else {
                // Custom ranges mode (default)
                rangeEditorSection.setDisable(false);
                fixedIntervalSection.setDisable(true);
                extractPagesSection.setDisable(true);
            }
            
            // Start progressive loading with initial batch
            int initialBatch = calculateInitialBatchSize();
            loadPageBatch(1, Math.min(initialBatch, totalPageCount));
            
            // Update empty state now that we have content
            updateEmptyState();
            
            updateUI();
        });
        
        loadPageCountTask.setOnFailed(e -> {
            showAlert("Failed to read PDF: " + file.getName(), Alert.AlertType.ERROR);
            isLoadingProperty.set(false);
            updateEmptyState();
        });
        
        Thread thread = new Thread(loadPageCountTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Load thumbnails for all pages in the PDF and display them in the grid.
     */
    private void loadAllPageThumbnails() {
        // Create thumbnail cards for all pages
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            PageThumbnailCard card = new PageThumbnailCard(pageNum);
            card.setLoading(true); // Show loading state initially
            pageThumbnails.add(card);
            pageGridContainer.getChildren().add(card);
        }
        
        // Load thumbnails asynchronously
        Task<Void> loadThumbnailsTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < pageThumbnails.size(); i++) {
                    final int index = i;
                    final int pageNum = i + 1;
                    
                    if (isCancelled()) {
                        break;
                    }
                    
                    // Generate thumbnail
                    Image thumbnail = thumbnailService.generateThumbnail(selectedFile, pageNum, 120);
                    
                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        if (index < pageThumbnails.size()) {
                            pageThumbnails.get(index).setThumbnail(thumbnail);
                        }
                    });
                    
                    // Small delay to avoid overwhelming the system
                    Thread.sleep(50);
                }
                return null;
            }
        };
        
        loadThumbnailsTask.setOnFailed(e -> {
            System.err.println("Failed to load thumbnails: " + e.getSource().getException().getMessage());
        });
        
        Thread thread = new Thread(loadThumbnailsTask);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void validateCurrentRange() {
        if (fromPageSpinner.getValue() == null || toPageSpinner.getValue() == null) {
            return;
        }
        
        int from = fromPageSpinner.getValue();
        int to = toPageSpinner.getValue();
        
        if (from > to) {
            validationLabel.setText(LocaleManager.getString("split.rangeInvalid"));
            validationLabel.setVisible(true);
            validationLabel.setManaged(true);
        } else if (from > totalPages || to > totalPages) {
            validationLabel.setText(LocaleManager.getString("split.rangeOutOfBounds"));
            validationLabel.setVisible(true);
            validationLabel.setManaged(true);
        } else {
            validationLabel.setVisible(false);
            validationLabel.setManaged(false);
        }
    }
    
    private void updateUI() {
        boolean hasFile = selectedFile != null;
        boolean isExtractMode = extractPagesToggle.isSelected();
        boolean isCustomRangeMode = !isExtractMode && customRangesToggle != null && customRangesToggle.isSelected();
        boolean hasSelectedPages = selectedPagesCountProperty.get() > 0;
        boolean hasValidExtractSpec = false;
        boolean hasExtractText = extractPagesField.getText() != null && !extractPagesField.getText().trim().isEmpty();
        
        // Check if Extract Pages mode has valid spec
        if (isExtractMode && hasFile && hasExtractText) {
            String pageSpec = extractPagesField.getText();
            try {
                com.pdftoolkit.utils.PageRangeParser.parse(pageSpec.trim(), totalPages);
                hasValidExtractSpec = true;
            } catch (IllegalArgumentException e) {
                hasValidExtractSpec = false;
            }
        }
        
        // In Extract mode, disable text field if pages are selected, and vice versa
        if (isExtractMode && extractPagesField != null) {
            extractPagesField.setDisable(hasSelectedPages);
            if (hasSelectedPages) {
                extractPagesField.setPromptText("Clear page selections to use text input");
            } else {
                extractPagesField.setPromptText("e.g., 1-3, 5, 7-9");
            }
        }
        
        // TASK 1: In Custom Range mode, disable Range Editor when pages are selected
        if (isCustomRangeMode && rangeEditorSection != null) {
            boolean shouldDisableRangeEditor = hasSelectedPages;
            rangeEditorSection.setDisable(shouldDisableRangeEditor);
            
            // Update validation label to show helpful message
            if (shouldDisableRangeEditor && validationLabel != null) {
                validationLabel.setText(LocaleManager.getString("split.rangeEditor.disabledDueToSelection"));
                validationLabel.setVisible(true);
                validationLabel.setManaged(true);
                validationLabel.getStyleClass().remove("validation-error");
                validationLabel.getStyleClass().add("hint-label");
            } else if (!shouldDisableRangeEditor && validationLabel != null) {
                // Restore normal validation behavior
                validationLabel.getStyleClass().remove("hint-label");
                validationLabel.getStyleClass().add("validation-error");
                validateCurrentRange();
            }
        }
        
        // Note: Empty state visibility is now managed by property bindings
        // pageEmptyStatePane visibility is bound to noContentProperty in initialize()
        
        // Note: Split button disable state is now managed by canSplitBinding in setupCanSplitBinding()
        // The binding automatically observes all relevant properties and updates the button state
    }
    
    private boolean isValid() {
        if (selectedFile == null) return false;
        
        // Check mode-specific validation
        if (extractPagesToggle.isSelected()) {
            // Extract Pages mode: validate page specification OR page selection
            String pageSpec = extractPagesField.getText();
            boolean hasPageSpec = pageSpec != null && !pageSpec.trim().isEmpty();
            boolean hasSelectedPages = selectedPagesCountProperty.get() > 0;
            
            if (!hasPageSpec && !hasSelectedPages) {
                // Neither page spec nor selection
                extractValidationLabel.setText(LocaleManager.getString("validation.emptyPageSpec"));
                extractValidationLabel.setVisible(true);
                extractValidationLabel.setManaged(true);
                return false;
            }
            
            if (hasPageSpec) {
                // Validate page spec
                try {
                    com.pdftoolkit.utils.PageRangeParser.parse(pageSpec.trim(), totalPages);
                    extractValidationLabel.setVisible(false);
                    extractValidationLabel.setManaged(false);
                } catch (IllegalArgumentException e) {
                    extractValidationLabel.setText(e.getMessage());
                    extractValidationLabel.setVisible(true);
                    extractValidationLabel.setManaged(true);
                    return false;
                }
            } else {
                // Has page selection - clear validation
                extractValidationLabel.setVisible(false);
                extractValidationLabel.setManaged(false);
            }
        } else if (fixedRangesToggle.isSelected()) {
            // Fixed Interval mode: validate pages per file
            int pagesPerFile = pagesPerFileSpinner.getValue();
            if (pagesPerFile < 1 || pagesPerFile > totalPages) {
                fixedIntervalValidationLabel.setText(LocaleManager.getString("validation.invalidInterval"));
                fixedIntervalValidationLabel.setVisible(true);
                fixedIntervalValidationLabel.setManaged(true);
                return false;
            }
            fixedIntervalValidationLabel.setVisible(false);
            fixedIntervalValidationLabel.setManaged(false);
        } else {
            // Split by Range mode (custom ranges): validate range in spinners
            Integer from = fromPageSpinner.getValue();
            Integer to = toPageSpinner.getValue();
            
            if (from == null || to == null) return false;
            if (from > to || from < 1 || to > totalPages) return false;
        }
        
        String outputPath = outputFolderField.getText();
        if (outputPath == null || outputPath.isEmpty()) return false;
        
        File outputDir = new File(outputPath);
        if (!outputDir.exists() || !outputDir.isDirectory()) return false;
        
        return true;
    }
    
    @FXML
    private void handleBrowseOutputFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LocaleManager.getString("split.chooseFolder"));
        
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
    private void handleSplit() {
        if (!isValid()) {
            // Show appropriate error message based on mode
            String errorMessage;
            if (extractPagesToggle.isSelected()) {
                errorMessage = LocaleManager.getString("validation.emptyPageSpec");
            } else if (fixedRangesToggle.isSelected()) {
                errorMessage = LocaleManager.getString("validation.invalidInterval");
            } else {
                // Custom ranges mode
                errorMessage = LocaleManager.getString("validation.noCustomRanges");
            }
            showAlert(errorMessage, Alert.AlertType.WARNING);
            return;
        }
        
        // Show progress overlay
        showProgressOverlay();
        
        File outputFolder = new File(outputFolderField.getText());
        lastOutputFolder = outputFolder;
        
        // Check which mode is selected
        boolean isExtractMode = extractPagesToggle.isSelected();
        boolean isFixedRangeMode = fixedRangesToggle.isSelected();
        
        if (isExtractMode) {
            handleExtractPages(outputFolder);
        } else if (isFixedRangeMode) {
            handleFixedIntervalSplit(outputFolder);
        } else {
            handleSplitByRanges(outputFolder);
        }
    }
    
    private void handleSplitByRanges(File outputFolder) {
        // TASK 1: Check if pages are selected (takes priority over range editor)
        boolean hasSelectedPages = selectedPagesCountProperty.get() > 0;
        
        if (hasSelectedPages) {
            // When pages are selected, create a SINGLE PDF with all selected pages
            handleExtractSelectedPagesAsSingleFile(outputFolder);
        } else {
            // When using range editor, extract that specific range as a single file
            handleExtractRangeAsSingleFile(outputFolder);
        }
    }
    
    /**
     * Extract selected pages into a single PDF file.
     * This handles non-consecutive page selections (e.g., 1,3,5,7 → one file with those pages)
     */
    private void handleExtractSelectedPagesAsSingleFile(File outputFolder) {
        List<Integer> selectedPageNumbers = pageThumbnails.stream()
            .filter(PageThumbnailCard::isSelected)
            .map(PageThumbnailCard::getPageNumber)
            .sorted()
            .collect(Collectors.toList());
        
        final String baseFileName = selectedFile.getName().replace(".pdf", "");
        
        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Extracting selected pages...");
                updateProgress(0, 3);
                Thread.sleep(200);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage(String.format("Creating PDF with %d selected pages...", selectedPageNumbers.size()));
                updateProgress(1, 3);
                
                // Use extractPages to create a single combined file with unique name
                String outputFileName = String.format("%s_selected_pages.pdf", baseFileName);
                File outputFile = getUniqueOutputFile(outputFolder, outputFileName);
                
                // Extract all selected pages into ONE file
                List<File> result = splitService.extractPagesAsSingleFile(
                    selectedFile, selectedPageNumbers, outputFile
                );
                
                updateProgress(2, 3);
                Thread.sleep(200);
                
                updateMessage("Extraction complete! Created: " + outputFile.getName());
                updateProgress(3, 3);
                
                return result.isEmpty() ? null : result.get(0);
            }
        };
        
        executeTask();
    }
    
    /**
     * Extract a range from spinners into a single PDF file.
     */
    private void handleExtractRangeAsSingleFile(File outputFolder) {
        int from = fromPageSpinner.getValue();
        int to = toPageSpinner.getValue();
        
        // Convert range to page list
        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            pageNumbers.add(i);
        }
        
        final String baseFileName = selectedFile.getName().replace(".pdf", "");
        
        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Extracting page range...");
                updateProgress(0, 3);
                Thread.sleep(200);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage(String.format("Creating PDF with pages %d-%d...", from, to));
                updateProgress(1, 3);
                
                String outputFileName = String.format("%s_pages_%d-%d.pdf", baseFileName, from, to);
                File outputFile = getUniqueOutputFile(outputFolder, outputFileName);
                
                // Extract range into ONE file
                List<File> result = splitService.extractPagesAsSingleFile(
                    selectedFile, pageNumbers, outputFile
                );
                
                updateProgress(2, 3);
                Thread.sleep(200);
                
                updateMessage("Extraction complete! Created: " + outputFile.getName());
                updateProgress(3, 3);
                
                return result.isEmpty() ? null : result.get(0);
            }
        };
        
        executeTask();
    }
    
    private void handleFixedIntervalSplit(File outputFolder) {
        int pagesPerFile = pagesPerFileSpinner.getValue();
        final String baseFileName = selectedFile.getName().replace(".pdf", "");
        
        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Calculating split intervals...");
                updateProgress(0, totalPages + 2);
                Thread.sleep(300);
                
                if (isCancelled()) {
                    return null;
                }
                
                // Calculate how many files we'll create
                int fileCount = (int) Math.ceil((double) totalPages / pagesPerFile);
                
                updateMessage("Splitting into " + fileCount + " files...");
                updateProgress(1, fileCount + 2);
                Thread.sleep(200);
                
                // Build page ranges for fixed intervals
                List<PdfSplitService.PageRange> pageRanges = new java.util.ArrayList<>();
                for (int i = 0; i < totalPages; i += pagesPerFile) {
                    int fromPage = i + 1;
                    int toPage = Math.min(i + pagesPerFile, totalPages);
                    pageRanges.add(new PdfSplitService.PageRange(fromPage, toPage));
                }
                
                // Perform actual split using PdfSplitService
                List<File> outputFiles = splitService.splitPdfByRanges(
                    selectedFile, pageRanges, outputFolder, baseFileName
                );
                
                // Update progress for each file
                for (int i = 0; i < outputFiles.size(); i++) {
                    if (isCancelled()) {
                        return null;
                    }
                    updateMessage(String.format("Created file %d of %d", i + 1, outputFiles.size()));
                    updateProgress(i + 2, fileCount + 2);
                    Thread.sleep(200);
                }
                
                updateMessage("Split complete! Created " + outputFiles.size() + " files.");
                updateProgress(fileCount + 2, fileCount + 2);
                
                return outputFolder;
            }
        };
        
        executeTask();
    }
    
    private void handleExtractPages(File outputFolder) {
        // Check if using page spec or page selection
        String pageSpec = extractPagesField.getText().trim();
        boolean usePageSelection = pageSpec.isEmpty() && selectedPagesCountProperty.get() > 0;
        
        final String baseFileName = selectedFile.getName().replace(".pdf", "");
        
        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Parsing page specification...");
                updateProgress(0, 4);
                Thread.sleep(200);
                
                if (isCancelled()) {
                    return null;
                }
                
                // Parse page specification or get selected pages
                List<Integer> pageNumbers;
                if (usePageSelection) {
                    // Use selected pages from thumbnails
                    pageNumbers = pageThumbnails.stream()
                        .filter(PageThumbnailCard::isSelected)
                        .map(PageThumbnailCard::getPageNumber)
                        .sorted()
                        .collect(Collectors.toList());
                } else {
                    // Parse text specification
                    try {
                        pageNumbers = com.pdftoolkit.utils.PageRangeParser.parse(pageSpec, totalPages);
                    } catch (IllegalArgumentException e) {
                        Platform.runLater(() -> {
                            extractValidationLabel.setText(e.getMessage());
                            extractValidationLabel.setVisible(true);
                            extractValidationLabel.setManaged(true);
                        });
                        throw e;
                    }
                }
                
                updateMessage("Extracting " + pageNumbers.size() + " pages...");
                updateProgress(1, 3);
                Thread.sleep(200);
                
                if (isCancelled()) {
                    return null;
                }
                
                // Generate output filename based on page selection
                String outputFileName;
                if (pageNumbers.size() == 1) {
                    outputFileName = String.format("%s_page_%d.pdf", baseFileName, pageNumbers.get(0));
                } else {
                    outputFileName = String.format("%s_extracted_pages.pdf", baseFileName);
                }
                File outputFile = getUniqueOutputFile(outputFolder, outputFileName);
                
                // Extract pages into ONE file
                List<File> outputFiles = splitService.extractPagesAsSingleFile(
                    selectedFile, pageNumbers, outputFile
                );
                
                updateMessage("Creating PDF with " + pageNumbers.size() + " pages...");
                updateProgress(2, 3);
                Thread.sleep(200);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Extraction complete! Created: " + outputFile.getName());
                updateProgress(3, 3);
                
                return outputFolder;
            }
        };
        
        executeTask();
    }
    
    private void executeTask() {
        // Bind progress
        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressMessage.textProperty().bind(currentTask.messageProperty());
        
        currentTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                File result = currentTask.getValue();
                if (result != null) {
                    showSuccess(LocaleManager.getString("split.success") + "\n\nOutput folder: " + result.getName());
                }
            });
        });
        
        currentTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                hideProgressOverlay();
                Throwable ex = currentTask.getException();
                showAlert("Split Error: " + (ex != null ? ex.getMessage() : "Unknown error"), 
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
        if (lastOutputFolder != null && lastOutputFolder.exists()) {
            try {
                java.awt.Desktop.getDesktop().open(lastOutputFolder);
            } catch (Exception ex) {
                showAlert("Cannot open folder: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    @FXML
    private void handleProcessAnother() {
        hideProgressOverlay();
        selectedFile = null;
        totalPages = 0;
        totalPageCount = 0;
        loadedPageCount = 0;
        selectedFileInfoLabel.setText(LocaleManager.getString("split.noFileSelected"));
        
        // Clear page thumbnails and reset properties
        pageThumbnails.clear();
        pageGridContainer.getChildren().clear();
        loadedThumbnailsCountProperty.set(0);
        isLoadingProperty.set(false);
        
        // Update empty state
        updateEmptyState();
        
        modeSection.setDisable(true);
        rangeEditorSection.setDisable(true);
        outputSection.setDisable(true);
        
        updateUI();
    }
    
    @FXML
    private void handleCloseSuccess() {
        hideProgressOverlay();
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
        // Unbind properties before hiding to prevent binding errors
        progressBar.progressProperty().unbind();
        progressMessage.textProperty().unbind();
        
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
        String title = switch (type) {
            case ERROR -> "Error";
            case WARNING -> "Warning";
            case INFORMATION -> "Information";
            default -> "Alert";
        };
        
        switch (type) {
            case ERROR -> CustomDialog.showError(title, message);
            case WARNING -> CustomDialog.showWarning(title, message);
            case INFORMATION -> CustomDialog.showInfo(title, message);
            default -> CustomDialog.showInfo(title, message);
        }
    }
    
    /**
     * Show the delete confirmation modal for a specific item.
     * @param itemDescription Description of what's being deleted (e.g., "Page 5" or "Range 1-10")
     * @param onConfirm Callback to execute if user confirms deletion
     */
    private void showDeleteConfirmation(String itemDescription, Runnable onConfirm) {
        deleteConfirmMessage.setText(String.format(
            LocaleManager.getString("split.confirmDeleteMessage") + "\n\n%s", 
            itemDescription
        ));
        
        // Store the confirmation action
        confirmDeleteButton.setOnAction(e -> {
            hideDeleteConfirmation();
            onConfirm.run();
        });
        
        // Show modal
        deleteConfirmOverlay.setVisible(true);
        deleteConfirmOverlay.setManaged(true);
    }
    
    /**
     * Handle Remove PDF action (TASK E).
     * Safely clears the loaded PDF and resets UI to empty state.
     */
    @FXML
    private void handleRemovePdf() {
        // Cancel any ongoing thumbnail rendering tasks
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }
        
        // Clear thumbnail cache (if using caching)
        thumbnailService.clearCache();
        
        // Clear state
        selectedFile = null;
        totalPages = 0;
        totalPageCount = 0;
        loadedPageCount = 0;
        
        // Clear collections
        pageThumbnails.clear();
        pageGridContainer.getChildren().clear();
        
        // Reset properties
        loadedThumbnailsCountProperty.set(0);
        isLoadingProperty.set(false);
        
        // Reset zoom to default
        if (zoomSlider != null) {
            zoomSlider.setValue(1.0);
        }
        
        // Clear AppState (TASK A, B - only cleared on explicit user action)
        toolState.clear();
        
        // Hide controls
        if (removePdfButton != null) {
            removePdfButton.setVisible(false);
            removePdfButton.setManaged(false);
        }
        if (zoomControlsBox != null) {
            zoomControlsBox.setVisible(false);
            zoomControlsBox.setManaged(false);
        }
        if (loadControlsBox != null) {
            loadControlsBox.setVisible(false);
            loadControlsBox.setManaged(false);
        }
        
        // Disable configuration sections
        modeSection.setDisable(true);
        rangeEditorSection.setDisable(true);
        outputSection.setDisable(true);
        extractPagesSection.setDisable(true);
        
        // Update file info label
        selectedFileInfoLabel.setText(LocaleManager.getString("split.noFileSelected"));
        
        // Update empty state
        updateEmptyState();
        updateUI();
    }
    
    @FXML
    private void handleCancelDelete() {
        hideDeleteConfirmation();
    }
    
    @FXML
    private void handleConfirmDelete() {
        // This will be set dynamically by showDeleteConfirmation
    }
    
    private void hideDeleteConfirmation() {
        deleteConfirmOverlay.setVisible(false);
        deleteConfirmOverlay.setManaged(false);
        pageToDelete = null;
    }
    
    // ==========================================
    // Zoom Control Handlers
    // ==========================================
    
    @FXML
    private void handleZoomIn() {
        if (zoomSlider != null) {
            double current = zoomSlider.getValue();
            double newZoom = Math.min(current + 0.5, 2.0); // Step by 0.5 for column changes
            zoomSlider.setValue(newZoom);
        }
    }
    
    @FXML
    private void handleZoomOut() {
        if (zoomSlider != null) {
            double current = zoomSlider.getValue();
            double newZoom = Math.max(current - 0.5, 1.0); // Step by 0.5 for column changes
            zoomSlider.setValue(newZoom);
        }
    }
    
    private void updateZoomPercentLabel(double zoom) {
        if (zoomPercentLabel != null) {
            // Convert zoom level to column count display
            int columns = getColumnsForZoom(zoom);
            ResourceBundle messages = LocaleManager.getBundle();
            zoomPercentLabel.setText(columns + " cols");
        }
    }
    
    private int getColumnsForZoom(double zoom) {
        // 1.0 = 3 columns, 1.5 = 2 columns, 2.0 = 1 column
        if (zoom >= 2.0) return 1;
        if (zoom >= 1.5) return 2;
        return 3;
    }
    
    private void reRenderLoadedThumbnails(double newZoom) {
        if (selectedFile == null || pageGridContainer == null) {
            return;
        }
        
        // Adjust columns based on zoom level - NO RE-RENDERING
        int columns = getColumnsForZoom(newZoom);
        pageGridContainer.setPrefColumns(columns);
        
        // Adjust tile sizes for better fit
        double tileWidth = 150 * (3.0 / columns); // Scale width inversely to columns
        double tileHeight = 200 * (3.0 / columns);
        pageGridContainer.setPrefTileWidth(tileWidth);
        pageGridContainer.setPrefTileHeight(tileHeight);
        
        // Update card zoom property for visual scaling (CSS only)
        for (PageThumbnailCard card : pageThumbnails) {
            card.zoomProperty().set(newZoom);
        }
    }
    
    // ==========================================
    // Progressive Loading Handlers
    // ==========================================
    
    private int calculateInitialBatchSize() {
        // Return configured initial batch size (30 pages)
        // This allows ~3 columns × 10 rows on typical screen
        return INITIAL_BATCH_SIZE;
    }
    
    private void loadPageBatch(int startPage, int endPage) {
        if (selectedFile == null) {
            return;
        }
        
        // Cancel any ongoing load task
        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }
        
        // Set loading state
        isLoadingProperty.set(true);
        
        Platform.runLater(() -> {
            if (loadingProgressIndicator != null) {
                loadingProgressIndicator.setVisible(true);
                loadingProgressIndicator.setManaged(true);
            }
            if (loadMoreButton != null) loadMoreButton.setDisable(true);
            if (loadAllButton != null) loadAllButton.setDisable(true);
        });
        
        currentLoadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                double zoom = zoomSlider != null ? zoomSlider.getValue() : 1.0;
                
                // TASK D: Load pages in batch, ensuring page numbers are correctly assigned
                // Page label is derived from actual pageIndex (i) which matches 1-based page numbers
                for (int i = startPage; i <= endPage && i <= totalPageCount; i++) {
                    if (isCancelled()) {
                        break;
                    }
                    
                    // pageNum is the actual 1-based page number (matches what users see)
                    final int pageNum = i;
                    PageThumbnailCard card = new PageThumbnailCard(pageNum);
                    card.zoomProperty().set(zoom);
                    card.setLoading(true);
                    
                    // Set delete action
                    card.setOnDelete(() -> handlePageDelete(card));
                    
                    // Set selection change listener
                    card.setOnSelectionChanged(() -> {
                        updateDeselectButtonVisibility();
                        updateSelectedPagesCount();
                        
                        // If user selects a page in Extract mode, clear the text field
                        if (extractPagesToggle.isSelected() && card.isSelected()) {
                            Platform.runLater(() -> {
                                if (!extractPagesField.getText().isEmpty()) {
                                    extractPagesField.clear();
                                }
                            });
                        }
                        
                        // Update UI to handle range editor state (TASK 1)
                        updateUI();
                    });
                    
                    // TASK 2: Setup drag-to-select functionality
                    setupDragSelection(card);
                    
                    Platform.runLater(() -> {
                        pageThumbnails.add(card);
                        pageGridContainer.getChildren().add(card);
                        loadedThumbnailsCountProperty.set(pageThumbnails.size());
                    });
                    
                    // Render thumbnail asynchronously
                    thumbnailService.generateThumbnailAsync(selectedFile, pageNum - 1, zoom)
                        .thenAccept(result -> Platform.runLater(() -> {
                            if (result != null && result.image() != null) {
                                card.setLoading(false);
                                card.setThumbnail(result.image());
                            }
                        }))
                        .exceptionally(e -> {
                            Platform.runLater(() -> card.setLoading(false));
                            System.err.println("Failed to load thumbnail for page " + pageNum + ": " + e.getMessage());
                            return null;
                        });
                    
                    // Throttle to avoid overwhelming the thread pool
                    Thread.sleep(50);
                    
                    loadedPageCount = pageNum;
                    Platform.runLater(() -> updateLoadStatus());
                }
                
                return null;
            }
        };
        
        currentLoadTask.setOnSucceeded(e -> {
            isLoadingProperty.set(false);
            updateEmptyState(); // Update empty state after loading completes
            if (loadingProgressIndicator != null) {
                loadingProgressIndicator.setVisible(false);
                loadingProgressIndicator.setManaged(false);
            }
            if (loadMoreButton != null) loadMoreButton.setDisable(false);
            if (loadAllButton != null) loadAllButton.setDisable(false);
            
            // Hide load controls if all pages are loaded
            if (loadedPageCount >= totalPageCount) {
                if (loadControlsBox != null) {
                    loadControlsBox.setVisible(false);
                    loadControlsBox.setManaged(false);
                }
            }
        });
        
        currentLoadTask.setOnFailed(e -> {
            isLoadingProperty.set(false);
            updateEmptyState();
            if (loadingProgressIndicator != null) {
                loadingProgressIndicator.setVisible(false);
                loadingProgressIndicator.setManaged(false);
            }
            if (loadMoreButton != null) loadMoreButton.setDisable(false);
            if (loadAllButton != null) loadAllButton.setDisable(false);
            System.err.println("Load task failed: " + currentLoadTask.getException().getMessage());
        });
        
        currentLoadTask.setOnCancelled(e -> {
            isLoadingProperty.set(false);
            updateEmptyState();
            if (loadingProgressIndicator != null) {
                loadingProgressIndicator.setVisible(false);
                loadingProgressIndicator.setManaged(false);
            }
            if (loadMoreButton != null) loadMoreButton.setDisable(false);
            if (loadAllButton != null) loadAllButton.setDisable(false);
        });
        
        new Thread(currentLoadTask).start();
    }
    
    @FXML
    private void handleLoadMore() {
        int nextStart = loadedPageCount + 1;
        int nextEnd = Math.min(nextStart + LOAD_MORE_BATCH_SIZE - 1, totalPageCount);
        loadPageBatch(nextStart, nextEnd);
    }
    
    @FXML
    private void handleLoadAll() {
        int nextStart = loadedPageCount + 1;
        loadPageBatch(nextStart, totalPageCount);
    }
    
    private void handlePageDelete(PageThumbnailCard card) {
        // Remove from UI and list
        pageGridContainer.getChildren().remove(card);
        pageThumbnails.remove(card);
        loadedPageCount--;
        totalPageCount--;
        updateLoadStatus();
        updateDeselectButtonVisibility();
    }
    
    @FXML
    private void handleDeselectAll() {
        for (PageThumbnailCard card : pageThumbnails) {
            card.setSelected(false);
        }
        updateDeselectButtonVisibility();
        updateSelectedPagesCount();
    }
    
    private void updateDeselectButtonVisibility() {
        if (deselectAllButton != null) {
            boolean hasSelection = pageThumbnails.stream().anyMatch(PageThumbnailCard::isSelected);
            deselectAllButton.setVisible(hasSelection);
            deselectAllButton.setManaged(hasSelection);
        }
    }
    
    /**
     * Update the selected pages count property.
     * This is called whenever page selection changes.
     */
    private void updateSelectedPagesCount() {
        int count = (int) pageThumbnails.stream().filter(PageThumbnailCard::isSelected).count();
        selectedPagesCountProperty.set(count);
    }
    
    /**
     * Clear all page selections.
     * Used when user switches to text input mode in Extract Pages.
     */
    private void clearPageSelections() {
        for (PageThumbnailCard card : pageThumbnails) {
            card.setSelected(false);
        }
        updateDeselectButtonVisibility();
        updateSelectedPagesCount();
    }
    
    /**
     * Generate a unique file name by adding suffix (_1, _2, etc.) if file exists.
     * For example: document.pdf -> document_1.pdf -> document_2.pdf
     */
    private File getUniqueOutputFile(File outputFolder, String baseFileName) {
        File outputFile = new File(outputFolder, baseFileName);
        
        if (!outputFile.exists()) {
            return outputFile;
        }
        
        // Extract name and extension
        String nameWithoutExt = baseFileName;
        String extension = "";
        int lastDot = baseFileName.lastIndexOf('.');
        if (lastDot > 0) {
            nameWithoutExt = baseFileName.substring(0, lastDot);
            extension = baseFileName.substring(lastDot);
        }
        
        // Try with suffix _1, _2, _3, etc.
        int counter = 1;
        while (outputFile.exists()) {
            String uniqueName = String.format("%s_%d%s", nameWithoutExt, counter, extension);
            outputFile = new File(outputFolder, uniqueName);
            counter++;
            
            // Safety check to prevent infinite loop
            if (counter > 9999) {
                throw new RuntimeException("Could not generate unique filename after 9999 attempts");
            }
        }
        
        return outputFile;
    }
    
    /**
     * Setup click-to-select functionality for a page card.
     * Simple single-click toggles selection state.
     */
    private void setupDragSelection(PageThumbnailCard card) {
        // Simple click handler - toggle selection on click
        card.setOnMouseClicked(event -> {
            if (event.getButton().equals(javafx.scene.input.MouseButton.PRIMARY)) {
                // Toggle selection state
                card.setSelected(!card.isSelected());
                
                // If in Extract mode, clear the text field when user clicks on page preview
                if (extractPagesToggle.isSelected() && card.isSelected()) {
                    if (!extractPagesField.getText().isEmpty()) {
                        Platform.runLater(() -> extractPagesField.clear());
                    }
                }
                
                event.consume();
            }
        });
    }
    
    private void updateLoadStatus() {
        if (pageLoadStatusLabel != null) {
            ResourceBundle messages = LocaleManager.getBundle();
            String status = String.format(
                messages.getString("split.showingCount"),
                loadedPageCount,
                totalPageCount
            );
            pageLoadStatusLabel.setText(status);
        }
    }
}
