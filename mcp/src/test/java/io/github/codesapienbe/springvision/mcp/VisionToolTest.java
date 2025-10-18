package io.github.codesapienbe.springvision.mcp;

import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionCategory;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.capabilities.EmbeddingCapability;
import io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class VisionToolTest {

    // 1x1 transparent PNG
    private static final byte[] SAMPLE_PNG = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=");

    private static final String IMAGE_URL_HAVING_ONE_FACE = "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg";


    @Test
    void countFaces_success_singleFace() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        FaceDetectionCapability mockBackend = Mockito.mock(FaceDetectionCapability.class);

        Detection d = Detection.of("face", 0.85, new BoundingBox(0.1, 0.1, 0.2, 0.2));
        
        // Mock the backend() call to return our mock backend
        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.detectFaces(Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class)))
            .thenReturn(List.of(d));

        VisionTool tool = new VisionTool(mockTemplate) {
            @Override
            protected byte[] downloadImageFromUrl(String imageUrl) {
                return SAMPLE_PNG;
            }
        };

        Map<String, Object> resp = tool.countFaces(IMAGE_URL_HAVING_ONE_FACE);

        assertEquals("success", resp.get("status"));
        assertEquals(1, resp.get("count"));
        assertTrue(resp.containsKey("averageConfidence"));
        // averageConfidence stored as rounded double
        assertEquals(0.85, (Double) resp.get("averageConfidence"), 0.0001);
        assertTrue(((String) resp.get("message")).contains("Detected 1 face"));
    }

    @Test
    void countFaces_emptyUrl_returnsError() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        VisionTool tool = new VisionTool(mockTemplate) {
            @Override
            protected byte[] downloadImageFromUrl(String imageUrl) {
                return SAMPLE_PNG;
            }
        };

        Map<String, Object> resp = tool.countFaces("");

        assertEquals("error", resp.get("status"));
        assertEquals(0, resp.get("count"));
        assertTrue(((String) resp.get("message")).toLowerCase().contains("image url is required"));
    }

    @Test
    void verifyFaces_matchingFaces_returnsMatch() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        // Create identical embeddings for matching faces
        float[] embedding1 = createNormalizedEmbedding(128);
        float[] embedding2 = createNormalizedEmbedding(128); // Same embedding

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of(embedding1))
            .thenReturn(List.of(embedding2));

        VisionTool tool = new VisionTool(mockTemplate) {
            @Override
            protected byte[] downloadImageFromUrl(String imageUrl) {
                return SAMPLE_PNG;
            }
        };

        Map<String, Object> resp = tool.verifyFaces("https://example.com/face1.jpg", "https://example.com/face2.jpg");

        assertEquals("success", resp.get("status"));
        assertTrue((Boolean) resp.get("isMatch"));
        assertTrue(((Number) resp.get("similarity")).doubleValue() > 0.6); // Above threshold
        assertTrue(resp.containsKey("metrics"));
        assertTrue(resp.containsKey("processingTimeMs"));
    }

    @Test
    void verifyFaces_differentFaces_returnsNoMatch() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        // Create different embeddings for non-matching faces
        float[] embedding1 = createNormalizedEmbedding(128);
        float[] embedding2 = createDifferentEmbedding(128);

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of(embedding1))
            .thenReturn(List.of(embedding2));

        VisionTool tool = new VisionTool(mockTemplate) {
            @Override
            protected byte[] downloadImageFromUrl(String imageUrl) {
                return SAMPLE_PNG;
            }
        };

        Map<String, Object> resp = tool.verifyFaces("https://example.com/face1.jpg", "https://example.com/face2.jpg");

        assertEquals("success", resp.get("status"));
        assertFalse((Boolean) resp.get("isMatch"));
        assertTrue(((Number) resp.get("similarity")).doubleValue() < 0.6); // Below threshold
    }

    @Test
    void verifyFaces_noFaceInSource_returnsError() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        // No faces detected in source
        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of());

        VisionTool tool = new VisionTool(mockTemplate) {
            @Override
            protected byte[] downloadImageFromUrl(String imageUrl) {
                return SAMPLE_PNG;
            }
        };

        Map<String, Object> resp = tool.verifyFaces("https://example.com/face1.jpg", "https://example.com/face2.jpg");

        assertEquals("error", resp.get("status"));
        assertFalse((Boolean) resp.get("isMatch"));
        assertTrue(((String) resp.get("message")).contains("No faces detected in source image"));
    }

    @Test
    void verifyFaces_emptyUrls_returnsError() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        VisionTool tool = new VisionTool(mockTemplate);

        Map<String, Object> resp1 = tool.verifyFaces("", "https://example.com/face2.jpg");
        assertEquals("error", resp1.get("status"));
        assertTrue(((String) resp1.get("message")).contains("Source image URL is required"));

        Map<String, Object> resp2 = tool.verifyFaces("https://example.com/face1.jpg", "");
        assertEquals("error", resp2.get("status"));
        assertTrue(((String) resp2.get("message")).contains("Target image URL is required"));
    }

    @Test
    void lookupFaces_findsMatches_returnsMatchesSortedBySimilarity() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        float[] sourceEmbedding = createNormalizedEmbedding(128);
        float[] matchingEmbedding1 = createNormalizedEmbedding(128); // High similarity
        float[] matchingEmbedding2 = createSlightlyDifferentEmbedding(128); // Medium similarity
        float[] nonMatchingEmbedding = createDifferentEmbedding(128); // Low similarity

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of(sourceEmbedding))
            .thenReturn(List.of(matchingEmbedding1))
            .thenReturn(List.of(matchingEmbedding2))
            .thenReturn(List.of(nonMatchingEmbedding));

        VisionTool tool = new VisionTool(mockTemplate) {
            @Override
            protected byte[] downloadImageFromUrl(String imageUrl) {
                return SAMPLE_PNG;
            }
        };

        Set<String> dataset = Set.of(
            "https://example.com/dataset1.jpg",
            "https://example.com/dataset2.jpg",
            "https://example.com/dataset3.jpg"
        );

        Map<String, Object> resp = tool.lookupFaces("https://example.com/source.jpg", dataset);

        assertEquals("success", resp.get("status"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = (List<Map<String, Object>>) resp.get("matches");
        assertNotNull(matches);
        assertTrue(matches.size() >= 2); // At least 2 matches above threshold

        // Verify sorted by similarity (descending)
        for (int i = 0; i < matches.size() - 1; i++) {
            double sim1 = ((Number) matches.get(i).get("similarity")).doubleValue();
            double sim2 = ((Number) matches.get(i + 1).get("similarity")).doubleValue();
            assertTrue(sim1 >= sim2, "Matches should be sorted by similarity descending");
        }

        assertEquals(3, resp.get("datasetSize"));
        assertEquals(3, resp.get("processedCount"));
        assertTrue(resp.containsKey("processingTimeMs"));
    }

    @Test
    void lookupFaces_noFacesInSource_returnsError() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of()); // No faces

        VisionTool tool = new VisionTool(mockTemplate) {
            @Override
            protected byte[] downloadImageFromUrl(String imageUrl) {
                return SAMPLE_PNG;
            }
        };

        Set<String> dataset = Set.of("https://example.com/dataset1.jpg");
        Map<String, Object> resp = tool.lookupFaces("https://example.com/source.jpg", dataset);

        assertEquals("error", resp.get("status"));
        assertTrue(((String) resp.get("message")).contains("No faces detected in source image"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = (List<Map<String, Object>>) resp.get("matches");
        assertTrue(matches.isEmpty());
    }

    @Test
    void lookupFaces_emptyDataset_returnsError() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        VisionTool tool = new VisionTool(mockTemplate);

        Map<String, Object> resp = tool.lookupFaces("https://example.com/source.jpg", Set.of());

        assertEquals("error", resp.get("status"));
        assertTrue(((String) resp.get("message")).contains("Dataset image URLs are required"));
    }

    @Test
    void lookupFaces_handlesDatasetErrors_continuesProcessing() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        float[] sourceEmbedding = createNormalizedEmbedding(128);
        float[] matchingEmbedding = createNormalizedEmbedding(128);

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of(sourceEmbedding))
            .thenReturn(List.of(matchingEmbedding))
            .thenThrow(new RuntimeException("Download failed")); // One fails

        VisionTool tool = new VisionTool(mockTemplate) {
            @Override
            protected byte[] downloadImageFromUrl(String imageUrl) {
                if (imageUrl.contains("error")) {
                    throw new RuntimeException("Download failed");
                }
                return SAMPLE_PNG;
            }
        };

        Set<String> dataset = Set.of(
            "https://example.com/dataset1.jpg",
            "https://example.com/error.jpg" // This will fail
        );

        Map<String, Object> resp = tool.lookupFaces("https://example.com/source.jpg", dataset);

        assertEquals("success", resp.get("status"));
        assertEquals(1, resp.get("errorCount")); // One error
        assertTrue(((Number) resp.get("processedCount")).intValue() >= 1); // At least one processed
    }

    @Test
    void verifyFacesFromBytes_matchingFaces_returnsMatch() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        float[] embedding1 = createNormalizedEmbedding(128);
        float[] embedding2 = createNormalizedEmbedding(128);

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of(embedding1))
            .thenReturn(List.of(embedding2));

        VisionTool tool = new VisionTool(mockTemplate);

        Map<String, Object> resp = tool.verifyFacesFromBytes(SAMPLE_PNG, SAMPLE_PNG);

        assertEquals("success", resp.get("status"));
        assertTrue((Boolean) resp.get("isMatch"));
        assertTrue(((Number) resp.get("similarity")).doubleValue() > 0.6);
    }

    @Test
    void verifyFacesFromBytes_differentFaces_returnsNoMatch() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        float[] embedding1 = createNormalizedEmbedding(128);
        float[] embedding2 = createDifferentEmbedding(128);

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of(embedding1))
            .thenReturn(List.of(embedding2));

        VisionTool tool = new VisionTool(mockTemplate);

        Map<String, Object> resp = tool.verifyFacesFromBytes(SAMPLE_PNG, SAMPLE_PNG);

        assertEquals("success", resp.get("status"));
        assertFalse((Boolean) resp.get("isMatch"));
    }

    @Test
    void lookupFacesFromBytes_findsMatches_returnsMatchesSortedBySimilarity() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        float[] sourceEmbedding = createNormalizedEmbedding(128);
        float[] matchingEmbedding1 = createNormalizedEmbedding(128);
        float[] matchingEmbedding2 = createSlightlyDifferentEmbedding(128);
        float[] nonMatchingEmbedding = createDifferentEmbedding(128);

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of(sourceEmbedding))
            .thenReturn(List.of(matchingEmbedding1))
            .thenReturn(List.of(matchingEmbedding2))
            .thenReturn(List.of(nonMatchingEmbedding));

        VisionTool tool = new VisionTool(mockTemplate);

        List<byte[]> dataset = List.of(SAMPLE_PNG, SAMPLE_PNG, SAMPLE_PNG);

        Map<String, Object> resp = tool.lookupFacesFromBytes(SAMPLE_PNG, dataset);

        assertEquals("success", resp.get("status"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = (List<Map<String, Object>>) resp.get("matches");
        assertNotNull(matches);
        assertTrue(matches.size() >= 2);

        for (int i = 0; i < matches.size() - 1; i++) {
            double sim1 = ((Number) matches.get(i).get("similarity")).doubleValue();
            double sim2 = ((Number) matches.get(i + 1).get("similarity")).doubleValue();
            assertTrue(sim1 >= sim2, "Matches should be sorted by similarity descending");
        }

        assertEquals(3, resp.get("datasetSize"));
    }

    @Test
    void lookupFacesFromBytes_handlesDatasetErrors_continuesProcessing() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);
        EmbeddingCapability mockBackend = Mockito.mock(EmbeddingCapability.class);

        float[] sourceEmbedding = createNormalizedEmbedding(128);
        float[] matchingEmbedding = createNormalizedEmbedding(128);

        Mockito.when(mockTemplate.backend()).thenReturn((VisionBackend) mockBackend);
        Mockito.when(mockBackend.extractEmbeddings(
            Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class), 
            Mockito.eq(DetectionCategory.FACE)))
            .thenReturn(List.of(sourceEmbedding))
            .thenReturn(List.of(matchingEmbedding))
            .thenThrow(new RuntimeException("Processing failed"));

        VisionTool tool = new VisionTool(mockTemplate);

        List<byte[]> dataset = List.of(SAMPLE_PNG, SAMPLE_PNG);

        Map<String, Object> resp = tool.lookupFacesFromBytes(SAMPLE_PNG, dataset);

        assertEquals("success", resp.get("status"));
        assertEquals(1, resp.get("errorCount"));
    }

    // Helper methods to create test embeddings
    private float[] createNormalizedEmbedding(int size) {
        float[] embedding = new float[size];
        for (int i = 0; i < size; i++) {
            embedding[i] = (float) (Math.sin(i) * 0.5);
        }
        return embedding;
    }

    private float[] createDifferentEmbedding(int size) {
        float[] embedding = new float[size];
        for (int i = 0; i < size; i++) {
            embedding[i] = (float) (Math.cos(i * 2) * 0.5);
        }
        return embedding;
    }

    private float[] createSlightlyDifferentEmbedding(int size) {
        float[] embedding = new float[size];
        for (int i = 0; i < size; i++) {
            embedding[i] = (float) (Math.sin(i) * 0.5 + 0.1);
        }
        return embedding;
    }
}
