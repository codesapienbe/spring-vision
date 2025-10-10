package io.github.codesapienbe.springvision.facebytes.core;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig;
import io.github.codesapienbe.springvision.facebytes.detectors.DetectorFactory;
import io.github.codesapienbe.springvision.facebytes.detectors.FaceDetector;
import io.github.codesapienbe.springvision.facebytes.enums.DetectorBackend;
import io.github.codesapienbe.springvision.facebytes.enums.DistanceMetric;
import io.github.codesapienbe.springvision.facebytes.enums.ModelType;
import io.github.codesapienbe.springvision.facebytes.models.AnalysisOnnxModels;
import io.github.codesapienbe.springvision.facebytes.models.ArcFaceModel;
import io.github.codesapienbe.springvision.facebytes.models.DeepFaceModel;
import io.github.codesapienbe.springvision.facebytes.models.DlibModel;
import io.github.codesapienbe.springvision.facebytes.models.Facenet512Model;
import io.github.codesapienbe.springvision.facebytes.models.FacenetModel;
import io.github.codesapienbe.springvision.facebytes.models.OpenFaceModel;
import io.github.codesapienbe.springvision.facebytes.models.SFaceModel;
import io.github.codesapienbe.springvision.facebytes.models.VGGFaceModel;
import io.github.codesapienbe.springvision.facebytes.utils.DistanceMetrics;
import io.github.codesapienbe.springvision.facebytes.utils.FacePreprocessor;
import io.github.codesapienbe.springvision.facebytes.utils.FaceQualityValidator;

import static io.github.codesapienbe.springvision.facebytes.utils.ImageUtils.loadImage;

import io.github.codesapienbe.springvision.facebytes.utils.Logs;

/**
 * Core FaceBytes API providing DeepFace-like operations for face recognition and analysis.
 * This class offers a static interface for common face-related tasks, including:
 * <ul>
 *     <li><b>verify:</b> Compare two faces to determine if they belong to the same person.</li>
 *     <li><b>find:</b> Find the most similar face in a gallery for a given query face.</li>
 *     <li><b>represent:</b> Convert a face image into a compact numerical vector (embedding).</li>
 *     <li><b>extractFaces:</b> Detect and crop face regions from an image.</li>
 *     <li><b>analyze:</b> Predict attributes like age, gender, emotion, and race for a face.</li>
 * </ul>
 * The class is designed to be a pure-Java, standalone equivalent of the popular Python `deepface` library,
 * leveraging ONNX models for inference. It includes multiple overloads for handling images from file paths,
 * byte arrays, and input streams.
 *
 * <p>All path-based methods include validation for file existence and size to prevent common errors.
 * Structured logging is used to provide detailed information about operations and potential issues.</p>
 */
public final class DeepFace {

    private static final Logger log = LoggerFactory.getLogger(DeepFace.class);
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50MB guardrail
    private static final long MAX_IMAGE_PIXELS = 80_000_000L; // ~80MP guardrail to prevent memory exhaustion

    private DeepFace() {
    }

    /**
     * Finds the best match in a gallery of images for a given query image, using the default distance metric.
     *
     * @param queryImagePath    The file path to the query image.
     * @param galleryImagePaths A list of file paths for the gallery images to search through.
     * @return A {@link FindResult} containing the path of the best matching image, the distance score, and whether the match is within the configured threshold.
     */
    public static FindResult find(String queryImagePath, List<String> galleryImagePaths) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        validateFile(queryImagePath);
        for (String p : galleryImagePaths) validateFile(p);
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImagePath));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY;
            String bestPath = null;
            for (String p : galleryImagePaths) {
                List<EmbeddingResult> g = represent(loadImage(p));
                for (EmbeddingResult qe : q) {
                    for (EmbeddingResult ge : g) {
                        double d = compute(metric, qe.embedding(), ge.embedding());
                        if (d < best) {
                            best = d;
                            bestPath = p;
                        }
                    }
                }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestPath, best, thr, best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }

    /**
     * Finds the best match with an explicit distance metric and an optional threshold override.
     *
     * @param queryImagePath    The file path to the query image.
     * @param galleryImagePaths A list of file paths for the gallery images.
     * @param metric            The {@link DistanceMetric} to use for comparison.
     * @param thresholdOverride An optional value to override the default similarity threshold.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(String queryImagePath, List<String> galleryImagePaths, DistanceMetric metric, Double thresholdOverride) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric m = (metric != null) ? metric : cfg.defaultDistanceMetric();
        validateFile(queryImagePath);
        for (String p : galleryImagePaths) validateFile(p);
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImagePath));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(m), false);
            double best = Double.POSITIVE_INFINITY;
            String bestPath = null;
            for (String p : galleryImagePaths) {
                List<EmbeddingResult> g = represent(loadImage(p));
                for (EmbeddingResult qe : q)
                    for (EmbeddingResult ge : g) {
                        double d = compute(m, qe.embedding(), ge.embedding());
                        if (d < best) {
                            best = d;
                            bestPath = p;
                        }
                    }
            }
            double thr = (thresholdOverride != null) ? thresholdOverride : cfg.threshold(m);
            return new FindResult(bestPath, best, thr, best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(m), false);
        }
    }

    /**
     * Finds the best match for a query image provided as a byte array against a gallery of image paths.
     *
     * @param queryImageBytes   The byte array of the query image.
     * @param galleryImagePaths A list of file paths for the gallery images.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(byte[] queryImageBytes, List<String> galleryImagePaths) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageBytes));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY;
            String bestPath = null;
            for (String p : galleryImagePaths) {
                validateFile(p);
                List<EmbeddingResult> g = represent(loadImage(p));
                for (EmbeddingResult qe : q)
                    for (EmbeddingResult ge : g) {
                        double d = compute(metric, qe.embedding(), ge.embedding());
                        if (d < best) {
                            best = d;
                            bestPath = p;
                        }
                    }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestPath, best, thr, best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }


    /**
     * Finds the best match for a query image from an InputStream against a gallery of image paths.
     *
     * @param queryImageStream  The InputStream of the query image.
     * @param galleryImagePaths A list of file paths for the gallery images.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(InputStream queryImageStream, List<String> galleryImagePaths) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageStream));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY;
            String bestPath = null;
            for (String p : galleryImagePaths) {
                validateFile(p);
                List<EmbeddingResult> g = represent(loadImage(p));
                for (EmbeddingResult qe : q)
                    for (EmbeddingResult ge : g) {
                        double d = compute(metric, qe.embedding(), ge.embedding());
                        if (d < best) {
                            best = d;
                            bestPath = p;
                        }
                    }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestPath, best, thr, best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }

    /**
     * Finds the best match for a query image from an InputStream with explicit metric and threshold.
     *
     * @param queryImageStream  The InputStream of the query image.
     * @param galleryImagePaths A list of file paths for the gallery images.
     * @param metric            The distance metric to use.
     * @param thresholdOverride An optional override for the similarity threshold.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(InputStream queryImageStream, List<String> galleryImagePaths, DistanceMetric metric, Double thresholdOverride) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric m = (metric != null) ? metric : cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageStream));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(m), false);
            double best = Double.POSITIVE_INFINITY;
            String bestPath = null;
            for (String p : galleryImagePaths) {
                validateFile(p);
                List<EmbeddingResult> g = represent(loadImage(p));
                for (EmbeddingResult qe : q)
                    for (EmbeddingResult ge : g) {
                        double d = compute(m, qe.embedding(), ge.embedding());
                        if (d < best) {
                            best = d;
                            bestPath = p;
                        }
                    }
            }
            double thr = (thresholdOverride != null) ? thresholdOverride : cfg.threshold(m);
            return new FindResult(bestPath, best, thr, best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(m), false);
        }
    }

    /**
     * Finds the best match for a pre-computed query embedding against a list of pre-computed gallery embeddings.
     *
     * @param queryEmbedding    The embedding vector of the query face.
     * @param galleryEmbeddings A list of embedding vectors for the gallery faces.
     * @return A {@link FindResult} with the best match details, where the ID is the index in the gallery list.
     */
    public static FindResult find(float[] queryEmbedding, List<float[]> galleryEmbeddings) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        double best = Double.POSITIVE_INFINITY;
        int idx = -1;
        for (int i = 0; i < galleryEmbeddings.size(); i++) {
            float[] g = galleryEmbeddings.get(i);
            double d = compute(metric, queryEmbedding, g);
            if (d < best) {
                best = d;
                idx = i;
            }
        }
        double thr = cfg.threshold(metric);
        String bestPath = (idx >= 0) ? String.valueOf(idx) : null; // index-as-identifier if paths unknown
        return new FindResult(bestPath, best, thr, best <= thr);
    }

    /**
     * Finds the best match for a query embedding against a gallery of embeddings with associated IDs.
     *
     * @param queryEmbedding    The embedding of the query face.
     * @param galleryEmbeddings A list of embeddings for the gallery faces.
     * @param galleryIds        A list of IDs corresponding to each gallery embedding.
     * @return A {@link FindResult} with the ID and distance of the best match.
     */
    public static FindResult find(float[] queryEmbedding, List<float[]> galleryEmbeddings, List<String> galleryIds) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        double best = Double.POSITIVE_INFINITY;
        int idx = -1;
        for (int i = 0; i < galleryEmbeddings.size(); i++) {
            float[] g = galleryEmbeddings.get(i);
            double d = compute(metric, queryEmbedding, g);
            if (d < best) {
                best = d;
                idx = i;
            }
        }
        double thr = cfg.threshold(metric);
        String bestId = (idx >= 0 && galleryIds != null && idx < galleryIds.size()) ? galleryIds.get(idx) : (idx >= 0 ? String.valueOf(idx) : null);
        return new FindResult(bestId, best, thr, bestId != null && best <= thr);
    }

    /**
     * Finds the top-K closest matches for a query embedding against a gallery of embeddings.
     *
     * @param queryEmbedding    The embedding of the query face.
     * @param galleryEmbeddings A list of embeddings for the gallery faces.
     * @param galleryIds        A list of IDs for the gallery embeddings.
     * @param k                 The number of top matches to return.
     * @return A list of {@link FindMatch} objects, sorted by distance in ascending order.
     */
    public static List<FindMatch> findTopK(float[] queryEmbedding, List<float[]> galleryEmbeddings, List<String> galleryIds, int k) {
        if (k <= 0) return List.of();
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        Comparator<FindMatch> byDistanceDesc = Comparator.comparingDouble(FindMatch::distance).reversed();
        PriorityQueue<FindMatch> heap = new PriorityQueue<>(k, byDistanceDesc);
        for (int i = 0; i < galleryEmbeddings.size(); i++) {
            float[] g = galleryEmbeddings.get(i);
            double d = compute(metric, queryEmbedding, g);
            String id = (galleryIds != null && i < galleryIds.size()) ? galleryIds.get(i) : String.valueOf(i);
            if (heap.size() < k) {
                heap.offer(new FindMatch(id, d));
            } else if (!heap.isEmpty() && d < heap.peek().distance()) {
                heap.poll();
                heap.offer(new FindMatch(id, d));
            }
        }
        List<FindMatch> result = new ArrayList<>(heap);
        result.sort(Comparator.comparingDouble(FindMatch::distance));
        return result;
    }

    /**
     * Finds the top-K closest matches for a query image against a gallery of image paths.
     * This is a convenience method that computes embeddings internally.
     *
     * @param queryImagePath    The file path to the query image.
     * @param galleryImagePaths A list of file paths for the gallery images.
     * @param k                 The number of top matches to return.
     * @return A list of {@link FindMatch} objects, sorted by distance.
     */
    public static List<FindMatch> findTopK(String queryImagePath, List<String> galleryImagePaths, int k) {
        validateFile(queryImagePath);
        for (String p : galleryImagePaths) validateFile(p);
        try {
            List<float[]> qEmbeddings = representEmbeddings(loadImage(queryImagePath));
            if (qEmbeddings.isEmpty()) return List.of();
            // For multiple faces in query, aggregate by taking min distance per gallery item
            DeepFaceConfig cfg = DeepFaceConfig.current();
            DistanceMetric metric = cfg.defaultDistanceMetric();
            Comparator<FindMatch> byDistanceDesc = Comparator.comparingDouble(FindMatch::distance).reversed();
            PriorityQueue<FindMatch> heap = new PriorityQueue<>(k, byDistanceDesc);
            for (int i = 0; i < galleryImagePaths.size(); i++) {
                String p = galleryImagePaths.get(i);
                List<float[]> gEmbeddings = representEmbeddings(loadImage(p));
                double best = Double.POSITIVE_INFINITY;
                for (float[] qe : qEmbeddings)
                    for (float[] ge : gEmbeddings) {
                        double d = compute(metric, qe, ge);
                        if (d < best) best = d;
                    }
                String id = String.valueOf(i);
                if (heap.size() < k) heap.offer(new FindMatch(id, best));
                else if (!heap.isEmpty() && best < heap.peek().distance()) {
                    heap.poll();
                    heap.offer(new FindMatch(id, best));
                }
            }
            List<FindMatch> result = new ArrayList<>(heap);
            result.sort(Comparator.comparingDouble(FindMatch::distance));
            return result;
        } catch (IOException e) {
            Logs.error("DeepFace", "findTopK.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Finds the top-K matches with an explicit distance metric.
     *
     * @param queryImagePath    The file path to the query image.
     * @param galleryImagePaths A list of file paths for the gallery images.
     * @param k                 The number of top matches to return.
     * @param metric            The distance metric to use.
     * @return A list of {@link FindMatch} objects, sorted by distance.
     */
    public static List<FindMatch> findTopK(String queryImagePath, List<String> galleryImagePaths, int k, DistanceMetric metric) {
        validateFile(queryImagePath);
        for (String p : galleryImagePaths) validateFile(p);
        DistanceMetric m = (metric != null) ? metric : DeepFaceConfig.current().defaultDistanceMetric();
        try {
            List<float[]> qEmbeddings = representEmbeddings(loadImage(queryImagePath));
            if (qEmbeddings.isEmpty()) return List.of();
            Comparator<FindMatch> byDistanceDesc = Comparator.comparingDouble(FindMatch::distance).reversed();
            PriorityQueue<FindMatch> heap = new PriorityQueue<>(k, byDistanceDesc);
            for (int i = 0; i < galleryImagePaths.size(); i++) {
                String p = galleryImagePaths.get(i);
                List<float[]> gEmbeddings = representEmbeddings(loadImage(p));
                double best = Double.POSITIVE_INFINITY;
                for (float[] qe : qEmbeddings)
                    for (float[] ge : gEmbeddings) {
                        double d = compute(m, qe, ge);
                        if (d < best) best = d;
                    }
                String id = String.valueOf(i);
                if (heap.size() < k) heap.offer(new FindMatch(id, best));
                else if (!heap.isEmpty() && best < heap.peek().distance()) {
                    heap.poll();
                    heap.offer(new FindMatch(id, best));
                }
            }
            List<FindMatch> result = new ArrayList<>(heap);
            result.sort(Comparator.comparingDouble(FindMatch::distance));
            return result;
        } catch (IOException e) {
            Logs.error("DeepFace", "findTopK.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Finds the best match for a query image path against a gallery of image byte arrays.
     *
     * @param queryImagePath        The file path to the query image.
     * @param galleryImageBytesList A list of byte arrays for the gallery images.
     * @param galleryIds            A list of IDs for the gallery images.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(String queryImagePath, List<byte[]> galleryImageBytesList, List<String> galleryIds) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        validateFile(queryImagePath);
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImagePath));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY;
            String bestId = null;
            for (int i = 0; i < galleryImageBytesList.size(); i++) {
                List<EmbeddingResult> g = represent(loadImage(galleryImageBytesList.get(i)));
                for (EmbeddingResult qe : q)
                    for (EmbeddingResult ge : g) {
                        double d = compute(metric, qe.embedding(), ge.embedding());
                        if (d < best) {
                            best = d;
                            bestId = (galleryIds != null && i < galleryIds.size()) ? galleryIds.get(i) : String.valueOf(i);
                        }
                    }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestId, best, thr, best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }

    /**
     * Finds the best match for a query image byte array against a gallery of image byte arrays.
     *
     * @param queryImageBytes       The byte array of the query image.
     * @param galleryImageBytesList A list of byte arrays for the gallery images.
     * @param galleryIds            A list of IDs for the gallery images.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(byte[] queryImageBytes, List<byte[]> galleryImageBytesList, List<String> galleryIds) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageBytes));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY;
            String bestId = null;
            for (int i = 0; i < galleryImageBytesList.size(); i++) {
                List<EmbeddingResult> g = represent(loadImage(galleryImageBytesList.get(i)));
                for (EmbeddingResult qe : q)
                    for (EmbeddingResult ge : g) {
                        double d = compute(metric, qe.embedding(), ge.embedding());
                        if (d < best) {
                            best = d;
                            bestId = (galleryIds != null && i < galleryIds.size()) ? galleryIds.get(i) : String.valueOf(i);
                        }
                    }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestId, best, thr, bestId != null && best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }


    /**
     * Finds the best match for a query image stream against a gallery of image streams.
     *
     * @param queryImageStream The InputStream of the query image.
     * @param galleryStreams   A list of InputStreams for the gallery images.
     * @param galleryIds       A list of IDs for the gallery images.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(InputStream queryImageStream, List<InputStream> galleryStreams, List<String> galleryIds) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageStream));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY;
            String bestId = null;
            for (int i = 0; i < galleryStreams.size(); i++) {
                List<EmbeddingResult> g = represent(loadImage(galleryStreams.get(i)));
                for (EmbeddingResult qe : q)
                    for (EmbeddingResult ge : g) {
                        double d = compute(metric, qe.embedding(), ge.embedding());
                        if (d < best) {
                            best = d;
                            bestId = (galleryIds != null && i < galleryIds.size()) ? galleryIds.get(i) : String.valueOf(i);
                        }
                    }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestId, best, thr, bestId != null && best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }


    // ========================= VERIFY =========================

    /**
     * Verifies if two images contain the same person, using default settings.
     *
     * @param img1 The first image as a {@link BufferedImage}.
     * @param img2 The second image as a {@link BufferedImage}.
     * @return A {@link VerificationResult} indicating if they match, the distance, and the threshold used.
     */
    public static VerificationResult verify(BufferedImage img1, BufferedImage img2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(img1, img2, ModelType.ARCFACE, cfg.defaultDistanceMetric(), cfg.detectorBackend());
    }

    /**
     * Verifies if two images from byte arrays contain the same person, using default settings.
     *
     * @param img1 The byte array of the first image.
     * @param img2 The byte array of the second image.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(byte[] img1, byte[] img2) {
        try {
            return verify(loadImage(img1), loadImage(img2));
        } catch (IOException e) {
            Logs.error("DeepFace", "verify.load_failed", e, Map.of());
            DeepFaceConfig cfg = DeepFaceConfig.current();
            return new VerificationResult(false, 1.0, cfg.threshold(cfg.defaultDistanceMetric()), ModelType.ARCFACE, cfg.detectorBackend(), 0L);
        }
    }

    /**
     * Verifies if two images from InputStreams contain the same person, using default settings.
     *
     * @param img1 The InputStream of the first image.
     * @param img2 The InputStream of the second image.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(InputStream img1, InputStream img2) {
        try {
            return verify(loadImage(img1), loadImage(img2));
        } catch (IOException e) {
            Logs.error("DeepFace", "verify.load_failed", e, Map.of());
            DeepFaceConfig cfg = DeepFaceConfig.current();
            return new VerificationResult(false, 1.0, cfg.threshold(cfg.defaultDistanceMetric()), ModelType.ARCFACE, cfg.detectorBackend(), 0L);
        }
    }

    /**
     * Verifies if two images from file paths contain the same person, using default settings.
     *
     * @param img1 The file path of the first image.
     * @param img2 The file path of the second image.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(String img1, String img2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(img1, img2, ModelType.ARCFACE, cfg.defaultDistanceMetric(), cfg.detectorBackend());
    }

    /**
     * Verifies if two images from file paths contain the same person, with explicit model, distance metric, and detector.
     *
     * @param img1     The file path of the first image.
     * @param img2     The file path of the second image.
     * @param model    The {@link ModelType} to use for embedding generation.
     * @param distance The {@link DistanceMetric} for comparison.
     * @param detector The {@link DetectorBackend} for face detection.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(String img1, String img2, ModelType model, DistanceMetric distance,
                                            DetectorBackend detector) {
        validateFile(img1);
        validateFile(img2);
        try {
            BufferedImage a = loadImage(img1);
            BufferedImage b = loadImage(img2);
            return verify(a, b, model, distance, detector);
        } catch (IOException e) {
            Logs.error("DeepFace", "verify.load_failed", e, Map.of());
            return new VerificationResult(false, 1.0, DeepFaceConfig.current().threshold(distance), model, detector, 0L);
        }
    }

    /**
     * Verifies if two pre-computed embeddings represent the same person.
     *
     * @param emb1           The first embedding vector.
     * @param emb2           The second embedding vector.
     * @param model          The model that generated the embeddings.
     * @param distanceMetric The distance metric to use for comparison.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(float[] emb1, float[] emb2, ModelType model, DistanceMetric distanceMetric) {
        double d = compute(distanceMetric, emb1, emb2);
        double thr = DeepFaceConfig.current().threshold(distanceMetric);
        boolean ok = d <= thr;
        return new VerificationResult(ok, d, thr, model, DeepFaceConfig.current().detectorBackend(), 0L);
    }

    /**
     * Verifies if two BufferedImages contain the same person, with full control over model, distance, and detector.
     *
     * @param img1     The first image.
     * @param img2     The second image.
     * @param model    The model to use.
     * @param distance The distance metric to use.
     * @param detector The detector backend to use.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(BufferedImage img1, BufferedImage img2, ModelType model,
                                            DistanceMetric distance, DetectorBackend detector) {
        long start = System.currentTimeMillis();
        DeepFaceConfig cfg = DeepFaceConfig.current();
        List<EmbeddingResult> e1 = represent(img1, model, detector);
        List<EmbeddingResult> e2 = represent(img2, model, detector);
        if (e1.isEmpty() || e2.isEmpty()) {
            if (cfg.enforceDetection()) throw new IllegalArgumentException("No valid face embeddings produced");
            return new VerificationResult(false, 1.0, cfg.threshold(model, distance), model, detector, System.currentTimeMillis() - start);
        }
        double best = Double.POSITIVE_INFINITY;
        for (EmbeddingResult a : e1) {
            for (EmbeddingResult b : e2) {
                double d = compute(distance, a.embedding(), b.embedding());
                if (d < best) best = d;
            }
        }
        double thr = cfg.threshold(model, distance);
        boolean ok = best <= thr;
        return new VerificationResult(ok, best, thr, model, detector, System.currentTimeMillis() - start);
    }

    /**
     * Verifies if two images from byte arrays contain the same person, with explicit settings.
     *
     * @param img1     The byte array of the first image.
     * @param img2     The byte array of the second image.
     * @param model    The model to use.
     * @param distance The distance metric to use.
     * @param detector The detector backend to use.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(byte[] img1, byte[] img2, ModelType model, DistanceMetric distance,
                                            DetectorBackend detector) {
        try {
            return verify(loadImage(img1), loadImage(img2), model, distance, detector);
        } catch (IOException e) {
            Logs.error("DeepFace", "verify.load_failed", e, Map.of());
            return new VerificationResult(false, 1.0, DeepFaceConfig.current().threshold(distance), model, detector, 0L);
        }
    }

    /**
     * Verifies if two images from InputStreams contain the same person, with explicit settings.
     *
     * @param img1     The InputStream of the first image.
     * @param img2     The InputStream of the second image.
     * @param model    The model to use.
     * @param distance The distance metric to use.
     * @param detector The detector backend to use.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(InputStream img1, InputStream img2, ModelType model, DistanceMetric distance,
                                            DetectorBackend detector) {
        try {
            return verify(loadImage(img1), loadImage(img2), model, distance, detector);
        } catch (IOException e) {
            Logs.error("DeepFace", "verify.load_failed", e, Map.of());
            return new VerificationResult(false, 1.0, DeepFaceConfig.current().threshold(distance), model, detector, 0L);
        }
    }

    /**
     * Verifies two embeddings using the default model and distance metric.
     *
     * @param emb1 The first embedding.
     * @param emb2 The second embedding.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verify(float[] emb1, float[] emb2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(emb1, emb2, ModelType.ARCFACE, cfg.defaultDistanceMetric());
    }

    /**
     * Verifies two sets of embeddings (e.g., from images with multiple faces) by finding the minimum pairwise distance.
     *
     * @param setA   The first set of embedding vectors.
     * @param setB   The second set of embedding vectors.
     * @param model  The model used to generate the embeddings.
     * @param metric The distance metric for comparison.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verifyEmbeddings(List<float[]> setA, List<float[]> setB, ModelType model, DistanceMetric metric) {
        double best = bestDistance(setA, setB, metric);
        double thr = DeepFaceConfig.current().threshold(metric);
        boolean ok = best <= thr;
        return new VerificationResult(ok, best, thr, model, DeepFaceConfig.current().detectorBackend(), 0L);
    }

    // ========================= REPRESENT (multiple overloads) =========================

    /**
     * Generates embeddings for all faces in an image from a file path, using default settings.
     *
     * @param imgPath The file path to the image.
     * @return A list of {@link EmbeddingResult} objects, each containing an embedding and the corresponding face region.
     */
    public static List<EmbeddingResult> represent(String imgPath) {
        validateFile(imgPath);
        try {
            BufferedImage img = loadImage(imgPath);
            return represent(img);
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings with an explicit detector backend.
     *
     * @param imgPath The file path to the image.
     * @param backend The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(String imgPath, DetectorBackend backend) {
        validateFile(imgPath);
        try {
            return represent(loadImage(imgPath), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings for all faces in an image from a byte array.
     *
     * @param imageBytes The byte array of the image.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(byte[] imageBytes) {
        try {
            return represent(loadImage(imageBytes));
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings from a byte array with an explicit detector backend.
     *
     * @param imageBytes The byte array of the image.
     * @param backend    The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(byte[] imageBytes, DetectorBackend backend) {
        try {
            return represent(loadImage(imageBytes), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings for all faces in an image from an InputStream.
     *
     * @param imageStream The InputStream of the image.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(InputStream imageStream) {
        try {
            return represent(loadImage(imageStream));
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings from an InputStream with an explicit detector backend.
     *
     * @param imageStream The InputStream of the image.
     * @param backend     The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(InputStream imageStream, DetectorBackend backend) {
        try {
            return represent(loadImage(imageStream), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings for all faces in a {@link BufferedImage}.
     * This is the core `represent` method that others delegate to.
     *
     * @param img The image to process.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(BufferedImage img) {
        validateImage(img);
        DeepFaceConfig cfg = DeepFaceConfig.current();
        FaceDetector fd = DetectorFactory.create(cfg.detectorBackend());
        List<FaceRegion> regions = fd.detectFaces(img);
        if (regions.isEmpty()) {
            if (cfg.enforceDetection()) throw new IllegalArgumentException("No faces detected");
            return Collections.emptyList();
        }
        FacePreprocessor pre = cfg.align() ? new FacePreprocessor() : null;
        FaceQualityValidator quality = new FaceQualityValidator();
        int target = cfg.inputSize();
        int margin = cfg.margin();
        List<EmbeddingResult> out = new ArrayList<>();
        for (FaceRegion r : regions) {
            boolean small = isTooSmall(r, img.getWidth(), img.getHeight());
            BufferedImage face = cropWithMargin(img, r, margin);
            if (small) {
                Logs.debug("DeepFace", "represent.enhance_small", Map.of("x", r.x(), "y", r.y(), "w", r.width(), "h", r.height()));
                face = enhanceSmallFace(face);
            }
            BufferedImage processed;
            if (pre != null && r.landmarks() != null && r.landmarks().length >= 10) {
                processed = pre.alignWithLandmarks(face, r.landmarks(), target, target, ModelType.ARCFACE);
            } else if (pre != null) {
                processed = pre.alignAndResize(face, target, target);
            } else {
                processed = resize(face, target, target);
            }
            if (!quality.isValidFace(processed)) {
                Logs.warn("DeepFace", "represent.drop_low_quality", Map.of("x", r.x(), "y", r.y(), "w", r.width(), "h", r.height()));
                continue;
            }
            float[] emb;
            try {
                emb = selectModelAndEmbed(processed, ModelType.ARCFACE, cfg);
            } catch (RuntimeException ex) {
                Logs.warn("DeepFace", "represent.embed_failed", Map.of("err", ex.getClass().getSimpleName()));
                continue;
            }
            if (emb == null || emb.length == 0) continue;
            out.add(new EmbeddingResult(emb, r));
        }
        if (out.isEmpty() && cfg.enforceDetection())
            throw new IllegalArgumentException("No valid face embeddings produced");
        return out;
    }

    /**
     * Generates embeddings from a {@link BufferedImage} with an explicit detector backend.
     *
     * @param img     The image to process.
     * @param backend The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(BufferedImage img, DetectorBackend backend) {
        validateImage(img);
        DeepFaceConfig cfg = DeepFaceConfig.current();
        FaceDetector fd = DetectorFactory.create(backend != null ? backend : cfg.detectorBackend());
        List<FaceRegion> regions = fd.detectFaces(img);
        if (regions.isEmpty()) {
            if (cfg.enforceDetection()) throw new IllegalArgumentException("No faces detected");
            return Collections.emptyList();
        }
        FacePreprocessor pre = cfg.align() ? new FacePreprocessor() : null;
        FaceQualityValidator quality = new FaceQualityValidator();
        int target = cfg.inputSize();
        int margin = cfg.margin();
        List<EmbeddingResult> out = new ArrayList<>();
        for (FaceRegion r : regions) {
            boolean small = isTooSmall(r, img.getWidth(), img.getHeight());
            BufferedImage face = cropWithMargin(img, r, margin);
            if (small) {
                Logs.debug("DeepFace", "represent.enhance_small", Map.of("x", r.x(), "y", r.y(), "w", r.width(), "h", r.height()));
                face = enhanceSmallFace(face);
            }
            BufferedImage processed;
            if (pre != null && r.landmarks() != null && r.landmarks().length >= 10) {
                processed = pre.alignWithLandmarks(face, r.landmarks(), target, target, ModelType.ARCFACE);
            } else if (pre != null) {
                processed = pre.alignAndResize(face, target, target);
            } else {
                processed = resize(face, target, target);
            }
            if (!quality.isValidFace(processed)) {
                Logs.warn("DeepFace", "represent.drop_low_quality", Map.of("x", r.x(), "y", r.y(), "w", r.width(), "h", r.height()));
                continue;
            }
            float[] emb;
            try {
                emb = selectModelAndEmbed(processed, ModelType.ARCFACE, cfg);
            } catch (RuntimeException ex) {
                Logs.warn("DeepFace", "represent.embed_failed", Map.of("err", ex.getClass().getSimpleName()));
                continue;
            }
            if (emb == null || emb.length == 0) continue;
            out.add(new EmbeddingResult(emb, r));
        }
        if (out.isEmpty() && cfg.enforceDetection())
            throw new IllegalArgumentException("No valid face embeddings produced");
        return out;
    }

    // New: model-aware represent overloads

    /**
     * Generates embeddings using a specific model and detector backend.
     *
     * @param imgPath The file path to the image.
     * @param model   The model to use for embeddings.
     * @param backend The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(String imgPath, ModelType model, DetectorBackend backend) {
        validateFile(imgPath);
        try {
            return represent(loadImage(imgPath), model, backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings from a byte array using a specific model and detector.
     *
     * @param imageBytes The byte array of the image.
     * @param model      The model to use.
     * @param backend    The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(byte[] imageBytes, ModelType model, DetectorBackend backend) {
        try {
            return represent(loadImage(imageBytes), model, backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings from an InputStream using a specific model and detector.
     *
     * @param imageStream The InputStream of the image.
     * @param model       The model to use.
     * @param backend     The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(InputStream imageStream, ModelType model, DetectorBackend backend) {
        try {
            return represent(loadImage(imageStream), model, backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "represent.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Generates embeddings from a {@link BufferedImage} using a specific model and detector.
     *
     * @param img     The image to process.
     * @param model   The model to use.
     * @param backend The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> represent(BufferedImage img, ModelType model, DetectorBackend backend) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        FaceDetector fd = DetectorFactory.create(backend != null ? backend : cfg.detectorBackend());
        List<FaceRegion> regions = fd.detectFaces(img);
        if (regions.isEmpty()) return Collections.emptyList();
        FacePreprocessor pre = cfg.align() ? new FacePreprocessor() : null;
        int target = cfg.inputSize();
        int margin = cfg.margin();
        List<EmbeddingResult> out = new ArrayList<>();
        for (FaceRegion r : regions) {
            BufferedImage face = cropWithMargin(img, r, margin);
            BufferedImage processed;
            if (pre != null && r.landmarks() != null && r.landmarks().length >= 10) {
                processed = pre.alignWithLandmarks(face, r.landmarks(), target, target, model != null ? model : ModelType.ARCFACE);
            } else if (pre != null) {
                processed = pre.alignAndResize(face, target, target);
            } else {
                processed = resize(face, target, target);
            }
            float[] emb = selectModelAndEmbed(processed, model, cfg);
            out.add(new EmbeddingResult(emb, r));
        }
        return out;
    }

    // ========================= REPRESENT (embeddings only helpers) =========================

    /**
     * Generates only the embedding vectors for all faces in an image from a file path.
     *
     * @param imgPath The file path to the image.
     * @return A list of float arrays, where each array is an embedding.
     */
    public static List<float[]> representEmbeddings(String imgPath) {
        validateFile(imgPath);
        try {
            return representEmbeddings(loadImage(imgPath));
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings with an explicit detector backend.
     *
     * @param imgPath The file path to the image.
     * @param backend The detector backend to use.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(String imgPath, DetectorBackend backend) {
        validateFile(imgPath);
        try {
            return representEmbeddings(loadImage(imgPath), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings from a byte array.
     *
     * @param imageBytes The byte array of the image.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(byte[] imageBytes) {
        try {
            return representEmbeddings(loadImage(imageBytes));
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings from a byte array with an explicit detector.
     *
     * @param imageBytes The byte array of the image.
     * @param backend    The detector backend to use.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(byte[] imageBytes, DetectorBackend backend) {
        try {
            return representEmbeddings(loadImage(imageBytes), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings from an InputStream.
     *
     * @param imageStream The InputStream of the image.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(InputStream imageStream) {
        try {
            return representEmbeddings(loadImage(imageStream));
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings from an InputStream with an explicit detector.
     *
     * @param imageStream The InputStream of the image.
     * @param backend     The detector backend to use.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(InputStream imageStream, DetectorBackend backend) {
        try {
            return representEmbeddings(loadImage(imageStream), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings from a {@link BufferedImage}.
     *
     * @param image The image to process.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(BufferedImage image) {
        List<EmbeddingResult> results = represent(image);
        List<float[]> out = new ArrayList<>(results.size());
        for (EmbeddingResult r : results) out.add(r.embedding());
        return out;
    }

    /**
     * Generates embeddings from a {@link BufferedImage} with an explicit detector.
     *
     * @param image   The image to process.
     * @param backend The detector backend to use.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(BufferedImage image, DetectorBackend backend) {
        List<EmbeddingResult> results = represent(image, backend);
        List<float[]> out = new ArrayList<>(results.size());
        for (EmbeddingResult r : results) out.add(r.embedding());
        return out;
    }

    // New: model-aware embeddings helpers

    /**
     * Generates embeddings using a specific model and detector.
     *
     * @param imgPath The file path to the image.
     * @param model   The model to use.
     * @param backend The detector backend to use.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(String imgPath, ModelType model, DetectorBackend backend) {
        validateFile(imgPath);
        try {
            return representEmbeddings(loadImage(imgPath), model, backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings from a byte array using a specific model and detector.
     *
     * @param imageBytes The byte array of the image.
     * @param model      The model to use.
     * @param backend    The detector backend to use.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(byte[] imageBytes, ModelType model, DetectorBackend backend) {
        try {
            return representEmbeddings(loadImage(imageBytes), model, backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings from an InputStream using a specific model and detector.
     *
     * @param imageStream The InputStream of the image.
     * @param model       The model to use.
     * @param backend     The detector backend to use.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(InputStream imageStream, ModelType model, DetectorBackend backend) {
        try {
            return representEmbeddings(loadImage(imageStream), model, backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    /**
     * Generates embeddings from a {@link BufferedImage} using a specific model and detector.
     *
     * @param image   The image to process.
     * @param model   The model to use.
     * @param backend The detector backend to use.
     * @return A list of embedding vectors.
     */
    public static List<float[]> representEmbeddings(BufferedImage image, ModelType model, DetectorBackend backend) {
        List<EmbeddingResult> results = represent(image, model, backend);
        List<float[]> out = new ArrayList<>(results.size());
        for (EmbeddingResult r : results) out.add(r.embedding());
        return out;
    }

    // ========================= DISTANCE UTILITY =========================

    /**
     * Computes the distance between two embedding vectors using a specified metric.
     *
     * @param emb1   The first embedding vector.
     * @param emb2   The second embedding vector.
     * @param metric The {@link DistanceMetric} to use for the calculation.
     * @return The calculated distance as a double.
     */
    public static double distance(float[] emb1, float[] emb2, DistanceMetric metric) {
        return compute(metric, emb1, emb2);
    }

    /**
     * Computes the distance between two embeddings using the default distance metric.
     *
     * @param emb1 The first embedding vector.
     * @param emb2 The second embedding vector.
     * @return The calculated distance.
     */
    public static double distance(float[] emb1, float[] emb2) {
        DistanceMetric m = DeepFaceConfig.current().defaultDistanceMetric();
        return compute(m, emb1, emb2);
    }

    // ========================= EXTRACT FACES (multiple overloads) =========================

    /**
     * Detects and extracts all face regions from an image specified by a file path.
     *
     * @param imgPath The file path to the image.
     * @return A list of {@link BufferedImage} objects, each containing a cropped face.
     */
    public static List<BufferedImage> extractFaces(String imgPath) {
        validateFile(imgPath);
        try {
            BufferedImage img = loadImage(imgPath);
            return extractFaces(img);
        } catch (IOException e) {
            Logs.error("DeepFace", "extractFaces.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Extracts faces with an explicit detector backend.
     *
     * @param imgPath The file path to the image.
     * @param backend The detector backend to use.
     * @return A list of cropped face images.
     */
    public static List<BufferedImage> extractFaces(String imgPath, DetectorBackend backend) {
        validateFile(imgPath);
        try {
            return extractFaces(loadImage(imgPath), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "extractFaces.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Extracts faces from an image provided as a byte array.
     *
     * @param imageBytes The byte array of the image.
     * @return A list of cropped face images.
     */
    public static List<BufferedImage> extractFaces(byte[] imageBytes) {
        try {
            return extractFaces(loadImage(imageBytes));
        } catch (IOException e) {
            Logs.error("DeepFace", "extractFaces.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Extracts faces from a byte array with an explicit detector backend.
     *
     * @param imageBytes The byte array of the image.
     * @param backend    The detector backend to use.
     * @return A list of cropped face images.
     */
    public static List<BufferedImage> extractFaces(byte[] imageBytes, DetectorBackend backend) {
        try {
            return extractFaces(loadImage(imageBytes), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "extractFaces.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Extracts faces from an image provided as an InputStream.
     *
     * @param imageStream The InputStream of the image.
     * @return A list of cropped face images.
     */
    public static List<BufferedImage> extractFaces(InputStream imageStream) {
        try {
            return extractFaces(loadImage(imageStream));
        } catch (IOException e) {
            Logs.error("DeepFace", "extractFaces.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Extracts faces from an InputStream with an explicit detector backend.
     *
     * @param imageStream The InputStream of the image.
     * @param backend     The detector backend to use.
     * @return A list of cropped face images.
     */
    public static List<BufferedImage> extractFaces(InputStream imageStream, DetectorBackend backend) {
        try {
            return extractFaces(loadImage(imageStream), backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "extractFaces.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Extracts faces from a {@link BufferedImage}.
     *
     * @param img The image to process.
     * @return A list of cropped face images.
     */
    public static List<BufferedImage> extractFaces(BufferedImage img) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        FaceDetector fd = DetectorFactory.create(cfg.detectorBackend());
        List<FaceRegion> regions = fd.detectFaces(img);
        int margin = cfg.margin();
        List<BufferedImage> crops = new ArrayList<>();
        for (FaceRegion r : regions) {
            crops.add(cropWithMargin(img, r, margin));
        }
        return crops;
    }

    /**
     * Extracts faces from a {@link BufferedImage} with an explicit detector backend.
     *
     * @param img     The image to process.
     * @param backend The detector backend to use.
     * @return A list of cropped face images.
     */
    public static List<BufferedImage> extractFaces(BufferedImage img, DetectorBackend backend) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        FaceDetector fd = DetectorFactory.create(backend != null ? backend : cfg.detectorBackend());
        List<FaceRegion> regions = fd.detectFaces(img);
        int margin = cfg.margin();
        List<BufferedImage> crops = new ArrayList<>();
        for (FaceRegion r : regions) {
            crops.add(cropWithMargin(img, r, margin));
        }
        return crops;
    }

    // ========================= ANALYZE =========================

    /**
     * Analyzes all supported attributes (age, gender, emotion, race) for faces in an image from a file path.
     *
     * @param imgPath The file path to the image.
     * @return A list of {@link AnalysisResult} objects, one for each detected face.
     */
    public static List<AnalysisResult> analyze(String imgPath) {
        return analyze(imgPath, null);
    }

    /**
     * Analyzes all supported attributes for faces in a {@link BufferedImage}.
     *
     * @param image The image to analyze.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(BufferedImage image) {
        return analyze(image, null);
    }

    /**
     * Analyzes specified attributes for faces in an image from a byte array.
     *
     * @param imageBytes The byte array of the image.
     * @param actions    An array of strings specifying the actions to perform (e.g., "age", "gender"). If null or empty, all actions are performed.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(byte[] imageBytes, String[] actions) {
        try {
            return analyze(loadImage(imageBytes), actions);
        } catch (IOException e) {
            Logs.error("DeepFace", "analyze.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Analyzes specified attributes for faces in an image from an InputStream.
     *
     * @param imageStream The InputStream of the image.
     * @param actions     The actions to perform.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(InputStream imageStream, String[] actions) {
        try {
            return analyze(loadImage(imageStream), actions);
        } catch (IOException e) {
            Logs.error("DeepFace", "analyze.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Analyzes all attributes for faces in an image from a byte array.
     *
     * @param imageBytes The byte array of the image.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(byte[] imageBytes) {
        return analyze(imageBytes, null);
    }

    /**
     * Analyzes all attributes for faces in an image from an InputStream.
     *
     * @param imageStream The InputStream of the image.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(InputStream imageStream) {
        return analyze(imageStream, null);
    }

    /**
     * Analyzes specified attributes for faces in an image from a file path.
     *
     * @param imgPath The file path to the image.
     * @param actions The actions to perform.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(String imgPath, String[] actions) {
        validateFile(imgPath);
        try {
            BufferedImage img = loadImage(imgPath);
            return analyze(img, actions);
        } catch (IOException e) {
            Logs.error("DeepFace", "analyze.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Analyzes attributes with an explicit detector backend.
     *
     * @param imgPath The file path to the image.
     * @param actions The actions to perform.
     * @param backend The detector backend to use.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(String imgPath, String[] actions, DetectorBackend backend) {
        validateFile(imgPath);
        try {
            return analyze(loadImage(imgPath), actions, backend);
        } catch (IOException e) {
            Logs.error("DeepFace", "analyze.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /**
     * Analyzes attributes for faces in a {@link BufferedImage}.
     * Supported actions: "age", "gender", "emotion", "race".
     *
     * @param image   The image to analyze.
     * @param actions The actions to perform. If null, all are performed.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(BufferedImage image, String[] actions) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        FaceDetector fd = DetectorFactory.create(cfg.detectorBackend());
        List<FaceRegion> regions = fd.detectFaces(image);
        if (regions.isEmpty()) return Collections.emptyList();

        boolean doAge = containsAction(actions, "age");
        boolean doGender = containsAction(actions, "gender");
        boolean doEmotion = containsAction(actions, "emotion");
        boolean doRace = containsAction(actions, "race");

        FacePreprocessor pre = cfg.align() ? new FacePreprocessor() : null;
        int margin = cfg.margin();

        List<AnalysisResult> results = new ArrayList<>();
        for (FaceRegion r : regions) {
            BufferedImage crop = cropWithMargin(image, r, margin);
            BufferedImage processed;
            if (pre != null && r.landmarks() != null && r.landmarks().length >= 10) {
                processed = pre.alignWithLandmarks(crop, r.landmarks(), 224, 224, ModelType.ARCFACE);
            } else if (pre != null) {
                processed = pre.alignAndResize(crop, 224, 224);
            } else {
                processed = resize(crop, 224, 224);
            }
            Integer age = null;
            String gender = null;
            String dominantEmotion = null;
            Map<String, Double> emotionDist = Map.of();
            Map<String, Double> raceDist = Map.of();

            try {
                if (doAge) {
                    if (cfg.ageOnnxPath() != null) {
                        int size = cfg.ageInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getAge().run(in, new long[]{1, 3, size, size});
                        age = mapAge(out);
                    }
                }
                if (doGender) {
                    if (cfg.genderOnnxPath() != null) {
                        int size = cfg.genderInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getGender().run(in, new long[]{1, 3, size, size});
                        gender = mapGender(out);
                    }
                }
                if (doEmotion) {
                    if (cfg.emotionOnnxPath() != null) {
                        int size = cfg.emotionInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getEmotion().run(in, new long[]{1, 3, size, size});
                        emotionDist = mapDistribution(out, emotionLabels());
                        dominantEmotion = argmaxLabel(emotionDist);
                    }
                }
                if (doRace) {
                    if (cfg.raceOnnxPath() != null) {
                        int size = cfg.raceInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getRace().run(in, new long[]{1, 3, size, size});
                        raceDist = mapDistribution(out, raceLabels());
                    }
                }
                // If ONNX models are not configured, fields remain null/empty and no mock is used.
            } catch (Exception e) {
                Logs.error("DeepFace", "analyze.inference_failed", e, Map.of());
            }

            results.add(new AnalysisResult(age, gender, dominantEmotion, emotionDist, raceDist, r));
        }

        return results;
    }

    /**
     * Analyzes attributes with an explicit detector backend.
     *
     * @param image   The image to analyze.
     * @param actions The actions to perform.
     * @param backend The detector backend to use.
     * @return A list of {@link AnalysisResult} objects.
     */
    public static List<AnalysisResult> analyze(BufferedImage image, String[] actions, DetectorBackend backend) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        FaceDetector fd = DetectorFactory.create(backend != null ? backend : cfg.detectorBackend());
        List<FaceRegion> regions = fd.detectFaces(image);
        if (regions.isEmpty()) return Collections.emptyList();

        boolean doAge = containsAction(actions, "age");
        boolean doGender = containsAction(actions, "gender");
        boolean doEmotion = containsAction(actions, "emotion");
        boolean doRace = containsAction(actions, "race");

        FacePreprocessor pre = cfg.align() ? new FacePreprocessor() : null;
        int margin = cfg.margin();

        List<AnalysisResult> results = new ArrayList<>();
        for (FaceRegion r : regions) {
            BufferedImage crop = cropWithMargin(image, r, margin);
            BufferedImage processed;
            if (pre != null && r.landmarks() != null && r.landmarks().length >= 10) {
                processed = pre.alignWithLandmarks(crop, r.landmarks(), 224, 224, ModelType.ARCFACE);
            } else if (pre != null) {
                processed = pre.alignAndResize(crop, 224, 224);
            } else {
                processed = resize(crop, 224, 224);
            }
            Integer age = null;
            String gender = null;
            String dominantEmotion = null;
            Map<String, Double> emotionDist = Map.of();
            Map<String, Double> raceDist = Map.of();

            try {
                if (doAge) {
                    if (cfg.ageOnnxPath() != null) {
                        int size = cfg.ageInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getAge().run(in, new long[]{1, 3, size, size});
                        age = mapAge(out);
                    }
                }
                if (doGender) {
                    if (cfg.genderOnnxPath() != null) {
                        int size = cfg.genderInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getGender().run(in, new long[]{1, 3, size, size});
                        gender = mapGender(out);
                    }
                }
                if (doEmotion) {
                    if (cfg.emotionOnnxPath() != null) {
                        int size = cfg.emotionInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getEmotion().run(in, new long[]{1, 3, size, size});
                        emotionDist = mapDistribution(out, emotionLabels());
                        dominantEmotion = argmaxLabel(emotionDist);
                    }
                }
                if (doRace) {
                    if (cfg.raceOnnxPath() != null) {
                        int size = cfg.raceInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getRace().run(in, new long[]{1, 3, size, size});
                        raceDist = mapDistribution(out, raceLabels());
                    }
                }
                // If ONNX models are not configured, fields remain null/empty and no mock is used.
            } catch (Exception e) {
                Logs.error("DeepFace", "analyze.inference_failed", e, Map.of());
            }

            results.add(new AnalysisResult(age, gender, dominantEmotion, emotionDist, raceDist, r));
        }

        return results;
    }

    private static boolean containsAction(String[] actions, String key) {
        if (actions == null || actions.length == 0) return true; // default analyze all
        for (String a : actions) {
            if (a != null && a.equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    private static String[] emotionLabels() {
        return new String[]{"angry", "disgust", "fear", "happy", "sad", "surprise", "neutral"};
    }

    private static String[] raceLabels() {
        return new String[]{"asian", "indian", "black", "white", "middle_eastern", "latino_hispanic"};
    }

    private static Map<String, Double> mapDistribution(float[] logits, String[] labels) {
        double[] sm = softmax(logits);
        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(labels.length, sm.length); i++) {
            out.put(labels[i], sm[i]);
        }
        return out;
    }

    private static String argmaxLabel(Map<String, Double> dist) {
        String best = null;
        double max = -1;
        for (Map.Entry<String, Double> e : dist.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private static Integer mapAge(float[] out) {
        if (out == null || out.length == 0) return null;
        if (out.length == 1) {
            int age = (int) Math.round(out[0]);
            return Math.max(0, Math.min(120, age));
        }
        int idx = 0;
        float mx = out[0];
        for (int i = 1; i < out.length; i++) {
            if (out[i] > mx) {
                mx = out[i];
                idx = i;
            }
        }
        return idx; // interpret as class index (0..N-1) if classification
    }

    private static String mapGender(float[] out) {
        if (out == null || out.length == 0) return null;
        if (out.length == 1) return out[0] >= 0.5 ? "male" : "female";
        int idx = 0;
        float mx = out[0];
        for (int i = 1; i < out.length; i++) {
            if (out[i] > mx) {
                mx = out[i];
                idx = i;
            }
        }
        return idx == 1 ? "female" : "male";
    }

    private static double[] softmax(float[] x) {
        double max = -Double.MAX_VALUE;
        for (float v : x) if (v > max) max = v;
        double sum = 0.0;
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            y[i] = Math.exp(x[i] - max);
            sum += y[i];
        }
        if (sum == 0.0) return y;
        for (int i = 0; i < y.length; i++) y[i] /= sum;
        return y;
    }

    private static float[] toNchw(BufferedImage img, FacePreprocessor pre, int size) {
        BufferedImage src = (pre != null) ? pre.alignAndResize(img, size, size) : resize(img, size, size);
        int w = src.getWidth();
        int h = src.getHeight();
        float[] out = new float[1 * 3 * h * w];
        int cStride = h * w;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;
                int idx = y * w + x;
                out[0 * cStride + idx] = r / 255.0f;
                out[1 * cStride + idx] = g / 255.0f;
                out[2 * cStride + idx] = b / 255.0f;
            }
        }
        return out;
    }

    private static void validateFile(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Image path must not be null or blank");
        }
        File f = new File(path);
        if (!f.isFile()) {
            throw new IllegalArgumentException("Image path does not point to a file: " + path);
        }
        if (f.length() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image exceeds maximum allowed size of " + MAX_FILE_SIZE_BYTES + " bytes");
        }
    }

    private static void validateImage(BufferedImage img) {
        if (img == null) {
            Logs.warn("DeepFace", "represent.null_image", Map.of());
            throw new IllegalArgumentException("Image must not be null");
        }
        long pixels = (long) img.getWidth() * (long) img.getHeight();
        if (pixels <= 0) {
            throw new IllegalArgumentException("Image has invalid dimensions");
        }
        if (pixels > MAX_IMAGE_PIXELS) {
            Logs.warn("DeepFace", "represent.too_large_image", Map.of("width", img.getWidth(), "height", img.getHeight(), "pixels", pixels));
            throw new IllegalArgumentException("Image exceeds maximum allowed pixel count");
        }
    }

    private static BufferedImage crop(BufferedImage img, FaceRegion r) {
        BufferedImage sub = new BufferedImage(r.width(), r.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sub.createGraphics();
        g.drawImage(img, 0, 0, r.width(), r.height(), r.x(), r.y(), r.x() + r.width(), r.y() + r.height(), null);
        g.dispose();
        return sub;
    }

    private static BufferedImage cropWithMargin(BufferedImage img, FaceRegion r, int margin) {
        int x = Math.max(0, r.x() - margin);
        int y = Math.max(0, r.y() - margin);
        int w = Math.min(img.getWidth() - x, r.width() + 2 * margin);
        int h = Math.min(img.getHeight() - y, r.height() + 2 * margin);
        BufferedImage sub = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sub.createGraphics();
        g.drawImage(img, 0, 0, w, h, x, y, x + w, y + h, null);
        g.dispose();
        return sub;
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static double compute(DistanceMetric metric, float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Embedding vectors must be non-null and have same length");
        }
        double[] da = new double[a.length];
        double[] db = new double[b.length];
        for (int i = 0; i < a.length; i++) {
            da[i] = a[i];
            db[i] = b[i];
        }
        return switch (metric) {
            case COSINE -> DistanceMetrics.cosineDistance(da, db);
            case EUCLIDEAN -> DistanceMetrics.euclideanDistance(da, db);
            case EUCLIDEAN_L2 -> DistanceMetrics.euclideanL2Distance(da, db);
        };
    }

    /**
     * Computes the minimum pairwise distance between two embedding sets. Returns +INF if any set is empty or null.
     *
     * @param setA   the first set of embeddings
     * @param setB   the second set of embeddings
     * @param metric the distance metric to use
     * @return the minimum pairwise distance
     */
    private static double bestDistance(List<float[]> setA, List<float[]> setB, DistanceMetric metric) {
        if (setA == null || setB == null || setA.isEmpty() || setB.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double best = Double.POSITIVE_INFINITY;
        for (float[] a : setA) {
            for (float[] b : setB) {
                double d = compute(metric, a, b);
                if (d < best) best = d;
            }
        }
        return best;
    }

    // ========================= FIND (Map-based precomputed galleries) =========================

    /**
     * Finds the best match for a query embedding against a precomputed gallery of embeddings stored in a map.
     *
     * @param queryEmbedding        The query embedding vector.
     * @param galleryEmbeddingsById A map where keys are gallery item IDs and values are lists of embeddings for that item.
     * @param metric                The distance metric to use.
     * @param thresholdOverride     An optional override for the similarity threshold.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(float[] queryEmbedding, Map<String, List<float[]>> galleryEmbeddingsById, DistanceMetric metric, Double thresholdOverride) {
        String bestId = null;
        double best = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, List<float[]>> e : galleryEmbeddingsById.entrySet()) {
            for (float[] ge : e.getValue()) {
                double d = compute(metric, queryEmbedding, ge);
                if (d < best) {
                    best = d;
                    bestId = e.getKey();
                }
            }
        }
        double thr = (thresholdOverride != null) ? thresholdOverride : DeepFaceConfig.current().threshold(metric);
        return new FindResult(bestId, best, thr, bestId != null && best <= thr);
    }

    /**
     * Finds the top-K matches for a query embedding against a precomputed gallery map.
     *
     * @param queryEmbedding        The query embedding vector.
     * @param galleryEmbeddingsById A map of gallery embeddings by ID.
     * @param k                     The number of top matches to return.
     * @param metric                The distance metric to use.
     * @return A list of {@link FindMatch} objects, sorted by distance.
     */
    public static List<FindMatch> findTopK(float[] queryEmbedding, Map<String, List<float[]>> galleryEmbeddingsById, int k, DistanceMetric metric) {
        if (k <= 0) return List.of();
        Comparator<FindMatch> byDistanceDesc = Comparator.comparingDouble(FindMatch::distance).reversed();
        PriorityQueue<FindMatch> heap = new PriorityQueue<>(k, byDistanceDesc);
        for (Map.Entry<String, List<float[]>> e : galleryEmbeddingsById.entrySet()) {
            String id = e.getKey();
            double best = Double.POSITIVE_INFINITY;
            for (float[] ge : e.getValue()) {
                double d = compute(metric, queryEmbedding, ge);
                if (d < best) best = d;
            }
            if (heap.size() < k) heap.offer(new FindMatch(id, best));
            else if (!heap.isEmpty() && best < heap.peek().distance()) {
                heap.poll();
                heap.offer(new FindMatch(id, best));
            }
        }
        List<FindMatch> result = new ArrayList<>(heap);
        result.sort(Comparator.comparingDouble(FindMatch::distance));
        return result;
    }

    // ========================= FIND against directory (DeepFace db_path parity) =========================

    /**
     * Finds the best match for a query image against all images in a directory (recursively).
     * This method mimics the `db_path` functionality of the Python DeepFace library.
     *
     * @param queryImagePath       The file path to the query image.
     * @param galleryDirectoryPath The path to the directory containing gallery images.
     * @return A {@link FindResult} with the best match details.
     */
    public static FindResult find(String queryImagePath, String galleryDirectoryPath) {
        validateFile(queryImagePath);
        List<String> gallery = listImageFilesSecure(galleryDirectoryPath);
        if (gallery.isEmpty()) {
            Logs.warn("DeepFace", "find.dir.empty", Map.of("dir", galleryDirectoryPath));
            DeepFaceConfig cfg = DeepFaceConfig.current();
            return new FindResult(null, 1.0, cfg.threshold(cfg.defaultDistanceMetric()), false);
        }
        return find(queryImagePath, gallery);
    }

    private static List<String> listImageFilesSecure(String directoryPath) {
        if (directoryPath == null || directoryPath.isBlank()) {
            throw new IllegalArgumentException("Gallery directory path must not be null or blank");
        }
        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Gallery path is not a directory: " + directoryPath);
        }
        final long maxFiles = 10000L;
        final Set<String> allowed = imageExtensions();
        try (Stream<Path> stream = Files.walk(dir)) {
            List<String> files = stream
                .filter(Files::isRegularFile)
                .filter(p -> hasAllowedExtension(p.getFileName().toString(), allowed))
                .limit(maxFiles)
                .map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.toList());
            if (files.size() == maxFiles) {
                Logs.warn("DeepFace", "find.dir.truncated", Map.of("dir", directoryPath, "max_files", maxFiles));
            }
            List<String> validated = new ArrayList<>(files.size());
            for (String p : files) {
                try {
                    validateFile(p);
                    validated.add(p);
                } catch (Exception ex) {
                    Logs.warn("DeepFace", "find.dir.skip_invalid", Map.of("path", p));
                }
            }
            return validated;
        } catch (IOException e) {
            Logs.error("DeepFace", "find.dir.scan_failed", e, Map.of("dir", directoryPath));
            return List.of();
        }
    }

    private static boolean hasAllowedExtension(String name, Set<String> allowed) {
        int idx = name.lastIndexOf('.');
        if (idx <= 0 || idx >= name.length() - 1) return false;
        String ext = name.substring(idx + 1).toLowerCase();
        return allowed.contains(ext);
    }

    private static Set<String> imageExtensions() {
        Set<String> s = new HashSet<>();
        Collections.addAll(s, "jpg", "jpeg", "png", "bmp", "gif", "webp", "tif", "tiff");
        return s;
    }

    // ========================= MODEL SELECTION =========================
    private static float[] selectModelAndEmbed(BufferedImage processed, ModelType model, DeepFaceConfig cfg) {
        if (model == null) model = ModelType.ARCFACE;
        switch (model) {
            case VGG_FACE: {
                VGGFaceModel m = new VGGFaceModel();
                return m.generateEmbedding(processed, Math.max(32, cfg.inputSize()));
            }
            case ARCFACE: {
                ArcFaceModel m = new ArcFaceModel();
                return m.generateEmbedding(processed, cfg.arcFaceInputSize());
            }
            case FACENET: {
                FacenetModel m = new FacenetModel();
                return m.generateEmbedding(processed, cfg.facenetInputSize());
            }
            case FACENET512: {
                Facenet512Model m = new Facenet512Model();
                return m.generateEmbedding(processed, cfg.facenet512InputSize());
            }
            case OPEN_FACE: {
                OpenFaceModel m = new OpenFaceModel();
                return m.generateEmbedding(processed, cfg.openfaceInputSize());
            }
            case SFACE: {
                SFaceModel m = new SFaceModel();
                return m.generateEmbedding(processed, cfg.sfaceInputSize());
            }
            case DEEP_FACE: {
                DeepFaceModel m = new DeepFaceModel();
                return m.generateEmbedding(processed, cfg.deepfaceInputSize());
            }
            case DLIB: {
                DlibModel m = new DlibModel();
                return m.generateEmbedding(processed, DlibModel.DEFAULT_INPUT_SIZE);
            }
            default: {
                VGGFaceModel m = new VGGFaceModel();
                return m.generateEmbedding(processed, Math.max(32, cfg.inputSize()));
            }
        }
    }

    // ========================= ARCFACE CONVENIENCE =========================

    /**
     * Generates ArcFace embeddings for all detected faces in an image from a file path.
     *
     * @param imgPath The file path to the image.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> representArcFace(String imgPath) {
        return represent(imgPath, ModelType.ARCFACE, DeepFaceConfig.current().detectorBackend());
    }

    /**
     * Generates ArcFace embeddings for all detected faces in a {@link BufferedImage}.
     *
     * @param image The image to process.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> representArcFace(BufferedImage image) {
        return represent(image, ModelType.ARCFACE, DeepFaceConfig.current().detectorBackend());
    }

    /**
     * Generates ArcFace embeddings with an explicit detector backend.
     *
     * @param imgPath The file path to the image.
     * @param backend The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> representArcFace(String imgPath, DetectorBackend backend) {
        return represent(imgPath, ModelType.ARCFACE, backend);
    }

    /**
     * Generates ArcFace embeddings from a {@link BufferedImage} with an explicit detector backend.
     *
     * @param image   The image to process.
     * @param backend The detector backend to use.
     * @return A list of {@link EmbeddingResult} objects.
     */
    public static List<EmbeddingResult> representArcFace(BufferedImage image, DetectorBackend backend) {
        return represent(image, ModelType.ARCFACE, backend);
    }

    /**
     * Verifies two images using the ArcFace model and default distance metric.
     *
     * @param img1 The file path of the first image.
     * @param img2 The file path of the second image.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verifyArcFace(String img1, String img2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(img1, img2, ModelType.ARCFACE, cfg.defaultDistanceMetric(), cfg.detectorBackend());
    }

    /**
     * Verifies two {@link BufferedImage}s using the ArcFace model.
     *
     * @param img1 The first image.
     * @param img2 The second image.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verifyArcFace(BufferedImage img1, BufferedImage img2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(img1, img2, ModelType.ARCFACE, cfg.defaultDistanceMetric(), cfg.detectorBackend());
    }

    /**
     * Verifies two images from byte arrays using the ArcFace model.
     *
     * @param img1 The byte array of the first image.
     * @param img2 The byte array of the second image.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verifyArcFace(byte[] img1, byte[] img2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(img1, img2, ModelType.ARCFACE, cfg.defaultDistanceMetric(), cfg.detectorBackend());
    }

    /**
     * Verifies two images from InputStreams using the ArcFace model.
     *
     * @param img1 The InputStream of the first image.
     * @param img2 The InputStream of the second image.
     * @return A {@link VerificationResult}.
     */
    public static VerificationResult verifyArcFace(InputStream img1, InputStream img2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(img1, img2, ModelType.ARCFACE, cfg.defaultDistanceMetric(), cfg.detectorBackend());
    }

    // Heuristic: reject faces that occupy less than a minimal fraction of the image area or have too small width/height.
    private static boolean isTooSmall(FaceRegion r, int imgW, int imgH) {
        // Defaults can be overridden via system properties; stricter by default to suppress tiny false positives.
        double minFrac = readDouble("facebytes.min_face_area_fraction", 0.006); // 0.6% of image area
        int minSide = (int) readDouble("facebytes.min_face_side_pixels", 36);   // at least 36px on each side
        long area = Math.max(0, r.width()) * (long) Math.max(0, r.height());
        long imgArea = Math.max(1, imgW) * (long) imgH;
        if (r.width() < minSide || r.height() < minSide) return true;
        return area < imgArea * minFrac;
    }

    private static double readDouble(String key, double def) {
        try {
            String v = System.getProperty(key);
            if (v == null || v.isBlank()) return def;
            return Double.parseDouble(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    // Enhance small face crops by upscaling and light deblurring/contrast normalization.
    private static BufferedImage enhanceSmallFace(BufferedImage face) {
        int w = Math.max(1, face.getWidth());
        int h = Math.max(1, face.getHeight());
        int upW = Math.min(w * 4, 1024);
        int upH = Math.min(h * 4, 1024);
        BufferedImage up = new BufferedImage(upW, upH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = up.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(face, 0, 0, upW, upH, null);
        g.dispose();

        // Mild unsharp mask: up -> blur -> subtract small amount
        BufferedImage blurred = gaussianBlur(up, 1);
        BufferedImage sharp = new BufferedImage(upW, upH, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < upH; y++) {
            for (int x = 0; x < upW; x++) {
                int a = up.getRGB(x, y);
                int b = blurred.getRGB(x, y);
                int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
                int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
                int rr = clamp8((int) Math.round(ar + 0.6 * (ar - br)));
                int rg = clamp8((int) Math.round(ag + 0.6 * (ag - bg)));
                int rb = clamp8((int) Math.round(ab + 0.6 * (ab - bb)));
                sharp.setRGB(x, y, (rr << 16) | (rg << 8) | rb);
            }
        }

        return sharp;
    }

    private static BufferedImage gaussianBlur(BufferedImage src, int radius) {
        // Simple separable box approximation for small radius
        int w = src.getWidth(), h = src.getHeight();
        int[] tmp = new int[w * h];
        int[] out = new int[w * h];
        src.getRGB(0, 0, w, h, tmp, 0, w);
        int r = Math.max(1, radius);
        int size = r * 2 + 1;

        // Horizontal pass
        for (int y = 0; y < h; y++) {
            int sumR = 0, sumG = 0, sumB = 0;
            for (int k = -r; k <= r; k++) {
                int x = Math.min(w - 1, Math.max(0, k));
                int c = tmp[y * w + x];
                sumR += (c >> 16) & 0xFF;
                sumG += (c >> 8) & 0xFF;
                sumB += c & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int xr = Math.max(0, x - r - 1);
                int xa = Math.min(w - 1, x + r);
                if (x > 0) {
                    int cRemove = tmp[y * w + xr];
                    int cAdd = tmp[y * w + xa];
                    sumR += ((cAdd >> 16) & 0xFF) - ((cRemove >> 16) & 0xFF);
                    sumG += ((cAdd >> 8) & 0xFF) - ((cRemove >> 8) & 0xFF);
                    sumB += (cAdd & 0xFF) - (cRemove & 0xFF);
                }
                int rr = sumR / size, gg = sumG / size, bb = sumB / size;
                out[idx] = (rr << 16) | (gg << 8) | bb;
            }
        }

        // Vertical pass
        int[] tmp2 = out.clone();
        for (int x = 0; x < w; x++) {
            int sumR = 0, sumG = 0, sumB = 0;
            for (int k = -r; k <= r; k++) {
                int y = Math.min(h - 1, Math.max(0, k));
                int c = tmp2[y * w + x];
                sumR += (c >> 16) & 0xFF;
                sumG += (c >> 8) & 0xFF;
                sumB += c & 0xFF;
            }
            for (int y = 0; y < h; y++) {
                int yr = Math.max(0, y - r - 1);
                int ya = Math.min(h - 1, y + r);
                if (y > 0) {
                    int cRemove = tmp2[yr * w + x];
                    int cAdd = tmp2[ya * w + x];
                    sumR += ((cAdd >> 16) & 0xFF) - ((cRemove >> 16) & 0xFF);
                    sumG += ((cAdd >> 8) & 0xFF) - ((cRemove >> 8) & 0xFF);
                    sumB += (cAdd & 0xFF) - (cRemove & 0xFF);
                }
                int rr = sumR / size, gg = sumG / size, bb = sumB / size;
                out[y * w + x] = (rr << 16) | (gg << 8) | bb;
            }
        }

        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        dst.setRGB(0, 0, w, h, out, 0, w);
        return dst;
    }

    private static int clamp8(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
