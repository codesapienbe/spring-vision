/**
 * Custom DJL translators for Spring Vision models.
 *
 * <p>Translators are responsible for pre-processing input data and post-processing
 * model outputs. They bridge the gap between application data types and model
 * tensor formats.</p>
 *
 * <h2>Available Translators</h2>
 * <ul>
 *   <li>{@link YuNetFaceDetectionTranslator} - YuNet face detection model</li>
 *   <li>{@link SFaceFaceRecognitionTranslator} - SFace face recognition model</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use custom translator with DJL
 * Criteria<Image, DetectedObjects> criteria = Criteria.builder()
 *     .setTypes(Image.class, DetectedObjects.class)
 *     .optModelPath(Paths.get(modelPath))
 *     .optTranslator(new YuNetFaceDetectionTranslator())
 *     .build();
 *
 * ZooModel<Image, DetectedObjects> model = criteria.loadModel();
 * }</pre>
 *
 * @since 1.0.5
 */
package io.github.codesapienbe.springvision.core.djl.translator;

