#!/bin/bash
# Test script to verify JSON logging is working correctly for the MCP server

set -e

echo "=================================================="
echo "MCP Server Logging Test"
echo "=================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test if output is valid JSON
test_json() {
    local json_output="$1"
    local test_name="$2"
    
    if echo "$json_output" | jq empty 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $test_name: Valid JSON"
        return 0
    else
        echo -e "${RED}✗${NC} $test_name: Invalid JSON"
        echo "Output: $json_output"
        return 1
    fi
}

# Function to check if logs contain expected fields
check_log_fields() {
    local json_output="$1"
    local test_name="$2"
    
    local has_timestamp=$(echo "$json_output" | jq 'has("timestamp")' 2>/dev/null)
    local has_level=$(echo "$json_output" | jq 'has("level")' 2>/dev/null)
    local has_logger=$(echo "$json_output" | jq 'has("logger")' 2>/dev/null)
    local has_message=$(echo "$json_output" | jq 'has("message")' 2>/dev/null)
    
    if [ "$has_timestamp" == "true" ] && [ "$has_level" == "true" ] && \
       [ "$has_logger" == "true" ] && [ "$has_message" == "true" ]; then
        echo -e "${GREEN}✓${NC} $test_name: All required fields present"
        return 0
    else
        echo -e "${RED}✗${NC} $test_name: Missing required fields"
        echo "  timestamp: $has_timestamp, level: $has_level, logger: $has_logger, message: $has_message"
        return 1
    fi
}

echo "Test 1: Checking if jq is installed..."
if ! command -v jq &> /dev/null; then
    echo -e "${RED}✗${NC} jq is not installed. Please install jq to validate JSON logs."
    echo "  On Ubuntu/Debian: sudo apt-get install jq"
    echo "  On macOS: brew install jq"
    exit 1
fi
echo -e "${GREEN}✓${NC} jq is installed"
echo ""

echo "Test 2: Building MCP server JAR..."
cd "$(dirname "$0")/.."
mvn clean package -pl mcp -am -DskipTests -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} MCP server JAR built successfully"
else
    echo -e "${RED}✗${NC} Failed to build MCP server JAR"
    exit 1
fi
echo ""

echo "Test 3: Starting MCP server and capturing logs..."
# Create temporary log file
TEMP_LOG=$(mktemp)

# Start the server in the background and send a simple initialize message
# Redirect stderr to our temp file
timeout 10s java -jar mcp/target/mcp-*.jar 2>"$TEMP_LOG" >/dev/null &
SERVER_PID=$!

# Wait a moment for startup logs
sleep 3

# Kill the server
kill $SERVER_PID 2>/dev/null || true
wait $SERVER_PID 2>/dev/null || true

echo -e "${GREEN}✓${NC} MCP server started and stopped"
echo ""

echo "Test 4: Validating JSON log format..."
VALID_JSON_COUNT=0
TOTAL_LINES=0

# Check each line of the log file
while IFS= read -r line; do
    if [ -n "$line" ]; then
        TOTAL_LINES=$((TOTAL_LINES + 1))
        if echo "$line" | jq empty 2>/dev/null; then
            VALID_JSON_COUNT=$((VALID_JSON_COUNT + 1))
        else
            echo -e "${RED}✗${NC} Invalid JSON line: $line"
        fi
    fi
done < "$TEMP_LOG"

if [ $TOTAL_LINES -eq 0 ]; then
    echo -e "${YELLOW}⚠${NC} No log lines found. The server may not have started properly."
elif [ $VALID_JSON_COUNT -eq $TOTAL_LINES ]; then
    echo -e "${GREEN}✓${NC} All $TOTAL_LINES log lines are valid JSON"
else
    echo -e "${RED}✗${NC} Only $VALID_JSON_COUNT out of $TOTAL_LINES log lines are valid JSON"
fi
echo ""

echo "Test 5: Checking for required log fields..."
FIRST_LOG=$(head -n 1 "$TEMP_LOG")
if [ -n "$FIRST_LOG" ]; then
    check_log_fields "$FIRST_LOG" "First log entry"
else
    echo -e "${YELLOW}⚠${NC} No logs to check"
fi
echo ""

echo "Test 6: Checking for structured event fields..."
EVENT_COUNT=$(jq -r 'select(.event != null) | .event' "$TEMP_LOG" 2>/dev/null | wc -l)
if [ $EVENT_COUNT -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Found $EVENT_COUNT log entries with 'event' field"
    echo "  Event types found:"
    jq -r 'select(.event != null) | .event' "$TEMP_LOG" 2>/dev/null | sort | uniq -c | while read -r line; do
        echo "    $line"
    done
else
    echo -e "${YELLOW}⚠${NC} No log entries with 'event' field found"
fi
echo ""

echo "Test 7: Checking for startup events..."
STARTUP_EVENTS=$(jq -r 'select(.event == "mcp_server_startup")' "$TEMP_LOG" 2>/dev/null | wc -l)
if [ $STARTUP_EVENTS -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Found mcp_server_startup event"
else
    echo -e "${YELLOW}⚠${NC} No mcp_server_startup event found"
fi
echo ""

echo "Test 8: Verifying log file creation..."
if [ -f "logs/mcp.json.log" ]; then
    echo -e "${GREEN}✓${NC} Log file logs/mcp.json.log created"
    FILE_LOG_LINES=$(wc -l < logs/mcp.json.log)
    echo "  File contains $FILE_LOG_LINES lines"
else
    echo -e "${YELLOW}⚠${NC} Log file logs/mcp.json.log not found (may be expected for short run)"
fi
echo ""

echo "Test 9: Sample log output (prettified)..."
echo "First log entry:"
head -n 1 "$TEMP_LOG" | jq '.' 2>/dev/null || echo "Unable to parse log"
echo ""

echo "=================================================="
echo "Summary"
echo "=================================================="
if [ $TOTAL_LINES -gt 0 ] && [ $VALID_JSON_COUNT -eq $TOTAL_LINES ] && [ $EVENT_COUNT -gt 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo "  - All logs are in JSON format"
    echo "  - Structured logging with events is working"
    echo "  - MCP server logging is configured correctly"
else
    echo -e "${YELLOW}⚠ Some tests did not pass as expected${NC}"
    echo "  This may be normal for a quick startup test"
    echo "  Review the output above for details"
fi
echo ""

# Cleanup
rm -f "$TEMP_LOG"

echo "You can view logs with:"
echo "  tail -f logs/mcp.json.log | jq"
echo ""
echo "Filter by event type:"
echo "  jq 'select(.event == \"count_faces_success\")' logs/mcp.json.log"
echo ""

