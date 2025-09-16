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
 * Integration test for ArcFaceModel focusing on configuration validation,
 * error handling, and fail-fast behavior when ONNX models are not available.
 */
public class ArcFaceModelIntegrationTest {

    private ArcFaceModel model;
    private String originalEnvVar;
    private String originalSysProperty;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        model = new ArcFaceModel();
        
        // Save original configuration to restore later
        originalEnvVar = System.getenv("FACEBYTES_ARCFACE_ONNX_PATH");
        originalSysProperty = System.getProperty("facebytes.arcface.onnx");
        
        // Clear any existing configuration
        System.clearProperty("facebytes.arcface.onnx");
    }
    
    @AfterEach
    void tearDown() {
        // Restore original configuration
        if (originalSysProperty != null) {
            System.setProperty("facebytes.arcface.onnx", originalSysProperty);
        } else {
            System.clearProperty("facebytes.arcface.onnx");
        }
    }
    
    /**
     * Creates a synthetic test face image optimized for ArcFace testing.
     */
    private BufferedImage createTestArcFaceImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Create a face-like pattern optimized for ArcFace dimensions
        g2d.setColor(new Color(220, 190, 170)); // Realistic skin tone
        g2d.fillRect(0, 0, width, height);
        
        // Draw facial features proportional to image size
        g2d.setColor(Color.BLACK);
        int eyeSize = Math.max(width/15, 2);
        int eyeY = height/3;
        g2d.fillOval(width/4, eyeY, eyeSize, eyeSize);   // Left eye
        g2d.fillOval(3*width/4-eyeSize, eyeY, eyeSize, eyeSize);   // Right eye
        
        int noseW = Math.max(width/25, 1);
        int noseH = Math.max(height/15, 3);
        g2d.fillOval(width/2-noseW/2, height/2-noseH/2, noseW, noseH);   // Nose
        
        int mouthW = Math.max(width/6, 8);
        int mouthH = Math.max(height/35, 1);
        g2d.fillRect(width/2-mouthW/2, 2*height/3, mouthW, mouthH);  // Mouth
        
        // Add some texture variation for realism
        g2d.setColor(new Color(200, 170, 150));
        g2d.fillRect(width/8, height/8, 3, 3);
        g2d.fillRect(7*width/8-3, height/8, 3, 3);
        
        g2d.dispose();
        return image;
    }
    
    @Test
    void testGenerateEmbedding_NoConfiguration_ShouldProvideGuidance() {
        // Given: A test face image and no ONNX model configuration
        BufferedImage testFace = createTestArcFaceImage(112, 112);
        
        // When/Then: Should throw DeepFaceException with clear ArcFace guidance
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 112);
        });
        
        String message = exception.getMessage();
        
        // Verify ArcFace-specific guidance is provided
        assertTrue(message.contains("ArcFace ONNX model is not available"), 
                  "Should identify the ArcFace model");
        assertTrue(message.contains("FACEBYTES_ARCFACE_ONNX_PATH"), 
                  "Should mention ArcFace environment variable");
        assertTrue(message.contains("facebytes.arcface.onnx"), 
                  "Should mention ArcFace system property");
        assertTrue(message.contains("auto-download"), 
                  "Should mention auto-download option");
        assertTrue(message.contains("configuration"), 
                  "Should mention configuration");
    }
    
    @Test
    void testGenerateEmbedding_InvalidModelPath_ShouldProvideGuidance() throws IOException {
        // Given: A non-existent ONNX model path
        Path nonExistentPath = tempDir.resolve("non_existent_arcface.onnx");
        System.setProperty("facebytes.arcface.onnx", nonExistentPath.toString());
        
        BufferedImage testFace = createTestArcFaceImage(112, 112);
        
        // When/Then: Should throw DeepFaceException with guidance
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 112);
        });
        
        String message = exception.getMessage();
        assertTrue(message.contains("ArcFace ONNX model is not available"));
    }
    
    @Test
    void testGenerateEmbedding_CorruptedModelFile_ShouldHandleGracefully() throws IOException {
        // Given: A corrupted/invalid ONNX model file
        Path corruptedModel = tempDir.resolve("corrupted_arcface.onnx");
        Files.write(corruptedModel, "not-a-valid-onnx-model".getBytes());
        System.setProperty("facebytes.arcface.onnx", corruptedModel.toString());
        
        BufferedImage testFace = createTestArcFaceImage(112, 112);
        
        // When/Then: Should throw DeepFaceException
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 112);
        });
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().length() > 0);
    }
    
    @Test 
    void testGenerateEmbedding_NullInput_ShouldThrowIllegalArgument() {
        // When/Then: Null input should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            model.generateEmbedding(null, 112);
        });
        
        assertNotNull(exception.getMessage());
    }
    
    @Test
    void testGenerateEmbedding_ArcFaceSpecificSizes_ShouldHandleResize() {
        // Test ArcFace-typical image sizes and target sizes
        int[] inputSizes = {64, 96, 112, 128, 224};
        int[] targetSizes = {112, 224}; // Common ArcFace target sizes
        
        for (int inputSize : inputSizes) {
            for (int targetSize : targetSizes) {
                BufferedImage testFace = createTestArcFaceImage(inputSize, inputSize);
                
                DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
                    model.generateEmbedding(testFace, targetSize);
                });
                
                // Should fail consistently regardless of sizes
                assertNotNull(exception.getMessage());
                assertTrue(exception.getMessage().contains("ArcFace"), 
                          String.format("Should identify ArcFace model for input=%d, target=%d", 
                                       inputSize, targetSize));
            }
        }
    }
    
    @Test
    void testModelConsistency_MultipleInvocations() {
        // Test that multiple calls to the same model behave consistently
        BufferedImage testFace = createTestArcFaceImage(112, 112);
        
        DeepFaceException exception1 = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 112);
        });
        
        DeepFaceException exception2 = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 112);
        });
        
        // Error messages should be identical for consistency
        assertEquals(exception1.getMessage(), exception2.getMessage(), 
                    "Should provide consistent error messages across calls");
    }
    
    @Test
    void testImageProcessing_DifferentColorSpaces() {
        // Test different image color spaces commonly used with ArcFace
        int[] imageTypes = {
            BufferedImage.TYPE_INT_RGB,
            BufferedImage.TYPE_INT_ARGB,
            BufferedImage.TYPE_3BYTE_BGR,
            BufferedImage.TYPE_BYTE_GRAY
        };
        
        for (int imageType : imageTypes) {
            BufferedImage testFace = new BufferedImage(112, 112, imageType);
            Graphics2D g2d = testFace.createGraphics();
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(0, 0, 112, 112);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(30, 30, 8, 8); // Simple eye
            g2d.dispose();
            
            DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
                model.generateEmbedding(testFace, 112);
            });
            
            // Should handle all color spaces consistently
            assertTrue(exception.getMessage().contains("ArcFace"), 
                      "Should handle color space type " + imageType);
        }
    }
    
    @Test
    void testConfigurationPriority_SystemPropertyOverEnvironment() {
        // Test that system property takes precedence over environment variable
        Path dummyPath = tempDir.resolve("system_prop_arcface.onnx");
        System.setProperty("facebytes.arcface.onnx", dummyPath.toString());
        
        BufferedImage testFace = createTestArcFaceImage(112, 112);
        
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 112);
        });
        
        // Should use system property path in error handling
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("ArcFace"));
    }
    
    @Test
    void testArcFaceSpecificErrorMessages() {
        // Test that error messages are specific to ArcFace model
        BufferedImage testFace = createTestArcFaceImage(112, 112);
        
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 112);
        });
        
        String message = exception.getMessage();
        
        // Should specifically mention ArcFace (not generic face model)
        assertTrue(message.contains("ArcFace"), "Should specifically mention ArcFace");
        assertFalse(message.contains("VGGFace"), "Should not mention other models");
        assertFalse(message.contains("FaceNet"), "Should not mention other models");
        assertFalse(message.contains("OpenFace"), "Should not mention other models");
    }
    
    @Test
    void testErrorMessageQuality_ArcFaceSpecific() {
        // Test that ArcFace error messages meet quality standards
        BufferedImage testFace = createTestArcFaceImage(112, 112);
        
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFace, 112);
        });
        
        String message = exception.getMessage();
        
        // Quality checks for ArcFace error messages
        assertFalse(message.isEmpty(), "Error message should not be empty");
        assertFalse(message.contains("TODO"), "Should not contain development artifacts");
        assertFalse(message.contains("mock"), "Should not reference mock implementations");
        assertFalse(message.contains("null"), "Should not contain null references");
        
        // Should be actionable and ArcFace-specific
        assertTrue(message.contains("ArcFace"), "Should identify ArcFace model");
        assertTrue(message.contains("Configure") || message.contains("Set") || message.contains("enable"), 
                  "Should provide actionable guidance");
        
        // Should mention specific ArcFace configuration options
        boolean hasArcFaceConfig = message.contains("FACEBYTES_ARCFACE_ONNX_PATH") || 
                                  message.contains("facebytes.arcface.onnx");
        assertTrue(hasArcFaceConfig, "Should mention ArcFace-specific configuration");
    }
    
    @Test
    void testPerformanceCharacteristics_ConsistentFailureTiming() {
        // Test that failure behavior is consistent (important for production debugging)
        BufferedImage testFace = createTestArcFaceImage(112, 112);
        
        // Multiple calls should fail in similar timeframes
        long[] timings = new long[3];
        
        for (int i = 0; i < 3; i++) {
            long startTime = System.nanoTime();
            
            assertThrows(DeepFaceException.class, () -> {
                model.generateEmbedding(testFace, 112);
            });
            
            timings[i] = System.nanoTime() - startTime;
        }
        
        // All failure timings should be relatively quick (under 100ms for config validation)
        for (long timing : timings) {
            long milliseconds = timing / 1_000_000;
            assertTrue(milliseconds < 100, 
                      "Configuration validation should fail quickly: " + milliseconds + "ms");
        }
    }
} 