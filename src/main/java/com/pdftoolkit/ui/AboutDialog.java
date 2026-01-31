package com.pdftoolkit.ui;

import com.pdftoolkit.utils.LocaleManager;
import com.pdftoolkit.utils.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

/**
 * About Dialog showing application information, version, and credits.
 * Follows the application's design system with modern styling.
 */
public class AboutDialog {
    
    private static final String APP_VERSION = loadVersion();
    private static final String APP_NAME = "PDFin";
    private static final String GITHUB_URL = "https://github.com/bicilique";
    private static final String COPYRIGHT = "© 2026 Cildev";
    
    /**
     * Show the About dialog with application information.
     */
    public static void show() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(LocaleManager.getString("about.title"));
        
        // Create dialog pane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
            AboutDialog.class.getResource("/css/app.css").toExternalForm()
        );
        dialogPane.getStyleClass().addAll("custom-dialog", "about-dialog");
        
        // Apply current theme
        if (ThemeManager.isDarkMode()) {
            dialogPane.getStyleClass().add("dark-mode");
        }
        
        // Create main container with close button
        BorderPane mainContainer = new BorderPane();
        
        // Create content
        VBox content = createContent();
        mainContainer.setCenter(content);
        
        // Create close button (X) in top right
        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("dialog-close-button");
        closeButton.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: -fx-text-base-color;" +
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 8px 12px;" +
            "-fx-background-radius: 4px;"
        );
        
        // Close action
        closeButton.setOnAction(e -> {
            dialog.setResult(ButtonType.CLOSE);
            dialog.close();
        });
        
        // Hover effect
        closeButton.setOnMouseEntered(e -> {
            closeButton.setStyle(
                "-fx-background-color: rgba(255, 0, 0, 0.1);" +
                "-fx-text-fill: #ff4444;" +
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8px 12px;" +
                "-fx-background-radius: 4px;"
            );
        });
        
        closeButton.setOnMouseExited(e -> {
            closeButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: -fx-text-base-color;" +
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8px 12px;" +
                "-fx-background-radius: 4px;"
            );
        });
        
        // Position close button in top right using HBox
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(8, 8, 0, 8));
        topBar.getChildren().add(closeButton);
        mainContainer.setTop(topBar);
        
        dialogPane.setContent(mainContainer);
        
        // Remove default buttons
        dialogPane.getButtonTypes().clear();
        
        dialog.showAndWait();
    }
    
    /**
     * Create the content of the about dialog.
     */
    private static VBox createContent() {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(32, 40, 32, 40));
        container.setPrefWidth(420);
        
        // App Icon/Logo
        Label logo = new Label("📄");
        logo.setStyle("-fx-font-size: 64px;");
        
        // App Name
        Label appName = new Label(APP_NAME);
        appName.getStyleClass().add("about-app-name");
        appName.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");
        
        // Tagline
        Label tagline = new Label(LocaleManager.getString("app.tagline"));
        tagline.getStyleClass().add("about-tagline");
        tagline.setStyle("-fx-font-size: 14px; -fx-opacity: 0.7;");
        
        // Version
        Label version = new Label(LocaleManager.getString("about.version") + " " + APP_VERSION);
        version.getStyleClass().add("about-version");
        version.setStyle("-fx-font-size: 13px; -fx-opacity: 0.6;");
        
        // Separator
        Separator separator = new Separator();
        separator.setPrefWidth(180);
        separator.setStyle("-fx-opacity: 0.3;");
        
        // GitHub link with icon
        HBox githubBox = createGitHubLink();
        
        // Separator
        Separator separator2 = new Separator();
        separator2.setPrefWidth(180);
        separator2.setStyle("-fx-opacity: 0.3;");
        
        // Copyright
        Label copyright = new Label(COPYRIGHT);
        copyright.getStyleClass().add("about-copyright");
        copyright.setStyle("-fx-font-size: 12px; -fx-opacity: 0.5;");
        
        // Java/JavaFX version info
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version", "21");
        Label techInfo = new Label(
            String.format("Java %s • JavaFX %s", javaVersion, javafxVersion)
        );
        techInfo.getStyleClass().add("about-tech-info");
        techInfo.setStyle("-fx-font-size: 11px; -fx-opacity: 0.4;");
        
        // Add all elements
        container.getChildren().addAll(
            logo,
            appName,
            tagline,
            version,
            separator,
            githubBox,
            separator2,
            copyright,
            techInfo
        );
        
        return container;
    }
    
    
    /**
     * Create GitHub link section with icon.
     */
    private static HBox createGitHubLink() {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER);
        
        // GitHub icon
        Label githubIcon = new Label("⚡");
        githubIcon.setStyle("-fx-font-size: 16px; -fx-opacity: 0.7;");
        
        // Link
        Hyperlink githubLink = new Hyperlink(GITHUB_URL);
        githubLink.getStyleClass().add("about-link");
        githubLink.setOnAction(e -> openURL(GITHUB_URL));
        githubLink.setStyle("-fx-font-size: 13px;");
        
        container.getChildren().addAll(githubIcon, githubLink);
        return container;
    }
    
    /**
     * Load version from pom.xml properties or return default.
     */
    private static String loadVersion() {
        try (InputStream input = AboutDialog.class.getResourceAsStream("/version.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                return props.getProperty("version", "1.0.0");
            }
        } catch (IOException e) {
            // Ignore and use default
        }
        return "1.0.0";
    }
    
    /**
     * Open URL in default browser.
     */
    private static void openURL(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback: copy to clipboard
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(url);
                clipboard.setContent(content);
                
                CustomDialog.showInfo(
                    LocaleManager.getString("about.link.title"),
                    LocaleManager.getString("about.link.copied")
                );
            }
        } catch (Exception e) {
            CustomDialog.showError(
                LocaleManager.getString("common.error"),
                LocaleManager.getString("about.link.error")
            );
        }
    }
}
