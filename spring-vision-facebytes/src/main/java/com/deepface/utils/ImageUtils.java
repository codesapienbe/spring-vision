package com.deepface.utils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class ImageUtils {

    private ImageUtils() {}

    public static BufferedImage loadImage(String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    public static void saveImage(BufferedImage img, String path) throws IOException {
        String format = inferFormat(path);
        ImageIO.write(img, format, new File(path));
    }

    public static BufferedImage resizeImage(BufferedImage img, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private static String inferFormat(String path) {
        int idx = path.lastIndexOf('.')
            ;
        if (idx > 0 && idx < path.length() - 1) {
            return path.substring(idx + 1).toLowerCase();
        }
        return "png";
    }
}
