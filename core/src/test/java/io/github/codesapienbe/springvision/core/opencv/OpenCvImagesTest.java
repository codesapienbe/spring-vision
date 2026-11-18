package io.github.codesapienbe.springvision.core.opencv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

@DisplayName("OpenCvImages")
class OpenCvImagesTest {

    /** A 64x64 BGR image whose blue channel is a horizontal gradient. */
    private static Mat gradient() {
        Mat image = new Mat(64, 64, opencv_core.CV_8UC3);
        try (UByteIndexer px = image.createIndexer()) {
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    px.put(y, x, 0, x * 4);
                    px.put(y, x, 1, 0);
                    px.put(y, x, 2, 0);
                }
            }
        }
        return image;
    }

    @Test
    @DisplayName("pixelated returns a new image and leaves the source untouched")
    void pixelatedDoesNotModifySource() {
        try (Mat source = gradient();
            Mat result = OpenCvImages.pixelated(source, List.of(new PixelRegion(0, 0, 32, 32)), 16)) {
            try (UByteIndexer src = source.createIndexer(); UByteIndexer out = result.createIndexer()) {
                // source keeps its gradient
                assertThat(src.get(0, 1, 0)).isEqualTo(4);
                assertThat(src.get(0, 2, 0)).isEqualTo(8);
                // inside the region, pixels of one block are now equal
                assertThat(out.get(0, 1, 0)).isEqualTo(out.get(0, 2, 0));
                // outside the region, the gradient survives
                assertThat(out.get(40, 41, 0)).isEqualTo(164);
                assertThat(out.get(40, 42, 0)).isEqualTo(168);
            }
        }
    }

    @Test
    @DisplayName("encode then decode keeps the image size")
    void encodeDecodeRoundTrip() {
        try (Mat source = gradient(); Mat decoded = OpenCvImages.decode(OpenCvImages.encode(source, "png"))) {
            assertThat(decoded.cols()).isEqualTo(64);
            assertThat(decoded.rows()).isEqualTo(64);
        }
    }

    @Test
    @DisplayName("decode rejects bytes that are not an image")
    void decodeRejectsGarbage() {
        assertThatThrownBy(() -> OpenCvImages.decode(new byte[] {1, 2, 3, 4}))
            .isInstanceOf(VisionProcessingException.class);
    }
}
