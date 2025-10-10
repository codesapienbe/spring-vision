package io.github.codesapienbe.springvision.facebytes.models;

import io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig;
import io.github.codesapienbe.springvision.facebytes.core.FaceRegion;
import io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Comprehensive facial analysis engine that orchestrates multiple prediction models.
 * Provides age, gender, emotion, and race analysis from face images.
 *
 * @author FaceBytes Team
 * @since 1.0.0
 */
public final class FacialAnalysisEngine {

    private final AgePredictor agePredictor;
    private final GenderPredictor genderPredictor;
    private final EmotionPredictor emotionPredictor;
    private final RacePredictor racePredictor;
    private final DeepFaceConfig config;
    private final ExecutorService executor;

    public FacialAnalysisEngine() {
        this.config = DeepFaceConfig.current();
        this.agePredictor = new AgePredictor(config);
        this.genderPredictor = new GenderPredictor(config);
        this.emotionPredictor = new EmotionPredictor(config);
        this.racePredictor = new RacePredictor(config);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public FacialAnalysisEngine(DeepFaceConfig config) {
        this.config = config;
        this.agePredictor = new AgePredictor(config);
        this.genderPredictor = new GenderPredictor(config);
        this.emotionPredictor = new EmotionPredictor(config);
        this.racePredictor = new RacePredictor(config);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Performs comprehensive facial analysis on a single face image.
     *
     * @param face the face image to analyze
     * @return comprehensive analysis result
     * @throws DeepFaceException if analysis fails
     */
    public FacialAnalysisResult analyzeFace(BufferedImage face) throws DeepFaceException {
        return analyzeFaceWithRegion(face, null);
    }

    /**
     * Performs comprehensive facial analysis on a face image with region information.
     *
     * @param face   the face image to analyze
     * @param region the face region information
     * @return comprehensive analysis result
     * @throws DeepFaceException if analysis fails
     */
    public FacialAnalysisResult analyzeFaceWithRegion(BufferedImage face, FaceRegion region) throws DeepFaceException {
        if (face == null) {
            throw new DeepFaceException("Face image cannot be null");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Perform all analyses sequentially for now (can be parallelized later)
            int age = agePredictor.predictAge(face);
            GenderPredictor.GenderResult gender = genderPredictor.predictGender(face);
            EmotionPredictor.EmotionResult emotion = emotionPredictor.predictEmotion(face);
            RacePredictor.RaceResult race = racePredictor.predictRace(face);

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            Logs.info("FacialAnalysisEngine", "analysis.completed", Map.of(
                "age", age,
                "gender", gender.gender(),
                "emotion", emotion.emotion(),
                "race", race.race(),
                "processing_time_ms", processingTime
            ));

            return new FacialAnalysisResult(
                age, gender, emotion, race, region, processingTime
            );

        } catch (Exception e) {
            Logs.error("FacialAnalysisEngine", "analysis.failed", e, Map.of());
            throw new DeepFaceException("Facial analysis failed", e);
        }
    }

    /**
     * Performs comprehensive facial analysis on multiple faces in parallel.
     *
     * @param faces list of face images to analyze
     * @return list of analysis results
     * @throws DeepFaceException if analysis fails
     */
    public List<FacialAnalysisResult> analyzeFaces(List<BufferedImage> faces) throws DeepFaceException {
        if (faces == null || faces.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Create parallel analysis tasks
            List<CompletableFuture<FacialAnalysisResult>> futures = new ArrayList<>();

            for (BufferedImage face : faces) {
                CompletableFuture<FacialAnalysisResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return analyzeFace(face);
                    } catch (DeepFaceException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all analyses to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            allFutures.join();

            // Collect results
            List<FacialAnalysisResult> results = new ArrayList<>();
            for (CompletableFuture<FacialAnalysisResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    Logs.error("FacialAnalysisEngine", "parallel.analysis.failed", e, Map.of());
                    throw new DeepFaceException("Parallel facial analysis failed", e);
                }
            }

            return results;

        } catch (Exception e) {
            Logs.error("FacialAnalysisEngine", "parallel.analysis.failed", e, Map.of());
            throw new DeepFaceException("Parallel facial analysis failed", e);
        }
    }

    /**
     * Performs analysis with specific actions (age, gender, emotion, race).
     *
     * @param face    the face image to analyze
     * @param actions array of actions to perform
     * @return analysis result with requested attributes
     * @throws DeepFaceException if analysis fails
     */
    public FacialAnalysisResult analyzeFaceWithActions(BufferedImage face, String[] actions) throws DeepFaceException {
        if (face == null) {
            throw new DeepFaceException("Face image cannot be null");
        }

        if (actions == null || actions.length == 0) {
            // Perform all analyses if no specific actions specified
            return analyzeFace(face);
        }

        long startTime = System.currentTimeMillis();
        int age = -1;
        GenderPredictor.GenderResult gender = null;
        EmotionPredictor.EmotionResult emotion = null;
        RacePredictor.RaceResult race = null;

        try {
            // Perform requested analyses
            for (String action : actions) {
                switch (action.toLowerCase()) {
                    case "age" -> age = agePredictor.predictAge(face);
                    case "gender" -> gender = genderPredictor.predictGender(face);
                    case "emotion" -> emotion = emotionPredictor.predictEmotion(face);
                    case "race" -> race = racePredictor.predictRace(face);
                    default -> Logs.warn("FacialAnalysisEngine", "unknown.action", Map.of("action", action));
                }
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            Logs.info("FacialAnalysisEngine", "selective.analysis.completed", Map.of(
                "actions", String.join(",", actions),
                "processing_time_ms", processingTime
            ));

            return new FacialAnalysisResult(
                age, gender, emotion, race, null, processingTime
            );

        } catch (Exception e) {
            Logs.error("FacialAnalysisEngine", "selective.analysis.failed", e, Map.of("actions", String.join(",", actions)));
            throw new DeepFaceException("Selective facial analysis failed", e);
        }
    }

    /**
     * Gets the age predictor instance.
     *
     * @return the age predictor
     */
    public AgePredictor getAgePredictor() {
        return agePredictor;
    }

    /**
     * Gets the gender predictor instance.
     *
     * @return the gender predictor
     */
    public GenderPredictor getGenderPredictor() {
        return genderPredictor;
    }

    /**
     * Gets the emotion predictor instance.
     *
     * @return the emotion predictor
     */
    public EmotionPredictor getEmotionPredictor() {
        return emotionPredictor;
    }

    /**
     * Gets the race predictor instance.
     *
     * @return the race predictor
     */
    public RacePredictor getRacePredictor() {
        return racePredictor;
    }

    /**
     * Gets the current configuration.
     *
     * @return the DeepFace configuration
     */
    public DeepFaceConfig getConfig() {
        return config;
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Comprehensive facial analysis result containing all predictions.
     */
    public static final class FacialAnalysisResult {
        private final int age;
        private final GenderPredictor.GenderResult gender;
        private final EmotionPredictor.EmotionResult emotion;
        private final RacePredictor.RaceResult race;
        private final FaceRegion region;
        private final long processingTimeMs;

        public FacialAnalysisResult(int age, GenderPredictor.GenderResult gender,
                                    EmotionPredictor.EmotionResult emotion, RacePredictor.RaceResult race,
                                    FaceRegion region, long processingTimeMs) {
            this.age = age;
            this.gender = gender;
            this.emotion = emotion;
            this.race = race;
            this.region = region;
            this.processingTimeMs = processingTimeMs;
        }

        public int age() {
            return age;
        }

        public GenderPredictor.GenderResult gender() {
            return gender;
        }

        public EmotionPredictor.EmotionResult emotion() {
            return emotion;
        }

        public RacePredictor.RaceResult race() {
            return race;
        }

        public FaceRegion region() {
            return region;
        }

        public long processingTimeMs() {
            return processingTimeMs;
        }

        @Override
        public String toString() {
            return String.format("FacialAnalysisResult{age=%d, gender=%s, emotion=%s, race=%s, processingTime=%dms}",
                age, gender, emotion, race, processingTimeMs);
        }
    }
}
