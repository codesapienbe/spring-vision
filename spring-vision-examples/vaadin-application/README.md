# Vaadin-Based GUI Example

This example showcases a skeleton Vaadin-based GUI application integrating with the Spring Vision framework.

## Status

- **Location**: `vaadin-application/`
- **Current state**: 🔧 Scaffolded — basic Vaadin app and build scaffolding are present.
- **Remaining work**:
  - Add advanced image upload with client-side preview
  - Implement interactive detection result visualization and overlays
  - Integrate WebSocket updates for realtime processing
  - Add export features and session management

## Prerequisites

- Java 21 or later
- Maven 3.6 or later
- Spring Vision framework (available via Maven coordinates)

## Quick Start

```bash
# Navigate to the example directory
cd spring-vision-examples/vaadin-application

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

## Access

- Main Application: http://localhost:8080
- Health Check: http://localhost:8080/actuator/health
- API Endpoint: http://localhost:8080/api/vision/health
