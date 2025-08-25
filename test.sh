#!/bin/bash

set -euo pipefail

print_usage() {
  cat <<EOF
Usage: $0 [--cli] [--api] [--web] [--all] [--help]

Goals:
  --cli   Run Picocli example commands (detect/verify/embed/obscure) and summarize results
  --api   Run API examples (GWT and Vaadin) on separate ports; verify /api/vision/health and summarize
  --web   Run Basic Face Detection web app; verify home page and URL-detect path and summarize
  --all   Run all of the above
  --help  Show this help

Environment:
  SERVER_PORT may be overridden per goal internally to avoid conflicts
EOF
}

if [[ ${1:-} == "--help" ]]; then
  print_usage
  exit 0
fi

RUN_CLI=0
RUN_API=0
RUN_WEB=0
if [[ $# -eq 0 ]]; then
  RUN_CLI=1
else
  for arg in "$@"; do
    case "$arg" in
      --cli) RUN_CLI=1 ;;
      --api) RUN_API=1 ;;
      --web) RUN_WEB=1 ;;
      --all) RUN_CLI=1; RUN_API=1; RUN_WEB=1 ;;
      *) echo "Unknown option: $arg"; print_usage; exit 1 ;;
    esac
  done
fi

# Build all modules once to ensure latest APIs are installed locally
SPRING_DOCKER_COMPOSE_ENABLED=false \
SERVER_PORT=9999 \
mvn -DskipTests install

FAILED=0

########################################
# Helpers
########################################
wait_for_http() {
  # wait_for_http <url> <timeout_secs>
  local url="$1"
  local timeout="$2"
  local start=$(date +%s)
  while true; do
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "^200$"; then
      return 0
    fi
    local now=$(date +%s)
    if (( now - start >= timeout )); then
      return 1
    fi
    sleep 1
  done
}

start_app() {
  # start_app <module_pom_path> <port>
  local pom="$1"
  local port="$2"
  SPRING_DOCKER_COMPOSE_ENABLED=false \
  SERVER_PORT="$port" \
  mvn -q -f "$pom" -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run >/dev/null 2>&1 &
  echo $!
}

stop_app() {
  # stop_app <pid>
  local pid="$1"
  if kill -0 "$pid" >/dev/null 2>&1; then
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" 2>/dev/null || true
  fi
}

########################################
# CLI goal
########################################
if [[ $RUN_CLI -eq 1 ]]; then
  # DETECT A (CSV for easy parsing)
  DETECT_A_OUT=$(SPRING_DOCKER_COMPOSE_ENABLED=false \
  VISION_BACKEND=opencv \
  VISION_DEEPFACE_ENABLED=false \
  VISION_COMPREFACE_ENABLED=false \
  SERVER_PORT=9999 \
  mvn -q -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
    -Dspring-boot.run.arguments="--detect ~/mockdata/spring-vision/selfie-a.jpg --format csv" 2>/dev/null || true)
  DETECT_A_RC=$?
  if [[ $DETECT_A_RC -ne 0 ]]; then
    echo "DETECT A failed"
    FAILED=1
  fi

  # DETECT B (CSV)
  DETECT_B_OUT=$(SPRING_DOCKER_COMPOSE_ENABLED=false \
  VISION_BACKEND=opencv \
  VISION_DEEPFACE_ENABLED=false \
  VISION_COMPREFACE_ENABLED=false \
  SERVER_PORT=9999 \
  mvn -q -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
    -Dspring-boot.run.arguments="--detect ~/mockdata/spring-vision/selfie-b.jpg --format csv" 2>/dev/null || true)
  DETECT_B_RC=$?
  if [[ $DETECT_B_RC -ne 0 ]]; then
    echo "DETECT B failed"
    FAILED=1
  fi

  # VERIFY A vs B (CSV)
  VERIFY_OUT=$(SPRING_DOCKER_COMPOSE_ENABLED=false \
  VISION_BACKEND=opencv \
  VISION_DEEPFACE_ENABLED=false \
  VISION_COMPREFACE_ENABLED=false \
  SERVER_PORT=9999 \
  mvn -q -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
    -Dspring-boot.run.arguments="--verify ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/selfie-b.jpg --metric cosine --format csv" 2>/dev/null || true)
  VERIFY_RC=$?
  if [[ $VERIFY_RC -ne 0 ]]; then
    echo "VERIFY failed"
    FAILED=1
  fi

  # EMBED A (CSV, truncate for readability)
  EMBED_OUT=$(SPRING_DOCKER_COMPOSE_ENABLED=false \
  VISION_BACKEND=opencv \
  VISION_DEEPFACE_ENABLED=false \
  VISION_COMPREFACE_ENABLED=false \
  SERVER_PORT=9999 \
  mvn -q -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
    -Dspring-boot.run.arguments="--embed ~/mockdata/spring-vision/selfie-a.jpg --format csv --truncate 8" 2>/dev/null || true)
  EMBED_RC=$?
  if [[ $EMBED_RC -ne 0 ]]; then
    echo "EMBED failed"
    FAILED=1
  fi

  # OBSCURE A -> A-obscured (no structured output; check status only)
  OBSCURE_STATUS=OK
  SPRING_DOCKER_COMPOSE_ENABLED=false \
  VISION_BACKEND=opencv \
  VISION_DEEPFACE_ENABLED=false \
  VISION_COMPREFACE_ENABLED=false \
  SERVER_PORT=9999 \
  mvn -q -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
    -Dspring-boot.run.arguments="--obscure ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/selfie-a-obscured.jpg" 2>/dev/null || true
  OBSCURE_RC=$?
  if [[ $OBSCURE_RC -ne 0 ]]; then
    echo "OBSCURE failed"
    OBSCURE_STATUS=FAIL
    FAILED=1
  fi

  # Parse outputs (only valid CSV rows)
  if [[ ${DETECT_A_RC:-1} -eq 0 ]]; then
    DETECT_A_COUNT=$(echo "$DETECT_A_OUT" | awk -F, 'BEGIN{flag=0} /^Index,Confidence/{flag=1; next} flag && $1 ~ /^[0-9]+$/{c++} END{print c+0}')
  else
    DETECT_A_COUNT="N/A"
  fi
  if [[ ${DETECT_B_RC:-1} -eq 0 ]]; then
    DETECT_B_COUNT=$(echo "$DETECT_B_OUT" | awk -F, 'BEGIN{flag=0} /^Index,Confidence/{flag=1; next} flag && $1 ~ /^[0-9]+$/{c++} END{print c+0}')
  else
    DETECT_B_COUNT="N/A"
  fi
  if [[ ${VERIFY_RC:-1} -eq 0 ]]; then
    VERIFY_LINE=$(echo "$VERIFY_OUT" | awk -F, '/^(cosine|euclidean),/{print $0; exit}')
    VERIFY_METRIC=$(echo "$VERIFY_LINE" | awk -F, '{print $1}')
    VERIFY_DISTANCE=$(echo "$VERIFY_LINE" | awk -F, '{print $2}')
    VERIFY_THRESHOLD=$(echo "$VERIFY_LINE" | awk -F, '{print $3}')
    VERIFY_MATCH=$(echo "$VERIFY_LINE" | awk -F, '{print $4}')
  else
    VERIFY_METRIC="N/A"; VERIFY_DISTANCE="N/A"; VERIFY_THRESHOLD="N/A"; VERIFY_MATCH="N/A"
  fi
  if [[ ${EMBED_RC:-1} -eq 0 ]]; then
    EMBED_COUNT=$(echo "$EMBED_OUT" | awk -F, 'BEGIN{flag=0} /^[Ii]ndex,[Cc]onfidence/{flag=1; next} flag && $1 ~ /^[0-9]+$/{c++} END{print c+0}')
  else
    EMBED_COUNT="N/A"
  fi
fi

########################################
# API goal (GWT + Vaadin health checks)
########################################
if [[ $RUN_API -eq 1 ]]; then
  # GWT on 8081
  GWT_PORT=8081
  GWT_PID=$(start_app spring-vision-examples/gwt-application/pom.xml $GWT_PORT)
  if wait_for_http "http://localhost:${GWT_PORT}/api/vision/health" 40; then
    GWT_HEALTH=$(curl -s "http://localhost:${GWT_PORT}/api/vision/health" | tr -d '\n')
    GWT_STATUS=OK
  else
    echo "GWT app failed to become healthy"
    GWT_STATUS=FAIL
    FAILED=1
  fi
  stop_app "$GWT_PID"

  # Vaadin on 8082
  VAADIN_PORT=8082
  VAADIN_PID=$(start_app spring-vision-examples/vaadin-application/pom.xml $VAADIN_PORT)
  if wait_for_http "http://localhost:${VAADIN_PORT}/api/vision/health" 60; then
    VAADIN_HEALTH=$(curl -s "http://localhost:${VAADIN_PORT}/api/vision/health" | tr -d '\n')
    VAADIN_STATUS=OK
  else
    echo "Vaadin app failed to become healthy"
    VAADIN_STATUS=FAIL
    FAILED=1
  fi
  stop_app "$VAADIN_PID"
fi

########################################
# WEB goal (Basic face detection app)
########################################
if [[ $RUN_WEB -eq 1 ]]; then
  WEB_PORT=8083
  WEB_PID=$(start_app spring-vision-examples/basic-face-detection/pom.xml $WEB_PORT)
  if wait_for_http "http://localhost:${WEB_PORT}/" 40; then
    WEB_HOME_STATUS=OK
  else
    echo "Basic web app home page not reachable"
    WEB_HOME_STATUS=FAIL
    FAILED=1
  fi
  # Try URL-based detection with a public sample image (best-effort)
  if curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:${WEB_PORT}/detect/url" \
       -H 'Content-Type: application/x-www-form-urlencoded' \
       --data "imageUrl=https://raw.githubusercontent.com/opencv/opencv/master/samples/data/lena.jpg" | grep -q '^200$'; then
    WEB_DETECT_URL_STATUS=OK
  else
    WEB_DETECT_URL_STATUS=FAIL
    # do not fail the whole suite if network is restricted
  fi
  stop_app "$WEB_PID"
fi

########################################
# Final Summary Table
########################################
printf "\n=== Spring Vision Test Summary ===\n"
printf "%-24s | %-10s | %-60s\n" "Task" "Result" "Details"
printf "%s\n" "--------------------------+------------+------------------------------------------------------------"

if [[ $RUN_CLI -eq 1 ]]; then
  printf "%-24s | %-10s | faces=%s\n" "CLI Detect selfie-a" "$([ ${DETECT_A_RC:-1} -eq 0 ] && echo OK || echo FAIL)" "${DETECT_A_COUNT:-N/A}"
  printf "%-24s | %-10s | faces=%s\n" "CLI Detect selfie-b" "$([ ${DETECT_B_RC:-1} -eq 0 ] && echo OK || echo FAIL)" "${DETECT_B_COUNT:-N/A}"
  printf "%-24s | %-10s | metric=%s distance=%s threshold=%s match=%s\n" "CLI Verify a vs b" "$([ ${VERIFY_RC:-1} -eq 0 ] && echo OK || echo FAIL)" "${VERIFY_METRIC:-N/A}" "${VERIFY_DISTANCE:-N/A}" "${VERIFY_THRESHOLD:-N/A}" "${VERIFY_MATCH:-N/A}"
  printf "%-24s | %-10s | embeddings=%s (truncated)\n" "CLI Embed selfie-a" "$([ ${EMBED_RC:-1} -eq 0 ] && echo OK || echo FAIL)" "${EMBED_COUNT:-N/A}"
  printf "%-24s | %-10s | output=selfie-a-obscured.jpg\n" "CLI Obscure selfie-a" "${OBSCURE_STATUS:-FAIL}" ""
fi

if [[ $RUN_API -eq 1 ]]; then
  printf "%-24s | %-10s | /api/vision/health on :8081\n" "API GWT health" "${GWT_STATUS:-FAIL}"
  printf "%-24s | %-10s | /api/vision/health on :8082\n" "API Vaadin health" "${VAADIN_STATUS:-FAIL}"
fi

if [[ $RUN_WEB -eq 1 ]]; then
  printf "%-24s | %-10s | GET / on :8083\n" "WEB home page" "${WEB_HOME_STATUS:-FAIL}"
  printf "%-24s | %-10s | POST /detect/url (best-effort)\n" "WEB URL detect" "${WEB_DETECT_URL_STATUS:-N/A}"
fi

# overall exit code
if [[ $FAILED -ne 0 ]]; then
  exit 1
fi
exit 0



