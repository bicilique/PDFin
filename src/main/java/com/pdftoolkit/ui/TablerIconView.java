package com.pdftoolkit.ui;

import javafx.beans.property.*;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SVG-based icon component that loads and renders Tabler Icons.
 * Uses actual SVG files with proper CSS theming support.
 * Icons inherit color from CSS via -fx-fill property.
 */
public class TablerIconView extends StackPane {
    
    private static final Map<String, List<String>> SVG_CACHE = new HashMap<>();
    private static final double DEFAULT_SIZE = 16.0;
    
    private final StringProperty iconName = new SimpleStringProperty();
    private final DoubleProperty iconSize = new SimpleDoubleProperty(DEFAULT_SIZE);
    private final List<SVGPath> pathNodes = new ArrayList<>();
    
    public TablerIconView() {
        this("", DEFAULT_SIZE);
    }
    
    public TablerIconView(String iconName) {
        this(iconName, DEFAULT_SIZE);
    }
    
    public TablerIconView(String iconName, double size) {
        getStyleClass().add("tabler-icon");
        setPickOnBounds(false); // Allow clicks to pass through transparent areas
        
        // Bind properties
        this.iconName.addListener((obs, old, newVal) -> updateIcon(newVal));
        this.iconSize.addListener((obs, old, newVal) -> updateSize(newVal.doubleValue()));
        
        // Set initial values
        setIconName(iconName);
        setIconSize(size);
    }
    
    private void updateIcon(String name) {
        // Clear existing paths
        getChildren().clear();
        pathNodes.clear();
        
        if (name == null || name.isEmpty()) {
            return;
        }
        
        // Load SVG paths
        List<String> paths = loadSvgPaths(name);
        if (paths == null || paths.isEmpty()) {
            System.err.println("⚠️ Failed to load icon: " + name);
            return;
        }
        
        // Create SVGPath nodes for each path
        for (String pathData : paths) {
            SVGPath svgPath = new SVGPath();
            svgPath.setContent(pathData);
            svgPath.getStyleClass().add("icon-svg-path");
            pathNodes.add(svgPath);
            getChildren().add(svgPath);
        }
        
        // Update size
        updateSize(iconSize.get());
    }
    
    private List<String> loadSvgPaths(String iconName) {
        // Check cache first
        if (SVG_CACHE.containsKey(iconName)) {
            return SVG_CACHE.get(iconName);
        }
        
        // Load from SVG file
        String resourcePath = "/icons/" + iconName + ".svg";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("⚠️ SVG file not found: " + resourcePath);
                return null;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            
            List<String> paths = new ArrayList<>();
            NodeList pathElements = doc.getElementsByTagName("path");
            
            for (int i = 0; i < pathElements.getLength(); i++) {
                Element pathElement = (Element) pathElements.item(i);
                String d = pathElement.getAttribute("d");
                if (d != null && !d.isEmpty()) {
                    paths.add(d);
                }
            }
            
            // Also check for other SVG elements
            NodeList circleElements = doc.getElementsByTagName("circle");
            for (int i = 0; i < circleElements.getLength(); i++) {
                Element circle = (Element) circleElements.item(i);
                String cx = circle.getAttribute("cx");
                String cy = circle.getAttribute("cy");
                String r = circle.getAttribute("r");
                if (cx != null && !cx.isEmpty() && cy != null && !cy.isEmpty() && r != null && !r.isEmpty()) {
                    try {
                        double cxVal = Double.parseDouble(cx);
                        double cyVal = Double.parseDouble(cy);
                        double rVal = Double.parseDouble(r);
                        // Convert circle to path using proper arc commands
                        String circlePath = String.format("M %f,%f m -%f,0 a %f,%f 0 1,0 %f,0 a %f,%f 0 1,0 -%f,0",
                                cxVal, cyVal, rVal, rVal, rVal, rVal * 2, rVal, rVal, rVal * 2);
                        paths.add(circlePath);
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ Failed to parse circle attributes: " + e.getMessage());
                    }
                }
            }
            
            NodeList rectElements = doc.getElementsByTagName("rect");
            for (int i = 0; i < rectElements.getLength(); i++) {
                Element rect = (Element) rectElements.item(i);
                String x = rect.getAttribute("x");
                String y = rect.getAttribute("y");
                String width = rect.getAttribute("width");
                String height = rect.getAttribute("height");
                String rx = rect.getAttribute("rx");
                
                if (x != null && y != null && width != null && height != null) {
                    double x1 = Double.parseDouble(x.isEmpty() ? "0" : x);
                    double y1 = Double.parseDouble(y.isEmpty() ? "0" : y);
                    double w = Double.parseDouble(width);
                    double h = Double.parseDouble(height);
                    double radius = rx != null && !rx.isEmpty() ? Double.parseDouble(rx) : 0;
                    
                    String rectPath;
                    if (radius > 0) {
                        // Rounded rectangle
                        rectPath = String.format("M %f,%f L %f,%f Q %f,%f %f,%f L %f,%f Q %f,%f %f,%f L %f,%f Q %f,%f %f,%f L %f,%f Q %f,%f %f,%f Z",
                                x1 + radius, y1,
                                x1 + w - radius, y1, x1 + w, y1, x1 + w, y1 + radius,
                                x1 + w, y1 + h - radius, x1 + w, y1 + h, x1 + w - radius, y1 + h,
                                x1 + radius, y1 + h, x1, y1 + h, x1, y1 + h - radius,
                                x1, y1 + radius, x1, y1, x1 + radius, y1);
                    } else {
                        // Simple rectangle
                        rectPath = String.format("M %f,%f L %f,%f L %f,%f L %f,%f Z",
                                x1, y1, x1 + w, y1, x1 + w, y1 + h, x1, y1 + h);
                    }
                    paths.add(rectPath);
                }
            }
            
            if (!paths.isEmpty()) {
                SVG_CACHE.put(iconName, paths);
                System.out.println("✅ Loaded SVG icon: " + iconName + " (" + paths.size() + " paths)");
            }
            
            return paths;
            
        } catch (Exception e) {
            System.err.println("⚠️ Error parsing SVG file " + resourcePath + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private void updateSize(double size) {
        // The original SVG viewBox is 24x24
        // Scale factor to resize from 24x24 to desired size
        double scale = size / 24.0;
        
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        
        for (SVGPath path : pathNodes) {
            path.setScaleX(scale);
            path.setScaleY(scale);
            path.setStrokeWidth(2.0 / scale); // Keep stroke width consistent
        }
        
        // Apply inline style for size (CSS can override)
        setStyle(String.format("-fx-min-width: %fpx; -fx-pref-width: %fpx; -fx-max-width: %fpx; " +
                               "-fx-min-height: %fpx; -fx-pref-height: %fpx; -fx-max-height: %fpx;",
                size, size, size, size, size, size));
    }
    
    // Property accessors
    public String getIconName() {
        return iconName.get();
    }
    
    public void setIconName(String iconName) {
        this.iconName.set(iconName);
    }
    
    public StringProperty iconNameProperty() {
        return iconName;
    }
    
    public double getIconSize() {
        return iconSize.get();
    }
    
    public void setIconSize(double size) {
        this.iconSize.set(size);
    }
    
    public DoubleProperty iconSizeProperty() {
        return iconSize;
    }
}

