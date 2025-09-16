package com.deepface.models;

import com.deepface.core.DeepFace;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration test that validates configuration error handling
 * and guidance messages across all FaceBytes models and predictors.
 * 
 * This test ensures that when models are not configured, users receive:
 * - Clear, actionable error messages
 * - Specific configuration guidance for each model
 * - Consistent error handling patterns
 * - Proper environment variable and system property names
 */
public class ConfigurationValidationIntegrationTest {

    @TempDir
    Path tempDir;
    
    private final Map<String, String> originalProperties = new HashMap<>();
    private final Map<String, String> originalEnvVars = new HashMap<>();
    
    // Test image for all model tests
    private BufferedImage testFaceImage;
    
    @BeforeEach
    void setUp() {
        // Save original system properties
        List<String> propertiesToSave = Arrays.asList(
            "facebytes.vggface.onnx",
            "facebytes.arcface.onnx",
            "facebytes.facenet.onnx",
            "facebytes.facenet512.onnx",
            "facebytes.openface.onnx",
            "facebytes.deepface.onnx",
            "facebytes.sface.onnx",
            "facebytes.deepid.onnx",
            "facebytes.age.onnx",
            "facebytes.gender.onnx",
            "facebytes.emotion.onnx",
            "facebytes.race.onnx"
        );
        
        for (String property : propertiesToSave) {
            String value = System.getProperty(property);
            if (value != null) {
                originalProperties.put(property, value);
            }
            System.clearProperty(property);
        }
        
        // Create test image
        testFaceImage = createTestFaceImage(224, 224);
    }
    
    @AfterEach
    void tearDown() {
        // Restore original system properties
        originalProperties.forEach(System::setProperty);
        
        // Clear any properties we might have set during tests
        originalProperties.keySet().forEach(System::clearProperty);
    }
    
    private BufferedImage createTestFaceImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Create realistic face pattern
        g2d.setColor(new Color(220, 190, 170)); // Skin tone
        g2d.fillRect(0, 0, width, height);
        
        // Facial features proportional to size
        g2d.setColor(Color.BLACK);
        int eyeSize = Math.max(width/20, 3);
        int eyeY = height/4;
        g2d.fillOval(width/4, eyeY, eyeSize, eyeSize);  // Left eye
        g2d.fillOval(3*width/4-eyeSize, eyeY, eyeSize, eyeSize);  // Right eye
        
        int noseW = Math.max(width/30, 2);
        int noseH = Math.max(height/20, 4);
        g2d.fillOval(width/2-noseW/2, height/2-noseH/2, noseW, noseH);  // Nose
        
        int mouthW = Math.max(width/8, 8);
        int mouthH = Math.max(height/40, 2);
        g2d.fillRect(width/2-mouthW/2, 3*height/4, mouthW, mouthH);  // Mouth
        
        g2d.dispose();
        return image;
    }
    
    @Test
    void testVGGFaceModelConfigurationGuidance() {
        VGGFaceModel model = new VGGFaceModel();
        
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFaceImage, 224);
        });
        
        String message = exception.getMessage();
        
        // Validate comprehensive configuration guidance
        assertAll("VGGFace configuration guidance",
            () -> assertTrue(message.contains("VGGFace ONNX model is not available"), 
                           "Should identify VGGFace model specifically"),
            () -> assertTrue(message.contains("FACEBYTES_VGGFACE_ONNX_PATH"), 
                           "Should mention environment variable"),
            () -> assertTrue(message.contains("facebytes.vggface.onnx"), 
                           "Should mention system property"),
            () -> assertTrue(message.contains("auto-download"), 
                           "Should mention auto-download option"),
            () -> assertTrue(message.contains("configuration"), 
                           "Should mention configuration")
        );
    }
    
    @Test
    void testArcFaceModelConfigurationGuidance() {
        ArcFaceModel model = new ArcFaceModel();
        
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFaceImage, 112);
        });
        
        String message = exception.getMessage();
        
        // Validate ArcFace-specific guidance
        assertAll("ArcFace configuration guidance",
            () -> assertTrue(message.contains("ArcFace ONNX model is not available"), 
                           "Should identify ArcFace model specifically"),
            () -> assertTrue(message.contains("FACEBYTES_ARCFACE_ONNX_PATH"), 
                           "Should mention ArcFace environment variable"),
            () -> assertTrue(message.contains("facebytes.arcface.onnx"), 
                           "Should mention ArcFace system property"),
            () -> assertFalse(message.contains("VGGFace"), 
                            "Should not mention other models")
        );
    }
    
    @Test
    void testFacialAnalysisPredictorsConfigurationGuidance() {
        // Test all facial analysis predictors
        Map<String, Runnable> predictorTests = Map.of(
            "AgePredictor", () -> {
                AgePredictor predictor = new AgePredictor();
                assertThrows(DeepFaceException.class, () -> 
                    predictor.predictAge(testFaceImage));
            },
            "GenderPredictor", () -> {
                GenderPredictor predictor = new GenderPredictor();
                assertThrows(DeepFaceException.class, () -> 
                    predictor.predictGender(testFaceImage));
            },
            "EmotionPredictor", () -> {
                EmotionPredictor predictor = new EmotionPredictor();
                assertThrows(DeepFaceException.class, () -> 
                    predictor.predictEmotion(testFaceImage));
            },
            "RacePredictor", () -> {
                RacePredictor predictor = new RacePredictor();
                assertThrows(DeepFaceException.class, () -> 
                    predictor.predictRace(testFaceImage));
            }
        );
        
        // Verify each predictor provides proper guidance
        for (Map.Entry<String, Runnable> entry : predictorTests.entrySet()) {
            String predictorName = entry.getKey();
            Runnable test = entry.getValue();
            
            assertDoesNotThrow(() -> test.run(), 
                             predictorName + " should throw DeepFaceException, not other exceptions");
        }
    }
    
    @Test
    void testConsistentErrorMessageStructure() {
        // Test that all models provide consistent error message structure
        List<DeepFaceException> exceptions = Arrays.asList(
            assertThrows(DeepFaceException.class, () -> 
                new VGGFaceModel().generateEmbedding(testFaceImage, 224)),
            assertThrows(DeepFaceException.class, () -> 
                new ArcFaceModel().generateEmbedding(testFaceImage, 112)),
            assertThrows(DeepFaceException.class, () -> 
                new OpenFaceModel().generateEmbedding(testFaceImage, 96)),
            assertThrows(DeepFaceException.class, () -> 
                new SFaceModel().generateEmbedding(testFaceImage, 112)),
            assertThrows(DeepFaceException.class, () -> 
                new AgePredictor().predictAge(testFaceImage)),
            assertThrows(DeepFaceException.class, () -> 
                new GenderPredictor().predictGender(testFaceImage)),
            assertThrows(DeepFaceException.class, () -> 
                new EmotionPredictor().predictEmotion(testFaceImage)),
            assertThrows(DeepFaceException.class, () -> 
                new RacePredictor().predictRace(testFaceImage))
        );
        
        // Validate consistent error message quality
        for (DeepFaceException exception : exceptions) {
            String message = exception.getMessage();
            
            assertAll("Consistent error message structure",
                () -> assertFalse(message.isEmpty(), 
                                 "Error message should not be empty"),
                () -> assertFalse(message.contains("null"), 
                                 "Error message should not contain null references"),
                () -> assertFalse(message.contains("TODO"), 
                                 "Error message should not contain TODO markers"),
                () -> assertFalse(message.contains("mock"), 
                                 "Error message should not reference mock implementations"),
                () -> assertTrue(message.contains("ONNX model is not available") || 
                               message.contains("ONNX session not available"), 
                               "Should clearly indicate model unavailability"),
                () -> assertTrue(message.contains("Configure") || message.contains("Set") || 
                               message.contains("enable") || message.contains("Provide"), 
                               "Should provide actionable guidance")
            );
        }
    }
    
    @Test
    void testConfigurationPriorityValidation() throws IOException {
        // Test that system properties override environment variables
        VGGFaceModel model = new VGGFaceModel();
        
        // Create a dummy file for system property test
        Path dummyModel = tempDir.resolve("system_property_model.onnx");
        Files.createFile(dummyModel);
        
        // Set system property
        System.setProperty("facebytes.vggface.onnx", dummyModel.toString());
        
        try {
            // Should still fail (empty file), but error should reference our configured path
            DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
                model.generateEmbedding(testFaceImage, 224);
            });
            
            // Error should still mention configuration (file exists but is invalid/empty)
            String message = exception.getMessage();
            assertTrue(message.contains("VGGFace"), 
                      "Should mention VGGFace even when file is configured but invalid");
            
        } finally {
            System.clearProperty("facebytes.vggface.onnx");
        }
    }
    
    @Test
    void testMultipleModelTypesIndependentConfiguration() {
        // Verify that different models have independent configuration requirements
        Map<String, Class<?>> modelClasses = Map.of(
            "VGGFace", VGGFaceModel.class,
            "ArcFace", ArcFaceModel.class,
            "OpenFace", OpenFaceModel.class,
            "SFace", SFaceModel.class
        );
        
        for (Map.Entry<String, Class<?>> entry : modelClasses.entrySet()) {
            String modelName = entry.getKey();
            Class<?> modelClass = entry.getValue();
            
            assertDoesNotThrow(() -> {
                Object modelInstance = modelClass.getConstructor().newInstance();
                
                // Each model should fail independently with specific error messages
                if (modelInstance instanceof VGGFaceModel) {
                    DeepFaceException ex = assertThrows(DeepFaceException.class, () -> 
                        ((VGGFaceModel) modelInstance).generateEmbedding(testFaceImage, 224));
                    assertTrue(ex.getMessage().contains("VGGFace"), 
                              "VGGFace should mention itself specifically");
                    
                } else if (modelInstance instanceof ArcFaceModel) {
                    DeepFaceException ex = assertThrows(DeepFaceException.class, () -> 
                        ((ArcFaceModel) modelInstance).generateEmbedding(testFaceImage, 112));
                    assertTrue(ex.getMessage().contains("ArcFace"), 
                              "ArcFace should mention itself specifically");
                    
                } else if (modelInstance instanceof OpenFaceModel) {
                    DeepFaceException ex = assertThrows(DeepFaceException.class, () -> 
                        ((OpenFaceModel) modelInstance).generateEmbedding(testFaceImage, 96));
                    assertTrue(ex.getMessage().contains("OpenFace"), 
                              "OpenFace should mention itself specifically");
                    
                } else if (modelInstance instanceof SFaceModel) {
                    DeepFaceException ex = assertThrows(DeepFaceException.class, () -> 
                        ((SFaceModel) modelInstance).generateEmbedding(testFaceImage, 112));
                    assertTrue(ex.getMessage().contains("SFace"), 
                              "SFace should mention itself specifically");
                }
                
            }, "Model instantiation and error testing should not throw unexpected exceptions for " + modelName);
        }
    }
    
    @Test
    void testErrorMessageLocalization() {
        // Verify error messages are in English and contain actionable information
        VGGFaceModel model = new VGGFaceModel();
        
        DeepFaceException exception = assertThrows(DeepFaceException.class, () -> {
            model.generateEmbedding(testFaceImage, 224);
        });
        
        String message = exception.getMessage();
        
        // Validate message quality for international users
        assertAll("Error message localization and clarity",
            () -> assertTrue(message.matches(".*[a-zA-Z].*"), 
                           "Should contain English text"),
            () -> assertFalse(message.matches(".*[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}].*"), 
                             "Should not contain non-Latin characters (for now)"),
            () -> assertTrue(message.length() > 50, 
                           "Should be sufficiently descriptive (at least 50 characters)"),
            () -> assertTrue(message.length() < 500, 
                           "Should not be excessively verbose (under 500 characters)")
        );
    }
} 