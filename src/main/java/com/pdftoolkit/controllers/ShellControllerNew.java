package com.pdftoolkit.controllers;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.ui.AboutDialog;
import com.pdftoolkit.ui.Icons;
import com.pdftoolkit.utils.LocaleManager;
import com.pdftoolkit.utils.ThemeManager;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
    @FXML private StackPane themeIconContainer;
    
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
        // Setup About button with icon
        setupAboutButton();
        
        // Setup language menu
        setupLanguageMenu();
        
        // Setup theme toggle (delay icon update to ensure theme is loaded)
        setupThemeToggle();
        
        // Setup navigation handlers
        setupNavigation();
        
        // Setup navigation icons
        setupNavigationIcons();
        
        // Update all text with current locale
        updateTexts();
        
        // Listen for locale changes
        LocaleManager.localeProperty().addListener((obs, oldVal, newVal) -> {
            updateTexts();
        });
        
        // Set home as default active
        setActiveNav(homeButton);
        
        // Update theme icon after initialization to ensure correct initial state
        javafx.application.Platform.runLater(() -> updateThemeIcon());
    }
    
    private void setupAboutButton() {
        // Replace text icon with SVG icon
        aboutButton.setGraphic(Icons.create("info-circle", 20));
        aboutButton.setTooltip(new Tooltip(LocaleManager.getString("about.tooltip")));
        aboutButton.setOnAction(e -> {
            animateButton(aboutButton);
            AboutDialog.show();
        });
    }
    
    private void setupNavigationIcons() {
        // Update home button icon
        if (homeButton.getGraphic() instanceof HBox) {
            HBox hbox = (HBox) homeButton.getGraphic();
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                hbox.getChildren().set(0, Icons.create("home", 20));
            }
        }
        
        // Update merge button icon
        if (mergeButton.getGraphic() instanceof HBox) {
            HBox hbox = (HBox) mergeButton.getGraphic();
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                hbox.getChildren().set(0, Icons.create("merge", 20));
            }
        }
        
        // Update split button icon
        if (splitButton.getGraphic() instanceof HBox) {
            HBox hbox = (HBox) splitButton.getGraphic();
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                hbox.getChildren().set(0, Icons.create("split", 20));
            }
        }
        
        // Update compress button icon (using resize icon)
        if (compressButton.getGraphic() instanceof HBox) {
            HBox hbox = (HBox) compressButton.getGraphic();
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                hbox.getChildren().set(0, Icons.create("resize", 20));
            }
        }
        
        // Update protect button icon (using lock-bolt icon)
        if (protectButton.getGraphic() instanceof HBox) {
            HBox hbox = (HBox) protectButton.getGraphic();
            if (!hbox.getChildren().isEmpty() && hbox.getChildren().get(0) instanceof Label) {
                hbox.getChildren().set(0, Icons.create("lock-bolt", 20));
            }
        }
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
        // Don't update icon here - will be done after full initialization
        themeToggleButton.setOnAction(e -> {
            ThemeManager.toggleTheme();
            // Update icon immediately after toggle
            updateThemeIcon();
        });
    }
    
    private void updateThemeIcon() {
        if (themeIconContainer != null) {
            themeIconContainer.getChildren().clear();
            // Show sun icon in dark mode (to switch to light), moon icon in light mode (to switch to dark)
            if (ThemeManager.isDarkMode()) {
                themeIconContainer.getChildren().add(Icons.create("sun", 20));
            } else {
                themeIconContainer.getChildren().add(Icons.create("moon", 20));
            }
        }
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
        // Update theme icon when reloading (in case it changed)
        updateThemeIcon();
        
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
