package com.pdftoolkit.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Modern iOS-style switch toggle control.
 * Alternative to CheckBox with better visual design.
 */
public class SwitchControl extends HBox {
    
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Label textLabel = new Label();
    private final StackPane switchTrack = new StackPane();
    private final Region thumb = new Region();
    
    public SwitchControl() {
        this("");
    }
    
    public SwitchControl(String text) {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        
        // Setup label
        textLabel.setText(text);
        textLabel.getStyleClass().add("switch-label");
        
        // Setup switch track (background pill)
        switchTrack.getStyleClass().add("switch-toggle");
        switchTrack.setMinSize(44, 24);
        switchTrack.setPrefSize(44, 24);
        switchTrack.setMaxSize(44, 24);
        switchTrack.setCursor(javafx.scene.Cursor.HAND);
        
        // Setup thumb (white circle)
        thumb.getStyleClass().add("thumb");
        thumb.setMinSize(20, 20);
        thumb.setPrefSize(20, 20);
        thumb.setMaxSize(20, 20);
        thumb.setStyle("-fx-background-color: white; -fx-background-radius: 10px; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);");
        
        switchTrack.getChildren().add(thumb);
        StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
        
        // Add components
        getChildren().addAll(switchTrack, textLabel);
        
        // Bind selected property to visual state
        selected.addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                switchTrack.pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("selected"), true);
                StackPane.setAlignment(thumb, Pos.CENTER_RIGHT);
            } else {
                switchTrack.pseudoClassStateChanged(
                    javafx.css.PseudoClass.getPseudoClass("selected"), false);
                StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
            }
        });
        
        // Click handler
        switchTrack.setOnMouseClicked(e -> setSelected(!isSelected()));
        setOnMouseClicked(e -> setSelected(!isSelected()));
        
        // Keyboard accessibility
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("SPACE") || e.getCode().toString().equals("ENTER")) {
                setSelected(!isSelected());
                e.consume();
            }
        });
    }
    
    // Property methods
    public boolean isSelected() {
        return selected.get();
    }
    
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
    
    public BooleanProperty selectedProperty() {
        return selected;
    }
    
    public String getText() {
        return textLabel.getText();
    }
    
    public void setText(String text) {
        textLabel.setText(text);
    }
    
    public Label getLabel() {
        return textLabel;
    }
}
