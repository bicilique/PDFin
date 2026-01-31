package com.pdftoolkit.controllers;

import com.pdftoolkit.models.SelectedPdfItem;
import com.pdftoolkit.services.PdfLockService;
import com.pdftoolkit.services.PdfThumbnailService;
import com.pdftoolkit.ui.Icons;
import com.pdftoolkit.utils.DefaultPaths;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Controller for Lock PDF feature with two-pane iLovePDF-like layout.
 * Left pane: file staging area with drag-drop and file cards
 * Right pane: settings panel with password fields and output options
 */
public class ProtectController {
    
    // LEFT PANE: Files Area
    @FXML private Button clearFilesButton;
    @FXML private StackPane dropZonePane;
    @FXML private StackPane dropZoneIconContainer;  // Icon container for folders icon
    @FXML private Button selectFilesButton;
    @FXML private ScrollPane filesScrollPane;
    @FXML private VBox filesContainer;
    @FXML private HBox addFilesToolbar;
    @FXML private Button addMoreFilesButton;
    @FXML private Label filesCountLabel;
    
    // RIGHT PANE: Settings
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmVisibleField;
    @FXML private CheckBox showPasswordCheckbox;
    @FXML private Label passwordStrengthLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Label confirmErrorLabel;
    @FXML private TextField outputFolderField;
    @FXML private Button browseFolderButton;
    @FXML private VBox singleFileNameSection;
    @FXML private TextField outputFilenameField;
    @FXML private VBox multiFileInfoSection;
    @FXML private Button lockPdfButton;
    @FXML private Button resetButton;
    @FXML private VBox statusInfoBox;
    @FXML private Label statusInfoLabel;
    
    // OVERLAYS (unified like Split/Compress)
    @FXML private StackPane progressOverlay;
    @FXML private Label progressTitle;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressMessage;
    @FXML private Button cancelProcessButton;
    @FXML private VBox successPane;
    @FXML private Label successMessage;
    @FXML private Button openFolderButton;
    @FXML private Button lockAnotherButton;
    @FXML private Button closeSuccessButton;
    
    // Data
    private final ObservableList<SelectedPdfItem> selectedFiles = FXCollections.observableArrayList();
    private final PdfLockService lockService = new PdfLockService();
    private final PdfThumbnailService thumbnailService = new PdfThumbnailService();
    private File outputFolder;
    private Task<Void> lockTask;
    
    // Preferences
    private static final String PREFS_LAST_OUTPUT_FOLDER = "lock.lastOutputFolder";
    private final Preferences prefs = Preferences.userNodeForPackage(ProtectController.class);
    
    /**
     * Initializes the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        if (passwordField == null || lockPdfButton == null || dropZonePane == null) {
            System.err.println("CRITICAL: FXML binding failed! Some components are null.");
            return;
        }
        
        // Setup drop zone icon
        setupDropZoneIcon();
        
        setupPasswordFields();
        setupOutputFolder();
        setupDragAndDrop();
        setupValidationBinding();
        updateFilesView();
        
        // Hide overlays initially
        if (progressOverlay != null) {
            progressOverlay.setVisible(false);
            progressOverlay.setManaged(false);
        }
        if (statusInfoBox != null) {
            statusInfoBox.setVisible(false);
            statusInfoBox.setManaged(false);
        }
    }
    
    /**
     * Setup drop zone icon with folders icon.
     */
    private void setupDropZoneIcon() {
        if (dropZoneIconContainer != null) {
            dropZoneIconContainer.getChildren().clear();
            dropZoneIconContainer.getChildren().add(Icons.create("folders", 60));
        }
    }
    
    /**
     * Sets up password fields with visibility toggle and validation.
     */
    private void setupPasswordFields() {
        // Bind visible/hidden fields bidirectionally
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        confirmVisibleField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
        
        // Toggle visibility
        showPasswordCheckbox.selectedProperty().addListener((obs, oldVal, showPassword) -> {
            passwordField.setVisible(!showPassword);
            passwordField.setManaged(!showPassword);
            passwordVisibleField.setVisible(showPassword);
            passwordVisibleField.setManaged(showPassword);
            
            confirmPasswordField.setVisible(!showPassword);
            confirmPasswordField.setManaged(!showPassword);
            confirmVisibleField.setVisible(showPassword);
            confirmVisibleField.setManaged(showPassword);
        });
        
        // Validation listeners
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePassword();
            updatePasswordStrength(newVal);
        });
        
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateConfirmPassword();
        });
        
        // Initially hide error labels
        passwordErrorLabel.setVisible(false);
        passwordErrorLabel.setManaged(false);
        confirmErrorLabel.setVisible(false);
        confirmErrorLabel.setManaged(false);
    }
    
    /**
     * Sets up output folder with default and persistence.
     */
    private void setupOutputFolder() {
        // Try to load last used folder
        String lastFolder = prefs.get(PREFS_LAST_OUTPUT_FOLDER, null);
        if (lastFolder != null) {
            File folder = new File(lastFolder);
            if (folder.exists() && folder.isDirectory()) {
                outputFolder = folder;
            }
        }
        
        // Fallback to app default
        if (outputFolder == null) {
            outputFolder = DefaultPaths.getAppOutputDir();
        }
        
        outputFolderField.setText(outputFolder.getAbsolutePath());
    }
    
    /**
     * Sets up drag-and-drop for the files area.
     */
    private void setupDragAndDrop() {
        // Drop zone
        dropZonePane.setOnDragOver(this::handleDragOver);
        dropZonePane.setOnDragDropped(this::handleDragDropped);
        
        // Files container (when files exist)
        filesScrollPane.setOnDragOver(this::handleDragOver);
        filesScrollPane.setOnDragDropped(this::handleDragDropped);
    }
    
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }
    
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        
        if (db.hasFiles()) {
            List<File> pdfFiles = db.getFiles().stream()
                    .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                    .collect(Collectors.toList());
            
            if (!pdfFiles.isEmpty()) {
                addFiles(pdfFiles);
                success = true;
            }
        }
        
        event.setDropCompleted(success);
        event.consume();
    }
    
    /**
     * Sets up validation binding for Lock PDF button.
     */
    private void setupValidationBinding() {
        BooleanBinding validBinding = Bindings.createBooleanBinding(
            () -> isFormValid(),
            selectedFiles,
            passwordField.textProperty(),
            confirmPasswordField.textProperty(),
            outputFolderField.textProperty()
        );
        
        lockPdfButton.disableProperty().bind(validBinding.not());
    }
    
    /**
     * Checks if form is valid for processing.
     */
    private boolean isFormValid() {
        // Must have files
        if (selectedFiles.isEmpty()) {
            return false;
        }
        
        // Password must be at least 8 characters
        String password = passwordField.getText();
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Passwords must match
        String confirm = confirmPasswordField.getText();
        if (!password.equals(confirm)) {
            return false;
        }
        
        // Output folder must be valid
        if (outputFolder == null || !outputFolder.exists()) {
            return false;
        }
        
        // For single file, output filename must be valid
        if (selectedFiles.size() == 1 && outputFilenameField != null) {
            String filename = outputFilenameField.getText();
            if (filename == null || filename.trim().isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates password field.
     */
    private void validatePassword() {
        String password = passwordField.getText();
        
        if (password.isEmpty()) {
            passwordErrorLabel.setVisible(false);
            passwordErrorLabel.setManaged(false);
            return;
        }
        
        if (password.length() < 8) {
            passwordErrorLabel.setText(LocaleManager.getString("lock.validation.passwordTooShort"));
            passwordErrorLabel.setVisible(true);
            passwordErrorLabel.setManaged(true);
        } else {
            passwordErrorLabel.setVisible(false);
            passwordErrorLabel.setManaged(false);
        }
    }
    
    /**
     * Validates confirm password field.
     */
    private void validateConfirmPassword() {
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        
        if (confirm.isEmpty()) {
            confirmErrorLabel.setVisible(false);
            confirmErrorLabel.setManaged(false);
            return;
        }
        
        if (!password.equals(confirm)) {
            confirmErrorLabel.setText(LocaleManager.getString("lock.validation.passwordMismatch"));
            confirmErrorLabel.setVisible(true);
            confirmErrorLabel.setManaged(true);
        } else {
            confirmErrorLabel.setVisible(false);
            confirmErrorLabel.setManaged(false);
        }
    }
    
    /**
     * Updates password strength indicator.
     */
    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            passwordStrengthLabel.setText("");
            passwordStrengthLabel.getStyleClass().clear();
            passwordStrengthLabel.getStyleClass().add("password-strength-badge");
            return;
        }
        
        int strength = calculateStrength(password);
        
        passwordStrengthLabel.getStyleClass().clear();
        passwordStrengthLabel.getStyleClass().add("password-strength-badge");
        
        if (strength < 30) {
            passwordStrengthLabel.setText(LocaleManager.getString("lock.strength.weak"));
            passwordStrengthLabel.getStyleClass().add("strength-weak");
        } else if (strength < 60) {
            passwordStrengthLabel.setText(LocaleManager.getString("lock.strength.ok"));
            passwordStrengthLabel.getStyleClass().add("strength-ok");
        } else {
            passwordStrengthLabel.setText(LocaleManager.getString("lock.strength.strong"));
            passwordStrengthLabel.getStyleClass().add("strength-strong");
        }
    }
    
    private int calculateStrength(String password) {
        int strength = 0;
        strength += Math.min(password.length() * 4, 40);
        if (password.matches(".*[a-z].*")) strength += 10;
        if (password.matches(".*[A-Z].*")) strength += 10;
        if (password.matches(".*\\d.*")) strength += 20;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) strength += 20;
        return Math.min(strength, 100);
    }
    
    /**
     * Opens file chooser to select PDF files.
     */
    @FXML
    private void selectFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(LocaleManager.getString("lock.selectFiles"));
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        List<File> files = chooser.showOpenMultipleDialog(selectFilesButton.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            addFiles(files);
        }
    }
    
    /**
     * Adds files to the selection list.
     */
    private void addFiles(List<File> files) {
        for (File file : files) {
            // Skip if already added
            boolean exists = selectedFiles.stream()
                    .anyMatch(item -> item.getSourceFile().equals(file));
            
            if (!exists && lockService.isValidPdf(file)) {
                SelectedPdfItem item = new SelectedPdfItem(file);
                selectedFiles.add(item);
                
                // Load page count async
                loadPageCountAsync(item);
                
                // Load thumbnail async
                loadThumbnailAsync(item);
            }
        }
        
        updateFilesView();
        updateFileNameSection();
        
        // Set output folder to first file's directory if not set
        if (outputFolder == null && !selectedFiles.isEmpty()) {
            File firstDir = selectedFiles.get(0).getSourceFile().getParentFile();
            if (firstDir != null && firstDir.exists()) {
                outputFolder = firstDir;
                outputFolderField.setText(outputFolder.getAbsolutePath());
            }
        }
    }
    
    /**
     * Loads page count for an item asynchronously.
     */
    private void loadPageCountAsync(SelectedPdfItem item) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return lockService.getPageCount(item.getSourceFile());
            }
        };
        
        task.setOnSucceeded(e -> {
            int count = task.getValue();
            if (count > 0) {
                item.setPageCount(count);
                // Refresh file card if needed
                updateFilesView();
            }
        });
        
        new Thread(task).start();
    }
    
    /**
     * Loads thumbnail for an item asynchronously.
     */
    private void loadThumbnailAsync(SelectedPdfItem item) {
        thumbnailService.generateThumbnailAsync(item.getSourceFile(), 0, 1.0)
            .thenAccept(result -> {
                Platform.runLater(() -> {
                    if (result.image() != null) {
                        item.setThumbnail(result.image());
                    }
                });
            })
            .exceptionally(throwable -> {
                // Log error but don't show to user - thumbnail is optional
                System.err.println("Failed to load thumbnail for " + item.getFileName() + ": " + throwable.getMessage());
                return null;
            });
    }
    
    /**
     * Updates the files view (drop zone vs file cards).
     */
    private void updateFilesView() {
        boolean hasFiles = !selectedFiles.isEmpty();
        
        // Toggle drop zone vs files list
        dropZonePane.setVisible(!hasFiles);
        dropZonePane.setManaged(!hasFiles);
        filesScrollPane.setVisible(hasFiles);
        filesScrollPane.setManaged(hasFiles);
        addFilesToolbar.setVisible(hasFiles);
        addFilesToolbar.setManaged(hasFiles);
        clearFilesButton.setVisible(hasFiles);
        clearFilesButton.setManaged(hasFiles);
        
        if (hasFiles) {
            // Update file count label
            int count = selectedFiles.size();
            String label = String.format(
                LocaleManager.getString("lock.filesCount"),
                count,
                count == 1 ? LocaleManager.getString("common.file") : LocaleManager.getString("common.files")
            );
            filesCountLabel.setText(label);
            
            // Rebuild file cards
            filesContainer.getChildren().clear();
            for (SelectedPdfItem item : selectedFiles) {
                filesContainer.getChildren().add(createFileCard(item));
            }
        }
    }
    
    /**
     * Creates a file card UI component.
     */
    private VBox createFileCard(SelectedPdfItem item) {
        VBox card = new VBox(8);
        card.getStyleClass().add("file-card");
        card.setPadding(new Insets(12));
        
        // Header: thumbnail + icon + name + remove button
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Thumbnail ImageView
        ImageView thumbnailView = new ImageView();
        thumbnailView.setFitWidth(48);
        thumbnailView.setFitHeight(48);
        thumbnailView.setPreserveRatio(true);
        thumbnailView.getStyleClass().add("file-card-thumbnail");
        
        // Bind thumbnail to item's thumbnail property
        thumbnailView.imageProperty().bind(item.thumbnailProperty());
        
        // Fallback icon when no thumbnail
        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 32px;");
        icon.visibleProperty().bind(item.thumbnailProperty().isNull());
        icon.managedProperty().bind(icon.visibleProperty());
        
        // Container for thumbnail/icon
        StackPane iconContainer = new StackPane(thumbnailView, icon);
        iconContainer.setMinSize(48, 48);
        iconContainer.setMaxSize(48, 48);
        
        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        
        Label nameLabel = new Label(item.getFileName());
        nameLabel.getStyleClass().add("file-card-name");
        nameLabel.setWrapText(true);
        
        Label sizeLabel = new Label();
        sizeLabel.getStyleClass().add("file-card-meta");
        
        // Build meta text: size • pages
        String meta = item.getFormattedSize();
        if (item.getPageCount() > 0) {
            meta += " • " + item.getPageCount() + " " + 
                    (item.getPageCount() == 1 ? 
                        LocaleManager.getString("common.page") : 
                        LocaleManager.getString("common.pages"));
        }
        sizeLabel.setText(meta);
        
        // Update meta when page count loads
        item.pageCountProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() > 0) {
                String updatedMeta = item.getFormattedSize() + " • " + newVal.intValue() + " " +
                        (newVal.intValue() == 1 ? 
                            LocaleManager.getString("common.page") : 
                            LocaleManager.getString("common.pages"));
                sizeLabel.setText(updatedMeta);
            }
        });
        
        nameBox.getChildren().addAll(nameLabel, sizeLabel);
        
        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("file-card-remove");
        removeBtn.setOnAction(e -> {
            selectedFiles.remove(item);
            updateFilesView();
            updateFileNameSection();
        });
        
        header.getChildren().addAll(iconContainer, nameBox, removeBtn);
        
        // Status indicator (shown during/after processing)
        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("file-card-status");
        statusLabel.visibleProperty().bind(
            item.statusProperty().isNotEqualTo(SelectedPdfItem.Status.READY)
        );
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        
        item.statusProperty().addListener((obs, oldVal, newVal) -> {
            String statusText = LocaleManager.getString(newVal.getResourceKey());
            statusLabel.setText(statusText);
            
            // Add style class based on status
            statusLabel.getStyleClass().removeIf(s -> s.startsWith("status-"));
            statusLabel.getStyleClass().add("status-" + newVal.name().toLowerCase());
        });
        
        // Error message (if failed)
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("file-card-error");
        errorLabel.setWrapText(true);
        errorLabel.visibleProperty().bind(
            item.statusProperty().isEqualTo(SelectedPdfItem.Status.FAILED)
        );
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        errorLabel.textProperty().bind(item.errorMessageProperty());
        
        card.getChildren().addAll(header, statusLabel, errorLabel);
        
        return card;
    }
    
    /**
     * Updates single file name section visibility.
     */
    private void updateFileNameSection() {
        boolean isSingleFile = selectedFiles.size() == 1;
        
        singleFileNameSection.setVisible(isSingleFile);
        singleFileNameSection.setManaged(isSingleFile);
        multiFileInfoSection.setVisible(!isSingleFile && !selectedFiles.isEmpty());
        multiFileInfoSection.setManaged(!isSingleFile && !selectedFiles.isEmpty());
        
        if (isSingleFile) {
            // Set default output filename
            String outputName = selectedFiles.get(0).getOutputName();
            outputFilenameField.setText(outputName);
        }
    }
    
    /**
     * Clears all selected files.
     */
    @FXML
    private void clearFiles() {
        selectedFiles.clear();
        updateFilesView();
        updateFileNameSection();
    }
    
    /**
     * Opens directory chooser for output folder.
     */
    @FXML
    private void browseOutputFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(LocaleManager.getString("lock.output.browse"));
        
        if (outputFolder != null && outputFolder.exists()) {
            chooser.setInitialDirectory(outputFolder);
        }
        
        File selected = chooser.showDialog(browseFolderButton.getScene().getWindow());
        if (selected != null) {
            outputFolder = selected;
            outputFolderField.setText(outputFolder.getAbsolutePath());
            prefs.put(PREFS_LAST_OUTPUT_FOLDER, outputFolder.getAbsolutePath());
        }
    }
    
    /**
     * Resets the form to initial state.
     */
    @FXML
    private void resetForm(ActionEvent event) {
        selectedFiles.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        showPasswordCheckbox.setSelected(false);
        passwordStrengthLabel.setText("");
        passwordErrorLabel.setVisible(false);
        passwordErrorLabel.setManaged(false);
        confirmErrorLabel.setVisible(false);
        confirmErrorLabel.setManaged(false);
        
        if (outputFilenameField != null) {
            outputFilenameField.clear();
        }
        
        updateFilesView();
        updateFileNameSection();
        
        if (statusInfoBox != null) {
            statusInfoBox.setVisible(false);
            statusInfoBox.setManaged(false);
        }
    }
    
    /**
     * Starts the lock PDF process.
     */
    @FXML
    private void lockPdf(ActionEvent event) {
        if (!isFormValid()) {
            return;
        }
        
        String password = passwordField.getText();
        
        // For single file, update output name from text field
        if (selectedFiles.size() == 1 && outputFilenameField != null) {
            String customName = outputFilenameField.getText();
            if (customName != null && !customName.trim().isEmpty()) {
                selectedFiles.get(0).setOutputName(customName);
            }
        }
        
        // Show progress overlay
        showProgressOverlay();
        
        // Create and start lock task
        lockTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int total = selectedFiles.size();
                int successCount = 0;
                int failureCount = 0;
                
                for (int i = 0; i < selectedFiles.size(); i++) {
                    SelectedPdfItem item = selectedFiles.get(i);
                    
                    // Update progress
                    int current = i + 1;
                    updateProgress(current, total);
                    updateMessage(String.format(
                        LocaleManager.getString("lock.progress.file"),
                        current, total
                    ));
                    
                    // Update item status
                    Platform.runLater(() -> item.setStatus(SelectedPdfItem.Status.PROCESSING));
                    
                    // Lock the PDF
                    File outputFile = new File(outputFolder, item.getOutputName());
                    PdfLockService.LockResult result = 
                            lockService.lockPdf(item.getSourceFile(), outputFile, password);
                    
                    // Update item based on result
                    if (result.isSuccess()) {
                        Platform.runLater(() -> item.setStatus(SelectedPdfItem.Status.DONE));
                        successCount++;
                    } else {
                        final String error = result.getErrorMessage();
                        Platform.runLater(() -> {
                            item.setStatus(SelectedPdfItem.Status.FAILED);
                            item.setErrorMessage(error);
                        });
                        failureCount++;
                    }
                }
                
                // Show result
                final int finalSuccess = successCount;
                final int finalFailure = failureCount;
                
                Platform.runLater(() -> {
                    showSuccess(finalSuccess, finalFailure);
                });
                
                return null;
            }
        };
        
        // Bind progress
        progressBar.progressProperty().bind(lockTask.progressProperty());
        progressMessage.textProperty().bind(lockTask.messageProperty());
        
        // Start task
        new Thread(lockTask).start();
    }
    
    /**
     * Shows progress overlay.
     */
    private void showProgressOverlay() {
        progressOverlay.setVisible(true);
        progressOverlay.setManaged(true);
        successPane.setVisible(false);
        successPane.setManaged(false);
        cancelProcessButton.setVisible(true);
        cancelProcessButton.setManaged(true);
        
        // Don't manually disable buttons if they have bindings
        // lockPdfButton has a binding, so don't call setDisable on it
        // Only disable buttons without bindings
        selectFilesButton.setDisable(true);
        resetButton.setDisable(true);
    }
    
    /**
     * Hides progress overlay.
     */
    private void hideProgressOverlay() {
        // Unbind properties before hiding to prevent binding errors
        progressBar.progressProperty().unbind();
        progressMessage.textProperty().unbind();
        
        progressOverlay.setVisible(false);
        progressOverlay.setManaged(false);
        
        // Re-enable inputs (only those without bindings)
        selectFilesButton.setDisable(false);
        resetButton.setDisable(false);
        // lockPdfButton re-enabled by its binding automatically
    }
    
    /**
     * Shows success state with results.
     */
    private void showSuccess(int successCount, int failureCount) {
        String message;
        if (failureCount == 0) {
            message = String.format(
                LocaleManager.getString("lock.success.all"),
                successCount
            );
        } else if (successCount == 0) {
            message = String.format(
                LocaleManager.getString("lock.error.all"),
                failureCount
            );
        } else {
            message = String.format(
                LocaleManager.getString("lock.success.partial"),
                successCount, failureCount
            );
        }
        
        cancelProcessButton.setVisible(false);
        cancelProcessButton.setManaged(false);
        successPane.setVisible(true);
        successPane.setManaged(true);
        successMessage.setText(message);
    }
    
    /**
     * Opens output folder in system file manager.
     */
    @FXML
    private void handleOpenFolder() {
        if (outputFolder != null && outputFolder.exists()) {
            try {
                Desktop.getDesktop().open(outputFolder);
            } catch (IOException e) {
                System.err.println("Failed to open folder: " + e.getMessage());
            }
        }
    }
    
    /**
     * Hides success overlay and resets for another operation.
     */
    @FXML
    private void handleLockAnother() {
        hideProgressOverlay();
        resetForm(null);
    }
    
    /**
     * Handle cancel process button.
     */
    @FXML
    private void handleCancelProcess() {
        if (lockTask != null && lockTask.isRunning()) {
            lockTask.cancel();
        }
    }
    
    /**
     * Handle close success button.
     */
    @FXML
    private void handleCloseSuccess() {
        hideProgressOverlay();
    }
}
