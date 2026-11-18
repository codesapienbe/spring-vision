package io.github.codesapienbe.springvision.core.opencv;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Draws test scenes with a Belgian-style plate (red characters and border on white, blue EU band
 * with a white "B") on a cluttered background. No real plate photo is used: plates are personal
 * data.
 */
public final class SyntheticPlates {

    /** Where {@link #scene(String)} draws the plate, in pixels: x, y, width, height. */
    public static final int[] PLATE_BOX = {460, 470, 380, 82};

    private SyntheticPlates() {
    }

    public static byte[] scene(String plateText) throws IOException {
        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(70, 90, 120), 0, 720, new Color(40, 40, 45)));
        g.fillRect(0, 0, 1280, 720);
        // car body and bumper
        g.setColor(new Color(25, 30, 40));
        g.fillRoundRect(240, 300, 820, 300, 60, 60);
        g.setColor(new Color(60, 60, 65));
        g.fillRect(260, 560, 780, 40);
        if (plateText != null) {
            drawPlate(g, plateText);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /** {@code png} blurred (5x5 box), downscaled to 960x540 and re-encoded as JPEG at quality 0.6. */
    public static byte[] degraded(byte[] png) throws IOException {
        BufferedImage source = ImageIO.read(new java.io.ByteArrayInputStream(png));
        float[] box = new float[25];
        java.util.Arrays.fill(box, 1f / 25);
        BufferedImage blurred = new java.awt.image.ConvolveOp(new java.awt.image.Kernel(5, 5, box),
            java.awt.image.ConvolveOp.EDGE_NO_OP, null).filter(source, null);
        BufferedImage small = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.drawImage(blurred, 0, 0, 960, 540, null);
        g.dispose();
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        javax.imageio.ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(0.6f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (javax.imageio.stream.ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new javax.imageio.IIOImage(small, null, null), params);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static void drawPlate(Graphics2D g, String text) {
        int x = PLATE_BOX[0];
        int y = PLATE_BOX[1];
        int w = PLATE_BOX[2];
        int h = PLATE_BOX[3];
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(new Color(0, 51, 153));
        g.fillRect(x + 4, y + 4, 36, h - 8);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString("B", x + 14, y + h - 16);
        Color red = new Color(200, 16, 46);
        g.setColor(red);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(x + 2, y + 2, w - 4, h - 4, 10, 10);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 54));
        FontMetrics fm = g.getFontMetrics();
        int textX = x + 48 + (w - 56 - fm.stringWidth(text)) / 2;
        int textY = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, textX, textY);
    }
}
