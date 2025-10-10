# Spring Vision Cyber Security Module

## Overview

The **Spring Vision Cyber Security** module adds security-focused capabilities on top of existing vision features. Rather than generic face or QR code detection, it performs security-specific analysis (threat intelligence, risk assessment, authentication decisions) using the standard ImageData and List<ImageData> data flow.

## Key Concept

This module does not replace face or barcode detection backends; it builds on them and enriches results with security context.

- Generic: "I detected a QR code"
- Cyber Security: "I detected a QR code that links to a phishing site (CRITICAL threat)"

Internally, it can leverage face detection and QR scanning but returns threat-aware detections with clear severity and metadata.

## Capabilities

The following capability interfaces are exposed in `spring-vision-core` and implemented by this module:

- **ThreatDetectionCapability** - `detectThreat(List<ImageData>)`
- **EavesdroppingDetectionCapability** - `detectEavesdropping(List<ImageData>)`
- **AccessAuthenticationCapability** - `authenticateAccess(ImageData)`

VisionTemplate offers convenience wrappers for these capabilities so you can call them through the same API you use for other modules.

## Getting Started

### 1. Add Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>spring-vision-cyber</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the Cyber Security module
spring.vision.cyber.enabled=true
# QR Code Security Settings
spring.vision.cyber.qr-code.sensitivity=0.7
spring.vision.cyber.qr-code.validate-urls=true
# Shoulder Surfing Detection Settings
spring.vision.cyber.shoulder-surfing.enabled=true
spring.vision.cyber.shoulder-surfing.sensitivity=0.8
spring.vision.cyber.shoulder-surfing.alert-threshold=3
# Physical Access Monitoring Settings
spring.vision.cyber.access-monitor.enabled=true
spring.vision.cyber.access-monitor.timeout-seconds=30
spring.vision.cyber.access-monitor.max-unauthorized-attempts=5
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/analyze-threats")
    public ResponseEntity<ThreatReport> analyzeThreat(
            @RequestParam("files") MultipartFile[] files) throws IOException {

        List<ImageData> images = Arrays.stream(files)
                .map(file -> {
                    try {
                        return ImageData.fromBytes(file.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<Detection> threats = visionTemplate.detectThreat(images);

        long critical = threats.stream()
                .filter(d -> "CRITICAL".equals(d.attributes().get("severity")))
                .count();

        return ResponseEntity.ok(new ThreatReport(threats, critical));
    }

    @PostMapping("/check-eavesdropping")
    public ResponseEntity<List<Detection>> checkEavesdropping(
            @RequestParam("frames") MultipartFile[] frames) throws IOException {

        List<ImageData> imageFrames = Arrays.stream(frames)
                .map(file -> {
                    try {
                        return ImageData.fromBytes(file.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<Detection> eavesdroppingEvents = visionTemplate.detectEavesdropping(imageFrames);

        return ResponseEntity.ok(eavesdroppingEvents);
    }
}
```

## Configuration Properties

| Property                                                       | Type    | Default | Description                          |
|----------------------------------------------------------------|---------|---------|--------------------------------------|
| `spring.vision.cyber.enabled`                                  | boolean | false   | Enable/disable the module            |
| `spring.vision.cyber.qr-code.sensitivity`                      | double  | 0.7     | QR code threat sensitivity (0.0-1.0) |
| `spring.vision.cyber.qr-code.validate-urls`                    | boolean | true    | Enable URL validation                |
| `spring.vision.cyber.shoulder-surfing.enabled`                 | boolean | true    | Enable shoulder surfing detection    |
| `spring.vision.cyber.shoulder-surfing.sensitivity`             | double  | 0.8     | Sensitivity threshold                |
| `spring.vision.cyber.shoulder-surfing.alert-threshold`         | int     | 3       | Alert after N detections             |
| `spring.vision.cyber.access-monitor.enabled`                   | boolean | true    | Enable access monitoring             |
| `spring.vision.cyber.access-monitor.timeout-seconds`           | int     | 30      | Access timeout                       |
| `spring.vision.cyber.access-monitor.max-unauthorized-attempts` | int     | 5       | Max failed attempts                  |

## Features

### Threat Detection

- Malicious QR code detection
- Phishing URL identification
- Security risk assessment
- Threat severity classification

### Eavesdropping Detection

- Shoulder surfing detection
- Unauthorized viewing attempts
- Privacy invasion monitoring
- Real-time alerts

### Access Authentication

- Face-based authentication
- Authorized access verification
- Unauthorized attempt logging
- Multi-factor support

## Manual Configuration (Optional)

If you need custom configuration, you can define beans manually:

```java

@Configuration
public class CustomSecurityConfig {

    @Bean
    public CyberSecurityBackend cyberSecurityBackend() {
        return new CyberSecurityBackend();
    }

    @Bean
    public VisionTemplate cyberVisionTemplate(CyberSecurityBackend backend) {
        return new VisionTemplate(backend);
    }
}
```

## Example: Complete Security Application

```java
@SpringBootApplication
public class SecurityApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/security")
class SecurityEndpoints {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @PostMapping("/scan-qr")
    public SecurityReport scanQRCode(@RequestParam("image") MultipartFile file) throws IOException {
        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> threats = visionTemplate.detectThreat(List.of(imageData));
        
        return new SecurityReport(
            threats.size(),
            threats.stream()
                .filter(d -> "CRITICAL".equals(d.attributes().get("severity")))
                .count(),
            threats
        );
    }
}
```

## Requirements

- Java 21+
- Spring Boot 3.2+
- ZXing library (for QR code scanning)

## License

See main project LICENSE file.

---

**Note**: This module requires proper security configuration and should be deployed following security best practices. Consult with security professionals when implementing in production environments.
