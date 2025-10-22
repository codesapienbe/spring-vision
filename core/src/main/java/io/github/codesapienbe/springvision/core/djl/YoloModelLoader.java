package io.github.codesapienbe.springvision.core.djl;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Joints;
import ai.djl.repository.zoo.Criteria;

import java.net.URL;

public class YoloModelLoader {

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

    public static boolean isModelAvailable(String modelPath) {
        URL resource = YoloModelLoader.class.getResource("/models/" + modelPath);
        return resource != null;
    }

    public static String getModelUrl(String modelPath) {
        URL resource = YoloModelLoader.class.getResource("/models/" + modelPath);
        return resource != null ? resource.toString() : null;
    }
}
