package io.github.codesapienbe.springvision.core.djl.translator;

import java.util.ArrayList;
import java.util.List;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

/**
 * Translator for a YOLOv8 single-class license-plate detector.
 *
 * <p>The model is expected to output a tensor of shape {@code [1, 5, num_anchors]}
 * (or {@code [5, num_anchors]} after squeezing the batch dimension) where the
 * five channels are {@code [cx, cy, w, h, plate_prob]} in pixel coordinates of
 * the 640x640 input. Probabilities are post-sigmoid and ready to threshold
 * directly.</p>
 *
 * <p>Greedy NMS is applied to suppress overlapping boxes that come from
 * neighbouring anchors firing on the same plate.</p>
 */
public class YoloLicensePlateTranslator implements Translator<Image, DetectedObjects> {

    private static final int INPUT_SIZE = 640;
    private static final String CLASS_NAME = "license-plate";
    private static final float DEFAULT_CONFIDENCE = 0.35f;
    private static final float DEFAULT_IOU = 0.45f;

    private final float confidenceThreshold;
    private final float iouThreshold;

    public YoloLicensePlateTranslator() {
        this(DEFAULT_CONFIDENCE, DEFAULT_IOU);
    }

    public YoloLicensePlateTranslator(float confidenceThreshold, float iouThreshold) {
        this.confidenceThreshold = confidenceThreshold;
        this.iouThreshold = iouThreshold;
    }

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        NDManager manager = ctx.getNDManager();
        Image resized = input.resize(INPUT_SIZE, INPUT_SIZE, true);
        NDArray array = resized.toNDArray(manager);
        if (array.getShape().dimension() == 3) {
            array = array.transpose(2, 0, 1);
        }
        array = array.div(255.0f);
        return new NDList(array);
    }

    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {
        NDArray output = list.singletonOrThrow();
        if (output.getShape().dimension() == 3) {
            output = output.squeeze(0);
        }
        long channels = output.getShape().get(0);
        if (channels < 5) {
            return new DetectedObjects(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        int numAnchors = (int) output.getShape().get(1);
        float[] data = output.toFloatArray();

        List<float[]> rawBoxes = new ArrayList<>();
        List<Float> rawScores = new ArrayList<>();

        for (int a = 0; a < numAnchors; a++) {
            float cx = data[a];
            float cy = data[numAnchors + a];
            float w  = data[2 * numAnchors + a];
            float h  = data[3 * numAnchors + a];
            float p  = data[4 * numAnchors + a];

            if (p < confidenceThreshold) {
                continue;
            }
            float x1 = Math.max(0f, (cx - w / 2f) / INPUT_SIZE);
            float y1 = Math.max(0f, (cy - h / 2f) / INPUT_SIZE);
            float bw = Math.min(1f - x1, w / INPUT_SIZE);
            float bh = Math.min(1f - y1, h / INPUT_SIZE);
            if (bw <= 0 || bh <= 0) {
                continue;
            }
            rawBoxes.add(new float[]{x1, y1, bw, bh});
            rawScores.add(p);
        }

        int[] kept = nms(rawBoxes, rawScores, iouThreshold);
        List<String> classNames = new ArrayList<>(kept.length);
        List<Double> probabilities = new ArrayList<>(kept.length);
        List<ai.djl.modality.cv.output.BoundingBox> boundingBoxes = new ArrayList<>(kept.length);
        for (int idx : kept) {
            float[] b = rawBoxes.get(idx);
            classNames.add(CLASS_NAME);
            probabilities.add((double) rawScores.get(idx));
            boundingBoxes.add(new Rectangle(b[0], b[1], b[2], b[3]));
        }
        return new DetectedObjects(classNames, probabilities, boundingBoxes);
    }

    private int[] nms(List<float[]> boxes, List<Float> scores, float iouThr) {
        int n = boxes.size();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (i, j) -> Float.compare(scores.get(j), scores.get(i)));
        boolean[] suppressed = new boolean[n];
        List<Integer> keep = new ArrayList<>();
        for (int idx : order) {
            if (suppressed[idx]) {
                continue;
            }
            keep.add(idx);
            for (int other : order) {
                if (other == idx || suppressed[other]) {
                    continue;
                }
                if (iou(boxes.get(idx), boxes.get(other)) > iouThr) {
                    suppressed[other] = true;
                }
            }
        }
        int[] out = new int[keep.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = keep.get(i);
        }
        return out;
    }

    private float iou(float[] a, float[] b) {
        float ax2 = a[0] + a[2];
        float ay2 = a[1] + a[3];
        float bx2 = b[0] + b[2];
        float by2 = b[1] + b[3];
        float interX1 = Math.max(a[0], b[0]);
        float interY1 = Math.max(a[1], b[1]);
        float interX2 = Math.min(ax2, bx2);
        float interY2 = Math.min(ay2, by2);
        float interW = Math.max(0f, interX2 - interX1);
        float interH = Math.max(0f, interY2 - interY1);
        float inter = interW * interH;
        float union = a[2] * a[3] + b[2] * b[3] - inter;
        return union <= 0 ? 0f : inter / union;
    }

    @Override
    public Batchifier getBatchifier() {
        return Batchifier.STACK;
    }
}
