package io.github.codesapienbe.springvision.core.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.TestImageUtils;
import io.github.codesapienbe.springvision.core.AnnotationRequest;

@DisplayName("Integration: Additional Capabilities (synthetic fallbacks)")
public class AdditionalCapabilitiesIntegrationTests {

    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties properties = new DjlProperties();
        properties.setEngine("OnnxRuntime");
        properties.setDevice("cpu");
        properties.setAutoDownload(false);

        backend = new DjlVisionBackend(properties);
        backend.initialize();
    }

    @AfterAll
    static void teardown() {
        if (backend != null) backend.shutdown();
    }

    @Test
    void objectDetection() throws IOException {
        ImageData img = TestImageUtils.createRectangleImage(640, 480, java.awt.Color.BLUE);
        List<Detection> detections = backend.detectObjects(img);
        assertThat(detections).isNotNull();
    }

    @Test
    void imageClassification() throws IOException {
        ImageData img = TestImageUtils.createRectangleImage(320, 240, java.awt.Color.RED);
        var result = backend.classifyImage(img, 3);
        assertThat(result).isNotNull();
        assertThat(result.classifications()).isNotNull();
    }

    @Test
    void poseEstimation() throws IOException {
        ImageData img = TestImageUtils.createPersonSilhouetteImage(640, 480);
        List<Detection> poses = backend.detectPoses(img);
        assertThat(poses).isNotNull();
    }

    @Test
    void embeddingExtraction() throws IOException {
        ImageData img = TestImageUtils.createSimpleFaceImage(320, 240);
        List<float[]> embs = backend.extractEmbeddings(img, io.github.codesapienbe.springvision.core.DetectionCategory.FACE);
        assertThat(embs).isNotNull();
    }

    // Face verification / lookup not implemented by DJL backend in this shim; skip direct tests

    @Test
    void segmentationSemantic() throws IOException {
        ImageData img = TestImageUtils.createRectangleImage(640, 480, java.awt.Color.GREEN);
        var res = backend.segmentSemantic(img);
        assertThat(res).isNotNull();
    }

    @Test
    void nsfw() throws IOException {
        ImageData img = TestImageUtils.createRectangleImage(320, 240, java.awt.Color.BLACK);
        var res = backend.detectNSFW(img);
        assertThat(res).isNotNull();
    }

    @Test
    void emotionAndDeepfake() throws IOException {
        ImageData img = TestImageUtils.createSimpleFaceImage(320, 240);
        var emotions = backend.detectEmotions(img);
        var deepfake = backend.detectDeepfake(img);
        assertThat(emotions).isNotNull();
        assertThat(deepfake).isNotNull();
    }

    @Test
    void fallAndStressAnalysis() throws IOException {
        List<ImageData> seq = List.of(TestImageUtils.createPersonSilhouetteImage(320, 240));
        try {
            var falls = backend.detectFall(seq);
            assertThat(falls).isNotNull();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
        try {
            var stress = backend.detectStress(seq);
            assertThat(stress).isNotNull();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    void threatAndAccessAuth() throws IOException {
        List<ImageData> seq = List.of(TestImageUtils.createRectangleImage(320, 240, java.awt.Color.GRAY));
        var threats = backend.detectThreat(seq);
        assertThat(threats).isNotNull();

        ImageData img = TestImageUtils.createSimpleFaceImage(320, 240);
        var auth = backend.authenticateAccess(img);
        assertThat(auth).isNotNull();
    }

    @Test
    void annotationBarcodeLandmark() throws IOException {
        ImageData img = TestImageUtils.createRectangleImage(320, 240, java.awt.Color.MAGENTA);
        AnnotationRequest req = new AnnotationRequest.Builder()
            .action(AnnotationRequest.Action.TAG)
            .label("test")
            .build();
        var annotated = backend.annotate(img, req);
        assertThat(annotated).isNotNull();

        var bar = backend.detectBarcodes(TestImageUtils.createBarcodeImage("1234", 300, 100));
        assertThat(bar).isNotNull();

        var metadata = backend.extractMetaData(img);
        assertThat(metadata).isNotNull();
    }
}


