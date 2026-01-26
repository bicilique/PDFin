package com.pdftoolkit.ui;

import com.pdftoolkit.models.PdfItem;
import com.pdftoolkit.utils.LocaleManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Custom ListCell for displaying PdfItem in a ListView.
 * Features:
 * - Only updates when the item changes (prevents unnecessary re-renders)
 * - Shows thumbnail, file name, size, and page count
 * - Displays loading indicator while metadata is being loaded
 * - Shows error state if loading fails
 * - Includes remove button
 * - Supports drag & drop for reordering
 */
public class PdfItemCell extends ListCell<PdfItem> {
    
    private final HBox root;
    private final Label dragHandle;
    private final VBox thumbnailContainer;
    private final ImageView thumbnailView;
    private final ProgressIndicator loadingIndicator;
    private final VBox infoBox;
    private final Label nameLabel;
    private final Label metaLabel;
    private final Label errorLabel;
    private final Button removeButton;
    
    private final Consumer<PdfItem> onRemove;
    
    private PdfItem currentItem;
    
    /**
     * Create a new PdfItemCell.
     * @param onRemove Callback invoked when the remove button is clicked
     */
    public PdfItemCell(Consumer<PdfItem> onRemove) {
        this.onRemove = onRemove;
        
        // Root container
        root = new HBox(12);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(12));
        root.getStyleClass().add("pdf-item-cell");
        
        // Drag handle
        dragHandle = new Label("⋮⋮");
        dragHandle.getStyleClass().add("drag-handle");
        dragHandle.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8; -fx-cursor: hand;");
        
        // Thumbnail container with loading indicator
        thumbnailContainer = new VBox();
        thumbnailContainer.setAlignment(Pos.CENTER);
        thumbnailContainer.setMinSize(48, 64);
        thumbnailContainer.setMaxSize(48, 64);
        thumbnailContainer.getStyleClass().add("thumbnail-container");
        thumbnailContainer.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 6px;");
        
        thumbnailView = new ImageView();
        thumbnailView.setFitWidth(48);
        thumbnailView.setFitHeight(64);
        thumbnailView.setPreserveRatio(true);
        thumbnailView.getStyleClass().add("pdf-thumbnail");
        
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(32, 32);
        loadingIndicator.setVisible(false);
        
        thumbnailContainer.getChildren().addAll(thumbnailView, loadingIndicator);
        
        // Info box (file name + metadata)
        infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        nameLabel = new Label();
        nameLabel.getStyleClass().add("pdf-item-name");
        nameLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14px;");
        nameLabel.setWrapText(false);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        
        metaLabel = new Label();
        metaLabel.getStyleClass().add("pdf-item-meta");
        metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        
        errorLabel = new Label();
        errorLabel.getStyleClass().add("pdf-item-error");
        errorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        infoBox.getChildren().addAll(nameLabel, metaLabel, errorLabel);
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Remove button
        removeButton = new Button("×");
        removeButton.getStyleClass().addAll("icon-button", "danger");
        removeButton.setStyle("-fx-font-size: 20px; -fx-min-width: 32px; -fx-min-height: 32px; " +
                              "-fx-background-radius: 6px; -fx-cursor: hand;");
        removeButton.setOnAction(e -> {
            if (currentItem != null && onRemove != null) {
                onRemove.accept(currentItem);
            }
        });
        
        root.getChildren().addAll(dragHandle, thumbnailContainer, infoBox, spacer, removeButton);
        
        // Setup drag and drop for reordering
        setupDragAndDrop();
    }
    
    @Override
    protected void updateItem(PdfItem item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setGraphic(null);
            currentItem = null;
            clearBindings();
        } else {
            // Only update if the item actually changed
            if (currentItem != item) {
                currentItem = item;
                setupBindings(item);
            }
            setGraphic(root);
        }
    }
    
    /**
     * Setup bindings to the PdfItem properties.
     * This ensures the UI updates automatically when the item's data loads.
     */
    private void setupBindings(PdfItem item) {
        // Clear old bindings
        clearBindings();
        
        // Bind file name
        nameLabel.textProperty().bind(item.fileNameProperty());
        
        // Use listeners instead of binding for metaLabel to allow manual updates
        item.fileSizeBytesProperty().addListener((obs, oldVal, newVal) -> {
            updateMetaLabel(item);
        });
        
        item.pageCountProperty().addListener((obs, oldVal, newVal) -> {
            updateMetaLabel(item);
        });
        
        item.loadingProperty().addListener((obs, oldVal, newVal) -> {
            updateMetaLabel(item);
        });
        
        // Initial meta label update
        updateMetaLabel(item);
        
        // Bind thumbnail
        item.thumbnailProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                thumbnailView.setImage(newVal);
                thumbnailView.setVisible(true);
                loadingIndicator.setVisible(false);
            }
        });
        
        // Initial thumbnail
        if (item.getThumbnail() != null) {
            thumbnailView.setImage(item.getThumbnail());
            thumbnailView.setVisible(true);
            loadingIndicator.setVisible(false);
        } else {
            thumbnailView.setImage(null);
            thumbnailView.setVisible(false);
        }
        
        // Bind loading state
        item.loadingProperty().addListener((obs, oldVal, newVal) -> {
            loadingIndicator.setVisible(newVal);
            thumbnailView.setVisible(!newVal && item.getThumbnail() != null);
        });
        
        // Initial loading state
        loadingIndicator.setVisible(item.isLoading());
        thumbnailView.setVisible(!item.isLoading() && item.getThumbnail() != null);
        
        // Bind error state
        item.errorProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasError = newVal != null && !newVal.isEmpty();
            errorLabel.setText(newVal);
            errorLabel.setVisible(hasError);
            errorLabel.setManaged(hasError);
        });
        
        // Initial error state
        if (item.hasError()) {
            errorLabel.setText(item.getError());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        } else {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
    
    /**
     * Update the meta label with formatted values.
     */
    private void updateMetaLabel(PdfItem item) {
        if (item.isLoading()) {
            metaLabel.setText("Loading...");
            return;
        }
        
        String sizeStr = item.getFormattedFileSize();
        int pages = item.getPageCount();
        
        if (pages > 0) {
            String pageWord = pages == 1 ? "page" : "pages";
            metaLabel.setText(sizeStr + " • " + pages + " " + pageWord);
        } else {
            metaLabel.setText(sizeStr);
        }
    }
    
    /**
     * Clear all bindings to prevent memory leaks.
     */
    private void clearBindings() {
        nameLabel.textProperty().unbind();
        metaLabel.textProperty().unbind();
    }
    
    /**
     * Setup drag and drop for reordering items in the list.
     */
    private void setupDragAndDrop() {
        // Drag detected - start drag operation
        root.setOnDragDetected(event -> {
            if (currentItem == null) return;
            
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(getIndex()));
            db.setContent(content);
            
            root.getStyleClass().add("dragging");
            event.consume();
        });
        
        // Drag over - accept drag if from another cell
        root.setOnDragOver(event -> {
            if (event.getGestureSource() != root && 
                event.getDragboard().hasString() &&
                currentItem != null) {
                event.acceptTransferModes(TransferMode.MOVE);
                root.getStyleClass().add("drag-over");
            }
            event.consume();
        });
        
        // Drag exited - remove highlight
        root.setOnDragExited(event -> {
            root.getStyleClass().remove("drag-over");
            event.consume();
        });
        
        // Drag dropped - signal to parent ListView to handle reordering
        root.setOnDragDropped(event -> {
            // The actual reordering is handled by the ListView/Controller
            // This just marks the drop as successful
            event.setDropCompleted(true);
            event.consume();
        });
        
        // Drag done - cleanup
        root.setOnDragDone(event -> {
            root.getStyleClass().remove("dragging");
            event.consume();
        });
    }
}
