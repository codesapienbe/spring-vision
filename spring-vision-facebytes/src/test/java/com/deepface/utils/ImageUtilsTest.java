package com.deepface.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Unit tests for ImageUtils utility class.
 * Tests image loading, saving, and resizing functionality.
 */
@DisplayName("Image Utils Tests")
class ImageUtilsTest {

    @TempDir
    Path tempDir;
    
    private File testImageFile;
    private BufferedImage testImage;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test image
        testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = testImage.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, 100, 100);
        g2d.setColor(Color.BLUE);
        g2d.drawRect(10, 10, 80, 80);
        g2d.dispose();
        
        // Save test image to temp directory
        testImageFile = tempDir.resolve("test-image.png").toFile();
        ImageIO.write(testImage, "PNG", testImageFile);
    }

    @Test
    @DisplayName("Load image from file path should work correctly")
    void loadImage_FromFilePath_WorksCorrectly() throws IOException {
        BufferedImage loaded = ImageUtils.loadImage(testImageFile.getAbsolutePath());
        
        assertNotNull(loaded, "Loaded image should not be null");
        assertEquals(100, loaded.getWidth(), "Loaded image width should match original");
        assertEquals(100, loaded.getHeight(), "Loaded image height should match original");
        assertEquals(BufferedImage.TYPE_INT_RGB, loaded.getType(), "Loaded image type should match original");
    }

    @Test
    @DisplayName("Load image from file path should throw exception for non-existent file")
    void loadImage_NonExistentFile_ThrowsException() {
        String nonExistentPath = tempDir.resolve("non-existent.png").toString();
        
        assertThrows(IOException.class, () -> 
            ImageUtils.loadImage(nonExistentPath), 
            "Should throw IOException for non-existent file"
        );
    }

    @Test
    @DisplayName("Load image from file path should throw exception for null path")
    void loadImage_NullPath_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.loadImage((String) null), 
            "Should throw IllegalArgumentException for null path"
        );
    }

    @Test
    @DisplayName("Load image from file path should throw exception for blank path")
    void loadImage_BlankPath_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.loadImage("   "), 
            "Should throw IllegalArgumentException for blank path"
        );
    }

    @Test
    @DisplayName("Load image from byte array should work correctly")
    void loadImage_FromByteArray_WorksCorrectly() throws IOException {
        // Convert test image to byte array
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(testImage, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        
        BufferedImage loaded = ImageUtils.loadImage(imageBytes);
        
        assertNotNull(loaded, "Loaded image from bytes should not be null");
        assertEquals(100, loaded.getWidth(), "Loaded image width should match original");
        assertEquals(100, loaded.getHeight(), "Loaded image height should match original");
    }

    @Test
    @DisplayName("Load image from byte array should throw exception for null bytes")
    void loadImage_NullBytes_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.loadImage((byte[]) null), 
            "Should throw IllegalArgumentException for null bytes"
        );
    }

    @Test
    @DisplayName("Load image from input stream should work correctly")
    void loadImage_FromInputStream_WorksCorrectly() throws IOException {
        // Convert test image to input stream
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(testImage, "PNG", baos);
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        
        BufferedImage loaded = ImageUtils.loadImage(bais);
        
        assertNotNull(loaded, "Loaded image from stream should not be null");
        assertEquals(100, loaded.getWidth(), "Loaded image width should match original");
        assertEquals(100, loaded.getHeight(), "Loaded image height should match original");
    }

    @Test
    @DisplayName("Load image from input stream should throw exception for null stream")
    void loadImage_NullInputStream_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.loadImage((java.io.InputStream) null), 
            "Should throw IllegalArgumentException for null input stream"
        );
    }

    @Test
    @DisplayName("Save image should work correctly")
    void saveImage_ShouldWorkCorrectly() throws IOException {
        File outputFile = tempDir.resolve("output-image.png").toFile();
        
        ImageUtils.saveImage(testImage, outputFile.getAbsolutePath());
        
        assertTrue(outputFile.exists(), "Output file should exist");
        assertTrue(outputFile.length() > 0, "Output file should not be empty");
        
        // Verify the saved image can be loaded back
        BufferedImage saved = ImageIO.read(outputFile);
        assertNotNull(saved, "Saved image should be loadable");
        assertEquals(100, saved.getWidth(), "Saved image width should match original");
        assertEquals(100, saved.getHeight(), "Saved image height should match original");
    }

    @Test
    @DisplayName("Save image should throw exception for null image")
    void saveImage_NullImage_ThrowsException() {
        File outputFile = tempDir.resolve("output-image.png").toFile();
        
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.saveImage(null, outputFile.getAbsolutePath()), 
            "Should throw IllegalArgumentException for null image"
        );
    }

    @Test
    @DisplayName("Save image should throw exception for null path")
    void saveImage_NullPath_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.saveImage(testImage, null), 
            "Should throw IllegalArgumentException for null path"
        );
    }

    @Test
    @DisplayName("Save image should throw exception for blank path")
    void saveImage_BlankPath_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.saveImage(testImage, "   "), 
            "Should throw IllegalArgumentException for blank path"
        );
    }

    @Test
    @DisplayName("Resize image should work correctly")
    void resizeImage_ShouldWorkCorrectly() {
        BufferedImage resized = ImageUtils.resizeImage(testImage, 50, 75);
        
        assertNotNull(resized, "Resized image should not be null");
        assertEquals(50, resized.getWidth(), "Resized image width should match requested width");
        assertEquals(75, resized.getHeight(), "Resized image height should match requested height");
        assertEquals(BufferedImage.TYPE_INT_RGB, resized.getType(), "Resized image type should be preserved");
    }

    @Test
    @DisplayName("Resize image should throw exception for null image")
    void resizeImage_NullImage_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.resizeImage(null, 50, 75), 
            "Should throw IllegalArgumentException for null image"
        );
    }

    @Test
    @DisplayName("Resize image should throw exception for invalid width")
    void resizeImage_InvalidWidth_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.resizeImage(testImage, 0, 75), 
            "Should throw IllegalArgumentException for zero width"
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.resizeImage(testImage, -10, 75), 
            "Should throw IllegalArgumentException for negative width"
        );
    }

    @Test
    @DisplayName("Resize image should throw exception for invalid height")
    void resizeImage_InvalidHeight_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.resizeImage(testImage, 50, 0), 
            "Should throw IllegalArgumentException for zero height"
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            ImageUtils.resizeImage(testImage, 50, -10), 
            "Should throw IllegalArgumentException for negative height"
        );
    }

    @Test
    @DisplayName("Resize image should handle extreme dimensions")
    void resizeImage_ExtremeDimensions_WorksCorrectly() {
        // Test with very small dimensions
        BufferedImage small = ImageUtils.resizeImage(testImage, 1, 1);
        assertEquals(1, small.getWidth(), "Very small width should work");
        assertEquals(1, small.getHeight(), "Very small height should work");
        
        // Test with very large dimensions
        BufferedImage large = ImageUtils.resizeImage(testImage, 1000, 1000);
        assertEquals(1000, large.getWidth(), "Very large width should work");
        assertEquals(1000, large.getHeight(), "Very large height should work");
    }

    @Test
    @DisplayName("Image format inference should work correctly")
    void formatInference_ShouldWorkCorrectly() throws IOException {
        // Test PNG format
        File pngFile = tempDir.resolve("test.png").toFile();
        ImageUtils.saveImage(testImage, pngFile.getAbsolutePath());
        assertTrue(pngFile.exists(), "PNG file should be created");
        
        // Test JPG format
        File jpgFile = tempDir.resolve("test.jpg").toFile();
        ImageUtils.saveImage(testImage, jpgFile.getAbsolutePath());
        assertTrue(jpgFile.exists(), "JPG file should be created");
        
        // Test with no extension (should default to PNG)
        File noExtFile = tempDir.resolve("test").toFile();
        ImageUtils.saveImage(testImage, noExtFile.getAbsolutePath());
        assertTrue(noExtFile.exists(), "File with no extension should be created");
    }
} 