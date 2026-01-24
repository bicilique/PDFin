package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.navigation.AppState;
import com.pdftoolkit.ui.Icons;
import com.pdftoolkit.utils.LocaleManager;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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

    @FXML private ListView<String> recentFilesList;
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
            mergeIconContainer.getChildren().add(Icons.create("merge", 48));
        }
        if (splitIconContainer != null) {
            splitIconContainer.getChildren().add(Icons.create("split", 48));
        }
        if (compressIconContainer != null) {
            compressIconContainer.getChildren().add(Icons.create("compress", 48));
        }
        if (protectIconContainer != null) {
            protectIconContainer.getChildren().add(Icons.create("lock", 48));
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

    private void loadRecentFiles() {
        var recentFiles = AppState.getInstance().getRecentFiles();
        
        if (recentFiles.isEmpty()) {
            noRecentLabel.setVisible(true);
            recentFilesList.setVisible(false);
        } else {
            noRecentLabel.setVisible(false);
            recentFilesList.setVisible(true);
            
            recentFiles.forEach(file -> {
                String displayText = String.format("%s - %s (%s)", 
                    file.getOperation(),
                    file.getFileName(),
                    file.getTimestamp().format(TIME_FORMATTER));
                recentFilesList.getItems().add(displayText);
            });
        }
    }
}
