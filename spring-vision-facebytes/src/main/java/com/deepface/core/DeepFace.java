package com.deepface.core;

import com.deepface.config.DeepFaceConfig;
import com.deepface.detectors.DetectorFactory;
import com.deepface.detectors.FaceDetector;
import com.deepface.enums.DetectorBackend;
import com.deepface.enums.DistanceMetric;
import com.deepface.enums.ModelType;
import com.deepface.models.VGGFaceModel;
import com.deepface.models.AnalysisOnnxModels;
import com.deepface.models.OnnxSimpleModel;
import com.deepface.models.MockAnalyzers;
import com.deepface.utils.DistanceMetrics;
import com.deepface.utils.FacePreprocessor;
import com.deepface.utils.Logs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;

import static com.deepface.utils.ImageUtils.loadImage;

/**
 * Core FaceBytes API providing DeepFace-like operations: verify, represent, extractFaces, analyze.
 * <p>
 * All path-based methods validate file size and log structured events. Analysis is not implemented yet.
 */
public final class DeepFace {

    private static final Logger log = LoggerFactory.getLogger(DeepFace.class);
    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50MB guardrail

    private DeepFace() {}

    /**
     * Finds the best match in gallery for the query image using the configured distance metric.
     */
    public static FindResult find(String queryImagePath, List<String> galleryImagePaths) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        validateFile(queryImagePath);
        for (String p : galleryImagePaths) validateFile(p);
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImagePath));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY; String bestPath = null;
            for (String p : galleryImagePaths) {
                List<EmbeddingResult> g = represent(loadImage(p));
                for (EmbeddingResult qe : q) {
                    for (EmbeddingResult ge : g) {
                        double d = compute(metric, qe.embedding(), ge.embedding());
                        if (d < best) { best = d; bestPath = p; }
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
     * Finds best match for query bytes against gallery paths.
     */
    public static FindResult find(byte[] queryImageBytes, List<String> galleryImagePaths) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageBytes));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY; String bestPath = null;
            for (String p : galleryImagePaths) {
                validateFile(p);
                List<EmbeddingResult> g = represent(loadImage(p));
                for (EmbeddingResult qe : q) for (EmbeddingResult ge : g) {
                    double d = compute(metric, qe.embedding(), ge.embedding());
                    if (d < best) { best = d; bestPath = p; }
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
     * Finds best match for query stream against gallery paths.
     */
    public static FindResult find(InputStream queryImageStream, List<String> galleryImagePaths) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageStream));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY; String bestPath = null;
            for (String p : galleryImagePaths) {
                validateFile(p);
                List<EmbeddingResult> g = represent(loadImage(p));
                for (EmbeddingResult qe : q) for (EmbeddingResult ge : g) {
                    double d = compute(metric, qe.embedding(), ge.embedding());
                    if (d < best) { best = d; bestPath = p; }
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
     * Finds best match using precomputed embeddings for query and a list of gallery embeddings.
     */
    public static FindResult find(float[] queryEmbedding, List<float[]> galleryEmbeddings) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        double best = Double.POSITIVE_INFINITY; int idx = -1;
        for (int i = 0; i < galleryEmbeddings.size(); i++) {
            float[] g = galleryEmbeddings.get(i);
            double d = compute(metric, queryEmbedding, g);
            if (d < best) { best = d; idx = i; }
        }
        double thr = cfg.threshold(metric);
        String bestPath = (idx >= 0) ? String.valueOf(idx) : null; // index-as-identifier if paths unknown
        return new FindResult(bestPath, best, thr, best <= thr);
    }

    public static FindResult find(float[] queryEmbedding, List<float[]> galleryEmbeddings, List<String> galleryIds) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        double best = Double.POSITIVE_INFINITY; int idx = -1;
        for (int i = 0; i < galleryEmbeddings.size(); i++) {
            float[] g = galleryEmbeddings.get(i);
            double d = compute(metric, queryEmbedding, g);
            if (d < best) { best = d; idx = i; }
        }
        double thr = cfg.threshold(metric);
        String bestId = (idx >= 0 && galleryIds != null && idx < galleryIds.size()) ? galleryIds.get(idx) : (idx >= 0 ? String.valueOf(idx) : null);
        return new FindResult(bestId, best, thr, bestId != null && best <= thr);
    }

    /**
     * Returns top-K closest matches for a query embedding against gallery embeddings.
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
     * Convenience: top-K over gallery paths by computing embeddings internally.
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
                for (float[] qe : qEmbeddings) for (float[] ge : gEmbeddings) {
                    double d = compute(metric, qe, ge);
                    if (d < best) best = d;
                }
                String id = String.valueOf(i);
                if (heap.size() < k) heap.offer(new FindMatch(id, best));
                else if (!heap.isEmpty() && best < heap.peek().distance()) { heap.poll(); heap.offer(new FindMatch(id, best)); }
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
     * Finds best match for query path against gallery bytes list.
     */
    public static FindResult find(String queryImagePath, List<byte[]> galleryImageBytesList, List<String> galleryIds) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        validateFile(queryImagePath);
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImagePath));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY; String bestId = null;
            for (int i = 0; i < galleryImageBytesList.size(); i++) {
                List<EmbeddingResult> g = represent(loadImage(galleryImageBytesList.get(i)));
                for (EmbeddingResult qe : q) for (EmbeddingResult ge : g) {
                    double d = compute(metric, qe.embedding(), ge.embedding());
                    if (d < best) { best = d; bestId = (galleryIds != null && i < galleryIds.size()) ? galleryIds.get(i) : String.valueOf(i); }
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
     * Finds best match for query bytes against gallery bytes list.
     */
    public static FindResult find(byte[] queryImageBytes, List<byte[]> galleryImageBytesList, List<String> galleryIds) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageBytes));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY; String bestId = null;
            for (int i = 0; i < galleryImageBytesList.size(); i++) {
                List<EmbeddingResult> g = represent(loadImage(galleryImageBytesList.get(i)));
                for (EmbeddingResult qe : q) for (EmbeddingResult ge : g) {
                    double d = compute(metric, qe.embedding(), ge.embedding());
                    if (d < best) { best = d; bestId = (galleryIds != null && i < galleryIds.size()) ? galleryIds.get(i) : String.valueOf(i); }
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
     * Finds best match for query stream against gallery streams.
     */
    public static FindResult find(InputStream queryImageStream, List<InputStream> galleryStreams, List<String> galleryIds) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<EmbeddingResult> q = represent(loadImage(queryImageStream));
            if (q.isEmpty()) return new FindResult(null, 1.0, cfg.threshold(metric), false);
            double best = Double.POSITIVE_INFINITY; String bestId = null;
            for (int i = 0; i < galleryStreams.size(); i++) {
                List<EmbeddingResult> g = represent(loadImage(galleryStreams.get(i)));
                for (EmbeddingResult qe : q) for (EmbeddingResult ge : g) {
                    double d = compute(metric, qe.embedding(), ge.embedding());
                    if (d < best) { best = d; bestId = (galleryIds != null && i < galleryIds.size()) ? galleryIds.get(i) : String.valueOf(i); }
                }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestId, best, thr, best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }

    // ========================= VERIFY =========================

    /** Convenience: verify with defaults using buffered images. */
    public static VerificationResult verify(BufferedImage img1, BufferedImage img2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(img1, img2, ModelType.DEEP_FACE, cfg.defaultDistanceMetric(), cfg.detectorBackend());
    }

    /** Convenience: verify with defaults using bytes. */
    public static VerificationResult verify(byte[] img1, byte[] img2) {
        try {
            return verify(loadImage(img1), loadImage(img2));
        } catch (IOException e) {
            Logs.error("DeepFace", "verify.load_failed", e, Map.of());
            DeepFaceConfig cfg = DeepFaceConfig.current();
            return new VerificationResult(false, 1.0, cfg.threshold(cfg.defaultDistanceMetric()), ModelType.DEEP_FACE, cfg.detectorBackend(), 0L);
        }
    }

    /** Convenience: verify with defaults using streams. */
    public static VerificationResult verify(InputStream img1, InputStream img2) {
        try {
            return verify(loadImage(img1), loadImage(img2));
        } catch (IOException e) {
            Logs.error("DeepFace", "verify.load_failed", e, Map.of());
            DeepFaceConfig cfg = DeepFaceConfig.current();
            return new VerificationResult(false, 1.0, cfg.threshold(cfg.defaultDistanceMetric()), ModelType.DEEP_FACE, cfg.detectorBackend(), 0L);
        }
    }

    /**
     * Verifies whether two images depict the same person using default configuration.
     */
    public static VerificationResult verify(String img1, String img2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(img1, img2, ModelType.DEEP_FACE, cfg.defaultDistanceMetric(), cfg.detectorBackend());
    }

    /**
     * Verifies whether two images depict the same person.
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

    public static VerificationResult verify(float[] emb1, float[] emb2, ModelType model, DistanceMetric distanceMetric) {
        double d = compute(distanceMetric, emb1, emb2);
        double thr = DeepFaceConfig.current().threshold(distanceMetric);
        boolean ok = d <= thr;
        return new VerificationResult(ok, d, thr, model, DeepFaceConfig.current().detectorBackend(), 0L);
    }

    /**
     * Verifies two buffered images with full control over model/distance/backend.
     */
    public static VerificationResult verify(BufferedImage img1, BufferedImage img2, ModelType model,
                                            DistanceMetric distance, DetectorBackend detector) {
        long start = System.currentTimeMillis();
        DeepFaceConfig cfg = DeepFaceConfig.current();
        List<EmbeddingResult> e1 = represent(img1);
        List<EmbeddingResult> e2 = represent(img2);
        if (e1.isEmpty() || e2.isEmpty()) {
            return new VerificationResult(false, 1.0, cfg.threshold(distance), model, detector, System.currentTimeMillis() - start);
        }
        double best = Double.POSITIVE_INFINITY;
        for (EmbeddingResult a : e1) {
            for (EmbeddingResult b : e2) {
                double d = compute(distance, a.embedding(), b.embedding());
                if (d < best) best = d;
            }
        }
        double thr = cfg.threshold(distance);
        boolean ok = best <= thr;
        return new VerificationResult(ok, best, thr, model, detector, System.currentTimeMillis() - start);
    }

    /**
     * Verifies two images given as bytes.
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
     * Verifies two images given as streams.
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

    /** Defaulted verify for two embeddings using current config. */
    public static VerificationResult verify(float[] emb1, float[] emb2) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        return verify(emb1, emb2, ModelType.DEEP_FACE, cfg.defaultDistanceMetric());
    }

    /** Verify two sets of embeddings (e.g., multiple faces) using minimum pairwise distance. */
    public static VerificationResult verifyEmbeddings(List<float[]> setA, List<float[]> setB, ModelType model, DistanceMetric metric) {
        double best = bestDistance(setA, setB, metric);
        double thr = DeepFaceConfig.current().threshold(metric);
        boolean ok = best <= thr;
        return new VerificationResult(ok, best, thr, model, DeepFaceConfig.current().detectorBackend(), 0L);
    }

    // ========================= REPRESENT (multiple overloads) =========================

    /**
     * Generates embeddings for all faces in the image located at {@code imgPath}.
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
     * Generates embeddings for all faces in the provided image bytes.
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
     * Generates embeddings for all faces in the provided image stream.
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
     * Generates embeddings for all faces in the provided image.
     */
    public static List<EmbeddingResult> represent(BufferedImage img) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        FaceDetector fd = DetectorFactory.create(cfg.detectorBackend());
        List<FaceRegion> regions = fd.detectFaces(img);
        if (regions.isEmpty()) {
            return Collections.emptyList();
        }
        VGGFaceModel model = new VGGFaceModel();
        FacePreprocessor pre = cfg.align() ? new FacePreprocessor() : null;
        int target = cfg.inputSize();
        int margin = cfg.margin();
        List<EmbeddingResult> out = new ArrayList<>();
        for (FaceRegion r : regions) {
            BufferedImage face = cropWithMargin(img, r, margin);
            BufferedImage processed = (pre != null) ? pre.alignAndResize(face, target, target) : resize(face, target, target);
            float[] emb = model.generateEmbedding(processed, target);
            out.add(new EmbeddingResult(emb, r));
        }
        return out;
    }

    // ========================= REPRESENT (embeddings only helpers) =========================

    public static List<float[]> representEmbeddings(String imgPath) {
        validateFile(imgPath);
        try {
            return representEmbeddings(loadImage(imgPath));
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    public static List<float[]> representEmbeddings(byte[] imageBytes) {
        try {
            return representEmbeddings(loadImage(imageBytes));
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    public static List<float[]> representEmbeddings(InputStream imageStream) {
        try {
            return representEmbeddings(loadImage(imageStream));
        } catch (IOException e) {
            Logs.error("DeepFace", "representEmbeddings.load_failed", e, Map.of());
            return List.of();
        }
    }

    public static List<float[]> representEmbeddings(BufferedImage image) {
        List<EmbeddingResult> results = represent(image);
        List<float[]> out = new ArrayList<>(results.size());
        for (EmbeddingResult r : results) out.add(r.embedding());
        return out;
    }

    // ========================= DISTANCE UTILITY =========================

    public static double distance(float[] emb1, float[] emb2, DistanceMetric metric) {
        return compute(metric, emb1, emb2);
    }

    // ========================= EXTRACT FACES (multiple overloads) =========================

    /**
     * Extracts face crops from the image located at {@code imgPath}.
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
     * Extracts face crops from image bytes.
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
     * Extracts face crops from image stream.
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
     * Extracts face crops from the provided buffered image.
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

    // ========================= ANALYZE =========================

    /** Convenience: analyze with defaults (all actions) from path. */
    public static List<AnalysisResult> analyze(String imgPath) {
        return analyze(imgPath, null);
    }

    /** Convenience: analyze with defaults (all actions) from buffered image. */
    public static List<AnalysisResult> analyze(BufferedImage image) {
        return analyze(image, null);
    }

    /** Analyze from bytes with explicit actions. */
    public static List<AnalysisResult> analyze(byte[] imageBytes, String[] actions) {
        try {
            return analyze(loadImage(imageBytes), actions);
        } catch (IOException e) {
            Logs.error("DeepFace", "analyze.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /** Analyze from stream with explicit actions. */
    public static List<AnalysisResult> analyze(InputStream imageStream, String[] actions) {
        try {
            return analyze(loadImage(imageStream), actions);
        } catch (IOException e) {
            Logs.error("DeepFace", "analyze.load_failed", e, Map.of());
            return Collections.emptyList();
        }
    }

    /** Convenience: analyze from bytes with defaults (all actions). */
    public static List<AnalysisResult> analyze(byte[] imageBytes) {
        return analyze(imageBytes, null);
    }

    /** Convenience: analyze from stream with defaults (all actions). */
    public static List<AnalysisResult> analyze(InputStream imageStream) {
        return analyze(imageStream, null);
    }

    /**
     * Analyzes faces in the image located at {@code imgPath} using configured ONNX models.
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
     * Analyzes faces in the provided image using configured ONNX models.
     * Supported actions: age, gender, emotion, race.
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
            BufferedImage processed = (pre != null) ? pre.alignAndResize(crop, 224, 224) : resize(crop, 224, 224);
            Integer age = null;
            String gender = null;
            String dominantEmotion = null;
            Map<String, Double> emotionDist = Map.of();
            Map<String, Double> raceDist = Map.of();

            try {
                boolean usedFallback = false;
                if (doAge) {
                    if (cfg.ageOnnxPath() != null) {
                        int size = cfg.ageInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getAge().run(in, new long[]{1, 3, size, size});
                        age = mapAge(out);
                    } else {
                        age = MockAnalyzers.predictAge(processed);
                        usedFallback = true;
                    }
                }
                if (doGender) {
                    if (cfg.genderOnnxPath() != null) {
                        int size = cfg.genderInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getGender().run(in, new long[]{1, 3, size, size});
                        gender = mapGender(out);
                    } else {
                        gender = MockAnalyzers.predictGender(processed);
                        usedFallback = true;
                    }
                }
                if (doEmotion) {
                    if (cfg.emotionOnnxPath() != null) {
                        int size = cfg.emotionInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getEmotion().run(in, new long[]{1, 3, size, size});
                        emotionDist = mapDistribution(out, emotionLabels());
                        dominantEmotion = argmaxLabel(emotionDist);
                    } else {
                        emotionDist = MockAnalyzers.predictEmotionDistribution(processed);
                        dominantEmotion = argmaxLabel(emotionDist);
                        usedFallback = true;
                    }
                }
                if (doRace) {
                    if (cfg.raceOnnxPath() != null) {
                        int size = cfg.raceInputSize();
                        float[] in = toNchw(crop, pre, size);
                        float[] out = AnalysisOnnxModels.getRace().run(in, new long[]{1, 3, size, size});
                        raceDist = mapDistribution(out, raceLabels());
                    } else {
                        raceDist = MockAnalyzers.predictRaceDistribution(processed);
                        usedFallback = true;
                    }
                }
                if (usedFallback) {
                    Logs.info("DeepFace", "analyze.fallback_mock", Map.of("reason", "onnx_not_configured"));
                }
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
        return new String[]{"angry","disgust","fear","happy","sad","surprise","neutral"};
    }

    private static String[] raceLabels() {
        return new String[]{"asian","indian","black","white","middle_eastern","latino_hispanic"};
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
        String best = null; double max = -1;
        for (Map.Entry<String, Double> e : dist.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    private static Integer mapAge(float[] out) {
        if (out == null || out.length == 0) return null;
        if (out.length == 1) {
            int age = (int) Math.round(out[0]);
            return Math.max(0, Math.min(120, age));
        }
        int idx = 0; float mx = out[0];
        for (int i = 1; i < out.length; i++) {
            if (out[i] > mx) { mx = out[i]; idx = i; }
        }
        return idx; // interpret as class index (0..N-1) if classification
    }

    private static String mapGender(float[] out) {
        if (out == null || out.length == 0) return null;
        if (out.length == 1) return out[0] >= 0.5 ? "male" : "female";
        int idx = 0; float mx = out[0];
        for (int i = 1; i < out.length; i++) {
            if (out[i] > mx) { mx = out[i]; idx = i; }
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
        for (int i = 0; i < a.length; i++) { da[i] = a[i]; db[i] = b[i]; }
        return switch (metric) {
            case COSINE -> DistanceMetrics.cosineDistance(da, db);
            case EUCLIDEAN -> DistanceMetrics.euclideanDistance(da, db);
            case EUCLIDEAN_L2 -> DistanceMetrics.euclideanL2Distance(da, db);
        };
    }

    /**
     * Computes the minimum pairwise distance between two embedding sets. Returns +INF if any set is empty or null.
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
     * Finds best match for a query embedding against a precomputed gallery map id -> embeddings list.
     */
    public static FindResult find(float[] queryEmbedding, Map<String, List<float[]>> galleryEmbeddingsById, DistanceMetric metric, Double thresholdOverride) {
        String bestId = null; double best = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, List<float[]>> e : galleryEmbeddingsById.entrySet()) {
            for (float[] ge : e.getValue()) {
                double d = compute(metric, queryEmbedding, ge);
                if (d < best) { best = d; bestId = e.getKey(); }
            }
        }
        double thr = (thresholdOverride != null) ? thresholdOverride : DeepFaceConfig.current().threshold(metric);
        return new FindResult(bestId, best, thr, bestId != null && best <= thr);
    }

    /**
     * Returns top-K matches for a query embedding against a precomputed gallery map.
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
            else if (!heap.isEmpty() && best < heap.peek().distance()) { heap.poll(); heap.offer(new FindMatch(id, best)); }
        }
        List<FindMatch> result = new ArrayList<>(heap);
        result.sort(Comparator.comparingDouble(FindMatch::distance));
        return result;
    }

    // ========================= FIND (multi-embedding) =========================

    /** Find best gallery id for a set of query embeddings against gallery embeddings list. */
    public static FindResult find(List<float[]> queryEmbeddings, List<float[]> galleryEmbeddings, List<String> galleryIds, DistanceMetric metric, Double thresholdOverride) {
        String bestId = null; double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < galleryEmbeddings.size(); i++) {
            double d = Double.POSITIVE_INFINITY;
            for (float[] q : queryEmbeddings) {
                d = Math.min(d, compute(metric, q, galleryEmbeddings.get(i)));
            }
            if (d < best) { best = d; bestId = (galleryIds != null && i < galleryIds.size()) ? galleryIds.get(i) : String.valueOf(i); }
        }
        double thr = (thresholdOverride != null) ? thresholdOverride : DeepFaceConfig.current().threshold(metric);
        return new FindResult(bestId, best, thr, bestId != null && best <= thr);
    }

    /** Top-K results for a set of query embeddings against a map of id->embeddings list. */
    public static List<FindMatch> findTopK(List<float[]> queryEmbeddings, Map<String, List<float[]>> galleryEmbeddingsById, int k, DistanceMetric metric) {
        if (k <= 0) return List.of();
        Comparator<FindMatch> byDistanceDesc = Comparator.comparingDouble(FindMatch::distance).reversed();
        PriorityQueue<FindMatch> heap = new PriorityQueue<>(k, byDistanceDesc);
        for (Map.Entry<String, List<float[]>> e : galleryEmbeddingsById.entrySet()) {
            String id = e.getKey();
            double best = Double.POSITIVE_INFINITY;
            for (float[] q : queryEmbeddings) {
                for (float[] ge : e.getValue()) {
                    double d = compute(metric, q, ge);
                    if (d < best) best = d;
                }
            }
            if (heap.size() < k) heap.offer(new FindMatch(id, best));
            else if (!heap.isEmpty() && best < heap.peek().distance()) { heap.poll(); heap.offer(new FindMatch(id, best)); }
        }
        List<FindMatch> result = new ArrayList<>(heap);
        result.sort(Comparator.comparingDouble(FindMatch::distance));
        return result;
    }

    // ========================= GALLERY UTILITIES =========================

    /** Precompute embeddings for a gallery of paths keyed by id. */
    public static Map<String, List<float[]>> computeGalleryEmbeddings(Map<String, List<String>> galleryPathsById) {
        Map<String, List<float[]>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : galleryPathsById.entrySet()) {
            String id = e.getKey();
            List<float[]> acc = new ArrayList<>();
            for (String path : e.getValue()) {
                validateFile(path);
                try {
                    acc.addAll(representEmbeddings(loadImage(path)));
                } catch (IOException ex) {
                    Logs.error("DeepFace", "computeGalleryEmbeddings.load_failed", ex, Map.of());
                }
            }
            out.put(id, acc);
        }
        return out;
    }

    /** Precompute embeddings for a gallery of byte arrays keyed by id. */
    public static Map<String, List<float[]>> computeGalleryEmbeddingsFromBytes(Map<String, List<byte[]>> galleryBytesById) {
        Map<String, List<float[]>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<byte[]>> e : galleryBytesById.entrySet()) {
            String id = e.getKey();
            List<float[]> acc = new ArrayList<>();
            for (byte[] bytes : e.getValue()) {
                try {
                    acc.addAll(representEmbeddings(loadImage(bytes)));
                } catch (IOException ex) {
                    Logs.error("DeepFace", "computeGalleryEmbeddingsFromBytes.load_failed", ex, Map.of());
                }
            }
            out.put(id, acc);
        }
        return out;
    }

    /** Precompute embeddings for a gallery of streams keyed by id. */
    public static Map<String, List<float[]>> computeGalleryEmbeddingsFromStreams(Map<String, List<InputStream>> galleryStreamsById) {
        Map<String, List<float[]>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<InputStream>> e : galleryStreamsById.entrySet()) {
            String id = e.getKey();
            List<float[]> acc = new ArrayList<>();
            for (InputStream is : e.getValue()) {
                try {
                    acc.addAll(representEmbeddings(loadImage(is)));
                } catch (IOException ex) {
                    Logs.error("DeepFace", "computeGalleryEmbeddingsFromStreams.load_failed", ex, Map.of());
                }
            }
            out.put(id, acc);
        }
        return out;
    }

    // ========================= FIND over Map<String, List<path/bytes/streams>> =========================

    /** Find best id for query path against map of id->paths. */
    public static FindResult find(String queryImagePath, Map<String, List<String>> galleryPathsById) {
        validateFile(queryImagePath);
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<float[]> qEmb = representEmbeddings(loadImage(queryImagePath));
            String bestId = null; double best = Double.POSITIVE_INFINITY;
            for (Map.Entry<String, List<String>> e : galleryPathsById.entrySet()) {
                double bestForId = Double.POSITIVE_INFINITY;
                for (String p : e.getValue()) {
                    validateFile(p);
                    List<float[]> gEmb = representEmbeddings(loadImage(p));
                    for (float[] qe : qEmb) for (float[] ge : gEmb) {
                        double d = compute(metric, qe, ge);
                        if (d < bestForId) bestForId = d;
                    }
                }
                if (bestForId < best) { best = bestForId; bestId = e.getKey(); }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestId, best, thr, bestId != null && best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.map_paths.load_failed", e, Map.of());
            return new FindResult(null, 1.0, DeepFaceConfig.current().threshold(metric), false);
        }
    }

    /** Top-K for query path against map of id->paths. */
    public static List<FindMatch> findTopK(String queryImagePath, Map<String, List<String>> galleryPathsById, int k) {
        validateFile(queryImagePath);
        if (k <= 0) return List.of();
        try {
            List<float[]> qEmb = representEmbeddings(loadImage(queryImagePath));
            DeepFaceConfig cfg = DeepFaceConfig.current();
            DistanceMetric metric = cfg.defaultDistanceMetric();
            Comparator<FindMatch> byDistanceDesc = Comparator.comparingDouble(FindMatch::distance).reversed();
            PriorityQueue<FindMatch> heap = new PriorityQueue<>(k, byDistanceDesc);
            for (Map.Entry<String, List<String>> e : galleryPathsById.entrySet()) {
                String id = e.getKey();
                double best = Double.POSITIVE_INFINITY;
                for (String p : e.getValue()) {
                    validateFile(p);
                    List<float[]> gEmb = representEmbeddings(loadImage(p));
                    for (float[] qe : qEmb) for (float[] ge : gEmb) {
                        double d = compute(metric, qe, ge);
                        if (d < best) best = d;
                    }
                }
                if (heap.size() < k) heap.offer(new FindMatch(id, best));
                else if (!heap.isEmpty() && best < heap.peek().distance()) { heap.poll(); heap.offer(new FindMatch(id, best)); }
            }
            List<FindMatch> result = new ArrayList<>(heap);
            result.sort(Comparator.comparingDouble(FindMatch::distance));
            return result;
        } catch (IOException e) {
            Logs.error("DeepFace", "findTopK.map_paths.load_failed", e, Map.of());
            return List.of();
        }
    }

    /** Find best id for query bytes against map of id->bytes list. */
    public static FindResult find(byte[] queryImageBytes, Map<String, List<byte[]>> galleryBytesById) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<float[]> qEmb = representEmbeddings(loadImage(queryImageBytes));
            String bestId = null; double best = Double.POSITIVE_INFINITY;
            for (Map.Entry<String, List<byte[]>> e : galleryBytesById.entrySet()) {
                double bestForId = Double.POSITIVE_INFINITY;
                for (byte[] b : e.getValue()) {
                    List<float[]> gEmb = representEmbeddings(loadImage(b));
                    for (float[] qe : qEmb) for (float[] ge : gEmb) {
                        double d = compute(metric, qe, ge);
                        if (d < bestForId) bestForId = d;
                    }
                }
                if (bestForId < best) { best = bestForId; bestId = e.getKey(); }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestId, best, thr, bestId != null && best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.map_bytes.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }

    /** Top-K for query bytes against map of id->bytes list. */
    public static List<FindMatch> findTopK(byte[] queryImageBytes, Map<String, List<byte[]>> galleryBytesById, int k) {
        if (k <= 0) return List.of();
        try {
            List<float[]> qEmb = representEmbeddings(loadImage(queryImageBytes));
            DeepFaceConfig cfg = DeepFaceConfig.current();
            DistanceMetric metric = cfg.defaultDistanceMetric();
            Comparator<FindMatch> byDistanceDesc = Comparator.comparingDouble(FindMatch::distance).reversed();
            PriorityQueue<FindMatch> heap = new PriorityQueue<>(k, byDistanceDesc);
            for (Map.Entry<String, List<byte[]>> e : galleryBytesById.entrySet()) {
                String id = e.getKey();
                double best = Double.POSITIVE_INFINITY;
                for (byte[] b : e.getValue()) {
                    List<float[]> gEmb = representEmbeddings(loadImage(b));
                    for (float[] qe : qEmb) for (float[] ge : gEmb) {
                        double d = compute(metric, qe, ge);
                        if (d < best) best = d;
                    }
                }
                if (heap.size() < k) heap.offer(new FindMatch(id, best));
                else if (!heap.isEmpty() && best < heap.peek().distance()) { heap.poll(); heap.offer(new FindMatch(id, best)); }
            }
            List<FindMatch> result = new ArrayList<>(heap);
            result.sort(Comparator.comparingDouble(FindMatch::distance));
            return result;
        } catch (IOException e) {
            Logs.error("DeepFace", "findTopK.map_bytes.load_failed", e, Map.of());
            return List.of();
        }
    }

    /** Find best id for query stream against map of id->streams list. */
    public static FindResult find(InputStream queryImageStream, Map<String, List<InputStream>> galleryStreamsById) {
        DeepFaceConfig cfg = DeepFaceConfig.current();
        DistanceMetric metric = cfg.defaultDistanceMetric();
        try {
            List<float[]> qEmb = representEmbeddings(loadImage(queryImageStream));
            String bestId = null; double best = Double.POSITIVE_INFINITY;
            for (Map.Entry<String, List<InputStream>> e : galleryStreamsById.entrySet()) {
                double bestForId = Double.POSITIVE_INFINITY;
                for (InputStream is : e.getValue()) {
                    List<float[]> gEmb = representEmbeddings(loadImage(is));
                    for (float[] qe : qEmb) for (float[] ge : gEmb) {
                        double d = compute(metric, qe, ge);
                        if (d < bestForId) bestForId = d;
                    }
                }
                if (bestForId < best) { best = bestForId; bestId = e.getKey(); }
            }
            double thr = cfg.threshold(metric);
            return new FindResult(bestId, best, thr, bestId != null && best <= thr);
        } catch (IOException e) {
            Logs.error("DeepFace", "find.map_streams.load_failed", e, Map.of());
            return new FindResult(null, 1.0, cfg.threshold(metric), false);
        }
    }

    /** Top-K for query stream against map of id->streams list. */
    public static List<FindMatch> findTopK(InputStream queryImageStream, Map<String, List<InputStream>> galleryStreamsById, int k) {
        if (k <= 0) return List.of();
        try {
            List<float[]> qEmb = representEmbeddings(loadImage(queryImageStream));
            DeepFaceConfig cfg = DeepFaceConfig.current();
            DistanceMetric metric = cfg.defaultDistanceMetric();
            Comparator<FindMatch> byDistanceDesc = Comparator.comparingDouble(FindMatch::distance).reversed();
            PriorityQueue<FindMatch> heap = new PriorityQueue<>(k, byDistanceDesc);
            for (Map.Entry<String, List<InputStream>> e : galleryStreamsById.entrySet()) {
                String id = e.getKey();
                double best = Double.POSITIVE_INFINITY;
                for (InputStream is : e.getValue()) {
                    List<float[]> gEmb = representEmbeddings(loadImage(is));
                    for (float[] qe : qEmb) for (float[] ge : gEmb) {
                        double d = compute(metric, qe, ge);
                        if (d < best) best = d;
                    }
                }
                if (heap.size() < k) heap.offer(new FindMatch(id, best));
                else if (!heap.isEmpty() && best < heap.peek().distance()) { heap.poll(); heap.offer(new FindMatch(id, best)); }
            }
            List<FindMatch> result = new ArrayList<>(heap);
            result.sort(Comparator.comparingDouble(FindMatch::distance));
            return result;
        } catch (IOException e) {
            Logs.error("DeepFace", "findTopK.map_streams.load_failed", e, Map.of());
            return List.of();
        }
    }
}

