package io.github.codesapienbe.springvision.core.opencv;

import static org.assertj.core.api.Assertions.assertThat;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_dnn.LayerParams;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves that the bundled OpenCV natives load on the build host and that the DNN module
 * runs a forward pass, without needing a model file: a one-layer ReLU network is built in
 * code, so the test exercises the same native path a real ONNX model would.
 */
@DisplayName("OpenCV natives — load and DNN forward pass")
class OpenCvNativeSmokeTest {

    @Test
    @DisplayName("loads OpenCV 4.14")
    void loadsExpectedOpenCvVersion() {
        assertThat(opencv_core.CV_VERSION()).startsWith("4.14.");
    }

    @Test
    @DisplayName("runs a DNN forward pass through a ReLU layer")
    void runsDnnForwardPass() {
        try (Net net = new Net();
            LayerParams params = new LayerParams();
            Mat input = new Mat(1, 4, opencv_core.CV_32F, new Scalar(-1.0))) {
            net.addLayerToPrev("relu", "ReLU", opencv_core.CV_32F, params);
            try (FloatIndexer in = input.createIndexer()) {
                in.put(0, 2, 3.0f);
            }

            net.setInput(input);
            try (Mat output = net.forward(); FloatIndexer out = output.createIndexer()) {
                assertThat(out.get(0, 0)).isZero();
                assertThat(out.get(0, 2)).isEqualTo(3.0f);
            }
        }
    }
}
