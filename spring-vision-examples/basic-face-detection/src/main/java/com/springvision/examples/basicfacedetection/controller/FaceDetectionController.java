package com.springvision.examples.basicfacedetection.controller;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.DetectionType;
import com.springvision.core.VisionTemplate;
import com.springvision.core.Detection;
import com.springvision.core.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

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

    /**
     * Constructor with VisionTemplate dependency injection.
     *
     * @param visionTemplate the vision template for processing images
     */
    @Autowired
    public FaceDetectionController(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
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

            if (result.hasDetections()) {
                List<Detection> detections = result.detections();

                model.addAttribute("detections", detections);
                logger.info("Detected {} faces in image", detections.size());
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

            if (result.hasDetections()) {
                model.addAttribute("detections", result.detections());
                logger.info("Detected {} faces from URL image", result.detectionCount());
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
}
