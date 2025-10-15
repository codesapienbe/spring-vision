package io.github.codesapienbe.springvision.vllm.onnx;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import io.github.codesapienbe.springvision.core.ImageData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Production-grade DJL Translator for vision models with strict pre/post-processing contracts.
 *
 * <p>This translator implements the critical data contract between Python-based quantization
 * calibration (CalibrationDataReader) and Java inference. The pre-processing logic must
 * EXACTLY replicate the transformations used during static quantization to maintain accuracy.</p>
 *
 * <p>Key Contract Points:</p>
 * <ul>
 *   <li>Image resizing to model input dimensions (e.g., 224x224 for SqueezeNet)</li>
 *   <li>Channel format conversion (HWC -> NCHW for CNNs)</li>
 *   <li>Precise normalization (e.g., pixel values / 255.0)</li>
 *   <li>Data type consistency (float32)</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see ai.djl.translate.Translator
 * @since 1.0.0
 */
public class VisionModelTranslator implements Translator<ImageData, float[]> {

    private final int inputWidth;
    private final int inputHeight;
    private final boolean normalize;
    private final float normalizationFactor;
    private final String[] classLabels;

    /**
     * Creates a translator with specified configuration.
     *
     * @param inputWidth          target width for input images
     * @param inputHeight         target height for input images
     * @param normalize           whether to normalize pixel values
     * @param normalizationFactor normalization factor (typically 255.0)
     * @param classLabels         array of class labels for classification
     */
    public VisionModelTranslator(int inputWidth, int inputHeight, boolean normalize,
                                 float normalizationFactor, String[] classLabels) {
        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        this.normalize = normalize;
        this.normalizationFactor = normalizationFactor;
        this.classLabels = classLabels != null ? classLabels : new String[0];
    }

    /**
     * Creates a default translator for standard ImageNet models (224x224, 1000 classes).
     */
    public VisionModelTranslator() {
        this(224, 224, true, 255.0f, loadImageNetLabels());
    }

    /**
     * Pre-processing: Transform ImageData to NDArray tensor.
     *
     * <p>This method implements the CRITICAL data contract established during Python-based
     * static quantization. Any deviation from the CalibrationDataReader pre-processing
     * will result in accuracy degradation or functional failure.</p>
     *
     * <p>Processing Steps (MUST match Python calibration):</p>
     * <ol>
     *   <li>Decode image bytes to BufferedImage</li>
     *   <li>Resize to model input dimensions (e.g., 224x224)</li>
     *   <li>Convert to NDArray with correct channel format</li>
     *   <li>Normalize pixel values (typically divide by 255.0)</li>
     *   <li>Transform from HWC to NCHW format for CNN models</li>
     *   <li>Add batch dimension (NCHW -> BNCHW where B=1)</li>
     * </ol>
     *
     * @param ctx   translator context containing NDManager
     * @param input the image data to process
     * @return NDList containing the preprocessed tensor
     * @throws IOException if image processing fails
     */
    @Override
    public NDList processInput(TranslatorContext ctx, ImageData input) throws IOException {
        NDManager manager = ctx.getNDManager();

        // Step 1: Decode image bytes to BufferedImage
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(input.data()));
        if (bufferedImage == null) {
            throw new IOException("Failed to decode image data");
        }

        // Step 2: Resize to model input dimensions
        // This MUST match the resize operation in CalibrationDataReader
        BufferedImage resizedImage = resizeImage(bufferedImage, inputWidth, inputHeight);

        // Step 3: Convert to NDArray
        // Extract pixel data in RGB format (3 channels)
        int width = resizedImage.getWidth();
        int height = resizedImage.getHeight();
        float[][][] pixelData = new float[height][width][3]; // HWC format

        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                int rgb = resizedImage.getRGB(w, h);
                // Extract RGB channels (order matters!)
                pixelData[h][w][0] = ((rgb >> 16) & 0xFF); // Red
                pixelData[h][w][1] = ((rgb >> 8) & 0xFF);  // Green
                pixelData[h][w][2] = (rgb & 0xFF);         // Blue
            }
        }

        // Step 4: Create NDArray from pixel data
        // Flatten to 1D array for DJL
        float[] flatData = new float[height * width * 3];
        int idx = 0;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                for (int c = 0; c < 3; c++) {
                    flatData[idx++] = pixelData[h][w][c];
                }
            }
        }

        NDArray array = manager.create(flatData, new ai.djl.ndarray.types.Shape(height, width, 3));

        // Step 5: Normalize pixel values (CRITICAL: must match Python normalization)
        if (normalize) {
            array = array.div(normalizationFactor); // Typically / 255.0
        }

        // Step 6: Transform from HWC to NCHW format (required for CNN models)
        // HWC: [Height, Width, Channels] -> NCHW: [Batch, Channels, Height, Width]
        array = array.transpose(2, 0, 1); // CHW
        array = array.expandDims(0);      // NCHW with batch=1

        return new NDList(array);
    }

    /**
     * Post-processing: Transform model output to classification probabilities.
     *
     * <p>Converts raw logits to probabilities using softmax and maps to class labels.</p>
     *
     * @param ctx    translator context
     * @param output model output tensors (raw logits)
     * @return probability array for each class
     */
    @Override
    public float[] processOutput(TranslatorContext ctx, NDList output) {
        // Extract primary output tensor (classification logits)
        NDArray logits = output.singletonOrThrow();

        // Apply softmax to convert logits to probabilities
        NDArray probabilities = logits.softmax(0);

        // Convert to float array
        return probabilities.toFloatArray();
    }

    @Override
    public Batchifier getBatchifier() {
        return Batchifier.STACK;
    }

    /**
     * Resizes image to target dimensions using high-quality scaling.
     *
     * @param source source image
     * @param width  target width
     * @param height target height
     * @return resized image
     */
    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = resized.createGraphics();

        // Use high-quality rendering for better results
        graphics.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();

        return resized;
    }

    /**
     * Loads ImageNet class labels (1000 classes).
     * In production, load from a proper resource file.
     *
     * @return array of class labels
     */
    private static String[] loadImageNetLabels() {
        // Placeholder - in production, load from resources/imagenet_labels.txt
        String[] labels = new String[1000];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = "class_" + i;
        }
        return labels;
    }

    /**
     * Gets the class label for a given index.
     *
     * @param index class index
     * @return class label
     */
    public String getClassLabel(int index) {
        if (index >= 0 && index < classLabels.length) {
            return classLabels[index];
        }
        return "unknown_" + index;
    }

    /**
     * Gets the top-K predictions from probabilities.
     *
     * @param probabilities probability array
     * @param k             number of top predictions to return
     * @return array of top-K class indices
     */
    public int[] getTopK(float[] probabilities, int k) {
        k = Math.min(k, probabilities.length);
        int[] indices = new int[k];
        float[] topProbs = new float[k];

        for (int i = 0; i < probabilities.length; i++) {
            float prob = probabilities[i];
            for (int j = 0; j < k; j++) {
                if (prob > topProbs[j]) {
                    // Shift lower probabilities down
                    System.arraycopy(topProbs, j, topProbs, j + 1, k - j - 1);
                    System.arraycopy(indices, j, indices, j + 1, k - j - 1);
                    topProbs[j] = prob;
                    indices[j] = i;
                    break;
                }
            }
        }

        return indices;
    }
}
