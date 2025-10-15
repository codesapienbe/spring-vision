package io.github.codesapienbe.springvision.facebytes.utils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * A utility class for handling common image operations such as loading, saving, and resizing.
 * This class provides static methods to abstract away the complexities of Java's ImageIO and AWT libraries.
 */
public final class ImageUtils {

    private ImageUtils() {
    }

    /**
     * Loads an image from a file path into a {@link BufferedImage}.
     *
     * @param path The file path of the image to load.
     * @return The loaded image as a {@link BufferedImage}.
     * @throws IOException if the file does not exist, is not a file, or is a corrupt or unsupported image format.
     * @throws IllegalArgumentException if the path is null or blank.
     */
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

    /**
     * Loads an image from a byte array into a {@link BufferedImage}.
     *
     * @param data The byte array containing the image data.
     * @return The loaded image.
     * @throws IOException if the image data is in an unsupported or corrupt format.
     * @throws IllegalArgumentException if the byte array is null.
     */
    public static BufferedImage loadImage(byte[] data) throws IOException {
        if (data == null) throw new IllegalArgumentException("image bytes must not be null");
        try (InputStream is = new ByteArrayInputStream(data)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) throw new IOException("unsupported or corrupt image bytes");
            return img;
        }
    }

    /**
     * Loads an image from an {@link InputStream} into a {@link BufferedImage}.
     *
     * @param is The InputStream from which to read the image data.
     * @return The loaded image.
     * @throws IOException if the image data is in an unsupported or corrupt format.
     * @throws IllegalArgumentException if the InputStream is null.
     */
    public static BufferedImage loadImage(InputStream is) throws IOException {
        if (is == null) throw new IllegalArgumentException("input stream must not be null");
        BufferedImage img = ImageIO.read(is);
        if (img == null) throw new IOException("unsupported or corrupt image stream");
        return img;
    }

    /**
     * Saves a {@link BufferedImage} to a file path, inferring the format from the file extension.
     *
     * @param img  The image to save.
     * @param path The file path where the image will be saved.
     * @throws IOException if the image fails to write.
     * @throws IllegalArgumentException if the image or path is null or blank.
     */
    public static void saveImage(BufferedImage img, String path) throws IOException {
        if (img == null) throw new IllegalArgumentException("image must not be null");
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path must not be null or blank");
        String format = inferFormat(path);
        if (!ImageIO.write(img, format, new File(path))) {
            throw new IOException("failed to write image with format: " + format);
        }
    }

    /**
     * Resizes a {@link BufferedImage} to the specified width and height.
     *
     * @param img    The image to resize.
     * @param width  The target width.
     * @param height The target height.
     * @return A new, resized {@link BufferedImage}.
     * @throws IllegalArgumentException if the image is null or if width/height are not positive.
     */
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
