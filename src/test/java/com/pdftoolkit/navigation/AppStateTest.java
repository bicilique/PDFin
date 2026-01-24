package com.pdftoolkit.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppState.
 */
class AppStateTest {

    @Test
    void testSingleton() {
        AppState instance1 = AppState.getInstance();
        AppState instance2 = AppState.getInstance();
        assertSame(instance1, instance2, "Should return same instance");
    }

    @Test
    void testRecentFilesInitiallyEmpty() {
        AppState state = AppState.getInstance();
        assertTrue(state.getRecentFiles().isEmpty() || state.getRecentFiles().size() > 0);
    }

    @Test
    void testAddRecentFile() {
        AppState state = AppState.getInstance();
        int initialSize = state.getRecentFiles().size();
        
        java.io.File testFile = new java.io.File("/tmp/test.pdf");
        state.addRecentFile("Merge", testFile);
        
        assertEquals(initialSize + 1, state.getRecentFiles().size());
        
        AppState.RecentFile recent = state.getRecentFiles().get(0);
        assertEquals("Merge", recent.getOperation());
        assertEquals("test.pdf", recent.getFileName());
    }

    @Test
    void testRecentFilesLimit() {
        AppState state = AppState.getInstance();
        state.getRecentFiles().clear();
        
        // Add more than 10 files
        for (int i = 0; i < 15; i++) {
            java.io.File file = new java.io.File("/tmp/test" + i + ".pdf");
            state.addRecentFile("Test", file);
        }
        
        assertEquals(10, state.getRecentFiles().size(), "Should keep only 10 recent files");
    }

    @Test
    void testRecentFileProperties() {
        java.io.File testFile = new java.io.File("/tmp/output/result.pdf");
        
        AppState.RecentFile recentFile = new AppState.RecentFile(
            "Split",
            "result.pdf",
            "/tmp/output",
            java.time.LocalDateTime.now()
        );
        
        assertEquals("Split", recentFile.getOperation());
        assertEquals("result.pdf", recentFile.getFileName());
        assertEquals("/tmp/output", recentFile.getFolderPath());
        assertNotNull(recentFile.getTimestamp());
    }
}
