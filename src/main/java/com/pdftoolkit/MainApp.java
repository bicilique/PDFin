package com.pdftoolkit;

import com.pdftoolkit.navigation.AppNavigator;
import com.pdftoolkit.utils.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main application entry point.
 * Initializes the JavaFX application and loads the shell layout.
 */
public class MainApp extends Application {

    private static final String APP_TITLE = "PDFin — PDF Tools";
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 800;
    private static final int MIN_WIDTH = 900;
    private static final int MIN_HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize navigator with primary stage
            AppNavigator.initialize(primaryStage);
            
            // Load shell layout
            Scene scene = new Scene(AppNavigator.loadShell(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
            
            // Load CSS
            scene.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
            );
            
            // Initialize theme manager
            ThemeManager.initialize(scene);
            
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            
            // Optional: Set application icon
            // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app-icon.png")));
            
            primaryStage.show();
            
            // Navigate to home after shell is loaded
            AppNavigator.navigateToHome();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start application: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
