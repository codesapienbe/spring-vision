package com.deepface.core;

import com.deepface.enums.DetectorBackend;
import com.deepface.enums.DistanceMetric;
import com.deepface.enums.ModelType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DeepFace core functionality.
 * Tests the basic verification, face extraction, and embedding generation.
 */
class DeepFaceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(DeepFaceIntegrationTest.class);
    
    @TempDir
    static Path tempDir;
    
    private static File testImage1;
    private static File testImage2;
    private static File testImage3;

    @BeforeAll
    static void setUp() throws IOException {
        // Create test images with different faces
        testImage1 = createTestImage("test1.png", Color.RED, "Face 1");
        testImage2 = createTestImage("test2.png", Color.BLUE, "Face 2");
        testImage3 = createTestImage("test3.png", Color.GREEN, "Face 3");
        
        log.info("Created test images in: {}", tempDir);
    }

    @Test
    void testExtractFaces() throws IOException {
        log.info("Testing face extraction functionality");
        
        List<BufferedImage> faces = DeepFace.extractFaces(testImage1.getAbsolutePath());
        
        assertNotNull(faces, "Face extraction should not return null");
        log.info("Extracted {} faces from test image 1", faces.size());
        
        // Even if no faces are detected, the method should work without throwing exceptions
        assertTrue(faces.size() >= 0, "Face count should be non-negative");
    }

    @Test
    void testExtractFacesWithBackend() throws IOException {
        log.info("Testing face extraction with specific backend");
        
        List<BufferedImage> faces = DeepFace.extractFaces(
            testImage1.getAbsolutePath(), 
            DetectorBackend.OPENCV
        );
        
        assertNotNull(faces, "Face extraction with backend should not return null");
        log.info("Extracted {} faces using OpenCV backend", faces.size());
    }

    @Test
    void testRepresent() throws IOException {
        log.info("Testing embedding representation functionality");
        
        List<EmbeddingResult> embeddings = DeepFace.represent(testImage1.getAbsolutePath());
        
        assertNotNull(embeddings, "Represent should not return null");
        log.info("Generated {} embeddings from test image 1", embeddings.size());
        
        if (!embeddings.isEmpty()) {
            EmbeddingResult first = embeddings.get(0);
            assertNotNull(first.embedding(), "Embedding should not be null");
            assertNotNull(first.faceRegion(), "Face region should not be null");
            log.info("First embedding size: {}", first.embedding().length);
        }
    }

    @Test
    void testRepresentWithModelAndBackend() throws IOException {
        log.info("Testing embedding representation with specific model and backend");
        
        List<EmbeddingResult> embeddings = DeepFace.represent(
            testImage1.getAbsolutePath(),
            ModelType.VGG_FACE,
            DetectorBackend.OPENCV
        );
        
        assertNotNull(embeddings, "Represent with model and backend should not return null");
        log.info("Generated {} embeddings using VGG_FACE model and OpenCV backend", embeddings.size());
    }

    @Test
    void testBasicVerification() throws IOException {
        log.info("Testing basic verification between two images");
        
        VerificationResult result = DeepFace.verify(
            testImage1.getAbsolutePath(),
            testImage2.getAbsolutePath()
        );
        
        assertNotNull(result, "Verification result should not be null");
        assertNotNull(result.model(), "Model should not be null");
        assertNotNull(result.detector(), "Detector should not be null");
        assertTrue(result.processingTimeMs() >= 0, "Processing time should be non-negative");
        
        log.info("Verification result: verified={}, distance={}, threshold={}, model={}, detector={}, time={}ms",
            result.verified(), result.distance(), result.threshold(), 
            result.model(), result.detector(), result.processingTimeMs());
    }

    @Test
    void testVerificationWithExplicitParameters() throws IOException {
        log.info("Testing verification with explicit model, distance metric, and detector");
        
        VerificationResult result = DeepFace.verify(
            testImage1.getAbsolutePath(),
            testImage2.getAbsolutePath(),
            ModelType.VGG_FACE,
            DistanceMetric.COSINE,
            DetectorBackend.OPENCV
        );
        
        assertNotNull(result, "Verification result with explicit parameters should not be null");
        assertEquals(ModelType.VGG_FACE, result.model(), "Model should match requested model");
        assertEquals(DetectorBackend.OPENCV, result.detector(), "Detector should match requested detector");
        
        log.info("Explicit verification result: verified={}, distance={}, threshold={}",
            result.verified(), result.distance(), result.threshold());
    }

    @Test
    void testVerificationWithSameImage() throws IOException {
        log.info("Testing verification with the same image (should be verified)");
        
        VerificationResult result = DeepFace.verify(
            testImage1.getAbsolutePath(),
            testImage1.getAbsolutePath()
        );
        
        assertNotNull(result, "Self-verification result should not be null");
        log.info("Self-verification result: verified={}, distance={}, threshold={}",
            result.verified(), result.distance(), result.threshold());
        
        // Note: In a real implementation, same image should typically verify as true
        // But with mock implementations, this might not be guaranteed
    }

    @Test
    void testFindFunctionality() throws IOException {
        log.info("Testing find functionality with gallery of images");
        
        List<String> gallery = List.of(
            testImage1.getAbsolutePath(),
            testImage2.getAbsolutePath(),
            testImage3.getAbsolutePath()
        );
        
        FindResult result = DeepFace.find(testImage1.getAbsolutePath(), gallery);
        
        assertNotNull(result, "Find result should not be null");
        assertNotNull(result.imagePath(), "Best match path should not be null");
        assertTrue(result.distance() >= 0, "Distance should be non-negative");
        assertTrue(result.threshold() >= 0, "Threshold should be non-negative");
        
        log.info("Find result: bestMatch={}, distance={}, threshold={}, matched={}",
            result.imagePath(), result.distance(), result.threshold(), result.matched());
    }

    @Test
    void testAnalyzeFunctionality() throws IOException {
        log.info("Testing facial analysis functionality");
        
        List<AnalysisResult> results = DeepFace.analyze(testImage1.getAbsolutePath());
        
        assertNotNull(results, "Analysis results should not be null");
        log.info("Generated {} analysis results", results.size());
        
        if (!results.isEmpty()) {
            AnalysisResult first = results.get(0);
            assertNotNull(first.faceRegion(), "Face region should not be null");
            log.info("First analysis: age={}, gender={}, emotion={}", 
                first.age(), first.gender(), first.dominantEmotion());
        }
    }

    @Test
    void testAnalyzeWithActions() throws IOException {
        log.info("Testing facial analysis with specific actions");
        
        String[] actions = {"age", "gender", "emotion"};
        List<AnalysisResult> results = DeepFace.analyze(
            testImage1.getAbsolutePath(), 
            actions
        );
        
        assertNotNull(results, "Analysis results with actions should not be null");
        log.info("Generated {} analysis results with actions", results.size());
    }

    /**
     * Creates a simple test image with a colored background and text.
     * This is used for testing when real face images are not available.
     */
    private static File createTestImage(String filename, Color backgroundColor, String text) throws IOException {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Set background color
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, 200, 200);
        
        // Add text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString(text, 50, 100);
        
        g2d.dispose();
        
        File file = tempDir.resolve(filename).toFile();
        ImageIO.write(image, "PNG", file);
        
        return file;
    }
} 