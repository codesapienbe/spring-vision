#!/bin/bash

# Build the project (FaceBytes)
mvn -pl spring-vision-facebytes -am clean install
if [ $? -ne 0 ]; then
    echo "Failed to build FaceBytes"
    exit 1
fi

