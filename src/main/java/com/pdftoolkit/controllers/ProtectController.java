package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.operations.PdfOperation;
import com.pdftoolkit.operations.stub.StubProtectOperation;
import com.pdftoolkit.utils.LocaleManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Protect PDF view.
 */
public class ProtectController {

    @FXML
    private VBox dropZone;

    @FXML
    private ListView<String> fileListView;

    @FXML
    private Button addFilesButton;

    @FXML
    private Button clearButton;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label strengthLabel;

    @FXML
    private CheckBox showPasswordCheckbox;

    @FXML
    private TextField outputFolderField;

    @FXML
    private Button browseOutputButton;

    @FXML
    private TextField outputFileNameField;

    @FXML
    private Label validationLabel;

    @FXML
    private Button backButton;

    @FXML
    private Button processButton;

    @FXML
    private StackPane progressOverlay;

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

    private final ObservableList<File> selectedFiles = FXCollections.observableArrayList();
    private final PdfOperation operation = new StubProtectOperation();
    private Task<File> currentTask;
    private File outputFolder;

    @FXML
    private void initialize() {
        setupDragAndDrop();
        setupPassword();
        setupButtons();
        setupValidation();
        
        outputFolderField.setText(System.getProperty("user.home") + File.separator + "Desktop");
        outputFolder = new File(outputFolderField.getText());
        outputFileNameField.setText("protected.pdf");
        
        progressOverlay.setVisible(false);
        completionPane.setVisible(false);
        
        // Setup locale listener for live language switching
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
        });
        
        // Initial text update
        updateTexts();
    }
    
    private void updateTexts() {
        // Update password strength if there's any password entered
        updatePasswordStrength(passwordField.getText());
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleDragDropped);
    }

    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();
        files.stream()
             .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
             .forEach(selectedFiles::add);
        updateFileList();
        event.setDropCompleted(true);
        event.consume();
    }

    private void setupPassword() {
        passwordField.textProperty().addListener((obs, old, newVal) -> {
            updatePasswordStrength(newVal);
        });
        
        showPasswordCheckbox.selectedProperty().addListener((obs, old, newVal) -> {
            // In a real app, you'd swap between PasswordField and TextField
            // For simplicity, we'll just note this is a placeholder
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            strengthLabel.setText("");
            strengthLabel.setStyle("");
            return;
        }
        
        int strength = calculateStrength(password);
        
        if (strength < 30) {
            strengthLabel.setText("Weak");
            strengthLabel.setStyle("-fx-text-fill: #e74c3c;");
        } else if (strength < 60) {
            strengthLabel.setText("Medium");
            strengthLabel.setStyle("-fx-text-fill: #f39c12;");
        } else {
            strengthLabel.setText("Strong");
            strengthLabel.setStyle("-fx-text-fill: #27ae60;");
        }
    }

    private int calculateStrength(String password) {
        int score = 0;
        
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 10;
        if (password.matches(".*[a-z].*")) score += 15;
        if (password.matches(".*[A-Z].*")) score += 15;
        if (password.matches(".*\\d.*")) score += 20;
        if (password.matches(".*[^a-zA-Z0-9].*")) score += 20;
        
        return Math.min(100, score);
    }

    private void setupButtons() {
        addFilesButton.setOnAction(e -> handleAddFiles());
        clearButton.setOnAction(e -> handleClear());
        browseOutputButton.setOnAction(e -> handleBrowseOutput());
        backButton.setOnAction(e -> AppNavigator.navigateToHome());
        processButton.setOnAction(e -> handleProcess());
        cancelButton.setOnAction(e -> handleCancel());
        openFolderButton.setOnAction(e -> handleOpenFolder());
        processAnotherButton.setOnAction(e -> handleProcessAnother());
    }

    private void setupValidation() {
        processButton.disableProperty().bind(
            javafx.beans.binding.Bindings.createBooleanBinding(
                () -> !isValid(),
                selectedFiles,
                passwordField.textProperty(),
                confirmPasswordField.textProperty(),
                outputFileNameField.textProperty()
            )
        );
    }

    private boolean isValid() {
        validationLabel.setVisible(false);
        
        Map<String, Object> params = new HashMap<>();
        params.put("password", passwordField.getText());
        params.put("confirmPassword", confirmPasswordField.getText());
        
        String error = operation.validate(selectedFiles, outputFolder, outputFileNameField.getText(), params);
        
        if (error != null) {
            validationLabel.setText(error);
            validationLabel.setVisible(true);
            return false;
        }
        return true;
    }

    private void handleAddFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select PDF Files");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        List<File> files = chooser.showOpenMultipleDialog(AppNavigator.getPrimaryStage());
        if (files != null) {
            selectedFiles.addAll(files);
            updateFileList();
        }
    }

    private void handleBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Folder");
        if (outputFolder != null && outputFolder.exists()) {
            chooser.setInitialDirectory(outputFolder);
        }
        
        File folder = chooser.showDialog(AppNavigator.getPrimaryStage());
        if (folder != null) {
            outputFolder = folder;
            outputFolderField.setText(folder.getAbsolutePath());
        }
    }

    private void handleProcess() {
        if (!isValid()) {
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("password", passwordField.getText());
        params.put("confirmPassword", confirmPasswordField.getText());

        currentTask = operation.execute(
            selectedFiles,
            outputFolder,
            outputFileNameField.getText(),
            params
        );

        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressLabel.textProperty().bind(currentTask.messageProperty());

        currentTask.setOnSucceeded(e -> {
            File result = currentTask.getValue();
            if (result != null) {
                showCompletion(result);
                AppState.getInstance().addRecentFile("Protect", result);
            }
        });

        currentTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Operation Failed");
            alert.setHeaderText("Failed to protect PDF");
            Throwable ex = currentTask.getException();
            alert.setContentText(ex != null ? ex.getMessage() : "Unknown error occurred");
            alert.showAndWait();
            progressOverlay.setVisible(false);
        });

        currentTask.setOnCancelled(e -> {
            progressOverlay.setVisible(false);
        });

        progressOverlay.setVisible(true);
        completionPane.setVisible(false);
        
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancel() {
        AppNavigator.navigateToHome();
    }
    
    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Berkas PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        List<File> files = fileChooser.showOpenMultipleDialog(AppNavigator.getPrimaryStage());
        if (files != null && !files.isEmpty()) {
            selectedFiles.addAll(files);
            updateFileList();
        }
    }
    
    @FXML
    private void handleClear() {
        selectedFiles.clear();
        updateFileList();
    }
    
    @FXML
    private void handleOutputBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Pilih Folder Output");
        if (outputFolder != null && outputFolder.exists()) {
            chooser.setInitialDirectory(outputFolder);
        }
        
        File folder = chooser.showDialog(AppNavigator.getPrimaryStage());
        if (folder != null) {
            outputFolder = folder;
            outputFolderField.setText(folder.getAbsolutePath());
        }
    }
    
    @FXML
    private void handleExecute() {
        if (selectedFiles.isEmpty()) {
            showAlert("Peringatan", "Pilih minimal 1 berkas PDF", Alert.AlertType.WARNING);
            return;
        }
        
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (password == null || password.isEmpty()) {
            showAlert("Peringatan", "Masukkan kata sandi", Alert.AlertType.WARNING);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showAlert("Peringatan", "Kata sandi tidak cocok", Alert.AlertType.WARNING);
            return;
        }
        
        if (outputFolder == null || !outputFolder.exists()) {
            showAlert("Peringatan", "Pilih folder output", Alert.AlertType.WARNING);
            return;
        }
        
        String filename = outputFileNameField.getText();
        if (filename == null || filename.trim().isEmpty()) {
            filename = "terkunci.pdf";
        }
        
        Map<String, Object> params = new HashMap<>();
        params.put("password", password);

        currentTask = operation.execute(selectedFiles, outputFolder, filename, params);

        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressLabel.textProperty().bind(currentTask.messageProperty());

        currentTask.setOnSucceeded(e -> {
            javafx.application.Platform.runLater(() -> {
                File result = currentTask.getValue();
                if (result != null) {
                    showCompletion(result);
                    AppState.getInstance().addRecentFile("Protect", result);
                }
            });
        });

        currentTask.setOnFailed(e -> {
            javafx.application.Platform.runLater(() -> {
                progressOverlay.setVisible(false);
                Throwable ex = currentTask.getException();
                showAlert("Gagal", 
                    "Gagal mengunci PDF: " + (ex != null ? ex.getMessage() : "Terjadi kesalahan"),
                    Alert.AlertType.ERROR);
            });
        });

        currentTask.setOnCancelled(e -> {
            javafx.application.Platform.runLater(() -> {
                progressOverlay.setVisible(false);
                showAlert("Dibatalkan", "Proses dibatalkan oleh pengguna", Alert.AlertType.INFORMATION);
            });
        });

        progressOverlay.setVisible(true);
        completionPane.setVisible(false);
        cancelButton.setVisible(true);
        
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

    private void showCompletion(File outputFile) {
        javafx.application.Platform.runLater(() -> {
            completionMessage.setText("Berhasil mengunci " + selectedFiles.size() + " berkas!");
            completionPane.setVisible(true);
            cancelButton.setVisible(false);
        });
    }

    @FXML
    private void handleOpenFolder() {
        try {
            java.awt.Desktop.getDesktop().open(outputFolder);
        } catch (Exception e) {
            showAlert("Error", "Tidak dapat membuka folder: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleProcessAnother() {
        selectedFiles.clear();
        updateFileList();
        passwordField.clear();
        confirmPasswordField.clear();
        strengthLabel.setText("");
        progressOverlay.setVisible(false);
        completionPane.setVisible(false);
        cancelButton.setVisible(true);
        outputFileNameField.setText("terkunci.pdf");
    }

    private void updateFileList() {
        fileListView.getItems().clear();
        selectedFiles.forEach(f -> fileListView.getItems().add(f.getName()));
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
