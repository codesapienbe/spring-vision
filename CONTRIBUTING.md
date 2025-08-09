# Contributing to Spring-Vision

Thank you for your interest in contributing to Spring-Vision! This document provides guidelines and information for contributors.

## 🚀 Quick Start

1. **Fork** the repository
2. **Clone** your fork locally
3. **Create** a feature branch
4. **Make** your changes following our [coding rules](CURSOR_IDE_RULES.md)
5. **Test** your changes thoroughly
6. **Submit** a pull request

## 📋 Prerequisites

- **Java 21+**: The project requires Java 21 or later
- **Maven 3.8+**: For building and testing
- **Git**: For version control
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

## 🔧 Development Setup

### 1. Clone the Repository

```bash
git clone https://github.com/codesapienbe/spring-vision.git
cd spring-vision
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run Tests

```bash
mvn test
```

### 4. Format Code

```bash
mvn spotless:apply
```

### 5. Check Code Style

```bash
mvn checkstyle:check
```

## 📝 Coding Standards

**IMPORTANT**: All contributors must read and follow our comprehensive [Cursor IDE Rules](CURSOR_IDE_RULES.md) before submitting any code.

### Key Requirements

- **Security-first approach**: All code must address OWASP Top-10 vulnerabilities
- **Comprehensive Javadoc**: Every public class and method must be documented
- **90%+ test coverage**: All public methods must have unit tests
- **Structured logging**: Use proper logging levels and structured data
- **Modern Java**: Leverage Java 21+ features (records, pattern matching, virtual threads)

## 🧪 Testing Guidelines

### Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn jacoco:report
```

### Test Structure

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions
- **Security Tests**: Test input validation and security measures
- **Performance Tests**: Benchmark critical operations

### Test Naming Convention

```java
@Test
@DisplayName("Should detect faces successfully")
void shouldDetectFacesSuccessfully() {
    // Test implementation
}
```

## 🔒 Security Guidelines

### Input Validation

- Validate all inputs, especially image data
- Implement proper size and format restrictions
- Use secure error handling without information leakage

### Example Security Test

```java
@Test
@DisplayName("Should reject oversized images")
void shouldRejectOversizedImages() {
    byte[] oversizedImage = new byte[MAX_IMAGE_SIZE + 1];
    
    assertThatThrownBy(() -> visionTemplate.detectFaces(oversizedImage))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Image size exceeds maximum allowed size");
}
```

## 📊 Logging Standards

### Structured Logging

```java
logger.info("Processing image", Map.of(
    "imageSize", imageData.length,
    "operation", "face_detection",
    "correlationId", correlationId
));
```

### Log Levels

- **ERROR**: Actionable errors requiring immediate attention
- **WARN**: Concerning situations needing investigation
- **INFO**: Business events and important operations
- **DEBUG**: Technical details for troubleshooting

## 🔧 Configuration Guidelines

### Configuration Properties

- Use `@ConfigurationProperties` for externalized configuration
- Provide sensible defaults
- Include validation annotations
- Document all properties

### Example Configuration

```java
@ConfigurationProperties(prefix = "vision")
public record VisionProperties(
    @DefaultValue("opencv")
    String backend,
    
    @DefaultValue("0.8")
    @Min(0.0) @Max(1.0)
    double confidenceThreshold
) {
    // Validation logic
}
```

## 📋 Pull Request Process

### Before Submitting

1. **Read the rules**: Ensure you've read [Cursor IDE Rules](CURSOR_IDE_RULES.md)
2. **Test thoroughly**: Run all tests and ensure they pass
3. **Format code**: Apply Spotless formatting
4. **Check style**: Run Checkstyle validation
5. **Update docs**: Update documentation if needed

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] All tests pass

## Security
- [ ] Input validation implemented
- [ ] Error handling secure
- [ ] No sensitive data exposed

## Documentation
- [ ] Javadoc updated
- [ ] README updated if needed
- [ ] API documentation updated

## Checklist
- [ ] Code follows [Cursor IDE Rules](CURSOR_IDE_RULES.md)
- [ ] 90%+ test coverage maintained
- [ ] No TODO comments without issue references
- [ ] Dependencies properly versioned
```

## 🐛 Issue Reporting

### Bug Reports

When reporting bugs, please include:

- **Environment**: OS, Java version, Maven version
- **Steps to reproduce**: Clear, step-by-step instructions
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Logs**: Relevant error logs (sanitized)
- **Screenshots**: If applicable

### Feature Requests

When requesting features, please include:

- **Use case**: Why this feature is needed
- **Proposed solution**: How you envision it working
- **Alternatives considered**: Other approaches you've thought about
- **Impact**: How this affects existing functionality

## 🏷️ Commit Message Guidelines

### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, etc.)
- **refactor**: Code refactoring
- **test**: Adding or updating tests
- **chore**: Build process or auxiliary tool changes

### Examples

```
feat(core): add face detection support

Add comprehensive face detection capabilities using OpenCV backend.
Includes bounding box detection and confidence scoring.

Closes #123
```

```
fix(autoconfigure): resolve bean creation issue

Fix conditional bean creation when vision backend is disabled.

Fixes #456
```

## 🤝 Code Review Process

### Review Criteria

- **Functionality**: Does the code work as intended?
- **Security**: Are there any security vulnerabilities?
- **Performance**: Is the code efficient?
- **Maintainability**: Is the code readable and well-structured?
- **Testing**: Are there adequate tests?
- **Documentation**: Is the code properly documented?

### Review Process

1. **Automated checks** must pass (tests, style, security)
2. **At least one maintainer** must approve
3. **All conversations** must be resolved
4. **Documentation** must be updated if needed

## 📚 Additional Resources

- **[Cursor IDE Rules](CURSOR_IDE_RULES.md)**: Comprehensive coding standards
- **[API Documentation](docs/API_REFERENCE.md)**: API reference guide
- **[Architecture Guide](docs/ARCHITECTURE.md)**: System architecture overview
- **[Getting Started](docs/GETTING_STARTED.md)**: Quick start guide

## 🆘 Getting Help

- **GitHub Issues**: For bug reports and feature requests
- **GitHub Discussions**: For questions and general discussion
- **Code of Conduct**: Please read our [Code of Conduct](CODE_OF_CONDUCT.md)

## 🎯 Recognition

Contributors will be recognized in:

- **README.md**: For significant contributions
- **Release notes**: For each release
- **Contributors list**: On GitHub

---

Thank you for contributing to Spring-Vision! Your contributions help make computer vision accessible to every Spring Boot developer.
