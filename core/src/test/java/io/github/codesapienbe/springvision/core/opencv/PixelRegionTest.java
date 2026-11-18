package io.github.codesapienbe.springvision.core.opencv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PixelRegion")
class PixelRegionTest {

    @Test
    @DisplayName("iou is 1 for identical regions and 0 for disjoint ones")
    void iouOfIdenticalAndDisjoint() {
        PixelRegion a = new PixelRegion(10, 10, 20, 20);
        assertThat(a.iou(new PixelRegion(10, 10, 20, 20))).isEqualTo(1.0);
        assertThat(a.iou(new PixelRegion(100, 100, 5, 5))).isZero();
    }

    @Test
    @DisplayName("iou of half-overlapping squares is 1/3")
    void iouOfHalfOverlap() {
        PixelRegion a = new PixelRegion(0, 0, 10, 10);
        PixelRegion b = new PixelRegion(5, 0, 10, 10);
        assertThat(a.iou(b)).isCloseTo(50.0 / 150.0, within(1e-9));
    }

    @Test
    @DisplayName("expandedWithin grows on every side and clips to the image")
    void expandedWithinClips() {
        assertThat(new PixelRegion(10, 10, 20, 20).expandedWithin(0.5, 100, 100))
            .isEqualTo(new PixelRegion(0, 0, 40, 40));
        assertThat(new PixelRegion(90, 90, 10, 10).expandedWithin(0.5, 100, 100))
            .isEqualTo(new PixelRegion(85, 85, 15, 15));
    }

    @Test
    @DisplayName("rejects empty regions")
    void rejectsEmpty() {
        assertThatThrownBy(() -> new PixelRegion(0, 0, 0, 5)).isInstanceOf(IllegalArgumentException.class);
    }
}
