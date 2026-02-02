package com.pdftoolkit;

/**
 * Launcher class for running the JavaFX application from a shaded JAR.
 * This class does not extend Application, which is required for proper
 * JavaFX initialization when running from an uber JAR.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
