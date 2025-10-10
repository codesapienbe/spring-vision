package io.github.codesapienbe.springvision.cyber;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.cyber.detectors.QRCodeSecurityDetector;
import io.github.codesapienbe.springvision.cyber.detectors.PhysicalAccessMonitor;
import io.github.codesapienbe.springvision.cyber.models.AuthorizedPerson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CyberSecurityBackend.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
class CyberSecurityBackendTest {

    private CyberSecurityBackend backend;

    @BeforeEach
    void setUp() {
        backend = new CyberSecurityBackend();
    }

    @Test
    void testBackendInitialization() {
        assertNotNull(backend);
        assertEquals("cyber-security", backend.getBackendId());
        assertEquals("Cyber Security Backend", backend.getDisplayName());
        assertEquals("1.0.0", backend.getVersion());
    }

    @Test
    void testSupportedDetectionTypes() {
        Set<DetectionType> types = backend.getSupportedDetectionTypes();
        assertNotNull(types);
        assertTrue(types.contains(DetectionType.FACE));
        assertTrue(types.contains(DetectionType.BARCODE));
        assertTrue(types.contains(DetectionType.THREAT));
        assertTrue(types.contains(DetectionType.EAVESDROPPING));
        assertTrue(types.contains(DetectionType.ACCESS_AUTH));
        assertTrue(types.contains(DetectionType.SECURITY_INCIDENT));
    }

    @Test
    void testHealthCheck() {
        assertTrue(backend.isHealthy());
        assertNotNull(backend.getHealthInfo());
    }

    @Test
    void testQRCodeDetectorAvailable() {
        QRCodeSecurityDetector detector = backend.getQrCodeDetector();
        assertNotNull(detector);
    }

    @Test
    void testPhysicalAccessMonitorAvailable() {
        PhysicalAccessMonitor monitor = backend.getPhysicalAccessMonitor();
        assertNotNull(monitor);
    }

    @Test
    void testAuthorizedPersonRegistration() {
        PhysicalAccessMonitor monitor = backend.getPhysicalAccessMonitor();

        AuthorizedPerson person = new AuthorizedPerson("EMP001", "John Doe");
        person.setDepartment("Engineering");
        person.setAccessLevel("STANDARD");

        monitor.registerAuthorizedPerson(person);

        assertEquals(1, monitor.getAuthorizedPersonCount());
        assertTrue(monitor.isAuthorized("EMP001"));
    }

    @Test
    void testAccessRevocation() {
        PhysicalAccessMonitor monitor = backend.getPhysicalAccessMonitor();

        AuthorizedPerson person = new AuthorizedPerson("EMP002", "Jane Smith");
        monitor.registerAuthorizedPerson(person);
        assertTrue(monitor.isAuthorized("EMP002"));

        monitor.revokeAccess("EMP002");
        assertFalse(monitor.isAuthorized("EMP002"));
    }

    @Test
    void testDetectWithNullImageData() {
        List<Detection> detections = backend.detect(null, null);
        assertNotNull(detections);
        assertTrue(detections.isEmpty());
    }

    @Test
    void testQRCodeSensitivityConfiguration() {
        QRCodeSecurityDetector detector = backend.getQrCodeDetector();
        assertDoesNotThrow(() -> detector.setSensitivity(0.8));
    }
}

