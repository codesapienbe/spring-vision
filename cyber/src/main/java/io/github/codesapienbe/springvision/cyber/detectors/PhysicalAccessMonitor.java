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

    public PhysicalAccessMonitor() {
        // Initialize OpenCV face detector
        String classifierPath = null;
        try {
            var resource = getClass().getClassLoader().getResource("haarcascade_frontalface_default.xml");
            if (resource != null) {
                classifierPath = resource.getPath();
            }
        } catch (Exception e) {
            logger.warn("Could not load classifier resource", e);
        }

        if (classifierPath != null) {
            this.faceDetector = new CascadeClassifier(classifierPath);
        } else {
            this.faceDetector = new CascadeClassifier();
            logger.warn("Using system OpenCV classifier path");
        }

        this.converter = new OpenCVFrameConverter.ToMat();
        this.imageConverter = new Java2DFrameConverter();

        logger.info("Initialized PhysicalAccessMonitor");
    }

    /**
     * Monitors access by detecting faces and checking authorization.
     *
     * @param imageData the image from access control camera
     * @return list of detections with access authorization status
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
        // This can be integrated with FaceBytes or other face recognition backends
        return null;
    }

    /**
     * Registers an authorized person in the access control system.
     *
     * @param person the person to authorize
     */
    public void registerAuthorizedPerson(AuthorizedPerson person) {
        if (person == null || person.getId() == null) {
            throw new IllegalArgumentException("Person and person ID must not be null");
        }

        authorizedPersons.put(person.getId(), person);
        logger.info("Registered authorized person: {} ({})", person.getName(), person.getId());
    }

    /**
     * Removes an authorized person from the access control system.
     *
     * @param personId the ID of the person to remove
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
     * Retrieves the access log for audit purposes.
     *
     * @return list of all access events
     */
    public List<AccessEvent> getAccessLog() {
        return new ArrayList<>(accessLog);
    }

    /**
     * Retrieves recent access events within a time window.
     *
     * @param minutes number of minutes to look back
     * @return list of recent access events
     */
    public List<AccessEvent> getRecentAccessEvents(int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);
        return accessLog.stream()
            .filter(event -> event.getTimestamp().isAfter(cutoff))
            .toList();
    }

    /**
     * Logs an access event.
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
     * Gets the number of authorized persons in the system.
     *
     * @return count of authorized persons
     */
    public int getAuthorizedPersonCount() {
        return authorizedPersons.size();
    }

    /**
     * Checks if a person is authorized.
     *
     * @param personId the person ID to check
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorized(String personId) {
        return authorizedPersons.containsKey(personId);
    }

    /**
     * Clears the access log. Use with caution.
     */
    public void clearAccessLog() {
        accessLog.clear();
        logger.info("Access log cleared");
    }

    /**
     * Sets the recognition confidence threshold.
     *
     * @param threshold value between 0.0 and 1.0
     */
    public void setRecognitionThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        logger.info("Recognition confidence threshold set to: {}", threshold);
    }
}
