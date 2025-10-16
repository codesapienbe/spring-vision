/**
 * Backend package for the Spring Vision framework.
 *
 * <p>This package contains the core interfaces and default backend utilities
 * used by Spring Vision. The project now uses DJL (Deep Java Library) as the
 * primary/default runtime for model inference. Backend implementations are
 * pluggable and can be provided via separate modules.</p>
 *
 * <h2>Default Backend</h2>
 *
 * <p>DJL is the default runtime for model loading and inference. The
 * codebase includes a DJL-backed implementation which handles model
 * lifecycle, translators, and loading from the DJL ModelZoo.
 * Optional/legacy backend implementations (historical OpenCV-based code)
 * have been removed from the main branch and are available only via the
 * opt-in {@code legacy-backends} Maven profile or archived documentation.</p>
 *
 * <h2>Usage</h2>
 *
 * <p>Work with the generic {@link io.github.codesapienbe.springvision.core.VisionBackend}
 * interface rather than backend-specific classes to ensure backend portability.</p>
 *
 * <pre>{@code J
 * VisionBackend backend = ...; // injected or created via VisionAutoConfiguration
 * backend.initialize();
 * VisionResult result = backend.detectFaces(ImageData.fromBytes(imageBytes));
 * if (result.hasDetections()) { ... }
 * backend.shutdown();
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>Configure the active backend via application properties. DJL is used by
 * default; legacy backends are opt-in through the Maven profile
 * {@code -Plegacy-backends}.</p>
 *
 * <h2>Notes</h2>
 *
 * <ul>
 *   <li>The old OpenCV-specific backend implementation and its configuration
 *       properties were removed to reduce native dependency complexity.</li>
 *   <li>If you need OpenCV/native artifacts for an older workflow, enable
 *       the {@code legacy-backends} profile during your build.</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see io.github.codesapienbe.springvision.core.VisionBackend
 * @see io.github.codesapienbe.springvision.core.VisionTemplate
 * @since 1.0.0
 */
package io.github.codesapienbe.springvision.core.backend;
