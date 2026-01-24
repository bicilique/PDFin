package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.operations.stub.StubMergeOperation;
import com.pdftoolkit.ui.Icons;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class MergeController {

    @FXML private ListView<File> fileListView;
    @FXML private Label fileCountLabel;
    @FXML private TextField outputFolderField;
    @FXML private TextField outputFilenameField;
    @FXML private Button processButton;
    
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

    private final ObservableList<File> files = FXCollections.observableArrayList();
    private final StubMergeOperation operation = new StubMergeOperation();
    private Task<File> currentTask;
    private File lastOutputFile;

    @FXML
    private void initialize() {
        fileListView.setItems(files);
        fileListView.setCellFactory(lv -> new FileListCell());
        
        // Update file count label and process button state
        files.addListener((javafx.collections.ListChangeListener.Change<? extends File> c) -> {
            if (fileCountLabel != null) {
                fileCountLabel.setText(files.size() + " berkas");
            }
            if (processButton != null) {
                processButton.setDisable(files.size() < 2);
            }
        });
        
        // Initialize count and button state
        if (fileCountLabel != null) {
            fileCountLabel.setText("0 berkas");
        }
        if (processButton != null) {
            processButton.setDisable(true);
        }
        
        // Setup locale listener for live language switching
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
        });
        
        // Initial text update
        updateTexts();
    }
    
    private void updateTexts() {
        // Update file count display
        if (fileCountLabel != null) {
            int count = files.size();
            fileCountLabel.setText(count + " " + LocaleManager.getString("merge.files"));
        }
    }

    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Berkas PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(
            fileListView.getScene().getWindow()
        );
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            files.addAll(selectedFiles);
        }
    }

    @FXML
    private void handleRemoveSelected() {
        File selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            files.remove(selected);
        }
    }

    @FXML
    private void handleMoveUp() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index > 0) {
            File file = files.remove(index);
            files.add(index - 1, file);
            fileListView.getSelectionModel().select(index - 1);
        }
    }

    @FXML
    private void handleMoveDown() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < files.size() - 1) {
            File file = files.remove(index);
            files.add(index + 1, file);
            fileListView.getSelectionModel().select(index + 1);
        }
    }

    @FXML
    private void handleOutputBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Pilih Folder Output");
        
        File selectedDir = chooser.showDialog(fileListView.getScene().getWindow());
        if (selectedDir != null && outputFolderField != null) {
            outputFolderField.setText(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    private void handleDragOver(DragEvent event) {
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

    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            List<File> pdfFiles = db.getFiles().stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());
            files.addAll(pdfFiles);
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void handleExecute() {
        // Validation
        if (files.size() < 2) {
            showAlert("Peringatan", "Pilih minimal 2 berkas PDF untuk digabung", Alert.AlertType.WARNING);
            return;
        }
        
        String outputPath = outputFolderField != null ? outputFolderField.getText() : null;
        if (outputPath == null || outputPath.isEmpty()) {
            showAlert("Peringatan", "Pilih folder output", Alert.AlertType.WARNING);
            return;
        }
        
        String filename = outputFilenameField != null ? outputFilenameField.getText() : "digabung.pdf";
        if (filename.isEmpty()) {
            filename = "digabung.pdf";
        }
        
        // Show progress overlay
        showProgressOverlay();
        
        // Create and execute task
        currentTask = operation.execute(
            files, 
            new File(outputPath), 
            filename, 
            null
        );
        
        // Bind progress
        if (progressBar != null) {
            progressBar.progressProperty().bind(currentTask.progressProperty());
        }
        if (progressMessage != null) {
            progressMessage.textProperty().bind(currentTask.messageProperty());
        }
        
        currentTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                File result = currentTask.getValue();
                lastOutputFile = result;
                showSuccess("Berkas PDF berhasil digabung!\n\n" + result.getName());
            });
        });
        
        currentTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                hideProgressOverlay();
                Throwable ex = currentTask.getException();
                showAlert("Gagal", 
                    "Gagal menggabungkan PDF: " + (ex != null ? ex.getMessage() : "Unknown error"),
                    Alert.AlertType.ERROR);
            });
        });
        
        currentTask.setOnCancelled(e -> {
            Platform.runLater(() -> {
                hideProgressOverlay();
                showAlert("Dibatalkan", "Proses dibatalkan oleh pengguna", Alert.AlertType.INFORMATION);
            });
        });
        
        new Thread(currentTask).start();
    }

    @FXML
    private void handleCancel() {
        AppNavigator.navigateToHome();
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
                Desktop.getDesktop().open(lastOutputFile.getParentFile());
            } catch (Exception ex) {
                showAlert("Error", "Tidak dapat membuka folder: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    @FXML
    private void handleProcessAnother() {
        hideProgressOverlay();
        files.clear();
        if (outputFilenameField != null) {
            outputFilenameField.setText("digabung.pdf");
        }
    }

    private void showProgressOverlay() {
        if (progressOverlay != null) {
            progressOverlay.setVisible(true);
            progressOverlay.setManaged(true);
        }
        if (successPane != null) {
            successPane.setVisible(false);
            successPane.setManaged(false);
        }
        if (cancelProcessButton != null) {
            cancelProcessButton.setVisible(true);
            cancelProcessButton.setManaged(true);
        }
    }
    
    private void hideProgressOverlay() {
        if (progressOverlay != null) {
            progressOverlay.setVisible(false);
            progressOverlay.setManaged(false);
        }
    }
    
    private void showSuccess(String message) {
        if (cancelProcessButton != null) {
            cancelProcessButton.setVisible(false);
            cancelProcessButton.setManaged(false);
        }
        if (successPane != null) {
            successPane.setVisible(true);
            successPane.setManaged(true);
        }
        if (successMessage != null) {
            successMessage.setText(message);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Custom List Cell with Icons
    private static class FileListCell extends ListCell<File> {
        @Override
        protected void updateItem(File file, boolean empty) {
            super.updateItem(file, empty);
            if (empty || file == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox box = new HBox(12);
                box.setAlignment(Pos.CENTER_LEFT);
                box.getStyleClass().add("file-item");
                
                // File icon
                var icon = Icons.create("file-pdf", 20);
                
                // File info
                VBox info = new VBox(2);
                Label name = new Label(file.getName());
                name.getStyleClass().add("file-name");
                name.setStyle("-fx-font-weight: 600;");
                
                Label size = new Label(formatFileSize(file.length()));
                size.getStyleClass().add("file-size");
                size.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
                
                info.getChildren().addAll(name, size);
                
                box.getChildren().addAll(icon, info);
                setGraphic(box);
                setText(null);
            }
        }
        
        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
