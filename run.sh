#!/bin/bash

# Spring Vision Examples Runner Script
# Usage: ./run.sh example <example-name>
# Examples: ./run.sh example basic
#          ./run.sh example batch
#          ./run.sh example gwt
#          ./run.sh example vaadin
#          ./run.sh example picocli
#          ./run.sh example javafx

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Spring Vision Examples Runner"
    echo ""
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  example <name>    Run a specific example"
    echo "  list              List all available examples"
    echo "  build             Build all examples"
    echo "  clean             Clean all examples"
    echo "  test-opencv       Test OpenCV functionality"
    echo ""
    echo "Available examples:"
    echo "  basic             Basic Face Detection Example"
    echo "  batch             Batch Processing Example"
    echo "  gwt               GWT Application Example"
    echo "  vaadin            Vaadin Application Example"
    echo "  picocli           PicoCLI Command-Line Application"
    echo "  javafx            JavaFX Desktop Application"
    echo ""
    echo "Examples:"
    echo "  $0 example basic"
    echo "  $0 example batch"
    echo "  $0 example gwt"
    echo "  $0 example vaadin"
    echo "  $0 example picocli"
    echo "  $0 example javafx"
    echo "  $0 list"
    echo "  $0 build"
    echo "  $0 clean"
    echo "  $0 test-opencv"
}

# Function to check if Maven is available
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed or not in PATH"
        print_info "Please install Maven and ensure it's available in your PATH"
        exit 1
    fi
}

# Function to check if Java is available
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed or not in PATH"
        print_info "Please install Java and ensure it's available in your PATH"
        exit 1
    fi

    # Check Java version (requires Java 21+)
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_error "Java 21 or higher is required. Found Java $JAVA_VERSION"
        print_info "Please upgrade to Java 21 or higher"
        exit 1
    fi
}

# Function to build core modules
build_core() {
    print_info "Building core Spring Vision modules..."
    cd "$PROJECT_ROOT"
    mvn clean install -DskipTests
    print_success "Core modules built successfully"
}

# Function to list examples
list_examples() {
    echo "Available Spring Vision Examples:"
    echo ""
    echo "1. basic-face-detection"
    echo "   - Simple web application for face detection"
    echo "   - Web interface with file upload"
    echo "   - Runs on http://localhost:8080"
    echo ""
    echo "2. batch-processing-example"
    echo "   - Demonstrates batch processing capabilities"
    echo "   - Command-line application"
    echo "   - Processes multiple images in batch"
    echo ""
    echo "3. gwt-application"
    echo "   - GWT-based GUI application"
    echo "   - Web-based interface using Google Web Toolkit"
    echo "   - Runs on http://localhost:8080"
    echo ""
    echo "4. vaadin-application"
    echo "   - Vaadin-based GUI application"
    echo "   - Modern web interface using Vaadin"
    echo "   - Runs on http://localhost:8080"
    echo ""
    echo "5. picocli-application"
    echo "   - Command-line interface application"
    echo "   - Uses PicoCLI framework for CLI operations"
    echo "   - Supports multiple output formats (text, JSON, CSV)"
    echo "   - File-based processing with batch capabilities"
    echo ""
    echo "6. javafx-application"
    echo "   - Desktop GUI application using JavaFX"
    echo "   - Modern desktop interface with drag-and-drop"
    echo "   - Visual result display with bounding boxes"
    echo "   - Asynchronous processing with progress indicators"
    echo ""
    echo "Usage: $0 example <name>"
    echo "Example: $0 example basic"
}

# Function to run a specific example
run_example() {
    local example_name=$1

    # Map short names to full directory names
    case $example_name in
        "basic")
            EXAMPLE_DIR="basic-face-detection"
            EXAMPLE_DESC="Basic Face Detection Example"
            ;;
        "batch")
            EXAMPLE_DIR="batch-processing-example"
            EXAMPLE_DESC="Batch Processing Example"
            ;;
        "gwt")
            EXAMPLE_DIR="gwt-application"
            EXAMPLE_DESC="GWT Application Example"
            ;;
        "vaadin")
            EXAMPLE_DIR="vaadin-application"
            EXAMPLE_DESC="Vaadin Application Example"
            ;;
        "picocli")
            EXAMPLE_DIR="picocli-application"
            EXAMPLE_DESC="PicoCLI Command-Line Application"
            ;;
        "javafx")
            EXAMPLE_DIR="javafx-application"
            EXAMPLE_DESC="JavaFX Desktop Application"
            ;;
        *)
            print_error "Unknown example: $example_name"
            print_info "Use '$0 list' to see available examples"
            exit 1
            ;;
    esac

    EXAMPLE_PATH="$PROJECT_ROOT/spring-vision-examples/$EXAMPLE_DIR"

    # Check if example directory exists
    if [ ! -d "$EXAMPLE_PATH" ]; then
        print_error "Example directory not found: $EXAMPLE_PATH"
        print_info "Make sure you're running this script from the project root directory"
        exit 1
    fi

    print_info "Running $EXAMPLE_DESC..."
    print_info "Directory: $EXAMPLE_PATH"

    # Step 1: Build the parent project first
    print_info "Step 1: Building parent project..."
    cd "$PROJECT_ROOT"
    mvn clean install -DskipTests
    print_success "Parent project built successfully"

    # Step 2: Wait for 1 second
    print_info "Step 2: Waiting 1 second..."
    sleep 1

    # Step 3: Build and run the specific example
    print_info "Step 3: Building and running $EXAMPLE_DESC..."
    cd "$EXAMPLE_PATH"

    # Check if pom.xml exists
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found in $EXAMPLE_PATH"
        exit 1
    fi

    # Special handling for different application types
    case $example_name in
        "picocli")
            # For PicoCLI, build the JAR and show usage instructions
            print_info "Building PicoCLI application..."
            mvn clean package -DskipTests
            print_success "PicoCLI application built successfully"
            print_info ""
            print_info "PicoCLI Application Usage:"
            print_info "=========================="
            print_info ""
            print_info "1. Show help:"
            print_info "   java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --help"
            print_info ""
            print_info "2. Detect faces in a single image:"
            print_info "   java -jar target/picocli-application-1.0.0-SNAPSHOT.jar detect /path/to/image.jpg"
            print_info ""
            print_info "3. Detect faces with JSON output:"
            print_info "   java -jar target/picocli-application-1.0.0-SNAPSHOT.jar detect /path/to/image.jpg -o json"
            print_info ""
            print_info "4. Check health status:"
            print_info "   java -jar target/picocli-application-1.0.0-SNAPSHOT.jar health"
            print_info ""
            print_info "Note: Replace /path/to/image.jpg with the actual path to your image file."
            print_info "Supported formats: JPG, JPEG, PNG, BMP, TIFF"
            ;;
        "javafx")
            # For JavaFX, build and run with proper module path
            print_info "Building JavaFX application..."
            mvn clean package -DskipTests
            print_success "JavaFX application built successfully"
            print_info ""
            print_info "Launching JavaFX application..."
            print_info "Note: JavaFX requires proper module configuration"
            print_info ""

            # Try to run with Maven first
            if mvn javafx:run 2>/dev/null; then
                print_success "JavaFX application launched successfully"
            else
                print_warning "Maven JavaFX plugin not available, trying direct Java execution..."
                # Fallback to direct Java execution
                java --module-path "$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
                     --add-modules javafx.controls,javafx.fxml,javafx.graphics \
                     -jar target/javafx-application-1.0.0-SNAPSHOT.jar
            fi
            ;;
        *)
            # Default behavior for web applications
            mvn clean package spring-boot:run
            ;;
    esac
}

# Function to build all examples
build_all_examples() {
    print_info "Building all Spring Vision examples..."

    # Build core modules first
    build_core

    # Build examples
    cd "$PROJECT_ROOT/spring-vision-examples"
    mvn clean install -DskipTests

    print_success "All examples built successfully"
}

# Function to clean all examples
clean_all_examples() {
    print_info "Cleaning all Spring Vision examples..."

    # Clean core modules
    cd "$PROJECT_ROOT"
    mvn clean

    # Clean examples
    cd "$PROJECT_ROOT/spring-vision-examples"
    mvn clean

    print_success "All examples cleaned successfully"
}

# Function to test OpenCV functionality
test_opencv_functionality() {
    print_info "Testing OpenCV functionality with embedded libraries..."

    # Build core modules first
    build_core

    # Run OpenCV test
    cd "$PROJECT_ROOT/spring-vision-core"
    print_info "Running OpenCV functionality test..."

    # Compile and run the test
    mvn compile exec:java -Dexec.mainClass="com.springvision.core.backend.OpenCvDemo" -Dexec.args="test"

    if [ $? -eq 0 ]; then
        print_success "OpenCV functionality test passed!"
    else
        print_error "OpenCV functionality test failed!"
        exit 1
    fi
}

# Main script logic
main() {
    # Get the project root directory (where this script is located)
    PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

    # Check prerequisites
    check_maven
    check_java

    # Parse command line arguments
    if [ $# -eq 0 ]; then
        show_usage
        exit 1
    fi

    case $1 in
        "example")
            if [ $# -lt 2 ]; then
                print_error "Example name is required"
                print_info "Usage: $0 example <name>"
                print_info "Use '$0 list' to see available examples"
                exit 1
            fi
            run_example "$2"
            ;;
        "list")
            list_examples
            ;;
        "build")
            build_all_examples
            ;;
        "clean")
            clean_all_examples
            ;;
        "test-opencv")
            test_opencv_functionality
            ;;
        "help"|"-h"|"--help")
            show_usage
            ;;
        *)
            print_error "Unknown command: $1"
            show_usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
