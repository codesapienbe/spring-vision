package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.capabilities.VehicleDamageDetectionCapability;

@DisplayName("VisionTool — vehicle damage review flag logic")
class VisionToolDamageReviewTest {

    interface DamageBackend extends VisionBackend, VehicleDamageDetectionCapability {}

    private VisionTool visionTool;

    @BeforeEach
    void setUp() {
        DamageBackend backend = mock(DamageBackend.class);
        VectorService vectorService = mock(VectorService.class);
        VisionTemplate template = new VisionTemplate(backend, vectorService);
        visionTool = new VisionTool(template);
        visionTool.reliableConfidenceThreshold = 0.60;
    }

    @Test
    @DisplayName("computeReviewFlag returns NO_DETECTIONS when count is zero")
    void noDetectionsFlag() {
        VisionTool.ReviewFlag flag = visionTool.computeReviewFlag(0, 0.0);
        assertThat(flag.flag()).isEqualTo("NO_DETECTIONS");
        assertThat(flag.reason()).contains("Manual review recommended");
        assertThat(flag.reason()).contains("scratch");
    }

    @Test
    @DisplayName("computeReviewFlag returns LOW_CONFIDENCE when avgConfidence below threshold")
    void lowConfidenceFlag() {
        VisionTool.ReviewFlag flag = visionTool.computeReviewFlag(3, 0.45);
        assertThat(flag.flag()).isEqualTo("LOW_CONFIDENCE");
        assertThat(flag.reason()).contains("45.0%");
        assertThat(flag.reason()).contains("60%");
    }

    @Test
    @DisplayName("computeReviewFlag returns NONE when detections present and confidence sufficient")
    void noFlagOnGoodDetections() {
        VisionTool.ReviewFlag flag = visionTool.computeReviewFlag(5, 0.82);
        assertThat(flag.flag()).isEqualTo("NONE");
        assertThat(flag.reason()).isNull();
    }

    @Test
    @DisplayName("computeReviewFlag returns NONE at exactly the reliable threshold boundary")
    void noneAtThresholdBoundary() {
        VisionTool.ReviewFlag flag = visionTool.computeReviewFlag(1, 0.60);
        assertThat(flag.flag()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("EXTENDED_DAMAGE_CLASSES constant is accessible and non-empty")
    void extendedTaxonomyConstant() {
        assertThat(VisionTool.EXTENDED_DAMAGE_CLASSES)
            .isNotEmpty()
            .contains("scratch", "broken-component", "missing-panel");
    }

    @Test
    @DisplayName("NO_DETECTIONS reason lists all extended taxonomy classes")
    void noDetectionsReasonListsExtendedClasses() {
        VisionTool.ReviewFlag flag = visionTool.computeReviewFlag(0, 0.0);
        for (String cls : VisionTool.EXTENDED_DAMAGE_CLASSES) {
            assertThat(flag.reason()).contains(cls);
        }
    }
}
