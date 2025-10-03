#!/usr/bin/env bash
set -euo pipefail

OUT_FILE="docs/TODO_SCAN.md"
mkdir -p "$(dirname "$OUT_FILE")"

echo "# TODO Scan Report" > "$OUT_FILE"
echo "" >> "$OUT_FILE"
echo "Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")" >> "$OUT_FILE"
echo "" >> "$OUT_FILE"
echo "This report lists occurrences of the string 'TODO' (case-sensitive) across the repository, excluding .git, target, node_modules and common generated frontend directories." >> "$OUT_FILE"
echo "" >> "$OUT_FILE"
echo "Notes: The scan intentionally excludes 'frontend' directories (common in Vaadin examples) and other generated assets. If you need a full scan including generated files, run: grep -RIn --line-number -e \"TODO\" ." >> "$OUT_FILE"
echo "" >> "$OUT_FILE"
echo '```' >> "$OUT_FILE"
# Use grep to find TODO occurrences; do not fail if none found
# Exclude common noisy directories: .git, target, node_modules, frontend
grep -RIn --line-number --exclude-dir=.git --exclude-dir=target --exclude-dir=node_modules --exclude-dir=frontend -e "TODO" . || true

echo '```' >> "$OUT_FILE"

echo "Report written to $OUT_FILE"
