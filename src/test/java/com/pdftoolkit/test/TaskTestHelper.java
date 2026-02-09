package com.pdftoolkit.test;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

/**
 * Test utility for running JavaFX tasks in unit tests.
 * Initializes JavaFX toolkit and executes tasks synchronously.
 */
public class TaskTestHelper {
    
    private static boolean toolkitInitialized = false;
    
    /**
     * Initializes the JavaFX Platform if not already initialized.
     * This is required for Task methods that use Platform.runLater().
     */
    private static void initToolkit() {
        if (!toolkitInitialized) {
            // Start JavaFX toolkit in a background thread
            CountDownLatch latch = new CountDownLatch(1);
            new Thread(() -> {
                try {
                    Platform.startup(() -> {});
                } catch (IllegalStateException e) {
                    // Platform already started
                }
                latch.countDown();
            }).start();
            
            try {
                latch.await();
                toolkitInitialized = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to initialize JavaFX toolkit", e);
            }
        }
    }
    
    /**
     * Execute a JavaFX Task synchronously for testing purposes.
     * Uses reflection to call the protected call() method.
     * Initializes JavaFX toolkit if needed for updateMessage/updateProgress calls.
     */
    @SuppressWarnings("unchecked")
    public static <V> V executeTask(Task<V> task) throws Exception {
        // Initialize toolkit before executing task
        initToolkit();
        
        Method callMethod = Task.class.getDeclaredMethod("call");
        callMethod.setAccessible(true);
        return (V) callMethod.invoke(task);
    }
}
