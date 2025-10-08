package com.springvision.backend.facebytes;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.facebytes.FaceBytesBackend;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FaceBytesVisionBackendBarcodeTest {

    @Test
    public void testFaceBytesDetectBarcodes() throws Exception {
        FaceBytesBackend backend = new FaceBytesBackend();
        String payload = "https://example.com/facebytes-qr";
        byte[] png = com.springvision.core.util.TestUtils.generateQrPng(payload, 300);
        ImageData imageData = ImageData.fromBytes(png, "image/png");

        List<Detection> detections = backend.detectBarcodes(imageData);
        assertNotNull(detections);
        assertFalse(detections.isEmpty(), "Expected barcode detection from FaceBytes backend");

        Object attr = detections.get(0).getAttribute("text");
        assertNotNull(attr);
        assertEquals(payload, attr.toString());
    }
}
