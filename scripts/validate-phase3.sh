#!/bin/bash
# Phase 3 Testing Validation Script

set -e

echo "=========================================="
echo "Spring Vision - Phase 3 Test Validation"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check Java version
echo "1. Checking Java version..."
java -version 2>&1 | grep -q "version \"21" && JAVA_OK=0 || JAVA_OK=1
print_status $JAVA_OK "Java 21 required"

# Check Maven
echo ""
echo "2. Checking Maven..."
if [ -f "./mvnw" ]; then
    print_status 0 "Maven wrapper found"
else
    print_status 1 "Maven wrapper not found"
    exit 1
fi

# Verify test files exist
echo ""
echo "3. Verifying test files..."
TEST_FILES=(
    "core/src/test/java/io/github/codesapienbe/springvision/core/djl/DjlPropertiesTest.java"
    "core/src/test/java/io/github/codesapienbe/springvision/core/djl/DjlAutoConfigurationTest.java"
    "core/src/test/java/io/github/codesapienbe/springvision/core/djl/DjlVisionBackendTest.java"
    "core/src/test/java/io/github/codesapienbe/springvision/core/djl/DjlModelLoaderTest.java"
    "core/src/test/java/io/github/codesapienbe/springvision/core/djl/DjlVisionBackendIntegrationTest.java"
    "core/src/test/java/io/github/codesapienbe/springvision/core/djl/DjlPerformanceTest.java"
    "core/src/test/java/io/github/codesapienbe/springvision/core/djl/DjlErrorHandlingTest.java"
    "core/src/test/java/io/github/codesapienbe/springvision/core/djl/DjlModelCacheTest.java"
    "core/src/test/java/io/github/codesapienbe/springvision/core/capabilities/CapabilitiesIntegrationTest.java"
)

for file in "${TEST_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_status 0 "$(basename $file)"
    else
        print_status 1 "$(basename $file) - NOT FOUND"
    fi
done

# Check documentation
echo ""
echo "4. Verifying documentation..."
DOC_FILES=(
    "docs/PHASE_3_TESTING.md"
    "docs/TESTING_GUIDE.md"
)

for file in "${DOC_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_status 0 "$(basename $file)"
    else
        print_status 1 "$(basename $file) - NOT FOUND"
    fi
done

# Run unit tests
echo ""
echo "5. Running unit tests (fast)..."
echo "   This may take a minute..."

./mvnw clean test -pl core -q 2>&1 | tee test-output.log

if [ ${PIPESTATUS[0]} -eq 0 ]; then
    print_status 0 "Unit tests passed"

    # Extract test statistics
    echo ""
    echo "   Test Statistics:"
    grep -E "Tests run:|BUILD SUCCESS" test-output.log | tail -5
else
    print_status 1 "Unit tests failed"
    echo ""
    echo "   Check test-output.log for details"
    print_warning "Some tests may fail if models are not downloaded"
fi

# Check test coverage configuration
echo ""
echo "6. Checking test coverage configuration..."
if grep -q "jacoco" core/pom.xml; then
    print_status 0 "JaCoCo coverage configured"
else
    print_warning "JaCoCo coverage not configured"
fi

# Summary
echo ""
echo "=========================================="
echo "Validation Complete"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Review test results above"
echo "  2. Check docs/PHASE_3_TESTING.md for details"
echo "  3. Run './mvnw test' to execute all tests"
echo "  4. Run './mvnw verify jacoco:report' for coverage"
echo ""
echo "Note: Some integration tests are disabled by default"
echo "      Enable them manually for full model testing"
echo ""

# Cleanup
rm -f test-output.log


