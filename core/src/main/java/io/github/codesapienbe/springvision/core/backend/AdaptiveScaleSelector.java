package io.github.codesapienbe.springvision.core.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptive scale selector for multi-scale face detection.
 *
 * <p>Instead of using fixed scales for all images, this selector chooses
 * scales intelligently based on image size and expected face sizes.
 * This improves both speed and detection quality:</p>
 * <ul>
 *   <li>Small images: Fewer scales, focus on larger faces</li>
 *   <li>Medium images: Standard multi-scale approach</li>
 *   <li>Large images: More scales to detect small distant faces</li>
 * </ul>
 *
 * <p>This reduces unnecessary processing while maintaining detection quality.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class AdaptiveScaleSelector {

    // Image size thresholds
    private static final int SMALL_IMAGE_THRESHOLD = 640;
    private static final int MEDIUM_IMAGE_THRESHOLD = 1920;

    // Scale configurations for different image sizes
    private static final double[] SMALL_IMAGE_SCALES = {1.0, 0.9, 1.1};
    private static final double[] MEDIUM_IMAGE_SCALES = {1.0, 0.85, 1.15, 0.75, 1.25};
    private static final double[] LARGE_IMAGE_SCALES = {1.0, 0.8, 0.9, 1.1, 1.2, 0.7, 1.3};
    private static final double[] XLARGE_IMAGE_SCALES = {1.0, 0.75, 0.85, 0.95, 1.05, 1.15, 1.25, 0.65, 1.35};

    /**
     * Selects optimal scales based on image dimensions.
     *
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @return array of scale factors to use for detection
     */
    public static double[] selectScales(int imageWidth, int imageHeight) {
        int minDim = Math.min(imageWidth, imageHeight);

        // Very small images: minimal scales
        if (minDim < SMALL_IMAGE_THRESHOLD) {
            return SMALL_IMAGE_SCALES;
        }

        // Medium images: standard scales
        if (minDim < MEDIUM_IMAGE_THRESHOLD) {
            return MEDIUM_IMAGE_SCALES;
        }

        // Large images: more scales
        if (minDim < 3840) { // Below 4K
            return LARGE_IMAGE_SCALES;
        }

        // Extra large images (4K+): maximum scales
        return XLARGE_IMAGE_SCALES;
    }

    /**
     * Selects optimal scales with consideration for expected face sizes.
     *
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @param minFaceSize minimum expected face size (in pixels)
     * @param maxFaceSize maximum expected face size (in pixels)
     * @return array of scale factors to use for detection
     */
    public static double[] selectScalesWithFaceSize(int imageWidth, int imageHeight,
                                                    int minFaceSize, int maxFaceSize) {
        int minDim = Math.min(imageWidth, imageHeight);

        // Calculate face size ratios
        double minFaceRatio = (double) minFaceSize / minDim;
        double maxFaceRatio = (double) maxFaceSize / minDim;

        // If faces are expected to be large, use fewer/larger scales
        if (minFaceRatio > 0.3) {
            return new double[]{1.0, 1.1, 1.2};
        }

        // If faces are expected to be small, use more/smaller scales
        if (maxFaceRatio < 0.1) {
            return new double[]{1.0, 0.8, 0.7, 0.6, 0.9, 1.1};
        }

        // Default to standard adaptive selection
        return selectScales(imageWidth, imageHeight);
    }

    /**
     * Estimates the optimal number of scales based on image complexity.
     *
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @return recommended number of scales
     */
    public static int estimateOptimalScaleCount(int imageWidth, int imageHeight) {
        int minDim = Math.min(imageWidth, imageHeight);

        if (minDim < SMALL_IMAGE_THRESHOLD) return 3;
        if (minDim < MEDIUM_IMAGE_THRESHOLD) return 5;
        if (minDim < 3840) return 7;
        return 9;
    }

    /**
     * Generates custom scales with specified count and range.
     *
     * @param count    number of scales to generate
     * @param minScale minimum scale factor
     * @param maxScale maximum scale factor
     * @return array of evenly distributed scale factors
     */
    public static double[] generateCustomScales(int count, double minScale, double maxScale) {
        if (count <= 1) {
            return new double[]{1.0};
        }

        double[] scales = new double[count];

        // Always include 1.0 (original size)
        int centerIndex = count / 2;
        scales[centerIndex] = 1.0;

        // Generate scales below 1.0
        double step = (1.0 - minScale) / (centerIndex + 1);
        for (int i = 0; i < centerIndex; i++) {
            scales[i] = minScale + step * (centerIndex - i);
        }

        // Generate scales above 1.0
        step = (maxScale - 1.0) / (count - centerIndex);
        for (int i = centerIndex + 1; i < count; i++) {
            scales[i] = 1.0 + step * (i - centerIndex);
        }

        return scales;
    }

    /**
     * Gets a scale description for logging and debugging.
     *
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @return human-readable description of the selected scale strategy
     */
    public static String getScaleDescription(int imageWidth, int imageHeight) {
        int minDim = Math.min(imageWidth, imageHeight);

        if (minDim < SMALL_IMAGE_THRESHOLD) {
            return "Small image - using minimal scales (3)";
        } else if (minDim < MEDIUM_IMAGE_THRESHOLD) {
            return "Medium image - using standard scales (5)";
        } else if (minDim < 3840) {
            return "Large image - using extended scales (7)";
        } else {
            return "Extra large image (4K+) - using maximum scales (9)";
        }
    }

    /**
     * Checks if multi-scale detection is beneficial for the given image size.
     *
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @return true if multi-scale detection should be used
     */
    public static boolean shouldUseMultiScale(int imageWidth, int imageHeight) {
        int minDim = Math.min(imageWidth, imageHeight);

        // Very small images don't benefit much from multi-scale
        return minDim >= 320;
    }

    /**
     * Calculates optimal minimum face size based on image dimensions.
     *
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @return recommended minimum face size in pixels
     */
    public static int calculateMinFaceSize(int imageWidth, int imageHeight) {
        int minDim = Math.min(imageWidth, imageHeight);

        // Use 3% of the smaller dimension as minimum
        int minFaceSize = (int) (minDim * 0.03);

        // Clamp to reasonable bounds
        return Math.max(20, Math.min(100, minFaceSize));
    }

    /**
     * Calculates optimal maximum face size based on image dimensions.
     *
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @return recommended maximum face size in pixels
     */
    public static int calculateMaxFaceSize(int imageWidth, int imageHeight) {
        int minDim = Math.min(imageWidth, imageHeight);

        // Use 80% of the smaller dimension as maximum
        int maxFaceSize = (int) (minDim * 0.8);

        // Ensure it's larger than min face size
        return Math.max(200, maxFaceSize);
    }

    /**
     * Generates a scale pyramid for the given image dimensions.
     * Returns scales that progressively downsample the image.
     *
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @param minScale    minimum scale factor (typically 0.5-0.7)
     * @param scaleFactor scale factor between levels (typically 1.1-1.2)
     * @return array of scale factors forming a pyramid
     */
    public static double[] generateScalePyramid(int imageWidth, int imageHeight,
                                                double minScale, double scaleFactor) {
        List<Double> scales = new ArrayList<>();

        // Start with original size
        double currentScale = 1.0;
        scales.add(currentScale);

        // Generate larger scales
        currentScale = scaleFactor;
        while (currentScale <= 1.5) {
            scales.add(currentScale);
            currentScale *= scaleFactor;
        }

        // Generate smaller scales
        currentScale = 1.0 / scaleFactor;
        while (currentScale >= minScale) {
            scales.add(0, currentScale); // Add to beginning
            currentScale /= scaleFactor;
        }

        // Convert to array and sort
        return scales.stream()
            .sorted()
            .mapToDouble(Double::doubleValue)
            .toArray();
    }

    /**
     * Filters out unnecessary scales based on minimum and maximum face sizes.
     *
     * @param scales      array of scale factors
     * @param imageWidth  width of the image
     * @param imageHeight height of the image
     * @param minFaceSize minimum expected face size in pixels
     * @param maxFaceSize maximum expected face size in pixels
     * @return filtered array of scale factors
     */
    public static double[] filterScalesByFaceSize(double[] scales, int imageWidth,
                                                  int imageHeight, int minFaceSize,
                                                  int maxFaceSize) {
        List<Double> filteredScales = new ArrayList<>();

        for (double scale : scales) {
            // Calculate effective face size at this scale
            int scaledMinDim = (int) (Math.min(imageWidth, imageHeight) * scale);

            // Keep scales where faces would be within the expected size range
            if (scaledMinDim >= minFaceSize * 0.5 && scaledMinDim <= maxFaceSize * 2.0) {
                filteredScales.add(scale);
            }
        }

        // Ensure we have at least one scale
        if (filteredScales.isEmpty()) {
            filteredScales.add(1.0);
        }

        return filteredScales.stream()
            .mapToDouble(Double::doubleValue)
            .toArray();
    }
}
