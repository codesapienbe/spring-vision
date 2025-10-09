#!/bin/bash

# One Million Challenge Runner
# Production-ready face recognition system with Spring Vision

echo "==========================================="
echo "🏆 Spring Vision One Million Challenge"
echo "Sub-Second Search Across 1M+ Photos"
echo "==========================================="
echo

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
JAVA_OPTS=${JAVA_OPTS:-"-Xmx4g -Xms1g -server"}
SPRING_PROFILES=${SPRING_PROFILES:-""}
SERVER_PORT=${SERVER_PORT:-8080}

# Functions
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check for Java
if ! command -v java &> /dev/null; then
    print_error "Java not found. Please install Java 21 or later."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "([0-9]+)' | grep -oP '[0-9]+' | head -1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    print_error "Java 21 or later required. Found version $JAVA_VERSION."
    exit 1
fi

print_success "Java $JAVA_VERSION detected"

# Check for Maven
if ! command -v mvn &> /dev/null; then
    print_error "Maven not found. Please install Apache Maven."
    exit 1
fi

print_success "Maven detected"

# Check if sample photos directory exists
if [ -d "./sample-photos" ]; then
    PHOTO_COUNT=$(find ./sample-photos -name "*.jpg" -o -name "*.jpeg" -o -name "*.png" | wc -l)
    if [ "$PHOTO_COUNT" -gt 0 ]; then
        print_info "Found $PHOTO_COUNT photos in ./sample-photos"
        print_info "These will be processed automatically on startup"
    else
        print_warning "Directory ./sample-photos exists but contains no photos"
        print_info "Add .jpg, .jpeg, or .png files to test with your photos"
    fi
else
    print_info "No ./sample-photos directory found"
    print_info "Create one and add photos to test the system:"
    echo -e "  ${BLUE}mkdir sample-photos${NC}"
    echo -e "  ${BLUE}cp /path/to/photos/*.jpg sample-photos/${NC}"
fi

# Check for query photo
if [ -f "./query-selfie.jpg" ]; then
    print_info "Query photo found: ./query-selfie.jpg"
    print_info "This will be used for automatic recognition demo"
else
    print_info "No query photo found"
    print_info "Add query-selfie.jpg to test face recognition"
fi

echo

# Build if necessary
if [ ! -f "target/one-million-challenge-1.0.jar" ]; then
    print_info "Building application..."
    mvn clean package -DskipTests -q
    if [ $? -ne 0 ]; then
        print_error "Build failed"
        exit 1
    fi
    print_success "Build completed"
fi

# Display system information
print_info "System Configuration:"
echo -e "  ${BLUE}Java Options:${NC} $JAVA_OPTS"
echo -e "  ${BLUE}Server Port:${NC} $SERVER_PORT"
echo -e "  ${BLUE}Spring Profiles:${NC} ${SPRING_PROFILES:-none}"

echo

# Set up JVM options for optimal face recognition performance
export MAVEN_OPTS="$JAVA_OPTS -Dserver.port=$SERVER_PORT"

if [ -n "$SPRING_PROFILES" ]; then
    export MAVEN_OPTS="$MAVEN_OPTS -Dspring.profiles.active=$SPRING_PROFILES"
fi

# Additional JVM tuning for face recognition workloads
export MAVEN_OPTS="$MAVEN_OPTS -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"

print_info "Starting One Million Challenge..."
echo

# Start the application
mvn spring-boot:run

# Check if the application started successfully
if [ $? -eq 0 ]; then
    print_success "Application started successfully"
    echo
    print_info "Application URLs:"
    echo -e "  ${BLUE}Web Interface:${NC} http://localhost:$SERVER_PORT"
    echo -e "  ${BLUE}REST API:${NC} http://localhost:$SERVER_PORT/api/recognition"
    echo -e "  ${BLUE}Health Check:${NC} http://localhost:$SERVER_PORT/actuator/health"
    echo -e "  ${BLUE}Metrics:${NC} http://localhost:$SERVER_PORT/actuator/metrics"
    echo
    print_info "Use Ctrl+C to stop the application"
else
    print_error "Application failed to start"
    echo
    print_info "Troubleshooting:"
    echo -e "  ${BLUE}1.${NC} Check logs above for error messages"
    echo -e "  ${BLUE}2.${NC} Ensure port $SERVER_PORT is not in use"
    echo -e "  ${BLUE}3.${NC} Verify Java 21+ and Maven are installed"
    echo -e "  ${BLUE}4.${NC} Check available memory (4GB recommended)"
    exit 1
fi
