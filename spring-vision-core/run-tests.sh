#!/bin/bash

# Spring Vision Core Test Execution Script
# This script provides various options for running tests in the core module

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CORE_MODULE="$SCRIPT_DIR"
TEST_RESULTS_DIR="$CORE_MODULE/target/test-results"
COVERAGE_DIR="$CORE_MODULE/target/site/jacoco"
LOG_DIR="$CORE_MODULE/logs"

# Default values
TEST_TYPE="all"
PROFILE="test"
PARALLEL="false"
THREAD_COUNT="4"
VERBOSE="false"
CLEAN="true"
COVERAGE="true"
REPORTS="true"
OPENCV_AVAILABLE="false"

# Function to print usage
print_usage() {
    echo -e "${BLUE}Spring Vision Core Test Execution Script${NC}"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -t, --test-type TYPE     Test type to run (all|unit|integration|performance|security|suite)"
    echo "  -p, --profile PROFILE    Spring profile to use (default: test)"
    echo "  -P, --parallel          Enable parallel test execution"
    echo "  -c, --thread-count NUM   Number of threads for parallel execution (default: 4)"
    echo "  -v, --verbose           Enable verbose output"
    echo "  -n, --no-clean          Skip cleaning before running tests"
    echo "  -C, --no-coverage       Skip coverage report generation"
    echo "  -R, --no-reports        Skip test report generation"
    echo "  -o, --opencv            Enable OpenCV for integration tests"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all tests"
    echo "  $0 -t unit                           # Run only unit tests"
    echo "  $0 -t integration -o                 # Run integration tests with OpenCV"
    echo "  $0 -t performance -P -c 8           # Run performance tests in parallel with 8 threads"
    echo "  $0 -t security -v                   # Run security tests with verbose output"
    echo ""
}

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

# Function to check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."

    # Check if Java is available
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed or not in PATH"
        exit 1
    fi

    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_error "Java 21 or later is required. Found version: $JAVA_VERSION"
        exit 1
    fi

    print_success "Java version: $(java -version 2>&1 | head -n 1)"

    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed or not in PATH"
        exit 1
    fi

    print_success "Maven version: $(mvn -version | head -n 1)"

    # Check if we're in the right directory
    if [ ! -f "$CORE_MODULE/pom.xml" ]; then
        print_error "pom.xml not found in $CORE_MODULE"
        exit 1
    fi

    print_success "Prerequisites check completed"
}

# Function to create directories
create_directories() {
    print_info "Creating necessary directories..."

    mkdir -p "$TEST_RESULTS_DIR"
    mkdir -p "$COVERAGE_DIR"
    mkdir -p "$LOG_DIR"

    print_success "Directories created"
}

# Function to clean project
clean_project() {
    if [ "$CLEAN" = "true" ]; then
        print_info "Cleaning project..."
        mvn clean -f "$CORE_MODULE/pom.xml" -q
        print_success "Project cleaned"
    else
        print_warning "Skipping clean step"
    fi
}

# Function to build project
build_project() {
    print_info "Building project..."
    mvn compile test-compile -f "$CORE_MODULE/pom.xml" -q
    print_success "Project built"
}

# Function to run tests
run_tests() {
    local test_type="$1"
    local maven_args=""

    print_info "Running $test_type tests..."

    # Set up Maven arguments based on test type
    case "$test_type" in
        "unit")
            maven_args="-Dtest=*Test -DexcludedGroups=integration,performance,security"
            ;;
        "integration")
            maven_args="-Dtest=*IntegrationTest"
            ;;
        "performance")
            maven_args="-Dtest=*PerformanceTest"
            ;;
        "security")
            maven_args="-Dtest=*SecurityTest"
            ;;
        "suite")
            maven_args="-Dtest=*TestSuite"
            ;;
        "all")
            maven_args=""
            ;;
        *)
            print_error "Unknown test type: $test_type"
            exit 1
            ;;
    esac

    # Add profile
    maven_args="$maven_args -Dspring.profiles.active=$PROFILE"

    # Add OpenCV flag if enabled
    if [ "$OPENCV_AVAILABLE" = "true" ]; then
        maven_args="$maven_args -Dopencv.available=true"
    fi

    # Add parallel execution if enabled
    if [ "$PARALLEL" = "true" ]; then
        maven_args="$maven_args -Dparallel=true -DthreadCount=$THREAD_COUNT"
    fi

    # Add verbose output if enabled
    if [ "$VERBOSE" = "true" ]; then
        maven_args="$maven_args -X"
    fi

    # Run tests
    if mvn test -f "$CORE_MODULE/pom.xml" $maven_args; then
        print_success "$test_type tests completed successfully"
    else
        print_error "$test_type tests failed"
        exit 1
    fi
}

# Function to generate coverage report
generate_coverage_report() {
    if [ "$COVERAGE" = "true" ]; then
        print_info "Generating coverage report..."
        if mvn jacoco:report -f "$CORE_MODULE/pom.xml" -q; then
            print_success "Coverage report generated at $COVERAGE_DIR"
        else
            print_warning "Failed to generate coverage report"
        fi
    else
        print_warning "Skipping coverage report generation"
    fi
}

# Function to generate test reports
generate_test_reports() {
    if [ "$REPORTS" = "true" ]; then
        print_info "Generating test reports..."
        if mvn surefire-report:report -f "$CORE_MODULE/pom.xml" -q; then
            print_success "Test reports generated"
        else
            print_warning "Failed to generate test reports"
        fi
    else
        print_warning "Skipping test report generation"
    fi
}

# Function to display test summary
display_summary() {
    print_info "Test execution summary:"
    echo "  Test type: $TEST_TYPE"
    echo "  Profile: $PROFILE"
    echo "  Parallel: $PARALLEL"
    if [ "$PARALLEL" = "true" ]; then
        echo "  Thread count: $THREAD_COUNT"
    fi
    echo "  OpenCV enabled: $OPENCV_AVAILABLE"
    echo "  Coverage: $COVERAGE"
    echo "  Reports: $REPORTS"
    echo ""

    # Display test results if available
    if [ -d "$TEST_RESULTS_DIR" ]; then
        local test_count=$(find "$TEST_RESULTS_DIR" -name "*.xml" | wc -l)
        print_info "Test result files: $test_count"
    fi

    # Display coverage summary if available
    if [ -f "$COVERAGE_DIR/index.html" ]; then
        print_info "Coverage report available at: $COVERAGE_DIR/index.html"
    fi
}

# Function to run specific test scenarios
run_test_scenarios() {
    case "$TEST_TYPE" in
        "unit")
            run_tests "unit"
            ;;
        "integration")
            run_tests "integration"
            ;;
        "performance")
            run_tests "performance"
            ;;
        "security")
            run_tests "security"
            ;;
        "suite")
            run_tests "suite"
            ;;
        "all")
            run_tests "all"
            ;;
        *)
            print_error "Unknown test type: $TEST_TYPE"
            exit 1
            ;;
    esac
}

# Function to handle cleanup
cleanup() {
    print_info "Cleaning up..."
    # Add any cleanup tasks here
    print_success "Cleanup completed"
}

# Main execution function
main() {
    print_info "Starting Spring Vision Core test execution..."
    echo ""

    # Check prerequisites
    check_prerequisites

    # Create directories
    create_directories

    # Clean project
    clean_project

    # Build project
    build_project

    # Run tests
    run_test_scenarios

    # Generate reports
    generate_coverage_report
    generate_test_reports

    # Display summary
    display_summary

    # Cleanup
    cleanup

    print_success "Test execution completed successfully!"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--test-type)
            TEST_TYPE="$2"
            shift 2
            ;;
        -p|--profile)
            PROFILE="$2"
            shift 2
            ;;
        -P|--parallel)
            PARALLEL="true"
            shift
            ;;
        -c|--thread-count)
            THREAD_COUNT="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE="true"
            shift
            ;;
        -n|--no-clean)
            CLEAN="false"
            shift
            ;;
        -C|--no-coverage)
            COVERAGE="false"
            shift
            ;;
        -R|--no-reports)
            REPORTS="false"
            shift
            ;;
        -o|--opencv)
            OPENCV_AVAILABLE="true"
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Validate arguments
if [ "$THREAD_COUNT" -lt 1 ] || [ "$THREAD_COUNT" -gt 16 ]; then
    print_error "Thread count must be between 1 and 16"
    exit 1
fi

# Run main function
main "$@"
