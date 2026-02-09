package com.pdftoolkit.ui;

import com.pdftoolkit.utils.PdfMetadataUtil;
import javafx.application.Platform;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;

/**
 * Custom card component for displaying PDF file information.
 * Shows thumbnail, filename, file size, and page count.
 * Automatically loads thumbnails and page counts asynchronously.
 */
public class PdfFileCard extends HBox {
    
    private final File file;
    private final ImageView thumbnailView;
    private final Label nameLabel;
    private final Label sizeLabel;
    private final Label pagesLabel;
    private final Label dragHandle;
    
    private int pageCount = 0;
    
    public PdfFileCard(File file) {
        this.file = file;
        
        getStyleClass().add("pdf-card");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(12));
        
        // Drag handle
        dragHandle = new Label("⋮⋮");
        dragHandle.getStyleClass().add("drag-handle");
        dragHandle.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8; -fx-cursor: hand;");
        
        // Thumbnail with ImageView
        thumbnailView = new ImageView();
        thumbnailView.setFitWidth(48);
        thumbnailView.setFitHeight(64);
        thumbnailView.setPreserveRatio(true);
        thumbnailView.getStyleClass().add("pdf-thumbnail");
        
        VBox thumbnailContainer = new VBox(thumbnailView);
        thumbnailContainer.setAlignment(Pos.CENTER);
        thumbnailContainer.setMinSize(48, 64);
        thumbnailContainer.setMaxSize(48, 64);
        thumbnailContainer.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 6px;");
        thumbnailContainer.getStyleClass().add("thumbnail-container");
        
        // Load thumbnail asynchronously
        loadThumbnailAsync();
        
        // File info
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        nameLabel = new Label(file.getName());
        nameLabel.getStyleClass().add("pdf-card-name");
        nameLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14px;");
        nameLabel.setWrapText(false);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        
        HBox metaBox = new HBox(12);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        
        sizeLabel = new Label(formatFileSize(file.length()));
        sizeLabel.getStyleClass().add("pdf-card-meta");
        sizeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        
        pagesLabel = new Label("— pages");
        pagesLabel.getStyleClass().add("pdf-card-meta");
        pagesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        
        metaBox.getChildren().addAll(sizeLabel, pagesLabel);
        infoBox.getChildren().addAll(nameLabel, metaBox);
        
        getChildren().addAll(dragHandle, thumbnailContainer, infoBox);
        
        // Load thumbnail and page count asynchronously
        loadThumbnailAsync();
        loadPageCountAsync();
    }
    
    private void loadThumbnailAsync() {
        // Load thumbnail in background thread to avoid blocking UI
        Thread thumbnailThread = new Thread(() -> {
            Image thumbnail = PdfMetadataUtil.generateThumbnail(file);
            Platform.runLater(() -> {
                if (thumbnail != null) {
                    thumbnailView.setImage(thumbnail);
                }
            });
        });
        thumbnailThread.setDaemon(true);
        thumbnailThread.start();
    }
    
    private void loadPageCountAsync() {
        // Load page count in background thread
        Thread pageCountThread = new Thread(() -> {
            int count = PdfMetadataUtil.getPageCount(file);
            Platform.runLater(() -> {
                setPageCount(count);
            });
        });
        pageCountThread.setDaemon(true);
        pageCountThread.start();
    }
    
    public File getFile() {
        return file;
    }
    
    public void setPageCount(int count) {
        this.pageCount = count;
        pagesLabel.setText(count + " pages");
    }
    
    public int getPageCount() {
        return pageCount;
    }
    
    public void setThumbnail(Image thumbnail) {
        if (thumbnail != null) {
            thumbnailView.setImage(thumbnail);
        }
    }
    
    public Label getDragHandle() {
        return dragHandle;
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
