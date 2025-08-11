package com.deepface.utils;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Validates whether a cropped face image is likely of sufficient quality for embedding generation.
 * <p>
 * This validator applies three lightweight heuristics:
 * <ul>
 *     <li>Minimum width/height</li>
 *     <li>Minimum luminance standard deviation (basic contrast check)</li>
 *     <li>Minimum edge density (approximate sharpness/texture check)</li>
 * </ul>
 * All thresholds are conservative defaults and can be adjusted at runtime via system properties:
 * <pre>
 *   -Dfacebytes.quality.min_width=40
 *   -Dfacebytes.quality.min_height=40
 *   -Dfacebytes.quality.min_stddev=15.0
 *   -Dfacebytes.quality.min_edge_density=0.02
 * </pre>
 * No exceptions are thrown; failures are logged at DEBUG level to avoid noisy logs while preserving observability.
 */
public final class FaceQualityValidator {

    private static final int DEFAULT_MIN_WIDTH = 40;
    private static final int DEFAULT_MIN_HEIGHT = 40;
    private static final double DEFAULT_MIN_STDDEV = 15.0; // on 0..255 luminance
    private static final double DEFAULT_MIN_EDGE_DENSITY = 0.02; // 2% pixels with noticeable gradient

    private final int minWidth;
    private final int minHeight;
    private final double minStdDev;
    private final double minEdgeDensity;

    /**
     * Constructs a validator with thresholds sourced from system properties or safe defaults.
     */
    public FaceQualityValidator() {
        this.minWidth = (int) readDouble("facebytes.quality.min_width", DEFAULT_MIN_WIDTH);
        this.minHeight = (int) readDouble("facebytes.quality.min_height", DEFAULT_MIN_HEIGHT);
        this.minStdDev = readDouble("facebytes.quality.min_stddev", DEFAULT_MIN_STDDEV);
        this.minEdgeDensity = readDouble("facebytes.quality.min_edge_density", DEFAULT_MIN_EDGE_DENSITY);
    }

    /**
     * Returns true if the image passes all quality heuristics.
     *
     * @param image cropped face image (RGB)
     * @return whether the image is considered valid for downstream embedding
     */
    public boolean isValidFace(BufferedImage image) {
        if (image == null) {
            Logs.debug("FaceQualityValidator", "null_image", Map.of());
            return false;
        }
        final int w = image.getWidth();
        final int h = image.getHeight();
        if (w < minWidth || h < minHeight) {
            Logs.debug("FaceQualityValidator", "too_small", Map.of("width", w, "height", h, "min_w", minWidth, "min_h", minHeight));
            return false;
        }

        double[] contrast = computeLuminanceStats(image);
        double stddev = contrast[1];
        if (Double.isNaN(stddev) || stddev < minStdDev) {
            Logs.debug("FaceQualityValidator", "low_contrast", Map.of("stddev", round2(stddev), "min_stddev", minStdDev, "width", w, "height", h));
            return false;
        }

        double edgeDensity = estimateEdgeDensity(image);
        if (Double.isNaN(edgeDensity) || edgeDensity < minEdgeDensity) {
            Logs.debug("FaceQualityValidator", "low_edges", Map.of("edge_density", round4(edgeDensity), "min_edge_density", minEdgeDensity));
            return false;
        }

        return true;
    }

    // Computes mean and standard deviation of luminance (0..255)
    private static double[] computeLuminanceStats(BufferedImage img) {
        final int w = img.getWidth();
        final int h = img.getHeight();
        final int n = w * h;
        if (n == 0) return new double[]{0.0, 0.0};
        double sum = 0.0;
        double sumSq = 0.0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;
                // ITU-R BT.601 luma approximation
                double l = 0.299 * r + 0.587 * g + 0.114 * b;
                sum += l;
                sumSq += l * l;
            }
        }
        double mean = sum / n;
        double variance = Math.max(0.0, (sumSq / n) - (mean * mean));
        double stddev = Math.sqrt(variance);
        return new double[]{mean, stddev};
    }

    // Simple gradient-based edge density estimation
    private static double estimateEdgeDensity(BufferedImage img) {
        final int w = img.getWidth();
        final int h = img.getHeight();
        if (w < 2 || h < 2) return 0.0;
        int edges = 0;
        int total = (w - 1) * (h - 1);
        final int threshold = 10; // gradient threshold on 0..255
        for (int y = 0; y < h - 1; y++) {
            for (int x = 0; x < w - 1; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;
                int l = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);

                int rgbX = img.getRGB(x + 1, y);
                int rx = (rgbX >> 16) & 0xFF;
                int gx = (rgbX >> 8) & 0xFF;
                int bx = (rgbX) & 0xFF;
                int lx = (int) Math.round(0.299 * rx + 0.587 * gx + 0.114 * bx);

                int rgbY = img.getRGB(x, y + 1);
                int ry = (rgbY >> 16) & 0xFF;
                int gy = (rgbY >> 8) & 0xFF;
                int by = (rgbY) & 0xFF;
                int ly = (int) Math.round(0.299 * ry + 0.587 * gy + 0.114 * by);

                int dx = Math.abs(lx - l);
                int dy = Math.abs(ly - l);
                if (dx > threshold || dy > threshold) edges++;
            }
        }
        return edges <= 0 ? 0.0 : (edges / (double) total);
    }

    private static double readDouble(String key, double def) {
        try {
            String v = System.getProperty(key);
            if (v == null || v.isBlank()) return def;
            return Double.parseDouble(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
}
