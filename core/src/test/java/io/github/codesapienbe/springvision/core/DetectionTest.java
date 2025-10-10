package io.github.codesapienbe.springvision.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Detection}.
 */
public class DetectionTest {

    @Test
    public void testFactoryAndAccessors() {
        BoundingBox box = new BoundingBox(0.1, 0.1, 0.2, 0.2);
        Detection d = Detection.of("face", 0.9, box);

        assertEquals("face", d.label());
        assertEquals(0.9, d.confidence(), 1e-9);
        assertSame(box, d.boundingBox());
        assertTrue(d.attributes().isEmpty());
        assertTrue(d.isHighConfidence());
        assertFalse(d.isMediumConfidence());
        assertFalse(d.isLowConfidence());

        assertEquals(box.area(), d.getArea(), 1e-9);
        double[] center = d.getCenter();
        assertEquals(0.2, center[0], 1e-9);
        assertEquals(0.2, center[1], 1e-9);
    }

    @Test
    public void testValidation() {
        BoundingBox box = new BoundingBox(0.0, 0.0, 0.1, 0.1);
        assertThrows(NullPointerException.class, () -> new Detection(null, 0.5, box, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new Detection("", 0.5, box, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new Detection("label", -0.1, box, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new Detection("label", 1.1, box, Map.of()));
    }

    @Test
    public void testAttributesAndHelpers() {
        BoundingBox box = new BoundingBox(0.0, 0.0, 0.1, 0.1);
        Detection d = new Detection("car", 0.6, box, Map.of("color", "red"));

        assertTrue(d.hasAttribute("color"));
        assertEquals("red", d.getAttribute("color"));
        assertEquals("unknown", d.getAttribute("missing", "unknown"));

        assertTrue(d.isMediumConfidence());
        assertFalse(d.isHighConfidence());
        assertFalse(d.isLowConfidence());
    }

    @Test
    public void testOverlapsAndMergeAttributes() {
        BoundingBox a = new BoundingBox(0.1, 0.1, 0.4, 0.4);
        BoundingBox b = new BoundingBox(0.3, 0.2, 0.4, 0.4);

        Detection da = new Detection("a", 0.5, a, Map.of("k1", "v1"));
        Detection db = new Detection("b", 0.6, b, Map.of("k2", "v2"));

        assertTrue(da.overlaps(db));
        assertTrue(da.intersectionOverUnion(db) > 0.0);

        Detection merged = da.withAttribute("extra", 123);
        assertNotNull(merged);
        assertTrue(merged instanceof Detection);
    }
}
