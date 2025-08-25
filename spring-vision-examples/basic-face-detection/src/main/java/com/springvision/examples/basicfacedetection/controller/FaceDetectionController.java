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

/**
 * Controller for handling face detection requests.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Displaying the main face detection page</li>
 *   <li>Processing uploaded images for face detection</li>
 *   <li>Returning detection results</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Controller
public class FaceDetectionController {

    private static final Logger logger = LoggerFactory.getLogger(FaceDetectionController.class);

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
}
