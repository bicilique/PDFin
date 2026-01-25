package com.pdftoolkit.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A card component representing a single page thumbnail in the PDF split preview.
 * Shows a thumbnail image of the page with page number overlay.
 */
public class PageThumbnailCard extends VBox {
    
    private final int pageNumber;
    private final StackPane thumbnailContainer;
    private final ImageView thumbnailView;
    private final Label pageNumberLabel;
    private final Label selectionIndicator;
    private boolean selected = false;
    
    private static final double THUMBNAIL_WIDTH = 120;
    private static final double THUMBNAIL_HEIGHT = 160;
    
    public PageThumbnailCard(int pageNumber) {
        this.pageNumber = pageNumber;
        
        // Configure card container
        setSpacing(8);
        setAlignment(Pos.CENTER);
        getStyleClass().add("page-thumbnail-card");
        setPrefWidth(THUMBNAIL_WIDTH + 16);
        setMaxWidth(THUMBNAIL_WIDTH + 16);
        
        // Thumbnail container with image
        thumbnailContainer = new StackPane();
        thumbnailContainer.getStyleClass().add("page-thumbnail-container");
        thumbnailContainer.setPrefSize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        thumbnailContainer.setMinSize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        thumbnailContainer.setMaxSize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        
        // Thumbnail image view
        thumbnailView = new ImageView();
        thumbnailView.setFitWidth(THUMBNAIL_WIDTH);
        thumbnailView.setFitHeight(THUMBNAIL_HEIGHT);
        thumbnailView.setPreserveRatio(true);
        thumbnailView.setSmooth(true);
        
        // Selection indicator (checkmark overlay)
        selectionIndicator = new Label("✓");
        selectionIndicator.getStyleClass().add("page-selection-indicator");
        selectionIndicator.setVisible(false);
        selectionIndicator.setManaged(false);
        StackPane.setAlignment(selectionIndicator, Pos.TOP_RIGHT);
        StackPane.setMargin(selectionIndicator, new Insets(4, 4, 0, 0));
        
        thumbnailContainer.getChildren().addAll(thumbnailView, selectionIndicator);
        
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
        });
        setOnMouseExited(e -> {
            thumbnailContainer.getStyleClass().remove("hover");
        });
    }
    
    /**
     * Set the thumbnail image for this page.
     * @param image The thumbnail image
     */
    public void setThumbnail(Image image) {
        if (image != null) {
            thumbnailView.setImage(image);
        }
    }
    
    /**
     * Set the loading state with a placeholder.
     */
    public void setLoading() {
        Label loadingLabel = new Label("⟳");
        loadingLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: #94a3b8;");
        thumbnailContainer.getChildren().clear();
        thumbnailContainer.getChildren().add(loadingLabel);
    }
    
    /**
     * Set the error state.
     */
    public void setError() {
        Label errorLabel = new Label("✕");
        errorLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: #ef4444;");
        thumbnailContainer.getChildren().clear();
        thumbnailContainer.getChildren().add(errorLabel);
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
}
