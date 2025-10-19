package io.github.codesapienbe.springvision.mcp;

import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VisionToolByteFlowsTest {

    @Test
    public void countFacesFromBytes_returnsSuccess() throws Exception {
        VisionTemplate vt = mock(VisionTemplate.class);
        VisionResult vr = mock(VisionResult.class);
        when(vr.detectionCount()).thenReturn(1);
        when(vr.averageConfidence()).thenReturn(0.95);
        when(vt.detectFaces(any())).thenReturn(vr);

        // use a minimal valid PNG (1x1) so ImageData.fromBytes accepts it
        byte[] validPng = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=");
        VisionTool tool = new VisionTool(vt, HttpClient.newHttpClient());
        Map<String, Object> resp = tool.countFaces(validPng);

        assertNotNull(resp);
        assertEquals("success", resp.get("status"));
        assertEquals(1, resp.get("count"));
    }

    @Test
    public void extractEmbeddingsFromBytes_returnsEmbeddings() throws Exception {
        VisionTemplate vt = mock(VisionTemplate.class);
        when(vt.extractEmbeddings(any(), any())).thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));

        byte[] validPng = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=");
        VisionTool tool = new VisionTool(vt, HttpClient.newHttpClient());
        Map<String, Object> resp = tool.extractEmbeddings(validPng);

        assertNotNull(resp);
        assertEquals("success", resp.get("status"));
        assertEquals(1, resp.get("count"));
    }
}


