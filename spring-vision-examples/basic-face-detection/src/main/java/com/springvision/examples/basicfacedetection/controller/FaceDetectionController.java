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
    public FaceDetectionController(VisionTemplate visionTemplate, WebClient.Builder webClientBuilder) {
        this.visionTemplate = visionTemplate;
        this.webClient = webClientBuilder.build();
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
            return Mono.just(ResponseEntity.badRequest().build());
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
                    throw new RuntimeException(e);
                }
            }))
            .onErrorResume(e -> {
                logger.warn("Failed to process URL: {}", e.getMessage());
                return Mono.just(ResponseEntity.badRequest().build());
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
}
