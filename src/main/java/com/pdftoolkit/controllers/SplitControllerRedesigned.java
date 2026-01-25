package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.services.PdfSplitService;
import com.pdftoolkit.services.PdfThumbnailService;
import com.pdftoolkit.ui.PageThumbnailCard;
import com.pdftoolkit.ui.RangeCard;
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
import javafx.scene.layout.FlowPane;
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
 */
public class SplitControllerRedesigned {

    // LEFT PANEL: Page preview elements
    @FXML private TilePane pageGridContainer;
    @FXML private StackPane pageEmptyStatePane;
    @FXML private Label selectedFileInfoLabel;
    @FXML private Button removePdfButton; // NEW: Remove PDF button
    @FXML private Button deselectAllButton;
    
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
    // NOTE: fileDropZone removed - no longer using right-side drag zone
    @FXML private Button selectFileButton;
    
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
    private final ObservableList<RangeCard> rangeCards = FXCollections.observableArrayList();
    private RangeCard selectedRangeCard;
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
        pageEmptyStatePane.visibleProperty().bind(noContentProperty);
        pageEmptyStatePane.managedProperty().bind(noContentProperty);
        pageEmptyStatePane.mouseTransparentProperty().bind(noContentProperty.not());
        
        // Bind page grid visibility to inverse of empty state
        pageGridContainer.visibleProperty().bind(noContentProperty.not());
        pageGridContainer.managedProperty().bind(noContentProperty.not());
        
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
                // Re-enable range editor if file is loaded
                if (selectedFile != null) {
                    rangeEditorSection.setDisable(selectedRangeCard == null);
                }
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
        
        // Setup spinners (will be configured when file is loaded)
        fromPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        toPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
        
        // Spinner listeners for validation and updating current range
        fromPageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateCurrentRange();
            if (selectedRangeCard != null && newVal != null) {
                selectedRangeCard.setRange(newVal, toPageSpinner.getValue());
            }
        });
        toPageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateCurrentRange();
            if (selectedRangeCard != null && newVal != null) {
                selectedRangeCard.setRange(fromPageSpinner.getValue(), newVal);
            }
        });
        
        // Extract pages field listener for validation
        extractPagesField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedFile != null && extractPagesToggle.isSelected()) {
                validateExtractPages();
            }
            updateUI();
        });
        
        // Range card selection listener
        rangeCards.addListener((javafx.collections.ListChangeListener<RangeCard>) c -> {
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
                // Extract mode: must have page selection or valid page spec
                String pageSpec = extractPagesField.getText();
                if (pageSpec != null && !pageSpec.trim().isEmpty()) {
                    try {
                        com.pdftoolkit.utils.PageRangeParser.parse(pageSpec.trim(), totalPages);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }
                // Or check if any pages are selected
                return pageThumbnails.stream().anyMatch(PageThumbnailCard::isSelected);
            } else {
                // Range mode: must have valid range
                int from = fromPageSpinner.getValue() != null ? fromPageSpinner.getValue() : 0;
                int to = toPageSpinner.getValue() != null ? toPageSpinner.getValue() : 0;
                return from > 0 && to > 0 && from <= to && to <= totalPages;
            }
        },
        // Observable dependencies
        outputFolderField.textProperty(),
        extractPagesField.textProperty(),
        fromPageSpinner.valueProperty(),
        toPageSpinner.valueProperty(),
        modeToggleGroup.selectedToggleProperty()
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
            String fileInfo = String.format(LocaleManager.getString("split.fileInfo"), 
                                           selectedFile.getName(), totalPages);
            selectedFileInfoLabel.setText(fileInfo);
        } else {
            selectedFileInfoLabel.setText(LocaleManager.getString("split.noFileSelected"));
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
        pageEmptyStatePane.setOnDragOver(this::handleLeftPanelDragOver);
        pageEmptyStatePane.setOnDragDropped(this::handleLeftPanelDragDropped);
        pageEmptyStatePane.setOnDragExited(event -> {
            pageEmptyStatePane.getStyleClass().remove("drag-over");
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
                if (!pageEmptyStatePane.getStyleClass().contains("drag-over")) {
                    pageEmptyStatePane.getStyleClass().add("drag-over");
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
                showAlert(LocaleManager.getString("split.dropInvalidFile"), Alert.AlertType.WARNING);
            }
        }
        
        event.setDropCompleted(success);
        pageEmptyStatePane.getStyleClass().remove("drag-over");
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
        
        File file = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());
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
            String fileInfo = String.format(LocaleManager.getString("split.fileInfo"), 
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
            
            // Enable sections
            modeSection.setDisable(false);
            rangeEditorSection.setDisable(false);
            outputSection.setDisable(false);
            extractPagesSection.setDisable(false);
            
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
    
    @FXML
    private void handleAddRange() {
        if (selectedFile == null) {
            showAlert(LocaleManager.getString("split.noFileSelected"), Alert.AlertType.WARNING);
            return;
        }
        
        int rangeNumber = rangeCards.size() + 1;
        RangeCard card = new RangeCard(rangeNumber);
        
        // Set default range values from spinners
        int from = fromPageSpinner.getValue();
        int to = toPageSpinner.getValue();
        card.setRange(from, to);
        
        // Provide source file for thumbnails
        if (selectedFile != null) {
            card.setSourceFile(selectedFile);
        }
        
        // Setup remove button
        card.getRemoveButton().setOnAction(e -> {
            confirmAndRemoveRange(card);
        });
        
        // Setup selection
        card.setOnMouseClicked(e -> {
            selectRangeCard(card);
        });
        
        rangeCards.add(card);
        selectRangeCard(card);
        updateUI();
    }
    
    private void selectRangeCard(RangeCard card) {
        // Deselect all
        rangeCards.forEach(c -> c.setSelected(false));
        
        // Select this one
        card.setSelected(true);
        selectedRangeCard = card;
        
        // Update spinners to show selected range
        fromPageSpinner.getValueFactory().setValue(card.getFromPage());
        toPageSpinner.getValueFactory().setValue(card.getToPage());
    }
    
    private void confirmAndRemoveRange(RangeCard card) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(LocaleManager.getString("split.confirmRemove"));
        confirmation.setHeaderText(null);
        confirmation.setContentText(LocaleManager.getString("split.confirmRemoveMessage"));
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            rangeCards.remove(card);
            if (selectedRangeCard == card) {
                selectedRangeCard = null;
            }
            updateUI();
        }
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
        boolean hasRanges = !rangeCards.isEmpty();
        boolean hasSelectedPages = pageThumbnails.stream().anyMatch(PageThumbnailCard::isSelected);
        boolean hasValidExtractSpec = false;
        
        // Check if Extract Pages mode has valid spec
        if (isExtractMode && hasFile) {
            String pageSpec = extractPagesField.getText();
            if (pageSpec != null && !pageSpec.trim().isEmpty()) {
                try {
                    com.pdftoolkit.utils.PageRangeParser.parse(pageSpec.trim(), totalPages);
                    hasValidExtractSpec = true;
                } catch (IllegalArgumentException e) {
                    hasValidExtractSpec = false;
                }
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
            // Extract Pages mode: validate page specification
            String pageSpec = extractPagesField.getText();
            if (pageSpec == null || pageSpec.trim().isEmpty()) {
                extractValidationLabel.setText(LocaleManager.getString("validation.emptyPageSpec"));
                extractValidationLabel.setVisible(true);
                extractValidationLabel.setManaged(true);
                return false;
            }
            // Try parsing to validate
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
            // Split by Range mode: validate ranges
            if (rangeCards.isEmpty()) return false;
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
            showAlert(LocaleManager.getString("validation.noRange"), Alert.AlertType.WARNING);
            return;
        }
        
        // Show progress overlay
        showProgressOverlay();
        
        File outputFolder = new File(outputFolderField.getText());
        lastOutputFolder = outputFolder;
        
        // Check which mode is selected
        boolean isExtractMode = extractPagesToggle.isSelected();
        
        if (isExtractMode) {
            handleExtractPages(outputFolder);
        } else {
            handleSplitByRanges(outputFolder);
        }
    }
    
    private void handleSplitByRanges(File outputFolder) {
        // Build page ranges from cards
        List<PdfSplitService.PageRange> pageRanges = rangeCards.stream()
            .map(card -> new PdfSplitService.PageRange(card.getFromPage(), card.getToPage()))
            .collect(Collectors.toList());
        
        // Create and execute real split task
        final String baseFileName = selectedFile.getName().replace(".pdf", "");
        
        currentTask = new Task<File>() {
            @Override
            protected File call() throws Exception {
                updateMessage("Analyzing PDF structure...");
                updateProgress(0, pageRanges.size() + 2);
                Thread.sleep(300);
                
                if (isCancelled()) {
                    return null;
                }
                
                updateMessage("Preparing to split into " + pageRanges.size() + " ranges...");
                updateProgress(1, pageRanges.size() + 2);
                Thread.sleep(200);
                
                // Perform actual split using PdfSplitService
                List<File> outputFiles = splitService.splitPdfByRanges(
                    selectedFile, pageRanges, outputFolder, baseFileName
                );
                
                // Update progress for each range
                for (int i = 0; i < outputFiles.size(); i++) {
                    if (isCancelled()) {
                        return null;
                    }
                    updateMessage(String.format("Created range file %d of %d", i + 1, outputFiles.size()));
                    updateProgress(i + 2, pageRanges.size() + 2);
                    Thread.sleep(200);
                }
                
                updateMessage("Split complete! Created " + outputFiles.size() + " files.");
                updateProgress(pageRanges.size() + 2, pageRanges.size() + 2);
                
                return outputFolder;
            }
        };
        
        executeTask();
    }
    
    private void handleExtractPages(File outputFolder) {
        String pageSpec = extractPagesField.getText().trim();
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
                
                // Parse page specification
                List<Integer> pageNumbers;
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
                
                updateMessage("Extracting " + pageNumbers.size() + " pages...");
                updateProgress(1, pageNumbers.size() + 2);
                Thread.sleep(200);
                
                if (isCancelled()) {
                    return null;
                }
                
                // Perform actual extraction using PdfSplitService
                List<File> outputFiles = splitService.extractPages(
                    selectedFile, pageNumbers, outputFolder, baseFileName
                );
                
                // Update progress for each page
                for (int i = 0; i < outputFiles.size(); i++) {
                    if (isCancelled()) {
                        return null;
                    }
                    updateMessage(String.format("Extracted page %d of %d", i + 1, outputFiles.size()));
                    updateProgress(i + 2, pageNumbers.size() + 2);
                    Thread.sleep(100);
                }
                
                updateMessage("Extraction complete! Created " + outputFiles.size() + " files.");
                updateProgress(pageNumbers.size() + 2, pageNumbers.size() + 2);
                
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
        rangeCards.clear();
        selectedRangeCard = null;
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
        rangeCards.clear();
        selectedRangeCard = null;
        
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
                    card.setOnSelectionChanged(() -> updateDeselectButtonVisibility());
                    
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
    }
    
    private void updateDeselectButtonVisibility() {
        if (deselectAllButton != null) {
            boolean hasSelection = pageThumbnails.stream().anyMatch(PageThumbnailCard::isSelected);
            deselectAllButton.setVisible(hasSelection);
            deselectAllButton.setManaged(hasSelection);
        }
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
