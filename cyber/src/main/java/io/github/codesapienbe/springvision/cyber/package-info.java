/**
 * Spring Vision Cyber Security Module
 *
 * <p>This module provides specialized computer vision capabilities for cybersecurity
 * applications, including QR code security analysis, shoulder surfing detection,
 * and physical access monitoring.
 *
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link io.github.codesapienbe.springvision.cyber.CyberSecurityBackend} -
 *       Main backend implementation extending VisionTemplate</li>
 *   <li>{@link io.github.codesapienbe.springvision.cyber.detectors.QRCodeSecurityDetector} -
 *       Detects and analyzes suspicious QR codes</li>
 *   <li>{@link io.github.codesapienbe.springvision.cyber.detectors.ShoulderSurfingDetector} -
 *       Identifies unauthorized viewing attempts</li>
 *   <li>{@link io.github.codesapienbe.springvision.cyber.detectors.PhysicalAccessMonitor} -
 *       Face-based access control and monitoring</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Autowired
 * private CyberSecurityBackend cyberBackend;
 *
 * public void scanQRCode(byte[] imageData) {
 *     ImageData image = ImageData.fromBytes(imageData);
 *     List<Detection> threats = cyberBackend.getQrCodeDetector()
 *         .detectSuspiciousQRCodes(image);
 *
 *     for (Detection detection : threats) {
 *         if ((boolean) detection.getMetadata().get("isSuspicious")) {
 *             // Handle suspicious QR code
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @version 1.0.0
 * @since 1.1.0
 */
package io.github.codesapienbe.springvision.cyber;

