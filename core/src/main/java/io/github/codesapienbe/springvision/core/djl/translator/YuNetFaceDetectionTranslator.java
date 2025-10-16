package io.github.codesapienbe.springvision.core.djl.translator;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.DataType;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom translator for YuNet face detection model.
 *
 * <p>YuNet is a lightweight face detection model from OpenCV Zoo that provides
 * accurate face detection with minimal computational overhead.</p>
 *
 * <p>Model details:</p>
 * <ul>
 *   <li>Input: RGB image, resized to 320x320</li>
 *   <li>Output: Face bounding boxes with confidence scores</li>
 *   <li>Format: ONNX</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
public class YuNetFaceDetectionTranslator implements Translator<Image, DetectedObjects> {

    private static final int INPUT_WIDTH = 320;
    private static final int INPUT_HEIGHT = 320;
    private static final float CONFIDENCE_THRESHOLD = 0.5f;

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        // Resize image to model input size
        Image resized = input.resize(INPUT_WIDTH, INPUT_HEIGHT, false);

        // Convert to NDArray and normalize
        NDArray array = resized.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);

        // Convert to float and normalize to [0, 1]
        array = array.toType(DataType.FLOAT32, false);
        array = array.div(255.0f);

        // Transpose from HWC to CHW format (required by most models)
        array = array.transpose(2, 0, 1);

        // Add batch dimension
        array = array.expandDims(0);

        return new NDList(array);
    }

    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {
        // YuNet output format: [batch, num_detections, 15]
        // 15 = [x, y, w, h, x_re, y_re, x_le, y_le, x_nt, y_nt, x_rcm, y_rcm, x_lcm, y_lcm, score]
        NDArray output = list.get(0);

        List<String> classNames = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();
        List<BoundingBox> boundingBoxes = new ArrayList<>();

        // Get number of detections
        long numDetections = output.getShape().get(1);

        for (int i = 0; i < numDetections; i++) {
            NDArray detection = output.get(0, i);

            float x = detection.getFloat(0);
            float y = detection.getFloat(1);
            float width = detection.getFloat(2);
            float height = detection.getFloat(3);
            float score = detection.getFloat(14); // Confidence score at index 14

            // Filter by confidence threshold
            if (score >= CONFIDENCE_THRESHOLD) {
                // Normalize coordinates to [0, 1]
                double normX = x / INPUT_WIDTH;
                double normY = y / INPUT_HEIGHT;
                double normWidth = width / INPUT_WIDTH;
                double normHeight = height / INPUT_HEIGHT;

                classNames.add("face");
                probabilities.add((double) score);
                boundingBoxes.add(new Rectangle(normX, normY, normWidth, normHeight));
            }
        }

        return new DetectedObjects(classNames, probabilities, boundingBoxes);
    }

    @Override
    public Batchifier getBatchifier() {
        return Batchifier.STACK;
    }
}

