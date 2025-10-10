package io.github.codesapienbe.springvision.core.backend;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import io.github.codesapienbe.springvision.core.util.TestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OpenCvVisionBackendBarcodeTest {

    @Test
    public void testDetectBarcodesOpenCvBackend() throws Exception {
        OpenCvVisionBackend backend = new OpenCvVisionBackend();
        try {
            backend.initialize();
        } catch (Exception e) {
            // If initialization failed due to native libs, skip the test
            Assumptions.assumeTrue(false, "OpenCV initialization failed: " + e.getMessage());
        }

        String payload = "https://example.com/opencv-qr";
        byte[] png = TestUtils.generateQrPng(payload, 300);
        ImageData imageData = ImageData.fromBytes(png, "image/png");

        List<Detection> detections = backend.detectBarcodes(imageData);
        assertNotNull(detections);
        assertFalse(detections.isEmpty(), "Expected at least one barcode detection from OpenCv backend");

        Object attr = detections.get(0).getAttribute("text");
        assertNotNull(attr);
        assertEquals(payload, attr.toString());
    }
}
