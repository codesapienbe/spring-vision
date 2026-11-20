package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;

/** What extract_text_u answers for a document: the text with page breaks, and the text per page. */
class VisionToolDocumentTextTest {

    private static OcrCapability.TextDetection line(String text, int page, String source) {
        return new OcrCapability.TextDetection(text, 1.0, null, Map.of("page", page, "source", source, "level", "line"));
    }

    @Test
    @DisplayName("a document's lines come back per page, with source, and the text keeps its line and page breaks")
    void pages(@TempDir Path dir) throws IOException {
        VisionBackend backend = mock(VisionBackend.class, withSettings().extraInterfaces(OcrCapability.class));
        when(backend.getBackendId()).thenReturn("stub");
        when(backend.getVersion()).thenReturn("0");
        when(((OcrCapability) backend).extractText(any())).thenReturn(List.of(
            line("CERTIFICATE OF CONFORMITY", 1, "pdf-text"),
            line("0.1 Make: Example Motors", 1, "pdf-text"),
            line("Signed and stamped", 2, "ocr")));
        Path pdf = Files.write(dir.resolve("CoC Example.pdf"), "%PDF-1.7 placeholder".getBytes());

        Map<String, Object> response = new VisionTool(new VisionTemplate(backend)).extractText(pdf.toString());

        assertThat(response).containsEntry("status", "success")
            .containsEntry("text", "CERTIFICATE OF CONFORMITY\n0.1 Make: Example Motors\n\nSigned and stamped");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pages = (List<Map<String, Object>>) response.get("pages");
        assertThat(pages).containsExactly(
            Map.<String, Object>of("page", 1, "source", "pdf-text", "text",
                "CERTIFICATE OF CONFORMITY\n0.1 Make: Example Motors"),
            Map.<String, Object>of("page", 2, "source", "ocr", "text", "Signed and stamped"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detections = (List<Map<String, Object>>) response.get("detections");
        assertThat(detections).extracting(item -> item.get("page")).containsExactly(1, 1, 2);
    }
}
