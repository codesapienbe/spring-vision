# Spring Vision Examples

This directory contains example applications demonstrating the Spring Vision framework capabilities.

## Planned Examples

### 1. CLI-Based Application (TODO)

- **Location**: `cli-application/`
- **Description**: Command-line interface for image processing
- **Features**:
  - Batch processing from command line
  - Interactive mode for single image processing
  - Progress bars and real-time feedback
  - Multiple output formats (JSON, CSV, XML)
  - Configuration file support
  - Help and usage documentation

### 2. GWT-Based GUI Application (TODO)

- **Location**: `gwt-application/`
- **Description**: Web-based GUI using Google Web Toolkit
- **Features**:
  - Drag-and-drop image upload
  - Real-time image preview and detection visualization
  - Batch processing with progress indicators
  - Detection result overlay on images
  - Configuration panels for detection parameters
  - Export functionality for results
  - Responsive design for different screen sizes

### 3. Vaadin-Based GUI Application (TODO)

- **Location**: `vaadin-application/`
- **Description**: Modern web-based GUI using Vaadin Framework
- **Features**:
  - Advanced image upload with preview
  - Interactive detection result visualization
  - Real-time processing with WebSocket updates
  - Comprehensive configuration management
  - Batch processing with detailed progress tracking
  - Result export in multiple formats
  - User authentication and session management
  - Responsive design and mobile support

## Current Examples

### Basic Face Detection Example

- **Location**: `basic-face-detection/`
- **Description**: Simple Spring Boot application demonstrating face detection
- **Status**: ✅ Complete

### Batch Processing Example

- **Location**: `batch-processing-example/`
- **Description**: Example demonstrating batch processing capabilities
- **Status**: ✅ Complete

## Manual Testing

### Prerequisites

Before running any examples, ensure the core Spring Vision modules are built first:

```bash
# From the root directory
cd ..
mvn clean install

# Then build examples
cd spring-vision-examples
mvn clean install
```

### Running Examples Individually

Each example can be run independently. **Important**: Only run one example at a time as they all use port 8080 by default.

#### 1. Basic Face Detection Example

```bash
# Navigate to the example directory
cd basic-face-detection

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

**Access**: <http://localhost:8080>

- Main application page with image upload form
- Health check: <http://localhost:8080/actuator/health>
- API endpoint: <http://localhost:8080/api/vision/health>

**Testing**:

1. Open <http://localhost:8080> in your browser
2. Upload an image containing faces
3. Click "Detect Faces" to process the image
4. View detection results with confidence scores

**Stop**: Press `Ctrl+C` in the terminal

#### 2. Batch Processing Example

```bash
# Navigate to the example directory
cd batch-processing-example

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

**Access**: <http://localhost:8080>

- Console output showing batch processing examples
- Health check: <http://localhost:8080/actuator/health>
- API endpoint: <http://localhost:8080/api/vision/health>

**Testing**:

1. Watch the console output for batch processing demonstrations
2. Examples include: basic batch processing, progress monitoring, multiple detection types, batch cancellation, and error handling
3. Check the logs for processing statistics and results

**Stop**: Press `Ctrl+C` in the terminal

#### 3. GWT-Based GUI Example

```bash
# Navigate to the example directory
cd gwt-application

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

**Access**: <http://localhost:8080>

- Placeholder GWT application page
- Health check: <http://localhost:8080/actuator/health>
- API endpoint: <http://localhost:8080/api/vision/health>

**Testing**:

1. Open <http://localhost:8080> in your browser
2. Currently shows a placeholder page (GWT modules need to be implemented)
3. Verify the application starts successfully

**Stop**: Press `Ctrl+C` in the terminal

#### 4. Vaadin-Based GUI Example

```bash
# Navigate to the example directory
cd vaadin-application

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

**Access**: <http://localhost:8080>

- Basic Vaadin application with a clickable button
- Health check: <http://localhost:8080/actuator/health>
- API endpoint: <http://localhost:8080/api/vision/health>

**Testing**:

1. Open <http://localhost:8080> in your browser
2. Click the "Click me" button to test basic Vaadin functionality
3. Verify the application responds to user interaction

**Stop**: Press `Ctrl+C` in the terminal

### Troubleshooting

**Port Already in Use**: If you get a "port already in use" error:

```bash
# Find the process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID with the actual process ID)
taskkill /PID <PID> /F
```

**Build Errors**: If you encounter build errors:

1. Ensure you've built the core modules first
2. Check that all dependencies are resolved
3. Verify Java 21 is being used

**Application Won't Start**: If an application fails to start:

1. Check the console output for error messages
2. Verify all required dependencies are available
3. Check that no other application is using port 8080

### Build All Examples

To build all examples at once, run from the examples directory:

```bash
cd spring-vision-examples
mvn clean install
```

**Note**: Before building examples, ensure the core Spring Vision modules are built first:

```bash
# From the root directory
cd ..
mvn clean install

# Then build examples
cd spring-vision-examples
mvn clean install
```

### GWT-Based GUI Example

- **Location**: `gwt-application/`
- **Description**: Skeleton GWT-based GUI application using Spring Vision
- **Status**: 🔧 Scaffolded

### Vaadin-Based GUI Example

- **Location**: `vaadin-application/`
- **Description**: Skeleton Vaadin-based GUI application using Spring Vision
- **Status**: 🔧 Scaffolded

## Development Priority

1. **CLI Application** - Start here as it's the simplest to implement and test
2. **GWT Application** - Demonstrates traditional web application patterns
3. **Vaadin Application** - Shows modern web application capabilities

## Notes

- All examples will serve as both documentation and validation of the framework
- Examples will demonstrate different UI paradigms and use cases
- Focus on manual testing and validation before adding automated tests
- Each example should be self-contained and runnable independently
