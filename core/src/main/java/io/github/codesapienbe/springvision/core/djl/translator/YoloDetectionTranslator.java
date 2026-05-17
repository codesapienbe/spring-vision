package io.github.codesapienbe.springvision.core.djl.translator;

import java.util.ArrayList;
import java.util.List;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

/**
 * Translator for YOLOv8 object detection models.
 * Converts between Image inputs and DetectedObjects outputs for YOLO models.
 */
public class YoloDetectionTranslator implements Translator<Image, DetectedObjects> {

    private static final int INPUT_SIZE = 640; // YOLOv8 default input size
    private static final float CONFIDENCE_THRESHOLD = 0.25f;

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

        // YOLOv8 output: [1, 84, num_anchors] — channel-major, batch dim present from Batchifier.STACK
        // 84 = 4 bbox coords (cx, cy, w, h in pixel space 0-INPUT_SIZE) + 80 class probs (sigmoid)
        if (output.getShape().dimension() == 3) {
            output = output.squeeze(0); // [84, num_anchors]
        }

        int numAnchors = (int) output.getShape().get(1);
        float[] data = output.toFloatArray();

        List<String> classNames = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();
        List<ai.djl.modality.cv.output.BoundingBox> boundingBoxes = new ArrayList<>();

        for (int a = 0; a < numAnchors; a++) {
            // Channel-major: data[channel * numAnchors + anchor]
            float cx = data[0 * numAnchors + a];
            float cy = data[1 * numAnchors + a];
            float w  = data[2 * numAnchors + a];
            float h  = data[3 * numAnchors + a];

            // Find highest-scoring class (no separate objectness in YOLOv8)
            float maxProb = 0;
            int maxClass = -1;
            for (int j = 0; j < 80; j++) {
                float p = data[(4 + j) * numAnchors + a];
                if (p > maxProb) {
                    maxProb = p;
                    maxClass = j;
                }
            }

            if (maxProb < CONFIDENCE_THRESHOLD || maxClass < 0) {
                continue;
            }

            // Normalize pixel coords to [0, 1]
            float x1 = Math.max(0f, (cx - w / 2f) / INPUT_SIZE);
            float y1 = Math.max(0f, (cy - h / 2f) / INPUT_SIZE);
            float bw = Math.min(1f - x1, w / INPUT_SIZE);
            float bh = Math.min(1f - y1, h / INPUT_SIZE);

            if (bw <= 0 || bh <= 0) {
                continue;
            }

            boundingBoxes.add(new ai.djl.modality.cv.output.Rectangle(x1, y1, bw, bh));
            classNames.add(getCocoClassName(maxClass));
            probabilities.add((double) maxProb);
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
