#!/usr/bin/env bash
set -euo pipefail

# Find the docker image tag for a repository whose last path component
# starts with "spring-vision". Prints the tag to stdout and returns 0.
# Exit codes:
# 0 - found and printed tag
# 2 - docker CLI missing
# 3 - not found

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker command not found" >&2
  exit 2
fi

# Use docker images with a simple format: <repository> <tag>
# Example repository values:
#  - spring-vision
#  - docker.io/codesapienbe/spring-vision
# We match on the last path component (after the final /) starting with 'spring-vision'

while IFS= read -r line; do
  # split line into repo and tag (repo may contain spaces theoretically, but docker output won't)
  repo=${line%% *}
  tag=${line##* }
  name=${repo##*/}
  if [[ $name == spring-vision* ]]; then
    echo "$tag"
    exit 0
  fi
done < <(docker images --format '{{.Repository}} {{.Tag}}')

# If we get here, nothing matched
echo "Error: no docker image found with repository name starting with 'spring-vision'" >&2
exit 3

