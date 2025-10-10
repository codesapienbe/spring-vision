package io.github.codesapienbe.springvision.persistence.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectorUtilsTest {

    @Test
    void testSerializeDeserializeFloatArray() {
        float[] original = {0.1f, 0.2f, 0.3f, 0.4f};
        byte[] serialized = VectorUtils.serializeFloatArray(original);
        float[] deserialized = VectorUtils.deserializeFloatArray(serialized);
        assertArrayEquals(original, deserialized, 0.0001f);
    }

    @Test
    void testCosineSimilarity() {
        float[] vec1 = {1.0f, 0.0f, 0.0f};
        float[] vec2 = {0.0f, 1.0f, 0.0f};
        double similarity = VectorUtils.cosineSimilarity(vec1, vec2);
        assertEquals(0.0, similarity, 0.001);
    }
}
