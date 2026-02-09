package com.pdftoolkit.state;

/**
 * Singleton state store that holds application state across UI rebuilds.
 * This ensures that state (like selected files) persists across language switches,
 * theme changes, or navigation changes that rebuild views.
 * 
 * Thread-safe singleton pattern using Bill Pugh initialization-on-demand holder idiom.
 */
public class StateStore {
    
    private final CompressPdfState compressPdfState;
    
    private StateStore() {
        this.compressPdfState = new CompressPdfState();
    }
    
    /**
     * Holder class for lazy initialization (thread-safe without synchronization).
     */
    private static class Holder {
        private static final StateStore INSTANCE = new StateStore();
    }
    
    /**
     * Get the singleton instance (thread-safe, lazy initialization).
     */
    public static StateStore getInstance() {
        return Holder.INSTANCE;
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
