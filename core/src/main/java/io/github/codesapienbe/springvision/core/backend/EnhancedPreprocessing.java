package io.github.codesapienbe.springvision.core.backend;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_core.*;

/**
 * Enhanced preprocessing utilities for improved face detection quality.
 *
 * <p>This class provides advanced image preprocessing techniques that significantly
 * improve detection accuracy in challenging conditions:</p>
 * <ul>
 *   <li>CLAHE (Contrast Limited Adaptive Histogram Equalization) for better local contrast</li>
 *   <li>Multi-scale Retinex for illumination normalization</li>
 *   <li>Gamma correction for exposure adjustment</li>
 *   <li>Bilateral filtering for noise reduction while preserving edges</li>
 * </ul>
 *
 * <p>These techniques are especially effective for:</p>
 * <ul>
 *   <li>Low-light or unevenly lit images</li>
 *   <li>High-contrast scenes (strong shadows)</li>
 *   <li>Overexposed or underexposed images</li>
 *   <li>Noisy images from low-quality cameras</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class EnhancedPreprocessing {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedPreprocessing.class);

    // CLAHE parameters
    private static final double CLAHE_CLIP_LIMIT = 2.0;
    private static final Size CLAHE_TILE_SIZE = new Size(8, 8);

    // Gamma correction parameters
    private static final double GAMMA_DARK = 1.5;    // For dark images
    private static final double GAMMA_BRIGHT = 0.7;  // For bright images
    private static final double GAMMA_NORMAL = 1.0;  // For normal images

    // Illumination thresholds
    private static final double DARK_THRESHOLD = 80.0;
    private static final double BRIGHT_THRESHOLD = 180.0;

    /**
     * Applies CLAHE (Contrast Limited Adaptive Histogram Equalization) for better local contrast.
     * This is superior to standard histogram equalization for face detection.
     *
     * @param grayImage input grayscale image
     * @return enhanced image with improved local contrast
     */
    public static Mat applyCLAHE(Mat grayImage) {
        if (grayImage == null || grayImage.empty()) {
            return grayImage;
        }

        try {
            org.bytedeco.opencv.opencv_imgproc.CLAHE clahe =
                createCLAHE(CLAHE_CLIP_LIMIT, CLAHE_TILE_SIZE);

            Mat enhanced = new Mat();
            clahe.apply(grayImage, enhanced);
            clahe.close();

            return enhanced;

        } catch (Exception e) {
            logger.debug("CLAHE preprocessing failed, falling back to histogram equalization: {}",
                e.getMessage());

            // Fallback to standard histogram equalization
            Mat enhanced = new Mat();
            equalizeHist(grayImage, enhanced);
            return enhanced;
        }
    }

    /**
     * Applies adaptive gamma correction based on image brightness.
     * Automatically selects appropriate gamma value.
     *
     * @param grayImage input grayscale image
     * @return gamma-corrected image
     */
    public static Mat applyAdaptiveGamma(Mat grayImage) {
        if (grayImage == null || grayImage.empty()) {
            return grayImage;
        }

        try {
            // Compute mean intensity to determine if image is dark or bright
            double meanIntensity = mean(grayImage).get(0);

            double gamma;
            if (meanIntensity < DARK_THRESHOLD) {
                // Dark image: increase brightness
                gamma = GAMMA_DARK;
                logger.debug("Applying gamma correction for dark image: gamma={}", gamma);
            } else if (meanIntensity > BRIGHT_THRESHOLD) {
                // Bright image: decrease brightness
                gamma = GAMMA_BRIGHT;
                logger.debug("Applying gamma correction for bright image: gamma={}", gamma);
            } else {
                // Normal image: no correction needed
                return grayImage;
            }

            return applyGammaCorrection(grayImage, gamma);

        } catch (Exception e) {
            logger.debug("Adaptive gamma correction failed: {}", e.getMessage());
            return grayImage;
        }
    }

    /**
     * Applies gamma correction to normalize exposure.
     *
     * @param grayImage input grayscale image
     * @param gamma     gamma value (> 1.0 brightens, < 1.0 darkens)
     * @return gamma-corrected image
     */
    public static Mat applyGammaCorrection(Mat grayImage, double gamma) {
        if (grayImage == null || grayImage.empty()) {
            return grayImage;
        }

        try {
            // Convert to float [0, 1] range
            Mat normalized = new Mat();
            grayImage.convertTo(normalized, CV_32F, 1.0 / 255.0, 0.0);

            // Apply gamma correction: output = input^gamma
            Mat corrected = new Mat();
            pow(normalized, gamma, corrected);

            // Convert back to 8-bit [0, 255]
            Mat result = new Mat();
            corrected.convertTo(result, CV_8U, 255.0, 0.0);

            // Cleanup
            normalized.releaseReference();
            corrected.releaseReference();

            return result;

        } catch (Exception e) {
            logger.debug("Gamma correction failed: {}", e.getMessage());
            return grayImage;
        }
    }

    /**
     * Applies bilateral filter for noise reduction while preserving edges.
     * This is important for face detection as it reduces noise without blurring facial features.
     *
     * @param image input image (grayscale or color)
     * @return filtered image
     */
    public static Mat applyBilateralFilter(Mat image) {
        if (image == null || image.empty()) {
            return image;
        }

        try {
            Mat filtered = new Mat();

            // Bilateral filter parameters:
            // - d=5: diameter of pixel neighborhood
            // - sigmaColor=50: filter sigma in color space
            // - sigmaSpace=50: filter sigma in coordinate space
            bilateralFilter(image, filtered, 5, 50.0, 50.0);

            return filtered;

        } catch (Exception e) {
            logger.debug("Bilateral filtering failed: {}", e.getMessage());
            return image;
        }
    }

    /**
     * Applies Multi-Scale Retinex for illumination normalization.
     * Excellent for handling uneven lighting conditions.
     *
     * @param grayImage input grayscale image
     * @return illumination-normalized image
     */
    public static Mat applyMultiScaleRetinex(Mat grayImage) {
        if (grayImage == null || grayImage.empty()) {
            return grayImage;
        }

        try {
            // Convert to float in [0, 1] range
            Mat inputFloat = new Mat();
            grayImage.convertTo(inputFloat, CV_32F, 1.0 / 255.0, 0.0);

            // Add small epsilon to avoid log(0)
            double eps = 1e-6;
            Mat epsMat = new Mat(inputFloat.size(), inputFloat.type(), new org.bytedeco.opencv.opencv_core.Scalar(eps));
            add(inputFloat, epsMat, inputFloat);

            // Compute log of input
            Mat logInput = new Mat();
            log(inputFloat, logInput);

            // Accumulate SSR across multiple Gaussian scales
            int[] kernelSizes = {15, 80, 250};
            Mat accumulator = new Mat(logInput.size(), logInput.type(), new org.bytedeco.opencv.opencv_core.Scalar(0));

            for (int kernelSize : kernelSizes) {
                // Ensure kernel size is odd
                if (kernelSize % 2 == 0) {
                    kernelSize++;
                }

                Mat blurred = new Mat();
                GaussianBlur(inputFloat, blurred, new Size(kernelSize, kernelSize), 0);
                add(blurred, epsMat, blurred);

                Mat logBlurred = new Mat();
                log(blurred, logBlurred);

                Mat ssr = new Mat();
                subtract(logInput, logBlurred, ssr);

                add(accumulator, ssr, accumulator);

                // Cleanup
                ssr.releaseReference();
                logBlurred.releaseReference();
                blurred.releaseReference();
            }

            // Average the accumulation
            Mat scaleFactor = new Mat(accumulator.size(), accumulator.type(),
                new org.bytedeco.opencv.opencv_core.Scalar(1.0 / kernelSizes.length));
            multiply(accumulator, scaleFactor, accumulator);

            // Normalize to [0, 255] range
            Mat result = new Mat();
            normalize(accumulator, result, 0, 255, NORM_MINMAX, CV_8U, new Mat());

            // Cleanup
            inputFloat.releaseReference();
            epsMat.releaseReference();
            logInput.releaseReference();
            accumulator.releaseReference();
            scaleFactor.releaseReference();

            return result;

        } catch (Exception e) {
            logger.debug("Multi-Scale Retinex failed: {}", e.getMessage());
            return grayImage;
        }
    }

    /**
     * Applies a complete preprocessing pipeline optimized for face detection.
     * Combines multiple techniques for best results.
     *
     * @param grayImage input grayscale image
     * @return fully preprocessed image ready for detection
     */
    public static Mat applyCompletePipeline(Mat grayImage) {
        if (grayImage == null || grayImage.empty()) {
            return grayImage;
        }

        Mat processed = grayImage;

        try {
            // Step 1: Adaptive gamma correction for exposure normalization
            processed = applyAdaptiveGamma(processed);

            // Step 2: Bilateral filter for noise reduction while preserving edges
            Mat denoised = applyBilateralFilter(processed);
            if (processed != grayImage) {
                processed.releaseReference();
            }
            processed = denoised;

            // Step 3: CLAHE for contrast enhancement
            Mat enhanced = applyCLAHE(processed);
            if (processed != grayImage) {
                processed.releaseReference();
            }
            processed = enhanced;

            return processed;

        } catch (Exception e) {
            logger.warn("Complete preprocessing pipeline failed: {}", e.getMessage());

            // Cleanup on failure
            if (processed != grayImage && processed != null) {
                try {
                    processed.releaseReference();
                } catch (Exception ignored) {
                }
            }

            // Fallback to simple histogram equalization
            Mat fallback = new Mat();
            equalizeHist(grayImage, fallback);
            return fallback;
        }
    }

    /**
     * Applies fast preprocessing for real-time scenarios.
     * Uses only essential operations to minimize latency.
     *
     * @param grayImage input grayscale image
     * @return preprocessed image
     */
    public static Mat applyFastPipeline(Mat grayImage) {
        if (grayImage == null || grayImage.empty()) {
            return grayImage;
        }

        try {
            // Only apply CLAHE for quick contrast enhancement
            return applyCLAHE(grayImage);
        } catch (Exception e) {
            logger.debug("Fast preprocessing failed, using histogram equalization: {}", e.getMessage());
            Mat result = new Mat();
            equalizeHist(grayImage, result);
            return result;
        }
    }

    /**
     * Checks if image needs preprocessing based on histogram analysis.
     * Avoids unnecessary processing for well-exposed images.
     *
     * @param grayImage input grayscale image
     * @return true if preprocessing would benefit detection quality
     */
    public static boolean needsPreprocessing(Mat grayImage) {
        if (grayImage == null || grayImage.empty()) {
            return false;
        }

        try {
            // Compute mean and standard deviation
            double meanIntensity = mean(grayImage).get(0);

            // Check if image is poorly exposed
            boolean isDark = meanIntensity < DARK_THRESHOLD;
            boolean isBright = meanIntensity > BRIGHT_THRESHOLD;

            if (isDark || isBright) {
                return true;
            }

            // Check contrast by estimating standard deviation
            Mat meanMat = new Mat();
            Mat stdMat = new Mat();
            meanStdDev(grayImage, meanMat, stdMat);

            double stdDev = stdMat.ptr(0, 0).get(0);

            meanMat.releaseReference();
            stdMat.releaseReference();

            // Low contrast images benefit from CLAHE
            return stdDev < 40.0;

        } catch (Exception e) {
            logger.debug("Failed to analyze image for preprocessing needs: {}", e.getMessage());
            // When in doubt, apply preprocessing
            return true;
        }
    }

    /**
     * Applies sharpening to enhance edge details for better detection.
     *
     * @param grayImage input grayscale image
     * @param amount    sharpening amount (0.5 to 2.0, default 1.0)
     * @return sharpened image
     */
    public static Mat applySharpen(Mat grayImage, double amount) {
        if (grayImage == null || grayImage.empty()) {
            return grayImage;
        }

        try {
            // Create Gaussian blur
            Mat blurred = new Mat();
            GaussianBlur(grayImage, blurred, new Size(0, 0), 3.0);

            // Unsharp mask: original + amount * (original - blurred)
            Mat result = new Mat();
            addWeighted(grayImage, 1.0 + amount, blurred, -amount, 0, result);

            blurred.releaseReference();

            return result;

        } catch (Exception e) {
            logger.debug("Sharpening failed: {}", e.getMessage());
            return grayImage;
        }
    }

    /**
     * Estimates optimal preprocessing strategy based on image characteristics.
     *
     * @param grayImage input grayscale image
     * @return recommended preprocessing strategy
     */
    public static PreprocessingStrategy estimateStrategy(Mat grayImage) {
        if (grayImage == null || grayImage.empty()) {
            return PreprocessingStrategy.NONE;
        }

        try {
            double meanIntensity = mean(grayImage).get(0);

            // Very dark or very bright images need full pipeline
            if (meanIntensity < DARK_THRESHOLD - 20 || meanIntensity > BRIGHT_THRESHOLD + 20) {
                return PreprocessingStrategy.COMPLETE;
            }

            // Moderately challenging images need standard processing
            if (meanIntensity < DARK_THRESHOLD || meanIntensity > BRIGHT_THRESHOLD) {
                return PreprocessingStrategy.STANDARD;
            }

            // Check contrast
            Mat stdMat = new Mat();
            meanStdDev(grayImage, new Mat(), stdMat);
            double stdDev = stdMat.ptr(0, 0).get(0);
            stdMat.releaseReference();

            if (stdDev < 40.0) {
                return PreprocessingStrategy.FAST;
            }

            // Well-exposed images with good contrast
            return PreprocessingStrategy.NONE;

        } catch (Exception e) {
            logger.debug("Failed to estimate preprocessing strategy: {}", e.getMessage());
            return PreprocessingStrategy.STANDARD;
        }
    }

    /**
     * Preprocessing strategy enum.
     */
    public enum PreprocessingStrategy {
        NONE,       // No preprocessing needed
        FAST,       // Only CLAHE
        STANDARD,   // CLAHE + adaptive gamma
        COMPLETE    // Full pipeline with all enhancements
    }
}
