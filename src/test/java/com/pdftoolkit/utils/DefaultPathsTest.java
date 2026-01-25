package com.pdftoolkit.utils;

import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultPaths utility class.
 */
class DefaultPathsTest {

    @BeforeEach
    void setUp() {
        // Reset cache before each test
        DefaultPaths.resetCache();
    }

    @Test
    @DisplayName("Should return non-null output directory")
    void testGetAppOutputDir_NotNull() {
        File outputDir = DefaultPaths.getAppOutputDir();
        assertNotNull(outputDir, "Output directory should not be null");
    }

    @Test
    @DisplayName("Should return directory under user home")
    void testGetAppOutputDir_UnderUserHome() {
        File outputDir = DefaultPaths.getAppOutputDir();
        String userHome = System.getProperty("user.home");
        assertTrue(outputDir.getAbsolutePath().startsWith(userHome),
                  "Output directory should be under user home");
    }

    @Test
    @DisplayName("Should create directory named PDFin")
    void testGetAppOutputDir_NamedPDFin() {
        File outputDir = DefaultPaths.getAppOutputDir();
        assertEquals("PDFin", outputDir.getName(),
                    "Directory should be named PDFin");
    }

    @Test
    @DisplayName("Should create directory if it doesn't exist")
    void testGetAppOutputDir_CreatesDirectory() {
        File outputDir = DefaultPaths.getAppOutputDir();
        assertTrue(outputDir.exists(), "Directory should exist");
        assertTrue(outputDir.isDirectory(), "Should be a directory");
    }

    @Test
    @DisplayName("Should return same instance on multiple calls")
    void testGetAppOutputDir_CachesSingleInstance() {
        File dir1 = DefaultPaths.getAppOutputDir();
        File dir2 = DefaultPaths.getAppOutputDir();
        assertSame(dir1, dir2, "Should return cached instance");
    }

    @Test
    @DisplayName("Should return absolute path as string")
    void testGetAppOutputPath_ReturnsAbsolutePath() {
        String path = DefaultPaths.getAppOutputPath();
        assertNotNull(path);
        assertTrue(path.contains("PDFin"), "Path should contain PDFin");
        assertFalse(path.isEmpty(), "Path should not be empty");
    }

    @Test
    @DisplayName("Should validate directory exists and is writable")
    void testIsAppOutputDirValid() {
        DefaultPaths.getAppOutputDir(); // Ensure it's created
        assertTrue(DefaultPaths.isAppOutputDirValid(),
                  "Valid directory should return true");
    }

    @Test
    @DisplayName("Should reset cache when requested")
    void testResetCache() {
        File dir1 = DefaultPaths.getAppOutputDir();
        DefaultPaths.resetCache();
        File dir2 = DefaultPaths.getAppOutputDir();
        
        // They should have same path but be different instances
        assertNotSame(dir1, dir2, "Should create new instance after reset");
        assertEquals(dir1.getAbsolutePath(), dir2.getAbsolutePath(),
                    "Paths should still be the same");
    }
}
