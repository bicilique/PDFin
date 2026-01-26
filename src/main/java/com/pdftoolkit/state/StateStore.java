package com.pdftoolkit.state;

/**
 * Singleton state store that holds application state across UI rebuilds.
 * This ensures that state (like selected files) persists across language switches,
 * theme changes, or navigation changes that rebuild views.
 * 
 * Thread-safe singleton pattern.
 */
public class StateStore {
    
    private static volatile StateStore instance;
    
    private final CompressPdfState compressPdfState;
    
    private StateStore() {
        this.compressPdfState = new CompressPdfState();
    }
    
    /**
     * Get the singleton instance (thread-safe double-checked locking).
     */
    public static StateStore getInstance() {
        if (instance == null) {
            synchronized (StateStore.class) {
                if (instance == null) {
                    instance = new StateStore();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get the persistent compress PDF state.
     * This state survives across view rebuilds.
     */
    public CompressPdfState getCompressPdfState() {
        return compressPdfState;
    }
    
    /**
     * Reset the compress PDF state (e.g., after successful compression or user reset).
     */
    public void resetCompressPdfState() {
        compressPdfState.reset();
    }
}
