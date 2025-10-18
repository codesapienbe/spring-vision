package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for biometric access authentication using face recognition.
 *
 * <p>This capability provides secure, vision-based authentication for access control
 * systems. It combines face detection, recognition, and matching against an authorized
 * user database to verify identity.</p>
 *
 * <h2>Authentication Flow</h2>
 * <ol>
 *   <li><b>Face Detection:</b> Locate face in the image</li>
 *   <li><b>Quality Check:</b> Verify image quality and face visibility</li>
 *   <li><b>Embedding Extraction:</b> Generate face embedding vector</li>
 *   <li><b>Database Matching:</b> Compare against authorized users</li>
 *   <li><b>Decision:</b> Authorize or deny access based on similarity threshold</li>
 * </ol>
 *
 * <p>The returned detection includes authentication metadata:</p>
 * <ul>
 *   <li><b>authorized:</b> Boolean indicating if access is granted</li>
 *   <li><b>userId:</b> Matched user ID if authorized</li>
 *   <li><b>userName:</b> Matched user name if authorized</li>
 *   <li><b>confidence:</b> Authentication confidence score (0.0 - 1.0)</li>
 *   <li><b>matchScore:</b> Similarity score with matched user</li>
 *   <li><b>reason:</b> Failure reason if unauthorized</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Authenticate user from camera image
 * List<Detection> authResults = accessAuth.authenticateAccess(cameraImage);
 * 
 * if (!authResults.isEmpty()) {
 *     Detection result = authResults.get(0);
 *     Boolean authorized = (Boolean) result.attributes().get("authorized");
 *     
 *     if (Boolean.TRUE.equals(authorized)) {
 *         String userId = (String) result.attributes().get("userId");
 *         String userName = (String) result.attributes().get("userName");
 *         // Grant access
 *         grantAccess(userId, userName);
 *     } else {
 *         String reason = (String) result.attributes().get("reason");
 *         // Deny access
 *         denyAccess(reason);
 *     }
 * }
 * }</pre>
 *
 * <h2>Security Considerations</h2>
 * <p><b>⚠️ IMPORTANT:</b> For production deployments, implement:</p>
 * <ul>
 *   <li><b>Liveness Detection:</b> Prevent photo/video replay attacks</li>
 *   <li><b>Multi-Factor Authentication:</b> Combine face with PIN, card, or OTP</li>
 *   <li><b>Audit Logging:</b> Log all authentication attempts with timestamps</li>
 *   <li><b>Secure Storage:</b> Encrypt biometric templates at rest and in transit</li>
 *   <li><b>Threshold Tuning:</b> Balance security vs. usability</li>
 *   <li><b>Fail-Safe Mechanisms:</b> Alternative authentication methods</li>
 * </ul>
 *
 * <h2>Privacy and Compliance</h2>
 * <ul>
 *   <li>Obtain explicit user consent for biometric data collection</li>
 *   <li>Comply with biometric privacy laws (GDPR, BIPA, etc.)</li>
 *   <li>Implement data retention and deletion policies</li>
 *   <li>Provide transparency about data usage</li>
 *   <li>Offer opt-out and alternative authentication methods</li>
 * </ul>
 *
 * <p><b>Recommended Use Cases:</b></p>
 * <ul>
 *   <li>Secure facility access control</li>
 *   <li>Time and attendance systems</li>
 *   <li>Device unlocking and login</li>
 *   <li>High-security areas (data centers, labs)</li>
 *   <li>VIP/employee verification</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 * @see Detection
 * @see io.github.codesapienbe.springvision.core.DetectionType#ACCESS_AUTH
 * @see EmbeddingCapability
 */
public interface AccessAuthenticationCapability {

    /**
     * Authenticates access from a single image using face recognition.
     *
     * <p>Performs biometric authentication by detecting a face, extracting its
     * embedding, and matching against authorized users. Returns authentication
     * result with detailed metadata.</p>
     *
     * <p><b>Authentication Status:</b></p>
     * <ul>
     *   <li><b>AUTHORIZED:</b> Face matched with confidence above threshold</li>
     *   <li><b>UNAUTHORIZED:</b> No match found or confidence too low</li>
     *   <li><b>ERROR:</b> No face detected, multiple faces, or poor image quality</li>
     * </ul>
     *
     * <p><b>Returned Detection Attributes:</b></p>
     * <ul>
     *   <li>{@code authorized} (Boolean): True if access granted</li>
     *   <li>{@code userId} (String): Matched user ID (if authorized)</li>
     *   <li>{@code userName} (String): Matched user name (if authorized)</li>
     *   <li>{@code confidence} (Double): Authentication confidence (0.0 - 1.0)</li>
     *   <li>{@code matchScore} (Double): Similarity score with matched user</li>
     *   <li>{@code reason} (String): Failure reason if unauthorized</li>
     *   <li>{@code timestamp} (String): Authentication timestamp (ISO 8601)</li>
     * </ul>
     *
     * @param imageData the image data containing a face to authenticate
     * @return a list containing a single detection with authentication results;
     *         empty list should not occur (error states return UNAUTHORIZED detection)
     * @throws io.github.codesapienbe.springvision.core.exception.BaseVisionException if authentication fails critically
     */
    List<Detection> authenticateAccess(ImageData imageData);
}
