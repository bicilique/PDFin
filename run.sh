#!/bin/bash

# PDF Toolkit Launcher Script
# This script runs the PDF Toolkit application with proper JavaFX configuration

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR_FILE="$SCRIPT_DIR/target/pdf-toolkit-1.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run 'mvn clean package' first"
    exit 1
fi

# Run with explicit classpath (not as a modular application)
exec java -cp "$JAR_FILE" com.pdftoolkit.MainApp
