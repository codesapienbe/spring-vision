---
name: Bug report
about: Create a report to help us improve
title: '[BUG] '
labels: ['bug', 'needs-triage']
assignees: ''
---

## 🐛 Bug Description

A clear and concise description of what the bug is.

## 🔄 Steps to Reproduce

1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

## ✅ Expected Behavior

A clear and concise description of what you expected to happen.

## ❌ Actual Behavior

A clear and concise description of what actually happened.

## 📸 Screenshots

If applicable, add screenshots to help explain your problem.

## 🖥️ Environment

**OS:** [e.g. Windows 10, macOS 12.0, Ubuntu 22.04]
**Java Version:** [e.g. OpenJDK 21.0.1]
**Maven Version:** [e.g. 3.9.5]
**Spring Boot Version:** [e.g. 3.2.0]
**Spring Vision Version:** [e.g. 1.0.0-SNAPSHOT]

## 📋 Configuration

Please share your relevant configuration:

```yaml
vision:
  backend: opencv
  confidence-threshold: 0.8
  max-image-size: 10485760
```

## 📊 Logs

Please provide relevant logs (sanitized of sensitive information):

```
[ERROR] 2024-01-15 10:30:45.123 - Failed to process image
java.lang.IllegalArgumentException: Image data must not be null
    at com.springvision.template.VisionTemplate.detectFaces(VisionTemplate.java:45)
```

## 🔍 Additional Context

Add any other context about the problem here.

## 🧪 Reproduction Code

If applicable, provide a minimal example to reproduce the issue:

```java
@SpringBootTest
class VisionTemplateTest {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Test
    void testBug() {
        byte[] imageData = loadTestImage("test.jpg");
        VisionResult result = visionTemplate.detectFaces(imageData);
        // This fails with the reported error
    }
}
```

## 📋 Checklist

- [ ] I have searched existing issues to avoid duplicates
- [ ] I have provided all required information
- [ ] I have included relevant logs (sanitized)
- [ ] I have tested with the latest version
- [ ] I have provided a minimal reproduction example

## 🔗 Related Issues

Link to any related issues here.

---

**Note:** Please ensure all logs and configuration are sanitized of any sensitive information before posting. 
