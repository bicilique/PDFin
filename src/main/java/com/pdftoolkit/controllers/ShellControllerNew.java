package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.ui.AboutDialog;
import com.pdftoolkit.utils.LocaleManager;
import com.pdftoolkit.utils.ThemeManager;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

/**
 * Controller for the modernized shell layout with i18n support
 */
public class ShellControllerNew {

    // Header elements
    @FXML private Label headerSubtitle;
    @FXML private Button aboutButton;
    @FXML private MenuButton languageButton;
    @FXML private MenuItem englishMenuItem;
    @FXML private MenuItem indonesianMenuItem;
    @FXML private Button themeToggleButton;
    @FXML private Label themeIcon;
    
    // Navigation buttons
    @FXML private Button homeButton;
    @FXML private Button mergeButton;
    @FXML private Button splitButton;
    @FXML private Button compressButton;
    @FXML private Button protectButton;
    
    // Navigation labels (for i18n updates)
    @FXML private Label homeLabel;
    @FXML private Label toolsLabel;
    @FXML private Label mergeLabel;
    @FXML private Label splitLabel;
    @FXML private Label compressLabel;
    @FXML private Label protectLabel;
    
    private Button currentActiveNav;

    @FXML
    private void initialize() {
        // Setup About button
        setupAboutButton();
        
        // Setup language menu
        setupLanguageMenu();
        
        // Setup theme toggle
        setupThemeToggle();
        
        // Setup navigation handlers
        setupNavigation();
        
        // Update all text with current locale
        updateTexts();
        
        // Listen for locale changes
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
        });
        
        // Set home as default active
        setActiveNav(homeButton);
    }
    
    private void setupAboutButton() {
        aboutButton.setTooltip(new Tooltip(LocaleManager.getString("about.tooltip")));
        aboutButton.setOnAction(e -> {
            animateButton(aboutButton);
            AboutDialog.show();
        });
    }
    
    private void setupLanguageMenu() {
        // Set language button text
        languageButton.setText(LocaleManager.getCurrentLanguageName());
        
        // English menu item - sets to ENGLISH
        englishMenuItem.setOnAction(e -> {
            if (!LocaleManager.getLocale().equals(LocaleManager.ENGLISH)) {
                LocaleManager.setLocale(LocaleManager.ENGLISH);
                languageButton.setText("English");
                // Smooth reload with animation
                smoothReloadCurrentView();
            }
        });
        
        // Indonesian menu item - sets to INDONESIAN
        indonesianMenuItem.setOnAction(e -> {
            if (!LocaleManager.getLocale().equals(LocaleManager.INDONESIAN)) {
                LocaleManager.setLocale(LocaleManager.INDONESIAN);
                languageButton.setText("Bahasa Indonesia");
                // Smooth reload with animation
                smoothReloadCurrentView();
            }
        });
    }
    
    private void smoothReloadCurrentView() {
        // Use delayed reload to allow text updates to process
        javafx.application.Platform.runLater(() -> {
            reloadCurrentView();
        });
    }
    
    private void setupThemeToggle() {
        updateThemeIcon();
        themeToggleButton.setOnAction(e -> {
            ThemeManager.toggleTheme();
            updateThemeIcon();
        });
    }
    
    private void updateThemeIcon() {
        themeIcon.setText(ThemeManager.getThemeIcon());
        themeToggleButton.setTooltip(new Tooltip(ThemeManager.getThemeTooltip()));
    }
    
    private void setupNavigation() {
        homeButton.setOnAction(e -> {
            animateButton(homeButton);
            AppNavigator.navigateToHome();
            setActiveNav(homeButton);
        });
        
        mergeButton.setOnAction(e -> {
            animateButton(mergeButton);
            AppNavigator.navigateToMerge();
            setActiveNav(mergeButton);
        });
        
        splitButton.setOnAction(e -> {
            animateButton(splitButton);
            AppNavigator.navigateToSplit();
            setActiveNav(splitButton);
        });
        
        compressButton.setOnAction(e -> {
            animateButton(compressButton);
            AppNavigator.navigateToCompress();
            setActiveNav(compressButton);
        });
        
        protectButton.setOnAction(e -> {
            animateButton(protectButton);
            AppNavigator.navigateToProtect();
            setActiveNav(protectButton);
        });
    }
    
    private void animateButton(Button button) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.95);
        scale.setToY(0.95);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }
    
    private void updateTexts() {
        // Update header
        headerSubtitle.setText(LocaleManager.getString("app.tagline"));
        
        // Update navigation labels
        homeLabel.setText(LocaleManager.getString("nav.home"));
        toolsLabel.setText(LocaleManager.getString("nav.tools"));
        mergeLabel.setText(LocaleManager.getString("nav.merge"));
        splitLabel.setText(LocaleManager.getString("nav.split"));
        compressLabel.setText(LocaleManager.getString("nav.compress"));
        protectLabel.setText(LocaleManager.getString("nav.protect"));
    }
    
    private void reloadCurrentView() {
        // Get current active view and reload it
        if (currentActiveNav == homeButton) {
            AppNavigator.navigateToHome();
        } else if (currentActiveNav == mergeButton) {
            AppNavigator.navigateToMerge();
        } else if (currentActiveNav == splitButton) {
            AppNavigator.navigateToSplit();
        } else if (currentActiveNav == compressButton) {
            AppNavigator.navigateToCompress();
        } else if (currentActiveNav == protectButton) {
            AppNavigator.navigateToProtect();
        }
    }
    
    private void setActiveNav(Button navButton) {
        if (currentActiveNav != null) {
            currentActiveNav.getStyleClass().remove("active");
        }
        navButton.getStyleClass().add("active");
        currentActiveNav = navButton;
    }
}
