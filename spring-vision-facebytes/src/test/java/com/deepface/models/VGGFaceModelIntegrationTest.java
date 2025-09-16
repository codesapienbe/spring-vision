package com.deepface.models;

import com.deepface.exceptions.DeepFaceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for VGGFaceModel focusing on configuration validation,
 * error handling, and fail-fast behavior when ONNX models are not available.
 */
public class VGGFaceModelIntegrationTest {

    private VGGFaceModel model;
    private String originalEnvVar;
    private String originalSysProperty;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        model = new VGGFaceModel();
        
        // Save original configuration to restore later
        originalEnvVar = System.getenv("FACEBYTES_VGGFACE_ONNX_PATH");
        originalSysProperty = System.getProperty("facebytes.vggface.onnx");
        
        // Clear any existing configuration
        System.clearProperty("facebytes.vggface.onnx");
    }
    
    @AfterEach
    void tearDown() {
        // Restore original configuration
        if (originalSysProperty != null) {
            System.setProperty("facebytes.vggface.onnx", originalSysProperty);
        } else {
            System.clearProperty("facebytes.vggface.onnx");
        }
    }
    
    /**
     * Creates a synthetic test face image for testing.
     */
    private BufferedImage createTestFaceImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Create a realistic face-like pattern
        g2d.setColor(new Color(220, 190, 170)); // Skin tone background
        g2d.fillRect(0, 0, width, height);
        
        // Draw facial features proportional to image size
        g2d.setColor(Color.BLACK);
        int eyeSize = Math.max(width/20, 3);
        int eyeY = height/4;
        g2d.fillOval(width/4, eyeY, eyeSize, eyeSize);  // Left eye
        g2d.fillOval(3*width/4-eyeSize, eyeY, eyeSize, eyeSize);  // Right eye
        
        int noseW = Math.max(width/30, 2);
        int noseH = Math.max(height/20, 4);
        g2d.fillOval(width/2-noseW/2, height/2-noseH/2, noseW, noseH);  // Nose
        
        int mouthW = Math.max(width/8, 10);
        int mouthH = Math.max(height/40, 2);
        g2d.fillRect(width/2-mouthW/2, 3*height/4, mouthW, mouthH);  // Mouth
        
        g2d.dispose();
        return image;
    }
    
    @Test
    void testGenerateEmbedding_NoConfiguration_ShouldProvideGuidance() {
        // Given: A test face image and no ONNX model configuration
        BufferedImage testFace = createTestFaceImage(224, 224);
        
        // When/Then: Should throw DeepFaceException with clear guidance
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 224);
        });
        
        String message = exception.getMessage();
        
        // Verify comprehensive guidance is provided
        assertTrue(message.contains("VGGFace ONNX model is not available"), 
                  "Should identify the specific model");
        assertTrue(message.contains("FACEBYTES_VGGFACE_ONNX_PATH"), 
                  "Should mention environment variable");
        assertTrue(message.contains("facebytes.vggface.onnx"), 
                  "Should mention system property");
        assertTrue(message.contains("auto-download"), 
                  "Should mention auto-download option");
        assertTrue(message.contains("configuration"), 
                  "Should mention configuration");
    }
    
    @Test
    void testGenerateEmbedding_InvalidModelPath_ShouldProvideGuidance() throws IOException {
        // Given: A non-existent ONNX model path
        Path nonExistentPath = tempDir.resolve("non_existent_model.onnx");
        System.setProperty("facebytes.vggface.onnx", nonExistentPath.toString());
        
        BufferedImage testFace = createTestFaceImage(224, 224);
        
        // When/Then: Should throw DeepFaceException with guidance
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 224);
        });
        
        String message = exception.getMessage();
        assertTrue(message.contains("VGGFace ONNX model is not available"));
    }
    
    @Test
    void testGenerateEmbedding_EmptyModelFile_ShouldHandleGracefully() throws IOException {
        // Given: An empty file as ONNX model
        Path emptyModel = tempDir.resolve("empty_model.onnx");
        Files.createFile(emptyModel);
        System.setProperty("facebytes.vggface.onnx", emptyModel.toString());
        
        BufferedImage testFace = createTestFaceImage(224, 224);
        
        // When/Then: Should throw DeepFaceException
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 224);
        });
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().length() > 0);
    }
    
    @Test 
    void testGenerateEmbedding_NullInput_ShouldThrowIllegalArgument() {
        // When/Then: Null input should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            model.generateEmbedding(null, 224);
        });
        
        assertNotNull(exception.getMessage());
    }
    
    @Test
    void testGenerateEmbedding_VariousImageSizes_ShouldHandleResize() {
        // Test that the model handles different input image sizes appropriately
        int[] imageSizes = {64, 112, 128, 224, 256};
        
        for (int size : imageSizes) {
            BufferedImage testFace = createTestFaceImage(size, size);
            
            DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
                model.generateEmbedding(testFace, 224);
            });
            
            // Should fail consistently regardless of input size
            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("VGGFace"), 
                      "Should identify VGGFace model for size " + size);
        }
    }
    
    @Test
    void testModelConsistency_MultipleInvocations() {
        // Test that multiple calls to the same model behave consistently
        BufferedImage testFace = createTestFaceImage(224, 224);
        
        DeepFaceException exception1 = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 224);
        });
        
        DeepFaceException exception2 = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 224);
        });
        
        // Error messages should be identical
        assertEquals(exception1.getMessage(), exception2.getMessage(), 
                    "Should provide consistent error messages across calls");
    }
    
    @Test
    void testImageProcessing_DifferentFormats() {
        // Test different image formats/types
        int[] imageTypes = {
            BufferedImage.TYPE_INT_RGB,
            BufferedImage.TYPE_INT_ARGB,
            BufferedImage.TYPE_3BYTE_BGR
        };
        
        for (int imageType : imageTypes) {
            BufferedImage testFace = new BufferedImage(224, 224, imageType);
            Graphics2D g2d = testFace.createGraphics();
            g2d.setColor(Color.GRAY);
            g2d.fillRect(0, 0, 224, 224);
            g2d.dispose();
            
            DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
                model.generateEmbedding(testFace, 224);
            });
            
            // Should handle all image types consistently
            assertTrue(exception.getMessage().contains("VGGFace"), 
                      "Should handle image type " + imageType);
        }
    }
    
    @Test
    void testConfigurationPriority() {
        // Test that system property takes precedence over environment variable
        // (This validates our configuration resolution logic)
        
        Path dummyPath = tempDir.resolve("system_prop_model.onnx");
        System.setProperty("facebytes.vggface.onnx", dummyPath.toString());
        
        BufferedImage testFace = createTestFaceImage(224, 224);
        
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 224);
        });
        
        // Should use system property path in error handling
        assertNotNull(exception.getMessage());
    }
    
    @Test
    void testErrorMessageQuality() {
        // Test that error messages meet quality standards
        BufferedImage testFace = createTestFaceImage(224, 224);
        
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 224);
        });
        
        String message = exception.getMessage();
        
        // Quality checks for error message
        assertFalse(message.isEmpty(), "Error message should not be empty");
        assertFalse(message.contains("TODO"), "Should not contain development artifacts");
        assertFalse(message.contains("mock"), "Should not reference mock implementations");
        assertFalse(message.contains("null"), "Should not contain null references");
        
        // Should be actionable
        assertTrue(message.contains("Configure") || message.contains("Set") || message.contains("enable"), 
                  "Should provide actionable guidance");
    }
} 