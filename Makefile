# Download all dependencies for offline use
default: build

.PHONY: build clean deploy release docs test default

# Load version from VERSION file
SPRING_VISION_VERSION := $(shell cat VERSION)

# Local simulation repo and signing control (can be overridden on the make command line)
LOCAL_REPO ?= $(CURDIR)/target/local-repo
GPG_SKIP ?= true
SKIP_DOCKER_BUILD ?= true

clean:
	mvn clean -q && docker image rm spring-vision-mcp:$(SPRING_VISION_VERSION) spring-vision-mcp:latest || true

build:
	@echo "Building project: Maven install (will also build the docker image) - Version: $(SPRING_VISION_VERSION)";
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false -DprocessAllModules=true;
	mvn clean install -DskipTests || ( echo "Maven install failed!" && exit 1 );

deploy:
	@echo "Tagging local image so run/deploy targets reference the same name";
	docker tag spring-vision-base:$(SPRING_VISION_VERSION) docker.io/codesapienbe/spring-vision-base:$(SPRING_VISION_VERSION) || true;
	docker tag spring-vision-base:$(SPRING_VISION_VERSION) docker.io/codesapienbe/spring-vision-base:latest || true;
	@echo "Pushing Docker image spring-vision-base:$(SPRING_VISION_VERSION) to registry";
	docker push docker.io/codesapienbe/spring-vision-base:$(SPRING_VISION_VERSION);
	docker push docker.io/codesapienbe/spring-vision-base:latest;
	@echo "Tagging local image so run/deploy targets reference the same name";
	docker tag spring-vision-mcp:$(SPRING_VISION_VERSION) docker.io/codesapienbe/spring-vision-mcp:$(SPRING_VISION_VERSION) || true;
	docker tag spring-vision-mcp:$(SPRING_VISION_VERSION) docker.io/codesapienbe/spring-vision-mcp:latest || true;
	@echo "Pushing Docker image spring-vision-mcp:$(SPRING_VISION_VERSION) to registry...";
	docker push docker.io/codesapienbe/spring-vision-mcp:$(SPRING_VISION_VERSION)
	docker push docker.io/codesapienbe/spring-vision-mcp:latest

release:
	@echo "Releasing all modules to GitHub Packages with version $(SPRING_VISION_VERSION)..."; \
	# Delete local tag if it exists (to overwrite it)
	git tag -d v$(SPRING_VISION_VERSION) 2>/dev/null || true; \
	# Create annotated git tag
	git tag -a v$(SPRING_VISION_VERSION) -m "Release v$(SPRING_VISION_VERSION)"; \
	# Check if tag exists in remote and force push if needed
	if git ls-remote --tags origin | grep -q "refs/tags/v$(SPRING_VISION_VERSION)"; then \
		echo "Tag v$(SPRING_VISION_VERSION) exists in remote, force pushing..."; \
		git push --force origin v$(SPRING_VISION_VERSION) || ( echo "Failed to force push tag to origin" && exit 1 ); \
	else \
		echo "Tag v$(SPRING_VISION_VERSION) does not exist in remote, pushing..."; \
		git push origin v$(SPRING_VISION_VERSION) || ( echo "Failed to push tag to origin" && exit 1 ); \
	fi

docs:
	@echo "Generating javadocs and creating report..."
	mvn javadoc:javadoc > javadocs.txt 2>&1 && \
	echo "Javadocs report generated successfully in javadocs.txt" || \
	echo "Javadocs generation completed with warnings/errors - see javadocs.txt for details"

# Run only the DjlVisionBackend integration test
test:
	@echo "Running DjlVisionBackend integration test..."
	mvn -q -Dtest=io.github.codesapienbe.springvision.core.djl.DjlVisionBackendIntegrationTest test || \
	( echo "Integration test failed" && exit 1 )
