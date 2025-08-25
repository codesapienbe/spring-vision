#!/bin/bash

# Build all modules once to ensure latest APIs are installed locally
SPRING_DOCKER_COMPOSE_ENABLED=false \
SERVER_PORT=9999

FAILED=0

# DETECT A (CSV for easy parsing)
DETECT_A_OUT=$(SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -q -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
  -Dspring-boot.run.arguments="--detect ~/mockdata/spring-vision/selfie-a.jpg --format csv" 2>/dev/null)
DETECT_A_RC=$?
if [ $DETECT_A_RC -ne 0 ]; then
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
  -Dspring-boot.run.arguments="--detect ~/mockdata/spring-vision/selfie-b.jpg --format csv" 2>/dev/null)
DETECT_B_RC=$?
if [ $DETECT_B_RC -ne 0 ]; then
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
  -Dspring-boot.run.arguments="--verify ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/selfie-b.jpg --metric cosine --format csv" 2>/dev/null)
VERIFY_RC=$?
if [ $VERIFY_RC -ne 0 ]; then
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
  -Dspring-boot.run.arguments="--embed ~/mockdata/spring-vision/selfie-a.jpg --format csv --truncate 8" 2>/dev/null)
EMBED_RC=$?
if [ $EMBED_RC -ne 0 ]; then
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
  -Dspring-boot.run.arguments="--obscure ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/selfie-a-obscured.jpg" 2>/dev/null
OBSCURE_RC=$?
if [ $OBSCURE_RC -ne 0 ]; then
  echo "OBSCURE failed"
  OBSCURE_STATUS=FAIL
  FAILED=1
fi

# ---- Parse outputs (only valid CSV rows) ----
# Detect counts: count lines starting with a numeric index after the header
if [ $DETECT_A_RC -eq 0 ]; then
  DETECT_A_COUNT=$(echo "$DETECT_A_OUT" | awk -F, 'BEGIN{flag=0} /^Index,Confidence/{flag=1; next} flag && $1 ~ /^[0-9]+$/{c++} END{print c+0}')
else
  DETECT_A_COUNT="N/A"
fi
if [ $DETECT_B_RC -eq 0 ]; then
  DETECT_B_COUNT=$(echo "$DETECT_B_OUT" | awk -F, 'BEGIN{flag=0} /^Index,Confidence/{flag=1; next} flag && $1 ~ /^[0-9]+$/{c++} END{print c+0}')
else
  DETECT_B_COUNT="N/A"
fi

# Verify fields
if [ $VERIFY_RC -eq 0 ]; then
  VERIFY_LINE=$(echo "$VERIFY_OUT" | awk -F, '/^(cosine|euclidean),/{print $0; exit}')
  VERIFY_METRIC=$(echo "$VERIFY_LINE" | awk -F, '{print $1}')
  VERIFY_DISTANCE=$(echo "$VERIFY_LINE" | awk -F, '{print $2}')
  VERIFY_THRESHOLD=$(echo "$VERIFY_LINE" | awk -F, '{print $3}')
  VERIFY_MATCH=$(echo "$VERIFY_LINE" | awk -F, '{print $4}')
else
  VERIFY_METRIC="N/A"; VERIFY_DISTANCE="N/A"; VERIFY_THRESHOLD="N/A"; VERIFY_MATCH="N/A"
fi

# Embed count
if [ $EMBED_RC -eq 0 ]; then
  EMBED_COUNT=$(echo "$EMBED_OUT" | awk -F, 'BEGIN{flag=0} /^[Ii]ndex,[Cc]onfidence/{flag=1; next} flag && $1 ~ /^[0-9]+$/{c++} END{print c+0}')
else
  EMBED_COUNT="N/A"
fi

# ---- Final Summary Table ----
printf "\n=== Spring Vision CLI Test Summary ===\n"
printf "%-20s | %-10s | %-40s\n" "Test" "Result" "Details"
printf "%s\n" "---------------------+------------+------------------------------------------"
printf "%-20s | %-10s | faces=%s\n" "Detect selfie-a" "$([ $DETECT_A_RC -eq 0 ] && echo OK || echo FAIL)" "${DETECT_A_COUNT}"
printf "%-20s | %-10s | faces=%s\n" "Detect selfie-b" "$([ $DETECT_B_RC -eq 0 ] && echo OK || echo FAIL)" "${DETECT_B_COUNT}"
printf "%-20s | %-10s | metric=%s distance=%s threshold=%s match=%s\n" "Verify a vs b" "$([ $VERIFY_RC -eq 0 ] && echo OK || echo FAIL)" "${VERIFY_METRIC}" "${VERIFY_DISTANCE}" "${VERIFY_THRESHOLD}" "${VERIFY_MATCH}"
printf "%-20s | %-10s | embeddings=%s (truncated)\n" "Embed selfie-a" "$([ $EMBED_RC -eq 0 ] && echo OK || echo FAIL)" "${EMBED_COUNT}"
printf "%-20s | %-10s | output=selfie-a-obscured.jpg\n" "Obscure selfie-a" "${OBSCURE_STATUS}" ""

# overall exit code
if [ $FAILED -ne 0 ]; then
  exit 1
fi
exit 0



