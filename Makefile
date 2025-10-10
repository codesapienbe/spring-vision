# Download all dependencies for offline use
deps:
	mvn -B dependency:go-offline

default: build

.PHONY: build run clean deps deploy release version default maven-deploy

# Default settings file for Maven (can be overridden with make release SETTINGS=/path/to/settings.xml)
SETTINGS ?= $(HOME)/.m2/settings.xml

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

# Maven deploy: deploy artifacts to Maven Central
maven-deploy:
	@echo "Deploying Maven artifacts to Maven Central...";
	@if [ -f "$(SETTINGS)" ]; then \
		echo "Using settings file: $(SETTINGS)"; \
		mvn -B -T 8 -s "$(SETTINGS)" -Prelease -DskipTests -pl "!spring-vision-examples" clean deploy; \
	else \
		echo "WARNING: Settings file $(SETTINGS) not found. Deploying without custom settings..."; \
		mvn -B -T 8 -Prelease -DskipTests -pl "!spring-vision-examples" clean deploy; \
	fi

# Release: run maven deploy with release profile (assumes $(SETTINGS) is configured)
release: maven-deploy
	@echo "Release complete!"

# Snapshot: deploy snapshot version to Maven Central
snapshot:
	@echo "Deploying SNAPSHOT version to Maven Central...";
	@CURRENT_VERSION=$$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout); \
	if [[ ! "$$CURRENT_VERSION" == *-SNAPSHOT ]]; then \
		echo "Converting version to SNAPSHOT..."; \
		mvn -B versions:set -DnewVersion=$${CURRENT_VERSION}-SNAPSHOT -DgenerateBackupPoms=false; \
	fi
	@$(MAKE) maven-deploy

# Get the Spring Vision docker image tag (repository name's last path component starts with 'spring-vision')
version:
	@echo "Looking up Docker image tag for repository name starting with 'spring-vision'..."
	@./scripts/get-spring-vision-image-tag.sh
