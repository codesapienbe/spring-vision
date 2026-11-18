package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.AnnotationRequest;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionCategory;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;
import io.github.codesapienbe.springvision.core.opencv.OpenCvImages;

/**
 * detectFaces, obscure and annotate(OBSCURE) on a real portrait. Only the OpenCV natives are
 * needed, so the backend is marked initialised without loading the DJL models.
 */
@DisplayName("DjlVisionBackend — OpenCV face detection and obscuring")
class DjlVisionBackendObscureTest {

    private DjlVisionBackend backend;
    private ImageData portrait;

    @BeforeEach
    void setUp() throws Exception {
        backend = new DjlVisionBackend();
        Field initialized = DjlVisionBackend.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(backend, true);
        try (InputStream in = getClass().getResourceAsStream("/faces/nasa-s69-31741-portrait.jpg")) {
            portrait = ImageData.fromBytes(in.readAllBytes(), "image/jpeg");
        }
    }

    @AfterEach
    void tearDown() {
        backend.shutdown();
    }

    @Test
    @DisplayName("detectFaces returns normalised boxes from the Haar cascade")
    void detectFacesReturnsNormalisedBoxes() {
        List<Detection> faces = backend.detectFaces(portrait);

        assertThat(faces).isNotEmpty();
        Detection face = faces.get(0);
        assertThat(face.label()).isEqualTo("face");
        assertThat(face.attributes()).containsEntry("model", "opencv-haar-cascade")
            .containsEntry("confidenceKind", "binary");
        assertThat(face.boundingBox().x() + face.boundingBox().width()).isLessThanOrEqualTo(1.0);
        assertThat(face.boundingBox().y() + face.boundingBox().height()).isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("obscure changes the face region and keeps the image size")
    void obscureChangesFaceRegion() throws IOException {
        Detection face = backend.detectFaces(portrait).get(0);

        ImageData obscured = backend.obscure(portrait, detection -> true);

        try (Mat before = OpenCvImages.decode(portrait.data()); Mat after = OpenCvImages.decode(obscured.data())) {
            assertThat(after.cols()).isEqualTo(before.cols());
            assertThat(after.rows()).isEqualTo(before.rows());
            int cx = (int) ((face.boundingBox().x() + face.boundingBox().width() / 2) * before.cols());
            int cy = (int) ((face.boundingBox().y() + face.boundingBox().height() / 2) * before.rows());
            assertThat(meanAbsDiff(before, after, cx, cy, 10)).isGreaterThan(5.0);
        }
    }

    @Test
    @DisplayName("obscure with a filter that matches nothing leaves the face detail")
    void obscureWithEmptyFilterKeepsFace() {
        Detection face = backend.detectFaces(portrait).get(0);

        ImageData untouched = backend.obscure(portrait, detection -> false);

        try (Mat before = OpenCvImages.decode(portrait.data()); Mat after = OpenCvImages.decode(untouched.data())) {
            int cx = (int) ((face.boundingBox().x() + face.boundingBox().width() / 2) * before.cols());
            int cy = (int) ((face.boundingBox().y() + face.boundingBox().height() / 2) * before.rows());
            // only JPEG re-encoding noise remains
            assertThat(meanAbsDiff(before, after, cx, cy, 10)).isLessThan(5.0);
        }
    }

    @Test
    @DisplayName("annotate(OBSCURE, FACE) obscures; other categories are unsupported")
    void annotateObscure() {
        AnnotationRequest faces = new AnnotationRequest.Builder()
            .action(AnnotationRequest.Action.OBSCURE).categories(Set.of(DetectionCategory.FACE)).build();
        assertThat(backend.annotate(portrait, faces).data()).isNotEqualTo(portrait.data());

        AnnotationRequest other = new AnnotationRequest.Builder()
            .action(AnnotationRequest.Action.OBSCURE).categories(Set.of(DetectionCategory.EYE)).build();
        assertThatThrownBy(() -> backend.annotate(portrait, other))
            .isInstanceOf(VisionUnsupportedException.class)
            .hasMessageContaining("FACE");
    }

    /** Mean absolute difference over a (2r+1)^2 window of all three channels. */
    private static double meanAbsDiff(Mat a, Mat b, int cx, int cy, int r) {
        long sum = 0;
        int count = 0;
        try (UByteIndexer ia = a.createIndexer(); UByteIndexer ib = b.createIndexer()) {
            for (int y = cy - r; y <= cy + r; y++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    for (int c = 0; c < 3; c++) {
                        sum += Math.abs(ia.get(y, x, c) - ib.get(y, x, c));
                        count++;
                    }
                }
            }
        }
        return (double) sum / count;
    }
}
