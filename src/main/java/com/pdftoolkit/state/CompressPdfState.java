package com.pdftoolkit.state;

import com.pdftoolkit.models.PdfItem;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;

/**
 * Persistent state for Compress PDF feature.
 * Holds selected files, compression settings, and output configuration.
 * This state survives across language switches and UI rebuilds.
 */
public class CompressPdfState {
    
    private final ObservableList<PdfItem> items;
    private final ObjectProperty<Path> outputFolder;
    private final StringProperty outputFileName;
    private final ObjectProperty<CompressionLevel> compressionLevel;
    private final BooleanProperty keepBestQuality;
    
    public CompressPdfState() {
        this.items = FXCollections.observableArrayList();
        this.outputFolder = new SimpleObjectProperty<>(null);
        this.outputFileName = new SimpleStringProperty("compressed.pdf");
        this.compressionLevel = new SimpleObjectProperty<>(CompressionLevel.RECOMMENDED);
        this.keepBestQuality = new SimpleBooleanProperty(false);
    }
    
    // Items (selected PDFs)
    public ObservableList<PdfItem> getItems() {
        return items;
    }
    
    // Output folder
    public ObjectProperty<Path> outputFolderProperty() {
        return outputFolder;
    }
    
    public Path getOutputFolder() {
        return outputFolder.get();
    }
    
    public void setOutputFolder(Path folder) {
        outputFolder.set(folder);
    }
    
    // Output filename
    public StringProperty outputFileNameProperty() {
        return outputFileName;
    }
    
    public String getOutputFileName() {
        return outputFileName.get();
    }
    
    public void setOutputFileName(String name) {
        outputFileName.set(name);
    }
    
    // Compression level
    public ObjectProperty<CompressionLevel> compressionLevelProperty() {
        return compressionLevel;
    }
    
    public CompressionLevel getCompressionLevel() {
        return compressionLevel.get();
    }
    
    public void setCompressionLevel(CompressionLevel level) {
        compressionLevel.set(level);
    }
    
    // Keep best quality flag
    public BooleanProperty keepBestQualityProperty() {
        return keepBestQuality;
    }
    
    public boolean isKeepBestQuality() {
        return keepBestQuality.get();
    }
    
    public void setKeepBestQuality(boolean keep) {
        keepBestQuality.set(keep);
    }
    
    /**
     * Reset state to initial values (but don't clear the items list to preserve added files).
     * Call clearItems() separately if you want to clear files.
     */
    public void reset() {
        items.clear();
        outputFileName.set("compressed.pdf");
        compressionLevel.set(CompressionLevel.RECOMMENDED);
        keepBestQuality.set(false);
    }
    
    /**
     * Check if this item is already added (by normalized absolute path).
     */
    public boolean containsItem(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        return items.stream()
                .anyMatch(item -> item.getPath().toAbsolutePath().normalize().equals(normalized));
    }
    
    /**
     * Add a new PDF item if not already present.
     * @return true if added, false if duplicate
     */
    public boolean addItem(PdfItem item) {
        if (containsItem(item.getPath())) {
            return false;
        }
        items.add(item);
        return true;
    }
    
    /**
     * Remove an item from the list.
     */
    public void removeItem(PdfItem item) {
        items.remove(item);
    }
    
    /**
     * Clear all items.
     */
    public void clearItems() {
        items.clear();
    }
    
    /**
     * Check if any items are loading.
     */
    public boolean isAnyLoading() {
        return items.stream().anyMatch(PdfItem::isLoading);
    }
    
    /**
     * Check if any items have errors.
     */
    public boolean hasErrors() {
        return items.stream().anyMatch(PdfItem::hasError);
    }
    
    /**
     * Check if state is valid for compression.
     */
    public boolean isValid() {
        return !items.isEmpty() 
                && !isAnyLoading() 
                && !hasErrors()
                && outputFolder.get() != null
                && outputFileName.get() != null 
                && !outputFileName.get().trim().isEmpty();
    }
}
