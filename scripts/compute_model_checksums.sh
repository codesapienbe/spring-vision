#!/usr/bin/env bash
set -euo pipefail

# Compute SHA-256 checksums for the default model URLs in ModelUrls.defaults()
# Usage: ./scripts/compute_model_checksums.sh [--temp-dir DIR] [--keys key1,key2,...]
# Example: ./scripts/compute_model_checksums.sh --keys arcface.onnx,vggface.onnx

TEMP_DIR="${TMPDIR:-/tmp}/spring-vision-models"
mkdir -p "$TEMP_DIR"

declare -A MODELS=(
  ["arcface.onnx"]="https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/ArcFace.onnx"
  ["facenet128.onnx"]="https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/Facenet128.onnx"
  ["openface.onnx"]="https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/OpenFace.onnx"
  ["deepid.onnx"]="https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/DeepID.onnx"
  ["vggface.onnx"]="https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/VGGFace.onnx"
)

# Parse args
KEYS=()
while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --temp-dir)
      shift; TEMP_DIR="$1"; shift;;
    --keys)
      shift; IFS=',' read -r -a KEYS <<< "$1"; shift;;
    *) echo "Unknown arg: $1"; exit 1;;
  esac
done

if [[ ${#KEYS[@]} -eq 0 ]]; then
  KEYS=("${!MODELS[@]}")
fi

printf "# Temp dir: %s\n" "$TEMP_DIR"

for key in "${KEYS[@]}"; do
  url=${MODELS[$key]:-}
  if [[ -z "$url" ]]; then
    printf "# Skipping unknown key: %s\n" "$key" >&2
    continue
  fi

  out="$TEMP_DIR/$key"
  echo "Downloading $key from $url -> $out"

  # curl with sensible timeouts, fail on HTTP errors, limit filesize to 1GB
  if ! curl --fail --location --connect-timeout 15 --max-time 300 --max-filesize 1073741824 -o "$out.part" "$url"; then
    echo "# Failed to download $url" >&2
    rm -f "$out.part"
    continue
  fi
  mv "$out.part" "$out"

  if [[ ! -s "$out" ]]; then
    echo "# Downloaded file is empty: $out" >&2
    rm -f "$out"
    continue
  fi

  checksum=$(sha256sum "$out" | awk '{print $1}')
  echo "Map.entry(\"$key\", \"$checksum\"),"
done

echo "# Done. Paste the printed Map.entry(...) lines into ModelUrls.checksums()."
