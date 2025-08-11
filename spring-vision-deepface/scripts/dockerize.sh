#!/usr/bin/env bash

cd ..

docker rm -f $(docker ps -a -q --filter "ancestor=deepface")

docker build -t deepface .

docker cp ~/.deepface/weights/. <CONTAINER_ID>:/root/.deepface/weights/

docker run -p 5005:5000 deepface

# or pull the pre-built image from docker hub and run it
# docker pull serengil/deepface
# docker run -p 5005:5000 serengil/deepface
