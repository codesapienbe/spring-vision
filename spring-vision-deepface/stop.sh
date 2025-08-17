#!/bin/bash

echo "Stopping DeepFace pipeline services..."

cd spring-producer
docker-compose down

echo "Services stopped successfully!"
