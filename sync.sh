#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="https://github.com/codesapienbe/deepface"
TARGET_DIR_NAME="spring-vision-deepface"

# Resolve script directory robustly
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
TARGET_DIR_PATH="${SCRIPT_DIR}/${TARGET_DIR_NAME}"
GIT_DIR_PATH="${TARGET_DIR_PATH}/.git"

on_error() {
  echo "[ERROR] sync.sh failed. See messages above." >&2
}
trap on_error ERR

echo "[INFO] Using repo: ${REPO_URL}"
echo "[INFO] Target directory: ${TARGET_DIR_PATH}"

ensure_dir_removed() {
  local dir_to_remove="$1"
  if [[ -z "${dir_to_remove}" || "${dir_to_remove}" == "/" ]]; then
    echo "[ERROR] Refusing to remove an empty or root directory." >&2
    exit 1
  fi
  # Safety guard: ensure we only remove the expected target directory
  if [[ "${dir_to_remove}" != "${TARGET_DIR_PATH}" ]]; then
    echo "[ERROR] Refusing to remove unexpected directory: ${dir_to_remove}" >&2
    exit 1
  fi
  if [[ -d "${dir_to_remove}" ]]; then
    echo "[INFO] Removing existing directory: ${dir_to_remove}"
    rm -rf -- "${dir_to_remove}"
  fi
}

clone_repo() {
  echo "[INFO] Cloning repository into ${TARGET_DIR_PATH}"
  git clone "${REPO_URL}" "${TARGET_DIR_PATH}"
}

update_repo_if_present() {
  echo "[INFO] Pulling latest changes in ${TARGET_DIR_PATH}"
  git -C "${TARGET_DIR_PATH}" fetch --all --prune --tags
  git -C "${TARGET_DIR_PATH}" pull --ff-only
}

# Main sync logic
if [[ ! -d "${TARGET_DIR_PATH}" ]]; then
  clone_repo
else
  if [[ -d "${GIT_DIR_PATH}" ]]; then
    update_repo_if_present
  else
    echo "[WARN] ${TARGET_DIR_NAME} exists but is not a Git repo (no .git). Re-cloning..."
    ensure_dir_removed "${TARGET_DIR_PATH}"
    clone_repo
  fi
fi

# Always remove the nested .git directory (do NOT touch parent repo)
if [[ -d "${GIT_DIR_PATH}" ]]; then
  # Safety guard: ensure path is exactly under expected target directory
  if [[ "${GIT_DIR_PATH}" == "${TARGET_DIR_PATH}/.git" ]]; then
    echo "[INFO] Removing ${GIT_DIR_PATH} to avoid nested Git repository"
    rm -rf -- "${GIT_DIR_PATH}"
  else
    echo "[ERROR] Unexpected .git path: ${GIT_DIR_PATH}" >&2
    exit 1
  fi
fi

# Dockerize the project
echo "[INFO] Dockerizing project..."
cd "${SCRIPT_DIR}/${TARGET_DIR_NAME}"
docker build -t spring-vision-deepface .
if [ $? -ne 0 ]; then
  echo "Docker build failed" >&2
  exit 1
fi

echo "[INFO] sync.sh completed successfully."
