package io.github.codesapienbe.springvision.core.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Test utilities shared across test classes.
 */
public final class TestUtils {

    private TestUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Generate a PNG byte array containing a QR code for the given text.
     *
     * @param text the payload to encode
     * @param size the width/height of the generated image in pixels
     * @return PNG bytes
     * @throws Exception on generation errors
     */
    public static byte[] generateQrPng(String text, int size) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int value = matrix.get(x, y) ? 0x000000 : 0xFFFFFF;
                image.setRGB(x, y, value);
            }
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }
}
