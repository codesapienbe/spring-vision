package io.github.codesapienbe.springvision.core.djl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for DJL auto-configuration.
 */
@SpringBootTest(classes = {DjlAutoConfiguration.class})
@TestPropertySource(properties = {
    "spring.vision.djl.enabled=true",
    "spring.vision.djl.engine=PyTorch",
    "spring.vision.djl.device=cpu"
})
class DjlAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void testDjlVisionBackendBeanCreated() {
        assertTrue(context.containsBean("djlVisionBackend"));

        DjlVisionBackend backend = context.getBean(DjlVisionBackend.class);
        assertNotNull(backend);
        assertEquals("djl", backend.getBackendId());
    }

    @Test
    void testDjlPropertiesBeanCreated() {
        DjlProperties properties = context.getBean(DjlProperties.class);
        assertNotNull(properties);
        assertTrue(properties.isEnabled());
        assertEquals("PyTorch", properties.getEngine());
    }

    @Test
    void testBackendMetadata() {
        DjlVisionBackend backend = context.getBean(DjlVisionBackend.class);

        assertEquals("djl", backend.getBackendId());
        assertEquals("DJL Vision Backend", backend.getDisplayName());
        assertNotNull(backend.getVersion());
        assertTrue(backend.getVersion().contains("DJL"));
    }

    @Test
    void testSupportedDetectionTypes() {
        DjlVisionBackend backend = context.getBean(DjlVisionBackend.class);

        assertNotNull(backend.getSupportedDetectionTypes());
        assertFalse(backend.getSupportedDetectionTypes().isEmpty());
    }
}
