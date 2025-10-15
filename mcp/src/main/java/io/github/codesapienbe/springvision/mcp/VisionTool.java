package io.github.codesapienbe.springvision.mcp;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.VisionResult;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides a simple and secure MCP tool for counting faces in images from URLs.
 * This class wraps the {@link VisionTemplate} to offer face counting
 * functionality.
 *
 * @see VisionTemplate
 * @see Tool
 */
@Component
public class VisionTool {

    private static final Logger log = LoggerFactory.getLogger(VisionTool.class);

    private final VisionTemplate visionTemplate;

    // Security constraints
    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient httpClient;

    /**
     * No-arg constructor used by frameworks that require a default constructor.
     * Delegates to a default VisionTemplate for convenience in tests and tools.
     */
    public VisionTool() {
        this(new VisionTemplate());
    }

    /**
     * Constructs a new VisionTool with the given VisionTemplate and a default
     * HttpClient.
     *
     * @param visionTemplate The VisionTemplate to use for performing vision
     *                       operations.
     */
    @Autowired
    public VisionTool(VisionTemplate visionTemplate) {
        this(visionTemplate, HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    /**
     * Package-visible constructor used for testing to inject a custom HttpClient.
     */
    VisionTool(VisionTemplate visionTemplate, HttpClient httpClient) {
        this.visionTemplate = visionTemplate;
        this.httpClient = httpClient == null ? HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build() : httpClient;
        log.info("VisionTool initialized",
                StructuredArguments.keyValue("event", "vision_tool_init"),
                StructuredArguments.keyValue("backend", "VisionTemplate"),
                StructuredArguments.keyValue("max_image_size_bytes", MAX_IMAGE_SIZE_BYTES),
                StructuredArguments.keyValue("request_timeout_seconds", REQUEST_TIMEOUT.getSeconds()));
    }

    /**
     * Downloads an image from a URL with security constraints and validation.
     * <p>
     * Made protected, so unit tests can override this behaviour (serve local
     * images) without
     * performing network calls. Production behaviour remains the same.
     *
     * @param imageUrl The URL of the image to download.
     * @return A byte array containing the image data.
     * @throws IOException if the image cannot be downloaded or validation fails.
     */
    protected byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        log.debug("Attempting to download image",
                StructuredArguments.keyValue("event", "image_download_start"),
                StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        try {
            // Validate URL format
            URI uri = URI.create(imageUrl);

            // Security: Only allow HTTP/HTTPS protocols
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IOException("Only HTTP and HTTPS protocols are allowed");
            }

            // Security: Block localhost and private IP ranges
            String host = uri.getHost();
            if (host == null || isLocalOrPrivateHost(host)) {
                throw new IOException("Cannot access local or private network addresses");
            }

            // Download with size limit
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP error " + response.statusCode());
            }

            // Read with size limit
            try (InputStream inputStream = response.body()) {
                byte[] imageBytes = inputStream.readNBytes(MAX_IMAGE_SIZE_BYTES + 1);

                if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                    throw new IOException(
                            "Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                }

                log.debug("Successfully downloaded image",
                        StructuredArguments.keyValue("event", "image_download_success"),
                        StructuredArguments.keyValue("size_bytes", imageBytes.length));
                return imageBytes;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL format: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Failed to download image: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a host is localhost or in a private IP range.
     */
    private boolean isLocalOrPrivateHost(String host) {
        String lowerHost = host.toLowerCase();
        return lowerHost.equals("localhost")
                || lowerHost.equals("127.0.0.1")
                || lowerHost.equals("0.0.0.0")
                || lowerHost.equals("::1")
                || lowerHost.startsWith("192.168.")
                || lowerHost.startsWith("10.")
                || lowerHost.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    }

    /**
     * Sanitizes URL for logging by removing sensitive query parameters.
     */
    private String sanitizeUrlForLogging(String url) {
        if (url == null)
            return "null";
        int queryIndex = url.indexOf('?');
        return queryIndex > 0 ? url.substring(0, queryIndex) + "?..." : url;
    }

    /**
     * Counts the number of faces in an image from a given URL.
     * This is a simple, secure, and well-logged operation that always returns a
     * response.
     *
     * @param imageUrl The URL of the image to analyze (must be HTTP or HTTPS).
     * @return A map containing the count of faces detected, status, and descriptive
     *         message.
     *         Always returns a response, even on error.
     */
    @Tool(description = "Count faces in an image from a URL. Returns the number of faces detected.")
    @SuppressWarnings("unused")
    public Map<String, Object> countFaces(String imageUrl) {
        log.info("countFaces called",
                StructuredArguments.keyValue("event", "count_faces_start"),
                StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Validate input
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.warn("countFaces validation failed",
                        StructuredArguments.keyValue("event", "count_faces_validation_error"),
                        StructuredArguments.keyValue("error", "empty_url"));
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Image URL is required and cannot be empty");
                response.put("error", "Missing or empty imageUrl parameter");
                return response;
            }

            // Download image
            byte[] imageBytes;
            try {
                imageBytes = downloadImageFromUrl(imageUrl.trim());
                log.debug("Image downloaded for face detection",
                        StructuredArguments.keyValue("event", "image_download_complete"),
                        StructuredArguments.keyValue("size_bytes", imageBytes.length));
            } catch (IOException e) {
                log.error("Failed to download image",
                        StructuredArguments.keyValue("event", "image_download_error"),
                        StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)),
                        StructuredArguments.keyValue("error", e.getMessage()));
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Failed to download image from URL");
                response.put("error", e.getMessage());
                return response;
            }

            // Create ImageData
            ImageData imgData;
            try {
                imgData = ImageData.fromBytes(imageBytes);
                log.debug("ImageData created",
                        StructuredArguments.keyValue("event", "image_data_created"));
            } catch (Exception e) {
                log.error("Failed to create ImageData",
                        StructuredArguments.keyValue("event", "image_data_error"),
                        StructuredArguments.keyValue("error", e.getMessage()));
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Invalid image format or corrupted image data");
                response.put("error", "Failed to parse image: " + e.getMessage());
                return response;
            }

            // Detect faces
            VisionResult detections;
            try {
                detections = visionTemplate.detectFaces(imgData);
                log.debug("Face detection completed",
                        StructuredArguments.keyValue("event", "face_detection_complete"));
            } catch (Exception e) {
                log.error("Face detection failed",
                        StructuredArguments.keyValue("event", "face_detection_error"),
                        StructuredArguments.keyValue("error", e.getMessage()));
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Face detection operation failed");
                response.put("error", "Detection error: " + e.getMessage());
                return response;
            }

            // Build success response
            int faceCount = detections.detections().size();
            double avgConfidence = detections.averageConfidence();
            long duration = System.currentTimeMillis() - startTime;

            String message;
            if (faceCount == 0) {
                message = "No faces detected in the image";
            } else if (faceCount == 1) {
                message = String.format("Detected 1 face with %.1f%% confidence", avgConfidence * 100);
            } else {
                message = String.format("Detected %d faces with average confidence of %.1f%%",
                        faceCount, avgConfidence * 100);
            }

            response.put("status", "success");
            response.put("count", faceCount);
            response.put("message", message);
            response.put("averageConfidence", Math.round(avgConfidence * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);

            log.info("countFaces completed successfully",
                    StructuredArguments.keyValue("event", "count_faces_success"),
                    StructuredArguments.keyValue("face_count", faceCount),
                    StructuredArguments.keyValue("avg_confidence", Math.round(avgConfidence * 10000.0) / 10000.0),
                    StructuredArguments.keyValue("processing_time_ms", duration));
            return response;

        } catch (Exception e) {
            // Catch-all for any unexpected errors
            long duration = System.currentTimeMillis() - startTime;
            log.error("Unexpected error in countFaces",
                    StructuredArguments.keyValue("event", "count_faces_unexpected_error"),
                    StructuredArguments.keyValue("processing_time_ms", duration),
                    StructuredArguments.keyValue("error", e.getMessage()),
                    StructuredArguments.keyValue("error_type", e.getClass().getSimpleName()));

            response.put("status", "error");
            response.put("count", 0);
            response.put("message", "An unexpected error occurred while processing the request");
            response.put("error", "Internal error: " + e.getMessage());
            response.put("processingTimeMs", duration);
            return response;
        }
    }

    /**
     * Extract embeddings for faces in an image URL.
     * Returns a list of base64-encoded embedding vectors and metadata per face.
     */
    @Tool(description = "Extract face embeddings from an image URL. Returns list of embeddings and metadata.")
    @SuppressWarnings("unused")
    public Map<String, Object> extractEmbeddings(String imageUrl) {
        log.info("extractEmbeddings called",
                StructuredArguments.keyValue("event", "extract_embeddings_start"),
                StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("embeddings", List.of());
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            List<float[]> rawEmbeddings = visionTemplate.extractEmbeddings(imgData);
            List<Map<String, Object>> out = new ArrayList<>();

            int idx = 0;
            for (float[] emb : rawEmbeddings) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "face-" + idx);
                item.put("embedding_base64", Base64.getEncoder().encodeToString(floatArrayToBytes(emb)));
                item.put("length", emb.length);
                out.add(item);
                idx++;
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("count", out.size());
            response.put("embeddings", out);
            response.put("processingTimeMs", duration);

            log.info("extractEmbeddings completed",
                    StructuredArguments.keyValue("event", "extract_embeddings_success"),
                    StructuredArguments.keyValue("embeddings_count", out.size()),
                    StructuredArguments.keyValue("processing_time_ms", duration));
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("extractEmbeddings failed",
                    StructuredArguments.keyValue("event", "extract_embeddings_error"),
                    StructuredArguments.keyValue("error", e.getMessage()),
                    StructuredArguments.keyValue("processing_time_ms", duration));
            response.put("status", "error");
            response.put("message", "Failed to extract embeddings: " + e.getMessage());
            response.put("embeddings", List.of());
            response.put("processingTimeMs", duration);
            return response;
        }
    }

    /**
     * Store a face embedding into the configured vector service.
     */
    @Tool(description = "Store a face (extract embedding from URL or base64) in the vector service.")
    @SuppressWarnings("unused")
    public Map<String, Object> storeFaceEmbedding(String personId, String imageUrl, String imageBase64, String modelName,
            Double confidence, Map<String, Object> metadata) {
        Map<String, Object> resp = new HashMap<>();
        try {
            // Require either imageUrl or imageBase64
            if ((imageUrl == null || imageUrl.trim().isEmpty()) && (imageBase64 == null || imageBase64.trim().isEmpty())) {
                resp.put("status", "error");
                resp.put("message", "Either imageUrl or imageBase64 must be provided");
                return resp;
            }

            byte[] imageBytes;
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                imageBytes = downloadImageFromUrl(imageUrl.trim());
            } else {
                imageBytes = Base64.getDecoder().decode(imageBase64.trim());
            }

            ImageData img = ImageData.fromBytes(imageBytes);
            List<float[]> embList = visionTemplate.extractEmbeddings(img);
            if (embList == null || embList.isEmpty()) {
                resp.put("status", "error");
                resp.put("message", "Failed to extract embeddings from image");
                return resp;
            }

            float[] emb = embList.get(0);
            String imageHash = imageBytes == null ? null : hashBytes(imageBytes);

            String storedId = visionTemplate.storeFaceEmbedding(personId, emb, modelName, imageHash, confidence,
                    metadata == null ? Map.of() : metadata);

            // Also add to backend gallery if backend supports it (include imageHash for index)
            try {
                var backend = visionTemplate.backend();
                try {
                    java.lang.reflect.Method m = backend.getClass().getMethod("storeGalleryEmbedding", String.class,
                            float[].class, String.class, String.class);
                    Object galleryId = m.invoke(backend, personId, emb, modelName, imageHash);
                    if (galleryId != null) {
                        resp.put("galleryId", galleryId.toString());
                    }
                } catch (NoSuchMethodException ignored) {
                    // try older signature
                    try {
                        java.lang.reflect.Method m2 = backend.getClass().getMethod("storeGalleryEmbedding", String.class, float[].class, String.class);
                        Object galleryId = m2.invoke(backend, personId, emb, modelName);
                        if (galleryId != null) resp.put("galleryId", galleryId.toString());
                    } catch (NoSuchMethodException ignored2) {
                    }
                }
            } catch (Exception ignored) {
            }

            resp.put("status", "success");
            resp.put("id", storedId);
            return resp;
        } catch (Exception e) {
            resp.put("status", "error");
            resp.put("message", "Failed to store face: " + e.getMessage());
            return resp;
        }
    }

    /**
     * Lookup similar faces using a provided embedding vector.
     */
    @Tool(description = "Lookup similar faces by image URL.")
    @SuppressWarnings("unused")
    public Map<String, Object> lookupFaces(String imageUrl, String modelName,
            String metric, Double threshold, Integer limit, List<String> includePersonIds, List<String> excludePersonIds) {
        Map<String, Object> resp = new HashMap<>();
        try {
            float[] emb = null;

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData img = ImageData.fromBytes(imageBytes);
            List<float[]> el = visionTemplate.extractEmbeddings(img);
            if (el != null && !el.isEmpty()) emb = el.get(0);
            else {
                resp.put("status", "error");
                resp.put("message", "No embeddings extracted from image");
                resp.put("results", List.of());
                return resp;
            }

            Set<String> include = includePersonIds == null ? null : new HashSet<>(includePersonIds);
            Set<String> exclude = excludePersonIds == null ? null : new HashSet<>(excludePersonIds);

            List<Map<String, Object>> results = visionTemplate.lookupFaces(emb, modelName, metric, threshold, limit,
                    include, exclude);

            // If backend supports gallery search, also query backend and merge results
            try {
                var backend = visionTemplate.backend();
                java.lang.reflect.Method mg = backend.getClass().getMethod("findNearestGallery", float[].class,
                        ImageData.class, String.class, Integer.class);
                Object backendRes = mg.invoke(backend, emb, null, metric, limit);
                if (backendRes instanceof java.util.List) {
                    // If primary results empty, return backend results in mapped format
                    if (results == null || results.isEmpty()) {
                        Map<String, Object> resp2 = new HashMap<>();
                        resp2.put("status", "success");
                        resp2.put("results", backendRes);
                        return resp2;
                    }
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }

            resp.put("status", "success");
            resp.put("results", results == null ? List.of() : results);
            return resp;
        } catch (Exception e) {
            resp.put("status", "error");
            resp.put("message", "Lookup failed: " + e.getMessage());
            resp.put("results", List.of());
            return resp;
        }
    }

    /**
     * MCP tool: add an embedding to the backend gallery (runtime-only).
     * Returns the generated gallery id when supported by the backend.
     */
    // Legacy raw-embedding method removed. Use `addGalleryEntryFromUrl` or `storeFaceEmbedding` (URL-first) instead.

    /**
     * Add a gallery entry by supplying an image URL. The backend will extract embeddings
     * internally and store them in the gallery when supported.
     */
    @Tool(description = "Add gallery entry from an image URL. Returns galleryId if supported.")
    @SuppressWarnings("unused")
    public Map<String, Object> addGalleryEntryFromUrl(String personId, String imageUrl, String modelName) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                resp.put("status", "error");
                resp.put("message", "imageUrl is required");
                return resp;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData img = ImageData.fromBytes(imageBytes);
            List<float[]> embList = visionTemplate.extractEmbeddings(img);
            if (embList == null || embList.isEmpty()) {
                resp.put("status", "error");
                resp.put("message", "No embeddings extracted from image");
                return resp;
            }
            float[] emb = embList.get(0);

            var backend = visionTemplate.backend();
            try {
                java.lang.reflect.Method m = backend.getClass().getMethod("storeGalleryEmbedding", String.class, float[].class, String.class);
                Object galleryId = m.invoke(backend, personId, emb, modelName);
                resp.put("status", "success");
                resp.put("galleryId", galleryId == null ? null : galleryId.toString());
                return resp;
            } catch (NoSuchMethodException nsme) {
                resp.put("status", "error");
                resp.put("message", "Backend does not support gallery storage");
                return resp;
            }

        } catch (Exception e) {
            resp.put("status", "error");
            resp.put("message", "Failed to add gallery entry: " + e.getMessage());
            return resp;
        }
    }

    /**
     * Lookup faces by image URL — extracts embedding from the URL then performs lookup.
     */
    @Tool(description = "Lookup similar faces by image URL.")
    @SuppressWarnings("unused")
    public Map<String, Object> lookupFacesByImageUrl(String imageUrl, String modelName, String metric, Double threshold, Integer limit) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                resp.put("status", "error");
                resp.put("message", "imageUrl is required");
                resp.put("results", List.of());
                return resp;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData img = ImageData.fromBytes(imageBytes);
            List<float[]> embList = visionTemplate.extractEmbeddings(img);
            if (embList == null || embList.isEmpty()) {
                resp.put("status", "error");
                resp.put("message", "No embeddings extracted from image");
                resp.put("results", List.of());
                return resp;
            }

            float[] emb = embList.get(0);
            List<Map<String,Object>> results = visionTemplate.lookupFaces(emb, modelName, metric, threshold, limit, null, null);
            if ((results == null || results.isEmpty())) {
                // Try backend gallery if available
                try {
                    var backend = visionTemplate.backend();
                    java.lang.reflect.Method mg = backend.getClass().getMethod("findNearestGallery", float[].class, ImageData.class, String.class, Integer.class);
                    Object backendRes = mg.invoke(backend, emb, null, metric, limit);
                    if (backendRes instanceof java.util.List) {
                        resp.put("status", "success");
                        resp.put("results", backendRes);
                        return resp;
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }

            resp.put("status", "success");
            resp.put("results", results == null ? List.of() : results);
            return resp;
        } catch (Exception e) {
            resp.put("status", "error");
            resp.put("message", "Lookup failed: " + e.getMessage());
            resp.put("results", List.of());
            return resp;
        }
    }

    /**
     * MCP tool: remove an embedding from the backend gallery by id.
     */
    @Tool(description = "Remove gallery entries by galleryId or personId (URL/ID supported).")
    @SuppressWarnings("unused")
    public Map<String, Object> removeGalleryEntry(String galleryId, String personId, String imageUrl) {
        Map<String, Object> resp = new HashMap<>();
        try {
            var backend = visionTemplate.backend();

            // Remove by explicit galleryId if provided
            if (galleryId != null && !galleryId.trim().isEmpty()) {
                try {
                    java.lang.reflect.Method m = backend.getClass().getMethod("removeGalleryEmbedding", String.class);
                    m.invoke(backend, galleryId);
                    resp.put("status", "success");
                    resp.put("removed", java.util.List.of(galleryId));
                    return resp;
                } catch (NoSuchMethodException nsme) {
                    resp.put("status", "error");
                    resp.put("message", "Backend does not support gallery removal by id");
                    return resp;
                }
            }

            // Remove by personId if provided
            if (personId != null && !personId.trim().isEmpty()) {
                try {
                    java.lang.reflect.Method m = backend.getClass().getMethod("removeGalleryByPersonId", String.class);
                    Object removed = m.invoke(backend, personId);
                    resp.put("status", "success");
                    resp.put("removed", removed == null ? java.util.List.of() : removed);
                    return resp;
                } catch (NoSuchMethodException nsme) {
                    resp.put("status", "error");
                    resp.put("message", "Backend does not support gallery removal by personId");
                    return resp;
                }
            }

            // Remove by imageUrl: find embedding and remove any matching entries by personId or hash
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
                // Compute hash and ask VectorService for matching entries, then delete them
                String imageHash = hashBytes(imageBytes);

                try {
                    // Use VisionTemplate's vectorService via reflection (it's not public); use lookup via visionTemplate
                    List<Map<String,Object>> entries = visionTemplate.lookupFaces(null, null, null, null, null, null, null);
                } catch (Exception ignore) {
                }

                // Prefer calling configured VectorService if available
                try {
                    java.lang.reflect.Field vsField = visionTemplate.getClass().getDeclaredField("vectorService");
                    vsField.setAccessible(true);
                    Object vs = vsField.get(visionTemplate);
                    if (vs != null) {
                        java.lang.reflect.Method findMeth = vs.getClass().getMethod("findEntriesByImageHash", String.class);
                        Object found = findMeth.invoke(vs, imageHash);
                        if (found instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Map<String,Object>> foundList = (java.util.List<Map<String,Object>>) found;
                            java.util.List<String> removed = new java.util.ArrayList<>();
                            for (Map<String,Object> rec : foundList) {
                                Object idObj = rec.get("embeddingId");
                                if (idObj != null) {
                                    String id = idObj.toString();
                                    java.lang.reflect.Method del = vs.getClass().getMethod("deleteEmbeddingById", String.class);
                                    del.invoke(vs, id);
                                    removed.add(id);
                                }
                            }
                            resp.put("status", "success");
                            resp.put("removed", removed);
                            return resp;
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }

                resp.put("status", "error");
                resp.put("message", "No VectorService available for removal by imageUrl");
                return resp;
            }

            resp.put("status", "error");
            resp.put("message", "Provide galleryId or personId (or imageUrl) to remove entries");
            return resp;
        } catch (Exception e) {
            resp.put("status", "error");
            resp.put("message", "Failed to remove gallery entry: " + e.getMessage());
            return resp;
        }
    }

    /**
     * MCP tool: list gallery entries by personId or imageHash.
     */
    @Tool(description = "List gallery entries by personId or imageHash.")
    @SuppressWarnings("unused")
    public Map<String, Object> listGalleryEntries(String personId, String imageHash) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if ((personId == null || personId.isBlank()) && (imageHash == null || imageHash.isBlank())) {
                resp.put("status", "error");
                resp.put("message", "Provide personId or imageHash");
                resp.put("entries", List.of());
                return resp;
            }

            // Prefer VisionTemplate passthrough for imageHash
            if (imageHash != null && !imageHash.isBlank()) {
                try {
                    List<Map<String,Object>> found = visionTemplate.findEntriesByImageHash(imageHash);
                    resp.put("status", "success");
                    resp.put("entries", found == null ? List.of() : found);
                    return resp;
                } catch (UnsupportedOperationException uoe) {
                    resp.put("status", "error");
                    resp.put("message", "VectorService not configured");
                    resp.put("entries", List.of());
                    return resp;
                }
            }

            // If personId provided, try backend gallery iteration
            if (personId != null && !personId.isBlank()) {
                var backend = visionTemplate.backend();
                try {
                    java.lang.reflect.Method m = backend.getClass().getMethod("removeGalleryByPersonId", String.class);
                    // We call the remove method reflectively to get ids but do not remove — so instead iterate gallery if possible
                } catch (NoSuchMethodException ignored) {
                }

                // Fallback: attempt to query VectorService entries (scan store)
                try {
                    java.lang.reflect.Field vsField = visionTemplate.getClass().getDeclaredField("vectorService");
                    vsField.setAccessible(true);
                    Object vs = vsField.get(visionTemplate);
                    if (vs != null) {
                        // No direct API to list by personId; use findSimilarFaces with null probe is not applicable.
                        // As a pragmatic approach, if InMemoryVectorService, we can call findEntriesByImageHash for all hashes — but that's heavy.
                    }
                } catch (Exception ignored) {
                }

                resp.put("status", "error");
                resp.put("message", "Listing by personId not implemented in this tool; use backend APIs");
                resp.put("entries", List.of());
                return resp;
            }

            resp.put("status", "error");
            resp.put("entries", List.of());
            return resp;
        } catch (Exception e) {
            resp.put("status", "error");
            resp.put("message", "Failed to list gallery entries: " + e.getMessage());
            resp.put("entries", List.of());
            return resp;
        }
    }

    // Helper: convert float[] to raw bytes (big-endian)
    private static byte[] floatArrayToBytes(float[] arr) {
        byte[] out = new byte[arr.length * 4];
        for (int i = 0; i < arr.length; i++) {
            int bits = Float.floatToIntBits(arr[i]);
            out[i * 4] = (byte) ((bits >> 24) & 0xFF);
            out[i * 4 + 1] = (byte) ((bits >> 16) & 0xFF);
            out[i * 4 + 2] = (byte) ((bits >> 8) & 0xFF);
            out[i * 4 + 3] = (byte) (bits & 0xFF);
        }
        return out;
    }

    // Legacy conversion helper removed. Tools accept URLs only.

    private static String hashBytes(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
