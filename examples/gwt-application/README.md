# GWT-Based GUI Example

This example showcases a skeleton Google Web Toolkit (GWT) based GUI application integrating with the Spring Vision framework.

## Status

- **Location**: `gwt-application/`
- **Current state**: 🔧 Scaffolded — placeholder pages and basic endpoints are present.
- **Remaining work**:
  - Implement drag-and-drop image upload and client-side preview
  - Add detection result overlays and visualization
  - Implement batch processing with progress indicators

## Prerequisites

- Java 21 or later
- Maven 3.6 or later
- Spring Vision framework (available via Maven coordinates)

## Quick Start

```bash
# Navigate to the example directory
cd spring-vision-examples/gwt-application

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

## Access

- Main Application: http://localhost:8080
- Health Check: http://localhost:8080/actuator/health
- API Endpoint: http://localhost:8080/api/vision/health
