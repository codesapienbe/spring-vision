package io.github.codesapienbe.springvision.core.opencv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlateLocator")
class PlateLocatorTest {

    @Test
    @DisplayName("finds a red-on-white candidate over the plate")
    void findsRedOnWhitePlate() throws IOException {
        PixelRegion truth = new PixelRegion(SyntheticPlates.PLATE_BOX[0], SyntheticPlates.PLATE_BOX[1],
            SyntheticPlates.PLATE_BOX[2], SyntheticPlates.PLATE_BOX[3]);
        try (Mat scene = OpenCvImages.decode(SyntheticPlates.scene("1-ABC-123"))) {
            List<PlateLocator.Candidate> candidates = PlateLocator.locate(scene);

            assertThat(candidates).isNotEmpty();
            PlateLocator.Candidate first = candidates.get(0);
            assertThat(first.cue()).isEqualTo(PlateLocator.Cue.RED_ON_WHITE);
            assertThat(first.plate().iou(truth)).isGreaterThan(0.4);
        }
    }

    @Test
    @DisplayName("finds no red-on-white candidate in a scene without a plate")
    void noRedCandidateWithoutPlate() throws IOException {
        try (Mat scene = OpenCvImages.decode(SyntheticPlates.scene(null))) {
            assertThat(PlateLocator.locate(scene))
                .noneMatch(c -> c.cue() == PlateLocator.Cue.RED_ON_WHITE);
        }
    }
}
