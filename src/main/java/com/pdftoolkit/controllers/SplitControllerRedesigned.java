package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.services.PdfSplitService;
import com.pdftoolkit.services.PdfThumbnailService;
import com.pdftoolkit.ui.PageThumbnailCard;
import com.pdftoolkit.ui.RangeCard;
import com.pdftoolkit.utils.AppPaths;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
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
    @FXML private FlowPane pageGridContainer;
    @FXML private StackPane pageEmptyStatePane;
    @FXML private Label selectedFileInfoLabel;
    
    // RIGHT PANEL: Configuration elements
    @FXML private VBox fileDropZone;
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
    private File lastOutputFolder;
    private ToggleGroup modeToggleGroup;

    @FXML
    private void initialize() {
        // Set default output folder using AppPaths
        outputFolderField.setText(AppPaths.getDefaultOutputPath());
        
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
        
        // Setup locale change listener
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> updateUI());
        
        // Initial UI update
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
        fileDropZone.setOnDragOver(this::handleDragOver);
        fileDropZone.setOnDragDropped(this::handleDragDropped);
    }
    
    @FXML
    private void handleDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            List<File> pdfFiles = db.getFiles().stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .limit(1) // Only accept one file for split
                .collect(Collectors.toList());
            if (!pdfFiles.isEmpty()) {
                event.acceptTransferModes(TransferMode.COPY);
                fileDropZone.getStyleClass().add("drag-over");
            }
        }
        event.consume();
    }
    
    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        
        if (db.hasFiles()) {
            Optional<File> pdfFile = db.getFiles().stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .findFirst();
            
            if (pdfFile.isPresent()) {
                setSelectedFile(pdfFile.get());
                success = true;
            }
        }
        
        event.setDropCompleted(success);
        fileDropZone.getStyleClass().remove("drag-over");
        event.consume();
    }
    
    @FXML
    private void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(LocaleManager.getString("split.selectFile"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showOpenDialog(fileDropZone.getScene().getWindow());
        if (file != null) {
            setSelectedFile(file);
        }
    }
    
    private void setSelectedFile(File file) {
        selectedFile = file;
        
        // Clear existing thumbnails
        pageThumbnails.clear();
        pageGridContainer.getChildren().clear();
        
        // Load actual page count using thumbnail service
        Task<Integer> loadPageCountTask = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return thumbnailService.getPageCount(file);
            }
        };
        
        loadPageCountTask.setOnSucceeded(e -> {
            totalPages = loadPageCountTask.getValue();
            
            // Update UI
            String fileInfo = String.format(LocaleManager.getString("split.fileInfo"), 
                                           file.getName(), totalPages);
            selectedFileInfoLabel.setText(fileInfo);
            
            // Hide empty state
            pageEmptyStatePane.setVisible(false);
            pageEmptyStatePane.setManaged(false);
            
            // Configure spinners with actual page range
            fromPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, totalPages, 1));
            toPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, totalPages, totalPages));
            
            // Enable sections
            modeSection.setDisable(false);
            rangeEditorSection.setDisable(false);
            outputSection.setDisable(false);
            extractPagesSection.setDisable(false);
            
            // Load all page thumbnails
            loadAllPageThumbnails();
            
            updateUI();
        });
        
        loadPageCountTask.setOnFailed(e -> {
            showAlert("Failed to read PDF: " + file.getName(), Alert.AlertType.ERROR);
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
            card.setLoading(); // Show loading state initially
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
        
        // Show/hide empty state for page grid
        boolean hasPages = !pageThumbnails.isEmpty();
        pageEmptyStatePane.setVisible(!hasPages);
        pageEmptyStatePane.setManaged(!hasPages);
        
        // Enable/disable split button based on mode and selection
        boolean canSplit = hasFile && (
            (isExtractMode && hasValidExtractSpec) || 
            (!isExtractMode && hasSelectedPages)
        );
        splitButton.setDisable(!canSplit);
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
        rangeCards.clear();
        selectedRangeCard = null;
        selectedFileInfoLabel.setText(LocaleManager.getString("split.noFileSelected"));
        
        // Clear page thumbnails
        pageThumbnails.clear();
        pageGridContainer.getChildren().clear();
        pageEmptyStatePane.setVisible(true);
        pageEmptyStatePane.setManaged(true);
        
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
}
