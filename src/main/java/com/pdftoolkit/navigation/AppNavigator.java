package com.pdftoolkit.navigation;

import com.pdftoolkit.utils.LocaleManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Centralized navigation controller for managing view transitions.
 * Loads FXML files and swaps them into the shell's content area.
 */
public class AppNavigator {

    private static Stage primaryStage;
    private static BorderPane shellLayout;

    public enum View {
        HOME("/views/home.fxml"),
        // MERGE("/views/merge.fxml"),
        // SPLIT("/views/split.fxml"),
        MERGE("/views/merge_redesigned_new.fxml"),
        SPLIT("/views/split_redesigned.fxml"),
        COMPRESS("/views/compress_redesigned.fxml"),
        PROTECT("/views/protect.fxml");

        private final String fxmlPath;

        View(String fxmlPath) {
            this.fxmlPath = fxmlPath;
        }

        public String getFxmlPath() {
            return fxmlPath;
        }
    }

    /**
     * Initialize the navigator with the primary stage.
     */
    public static void initialize(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Load the shell layout (app_shell_new.fxml - modernized version with i18n).
     */
    public static Parent loadShell() throws IOException {
        FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource("/views/app_shell_new.fxml"));
        loader.setResources(LocaleManager.getBundle());
        shellLayout = loader.load();
        return shellLayout;
    }

    /**
     * Navigate to a specific view by loading its FXML and setting it as the center content.
     * Includes fade transition for smooth page switching.
     */
    public static void navigateTo(View view) {
        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(view.getFxmlPath()));
            loader.setResources(LocaleManager.getBundle());
            Parent content = loader.load();
            
            // Fade out current content
            Parent currentContent = (Parent) shellLayout.getCenter();
            if (currentContent != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), currentContent);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    // Set new content and fade in
                    shellLayout.setCenter(content);
                    content.setOpacity(0.0);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), content);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                // First load, no fade out needed
                shellLayout.setCenter(content);
                content.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), content);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to navigate to " + view + ": " + e.getMessage());
        }
    }

    /**
     * Navigate to home view.
     */
    public static void navigateToHome() {
        navigateTo(View.HOME);
    }

    /**
     * Navigate to merge view.
     */
    public static void navigateToMerge() {
        navigateTo(View.MERGE);
    }

    /**
     * Navigate to split view.
     */
    public static void navigateToSplit() {
        navigateTo(View.SPLIT);
    }

    /**
     * Navigate to compress view.
     */
    public static void navigateToCompress() {
        navigateTo(View.COMPRESS);
    }

    /**
     * Navigate to protect view.
     */
    public static void navigateToProtect() {
        navigateTo(View.PROTECT);
    }

    /**
     * Get the primary stage.
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
