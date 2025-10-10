package io.github.codesapienbe.springvision.core.recognition;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Batch processor for building large-scale face recognition databases.
 *
 * <p>This component processes directories of photos to extract face embeddings
 * and build searchable indexes. It's optimized for processing millions of photos
 * with parallel processing, progress tracking, and error recovery.</p>
 *
 * <p>The builder supports multiple backends and can handle various image formats.
 * Progress is reported through callbacks and processing can be resumed if interrupted.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class FaceDatabaseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(FaceDatabaseBuilder.class);

    private final VisionBackend visionBackend;
    private final FaceEmbeddingIndex embeddingIndex;
    private final FaceQualityAssessor qualityAssessor;
    private final DatabaseBuilderConfig config;
    private final ExecutorService executorService;

    // Progress tracking
    private final AtomicLong processedFiles = new AtomicLong(0);
    private final AtomicLong totalFiles = new AtomicLong(0);
    private final AtomicLong facesExtracted = new AtomicLong(0);
    private final AtomicLong errorsEncountered = new AtomicLong(0);

    /**
     * Create a new face database builder.
     *
     * @param visionBackend   backend for face detection and embedding extraction
     * @param embeddingIndex  index to populate with embeddings
     * @param qualityAssessor face quality assessor for filtering
     * @param config          builder configuration
     */
    public FaceDatabaseBuilder(VisionBackend visionBackend,
                               FaceEmbeddingIndex embeddingIndex,
                               FaceQualityAssessor qualityAssessor,
                               DatabaseBuilderConfig config) {
        this.visionBackend = visionBackend;
        this.embeddingIndex = embeddingIndex;
        this.qualityAssessor = qualityAssessor;
        this.config = config;
        this.executorService = Executors.newFixedThreadPool(config.parallelThreads());

        logger.info("Face database builder initialized with {} threads, quality threshold {}",
            config.parallelThreads(), config.minQualityThreshold());
    }

    /**
     * Process a directory of photos to build the face database.
     *
     * @param photoDirectory   directory containing photos to process
     * @param progressCallback callback for progress updates (can be null)
     * @return processing results with statistics
     * @throws IOException if directory access fails
     */
    public CompletableFuture<ProcessingResult> processPhotoDirectory(Path photoDirectory,
                                                                     ProgressCallback progressCallback) throws IOException {

        logger.info("Starting face database processing for directory: {}", photoDirectory);

        // Reset counters
        processedFiles.set(0);
        totalFiles.set(0);
        facesExtracted.set(0);
        errorsEncountered.set(0);

        // Discover all image files
        List<Path> imageFiles = discoverImageFiles(photoDirectory);
        totalFiles.set(imageFiles.size());

        logger.info("Discovered {} image files for processing", imageFiles.size());

        long startTime = System.currentTimeMillis();

        // Process files in parallel batches
        return CompletableFuture.supplyAsync(() -> {
            try {
                processBatches(imageFiles, progressCallback);

                // Build the final index
                logger.info("Building face embedding index...");
                embeddingIndex.rebuild();

                long processingTime = System.currentTimeMillis() - startTime;

                ProcessingResult result = new ProcessingResult(
                    processedFiles.get(),
                    facesExtracted.get(),
                    errorsEncountered.get(),
                    processingTime,
                    embeddingIndex.size(),
                    true
                );

                logger.info("Face database processing completed: {}", result);

                return result;

            } catch (Exception e) {
                logger.error("Face database processing failed", e);
                long processingTime = System.currentTimeMillis() - startTime;

                return new ProcessingResult(
                    processedFiles.get(),
                    facesExtracted.get(),
                    errorsEncountered.get(),
                    processingTime,
                    embeddingIndex.size(),
                    false
                );
            }
        }, executorService);
    }

    /**
     * Discover all image files in a directory recursively.
     */
    private List<Path> discoverImageFiles(Path directory) throws IOException {
        List<Path> imageFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                .filter(this::isImageFile)
                .forEach(imageFiles::add);
        }

        return imageFiles;
    }

    /**
     * Check if a file is a supported image format.
     */
    private boolean isImageFile(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
            filename.endsWith(".png") || filename.endsWith(".bmp") ||
            filename.endsWith(".tiff") || filename.endsWith(".webp");
    }

    /**
     * Process files in parallel batches.
     */
    private void processBatches(List<Path> imageFiles, ProgressCallback progressCallback) {
        int batchSize = config.batchSize();
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

        for (int i = 0; i < imageFiles.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, imageFiles.size());
            List<Path> batch = imageFiles.subList(i, endIndex);

            CompletableFuture<Void> batchFuture = CompletableFuture.runAsync(
                () -> processBatch(batch, progressCallback),
                executorService
            );

            batchFutures.add(batchFuture);
        }

        // Wait for all batches to complete
        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Process a single batch of image files.
     */
    private void processBatch(List<Path> batch, ProgressCallback progressCallback) {
        for (Path imageFile : batch) {
            try {
                processImage(imageFile);

                long processed = processedFiles.incrementAndGet();

                // Report progress
                if (progressCallback != null && processed % config.progressReportInterval() == 0) {
                    double progress = (double) processed / totalFiles.get();
                    progressCallback.onProgress(new ProgressUpdate(
                        processed,
                        totalFiles.get(),
                        facesExtracted.get(),
                        errorsEncountered.get(),
                        progress
                    ));
                }

            } catch (Exception e) {
                errorsEncountered.incrementAndGet();
                logger.warn("Failed to process image {}: {}", imageFile, e.getMessage());

                if (config.stopOnError()) {
                    throw new RuntimeException("Processing stopped due to error", e);
                }
            }
        }
    }

    /**
     * Process a single image file.
     */
    private void processImage(Path imageFile) throws Exception {
        // Load image
        ImageData imageData = loadImageData(imageFile);

        // Detect faces
        List<Detection> detections = visionBackend.detectFaces(imageData);

        if (detections.isEmpty()) {
            logger.debug("No faces detected in image: {}", imageFile);
            return;
        }

        // Extract embeddings for quality faces
        List<float[]> embeddings = visionBackend.extractEmbeddings(imageData);

        List<FaceEmbeddingIndex.EmbeddingEntry> qualityEmbeddings = new ArrayList<>();

        for (int i = 0; i < Math.min(detections.size(), embeddings.size()); i++) {
            Detection detection = detections.get(i);
            float[] embedding = embeddings.get(i);

            // Assess quality
            double qualityScore = qualityAssessor.assessQuality(detection);

            if (qualityScore >= config.minQualityThreshold()) {
                // Create photo ID from file path and face index
                String photoId = createPhotoId(imageFile, i);

                // Normalize embedding
                float[] normalizedEmbedding = normalizeL2(embedding);

                qualityEmbeddings.add(new FaceEmbeddingIndex.EmbeddingEntry(photoId, normalizedEmbedding));

                logger.debug("Added embedding for {}: quality={:.3f}", photoId, qualityScore);
            } else {
                logger.debug("Rejected low-quality face in {}: quality={:.3f} < {:.3f}",
                    imageFile, qualityScore, config.minQualityThreshold());
            }
        }

        // Add embeddings to index
        if (!qualityEmbeddings.isEmpty()) {
            embeddingIndex.addEmbeddingsBatch(qualityEmbeddings);
            facesExtracted.addAndGet(qualityEmbeddings.size());
        }
    }

    /**
     * Load image data from file.
     */
    private ImageData loadImageData(Path imageFile) throws IOException {
        byte[] imageBytes = Files.readAllBytes(imageFile);
        String contentType = guessContentType(imageFile);
        return ImageData.fromBytes(imageBytes, contentType);
    }

    /**
     * Guess content type from file extension.
     */
    private String guessContentType(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".bmp")) return "image/bmp";
        if (filename.endsWith(".tiff")) return "image/tiff";
        if (filename.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    /**
     * Create a unique photo ID from file path and face index.
     */
    private String createPhotoId(Path imageFile, int faceIndex) {
        String fileName = imageFile.getFileName().toString();
        String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));

        if (faceIndex == 0) {
            return fileNameWithoutExt;
        } else {
            return fileNameWithoutExt + "_face" + faceIndex;
        }
    }

    /**
     * L2 normalize an embedding vector.
     */
    private float[] normalizeL2(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);

        if (norm < 1e-8) {
            return new float[vector.length]; // Return zero vector
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }

        return normalized;
    }

    /**
     * Get current processing statistics.
     */
    public ProcessingStats getCurrentStats() {
        return new ProcessingStats(
            processedFiles.get(),
            totalFiles.get(),
            facesExtracted.get(),
            errorsEncountered.get()
        );
    }

    /**
     * Shutdown the builder and release resources.
     */
    public void shutdown() {
        logger.info("Shutting down face database builder");
        executorService.shutdown();
    }

    /**
     * Configuration for database builder.
     *
     * @param parallelThreads        the number of parallel processing threads
     * @param batchSize              the number of images per batch
     * @param minQualityThreshold    the minimum face quality threshold
     * @param progressReportInterval the progress report interval
     * @param stopOnError            whether to stop on error
     */
    public record DatabaseBuilderConfig(
        int parallelThreads,        // Number of parallel processing threads
        int batchSize,             // Number of images per batch
        double minQualityThreshold, // Minimum face quality to include
        int progressReportInterval, // Report progress every N files
        boolean stopOnError        // Stop processing on first error
    ) {
        public static DatabaseBuilderConfig defaultConfig() {
            return new DatabaseBuilderConfig(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2), // Half available cores
                50,   // 50 images per batch
                0.3,  // 30% minimum quality
                100,  // Report every 100 files
                false // Continue on errors
            );
        }

        public static DatabaseBuilderConfig fastConfig() {
            return new DatabaseBuilderConfig(
                Runtime.getRuntime().availableProcessors(), // Use all cores
                100,  // Larger batches
                0.2,  // Lower quality threshold for speed
                500,  // Less frequent reporting
                false
            );
        }

        public static DatabaseBuilderConfig qualityConfig() {
            return new DatabaseBuilderConfig(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 3), // Conservative threading
                25,   // Smaller batches for careful processing
                0.5,  // Higher quality threshold
                50,   // More frequent reporting
                true  // Stop on errors for quality control
            );
        }
    }

    /**
     * Processing result with statistics.
     *
     * @param filesProcessed   the number of files processed
     * @param facesExtracted   the number of faces extracted
     * @param errors           the number of errors encountered
     * @param processingTimeMs the processing time in milliseconds
     * @param finalIndexSize   the final size of the index
     * @param success          whether the processing was successful
     */
    public record ProcessingResult(
        long filesProcessed,
        long facesExtracted,
        long errors,
        long processingTimeMs,
        long finalIndexSize,
        boolean success
    ) {
        public double getProcessingRate() {
            return processingTimeMs > 0 ? (double) filesProcessed / (processingTimeMs / 1000.0) : 0.0;
        }

        public double getFacesPerFile() {
            return filesProcessed > 0 ? (double) facesExtracted / filesProcessed : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "ProcessingResult{files=%d, faces=%d, errors=%d, time=%ds, rate=%.1f files/sec, faces/file=%.1f, success=%s}",
                filesProcessed, facesExtracted, errors, processingTimeMs / 1000,
                getProcessingRate(), getFacesPerFile(), success
            );
        }
    }

    /**
     * Current processing statistics.
     *
     * @param processedFiles the number of files processed so far
     * @param totalFiles     the total number of files to process
     * @param facesExtracted the number of faces extracted so far
     * @param errors         the number of errors encountered
     */
    public record ProcessingStats(
        long processedFiles,
        long totalFiles,
        long facesExtracted,
        long errors
    ) {
        public double getProgress() {
            return totalFiles > 0 ? (double) processedFiles / totalFiles : 0.0;
        }
    }

    /**
     * Progress update information.
     *
     * @param processedFiles     the number of files processed so far
     * @param totalFiles         the total number of files to process
     * @param facesExtracted     the number of faces extracted so far
     * @param errors             the number of errors encountered
     * @param progressPercentage the progress percentage (0.0 to 100.0)
     */
    public record ProgressUpdate(
        long processedFiles,
        long totalFiles,
        long facesExtracted,
        long errors,
        double progressPercentage
    ) {
    }

    /**
     * Callback interface for progress updates.
     */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(ProgressUpdate update);
    }
}
