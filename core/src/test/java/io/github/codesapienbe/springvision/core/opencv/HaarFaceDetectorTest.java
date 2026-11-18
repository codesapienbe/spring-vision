package io.github.codesapienbe.springvision.core.opencv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Runs the real Haar cascades on a public-domain portrait (see
 * {@code src/test/resources/faces/SOURCES.md}).
 */
@DisplayName("HaarFaceDetector")
class HaarFaceDetectorTest {

    private static HaarFaceDetector detector;

    @BeforeAll
    static void loadCascades() {
        detector = new HaarFaceDetector();
    }

    @AfterAll
    static void close() {
        detector.close();
    }

    static byte[] portrait() throws IOException {
        try (InputStream in = HaarFaceDetectorTest.class.getResourceAsStream("/faces/nasa-s69-31741-portrait.jpg")) {
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("finds the face in a portrait, in the upper half of the image")
    void findsFaceInPortrait() throws IOException {
        try (Mat image = OpenCvImages.decode(portrait())) {
            List<HaarFaceDetector.Face> faces = detector.detect(image);

            assertThat(faces).isNotEmpty();
            PixelRegion face = faces.get(0).region();
            assertThat(face.y() + face.height() / 2).isLessThan(image.rows() / 2);
            assertThat(face.width()).isGreaterThan(image.cols() / 8);
        }
    }

    @Test
    @DisplayName("finds nothing in a plain grey image")
    void findsNothingInPlainImage() {
        try (Mat plain = new Mat(480, 384, opencv_core.CV_8UC3, new Scalar(128, 128, 128, 0))) {
            assertThat(detector.detect(plain)).isEmpty();
        }
    }
}
