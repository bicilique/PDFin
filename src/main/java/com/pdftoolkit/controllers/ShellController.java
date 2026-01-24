package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.ui.Icons;
import com.pdftoolkit.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/**
 * Controller for the main shell layout (app_shell.fxml).
 * Handles navigation from the sidebar with icons and theme switching.
 */
public class ShellController {

    @FXML private Button homeButton;
    @FXML private Button mergeButton;
    @FXML private Button splitButton;
    @FXML private Button compressButton;
    @FXML private Button protectButton;
    @FXML private Button themeToggleButton;
    
    private Button currentActiveNav;

    @FXML
    private void initialize() {
        // Add icons to navigation buttons
        homeButton.setGraphic(Icons.create("home", 18));
        mergeButton.setGraphic(Icons.create("merge", 18));
        splitButton.setGraphic(Icons.create("split", 18));
        compressButton.setGraphic(Icons.create("compress", 18));
        protectButton.setGraphic(Icons.create("lock", 18));
        
        // Setup theme toggle
        updateThemeButton();
        
        // Navigation handlers
        homeButton.setOnAction(e -> {
            AppNavigator.navigateToHome();
            setActiveNav(homeButton);
        });
        mergeButton.setOnAction(e -> {
            AppNavigator.navigateToMerge();
            setActiveNav(mergeButton);
        });
        splitButton.setOnAction(e -> {
            AppNavigator.navigateToSplit();
            setActiveNav(splitButton);
        });
        compressButton.setOnAction(e -> {
            AppNavigator.navigateToCompress();
            setActiveNav(compressButton);
        });
        protectButton.setOnAction(e -> {
            AppNavigator.navigateToProtect();
            setActiveNav(protectButton);
        });
        
        // Set home as default active
        setActiveNav(homeButton);
    }
    
    @FXML
    private void handleThemeToggle() {
        ThemeManager.toggleTheme();
        updateThemeButton();
    }
    
    private void updateThemeButton() {
        themeToggleButton.setText(ThemeManager.getThemeIcon());
        themeToggleButton.setTooltip(new Tooltip(ThemeManager.getThemeTooltip()));
    }
    
    private void setActiveNav(Button navButton) {
        if (currentActiveNav != null) {
            currentActiveNav.getStyleClass().remove("active");
        }
        navButton.getStyleClass().add("active");
        currentActiveNav = navButton;
    }
}
