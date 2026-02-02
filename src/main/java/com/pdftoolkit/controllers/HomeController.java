package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.ui.Icons;
import com.pdftoolkit.utils.LocaleManager;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the home/dashboard view.
 * Displays tool tiles with icons and recent files.
 */
public class HomeController {

    @FXML private Label welcomeLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label recentTitleLabel;
    
    @FXML private VBox mergeTile;
    @FXML private StackPane mergeIconContainer;
    @FXML private Label mergeTitleLabel;
    @FXML private Label mergeDescLabel;

    @FXML private VBox splitTile;
    @FXML private StackPane splitIconContainer;
    @FXML private Label splitTitleLabel;
    @FXML private Label splitDescLabel;

    @FXML private VBox compressTile;
    @FXML private StackPane compressIconContainer;
    @FXML private Label compressTitleLabel;
    @FXML private Label compressDescLabel;

    @FXML private VBox protectTile;
    @FXML private StackPane protectIconContainer;
    @FXML private Label protectTitleLabel;
    @FXML private Label protectDescLabel;

    @FXML private TableView<AppState.RecentFile> recentFilesTable;
    @FXML private TableColumn<AppState.RecentFile, String> fileNameColumn;
    @FXML private TableColumn<AppState.RecentFile, String> actionColumn;
    @FXML private TableColumn<AppState.RecentFile, String> statusColumn;
    @FXML private TableColumn<AppState.RecentFile, LocalDateTime> timestampColumn;
    @FXML private Label noRecentLabel;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

    @FXML
    private void initialize() {
        // Update texts with i18n
        updateTexts();
        
        // Listen for locale changes
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
            loadRecentFiles(); // Reload to update operation names
        });
        
        // Add icons to tiles
        if (mergeIconContainer != null) {
            mergeIconContainer.getChildren().add(Icons.xlarge("merge"));
        }
        if (splitIconContainer != null) {
            splitIconContainer.getChildren().add(Icons.xlarge("split"));
        }
        if (compressIconContainer != null) {
            compressIconContainer.getChildren().add(Icons.xlarge("resize"));
        }
        if (protectIconContainer != null) {
            protectIconContainer.getChildren().add(Icons.xlarge("lock-bolt"));
        }
        
        // Tile click handlers with animations
        mergeTile.setOnMouseClicked(e -> {
            animateTile(mergeTile);
            AppNavigator.navigateToMerge();
        });
        splitTile.setOnMouseClicked(e -> {
            animateTile(splitTile);
            AppNavigator.navigateToSplit();
        });
        compressTile.setOnMouseClicked(e -> {
            animateTile(compressTile);
            AppNavigator.navigateToCompress();
        });
        protectTile.setOnMouseClicked(e -> {
            animateTile(protectTile);
            AppNavigator.navigateToProtect();
        });
        
        // Hover animations
        setupTileHoverAnimation(mergeTile);
        setupTileHoverAnimation(splitTile);
        setupTileHoverAnimation(compressTile);
        setupTileHoverAnimation(protectTile);

        // Make tiles keyboard accessible
        mergeTile.setFocusTraversable(true);
        splitTile.setFocusTraversable(true);
        compressTile.setFocusTraversable(true);
        protectTile.setFocusTraversable(true);

        mergeTile.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") || e.getCode().toString().equals("SPACE")) {
                AppNavigator.navigateToMerge();
            }
        });
        splitTile.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") || e.getCode().toString().equals("SPACE")) {
                AppNavigator.navigateToSplit();
            }
        });
        compressTile.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") || e.getCode().toString().equals("SPACE")) {
                AppNavigator.navigateToCompress();
            }
        });
        protectTile.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") || e.getCode().toString().equals("SPACE")) {
                AppNavigator.navigateToProtect();
            }
        });

        // Recent files
        setupRecentFilesTable();
        loadRecentFiles();
    }
    
    private void setupTileHoverAnimation(VBox tile) {
        tile.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), tile);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });
        
        tile.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), tile);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
    }
    
    private void animateTile(VBox tile) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(100), tile);
        scale.setToX(0.95);
        scale.setToY(0.95);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }
    
    private void updateTexts() {
        welcomeLabel.setText(LocaleManager.getString("home.title"));
        subtitleLabel.setText(LocaleManager.getString("home.subtitle"));
        recentTitleLabel.setText(LocaleManager.getString("home.recent.title"));
        noRecentLabel.setText(LocaleManager.getString("home.recent.empty"));
        
        mergeTitleLabel.setText(LocaleManager.getString("home.merge.title"));
        mergeDescLabel.setText(LocaleManager.getString("home.merge.description"));
        
        splitTitleLabel.setText(LocaleManager.getString("home.split.title"));
        splitDescLabel.setText(LocaleManager.getString("home.split.description"));
        
        compressTitleLabel.setText(LocaleManager.getString("home.compress.title"));
        compressDescLabel.setText(LocaleManager.getString("home.compress.description"));
        
        protectTitleLabel.setText(LocaleManager.getString("home.protect.title"));
        protectDescLabel.setText(LocaleManager.getString("home.protect.description"));
    }
    
    private void setupRecentFilesTable() {
        if (recentFilesTable == null) return;
        
        // Setup columns
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileNameColumn.setText(LocaleManager.getString("home.recent.fileName"));
        
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("operation"));
        actionColumn.setText(LocaleManager.getString("home.recent.action"));
        
        statusColumn.setCellValueFactory(cellData -> {
            boolean success = cellData.getValue().isSuccess();
            return new javafx.beans.property.SimpleStringProperty(
                success ? "✓ " + LocaleManager.getString("home.recent.success") 
                        : "✗ " + LocaleManager.getString("home.recent.failed")
            );
        });
        statusColumn.setText(LocaleManager.getString("home.recent.status"));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    HBox container = new HBox(6);
                    container.setAlignment(Pos.CENTER_LEFT);
                    
                    // Create icon label
                    Label iconLabel = new Label();
                    iconLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700;");
                    
                    // Create text label
                    Label textLabel = new Label();
                    textLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
                    
                    if (item.startsWith("✓")) {
                        iconLabel.setText("✓");
                        textLabel.setText(LocaleManager.getString("home.recent.success"));
                        iconLabel.setStyle("-fx-text-fill: #1a7f64; -fx-font-size: 14px; -fx-font-weight: 700;");
                        textLabel.setStyle("-fx-text-fill: #1a7f64; -fx-font-size: 13px; -fx-font-weight: 600;");
                    } else {
                        iconLabel.setText("✗");
                        textLabel.setText(LocaleManager.getString("home.recent.failed"));
                        iconLabel.setStyle("-fx-text-fill: #d1242f; -fx-font-size: 14px; -fx-font-weight: 700;");
                        textLabel.setStyle("-fx-text-fill: #d1242f; -fx-font-size: 13px; -fx-font-weight: 600;");
                    }
                    
                    container.getChildren().addAll(iconLabel, textLabel);
                    setGraphic(container);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });
        
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timestampColumn.setText(LocaleManager.getString("home.recent.timestamp"));
        timestampColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(TIME_FORMATTER));
                }
            }
        });
        
        // Make table not editable
        recentFilesTable.setEditable(false);
        recentFilesTable.setSelectionModel(null);
    }

    private void loadRecentFiles() {
        var recentFiles = AppState.getInstance().getRecentFiles();
        
        if (recentFiles.isEmpty()) {
            noRecentLabel.setVisible(true);
            if (recentFilesTable != null) {
                recentFilesTable.setVisible(false);
            }
        } else {
            noRecentLabel.setVisible(false);
            if (recentFilesTable != null) {
                recentFilesTable.setVisible(true);
                recentFilesTable.setItems(recentFiles);
            }
        }
    }
}
