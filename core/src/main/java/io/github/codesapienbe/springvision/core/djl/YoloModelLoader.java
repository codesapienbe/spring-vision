package io.github.codesapienbe.springvision.core.djl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Joints;
import ai.djl.repository.zoo.Criteria;

public class YoloModelLoader {

    private static final ConcurrentHashMap<String, String> EXTRACTED_URLS = new ConcurrentHashMap<>();

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

    /**
     * Returns a file:// URL that DJL can load. For resources inside a nested boot JAR
     * (jar:nested:...), the resource is extracted once into the DJL cache directory and
     * the on-disk path is returned. file:-URL resources are returned as-is.
     */
    public static String getModelUrl(String modelPath) {
        URL resource = YoloModelLoader.class.getResource("/models/" + modelPath);
        if (resource == null) {
            return null;
        }
        String protocol = resource.getProtocol();
        if ("file".equals(protocol)) {
            return resource.toString();
        }
        return EXTRACTED_URLS.computeIfAbsent(modelPath, key -> extractToCache(key, resource));
    }

    private static String extractToCache(String modelPath, URL resource) {
        try {
            String cacheRoot = System.getProperty("ai.djl.repository.cache.dir");
            Path baseDir;
            if (cacheRoot != null && !cacheRoot.isBlank()) {
                baseDir = Paths.get(cacheRoot, "spring-vision-models");
            } else {
                baseDir = Paths.get(System.getProperty("user.home"), ".djl.ai", "spring-vision-models");
            }
            Path target = baseDir.resolve(modelPath);
            if (!Files.exists(target)) {
                Files.createDirectories(target.getParent());
                try (InputStream in = resource.openStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return target.toUri().toString();
        } catch (IOException e) {
            return null;
        }
    }
}
