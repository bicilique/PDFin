package com.pdftoolkit.ui;

import com.pdftoolkit.services.PdfThumbnailService;
import com.pdftoolkit.utils.LocaleManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;

/**
 * Custom card component for displaying split range information.
 * Shows range label with first and last page thumbnails in a modern tile layout.
 * Supports async thumbnail loading using PdfThumbnailService.
 */
public class RangeCard extends VBox {
    
    private final int rangeNumber;
    private final Label rangeLabel;
    private final Label rangeText;
    private final HBox thumbnailBox;
    private final VBox firstPageTile;
    private final VBox lastPageTile;
    private final ImageView firstPageThumb;
    private final ImageView lastPageThumb;
    private final Label firstPageLabel;
    private final Label lastPageLabel;
    private final Button removeButton;
    private final PdfThumbnailService thumbnailService;
    
    private int fromPage = 1;
    private int toPage = 1;
    private boolean isSelected = false;
    private File sourceFile;
    
    public RangeCard(int rangeNumber) {
        this.rangeNumber = rangeNumber;
        this.thumbnailService = new PdfThumbnailService();
        
        getStyleClass().add("range-card");
        setSpacing(12);
        setPadding(new Insets(16));
        setAlignment(Pos.TOP_LEFT);
        setMinWidth(280);
        setPrefWidth(300);
        setMaxWidth(350);
        
        // Header with range label and remove button
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        rangeLabel = new Label("Range " + rangeNumber);
        rangeLabel.getStyleClass().add("range-card-title");
        HBox.setHgrow(rangeLabel, Priority.ALWAYS);
        
        removeButton = new Button();
        removeButton.setGraphic(Icons.create("trash", 16));
        removeButton.getStyleClass().addAll("icon-button", "danger-button");
        
        header.getChildren().addAll(rangeLabel, removeButton);
        
        // Thumbnail preview box with two tiles
        thumbnailBox = new HBox(12);
        thumbnailBox.setAlignment(Pos.CENTER);
        thumbnailBox.setPadding(new Insets(12));
        thumbnailBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 6px;");
        HBox.setHgrow(thumbnailBox, Priority.ALWAYS);
        
        // First page tile
        firstPageTile = createThumbnailTile();
        firstPageThumb = (ImageView) ((VBox) firstPageTile.getChildren().get(0)).getChildren().get(0);
        firstPageLabel = (Label) firstPageTile.getChildren().get(1);
        
        // Last page tile
        lastPageTile = createThumbnailTile();
        lastPageThumb = (ImageView) ((VBox) lastPageTile.getChildren().get(0)).getChildren().get(0);
        lastPageLabel = (Label) lastPageTile.getChildren().get(1);
        
        thumbnailBox.getChildren().addAll(firstPageTile, lastPageTile);
        
        // Range text
        rangeText = new Label("Pages 1–1");
        rangeText.getStyleClass().add("range-card-text");
        
        getChildren().addAll(header, thumbnailBox, rangeText);
        
        // Selection behavior
        setOnMouseClicked(e -> {
            // Selection handled by controller
        });
    }
    
    /**
     * Creates a thumbnail tile with ImageView and page label.
     */
    private VBox createThumbnailTile() {
        VBox tile = new VBox(6);
        tile.setAlignment(Pos.CENTER);
        tile.getStyleClass().add("thumb-tile");
        HBox.setHgrow(tile, Priority.ALWAYS);
        
        // Thumbnail container
        VBox thumbContainer = new VBox();
        thumbContainer.setAlignment(Pos.CENTER);
        thumbContainer.setMinSize(80, 106);
        thumbContainer.setPrefSize(80, 106);
        thumbContainer.setMaxSize(80, 106);
        thumbContainer.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                               "-fx-border-width: 1px; -fx-background-radius: 4px; " +
                               "-fx-border-radius: 4px;");
        
        ImageView imageView = new ImageView();
        imageView.setFitWidth(78);
        imageView.setFitHeight(104);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        
        thumbContainer.getChildren().add(imageView);
        
        // Page label
        Label pageLabel = new Label("Page 1");
        pageLabel.getStyleClass().add("thumb-label");
        pageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: 500;");
        
        tile.getChildren().addAll(thumbContainer, pageLabel);
        return tile;
    }
    
    public void setRange(int from, int to) {
        this.fromPage = from;
        this.toPage = to;
        updateRangeText();
        updatePageLabels();
        updateThumbnails();
    }
    
    public void setSourceFile(File file) {
        this.sourceFile = file;
        updateThumbnails();
    }
    
    /**
     * Updates the page labels under thumbnails.
     */
    private void updatePageLabels() {
        firstPageLabel.setText(String.format(LocaleManager.getString("split.pageLabel"), fromPage));
        lastPageLabel.setText(String.format(LocaleManager.getString("split.pageLabel"), toPage));
    }
    
    /**
     * Loads thumbnails asynchronously using PdfThumbnailService.
     */
    private void updateThumbnails() {
        if (sourceFile == null || !sourceFile.exists()) {
            return;
        }
        
        // Load first page thumbnail
        Task<Image> firstThumbTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                return thumbnailService.generateThumbnail(sourceFile, fromPage - 1, 80);
            }
        };
        
        firstThumbTask.setOnSucceeded(e -> {
            Image img = firstThumbTask.getValue();
            if (img != null) {
                firstPageThumb.setImage(img);
            }
        });
        
        Thread firstThread = new Thread(firstThumbTask);
        firstThread.setDaemon(true);
        firstThread.start();
        
        // Load last page thumbnail (only if different from first)
        if (toPage != fromPage) {
            Task<Image> lastThumbTask = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    return thumbnailService.generateThumbnail(sourceFile, toPage - 1, 80);
                }
            };
            
            lastThumbTask.setOnSucceeded(e -> {
                Image img = lastThumbTask.getValue();
                if (img != null) {
                    lastPageThumb.setImage(img);
                }
            });
            
            Thread lastThread = new Thread(lastThumbTask);
            lastThread.setDaemon(true);
            lastThread.start();
        } else {
            // Same page, use same image
            firstThumbTask.setOnSucceeded(e -> {
                Image img = firstThumbTask.getValue();
                if (img != null) {
                    firstPageThumb.setImage(img);
                    lastPageThumb.setImage(img);
                }
            });
        }
    }
    
    public int getFromPage() {
        return fromPage;
    }
    
    public int getToPage() {
        return toPage;
    }
    
    public int getRangeNumber() {
        return rangeNumber;
    }
    
    public Button getRemoveButton() {
        return removeButton;
    }
    
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (selected) {
            setStyle("-fx-border-color: #5b65ea; -fx-border-width: 2px; -fx-background-color: #f0f4ff;");
        } else {
            setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1px; -fx-background-color: white;");
        }
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    private void updateRangeText() {
        if (fromPage == toPage) {
            rangeText.setText("Page " + fromPage);
        } else {
            rangeText.setText("Pages " + fromPage + "–" + toPage);
        }
    }
}
