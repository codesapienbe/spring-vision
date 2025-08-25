#!/bin/bash

export SPRING_DOCKER_COMPOSE_ENABLED=false
mvn -T 1C -DskipTests clean install

if [ $? -ne 0 ]; then
  echo "BUILD failed"
  exit 1
fi

echo "BUILD completed successfully"