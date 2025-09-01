# Spring Vision MediaPipe Backend

This module provides MediaPipe backend implementation for the Spring Vision framework.

## Features

- Face detection using MediaPipe Face Detector
- Hand landmark detection using MediaPipe Hand Landmarker  
- Pose landmark detection using MediaPipe Pose Landmarker
- Automatic model downloading and caching
- Thread-safe object pooling for MediaPipe tasks

## Usage

Add this module as a dependency to your Spring Boot application:

```xml
<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-mediapipe</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The backend will be automatically discovered and available for use through the Spring Vision API.

## Configuration

Configure the MediaPipe backend using application properties:

```yaml
spring:
  vision:
    mediapipe:
      enabled: true
      model-path: ~/.spring-vision/models/mediapipe
      confidence-threshold: 0.7
      max-detections: 10
      enable-auto-download: true
```

## Requirements

- MediaPipe Java library (optional - loaded via reflection)
- Java 21+
- Spring Boot 3.2+ 