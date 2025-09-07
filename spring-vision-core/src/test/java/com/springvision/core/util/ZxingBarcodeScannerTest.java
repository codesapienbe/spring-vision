package com.springvision.core.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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