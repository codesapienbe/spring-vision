package io.github.codesapienbe.springvision.core.util;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ZxingBarcodeScannerTest {

    // QR generation helper moved to TestUtils.generateQrPng

    @Test
    public void testDetectQrCode() throws Exception {
        String payload = "https://example.com/test-qr";
        byte[] png = TestUtils.generateQrPng(payload, 300);
        ImageData imageData = ImageData.fromBytes(png, "image/png");

        List<Detection> detections = ZxingBarcodeScanner.detectBarcodes(imageData);
        assertNotNull(detections);
        assertFalse(detections.isEmpty(), "Expected at least one barcode detection");

        Object attr = detections.get(0).getAttribute("text");
        assertNotNull(attr, "Expected 'text' attribute on detection");
        assertEquals(payload, attr.toString());
    }
}
