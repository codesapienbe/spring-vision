/**
 * Spring Vision Robotics Module
 *
 * <p>This module provides computer vision capabilities for industrial automation and robotics applications.
 * It includes specialized detectors for defect detection, robotic arm guidance, and component verification
 * in manufacturing and assembly processes.</p>
 *
 * <h2>Main Features</h2>
 * <ul>
 *   <li><b>Defect Detection</b> - Identifies surface defects, dimensional issues, and quality problems in manufactured products</li>
 *   <li><b>Robotic Arm Guidance</b> - Provides 3D position, orientation, and grasp point information for pick-and-place operations</li>
 *   <li><b>Component Verification</b> - Verifies correct component usage and placement during assembly</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Autowired
 * private RoboticsBackend roboticsBackend;
 *
 * // Detect defects in a product image
 * List<Detection> defects = roboticsBackend.detectDefects(List.of(imageData));
 *
 * // Get guidance for robotic arm
 * List<Detection> guidance = roboticsBackend.provideGuidance(List.of(imageData));
 *
 * // Verify components in assembly
 * List<Detection> verifications = roboticsBackend.verifyComponents(List.of(imageData));
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <p>Enable the robotics backend in your application.properties:</p>
 * <pre>
 * spring.vision.robotics.enabled=true
 * </pre>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
package io.github.codesapienbe.springvision.robotics;

