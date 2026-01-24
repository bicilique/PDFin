package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.operations.PdfOperation;
import com.pdftoolkit.operations.stub.StubSplitOperation;
import com.pdftoolkit.ui.Icons;
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
 * Controller for Split PDF view.
 */
public class SplitController {

    @FXML
    private VBox dropZone;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private Button addFileButton;

    @FXML
    private ToggleButton extractPagesToggle;

    @FXML
    private ToggleButton splitByRangesToggle;

    @FXML
    private ToggleButton splitEveryNToggle;

    @FXML
    private TextField rangeInputField;

    @FXML
    private Label rangeHintLabel;

    @FXML
    private TextField outputFolderField;

    @FXML
    private Button browseOutputButton;

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

    private File selectedFile;
    private File outputFolder;
    private final PdfOperation operation = new StubSplitOperation();
    private Task<File> currentTask;
    private ToggleGroup splitModeGroup;

    @FXML
    private void initialize() {
        // Add icons to buttons
        addFileButton.setGraphic(Icons.create("folder-open", 16));
        browseOutputButton.setGraphic(Icons.create("folder", 16));
        backButton.setGraphic(Icons.create("home", 16));
        processButton.setGraphic(Icons.create("play", 16));
        
        setupDragAndDrop();
        setupSplitModes();
        setupButtons();
        setupValidation();
        
        outputFolderField.setText(System.getProperty("user.home") + File.separator + "Desktop");
        outputFolder = new File(outputFolderField.getText());
        
        progressOverlay.setVisible(false);
        completionPane.setVisible(false);
        
        selectedFileLabel.setText("Belum ada berkas dipilih");
        
        // Setup locale listener for live language switching
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
        });
        
        // Initial text update
        updateTexts();
    }
    
    private void updateTexts() {
        // Update selected file label if no file selected
        if (selectedFile == null && selectedFileLabel != null) {
            selectedFileLabel.setText(LocaleManager.getString("split.nofile"));
        }
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
             .findFirst()
             .ifPresent(this::setSelectedFile);
        event.setDropCompleted(true);
        event.consume();
    }

    private void setupSplitModes() {
        splitModeGroup = new ToggleGroup();
        extractPagesToggle.setToggleGroup(splitModeGroup);
        splitByRangesToggle.setToggleGroup(splitModeGroup);
        splitEveryNToggle.setToggleGroup(splitModeGroup);
        
        extractPagesToggle.setSelected(true);
        
        splitModeGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            updateRangeHint();
        });
        
        updateRangeHint();
    }

    private void updateRangeHint() {
        if (extractPagesToggle.isSelected()) {
            rangeHintLabel.setText("Contoh: 1-3,5,8");
        } else if (splitByRangesToggle.isSelected()) {
            rangeHintLabel.setText("Contoh: 1-4 | 5-9");
        } else if (splitEveryNToggle.isSelected()) {
            rangeHintLabel.setText("Contoh: 3 (pisah setiap 3 halaman)");
        }
    }

    private void setupButtons() {
        addFileButton.setOnAction(e -> handleAddFile());
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
                rangeInputField.textProperty()
            )
        );
    }

    private boolean isValid() {
        validationLabel.setVisible(false);
        
        if (selectedFile == null) {
            validationLabel.setText("Please select a PDF file");
            validationLabel.setVisible(true);
            return false;
        }
        
        Map<String, Object> params = buildParameters();
        ObservableList<File> files = FXCollections.observableArrayList(selectedFile);
        String error = operation.validate(files, outputFolder, "split", params);
        
        if (error != null) {
            validationLabel.setText(error);
            validationLabel.setVisible(true);
            return false;
        }
        return true;
    }

    private void handleAddFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select PDF File");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = chooser.showOpenDialog(AppNavigator.getPrimaryStage());
        if (file != null) {
            setSelectedFile(file);
        }
    }

    private void setSelectedFile(File file) {
        selectedFile = file;
        selectedFileLabel.setText(file.getName());
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

        Map<String, Object> params = buildParameters();
        ObservableList<File> files = FXCollections.observableArrayList(selectedFile);

        currentTask = operation.execute(files, outputFolder, "split", params);

        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressLabel.textProperty().bind(currentTask.messageProperty());

        currentTask.setOnSucceeded(e -> {
            File result = currentTask.getValue();
            if (result != null) {
                showCompletion();
                AppState.getInstance().addRecentFile("Split", result);
            }
        });

        currentTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Operation Failed");
            alert.setHeaderText("Failed to split PDF");
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

    private Map<String, Object> buildParameters() {
        Map<String, Object> params = new HashMap<>();
        
        if (extractPagesToggle.isSelected()) {
            params.put("splitMode", "extractPages");
        } else if (splitByRangesToggle.isSelected()) {
            params.put("splitMode", "splitByRanges");
        } else {
            params.put("splitMode", "splitEveryN");
        }
        
        params.put("rangeInput", rangeInputField.getText());
        return params;
    }

    private void handleCancel() {
        AppNavigator.navigateToHome();
    }
    
    @FXML
    private void handleBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih Berkas PDF");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = chooser.showOpenDialog(AppNavigator.getPrimaryStage());
        if (file != null) {
            setSelectedFile(file);
        }
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
        if (!isValid()) {
            return;
        }

        Map<String, Object> params = buildParameters();
        ObservableList<File> files = FXCollections.observableArrayList(selectedFile);

        currentTask = operation.execute(files, outputFolder, "split", params);

        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressLabel.textProperty().bind(currentTask.messageProperty());

        currentTask.setOnSucceeded(e -> {
            File result = currentTask.getValue();
            if (result != null) {
                showCompletion();
                AppState.getInstance().addRecentFile("Split", result);
            }
        });

        currentTask.setOnFailed(e -> {
            javafx.application.Platform.runLater(() -> {
                progressOverlay.setVisible(false);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Gagal");
                alert.setHeaderText("Gagal memisahkan PDF");
                Throwable ex = currentTask.getException();
                alert.setContentText(ex != null ? ex.getMessage() : "Terjadi kesalahan");
                alert.showAndWait();
            });
        });

        currentTask.setOnCancelled(e -> {
            javafx.application.Platform.runLater(() -> {
                progressOverlay.setVisible(false);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Dibatalkan");
                alert.setContentText("Proses dibatalkan oleh pengguna");
                alert.showAndWait();
            });
        });

        progressOverlay.setVisible(true);
        completionPane.setVisible(false);
        
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

    private void showCompletion() {
        javafx.application.Platform.runLater(() -> {
            completionMessage.setText("Berkas berhasil dipisah!");
            completionPane.setVisible(true);
            cancelButton.setVisible(false);
        });
    }

    @FXML
    private void handleOpenFolder() {
        try {
            java.awt.Desktop.getDesktop().open(outputFolder);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Tidak dapat membuka folder: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleProcessAnother() {
        selectedFile = null;
        selectedFileLabel.setText("Belum ada berkas dipilih");
        rangeInputField.clear();
        progressOverlay.setVisible(false);
        completionPane.setVisible(false);
        cancelButton.setVisible(true);
    }
}
