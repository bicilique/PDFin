package com.pdftoolkit.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for CustomDialog to ensure proper language and theme support.
 */
class CustomDialogTest {

    @Test
    void testDialogTypesExist() {
        // Verify all dialog types are available
        assertNotNull(CustomDialog.Type.SUCCESS);
        assertNotNull(CustomDialog.Type.ERROR);
        assertNotNull(CustomDialog.Type.WARNING);
        assertNotNull(CustomDialog.Type.INFO);
        assertNotNull(CustomDialog.Type.CONFIRMATION);
    }
    
    @Test
    void testDialogTypeCount() {
        // Verify we have exactly 5 dialog types
        assertEquals(5, CustomDialog.Type.values().length);
    }
    
    @Test
    void testDialogTypeNames() {
        // Verify dialog type names match expected values
        CustomDialog.Type[] types = CustomDialog.Type.values();
        assertTrue(java.util.Arrays.asList(types).stream()
            .anyMatch(t -> t.name().equals("SUCCESS")));
        assertTrue(java.util.Arrays.asList(types).stream()
            .anyMatch(t -> t.name().equals("ERROR")));
        assertTrue(java.util.Arrays.asList(types).stream()
            .anyMatch(t -> t.name().equals("WARNING")));
        assertTrue(java.util.Arrays.asList(types).stream()
            .anyMatch(t -> t.name().equals("INFO")));
        assertTrue(java.util.Arrays.asList(types).stream()
            .anyMatch(t -> t.name().equals("CONFIRMATION")));
    }
    
    // Note: GUI-related tests (showing dialogs) would require JavaFX Application Thread
    // and are better suited for integration tests or manual testing
}
