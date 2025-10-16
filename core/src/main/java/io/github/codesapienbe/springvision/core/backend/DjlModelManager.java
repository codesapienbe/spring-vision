package io.github.codesapienbe.springvision.core.backend;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import io.github.codesapienbe.springvision.core.djl.DjlModelLoader;
import io.github.codesapienbe.springvision.core.djl.translator.SFaceFaceRecognitionTranslator;
import io.github.codesapienbe.springvision.core.djl.translator.YuNetFaceDetectionTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DJL-based model manager for OpenCV backend.
 *
 * <p>This class replaces custom ONNX model loading code with DJL's unified API,
 * significantly reducing complexity and improving maintainability.</p>
 *
 * <p>Benefits over custom loading:</p>
 * <ul>
 *   <li>Automatic model caching and version management</li>
 *   <li>Thread-safe model loading and inference</li>
 *   <li>Support for multiple model sources (local, URL, S3, etc.)</li>
 *   <li>Built-in error handling and retry logic</li>
 *   <li>GPU acceleration support</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
public class DjlModelManager {

    private static final Logger logger = LoggerFactory.getLogger(DjlModelManager.class);

    private ZooModel<Image, DetectedObjects> yuNetModel;
    private ZooModel<Image, float[]> sFaceModel;

    /**
     * Loads YuNet face detection model using DJL.
     *
     * @param modelPath path to the model file or directory
     * @return true if model loaded successfully, false otherwise
     */
    public boolean loadYuNetModel(String modelPath) {
        try {
            logger.info("Loading YuNet face detection model using DJL: {}", modelPath);

            Path path = Paths.get(modelPath);

            // Check if it's a file or directory
            String modelName;
            Path modelDir;

            if (Files.isDirectory(path)) {
                modelDir = path;
                modelName = "face_detection_yunet_2023mar";
            } else {
                modelDir = path.getParent();
                modelName = path.getFileName().toString().replace(".onnx", "");
            }

            // Create criteria with a custom translator
            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelPath(modelDir)
                .optModelName(modelName)
                .optEngine("OnnxRuntime")
                .optTranslator(new YuNetFaceDetectionTranslator())
                .build();

            // Load model using DJL
            yuNetModel = DjlModelLoader.loadModel(criteria);

            logger.info("YuNet model loaded successfully using DJL");
            return true;

        } catch (Exception e) {
            logger.warn("Failed to load YuNet model using DJL: {}", e.getMessage());
            yuNetModel = null;
            return false;
        }
    }

    /**
     * Loads SFace recognition model using DJL.
     *
     * @param modelPath path to the model file or directory
     * @return true if the model loaded successfully, false otherwise
     */
    public boolean loadSFaceModel(String modelPath) {
        try {
            logger.info("Loading SFace recognition model using DJL: {}", modelPath);

            Path path = Paths.get(modelPath);

            // Check if it's a file or directory
            String modelName;
            Path modelDir;

            if (Files.isDirectory(path)) {
                modelDir = path;
                modelName = "face_recognition_sface_2021dec";
            } else {
                modelDir = path.getParent();
                modelName = path.getFileName().toString().replace(".onnx", "");
            }

            // Create criteria with custom translator
            Criteria<Image, float[]> criteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optModelPath(modelDir)
                .optModelName(modelName)
                .optEngine("OnnxRuntime")
                .optTranslator(new SFaceFaceRecognitionTranslator())
                .build();

            // Load model using DJL
            sFaceModel = DjlModelLoader.loadModel(criteria);

            logger.info("SFace model loaded successfully using DJL");
            return true;

        } catch (Exception e) {
            logger.warn("Failed to load SFace model using DJL: {}", e.getMessage());
            sFaceModel = null;
            return false;
        }
    }

    /**
     * Detects faces using YuNet model via DJL.
     *
     * @param imageBytes image data
     * @return detected objects or null if detection fails
     */
    public DetectedObjects detectFacesWithYuNet(byte[] imageBytes) {
        if (yuNetModel == null) {
            logger.warn("YuNet model not loaded");
            return null;
        }

        try (Predictor<Image, DetectedObjects> predictor = yuNetModel.newPredictor()) {
            Image image = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageBytes));

            return predictor.predict(image);

        } catch (Exception e) {
            logger.error("Error detecting faces with YuNet: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generates face embedding using SFace model via DJL.
     *
     * @param faceImageBytes face image data
     * @return 128-dimensional embedding vector or null if generation fails
     */
    public float[] generateEmbeddingWithSFace(byte[] faceImageBytes) {
        if (sFaceModel == null) {
            logger.warn("SFace model not loaded");
            return null;
        }

        try (Predictor<Image, float[]> predictor = sFaceModel.newPredictor()) {
            Image faceImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(faceImageBytes));

            return predictor.predict(faceImage);

        } catch (Exception e) {
            logger.error("Error generating embedding with SFace: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if YuNet model is loaded.
     */
    public boolean isYuNetLoaded() {
        return yuNetModel != null;
    }

    /**
     * Checks if SFace model is loaded.
     */
    public boolean isSFaceLoaded() {
        return sFaceModel != null;
    }

    /**
     * Closes all loaded models and releases resources.
     */
    public void close() {
        if (yuNetModel != null) {
            try {
                yuNetModel.close();
                logger.info("YuNet model closed successfully");
            } catch (Exception e) {
                logger.warn("Error closing YuNet model: {}", e.getMessage());
            } finally {
                yuNetModel = null;
            }
        }

        if (sFaceModel != null) {
            try {
                sFaceModel.close();
                logger.info("SFace model closed successfully");
            } catch (Exception e) {
                logger.warn("Error closing SFace model: {}", e.getMessage());
            } finally {
                sFaceModel = null;
            }
        }
    }
}

