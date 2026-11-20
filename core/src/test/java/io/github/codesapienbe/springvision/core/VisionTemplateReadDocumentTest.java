package io.github.codesapienbe.springvision.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.capabilities.DocumentReadingCapability;
import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;
import io.github.codesapienbe.springvision.core.document.DocumentContent;
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;

class VisionTemplateReadDocumentTest {

    private static final ImageData IMAGE = ImageData.fromBytes("%PDF-1.7 placeholder".getBytes(StandardCharsets.US_ASCII));

    @Test
    @DisplayName("a backend that reads documents answers with everything it read")
    void documentReader() {
        VisionBackend backend = mock(VisionBackend.class, withSettings().extraInterfaces(DocumentReadingCapability.class));
        DocumentContent content = new DocumentContent(List.of(), List.of(), Map.of("plate", "1-ABC-123"), Map.of());
        when(((DocumentReadingCapability) backend).readDocument(any())).thenReturn(content);

        assertThat(new VisionTemplate(backend).readDocument(IMAGE)).isSameAs(content);
    }

    @Test
    @DisplayName("a backend with OCR only gives the lines, and so the fields")
    void ocrOnly() {
        VisionBackend backend = mock(VisionBackend.class, withSettings().extraInterfaces(OcrCapability.class));
        when(((OcrCapability) backend).extractText(any())).thenReturn(List.of(
            new OcrCapability.TextDetection("Name: Example", 0.9, null, Map.of())));

        DocumentContent content = new VisionTemplate(backend).readDocument(IMAGE);

        assertThat(content.fields()).hasSize(1);
        assertThat(content.tables()).isEmpty();
    }

    @Test
    @DisplayName("a backend that reads no text refuses")
    void unsupported() {
        VisionBackend backend = mock(VisionBackend.class);
        when(backend.getBackendId()).thenReturn("stub");

        assertThatThrownBy(() -> new VisionTemplate(backend).readDocument(IMAGE))
            .isInstanceOf(VisionUnsupportedException.class);
    }
}
