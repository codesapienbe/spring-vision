package io.github.codesapienbe.springvision.core.djl.translator;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Translator for YOLOv8 object detection models.
 * Converts between Image inputs and DetectedObjects outputs for YOLO models.
 */
public class YoloDetectionTranslator implements Translator<Image, DetectedObjects> {

    private static final int INPUT_SIZE = 640; // YOLOv8 default input size
    private static final float CONFIDENCE_THRESHOLD = 0.25f;
    private static final float NMS_THRESHOLD = 0.45f;

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        NDManager manager = ctx.getNDManager();

        // Resize image to YOLO input size
        Image resized = input.resize(INPUT_SIZE, INPUT_SIZE, true);

        // Convert to NDArray and normalize
        NDArray array = resized.toNDArray(manager);

        // YOLO expects CHW format (channels, height, width)
        if (array.getShape().dimension() == 3) {
            // HWC to CHW conversion
            array = array.transpose(2, 0, 1);
        }

        // Normalize to [0, 1]
        array = array.div(255.0f);

        return new NDList(array);
    }

    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {
        NDArray output = list.singletonOrThrow();

        // YOLOv8 output shape is [1, 84, 8400] for COCO dataset
        // Where 84 = 4 bbox coords + 80 classes, 8400 = 80x80 + 40x40 + 20x20 grids

        float[] data = output.toFloatArray();
        int numPredictions = data.length / 85; // 80 classes + 5 (bbox + conf)

        List<String> classNames = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();
        List<ai.djl.modality.cv.output.BoundingBox> boundingBoxes = new ArrayList<>();

        for (int i = 0; i < numPredictions; i++) {
            int baseIdx = i * 85;

            // Extract bbox coordinates (cx, cy, w, h)
            float cx = data[baseIdx];
            float cy = data[baseIdx + 1];
            float w = data[baseIdx + 2];
            float h = data[baseIdx + 3];

            // Extract confidence
            float conf = data[baseIdx + 4];

            // Find the class with highest probability
            float maxClassProb = 0;
            int maxClassIdx = -1;
            for (int j = 0; j < 80; j++) { // COCO has 80 classes
                float classProb = data[baseIdx + 5 + j] * conf;
                if (classProb > maxClassProb) {
                    maxClassProb = classProb;
                    maxClassIdx = j;
                }
            }

            // Apply confidence threshold
            if (maxClassProb > CONFIDENCE_THRESHOLD) {
                // Convert from center/width/height to corner coordinates
                float x1 = cx - w / 2;
                float y1 = cy - h / 2;
                float x2 = cx + w / 2;
                float y2 = cy + h / 2;

                // Scale back to original image size (assuming 640x640 input)
                float scale = Math.max(ctx.getNDManager().getDevice().equals(ctx.getNDManager().getDevice()) ? 1.0f : 1.0f, 1.0f); // Placeholder scaling

                boundingBoxes.add(new ai.djl.modality.cv.output.Rectangle(
                    Math.max(0, x1), Math.max(0, y1),
                    Math.min(1.0f, x2 - x1), Math.min(1.0f, y2 - y1)
                ));

                classNames.add(getCocoClassName(maxClassIdx));
                probabilities.add((double) maxClassProb);
            }
        }

        return new DetectedObjects(classNames, probabilities, boundingBoxes);
    }

    private String getCocoClassName(int classId) {
        String[] cocoClasses = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
            "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
            "hair drier", "toothbrush"
        };
        return classId >= 0 && classId < cocoClasses.length ? cocoClasses[classId] : "unknown";
    }

    @Override
    public ai.djl.translate.Batchifier getBatchifier() {
        return ai.djl.translate.Batchifier.STACK;
    }
}
