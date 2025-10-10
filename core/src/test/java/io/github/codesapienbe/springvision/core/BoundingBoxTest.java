package io.github.codesapienbe.springvision.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BoundingBox}.
 */
public class BoundingBoxTest {

    @Test
    public void testValidConstructionAndProperties() {
        BoundingBox box = new BoundingBox(0.1, 0.2, 0.3, 0.4);

        assertEquals(0.1, box.x(), 1e-9);
        assertEquals(0.2, box.y(), 1e-9);
        assertEquals(0.3, box.width(), 1e-9);
        assertEquals(0.4, box.height(), 1e-9);

        assertEquals(0.4, box.getRight(), 1e-9);
        assertEquals(0.6, box.getBottom(), 1e-9);
        assertEquals(0.25, box.getCenterX(), 1e-9);
        assertEquals(0.4, box.getCenterY(), 1e-9);

        assertEquals(0.12, box.area(), 1e-9);
        assertTrue(box.contains(0.2, 0.3));
        assertFalse(box.contains(0.0, 0.0));
    }

    @Test
    public void testFromPixelsNormalization() {
        BoundingBox normalized = BoundingBox.fromPixels(100, 200, 300, 400, 1000, 1000);

        assertEquals(0.1, normalized.x(), 1e-9);
        assertEquals(0.2, normalized.y(), 1e-9);
        assertEquals(0.3, normalized.width(), 1e-9);
        assertEquals(0.4, normalized.height(), 1e-9);
    }

    @Test
    public void testFromAbsolutePixelsClamps() {
        BoundingBox clamped = BoundingBox.fromAbsolutePixels(1200, 1200, 2000, 2000);

        assertEquals(1.0, clamped.x(), 1e-9);
        assertEquals(1.0, clamped.y(), 1e-9);
        assertEquals(1.0, clamped.width(), 1e-9);
        assertEquals(1.0, clamped.height(), 1e-9);
    }

    @Test
    public void testToPixelsAndBack() {
        BoundingBox original = new BoundingBox(0.2, 0.2, 0.4, 0.4);
        BoundingBox roundTrip = original.toPixels(1000, 1000);

        // toPixels -> fromPixels should preserve normalized values for this scale
        assertEquals(original.x(), roundTrip.x(), 1e-9);
        assertEquals(original.y(), roundTrip.y(), 1e-9);
        assertEquals(original.width(), roundTrip.width(), 1e-9);
        assertEquals(original.height(), roundTrip.height(), 1e-9);
    }

    @Test
    public void testIntersectionAndIoU() {
        BoundingBox a = new BoundingBox(0.1, 0.1, 0.4, 0.4);
        BoundingBox b = new BoundingBox(0.3, 0.2, 0.4, 0.4);

        assertTrue(a.intersects(b));

        double intersection = a.intersectionArea(b);
        assertTrue(intersection > 0.0);

        double iou = a.intersectionOverUnion(b);
        assertTrue(iou > 0.0 && iou <= 1.0);
    }

    @Test
    public void testInvalidCoordinatesThrows() {
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(-0.1, 0.0, 0.1, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(0.0, -0.1, 0.1, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(0.0, 0.0, -0.1, 0.1));
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(0.0, 0.0, 0.1, -0.1));
        assertThrows(IllegalArgumentException.class, () -> BoundingBox.fromPixels(0, 0, 0, 0, 0, 0));
    }
}
