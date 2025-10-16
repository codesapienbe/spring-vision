#!/usr/bin/env bash
# Moves all Markdown files (case-insensitive) into ./docs and lowercases filenames.
# Skips any README.md (case-insensitive).
# If a lowercased filename collides, appends -1, -2, ... before the extension.
# Usage: ./move_markdowns_to_docs.sh [-n]
#   -n  : dry-run (show actions, don't move files)

set -euo pipefail

DRY_RUN=false
while getopts "n" opt; do
  case "$opt" in
    n) DRY_RUN=true ;;
    *) echo "Usage: $0 [-n]"; exit 1 ;;
  esac
done

TARGET_DIR="./docs"
mkdir -p "$TARGET_DIR"

# Find markdown files, case-insensitive, excluding the docs directory itself.
# Use -print0 to safely handle any filenames (spaces/newlines).
find . -type f -iname '*.md' -not -path "${TARGET_DIR}/*" -print0 |
while IFS= read -r -d '' file; do
  # Skip the find results like ./docs (already excluded) and current script file if it's .md
  # Normalize base name to lowercase
  base=$(basename -- "$file")
  lbase=$(printf '%s' "$base" | tr '[:upper:]' '[:lower:]')

  # Skip README.md (case-insensitive)
  if [ "$lbase" = "readme.md" ]; then
    printf 'Skipping README: %s\n' "$file"
    continue
  fi

  name="${lbase%.*}"
  ext="${lbase##*.}"

  dest="$TARGET_DIR/$lbase"
  i=1
  while [ -e "$dest" ]; do
    dest="$TARGET_DIR/${name}-$i.$ext"
    i=$((i+1))
  done

  if $DRY_RUN; then
    printf 'Would move: %s -> %s\n' "$file" "$dest"
  else
    printf 'Moving: %s -> %s\n' "$file" "$dest"
    mv -- "$file" "$dest"
  fi

done

