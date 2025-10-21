package io.github.codesapienbe.springvision.mcp;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Joints;
import ai.djl.repository.zoo.Criteria;
import ai.djl.translate.TranslateException;

public class YoloLoader {

    public static Criteria<Image, DetectedObjects> createDetectionCriteria() {
        return Criteria.builder()
            .setTypes(Image.class, DetectedObjects.class)
            .optModelUrls("classpath:/models/yolov8/yolov8n.pt")
            .optEngine("PyTorch")
            .optOption("mapLocation", "true")  // Load to CPU
            .build();
    }

    public static Criteria<Image, Image> createSegmentationCriteria() {
        return Criteria.builder()
            .setTypes(Image.class, Image.class)
            .optModelUrls("classpath:/models/yolov8-seg/yolov8n-seg.pt")
            .optEngine("PyTorch")
            .build();
    }

    public static Criteria<Image, Joints> createPoseCriteria() {
        return Criteria.builder()
            .setTypes(Image.class, Joints.class)
            .optModelUrls("classpath:/models/yolov8-pose/yolov8n-pose.pt")
            .optEngine("PyTorch")
            .build();
    }
}
