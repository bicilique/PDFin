package com.pdftoolkit.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A card component representing a single page thumbnail in the PDF split preview.
 * Shows a thumbnail image of the page with page number overlay.
 * Supports zoom via size binding.
 */
public class PageThumbnailCard extends VBox {
    
    private final int pageNumber;
    private final StackPane thumbnailContainer;
    private final ImageView thumbnailView;
    private final Label pageNumberLabel;
    private final Label selectionIndicator;
    private final Button deleteButton;
    private final ProgressIndicator loadingIndicator;
    private boolean selected = false;
    private boolean loading = false;
    private Runnable onDeleteAction;
    private Runnable onSelectionChanged;
    
    private static final double BASE_THUMBNAIL_WIDTH = 120;
    private static final double BASE_THUMBNAIL_HEIGHT = 160;
    
    private final DoubleProperty zoomProperty = new SimpleDoubleProperty(1.0);
    
    public PageThumbnailCard(int pageNumber) {
        this.pageNumber = pageNumber;
        
        // Configure card container
        setSpacing(8);
        setAlignment(Pos.CENTER);
        getStyleClass().add("page-thumbnail-card");
        
        // Bind card width to zoom
        prefWidthProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_WIDTH + 16));
        maxWidthProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_WIDTH + 16));
        
        // Thumbnail container with image
        thumbnailContainer = new StackPane();
        thumbnailContainer.getStyleClass().add("page-thumbnail-container");
        
        // Bind container size to zoom
        thumbnailContainer.prefWidthProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_WIDTH));
        thumbnailContainer.prefHeightProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_HEIGHT));
        thumbnailContainer.minWidthProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_WIDTH));
        thumbnailContainer.minHeightProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_HEIGHT));
        thumbnailContainer.maxWidthProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_WIDTH));
        thumbnailContainer.maxHeightProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_HEIGHT));
        
        // Thumbnail image view
        thumbnailView = new ImageView();
        thumbnailView.fitWidthProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_WIDTH));
        thumbnailView.fitHeightProperty().bind(zoomProperty.multiply(BASE_THUMBNAIL_HEIGHT));
        thumbnailView.setPreserveRatio(true);
        thumbnailView.setSmooth(true);
        thumbnailView.getStyleClass().add("thumb-image");
        
        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.getStyleClass().add("thumb-loading");
        loadingIndicator.setMaxSize(40, 40);
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        
        // Selection indicator (checkmark overlay)
        selectionIndicator = new Label("✓");
        selectionIndicator.getStyleClass().add("page-selection-indicator");
        selectionIndicator.setVisible(false);
        selectionIndicator.setManaged(false);
        StackPane.setAlignment(selectionIndicator, Pos.TOP_RIGHT);
        StackPane.setMargin(selectionIndicator, new Insets(4, 4, 0, 0));
        
        // Delete button (X button in top-left corner)
        deleteButton = new Button("×");
        deleteButton.getStyleClass().addAll("delete-page-button");
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);
        StackPane.setAlignment(deleteButton, Pos.TOP_LEFT);
        StackPane.setMargin(deleteButton, new Insets(4, 0, 0, 4));
        deleteButton.setOnAction(e -> {
            e.consume(); // Prevent triggering card selection
            if (onDeleteAction != null) {
                onDeleteAction.run();
            }
        });
        
        thumbnailContainer.getChildren().addAll(thumbnailView, loadingIndicator, selectionIndicator, deleteButton);
        
        // Page number label
        pageNumberLabel = new Label("Page " + pageNumber);
        pageNumberLabel.getStyleClass().add("page-number-label");
        
        getChildren().addAll(thumbnailContainer, pageNumberLabel);
        
        // Click handler for selection
        setOnMouseClicked(e -> toggleSelection());
        
        // Hover effect
        setOnMouseEntered(e -> {
            if (!selected) {
                thumbnailContainer.getStyleClass().add("hover");
            }
            deleteButton.setVisible(true);
            deleteButton.setManaged(true);
        });
        setOnMouseExited(e -> {
            thumbnailContainer.getStyleClass().remove("hover");
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
        });
    }
    
    /**
     * Set the thumbnail image for this page.
     * @param image The thumbnail image
     */
    public void setThumbnail(Image image) {
        if (image != null) {
            thumbnailView.setImage(image);
            setLoading(false);
        }
    }
    
    /**
     * Set the loading state.
     * @param isLoading true to show loading indicator
     */
    public void setLoading(boolean isLoading) {
        this.loading = isLoading;
        loadingIndicator.setVisible(isLoading);
        loadingIndicator.setManaged(isLoading);
        thumbnailView.setVisible(!isLoading);
    }
    
    /**
     * Set the error state.
     */
    public void setError() {
        setLoading(false);
        Label errorLabel = new Label("✕");
        errorLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: #ef4444;");
        errorLabel.getStyleClass().add("thumb-error");
        thumbnailContainer.getChildren().clear();
        thumbnailContainer.getChildren().addAll(errorLabel, selectionIndicator);
    }
    
    /**
     * Toggle the selection state of this page.
     */
    public void toggleSelection() {
        setSelected(!selected);
    }
    
    /**
     * Set the selection state.
     * @param selected true to select, false to deselect
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            getStyleClass().add("selected");
            selectionIndicator.setVisible(true);
        } else {
            getStyleClass().remove("selected");
            selectionIndicator.setVisible(false);
        }
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }
    
    /**
     * Check if this page is currently selected.
     * @return true if selected, false otherwise
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * Get the page number for this card.
     * @return The page number (1-indexed)
     */
    public int getPageNumber() {
        return pageNumber;
    }
    
    /**
     * Get the zoom property for binding.
     * @return Zoom property
     */
    public DoubleProperty zoomProperty() {
        return zoomProperty;
    }
    
    /**
     * Set the zoom level.
     * @param zoom Zoom level (1.0 to 2.0)
     */
    public void setZoom(double zoom) {
        zoomProperty.set(zoom);
    }
    
    /**
     * Get current zoom level.
     * @return Zoom level
     */
    public double getZoom() {
        return zoomProperty.get();
    }
    
    /**
     * Set the action to perform when delete button is clicked.
     * @param action Runnable to execute on delete
     */
    public void setOnDelete(Runnable action) {
        this.onDeleteAction = action;
    }
    
    /**
     * Set the action to perform when selection state changes.
     * @param action Runnable to execute on selection change
     */
    public void setOnSelectionChanged(Runnable action) {
        this.onSelectionChanged = action;
    }
}
