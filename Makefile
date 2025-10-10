# Download all dependencies for offline use
default: build

.PHONY: build run clean deploy release default

# Build target: Maven package and optional Docker image build
build:
	@echo "Building project: Maven install (will also build the docker image)"
	mvn clean install -DskipTests -q

verify:
	@echo "Testing project: Maven test"
	mvn clean test test

# Run the example (unchanged)
run:
	@echo "Running basic-face-detection example..."
	cd examples/basic-face-detection && mvn -o spring-boot:run

# Clean the project (unchanged)
clean:
	mvn clean
    docker image rm spring-vision:latest

# Deploy: push Docker image to registry (assumes docker is logged in and IMAGE is set to the full repo:tag)
deploy:
	@echo "Pushing Docker image spring-vision:latest to registry...";
	docker tag spring-vision:1.0 docker.io/codesapienbe/spring-vision:latest;
	docker push docker.io/codesapienbe/spring-vision:latest

# Release: deploy to Maven Central via Sonatype
release:
	@echo "Releasing artifacts to Maven Central..."
	mvn clean deploy -P release
