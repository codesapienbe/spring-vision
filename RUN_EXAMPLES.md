# Running Spring Vision Examples

This document explains how to run the Spring Vision examples using the provided `run.sh` script.

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Bash shell (Linux, macOS, or Git Bash on Windows)

## Quick Start

Make the script executable (if not already):
```bash
chmod +x run.sh
```

## Available Commands

### List Examples
```bash
./run.sh list
```

### Run Examples
```bash
./run.sh example basic    # Basic Face Detection Example
./run.sh example batch    # Batch Processing Example
./run.sh example gwt      # GWT Application Example
./run.sh example vaadin   # Vaadin Application Example
```

### Build All Examples
```bash
./run.sh build
```

### Clean All Examples
```bash
./run.sh clean
```

### Show Help
```bash
./run.sh help
```

## Example Details

### 1. Basic Face Detection Example
- **Command**: `./run.sh example basic`
- **Description**: Simple web application for face detection
- **Features**: Web interface with file upload, real-time face detection
- **URL**: http://localhost:8080
- **Use Case**: Upload images and detect faces using OpenCV

### 2. Batch Processing Example
- **Command**: `./run.sh example batch`
- **Description**: Demonstrates batch processing capabilities
- **Features**: Command-line application, processes multiple images
- **Use Case**: Process large numbers of images efficiently

### 3. GWT Application Example
- **Command**: `./run.sh example gwt`
- **Description**: GWT-based GUI application
- **Features**: Web-based interface using Google Web Toolkit
- **URL**: http://localhost:8080
- **Use Case**: Rich web interface for vision operations

### 4. Vaadin Application Example
- **Command**: `./run.sh example vaadin`
- **Description**: Vaadin-based GUI application
- **Features**: Modern web interface using Vaadin
- **URL**: http://localhost:8080
- **Use Case**: Modern, responsive web interface

## What the Script Does

The `run.sh` script performs the following steps:

1. **Prerequisites Check**: Verifies Java 21+ and Maven are installed
2. **Core Module Build**: Builds the core Spring Vision modules if needed
3. **Example Execution**: Changes to the example directory and runs `mvn clean package spring-boot:run`
4. **Error Handling**: Provides clear error messages and colored output

## Troubleshooting

### Permission Denied
```bash
chmod +x run.sh
```

### Java Version Issues
Ensure you have Java 21 or higher:
```bash
java -version
```

### Maven Not Found
Install Maven and ensure it's in your PATH:
```bash
mvn -version
```

### Port Already in Use
If port 8080 is already in use, stop the running application first:
```bash
# Find and kill the process using port 8080
lsof -ti:8080 | xargs kill -9
```

## Manual Execution

If you prefer to run examples manually:

```bash
# Build core modules first
mvn clean install -DskipTests

# Run specific example
cd spring-vision-examples/basic-face-detection
mvn clean package spring-boot:run
```

## Development

The script is located in the project root directory and supports:
- Colored output for better readability
- Comprehensive error handling
- Automatic prerequisite checking
- Flexible command structure

For script modifications, see the comments in `run.sh` for detailed explanations of each function. 
