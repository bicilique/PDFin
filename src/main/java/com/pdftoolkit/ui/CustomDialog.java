package com.pdftoolkit.ui;

import com.pdftoolkit.utils.LocaleManager;
import com.pdftoolkit.utils.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.util.Optional;

/**
 * Custom dialog utility that follows the application's design system.
 * Replaces standard JavaFX Alert dialogs with themed, modern dialogs.
 * Supports language switching and light/dark mode.
 */
public class CustomDialog {
    
    public enum Type {
        SUCCESS,
        ERROR,
        WARNING,
        INFO,
        CONFIRMATION
    }
    
    /**
     * Show a success dialog.
     */
    public static void showSuccess(String title, String message) {
        show(Type.SUCCESS, title, message, null);
    }
    
    /**
     * Show an error dialog.
     */
    public static void showError(String title, String message) {
        show(Type.ERROR, title, message, null);
    }
    
    /**
     * Show a warning dialog.
     */
    public static void showWarning(String title, String message) {
        show(Type.WARNING, title, message, null);
    }
    
    /**
     * Show an info dialog.
     */
    public static void showInfo(String title, String message) {
        show(Type.INFO, title, message, null);
    }
    
    /**
     * Show a confirmation dialog and return the user's choice.
     */
    public static boolean showConfirmation(String title, String message) {
        return showConfirmation(title, message, 
            LocaleManager.getString("common.ok"), 
            LocaleManager.getString("common.cancel"));
    }
    
    /**
     * Show a confirmation dialog with custom button labels.
     */
    public static boolean showConfirmation(String title, String message, String confirmText, String cancelText) {
        Dialog<ButtonType> dialog = createDialog(Type.CONFIRMATION, title, message, null);
        
        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().setAll(confirmButton, cancelButton);
        
        // Style buttons
        Button confirmBtn = (Button) dialog.getDialogPane().lookupButton(confirmButton);
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelButton);
        
        confirmBtn.getStyleClass().addAll("btn", "btn-primary");
        cancelBtn.getStyleClass().addAll("btn", "btn-secondary");
        
        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }
    
    /**
     * Show a dialog with three options (e.g., Save, Don't Save, Cancel).
     */
    public static Optional<ButtonType> showTripleChoice(String title, String message, 
                                                        String option1, String option2, String option3) {
        Dialog<ButtonType> dialog = createDialog(Type.WARNING, title, message, null);
        
        ButtonType button1 = new ButtonType(option1, ButtonBar.ButtonData.YES);
        ButtonType button2 = new ButtonType(option2, ButtonBar.ButtonData.NO);
        ButtonType button3 = new ButtonType(option3, ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().setAll(button1, button2, button3);
        
        // Style buttons
        Button btn1 = (Button) dialog.getDialogPane().lookupButton(button1);
        Button btn2 = (Button) dialog.getDialogPane().lookupButton(button2);
        Button btn3 = (Button) dialog.getDialogPane().lookupButton(button3);
        
        btn1.getStyleClass().addAll("btn", "btn-primary");
        btn2.getStyleClass().addAll("btn", "btn-secondary");
        btn3.getStyleClass().addAll("btn", "btn-secondary");
        
        return dialog.showAndWait();
    }
    
    /**
     * Show a custom dialog with arbitrary content.
     */
    public static void show(Type type, String title, String message, Node customContent) {
        Dialog<Void> dialog = createDialog(type, title, message, customContent);
        
        ButtonType okButton = new ButtonType(LocaleManager.getString("common.ok"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okButton);
        
        // Style button
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(okButton);
        okBtn.getStyleClass().addAll("btn", "btn-primary");
        okBtn.setDefaultButton(true);
        
        dialog.showAndWait();
    }
    
    /**
     * Create a styled dialog.
     */
    private static <T> Dialog<T> createDialog(Type type, String title, String message, Node customContent) {
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        // Create custom dialog pane
        DialogPane dialogPane = new DialogPane();
        dialogPane.getStyleClass().add("custom-dialog");
        
        // Apply dark mode class if active
        if (ThemeManager.isDarkMode()) {
            dialogPane.getStyleClass().add("dark-mode");
        }
        
        // Load CSS (tokens.css is imported by app.css)
        dialogPane.getStylesheets().addAll(
            CustomDialog.class.getResource("/css/tokens.css").toExternalForm(),
            CustomDialog.class.getResource("/css/components.css").toExternalForm(),
            CustomDialog.class.getResource("/css/app.css").toExternalForm()
        );
        
        // Create content
        VBox content = new VBox(16);
        content.getStyleClass().add("dialog-content");
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_LEFT);
        
        // Icon and title row
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(getIcon(type));
        iconLabel.getStyleClass().addAll("dialog-icon", "dialog-icon-" + type.name().toLowerCase());
        iconLabel.setStyle("-fx-font-size: 32px;");
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        header.getChildren().addAll(iconLabel, titleLabel);
        
        // Message
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(480);
        
        content.getChildren().addAll(header, messageLabel);
        
        // Add custom content if provided
        if (customContent != null) {
            content.getChildren().add(customContent);
        }
        
        dialogPane.setContent(content);
        dialog.setDialogPane(dialogPane);
        
        return dialog;
    }
    
    /**
     * Get icon for dialog type.
     */
    private static String getIcon(Type type) {
        return switch (type) {
            case SUCCESS -> "✓";
            case ERROR -> "✕";
            case WARNING -> "⚠";
            case INFO -> "ℹ";
            case CONFIRMATION -> "?";
        };
    }
}
