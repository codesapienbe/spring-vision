package io.github.codesapienbe.springvision.core.djl;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Utility class for creating realistic test images for integration testing.
 * Provides methods to create images with specific content that should trigger
 * detections from various computer vision models.
 */
public class TestImageUtils {

    private static final Random RANDOM = new Random(42); // Deterministic for tests

    /**
     * Creates a test image with a colored rectangle that could be detected as an object.
     */
    public static ImageData createRectangleImage(int width, int height, Color rectColor) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Anti-aliasing for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(0, 0, width, height);

        // Rectangle object
        g2d.setColor(rectColor);
        int rectWidth = width / 3;
        int rectHeight = height / 3;
        int x = (width - rectWidth) / 2;
        int y = (height - rectHeight) / 2;
        g2d.fillRect(x, y, rectWidth, rectHeight);

        g2d.dispose();
        return convertToImageData(image, "png");
    }

    /**
     * Creates a test image with a simple geometric shape that object detection models should detect.
     */
    public static ImageData createGeometricShapeImage(int width, int height, String shape) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Shape
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3.0f));

        int centerX = width / 2;
        int centerY = height / 2;
        int size = Math.min(width, height) / 4;

        switch (shape.toLowerCase()) {
            case "circle":
                g2d.fillOval(centerX - size, centerY - size, size * 2, size * 2);
                break;
            case "triangle":
                int[] xPoints = {centerX, centerX - size, centerX + size};
                int[] yPoints = {centerY - size, centerY + size, centerY + size};
                g2d.fillPolygon(xPoints, yPoints, 3);
                break;
            case "square":
            default:
                g2d.fillRect(centerX - size, centerY - size, size * 2, size * 2);
                break;
        }

        g2d.dispose();
        return convertToImageData(image, "png");
    }

    /**
     * Creates a test image with text for OCR testing.
     */
    public static ImageData createTextImage(String text, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Text
        g2d.setColor(Color.BLACK);
        Font font = new Font("Arial", Font.BOLD, 24);
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int x = (width - textWidth) / 2;
        int y = (height + textHeight) / 2 - fm.getDescent();

        g2d.drawString(text, x, y);

        g2d.dispose();
        return convertToImageData(image, "png");
    }

    /**
     * Creates a test image with a simple face-like pattern for face detection testing.
     * Note: This won't fool advanced face detectors, but can test basic functionality.
     */
    public static ImageData createSimpleFaceImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Skin tone background
        g2d.setColor(new Color(255, 218, 185)); // Peach puff
        g2d.fillRect(0, 0, width, height);

        // Face oval
        g2d.setColor(new Color(255, 228, 196)); // Bisque
        int faceWidth = width * 3 / 4;
        int faceHeight = height * 3 / 4;
        int faceX = (width - faceWidth) / 2;
        int faceY = (height - faceHeight) / 2;
        g2d.fillOval(faceX, faceY, faceWidth, faceHeight);

        // Eyes
        g2d.setColor(Color.BLACK);
        int eyeSize = faceWidth / 10;
        int eyeY = faceY + faceHeight / 3;
        int leftEyeX = faceX + faceWidth / 3 - eyeSize / 2;
        int rightEyeX = faceX + 2 * faceWidth / 3 - eyeSize / 2;
        g2d.fillOval(leftEyeX, eyeY, eyeSize, eyeSize);
        g2d.fillOval(rightEyeX, eyeY, eyeSize, eyeSize);

        // Mouth
        g2d.setColor(Color.RED);
        int mouthWidth = faceWidth / 4;
        int mouthHeight = faceHeight / 8;
        int mouthX = faceX + faceWidth / 2 - mouthWidth / 2;
        int mouthY = faceY + 2 * faceHeight / 3;
        g2d.fillOval(mouthX, mouthY, mouthWidth, mouthHeight);

        g2d.dispose();
        return convertToImageData(image, "png");
    }

    /**
     * Creates a test image with a person-like silhouette for pose estimation testing.
     */
    public static ImageData createPersonSilhouetteImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Person silhouette
        g2d.setColor(Color.BLACK);

        // Head
        int headSize = width / 8;
        int headX = width / 2 - headSize / 2;
        int headY = height / 6;
        g2d.fillOval(headX, headY, headSize, headSize);

        // Body
        int bodyWidth = headSize * 2 / 3;
        int bodyHeight = height / 3;
        int bodyX = width / 2 - bodyWidth / 2;
        int bodyY = headY + headSize;
        g2d.fillRect(bodyX, bodyY, bodyWidth, bodyHeight);

        // Arms
        int armLength = headSize;
        int armY = bodyY + headSize / 2;
        g2d.fillRect(bodyX - armLength, armY, armLength, headSize / 4); // Left arm
        g2d.fillRect(bodyX + bodyWidth, armY, armLength, headSize / 4); // Right arm

        // Legs
        int legY = bodyY + bodyHeight;
        int legWidth = headSize / 3;
        int legHeight = height / 4;
        g2d.fillRect(bodyX, legY, legWidth, legHeight); // Left leg
        g2d.fillRect(bodyX + bodyWidth - legWidth, legY, legWidth, legHeight); // Right leg

        g2d.dispose();
        return convertToImageData(image, "png");
    }

    /**
     * Creates a test image with a barcode pattern.
     */
    public static ImageData createBarcodeImage(String barcodeText, int width, int height) throws IOException {
        // For now, create a simple striped pattern that might be detected as a barcode
        // In a real implementation, you'd use a barcode generation library
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Simple barcode-like pattern
        g2d.setColor(Color.BLACK);
        int barWidth = width / 20;
        int barHeight = height * 3 / 4;
        int startY = height / 8;

        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) { // Alternate black and white bars
                int x = i * barWidth + width / 4;
                g2d.fillRect(x, startY, barWidth, barHeight);
            }
        }

        // Add text below
        g2d.setColor(Color.BLACK);
        Font font = new Font("Arial", Font.PLAIN, 12);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(barcodeText);
        int textX = (width - textWidth) / 2;
        int textY = startY + barHeight + 20;
        g2d.drawString(barcodeText, textX, textY);

        g2d.dispose();
        return convertToImageData(image, "png");
    }

    /**
     * Creates a test image with random noise for testing robustness.
     */
    public static ImageData createNoiseImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Fill with random colored pixels
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color randomColor = new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
                image.setRGB(x, y, randomColor.getRGB());
            }
        }

        g2d.dispose();
        return convertToImageData(image, "png");
    }

    /**
     * Creates an empty/blank image.
     */
    public static ImageData createEmptyImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.dispose();
        return convertToImageData(image, "png");
    }

    private static ImageData convertToImageData(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return ImageData.fromBytes(baos.toByteArray());
    }
}
