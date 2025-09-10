package com.springvision.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DetectionType}.
 */
public class DetectionTypeTest {

    @Test
    public void testFromCodeAndDefaults() {
        assertEquals(DetectionType.FACE, DetectionType.fromCode("face"));
        assertEquals(DetectionType.OBJECT, DetectionType.fromCode("object"));
        assertNull(DetectionType.fromCode(null));
        assertNull(DetectionType.fromCode("nonexistent"));

        DetectionType[] defaults = DetectionType.getDefaultSupportedTypes();
        assertTrue(defaults.length >= 1);
        boolean containsFace = false;
        for (DetectionType dt : defaults) {
            if (dt == DetectionType.FACE) {
                containsFace = true;
                break;
            }
        }
        assertTrue(containsFace);
    }

    @Test
    public void testToString() {
        assertEquals(DetectionType.FACE.getDisplayName(), DetectionType.FACE.toString());
    }
} 