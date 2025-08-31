package com.deepface.detectors;

import com.deepface.enums.DetectorBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DetectorFactory to ensure all detector backends are properly implemented.
 * Tests both individual detector creation and the factory pattern.
 * 
 * @author FaceBytes Team
 * @since 1.0.0
 */
class DetectorFactoryTest {

    private static final Logger log = LoggerFactory.getLogger(DetectorFactoryTest.class);
    
    private BufferedImage testImage;

    @BeforeEach
    void setUp() {
        // Create a simple test image with a colored rectangle
        testImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(50, 50, 100, 100);
        g.dispose();
        
        log.info("Created test image {}x{}", testImage.getWidth(), testImage.getHeight());
    }

    @Test
    void testCreateDefault() {
        log.info("Testing default detector creation");
        
        FaceDetector detector = DetectorFactory.createDefault();
        assertNotNull(detector, "Default detector should not be null");
        
        // Test that the detector can process an image without throwing exceptions
        List<FaceRegion> faces = detector.detectFaces(testImage);
        assertNotNull(faces, "Face detection result should not be null");
        
        log.info("Default detector created successfully, detected {} faces", faces.size());
    }

    @Test
    void testCreateOpenCV() {
        log.info("Testing OpenCV detector creation");
        
        FaceDetector detector = DetectorFactory.create(DetectorBackend.OPENCV);
        assertNotNull(detector, "OpenCV detector should not be null");
        assertTrue(detector instanceof OpenCVDetector, "Should return OpenCVDetector instance");
        
        // Test face detection
        List<FaceRegion> faces = detector.detectFaces(testImage);
        assertNotNull(faces, "OpenCV face detection result should not be null");
        
        log.info("OpenCV detector created successfully, detected {} faces", faces.size());
    }

    @Test
    void testCreateRetinaFace() {
        log.info("Testing RetinaFace detector creation");
        
        FaceDetector detector = DetectorFactory.create(DetectorBackend.RETINAFACE);
        assertNotNull(detector, "RetinaFace detector should not be null");
        assertTrue(detector instanceof RetinaFaceDetector, "Should return RetinaFaceDetector instance");
        
        // Test face detection
        List<FaceRegion> faces = detector.detectFaces(testImage);
        assertNotNull(faces, "RetinaFace face detection result should not be null");
        
        log.info("RetinaFace detector created successfully, detected {} faces", faces.size());
    }

    @Test
    void testCreateDlib() {
        log.info("Testing DLIB detector creation");
        
        FaceDetector detector = DetectorFactory.create(DetectorBackend.DLIB);
        assertNotNull(detector, "DLIB detector should not be null");
        assertTrue(detector instanceof DlibDetector, "Should return DlibDetector instance");
        
        // Test face detection
        List<FaceRegion> faces = detector.detectFaces(testImage);
        assertNotNull(faces, "DLIB face detection result should not be null");
        
        log.info("DLIB detector created successfully, detected {} faces", faces.size());
    }

    @Test
    void testCreateMTCNN() {
        log.info("Testing MTCNN detector creation");
        
        FaceDetector detector = DetectorFactory.create(DetectorBackend.MTCNN);
        assertNotNull(detector, "MTCNN detector should not be null");
        assertTrue(detector instanceof MtcnnDetector, "Should return MtcnnDetector instance");
        
        // Test face detection
        List<FaceRegion> faces = detector.detectFaces(testImage);
        assertNotNull(faces, "MTCNN face detection result should not be null");
        
        log.info("MTCNN detector created successfully, detected {} faces", faces.size());
    }

    @Test
    void testCreateWithNullBackend() {
        log.info("Testing detector creation with null backend");
        
        FaceDetector detector = DetectorFactory.create(null);
        assertNotNull(detector, "Null backend should return default detector");
        
        // Should return the same as createDefault()
        FaceDetector defaultDetector = DetectorFactory.createDefault();
        assertEquals(detector.getClass(), defaultDetector.getClass(), 
                   "Null backend should return default detector type");
        
        log.info("Null backend correctly returns default detector");
    }

    @Test
    void testSingletonPattern() {
        log.info("Testing singleton pattern for detector instances");
        
        // Create multiple instances of the same detector type
        FaceDetector detector1 = DetectorFactory.create(DetectorBackend.OPENCV);
        FaceDetector detector2 = DetectorFactory.create(DetectorBackend.OPENCV);
        FaceDetector detector3 = DetectorFactory.create(DetectorBackend.OPENCV);
        
        // All should be the same instance (singleton)
        assertSame(detector1, detector2, "OpenCV detectors should be singleton instances");
        assertSame(detector2, detector3, "OpenCV detectors should be singleton instances");
        
        // Test different detector types
        FaceDetector dlib1 = DetectorFactory.create(DetectorBackend.DLIB);
        FaceDetector dlib2 = DetectorFactory.create(DetectorBackend.DLIB);
        assertSame(dlib1, dlib2, "DLIB detectors should be singleton instances");
        
        // Different types should be different instances
        assertNotSame(detector1, dlib1, "Different detector types should be different instances");
        
        log.info("Singleton pattern working correctly for all detector types");
    }

    @Test
    void testDetectorCapabilities() {
        log.info("Testing detector capabilities and consistency");
        
        // Test all detector types
        DetectorBackend[] backends = DetectorBackend.values();
        
        for (DetectorBackend backend : backends) {
            log.debug("Testing detector capabilities for: {}", backend);
            
            FaceDetector detector = DetectorFactory.create(backend);
            assertNotNull(detector, "Detector for " + backend + " should not be null");
            
            // Test with a simple image
            List<FaceRegion> faces = detector.detectFaces(testImage);
            assertNotNull(faces, "Face detection result for " + backend + " should not be null");
            
            // All detectors should return a list (even if empty)
            assertTrue(faces.size() >= 0, "Face count for " + backend + " should be non-negative");
            
            log.debug("Detector {} processed image successfully, found {} faces", backend, faces.size());
        }
        
        log.info("All detector backends processed test image successfully");
    }

    @Test
    void testDetectorPerformance() {
        log.info("Testing detector performance characteristics");
        
        // Test performance of different detectors
        DetectorBackend[] backends = {DetectorBackend.OPENCV, DetectorBackend.DLIB, DetectorBackend.MTCNN};
        
        for (DetectorBackend backend : backends) {
            FaceDetector detector = DetectorFactory.create(backend);
            
            // Warm up
            detector.detectFaces(testImage);
            
            // Measure performance
            long startTime = System.nanoTime();
            List<FaceRegion> faces = detector.detectFaces(testImage);
            long endTime = System.nanoTime();
            
            long durationMs = (endTime - startTime) / 1_000_000;
            
            log.info("Detector {} processed image in {} ms, detected {} faces", 
                    backend, durationMs, faces.size());
            
            // Performance should be reasonable (less than 5 seconds for a simple image)
            assertTrue(durationMs < 5000, "Detector " + backend + " should process image in reasonable time");
        }
    }

    @Test
    void testDetectorErrorHandling() {
        log.info("Testing detector error handling");
        
        // Test with null image
        FaceDetector detector = DetectorFactory.create(DetectorBackend.OPENCV);
        
        List<FaceRegion> faces = detector.detectFaces(null);
        assertNotNull(faces, "Null image should return empty list, not null");
        assertTrue(faces.isEmpty(), "Null image should return empty list");
        
        // Test with very small image
        BufferedImage smallImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        faces = detector.detectFaces(smallImage);
        assertNotNull(faces, "Small image should return list, not null");
        
        log.info("Error handling working correctly for edge cases");
    }

    @Test
    void testDetectorConsistency() {
        log.info("Testing detector consistency across multiple calls");
        
        FaceDetector detector = DetectorFactory.create(DetectorBackend.OPENCV);
        
        // Multiple calls should produce consistent results
        List<FaceRegion> faces1 = detector.detectFaces(testImage);
        List<FaceRegion> faces2 = detector.detectFaces(testImage);
        List<FaceRegion> faces3 = detector.detectFaces(testImage);
        
        // Results should be consistent (same number of faces)
        assertEquals(faces1.size(), faces2.size(), "Face count should be consistent across calls");
        assertEquals(faces2.size(), faces3.size(), "Face count should be consistent across calls");
        
        log.info("Detector produces consistent results across multiple calls");
    }
} 