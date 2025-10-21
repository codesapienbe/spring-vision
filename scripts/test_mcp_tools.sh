#!/usr/bin/env bash
# MCP tools URL-only tester
# Creates per-tool timestamped logs in mcp-tool-tests/logs/ and a summary file

set -uo pipefail

ROOT_DIR="$(pwd)"
LOG_DIR="$ROOT_DIR/mcp-tool-tests/logs"
SUMMARY_FILE="$ROOT_DIR/mcp-tool-tests/summary.txt"
JBANG_RUNNER="$ROOT_DIR/run.java"
SLEEP_SECONDS=1

mkdir -p "$LOG_DIR"
rm -f "$SUMMARY_FILE"
echo "MCP Tools Test Summary" > "$SUMMARY_FILE"
echo "Started: $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$SUMMARY_FILE"
echo "" >> "$SUMMARY_FILE"

# Example URLs (from docs/getting-started/mcp-tools-testing.md)
URLS=(
  "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
  "https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"
  "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"
)

# Tools to run (URL-only variants)
TOOLS=(
  "count_faces_u"
  "extract_face_embeddings_u"
  "detect_objects_u"
  "classify_image_u"
  "extract_text_u"
  "detect_emotions_u"
  "detect_poses_u"
  "detect_nsfw_u"
  "extract_image_metadata_u"
)

if ! command -v jbang >/dev/null 2>&1; then
  echo "JBang is not installed or not in PATH. Please install JBang from https://jbang.dev/" >&2
  exit 2
fi

if [ ! -f "$JBANG_RUNNER" ]; then
  echo "JBang runner script not found at $JBANG_RUNNER" >&2
  exit 3
fi

total=0
failed=0

for i in "${!TOOLS[@]}"; do
  tool="${TOOLS[$i]}"
  url="${URLS[$((i % ${#URLS[@]}))]}"
  timestamp=$(date +%Y%m%d_%H%M%S)
  logfile="$LOG_DIR/${tool}-${timestamp}.log"

  # Build payload depending on tool
  case "$tool" in
    "classify_image_u")
      PAYLOAD=$(cat <<EOF
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"$tool","arguments":{"imageUrl":"$url","topK":5}}}
EOF
)
      ;;
    *)
      PAYLOAD=$(cat <<EOF
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"$tool","arguments":{"imageUrl":"$url"}}}
EOF
)
      ;;
  esac

  echo "Running $tool against $url -> log: $logfile"
  start_ts=$(date +%s%3N)
  # Run the JBang runner script and capture output
  echo "$PAYLOAD" | jbang "$JBANG_RUNNER" > "$logfile" 2>&1
  exit_code=$?
  end_ts=$(date +%s%3N)
  duration_ms=$((end_ts - start_ts))

  total=$((total + 1))
  if [ $exit_code -ne 0 ]; then
    failed=$((failed + 1))
    status="FAIL"
  else
    status="OK"
  fi

  echo "$tool | $status | exit:$exit_code | time:${duration_ms}ms | log:$(basename "$logfile")" >> "$SUMMARY_FILE"

  # brief sleep between calls
  sleep "$SLEEP_SECONDS"
done

echo "" >> "$SUMMARY_FILE"
echo "Finished: $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$SUMMARY_FILE"
echo "Total: $total, Failed: $failed" >> "$SUMMARY_FILE"

if [ $failed -ne 0 ]; then
  echo "One or more tool calls failed. See $LOG_DIR and $SUMMARY_FILE for details." >&2
  exit 4
fi

echo "All tool calls completed successfully. See $SUMMARY_FILE for details."
exit 0


