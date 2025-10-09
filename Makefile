# Download all dependencies for offline use
deps:
	mvn -B dependency:go-offline

default: build

.PHONY: build run clean deps deploy release version default

# Build target: Maven package and optional Docker image build
build:
	@echo "Building project: Maven install (will also build the docker image)"
	mvn -B -T 8 install -DskipTests

verify:
	@echo "Testing project: Maven test"
	mvn -B test

# Run the example (unchanged)
run:
	@echo "Running basic-face-detection example..."
	cd spring-vision-examples/basic-face-detection && mvn -o spring-boot:run

# Clean the project (unchanged)
clean:
	mvn -B clean

# Deploy: push Docker image to registry (assumes docker is logged in and IMAGE is set to the full repo:tag)
deploy:
	@echo "Pushing Docker image spring-vision:latest to registry...";
	docker tag spring-vision:1.0 docker.io/codesapienbe/spring-vision:latest;
	docker push docker.io/codesapienbe/spring-vision:latest

# Release: run maven deploy (assumes $(SETTINGS) is configured)
release:
	@echo "Releasing Maven artifacts (deploy) using settings $(SETTINGS)...";
	mvn -B -T 8 -DskipTests -pl "!spring-vision-examples" clean deploy

# Get the Spring Vision docker image tag (repository name's last path component starts with 'spring-vision')
version:
	@echo "Looking up Docker image tag for repository name starting with 'spring-vision'..."
	@./scripts/get-spring-vision-image-tag.sh
