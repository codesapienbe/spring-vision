package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Capability for non-contact heart rate estimation from video sequences.
 *
 * <p>This capability attempts to estimate heart rate using remote photoplethysmography (rPPG),
 * which detects subtle color changes in facial skin caused by blood flow. This is a **research-level**
 * computer vision task with significant technical challenges and limitations.</p>
 *
 * <h2>⚠️ Important Limitations</h2>
 * <ul>
 *   <li><b>NOT a medical device</b> - Not FDA approved, not for clinical use</li>
 *   <li><b>Research accuracy</b> - Typical error: ±5-15 BPM under ideal conditions</li>
 *   <li><b>Environmental sensitivity</b> - Requires good lighting, minimal motion</li>
 *   <li><b>Technical requirements</b> - Needs 10-30 second video sequences</li>
 *   <li><b>Individual variation</b> - Accuracy varies significantly by person and conditions</li>
 * </ul>
 *
 * <h2>Use Cases (Non-Medical)</h2>
 * <ul>
 *   <li>Wellness and fitness applications (indicative only)</li>
 *   <li>Research and educational demonstrations</li>
 *   <li>Gaming and interactive experiences</li>
 *   <li>Stress monitoring (combined with other indicators)</li>
 * </ul>
 *
 * <h2>Technical Approach: Remote PPG</h2>
 * <p>Heart rate estimation involves multiple complex steps:</p>
 * <ol>
 *   <li><b>Face Detection & Tracking</b> - Locate and track face across frames</li>
 *   <li><b>ROI Selection</b> - Extract regions (forehead, cheeks) with good blood flow</li>
 *   <li><b>Signal Extraction</b> - Measure RGB color intensity changes over time</li>
 *   <li><b>Signal Processing</b> - Apply filters to remove noise and motion artifacts</li>
 *   <li><b>Frequency Analysis</b> - FFT to identify dominant periodic component</li>
 *   <li><b>BPM Calculation</b> - Convert frequency to beats per minute (45-240 BPM)</li>
 * </ol>
 *
 * <h2>Requirements for Accurate Estimation</h2>
 * <ul>
 *   <li><b>Video Length:</b> Minimum 10-30 seconds of continuous video</li>
 *   <li><b>Frame Rate:</b> At least 20-30 FPS for reliable signal</li>
 *   <li><b>Lighting:</b> Consistent, natural or full-spectrum lighting</li>
 *   <li><b>Motion:</b> Subject should remain relatively still</li>
 *   <li><b>Camera Quality:</b> Good resolution and color accuracy</li>
 *   <li><b>Face Visibility:</b> Clear, frontal view of face</li>
 * </ul>
 *
 * <h2>Detection Attributes</h2>
 * <p>Each Detection contains:</p>
 * <ul>
 *   <li><b>heartRate</b> (Double) - Estimated BPM (beats per minute)</li>
 *   <li><b>confidence</b> (Double) - Estimation confidence (0.0-1.0)</li>
 *   <li><b>signalQuality</b> (String) - "poor", "fair", "good"</li>
 *   <li><b>bpmRange</b> (String) - Estimated range (e.g., "70-75")</li>
 *   <li><b>framesAnalyzed</b> (Integer) - Number of frames processed</li>
 *   <li><b>duration</b> (Double) - Video duration in seconds</li>
 *   <li><b>method</b> (String) - Detection method used</li>
 * </ul>
 *
 * <h2>Implementation Status</h2>
 * <p><b>Current:</b> Placeholder implementation with mock data</p>
 * <p><b>Future:</b> Full rPPG implementation requires:</p>
 * <ul>
 *   <li>Signal processing library (FFT, bandpass filters)</li>
 *   <li>Advanced face tracking across frames</li>
 *   <li>Motion artifact detection and compensation</li>
 *   <li>Extensive validation with ground truth data</li>
 *   <li>Optimization for different lighting and skin tones</li>
 * </ul>
 *
 * <h2>Ethical & Legal Considerations</h2>
 * <p><b>Critical:</b> This capability must NEVER be used for:</p>
 * <ul>
 *   <li>Medical diagnosis or treatment decisions</li>
 *   <li>Emergency medical assessment</li>
 *   <li>Clinical monitoring without proper validation</li>
 *   <li>Any life-critical applications</li>
 * </ul>
 * <p>Always provide clear disclaimers and obtain informed consent.</p>
 *
 * @author Spring Vision Team
 * @see FaceDetectionCapability
 * @since 1.0.8
 */
public interface HeartRateCapability {

    /**
     * Estimates heart rate from a video sequence (list of frames).
     *
     * <p><b>Important:</b> Requires minimum 10-30 second video at 20+ FPS for reliable results.</p>
     * <p><b>Current Implementation:</b> Returns placeholder/mock data with clear disclaimers.</p>
     *
     * <p>Returns Detection(s) with estimated heart rate and quality metrics. For sequences,
     * may return per-frame estimates plus an aggregated final estimate.</p>
     *
     * @param imageDataList list of video frames (minimum 10-30 seconds recommended)
     * @return list of detections with heart rate estimates and quality indicators
     * @throws BaseVisionException if estimation fails
     * @throws IllegalArgumentException if insufficient frames provided
     */
    List<Detection> detectHeartRate(List<ImageData> imageDataList) throws BaseVisionException;
}
