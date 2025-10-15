package io.github.codesapienbe.springvision.mcp;

import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VisionToolTest {

    // 1x1 transparent PNG
    private static final byte[] SAMPLE_PNG = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=");

    private static final String IMAGE_URL_HAVING_ONE_FACE = "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg";


    @Test
    void countFaces_success_singleFace() {
        VisionTemplate mockTemplate = Mockito.mock(VisionTemplate.class);

        Detection d = Detection.of("face", 0.85, new BoundingBox(0.1, 0.1, 0.2, 0.2));
        VisionResult vr = VisionResult.of(DetectionType.FACE, List.of(d), 0.85, 10);
        // Use a typed matcher to disambiguate overloaded methods
        Mockito.when(mockTemplate.detectFaces(Mockito.any(io.github.codesapienbe.springvision.core.ImageData.class))).thenReturn(vr);

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
}
