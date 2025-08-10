package com.deepface.utils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

public final class ImageUtils {

    private ImageUtils() {}

    public static BufferedImage loadImage(String path) throws IOException {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("image path must not be null or blank");
        }
        File file = new File(path);
        if (!file.isFile()) {
            throw new IOException("image path does not point to a file: " + path);
        }
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("unsupported or corrupt image format: " + path);
        }
        return img;
    }

    public static BufferedImage loadImage(byte[] data) throws IOException {
        if (data == null) throw new IllegalArgumentException("image bytes must not be null");
        try (InputStream is = new ByteArrayInputStream(data)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) throw new IOException("unsupported or corrupt image bytes");
            return img;
        }
    }

    public static BufferedImage loadImage(InputStream is) throws IOException {
        if (is == null) throw new IllegalArgumentException("input stream must not be null");
        BufferedImage img = ImageIO.read(is);
        if (img == null) throw new IOException("unsupported or corrupt image stream");
        return img;
    }

    public static void saveImage(BufferedImage img, String path) throws IOException {
        if (img == null) throw new IllegalArgumentException("image must not be null");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path must not be null or blank");
        String format = inferFormat(path);
        if (!ImageIO.write(img, format, new File(path))) {
            throw new IOException("failed to write image with format: " + format);
        }
    }

    public static BufferedImage resizeImage(BufferedImage img, int width, int height) {
        if (img == null) throw new IllegalArgumentException("image must not be null");
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("width and height must be positive");
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private static String inferFormat(String path) {
        int idx = path.lastIndexOf('.');
        if (idx > 0 && idx < path.length() - 1) {
            return path.substring(idx + 1).toLowerCase();
        }
        return "png";
    }
}
