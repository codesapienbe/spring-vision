package com.springvision.examples.basicfacedetection.controller;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.VisionTemplate;
import com.springvision.core.Detection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ResponseBody;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Controller for handling face detection requests.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Displaying the main face detection page</li>
 *   <li>Processing uploaded images for face detection</li>
 *   <li>Processing public image URLs for face detection (in-memory download)</li>
 *   <li>Returning detection results</li>
 * </ul>
 *
 * <p>All network downloads are SSRF-hardened with public-host checks, timeouts,
 * and size limits, and are kept fully in memory without persisting to disk.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Controller
public class FaceDetectionController {

    private static final Logger logger = LoggerFactory.getLogger(FaceDetectionController.class);

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_DOWNLOAD_BYTES = 50 * 1024 * 1024; // 50MB cap

    private final VisionTemplate visionTemplate;
    private final WebClient webClient;

    /**
     * Constructor with VisionTemplate and WebClient dependency injection.
     *
     * @param visionTemplate the vision template for processing images
     * @param webClientBuilder the web client builder for reactive HTTP calls
     */
    private final com.springvision.core.async.AsyncVisionProcessor asyncProcessor;

    public FaceDetectionController(VisionTemplate visionTemplate, WebClient.Builder webClientBuilder) {
        this.visionTemplate = visionTemplate;
        this.webClient = webClientBuilder.build();
        this.asyncProcessor = new com.springvision.core.async.AsyncVisionProcessor(visionTemplate);
    }

    /**
     * Displays the main face detection page.
     *
     * @return the index template name
     */
    @GetMapping("/")
    public String index() {
        logger.debug("Displaying main face detection page");
        return "index";
    }

    /**
     * Annotated image endpoint (upload) - returns PNG with rectangles drawn around faces.
     */
    @PostMapping(value = "/api/vision/detect/annotated", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> annotatedFromUpload(@RequestParam("file") MultipartFile file) throws IOException {
        logger.info("Processing annotated image request for file: {}", file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            logger.warn("No file provided for annotated request");
            return ResponseEntity.badRequest().build();
        }

        byte[] imageBytes = file.getBytes();
        ImageData imageData = ImageData.fromBytes(imageBytes);

        com.springvision.core.DetectionQuery query = new com.springvision.core.DetectionQuery.Builder()
            .type(com.springvision.core.DetectionType.FACE)
            .categories(java.util.Set.of(com.springvision.core.DetectionCategory.FACE))
            .build();
        VisionResult result = visionTemplate.detect(imageData, query);

        // Filter detections by confidence threshold
        List<Detection> filteredDetections = result.detections().stream()
            .filter(d -> d.confidence() >= 0.3)
            .collect(Collectors.toList());

        byte[] png = annotateImageBytes(imageBytes, filteredDetections);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    /**
     * Annotated image endpoint (URL) - downloads URL in-memory and returns PNG with rectangles.
     */
    @PostMapping(value = "/api/vision/detect/annotated/url", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<byte[]>> annotatedFromUrl(@RequestParam("imageUrl") String imageUrl) {
        logger.info("Processing annotated image request for URL: {}", imageUrl);
        if (imageUrl == null || imageUrl.isBlank()) {
            return Mono.just(ResponseEntity.<byte[]>badRequest().body(new byte[0]));
        }
        return webClient.get()
            .uri(imageUrl)
            .retrieve()
            .bodyToMono(byte[].class)
            .flatMap(imageBytes -> Mono.fromCallable(() -> {
                try {
                    if (!isHttpUrl(imageUrl)) {
                        throw new IllegalArgumentException("Only HTTP/HTTPS URLs are supported");
                    }
                    URL url = URI.create(imageUrl).toURL();
                    validatePublicHost(url);
                    // Note: WebClient doesn't check content-type easily, assuming it's image

                    ImageData imageData = ImageData.fromBytes(imageBytes);
                    com.springvision.core.DetectionQuery query = new com.springvision.core.DetectionQuery.Builder()
                        .type(com.springvision.core.DetectionType.FACE)
                        .categories(java.util.Set.of(com.springvision.core.DetectionCategory.FACE))
                        .build();
                    VisionResult result = visionTemplate.detect(imageData, query);
                    List<Detection> filteredDetections = result.detections().stream()
                        .filter(d -> d.confidence() >= 0.3)
                        .collect(Collectors.toList());
                    byte[] png = annotateImageBytes(imageBytes, filteredDetections);
                    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
                } catch (Exception e) {
                    logger.error("Error processing URL image", e);
                    return ResponseEntity.<byte[]>badRequest().body(new byte[0]);
                }
            }))
            .onErrorResume(ex -> {
                logger.warn("Failed to process URL: {}", ex.getMessage());
                return Mono.just(ResponseEntity.<byte[]>badRequest().body(new byte[0]));
            });
    }

    /**
     * REST API endpoint for face detection from public image URL - returns JSON with detections.
     */
    @PostMapping(value = "/api/vision/detect/faces/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<java.util.Map<String,Object>>> detectFacesFromUrlApi(@RequestParam("imageUrl") String imageUrl) {
        logger.info("Processing REST face detection request for URL: {}", imageUrl);
        if (imageUrl == null || imageUrl.isBlank()) {
            return Mono.just(ResponseEntity.<java.util.Map<String,Object>>badRequest().body(new java.util.HashMap<>()));
        }
        return webClient.get()
            .uri(imageUrl)
            .retrieve()
            .bodyToMono(byte[].class)
            .flatMap(imageBytes -> Mono.fromCallable(() -> {
                try {
                    if (!isHttpUrl(imageUrl)) {
                        throw new IllegalArgumentException("Only HTTP/HTTPS URLs are supported");
                    }
                    URL url = URI.create(imageUrl).toURL();
                    validatePublicHost(url);
                    // Note: WebClient doesn't check content-type easily, assuming it's image

                    ImageData imageData = ImageData.fromBytes(imageBytes);
                    VisionResult result = visionTemplate.detectFaces(imageData);
                    java.util.Map<String,Object> payload = new java.util.HashMap<>();
                    payload.put("detectionType", "face");
                    payload.put("detections", result.detections());
                    payload.put("count", result.detectionCount());
                    payload.put("avgConfidence", result.averageConfidence());
                    return ResponseEntity.<java.util.Map<String,Object>>ok(payload);
                } catch (Exception e) {
                    logger.error("Error processing URL image", e);
                    java.util.Map<String,Object> err = new java.util.HashMap<>();
                    err.put("error", e.getMessage());
                    return ResponseEntity.<java.util.Map<String,Object>>badRequest().body(err);
                }
            }))
            .onErrorResume(ex -> {
                logger.warn("Failed to process URL: {}", ex.getMessage());
                java.util.Map<String,Object> err = new java.util.HashMap<>();
                err.put("error", ex.getMessage());
                return Mono.just(ResponseEntity.<java.util.Map<String,Object>>badRequest().body(err));
            });
    }

    /**
     * Processes uploaded images for face detection.
     *
     * @param file the uploaded image file
     * @param model the model to add results to
     * @return the index template name with results
     */
    @PostMapping("/detect")
    public String detectFaces(@RequestParam("file") MultipartFile file, Model model) {
        logger.info("Processing face detection request for file: {}", file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a file to upload");
                return "index";
            }

            // Convert to ImageData
            byte[] imageBytes = file.getBytes();
            ImageData imageData = ImageData.fromBytes(imageBytes);

            // Perform face detection
            com.springvision.core.DetectionQuery query = new com.springvision.core.DetectionQuery.Builder()
                .type(com.springvision.core.DetectionType.FACE)
                .categories(java.util.Set.of(com.springvision.core.DetectionCategory.FACE))
                .build();
            VisionResult result = visionTemplate.detect(imageData, query);

            // Generate annotated image
            byte[] annotatedBytes = annotateImageBytes(imageBytes, result.detections());
            String annotatedBase64 = Base64.getEncoder().encodeToString(annotatedBytes);
            model.addAttribute("annotatedImage", "data:image/png;base64," + annotatedBase64);

            if (result.hasDetections()) {
                List<Detection> detections = result.detections();

                // Sort detections by confidence descending
                List<Detection> sortedDetections = detections.stream()
                    .sorted((d1, d2) -> Double.compare(d2.confidence(), d1.confidence()))
                    .toList();

                // Read image for cropping
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

                // Crop faces and encode as base64
                List<String> croppedImages = new ArrayList<>();
                List<String> embeddingStrings = new ArrayList<>();
                for (Detection d : sortedDetections) {
                    com.springvision.core.BoundingBox b = d.boundingBox();
                    int imgW = img.getWidth();
                    int imgH = img.getHeight();
                    int x = Math.max(0, (int) Math.round(b.x() * imgW));
                    int y = Math.max(0, (int) Math.round(b.y() * imgH));
                    int w = Math.max(1, (int) Math.round(b.width() * imgW));
                    int h = Math.max(1, (int) Math.round(b.height() * imgH));

                    // Ensure within bounds
                    w = Math.min(w, imgW - x);
                    h = Math.min(h, imgH - y);

                    BufferedImage faceImg = img.getSubimage(x, y, w, h);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(faceImg, "png", baos);
                    byte[] faceBytes = baos.toByteArray();
                    String faceBase64 = Base64.getEncoder().encodeToString(faceBytes);
                    croppedImages.add(faceBase64);

                    // Extract embedding for this face
                    try {
                        ImageData faceImageData = ImageData.fromBytes(faceBytes);
                        List<float[]> faceEmbeddings = visionTemplate.getBackend().extractEmbeddings(faceImageData);
                        if (!faceEmbeddings.isEmpty()) {
                            embeddingStrings.add(Arrays.toString(faceEmbeddings.get(0)));
                        } else {
                            embeddingStrings.add("No embedding extracted");
                        }
                    } catch (Exception e) {
                        embeddingStrings.add("Error extracting embedding: " + e.getMessage());
                    }
                }

                model.addAttribute("detections", sortedDetections);
                model.addAttribute("croppedImages", croppedImages);
                model.addAttribute("embeddingStrings", embeddingStrings);
                logger.info("Detected {} faces in image", sortedDetections.size());

            } else {
                model.addAttribute("error", "No faces detected in the uploaded image");
                logger.info("No faces detected in uploaded image");
            }

        } catch (IOException e) {
            logger.error("Error reading uploaded file", e);
            model.addAttribute("error", "Error reading uploaded file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error during face detection", e);
            model.addAttribute("error", "Error during face detection: " + e.getMessage());
        }

        return "index";
    }

    /**
     * Processes a public image URL for face detection in-memory.
     *
     * @param imageUrl the public HTTP/HTTPS image URL
     * @param model the model to add results to
     * @return the index template name with results
     */
    @PostMapping("/detect/url")
    public String detectFacesFromUrl(@RequestParam("imageUrl") String imageUrl, Model model) {
        logger.info("Processing face detection request for URL: {}", imageUrl);
        try {
            if (imageUrl == null || imageUrl.isBlank()) {
                model.addAttribute("error", "Please provide an image URL");
                return "index";
            }
            byte[] imageBytes = downloadImageBytes(imageUrl.trim());
            ImageData imageData = ImageData.fromBytes(imageBytes);

            com.springvision.core.DetectionQuery query = new com.springvision.core.DetectionQuery.Builder()
                .type(com.springvision.core.DetectionType.FACE)
                .categories(java.util.Set.of(com.springvision.core.DetectionCategory.FACE))
                .build();
            VisionResult result = visionTemplate.detect(imageData, query);

            // Generate annotated image
            byte[] annotatedBytes = annotateImageBytes(imageBytes, result.detections());
            String annotatedBase64 = Base64.getEncoder().encodeToString(annotatedBytes);
            model.addAttribute("annotatedImage", "data:image/png;base64," + annotatedBase64);

            if (result.hasDetections()) {
                List<Detection> detections = result.detections();

                // Sort detections by confidence descending
                List<Detection> sortedDetections = detections.stream()
                    .sorted((d1, d2) -> Double.compare(d2.confidence(), d1.confidence()))
                    .collect(Collectors.toList());

                // Read image for cropping
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

                // Crop faces and encode as base64
                List<String> croppedImages = new ArrayList<>();
                List<String> embeddingStrings = new ArrayList<>();
                for (Detection d : sortedDetections) {
                    com.springvision.core.BoundingBox b = d.boundingBox();
                    int imgW = img.getWidth();
                    int imgH = img.getHeight();
                    int x = Math.max(0, (int) Math.round(b.x() * imgW));
                    int y = Math.max(0, (int) Math.round(b.y() * imgH));
                    int w = Math.max(1, (int) Math.round(b.width() * imgW));
                    int h = Math.max(1, (int) Math.round(b.height() * imgH));

                    // Ensure within bounds
                    w = Math.min(w, imgW - x);
                    h = Math.min(h, imgH - y);

                    BufferedImage faceImg = img.getSubimage(x, y, w, h);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(faceImg, "png", baos);
                    byte[] faceBytes = baos.toByteArray();
                    String faceBase64 = Base64.getEncoder().encodeToString(faceBytes);
                    croppedImages.add(faceBase64);

                    // Extract embedding for this face
                    try {
                        ImageData faceImageData = ImageData.fromBytes(faceBytes);
                        List<float[]> faceEmbeddings = visionTemplate.getBackend().extractEmbeddings(faceImageData);
                        if (!faceEmbeddings.isEmpty()) {
                            embeddingStrings.add(Arrays.toString(faceEmbeddings.get(0)));
                        } else {
                            embeddingStrings.add("No embedding extracted");
                        }
                    } catch (Exception e) {
                        embeddingStrings.add("Error extracting embedding: " + e.getMessage());
                    }
                }

                model.addAttribute("detections", sortedDetections);
                model.addAttribute("croppedImages", croppedImages);
                model.addAttribute("embeddingStrings", embeddingStrings);
                logger.info("Detected {} faces from URL image", sortedDetections.size());

            } else {
                model.addAttribute("error", "No faces detected in the image at the provided URL");
                logger.info("No faces detected for URL");
            }
        } catch (IllegalArgumentException iae) {
            logger.warn("URL validation failed: {}", iae.getMessage());
            model.addAttribute("error", iae.getMessage());
        } catch (IOException ioe) {
            logger.error("Failed to download image: {}", ioe.getMessage(), ioe);
            model.addAttribute("error", "Failed to download image: " + ioe.getMessage());
        } catch (Exception e) {
            logger.error("Error during face detection from URL", e);
            model.addAttribute("error", "Error during face detection: " + e.getMessage());
        }
        return "index";
    }

    private byte[] downloadImageBytes(String urlString) throws IOException {
        if (!isHttpUrl(urlString)) {
            throw new IllegalArgumentException("Only HTTP/HTTPS URLs are supported");
        }
        URL url = URI.create(urlString).toURL();
        validatePublicHost(url);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "spring-vision-basic-example/1.0");
        String ct = conn.getContentType();
        if (ct != null && !ct.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("URL does not point to an image content type");
        }
        int contentLength = conn.getContentLength();
        if (contentLength > 0 && contentLength > MAX_DOWNLOAD_BYTES) {
            throw new IllegalArgumentException("Remote content too large: " + contentLength + " bytes");
        }
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int read; int total = 0;
            while ((read = in.read(buf)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IllegalArgumentException("Download exceeds size limit");
                }
                baos.write(buf, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private boolean isHttpUrl(String s) {
        String v = s == null ? "" : s.trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    private void validatePublicHost(URL url) throws IOException {
        String host = url.getHost();
        if (host == null || host.isBlank()) throw new IOException("Invalid URL host");
        InetAddress[] addrs;
        try {
            addrs = InetAddress.getAllByName(host);
        } catch (Exception e) {
            throw new IOException("Failed to resolve host: " + host);
        }
        for (InetAddress a : addrs) {
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) {
                throw new IOException("Refusing to connect to non-public address: " + a.getHostAddress());
            }
        }
    }

    /**
     * Draws rectangles and confidence labels onto the provided image bytes and returns PNG bytes.
     * Assumes bounding boxes are in pixel coordinates; if normalized, scale externally.
     */
    private byte[] annotateImageBytes(byte[] originalImageBytes, List<Detection> detections) throws IOException {
        if (originalImageBytes == null || originalImageBytes.length == 0) return new byte[0];

        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalImageBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                throw new IOException("Failed to decode image for annotation");
            }

            Graphics2D g = img.createGraphics();
            try {
                g.setStroke(new BasicStroke(3f));
                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.setColor(Color.RED);

                for (Detection d : detections) {
                    com.springvision.core.BoundingBox b = d.boundingBox();
                    // bounding boxes in core are normalized [0..1] - convert to pixel coords
                    int imgW = img.getWidth();
                    int imgH = img.getHeight();
                    int x = Math.max(0, (int) Math.round(b.x() * imgW));
                    int y = Math.max(0, (int) Math.round(b.y() * imgH));
                    int w = Math.max(1, (int) Math.round(b.width() * imgW));
                    int h = Math.max(1, (int) Math.round(b.height() * imgH));

                    g.drawRect(x, y, w, h);

                    String label = String.format("%.2f%%", d.confidence() * 100.0);
                    int labelX = x;
                    int labelY = Math.max(18, y - 6);
                    // Draw background for label for readability
                    java.awt.FontMetrics fm = g.getFontMetrics();
                    int lw = fm.stringWidth(label) + 6;
                    int lh = fm.getHeight();
                    g.setColor(new Color(0, 0, 0, 160));
                    g.fillRect(labelX, labelY - lh + 4, lw, lh);
                    g.setColor(Color.WHITE);
                    g.drawString(label, labelX + 3, labelY);
                    g.setColor(Color.RED);
                }
            } finally {
                g.dispose();
            }

            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        }
    }

    // --- Core features showcase endpoints ---

    @GetMapping("/api/vision/health")
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> health() {
        var info = visionTemplate.getBackendHealthInfo();
        java.util.Set<com.springvision.core.DetectionType> types = visionTemplate.getSupportedDetectionTypes();
        return ResponseEntity.ok(java.util.Map.of(
            "backendId", visionTemplate.getBackendId(),
            "backendName", visionTemplate.getBackendDisplayName(),
            "backendVersion", visionTemplate.getBackendVersion(),
            "status", info.status().toString(),
            "statusMessage", info.statusMessage(),
            "responseTimeMs", info.responseTimeMs(),
            "supportedDetectionTypes", types.stream().map(com.springvision.core.DetectionType::getCode).toList()
        ));
    }

    @GetMapping("/api/vision/supported-types")
    @ResponseBody
    public ResponseEntity<java.util.List<String>> supportedTypes() {
        java.util.List<String> types = visionTemplate.getSupportedDetectionTypes().stream()
            .map(com.springvision.core.DetectionType::getCode)
            .sorted()
            .toList();
        return ResponseEntity.ok(types);
    }

    @PostMapping(value = "/api/vision/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> verify(
            @RequestParam("fileA") MultipartFile fileA,
            @RequestParam("fileB") MultipartFile fileB,
            @RequestParam(value = "metric", required = false, defaultValue = "cosine") String metric,
            @RequestParam(value = "threshold", required = false, defaultValue = "0.6") double threshold) {
        try {
            ImageData a = ImageData.fromBytes(fileA.getBytes());
            ImageData b = ImageData.fromBytes(fileB.getBytes());
            boolean match = visionTemplate.verify(a, b, metric, threshold);
            return ResponseEntity.ok(java.util.Map.of(
                "metric", metric,
                "threshold", threshold,
                "match", match
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/api/vision/detect/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> detectMultipleApi(
            @RequestParam("file") MultipartFile file,
            @RequestParam("detectionTypes") String detectionTypesCsv) {
        try {
            java.util.List<com.springvision.core.DetectionType> types = java.util.Arrays.stream(detectionTypesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(com.springvision.core.DetectionType::fromCode)
                .toList();
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            java.util.List<VisionResult> results = visionTemplate.detectMultiple(imageData, types);
            java.util.Map<String,Object> payload = new java.util.HashMap<>();
            payload.put("types", types.stream().map(com.springvision.core.DetectionType::getCode).toList());
            payload.put("results", results.stream().map(r -> java.util.Map.of(
                "type", r.detectionType().getCode(),
                "count", r.detectionCount(),
                "avgConfidence", r.averageConfidence(),
                "processingTimeMs", r.processingTimeMs(),
                "detections", r.detections()
            )).toList());
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/api/vision/obscure", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> obscureFacesApi(@RequestParam("file") MultipartFile file) {
        try {
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            ImageData obscured = visionTemplate.obscureFaces(imageData);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(obscured.data());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/api/vision/tag", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> tagApi(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "label", defaultValue = "TAG") String label,
            @RequestParam(value = "categories", defaultValue = "FACE") String categoriesCsv) {
        try {
            java.util.Set<com.springvision.core.DetectionCategory> cats = java.util.Arrays.stream(categoriesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(com.springvision.core.DetectionCategory::valueOf)
                .collect(java.util.stream.Collectors.toSet());
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            ImageData tagged = visionTemplate.tag(imageData, label, cats);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(tagged.data());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/api/vision/mark", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> markApi(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categories", defaultValue = "FACE") String categoriesCsv) {
        try {
            java.util.Set<com.springvision.core.DetectionCategory> cats = java.util.Arrays.stream(categoriesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(com.springvision.core.DetectionCategory::valueOf)
                .collect(java.util.stream.Collectors.toSet());
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            ImageData marked = visionTemplate.mark(imageData, cats);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(marked.data());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/api/vision/tasks/detect/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> submitAsyncFaceTask(@RequestParam("file") MultipartFile file) {
        try {
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            var handle = asyncProcessor.processAsyncWithHandle(
                imageData,
                com.springvision.core.DetectionType.FACE,
                java.util.Map.of(),
                null
            );
            return ResponseEntity.accepted().body(java.util.Map.of("taskId", handle.taskId(), "status", "accepted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/api/vision/tasks/detect/{detectionType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> submitAsyncTask(@RequestParam("file") MultipartFile file,
            @org.springframework.web.bind.annotation.PathVariable("detectionType") String detectionType) {
        try {
            com.springvision.core.DetectionType type = com.springvision.core.DetectionType.fromCode(detectionType);
            if (type == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid detection type: " + detectionType));
            }
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            var handle = asyncProcessor.processAsyncWithHandle(
                imageData,
                type,
                java.util.Map.of(),
                null
            );
            return ResponseEntity.accepted().body(java.util.Map.of("taskId", handle.taskId(), "status", "accepted", "detectionType", detectionType));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/vision/tasks/{taskId}")
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> getAsyncTask(@org.springframework.web.bind.annotation.PathVariable("taskId") String taskId) {
        com.springvision.core.async.TaskProgress progress = asyncProcessor.getTaskProgress(taskId);
        if (progress == null) {
            return ResponseEntity.status(404).body(java.util.Map.of("taskId", taskId, "status", "not_found"));
        }
        java.util.Map<String,Object> payload = new java.util.HashMap<>();
        payload.put("taskId", progress.getTaskId());
        payload.put("status", progress.getStatus().toString());
        payload.put("completion", progress.getCompletionPercentage());
        payload.put("message", progress.getMessage());
        if (progress.isCompleted() && progress.getResult() != null) {
            VisionResult r = progress.getResult();
            payload.put("result", java.util.Map.of(
                "type", r.detectionType().getCode(),
                "count", r.detectionCount(),
                "avgConfidence", r.averageConfidence()
            ));
        }
        return ResponseEntity.ok(payload);
    }

    @PostMapping(value = "/api/vision/batch/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> batchFaces(@RequestParam("files") java.util.List<MultipartFile> files) {
        try {
            java.util.List<ImageData> images = new java.util.ArrayList<>();
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    images.add(ImageData.fromBytes(f.getBytes()));
                }
            }
            if (images.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "No files provided"));
            }
            com.springvision.core.batch.BatchVisionProcessor batch = new com.springvision.core.batch.BatchVisionProcessor();
            java.util.concurrent.CompletableFuture<java.util.List<VisionResult>> future = batch.processBatch(images, com.springvision.core.DetectionType.FACE, java.util.Map.of());
            java.util.List<VisionResult> results = future.join();
            int total = results.stream().mapToInt(VisionResult::detectionCount).sum();
            return ResponseEntity.ok(java.util.Map.of(
                "images", images.size(),
                "totalDetections", total
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/api/vision/batch/{detectionType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> batchDetection(@RequestParam("files") java.util.List<MultipartFile> files,
            @org.springframework.web.bind.annotation.PathVariable("detectionType") String detectionType) {
        try {
            com.springvision.core.DetectionType type = com.springvision.core.DetectionType.fromCode(detectionType);
            if (type == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid detection type: " + detectionType));
            }
            java.util.List<ImageData> images = new java.util.ArrayList<>();
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    images.add(ImageData.fromBytes(f.getBytes()));
                }
            }
            if (images.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "No files provided"));
            }
            com.springvision.core.batch.BatchVisionProcessor batch = new com.springvision.core.batch.BatchVisionProcessor();
            java.util.concurrent.CompletableFuture<java.util.List<VisionResult>> future = batch.processBatch(images, type, java.util.Map.of());
            java.util.List<VisionResult> results = future.join();
            int total = results.stream().mapToInt(VisionResult::detectionCount).sum();
            return ResponseEntity.ok(java.util.Map.of(
                "detectionType", detectionType,
                "images", images.size(),
                "totalDetections", total
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // Basic Detection Endpoints
    @PostMapping(value = "/api/vision/detect/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> detectFacesApi(@RequestParam("file") MultipartFile file) {
        try {
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            VisionResult result = visionTemplate.detectFaces(imageData);
            return ResponseEntity.ok(java.util.Map.<String,Object>of(
                "detectionType", "face",
                "detections", result.detections(),
                "count", result.detectionCount(),
                "avgConfidence", result.averageConfidence()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // Object Detection Endpoints
    @PostMapping(value = "/api/vision/detect/objects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> detectObjects(@RequestParam("file") MultipartFile file) {
        try {
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            VisionResult result = visionTemplate.detect(imageData, com.springvision.core.DetectionType.OBJECT);
            return ResponseEntity.ok(java.util.Map.of(
                "detectionType", "object",
                "detections", result.detections(),
                "count", result.detectionCount(),
                "avgConfidence", result.averageConfidence()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/api/vision/detect/barcodes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> detectBarcodes(@RequestParam("file") MultipartFile file) {
        try {
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            VisionResult result = visionTemplate.detect(imageData, com.springvision.core.DetectionType.BARCODE);
            return ResponseEntity.ok(java.util.Map.of(
                "detectionType", "barcode",
                "detections", result.detections(),
                "count", result.detectionCount(),
                "avgConfidence", result.averageConfidence()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/api/vision/detect/text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> detectText(@RequestParam("file") MultipartFile file) {
        try {
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            VisionResult result = visionTemplate.detect(imageData, com.springvision.core.DetectionType.TEXT);
            return ResponseEntity.ok(java.util.Map.of(
                "detectionType", "text",
                "detections", result.detections(),
                "count", result.detectionCount(),
                "avgConfidence", result.averageConfidence()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // Enhanced annotation endpoints
    @PostMapping(value = "/api/vision/annotate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> annotateImage(@RequestParam("file") MultipartFile file,
            @RequestParam("action") String action,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "categories", defaultValue = "FACE") String categoriesCsv) {
        try {
            com.springvision.core.AnnotationRequest.Action annotationAction = com.springvision.core.AnnotationRequest.Action.valueOf(action.toUpperCase());
            java.util.Set<com.springvision.core.DetectionCategory> cats = java.util.Arrays.stream(categoriesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(com.springvision.core.DetectionCategory::valueOf)
                .collect(java.util.stream.Collectors.toSet());

            com.springvision.core.AnnotationRequest req = new com.springvision.core.AnnotationRequest.Builder()
                .action(annotationAction)
                .categories(cats)
                .label(label)
                .build();

            ImageData imageData = ImageData.fromBytes(file.getBytes());
            visionTemplate.annotate(imageData, req);

            return ResponseEntity.ok(java.util.Map.of(
                "action", action,
                "categories", cats.stream().map(Enum::name).toList(),
                "label", label,
                "annotated", true
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/api/vision/detect/query", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> detectWithQuery(@RequestParam("file") MultipartFile file,
            @RequestParam("detectionType") String detectionType,
            @RequestParam(value = "minConfidence", defaultValue = "0.5") double minConfidence,
            @RequestParam(value = "maxDetections", defaultValue = "100") int maxDetections) {
        try {
            com.springvision.core.DetectionType type = com.springvision.core.DetectionType.fromCode(detectionType);
            if (type == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid detection type: " + detectionType));
            }

            com.springvision.core.DetectionQuery.Builder queryBuilder = new com.springvision.core.DetectionQuery.Builder()
                .type(type)
                .minConfidence(minConfidence)
                .maxDetections(maxDetections);

            com.springvision.core.DetectionQuery query = queryBuilder.build();
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            VisionResult result = visionTemplate.detect(imageData, query);

            return ResponseEntity.ok(java.util.Map.of(
                "detectionType", detectionType,
                "query", java.util.Map.of(
                    "minConfidence", minConfidence,
                    "maxDetections", maxDetections
                ),
                "detections", result.detections(),
                "count", result.detectionCount(),
                "avgConfidence", result.averageConfidence()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
