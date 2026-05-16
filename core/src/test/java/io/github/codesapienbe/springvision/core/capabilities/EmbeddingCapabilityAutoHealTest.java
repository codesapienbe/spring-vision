package io.github.codesapienbe.springvision.core.capabilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmbeddingCapabilityAutoHealTest {

    /** Default implementation delegates to isEmbeddingModelAvailable(). */
    @Test
    void defaultEnsureReturnsTrueWhenModelAvailable() {
        EmbeddingCapability available = new StubEmbeddingCapability(true);
        assertTrue(available.ensureEmbeddingModelLoaded());
    }

    @Test
    void defaultEnsureReturnsFalseWhenModelUnavailable() {
        EmbeddingCapability unavailable = new StubEmbeddingCapability(false);
        assertFalse(unavailable.ensureEmbeddingModelLoaded());
    }

    @Test
    void overriddenEnsureCanLoadModelOnDemand() {
        SelfHealingStub stub = new SelfHealingStub();
        assertFalse(stub.isEmbeddingModelAvailable(), "model should start unloaded");
        assertTrue(stub.ensureEmbeddingModelLoaded(), "should report loaded after heal");
        assertTrue(stub.isEmbeddingModelAvailable(), "model should be loaded now");
    }

    // --- stubs ---

    private static final class StubEmbeddingCapability implements EmbeddingCapability {
        private final boolean available;

        StubEmbeddingCapability(boolean available) {
            this.available = available;
        }

        @Override
        public java.util.List<float[]> extractEmbeddings(
                io.github.codesapienbe.springvision.core.ImageData imageData,
                io.github.codesapienbe.springvision.core.DetectionCategory subject) {
            return java.util.List.of();
        }

        @Override
        public boolean isEmbeddingModelAvailable() {
            return available;
        }
    }

    private static final class SelfHealingStub implements EmbeddingCapability {
        private boolean loaded = false;

        @Override
        public java.util.List<float[]> extractEmbeddings(
                io.github.codesapienbe.springvision.core.ImageData imageData,
                io.github.codesapienbe.springvision.core.DetectionCategory subject) {
            return java.util.List.of();
        }

        @Override
        public boolean isEmbeddingModelAvailable() {
            return loaded;
        }

        @Override
        public boolean ensureEmbeddingModelLoaded() {
            loaded = true; // simulate successful on-demand load
            return true;
        }
    }
}
