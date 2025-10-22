package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.AnnotationRequest;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Comprehensive integration test for advanced DJL Vision Backend capabilities.
 * Tests emotion detection, demographics, threat detection, embeddings, annotations, and other advanced features.
 */
@DisplayName("Advanced Capabilities Integration Tests")
public class AdvancedCapabilitiesIntegrationTest {

    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        // Configure backend for real capability testing
        System.setProperty("ai.djl.offline", "true");
        System.setProperty("OPT_OUT_TRACKING", "true");

        DjlProperties properties = new DjlProperties();
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setAutoDownload(false); // Use offline mode

        backend = new DjlVisionBackend(properties);

        // Initialize backend
        backend.initialize();

        // Verify backend is ready
        assertThat(backend.isHealthy()).isTrue();
    }

    @AfterAll
    static void teardown() {
        if (backend != null) {
            backend.shutdown();
        }
    }

    // ==================== Emotion Detection Tests ====================

    @Test
    @DisplayName("Should detect emotions in face-like images")
    void shouldDetectEmotionsInFaceImages() throws IOException {
        // Given: Simple face-like image
        ImageData faceImage = TestImageUtils.createSimpleFaceImage(320, 240);

        // When: Emotion detection is performed
        List<Detection> emotions = backend.detectEmotions(faceImage);

        // Then: Should return emotion detection results
        assertThat(emotions).isNotNull();
        assertThat(emotions).isInstanceOf(List.class);

        // Should have emotion detections with valid structure
        for (Detection emotion : emotions) {
            assertThat(emotion).isNotNull();
            assertThat(emotion.label()).isNotNull(); // Emotion name
            assertThat(emotion.confidence()).isBetween(0.0, 1.0);

            // Attributes should contain emotion information
            assertThat(emotion.attributes()).isNotNull();
            assertThat(emotion.attributes()).containsKey("emotion");
            assertThat(emotion.attributes()).containsKey("backend");
            assertThat(emotion.attributes().get("backend")).isEqualTo("djl");
        }
    }

    @Test
    @DisplayName("Should handle emotion detection on non-face images")
    void shouldHandleEmotionDetectionOnNonFaceImages() throws IOException {
        // Given: Image without faces
        ImageData noFaceImage = TestImageUtils.createGeometricShapeImage(320, 240, "circle");

        // When: Emotion detection is performed
        List<Detection> emotions = backend.detectEmotions(noFaceImage);

        // Then: Should return empty or handle gracefully
        assertThat(emotions).isNotNull();
        assertThat(emotions).isInstanceOf(List.class);
    }

    // ==================== Demographics Tests ====================

    @Test
    @DisplayName("Should detect demographics in face-like images")
    void shouldDetectDemographicsInFaceImages() throws IOException {
        // Given: Simple face-like image
        ImageData faceImage = TestImageUtils.createSimpleFaceImage(320, 240);

        // When: Demographics detection is performed
        List<Detection> demographics = backend.detectDemographics(faceImage);

        // Then: Should return demographics detection results
        assertThat(demographics).isNotNull();
        assertThat(demographics).isInstanceOf(List.class);

        // Should have demographic detections with valid structure
        for (Detection demo : demographics) {
            assertThat(demo).isNotNull();
            assertThat(demo.label()).isNotNull(); // Gender
            assertThat(demo.confidence()).isBetween(0.0, 1.0);

            // Attributes should contain demographic information
            assertThat(demo.attributes()).isNotNull();
            assertThat(demo.attributes()).containsKey("age");
            assertThat(demo.attributes()).containsKey("gender");
            assertThat(demo.attributes()).containsKey("backend");
            assertThat(demo.attributes().get("backend")).isEqualTo("djl");
        }
    }

    // ==================== Threat Detection Tests ====================

    @Test
    @DisplayName("Should detect threats in images")
    void shouldDetectThreatsInImages() throws IOException {
        // Given: Test images that might contain threat-like objects
        ImageData[] testImages = {
            TestImageUtils.createRectangleImage(640, 480, Color.BLACK), // Could be mistaken for objects
            TestImageUtils.createGeometricShapeImage(640, 480, "square"),
            TestImageUtils.createPersonSilhouetteImage(640, 480)
        };

        for (ImageData image : testImages) {
            // When: Threat detection is performed
            List<Detection> threats = backend.detectThreat(List.of(image));

            // Then: Should return threat detection results
            assertThat(threats).isNotNull();
            assertThat(threats).isInstanceOf(List.class);

            // Each threat detection should have valid structure
            for (Detection threat : threats) {
                assertThat(threat).isNotNull();
                assertThat(threat.label()).isNotNull();
                assertThat(threat.confidence()).isBetween(0.0, 1.0);

                // Attributes should contain threat information
                assertThat(threat.attributes()).isNotNull();
                assertThat(threat.attributes()).containsKey("threatType");
                assertThat(threat.attributes()).containsKey("severity");
                assertThat(threat.attributes()).containsKey("backend");
                assertThat(threat.attributes().get("backend")).isEqualTo("djl");
            }
        }
    }

    // ==================== Embedding Tests ====================

    @Test
    @DisplayName("Should extract face embeddings")
    void shouldExtractFaceEmbeddings() throws IOException {
        // Given: Face-like image
        ImageData faceImage = TestImageUtils.createSimpleFaceImage(320, 240);

        // When: Face embedding extraction is performed
        List<float[]> embeddings = backend.extractEmbeddings(faceImage,
            io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

        // Then: Should return embedding results
        assertThat(embeddings).isNotNull();
        assertThat(embeddings).isInstanceOf(List.class);

        // If embeddings were extracted, they should have proper structure
        for (float[] embedding : embeddings) {
            assertThat(embedding).isNotNull();
            assertThat(embedding.length).isGreaterThan(0);

            // Embedding values should be normalized (L2 normalized)
            double norm = 0.0;
            for (float val : embedding) {
                norm += val * val;
            }
            // Should be approximately 1.0 (L2 normalized)
            assertThat(Math.sqrt(norm)).isBetween(0.9, 1.1);
        }
    }

    @Test
    @DisplayName("Should handle embedding extraction on non-face images")
    void shouldHandleEmbeddingExtractionOnNonFaceImages() throws IOException {
        // Given: Image without faces
        ImageData noFaceImage = TestImageUtils.createGeometricShapeImage(320, 240, "triangle");

        // When: Face embedding extraction is attempted
        List<float[]> embeddings = backend.extractEmbeddings(noFaceImage,
            io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

        // Then: Should return empty list or handle gracefully
        assertThat(embeddings).isNotNull();
        assertThat(embeddings).isInstanceOf(List.class);
    }

    // ==================== Annotation Tests ====================

    @Test
    @DisplayName("Should annotate images with TAG action")
    void shouldAnnotateImagesWithTagAction() throws IOException {
        // Given: Test image and tag annotation request
        ImageData image = TestImageUtils.createRectangleImage(400, 300, Color.BLUE);
        AnnotationRequest tagRequest = new AnnotationRequest.Builder()
            .action(AnnotationRequest.Action.TAG)
            .label("TEST TAG")
            .build();

        // When: Image annotation is performed
        ImageData annotatedImage = backend.annotate(image, tagRequest);

        // Then: Should return annotated image
        assertThat(annotatedImage).isNotNull();
        assertThat(annotatedImage.data()).isNotNull();
        assertThat(annotatedImage.data().length).isGreaterThan(0);
        assertThat(annotatedImage.mimeType()).isEqualTo(image.mimeType());
    }

    @Test
    @DisplayName("Should annotate images with MARK action")
    void shouldAnnotateImagesWithMarkAction() throws IOException {
        // Given: Test image and mark annotation request
        ImageData image = TestImageUtils.createGeometricShapeImage(400, 300, "circle");
        AnnotationRequest markRequest = new AnnotationRequest.Builder()
            .action(AnnotationRequest.Action.MARK)
            .build();

        // When: Image annotation is performed
        ImageData annotatedImage = backend.annotate(image, markRequest);

        // Then: Should return annotated image
        assertThat(annotatedImage).isNotNull();
        assertThat(annotatedImage.data()).isNotNull();
        assertThat(annotatedImage.data().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should annotate images with OBSCURE action")
    void shouldAnnotateImagesWithObscureAction() throws IOException {
        // Given: Test image and obscure annotation request
        ImageData image = TestImageUtils.createTextImage("SECRET", 400, 100);
        AnnotationRequest obscureRequest = new AnnotationRequest.Builder()
            .action(AnnotationRequest.Action.OBSCURE)
            .build();

        // When: Image annotation is performed
        ImageData annotatedImage = backend.annotate(image, obscureRequest);

        // Then: Should return annotated image
        assertThat(annotatedImage).isNotNull();
        assertThat(annotatedImage.data()).isNotNull();
        assertThat(annotatedImage.data().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle annotation on different image sizes")
    void shouldHandleAnnotationOnDifferentImageSizes() throws IOException {
        // Test different image sizes
        int[][] sizes = {{200, 150}, {400, 300}, {800, 600}};

        for (int[] size : sizes) {
            // Given: Image of specific size and annotation request
            ImageData image = TestImageUtils.createRectangleImage(size[0], size[1], Color.GREEN);
            AnnotationRequest request = new AnnotationRequest.Builder()
                .action(AnnotationRequest.Action.TAG)
                .label("SIZE TEST")
                .build();

            // When: Image annotation is performed
            ImageData annotatedImage = backend.annotate(image, request);

            // Then: Should complete successfully
            assertThat(annotatedImage).isNotNull();
            assertThat(annotatedImage.data()).isNotNull();
        }
    }

    // ==================== Hand Detection Tests ====================

    @Test
    @DisplayName("Should detect hands in images")
    void shouldDetectHandsInImages() throws IOException {
        // Given: Person-like image that might contain hands
        ImageData personImage = TestImageUtils.createPersonSilhouetteImage(640, 480);

        // When: Hand detection is performed
        List<Detection> hands = backend.detectHands(personImage);

        // Then: Should return hand detection results
        assertThat(hands).isNotNull();
        assertThat(hands).isInstanceOf(List.class);

        // Each hand detection should have valid structure
        for (Detection hand : hands) {
            assertThat(hand).isNotNull();
            assertThat(hand.label()).isNotNull();
            assertThat(hand.confidence()).isBetween(0.0, 1.0);

            // Attributes should contain hand information
            assertThat(hand.attributes()).isNotNull();
            assertThat(hand.attributes()).containsKey("backend");
            assertThat(hand.attributes().get("backend")).isEqualTo("djl");
        }
    }

    // ==================== Fall Detection Tests ====================

    @Test
    @DisplayName("Should detect falls from pose sequences")
    void shouldDetectFallsFromPoseSequences() throws IOException {
        // Given: Sequence of person images (single image for now)
        ImageData personImage = TestImageUtils.createPersonSilhouetteImage(640, 480);
        List<ImageData> imageSequence = List.of(personImage);

        // When: Fall detection is performed
        List<Detection> fallDetections = backend.detectFall(imageSequence);

        // Then: Should return fall detection results
        assertThat(fallDetections).isNotNull();
        assertThat(fallDetections).isInstanceOf(List.class);

        // Each fall detection should have valid structure
        for (Detection fall : fallDetections) {
            assertThat(fall).isNotNull();
            assertThat(fall.label()).isNotNull(); // Body orientation
            assertThat(fall.confidence()).isBetween(0.0, 1.0);

            // Attributes should contain fall analysis information
            assertThat(fall.attributes()).isNotNull();
            assertThat(fall.attributes()).containsKey("fallDetected");
            assertThat(fall.attributes()).containsKey("bodyOrientation");
            assertThat(fall.attributes()).containsKey("riskLevel");
            assertThat(fall.attributes()).containsKey("backend");
            assertThat(fall.attributes().get("backend")).isEqualTo("djl");
        }
    }

    // ==================== Stress Analysis Tests ====================

    @Test
    @DisplayName("Should analyze stress from facial expressions")
    void shouldAnalyzeStressFromFacialExpressions() throws IOException {
        // Given: Sequence of face images
        ImageData faceImage = TestImageUtils.createSimpleFaceImage(320, 240);
        List<ImageData> imageSequence = List.of(faceImage);

        // When: Stress analysis is performed
        List<Detection> stressDetections = backend.detectStress(imageSequence);

        // Then: Should return stress analysis results
        assertThat(stressDetections).isNotNull();
        assertThat(stressDetections).isInstanceOf(List.class);

        // Each stress detection should have valid structure
        for (Detection stress : stressDetections) {
            assertThat(stress).isNotNull();
            assertThat(stress.label()).isNotNull(); // Stress level
            assertThat(stress.confidence()).isBetween(0.0, 1.0);

            // Attributes should contain stress analysis information
            assertThat(stress.attributes()).isNotNull();
            assertThat(stress.attributes()).containsKey("stressLevel");
            assertThat(stress.attributes()).containsKey("stressScore");
            assertThat(stress.attributes()).containsKey("dominantEmotion");
            assertThat(stress.attributes()).containsKey("backend");
            assertThat(stress.attributes().get("backend")).isEqualTo("djl");
        }
    }

    // ==================== Heart Rate Tests ====================

    @Test
    @DisplayName("Should estimate heart rate from video frames")
    void shouldEstimateHeartRateFromVideoFrames() throws IOException {
        // Given: Sequence of face images (simulating video frames)
        // Note: This is a challenging test as heart rate needs many frames
        ImageData faceImage = TestImageUtils.createSimpleFaceImage(320, 240);
        List<ImageData> videoFrames = List.of(faceImage); // Minimal sequence

        // When: Heart rate estimation is attempted
        try {
            List<Detection> heartRateDetections = backend.detectHeartRate(videoFrames);

            // Then: Should return heart rate estimation results
            assertThat(heartRateDetections).isNotNull();
            assertThat(heartRateDetections).isInstanceOf(List.class);

            // Each heart rate detection should have valid structure
            for (Detection hr : heartRateDetections) {
                assertThat(hr).isNotNull();
                assertThat(hr.label()).isNotNull(); // BPM reading
                assertThat(hr.confidence()).isBetween(0.0, 1.0);

                // Attributes should contain heart rate information
                assertThat(hr.attributes()).isNotNull();
                assertThat(hr.attributes()).containsKey("heartRate");
                assertThat(hr.attributes()).containsKey("backend");
                assertThat(hr.attributes().get("backend")).isEqualTo("djl");
            }
        } catch (IllegalArgumentException e) {
            // Acceptable if sequence is too short
            assertThat(e.getMessage()).contains("requires minimum");
        }
    }

    // ==================== Deepfake Detection Tests ====================

    @Test
    @DisplayName("Should detect deepfakes in images")
    void shouldDetectDeepfakesInImages() throws IOException {
        // Given: Test images
        ImageData[] testImages = {
            TestImageUtils.createSimpleFaceImage(320, 240),
            TestImageUtils.createRectangleImage(320, 240, Color.BLUE),
            TestImageUtils.createNoiseImage(320, 240)
        };

        for (ImageData image : testImages) {
            // When: Deepfake detection is performed
            List<Detection> deepfakeDetections = backend.detectDeepfake(image);

            // Then: Should return deepfake detection results
            assertThat(deepfakeDetections).isNotNull();
            assertThat(deepfakeDetections).isInstanceOf(List.class);

            // Each deepfake detection should have valid structure
            for (Detection df : deepfakeDetections) {
                assertThat(df).isNotNull();
                assertThat(df.label()).isIn("real", "fake"); // Binary classification
                assertThat(df.confidence()).isBetween(0.0, 1.0);

                // Attributes should contain deepfake analysis information
                assertThat(df.attributes()).isNotNull();
                assertThat(df.attributes()).containsKey("isFake");
                assertThat(df.attributes()).containsKey("classification");
                assertThat(df.attributes()).containsKey("backend");
                assertThat(df.attributes().get("backend")).isEqualTo("djl");
            }
        }
    }

    // ==================== Access Authentication Tests ====================

    @Test
    @DisplayName("Should authenticate access using face recognition")
    void shouldAuthenticateAccessUsingFaceRecognition() throws IOException {
        // Given: Face image for authentication
        ImageData faceImage = TestImageUtils.createSimpleFaceImage(320, 240);

        // When: Access authentication is performed
        List<Detection> authResults = backend.authenticateAccess(faceImage);

        // Then: Should return authentication results
        assertThat(authResults).isNotNull();
        assertThat(authResults).isInstanceOf(List.class);
        assertThat(authResults).hasSize(1); // Should have one authentication result

        Detection authResult = authResults.get(0);
        assertThat(authResult).isNotNull();
        assertThat(authResult.label()).isIn("AUTHORIZED", "UNAUTHORIZED");
        assertThat(authResult.confidence()).isBetween(0.0, 1.0);

        // Attributes should contain authentication information
        assertThat(authResult.attributes()).isNotNull();
        assertThat(authResult.attributes()).containsKey("authorized");
        assertThat(authResult.attributes()).containsKey("backend");
        assertThat(authResult.attributes().get("backend")).isEqualTo("djl");
    }

    // ==================== Combined Advanced Tests ====================

    @Test
    @DisplayName("Should handle multiple advanced capabilities concurrently")
    void shouldHandleMultipleAdvancedCapabilitiesConcurrently() throws IOException {
        // Given: Test image suitable for multiple analyses
        ImageData testImage = TestImageUtils.createSimpleFaceImage(320, 240);

        // When: Multiple advanced capabilities are tested
        List<Detection> emotions = backend.detectEmotions(testImage);
        List<Detection> demographics = backend.detectDemographics(testImage);
        List<float[]> embeddings = backend.extractEmbeddings(testImage,
            io.github.codesapienbe.springvision.core.DetectionCategory.FACE);
        List<Detection> auth = backend.authenticateAccess(testImage);

        // Then: All should complete successfully
        assertThat(emotions).isNotNull();
        assertThat(demographics).isNotNull();
        assertThat(embeddings).isNotNull();
        assertThat(auth).isNotNull();
    }
}
