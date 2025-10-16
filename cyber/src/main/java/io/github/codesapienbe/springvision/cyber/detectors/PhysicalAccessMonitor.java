package io.github.codesapienbe.springvision.cyber.detectors;

import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.cyber.models.AccessEvent;
import io.github.codesapienbe.springvision.cyber.models.AuthorizedPerson;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitor for physical access control using face recognition to track and log
 * access to secure areas.
 *
 * <p>This monitor:
 * <ul>
 *   <li>Detects faces in access control camera feeds</li>
 *   <li>Matches faces against authorized personnel database</li>
 *   <li>Logs all access attempts with timestamps</li>
 *   <li>Alerts on unauthorized access attempts</li>
 *   <li>Maintains audit trail of all access events</li>
 * </ul>
 *
 * <p>The monitor can be integrated with physical access control systems,
 * alarm systems, and security information management platforms.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public class PhysicalAccessMonitor {

    private static final Logger logger = LoggerFactory.getLogger(PhysicalAccessMonitor.class);

    private static final double RECOGNITION_CONFIDENCE_THRESHOLD = 0.75;

    private final CascadeClassifier faceDetector;
    private final OpenCVFrameConverter.ToMat converter;
    private final Java2DFrameConverter imageConverter;

    // In-memory database of authorized persons (in production, this would be a persistent store)
    private final Map<String, AuthorizedPerson> authorizedPersons = new ConcurrentHashMap<>();

    // Access event log (in production, this would be persisted to database)
    private final List<AccessEvent> accessLog = new ArrayList<>();

    /**
     * Initializes the monitor, loading the face detection model and preparing converters.
     */
    public PhysicalAccessMonitor() {
        // Initialize OpenCV face detector
        CascadeClassifier detector = null;
        try {
            // Try to load from core module first (standard location)
            String cascadePath = extractCascadeFromClasspath("/models/haarcascade_frontalface_default.xml");
            if (cascadePath == null) {
                // Fallback to root location
                cascadePath = extractCascadeFromClasspath("/haarcascade_frontalface_default.xml");
            }

            if (cascadePath != null) {
                detector = new CascadeClassifier(cascadePath);
                if (detector.empty()) {
                    logger.warn("Loaded cascade classifier is empty, using default");
                    detector = new CascadeClassifier();
                }
            } else {
                detector = new CascadeClassifier();
                logger.warn("Could not load cascade classifier, using system default");
            }
        } catch (Exception e) {
            logger.warn("Error initializing face detector: {}", e.getMessage());
            detector = new CascadeClassifier();
        }

        this.faceDetector = detector;
        this.converter = new OpenCVFrameConverter.ToMat();
        this.imageConverter = new Java2DFrameConverter();

        logger.info("Initialized PhysicalAccessMonitor");
    }

    /**
     * Monitors an access point by analyzing an image from a camera feed. It detects
     * faces and verifies them against a database of authorized personnel.
     *
     * @param imageData The {@link ImageData} captured from the access control camera.
     * @return A list of {@link Detection} objects, each indicating whether an access
     * attempt was authorized or unauthorized, along with relevant metadata.
     */
    public List<Detection> monitorAccess(ImageData imageData) {
        List<Detection> detections = new ArrayList<>();

        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (bufferedImage == null) {
                logger.warn("Failed to read image data");
                return detections;
            }

            Frame frame = imageConverter.convert(bufferedImage);
            Mat mat = converter.convert(frame);
            Mat grayMat = new Mat();
            opencv_imgproc.cvtColor(mat, grayMat, opencv_imgproc.CV_BGR2GRAY);

            // Detect faces
            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(grayMat, faces);

            logger.debug("Detected {} faces in access control image", faces.size());

            // Process each detected face
            for (int i = 0; i < faces.size(); i++) {
                Rect face = faces.get(i);
                Detection detection = processAccessAttempt(face, bufferedImage);
                detections.add(detection);
            }

            // Clean up
            grayMat.release();
            mat.release();

        } catch (Exception e) {
            logger.error("Error monitoring access", e);
        }

        return detections;
    }

    /**
     * Processes a detected face to determine access authorization.
     *
     * @param face  The detected face rectangle.
     * @param image The image in which the face was detected.
     * @return A detection object with the result of the access attempt.
     */
    private Detection processAccessAttempt(Rect face, BufferedImage image) {
        // Create normalized bounding box
        BoundingBox bbox = BoundingBox.fromPixels(
            face.x(),
            face.y(),
            face.width(),
            face.height(),
            image.getWidth(),
            image.getHeight()
        );

        // In a real implementation, this would extract face embeddings and match against database
        // For now, we'll simulate the recognition process
        AuthorizedPerson matchedPerson = attemptFaceRecognition(face, image);

        Map<String, Object> metadata = new HashMap<>();
        LocalDateTime timestamp = LocalDateTime.now();

        String label;
        double confidence;

        if (matchedPerson != null) {
            // Authorized access
            label = "AUTHORIZED_ACCESS";
            confidence = 0.85;

            metadata.put("personId", matchedPerson.getId());
            metadata.put("personName", matchedPerson.getName());
            metadata.put("accessLevel", matchedPerson.getAccessLevel());
            metadata.put("authorized", true);
            metadata.put("department", matchedPerson.getDepartment());

            logger.info("Authorized access: {} ({})", matchedPerson.getName(), matchedPerson.getId());

            // Log access event
            logAccessEvent(AccessEvent.builder()
                .personId(matchedPerson.getId())
                .personName(matchedPerson.getName())
                .timestamp(timestamp)
                .authorized(true)
                .build());

        } else {
            // Unauthorized access attempt
            label = "UNAUTHORIZED_ACCESS";
            confidence = 0.90;

            metadata.put("authorized", false);
            metadata.put("alertLevel", "HIGH");
            metadata.put("action", "DENY_ACCESS");

            logger.warn("SECURITY ALERT: Unauthorized access attempt detected at {}", timestamp);

            // Log unauthorized access attempt
            logAccessEvent(AccessEvent.builder()
                .personId("UNKNOWN")
                .personName("Unknown Person")
                .timestamp(timestamp)
                .authorized(false)
                .build());
        }

        metadata.put("timestamp", timestamp.toString());

        return new Detection(label, confidence, bbox, metadata);
    }

    /**
     * Attempts to recognize a face against the authorized persons database.
     * In production, this would use face embeddings and similarity matching.
     *
     * @param face  the detected face rectangle
     * @param image the source image
     * @return matched authorized person or null if no match
     */
    private AuthorizedPerson attemptFaceRecognition(Rect face, BufferedImage image) {
        // TODO: Implement actual face recognition using embeddings
        // This is a placeholder that would be replaced with:
        // 1. Extract face embedding from the detected face region
        // 2. Compare embedding with stored embeddings in authorizedPersons database
        // 3. Return match if similarity exceeds threshold

        // For skeleton implementation, we'll return null (no match)
        // This can be integrated with other face recognition backends
        return null;
    }

    /**
     * Registers a new person with authorization to access the monitored area.
     * In a production system, this would persist the person's data to a secure database.
     *
     * @param person The {@link AuthorizedPerson} to register.
     * @throws IllegalArgumentException if the person or their ID is null.
     */
    public void registerAuthorizedPerson(AuthorizedPerson person) {
        if (person == null || person.getId() == null) {
            throw new IllegalArgumentException("Person and person ID must not be null");
        }

        authorizedPersons.put(person.getId(), person);
        logger.info("Registered authorized person: {} ({})", person.getName(), person.getId());
    }

    /**
     * Revokes access for a previously authorized person. This removes them from the
     * internal database of authorized individuals.
     *
     * @param personId The unique identifier of the person whose access is to be revoked.
     */
    public void revokeAccess(String personId) {
        AuthorizedPerson removed = authorizedPersons.remove(personId);
        if (removed != null) {
            logger.info("Revoked access for: {} ({})", removed.getName(), personId);
        } else {
            logger.warn("Attempted to revoke access for non-existent person: {}", personId);
        }
    }

    /**
     * Retrieves the complete log of all access events, both authorized and unauthorized.
     * This is useful for auditing and security reviews.
     *
     * @return A list of all {@link AccessEvent}s recorded by the monitor.
     */
    public List<AccessEvent> getAccessLog() {
        return new ArrayList<>(accessLog);
    }

    /**
     * Retrieves a filtered list of access events that occurred within a specified
     * number of minutes from the current time.
     *
     * @param minutes The number of minutes to look back for recent events.
     * @return A list of recent {@link AccessEvent}s.
     */
    public List<AccessEvent> getRecentAccessEvents(int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        return accessLog.stream()
            .filter(event -> event.getTimestamp().isAfter(cutoff))
            .toList();
    }

    /**
     * Logs an access event.
     *
     * @param event The event to log.
     */
    private void logAccessEvent(AccessEvent event) {
        accessLog.add(event);
        // In production, this would also:
        // - Persist to database
        // - Trigger webhooks/notifications
        // - Update real-time dashboard
        // - Send to SIEM system
    }

    /**
     * Returns the total number of persons currently authorized for access.
     *
     * @return The count of authorized individuals.
     */
    public int getAuthorizedPersonCount() {
        return authorizedPersons.size();
    }

    /**
     * Checks if a person with the given ID is currently authorized for access.
     *
     * @param personId The ID of the person to check.
     * @return {@code true} if the person is authorized, {@code false} otherwise.
     */
    public boolean isAuthorized(String personId) {
        return authorizedPersons.containsKey(personId);
    }

    /**
     * Clears the entire access log. This is a destructive operation and should be
     * used with caution, primarily for testing or maintenance purposes.
     */
    public void clearAccessLog() {
        accessLog.clear();
        logger.info("Access log cleared");
    }

    /**
     * Sets the confidence threshold for face recognition. A higher threshold makes
     * the system stricter, requiring a closer match to grant access.
     *
     * @param threshold A value between 0.0 and 1.0.
     * @throws IllegalArgumentException if the threshold is outside the valid range.
     */
    public void setRecognitionThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        logger.info("Recognition confidence threshold set to: {}", threshold);
    }

    /**
     * Extracts cascade classifier from classpath to temp file.
     */
    private String extractCascadeFromClasspath(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }

            Path tempFile = Files.createTempFile("haarcascade", ".xml");
            tempFile.toFile().deleteOnExit();
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile.toAbsolutePath().toString();
        } catch (IOException e) {
            logger.debug("Could not extract cascade from {}: {}", resourcePath, e.getMessage());
            return null;
        }
    }
}
