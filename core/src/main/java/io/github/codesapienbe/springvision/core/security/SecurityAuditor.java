package io.github.codesapienbe.springvision.core.security;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.DetectionQuery;
import io.github.codesapienbe.springvision.core.logging.VisionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive security audit and penetration testing framework for the Spring Vision framework.
 *
 * <p>This class provides security vulnerability scanning, input validation testing,
 * SSRF protection verification, and comprehensive security reporting.</p>
 *
 * <p>The security auditor supports automated security testing, vulnerability assessment,
 * and security compliance reporting.</p>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
public class SecurityAuditor {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditor.class);

    // Security metrics
    private final AtomicLong securityEventCount = new AtomicLong(0);
    private final AtomicLong vulnerabilityCount = new AtomicLong(0);
    private final AtomicLong blockedRequests = new AtomicLong(0);

    // Security configuration
    private final Set<String> allowedHosts = new HashSet<>();
    private final Set<String> blockedHosts = new HashSet<>();
    private final Map<String, SecurityRule> securityRules = new ConcurrentHashMap<>();

    // Security audit results
    private final Map<String, SecurityVulnerability> vulnerabilities = new ConcurrentHashMap<>();
    private final Map<String, SecurityEvent> securityEvents = new ConcurrentHashMap<>();

    public SecurityAuditor() {
        initializeSecurityRules();
        initializeAllowedHosts();
    }

    /**
     * Runs comprehensive security audit.
     */
    public SecurityAuditReport runSecurityAudit() {
        logger.info("Starting comprehensive security audit");

        SecurityAuditReport report = new SecurityAuditReport();

        // Run various security tests
        report.addVulnerabilities(runInputValidationTests());
        report.addVulnerabilities(runSSRFTests());
        report.addVulnerabilities(runPathTraversalTests());
        report.addVulnerabilities(runInjectionTests());
        report.addVulnerabilities(runResourceExhaustionTests());
        report.addVulnerabilities(runAuthenticationTests());
        report.addVulnerabilities(runAuthorizationTests());
        report.addVulnerabilities(runDataExposureTests());

        // Generate security score
        report.calculateSecurityScore();

        logger.info("Security audit completed: {} vulnerabilities found", report.getVulnerabilityCount());
        return report;
    }

    /**
     * Validates input for security vulnerabilities.
     */
    public SecurityValidationResult validateInput(ImageData imageData, DetectionQuery query) {
        SecurityValidationResult result = new SecurityValidationResult();

        // Validate image data
        if (imageData == null) {
            result.addVulnerability("NULL_IMAGE_DATA", "Image data is null", SecuritySeverity.HIGH);
            return result;
        }

        // Check image size
        if (imageData.data().length > 50 * 1024 * 1024) { // 50MB limit
            result.addVulnerability("LARGE_IMAGE_SIZE", "Image size exceeds limit", SecuritySeverity.MEDIUM);
        }

        // Check for malicious file signatures
        if (containsMaliciousSignature(imageData.data())) {
            result.addVulnerability("MALICIOUS_FILE_SIGNATURE", "File contains malicious signature", SecuritySeverity.CRITICAL);
            blockedRequests.incrementAndGet();
        }

        // Validate query parameters
        if (query != null) {
            validateQueryParameters(query, result);
        }

        // Log security event
        if (!result.getVulnerabilities().isEmpty()) {
            Map<String, Object> details = Map.of("vulnerability_count", result.getVulnerabilities().size());
            logSecurityEvent("INPUT_VALIDATION", "Input validation failed", details);
        }

        return result;
    }

    /**
     * Validates URL for SSRF protection.
     */
    public boolean validateUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();

            // Check against allowed/blocked hosts
            if (blockedHosts.contains(host)) {
                logSecurityEvent("SSRF_BLOCKED", "SSRF attempt blocked", Map.of("host", host));
                blockedRequests.incrementAndGet();
                return false;
            }

            if (!allowedHosts.isEmpty() && !allowedHosts.contains(host)) {
                logSecurityEvent("SSRF_BLOCKED", "SSRF attempt blocked - host not in allowed list", Map.of("host", host));
                blockedRequests.incrementAndGet();
                return false;
            }

            // Check for private IP ranges
            if (isPrivateIpAddress(host)) {
                logSecurityEvent("SSRF_BLOCKED", "SSRF attempt blocked - private IP address", Map.of("host", host));
                blockedRequests.incrementAndGet();
                return false;
            }

            return true;

        } catch (Exception e) {
            logSecurityEvent("SSRF_BLOCKED", "SSRF attempt blocked - invalid URL", Map.of("url", url, "error", e.getMessage()));
            blockedRequests.incrementAndGet();
            return false;
        }
    }

    /**
     * Validates file path for path traversal attacks.
     */
    public boolean validateFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        // Check for path traversal patterns
        String[] suspiciousPatterns = {
            "..", "~", "\\", "//", "\\\\", "..\\", "../", "~\\", "~/"
        };

        for (String pattern : suspiciousPatterns) {
            if (filePath.contains(pattern)) {
                logSecurityEvent("PATH_TRAVERSAL_BLOCKED", "Path traversal attempt blocked", Map.of("path", filePath, "pattern", pattern));
                blockedRequests.incrementAndGet();
                return false;
            }
        }

        // Check for absolute paths
        Path path = Path.of(filePath);
        if (path.isAbsolute()) {
            logSecurityEvent("PATH_TRAVERSAL_BLOCKED", "Absolute path blocked", Map.of("path", filePath));
            blockedRequests.incrementAndGet();
            return false;
        }

        return true;
    }

    /**
     * Validates model file for integrity.
     */
    public boolean validateModelFile(Path modelPath, String expectedChecksum) {
        try {
            if (!Files.exists(modelPath)) {
                return false;
            }

            byte[] fileData = Files.readAllBytes(modelPath);
            String actualChecksum = calculateChecksum(fileData);

            if (!expectedChecksum.equals(actualChecksum)) {
                logSecurityEvent("MODEL_INTEGRITY_FAILED", "Model file integrity check failed",
                    Map.of("path", modelPath.toString(), "expected", expectedChecksum, "actual", actualChecksum));
                return false;
            }

            return true;

        } catch (IOException e) {
            logSecurityEvent("MODEL_VALIDATION_ERROR", "Model file validation error",
                Map.of("path", modelPath.toString(), "error", e.getMessage()));
            return false;
        }
    }

    /**
     * Runs input validation security tests.
     */
    private List<SecurityVulnerability> runInputValidationTests() {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        // Test null input handling
        try {
            validateInput(null, null);
        } catch (Exception e) {
            vulnerabilities.add(new SecurityVulnerability("NULL_INPUT_HANDLING",
                "Null input not properly handled", SecuritySeverity.HIGH, e.getMessage()));
        }

        // Test oversized input
        byte[] oversizedData = new byte[100 * 1024 * 1024]; // 100MB
        ImageData oversizedImage = new ImageData(oversizedData, "image/jpeg", oversizedData.length, "jpeg");
        SecurityValidationResult result = validateInput(oversizedImage, null);

        if (result.getVulnerabilities().isEmpty()) {
            vulnerabilities.add(new SecurityVulnerability("OVERSIZED_INPUT_VALIDATION",
                "Oversized input not properly validated", SecuritySeverity.MEDIUM, "No validation for oversized input"));
        }

        // Test malicious file signatures
        byte[] maliciousData = createMaliciousTestData();
        ImageData maliciousImage = new ImageData(maliciousData, "image/jpeg", maliciousData.length, "jpeg");
        result = validateInput(maliciousImage, null);

        if (result.getVulnerabilities().isEmpty()) {
            vulnerabilities.add(new SecurityVulnerability("MALICIOUS_FILE_VALIDATION",
                "Malicious file signatures not detected", SecuritySeverity.CRITICAL, "No detection for malicious files"));
        }

        return vulnerabilities;
    }

    /**
     * Runs SSRF security tests.
     */
    private List<SecurityVulnerability> runSSRFTests() {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        // Test private IP addresses
        String[] privateIps = {"192.168.1.1", "10.0.0.1", "172.16.0.1", "127.0.0.1", "localhost"};

        for (String ip : privateIps) {
            if (validateUrl("http://" + ip + "/malicious")) {
                vulnerabilities.add(new SecurityVulnerability("SSRF_PRIVATE_IP",
                    "Private IP address not blocked", SecuritySeverity.HIGH, "IP: " + ip));
            }
        }

        // Test blocked hosts
        for (String host : blockedHosts) {
            if (validateUrl("http://" + host + "/malicious")) {
                vulnerabilities.add(new SecurityVulnerability("SSRF_BLOCKED_HOST",
                    "Blocked host not properly blocked", SecuritySeverity.HIGH, "Host: " + host));
            }
        }

        return vulnerabilities;
    }

    /**
     * Runs path traversal security tests.
     */
    private List<SecurityVulnerability> runPathTraversalTests() {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        String[] maliciousPaths = {
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "~/../../etc/shadow",
            "..//..//..//etc//passwd"
        };

        for (String path : maliciousPaths) {
            if (validateFilePath(path)) {
                vulnerabilities.add(new SecurityVulnerability("PATH_TRAVERSAL",
                    "Path traversal not blocked", SecuritySeverity.CRITICAL, "Path: " + path));
            }
        }

        return vulnerabilities;
    }

    /**
     * Runs injection security tests.
     */
    private List<SecurityVulnerability> runInjectionTests() {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        // Test SQL injection patterns in query parameters
        String[] sqlInjectionPatterns = {
            "'; DROP TABLE users; --",
            "' OR '1'='1",
            "'; INSERT INTO users VALUES ('hacker', 'password'); --"
        };

        for (String pattern : sqlInjectionPatterns) {
            // This would be tested against actual database queries
            vulnerabilities.add(new SecurityVulnerability("SQL_INJECTION_TEST",
                "SQL injection patterns should be tested", SecuritySeverity.HIGH, "Pattern: " + pattern));
        }

        return vulnerabilities;
    }

    /**
     * Runs resource exhaustion security tests.
     */
    private List<SecurityVulnerability> runResourceExhaustionTests() {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        // Test memory exhaustion
        try {
            byte[] largeData = new byte[200 * 1024 * 1024]; // 200MB
            ImageData largeImage = new ImageData(largeData, "image/jpeg", largeData.length, "jpeg");
            validateInput(largeImage, null);
        } catch (OutOfMemoryError e) {
            vulnerabilities.add(new SecurityVulnerability("MEMORY_EXHAUSTION",
                "Memory exhaustion not properly handled", SecuritySeverity.HIGH, e.getMessage()));
        }

        return vulnerabilities;
    }

    /**
     * Runs authentication security tests.
     */
    private List<SecurityVulnerability> runAuthenticationTests() {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        // Test weak authentication
        vulnerabilities.add(new SecurityVulnerability("AUTHENTICATION_REQUIRED",
            "Authentication mechanism should be implemented", SecuritySeverity.MEDIUM, "No authentication implemented"));

        return vulnerabilities;
    }

    /**
     * Runs authorization security tests.
     */
    private List<SecurityVulnerability> runAuthorizationTests() {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        // Test access control
        vulnerabilities.add(new SecurityVulnerability("AUTHORIZATION_REQUIRED",
            "Authorization mechanism should be implemented", SecuritySeverity.MEDIUM, "No authorization implemented"));

        return vulnerabilities;
    }

    /**
     * Runs data exposure security tests.
     */
    private List<SecurityVulnerability> runDataExposureTests() {
        List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        // Test sensitive data exposure
        vulnerabilities.add(new SecurityVulnerability("SENSITIVE_DATA_PROTECTION",
            "Sensitive data protection should be implemented", SecuritySeverity.MEDIUM, "No data protection implemented"));

        return vulnerabilities;
    }

    /**
     * Validates query parameters for security issues.
     */
    private void validateQueryParameters(DetectionQuery query, SecurityValidationResult result) {
        // Check confidence threshold
        if (query.getMinConfidence() < 0.0 || query.getMinConfidence() > 1.0) {
            result.addVulnerability("INVALID_CONFIDENCE_THRESHOLD",
                "Confidence threshold out of valid range", SecuritySeverity.MEDIUM);
        }

        // Check max detections
        if (query.getMaxDetections() <= 0 || query.getMaxDetections() > 1000) {
            result.addVulnerability("INVALID_MAX_DETECTIONS",
                "Max detections out of valid range", SecuritySeverity.MEDIUM);
        }

        // Note: NMS threshold is not available in DetectionQuery, so we skip this check
    }

    /**
     * Checks if data contains malicious signatures.
     */
    private boolean containsMaliciousSignature(byte[] data) {
        if (data.length < 4) {
            return false;
        }

        // Check for executable file signatures
        byte[] exeSignature = {0x4D, 0x5A}; // MZ header
        byte[] elfSignature = {0x7F, 0x45, 0x4C, 0x46}; // ELF header

        for (int i = 0; i <= data.length - 2; i++) {
            if (data[i] == exeSignature[0] && data[i + 1] == exeSignature[1]) {
                return true;
            }
        }

        for (int i = 0; i <= data.length - 4; i++) {
            if (data[i] == elfSignature[0] && data[i + 1] == elfSignature[1] &&
                data[i + 2] == elfSignature[2] && data[i + 3] == elfSignature[3]) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if host is a private IP address.
     */
    private boolean isPrivateIpAddress(String host) {
        if (host == null) {
            return false;
        }

        // Check for localhost
        if (host.equals("localhost") || host.equals("127.0.0.1")) {
            return true;
        }

        // Check for private IP ranges
        String[] privateRanges = {
            "192.168.", "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31."
        };

        for (String range : privateRanges) {
            if (host.startsWith(range)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates SHA-256 checksum of data.
     */
    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "sha256:" + hexString.toString();

        } catch (Exception e) {
            logger.error("Failed to calculate checksum", e);
            return "";
        }
    }

    /**
     * Creates malicious test data.
     */
    private byte[] createMaliciousTestData() {
        // Create data with executable signature
        byte[] data = new byte[1024];
        data[0] = 0x4D; // M
        data[1] = 0x5A; // Z
        return data;
    }

    /**
     * Logs security event.
     */
    private void logSecurityEvent(String eventType, String message, Map<String, Object> details) {
        SecurityEvent event = new SecurityEvent(eventType, message, details, System.currentTimeMillis());
        securityEvents.put(event.getId(), event);

        VisionLogger.logSecurityEvent("security-auditor", eventType, message, details);
    }

    /**
     * Initializes security rules.
     */
    private void initializeSecurityRules() {
        securityRules.put("MAX_IMAGE_SIZE", new SecurityRule("MAX_IMAGE_SIZE", 50 * 1024 * 1024L));
        securityRules.put("MAX_DETECTIONS", new SecurityRule("MAX_DETECTIONS", 1000));
        securityRules.put("MAX_CONFIDENCE", new SecurityRule("MAX_CONFIDENCE", 1.0));
        securityRules.put("MIN_CONFIDENCE", new SecurityRule("MIN_CONFIDENCE", 0.0));
    }

    /**
     * Initializes allowed hosts.
     */
    private void initializeAllowedHosts() {
        // Add common trusted hosts
        allowedHosts.add("storage.googleapis.com");
        allowedHosts.add("github.com");
        allowedHosts.add("githubusercontent.com");

        // Add blocked hosts
        blockedHosts.add("malicious-site.com");
        blockedHosts.add("evil-domain.org");
    }

    /**
     * Gets security metrics.
     */
    public Map<String, Object> getSecurityMetrics() {
        return Map.of(
            "security_events", securityEventCount.get(),
            "vulnerability_count", vulnerabilityCount.get(),
            "blocked_requests", blockedRequests.get(),
            "vulnerabilities", vulnerabilities.size(),
            "security_events_map_size", securityEvents.size()
        );
    }

    /**
     * Security validation result data class.
     */
    public static class SecurityValidationResult {
        private final List<SecurityVulnerability> vulnerabilities = new ArrayList<>();

        public void addVulnerability(String type, String description, SecuritySeverity severity) {
            vulnerabilities.add(new SecurityVulnerability(type, description, severity, null));
        }

        public void addVulnerability(String type, String description, SecuritySeverity severity, String details) {
            vulnerabilities.add(new SecurityVulnerability(type, description, severity, details));
        }

        public List<SecurityVulnerability> getVulnerabilities() {
            return vulnerabilities;
        }

        public boolean isValid() {
            return vulnerabilities.isEmpty();
        }
    }

    /**
     * Security vulnerability data class.
     */
    public static class SecurityVulnerability {
        private final String type;
        private final String description;
        private final SecuritySeverity severity;
        private final String details;
        private final long timestamp;

        public SecurityVulnerability(String type, String description, SecuritySeverity severity, String details) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.details = details;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public SecuritySeverity getSeverity() {
            return severity;
        }

        public String getDetails() {
            return details;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Security event data class.
     */
    public static class SecurityEvent {
        private final String id;
        private final String eventType;
        private final String message;
        private final Map<String, Object> details;
        private final long timestamp;

        public SecurityEvent(String eventType, String message, Map<String, Object> details, long timestamp) {
            this.id = UUID.randomUUID().toString();
            this.eventType = eventType;
            this.message = message;
            this.details = details;
            this.timestamp = timestamp;
        }

        // Getters
        public String getId() {
            return id;
        }

        public String getEventType() {
            return eventType;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Security rule data class.
     */
    public static class SecurityRule {
        private final String name;
        private final Object value;

        public SecurityRule(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        // Getters
        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }
    }

    /**
     * Security severity enum.
     */
    public enum SecuritySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Security audit report data class.
     */
    public static class SecurityAuditReport {
        private final List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
        private double securityScore = 0.0;

        public void addVulnerabilities(List<SecurityVulnerability> newVulnerabilities) {
            vulnerabilities.addAll(newVulnerabilities);
        }

        public void calculateSecurityScore() {
            if (vulnerabilities.isEmpty()) {
                securityScore = 100.0;
                return;
            }

            double totalScore = 100.0;
            for (SecurityVulnerability vulnerability : vulnerabilities) {
                switch (vulnerability.getSeverity()) {
                    case CRITICAL -> totalScore -= 25.0;
                    case HIGH -> totalScore -= 15.0;
                    case MEDIUM -> totalScore -= 10.0;
                    case LOW -> totalScore -= 5.0;
                }
            }

            securityScore = Math.max(0.0, totalScore);
        }

        public int getVulnerabilityCount() {
            return vulnerabilities.size();
        }

        public double getSecurityScore() {
            return securityScore;
        }

        public List<SecurityVulnerability> getVulnerabilities() {
            return vulnerabilities;
        }

        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Security Audit Report\n");
            summary.append("====================\n");
            summary.append(String.format("Security Score: %.1f/100\n", securityScore));
            summary.append(String.format("Vulnerabilities Found: %d\n", vulnerabilities.size()));

            if (!vulnerabilities.isEmpty()) {
                summary.append("\nVulnerabilities:\n");
                for (SecurityVulnerability vulnerability : vulnerabilities) {
                    summary.append(String.format("  [%s] %s: %s\n",
                        vulnerability.getSeverity(), vulnerability.getType(), vulnerability.getDescription()));
                }
            }

            return summary.toString();
        }
    }
}
