package io.github.codesapienbe.springvision.cyber.detectors;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.cyber.models.QRCodeThreat;
import io.github.codesapienbe.springvision.cyber.utils.URLSecurityAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detector for identifying suspicious QR codes that may lead to malicious websites
 * or represent security threats.
 *
 * <p>This detector:
 * <ul>
 *   <li>Scans images for QR codes using ZXing library</li>
 *   <li>Extracts and analyzes embedded URLs</li>
 *   <li>Identifies potential phishing or malicious links</li>
 *   <li>Provides threat severity ratings</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public class QRCodeSecurityDetector {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeSecurityDetector.class);

    private final MultiFormatReader qrReader;
    private final URLSecurityAnalyzer urlAnalyzer;

    public QRCodeSecurityDetector() {
        this.qrReader = new MultiFormatReader();
        this.urlAnalyzer = new URLSecurityAnalyzer();
        logger.info("Initialized QRCodeSecurityDetector");
    }

    /**
     * Detects and analyzes QR codes in an image for potential security threats.
     *
     * @param imageData the image to analyze
     * @return list of detections with threat information
     */
    public List<Detection> detectSuspiciousQRCodes(ImageData imageData) {
        List<Detection> detections = new ArrayList<>();

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (image == null) {
                logger.warn("Failed to read image data");
                return detections;
            }

            // Decode QR code
            BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image))
            );

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            try {
                Result result = qrReader.decode(bitmap, hints);
                String decodedText = result.getText();

                logger.debug("Decoded QR code: {}", decodedText);

                // Analyze the decoded content for security threats
                QRCodeThreat threat = analyzeQRCodeContent(decodedText);

                // Create detection with threat information
                Detection detection = createDetectionFromThreat(threat, image.getWidth(), image.getHeight());
                detections.add(detection);

            } catch (Exception e) {
                logger.debug("No QR code found or decode failed: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Error detecting QR codes", e);
        }

        return detections;
    }

    /**
     * Analyzes QR code content to identify potential threats.
     *
     * @param content the decoded QR code content
     * @return threat analysis result
     */
    private QRCodeThreat analyzeQRCodeContent(String content) {
        QRCodeThreat threat = new QRCodeThreat(content);

        // Check if content is a URL
        if (content.startsWith("http://") || content.startsWith("https://")) {
            threat.setType(QRCodeThreat.ThreatType.URL);

            // Analyze URL for security issues
            URLSecurityAnalyzer.SecurityScore score = urlAnalyzer.analyzeURL(content);
            threat.setSeverity(score.getSeverity());
            threat.setThreats(score.getThreats());
            threat.setUrl(content);

        } else if (content.startsWith("tel:") || content.startsWith("sms:")) {
            threat.setType(QRCodeThreat.ThreatType.COMMUNICATION);
            threat.setSeverity(QRCodeThreat.ThreatSeverity.LOW);
        } else if (content.contains("wifi:") || content.startsWith("WIFI:")) {
            threat.setType(QRCodeThreat.ThreatType.WIFI);
            threat.setSeverity(QRCodeThreat.ThreatSeverity.MEDIUM);
        } else {
            threat.setType(QRCodeThreat.ThreatType.UNKNOWN);
            threat.setSeverity(QRCodeThreat.ThreatSeverity.LOW);
        }

        return threat;
    }

    /**
     * Creates a Detection object from threat analysis.
     */
    private Detection createDetectionFromThreat(QRCodeThreat threat, int imageWidth, int imageHeight) {
        // Create normalized bounding box (center of image as default)
        BoundingBox bbox = BoundingBox.fromPixels(
            imageWidth / 4,
            imageHeight / 4,
            imageWidth / 2,
            imageHeight / 2,
            imageWidth,
            imageHeight
        );

        // Add threat metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("content", threat.getContent());
        metadata.put("threatType", threat.getType().name());
        metadata.put("severity", threat.getSeverity().name());
        metadata.put("isSuspicious", threat.getSeverity().ordinal() >= QRCodeThreat.ThreatSeverity.MEDIUM.ordinal());

        if (threat.getUrl() != null) {
            metadata.put("url", threat.getUrl());
        }
        if (threat.getThreats() != null && !threat.getThreats().isEmpty()) {
            metadata.put("threats", threat.getThreats());
        }

        return new Detection("QR_CODE", 0.95, bbox, metadata);
    }

    /**
     * Configures the sensitivity of threat detection.
     *
     * @param sensitivity sensitivity level (0.0 to 1.0)
     */
    public void setSensitivity(double sensitivity) {
        urlAnalyzer.setSensitivity(sensitivity);
        logger.info("QR code detection sensitivity set to: {}", sensitivity);
    }
}
