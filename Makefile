# Download all dependencies for offline use
default: build

.PHONY: build clean deploy release docs default release-local

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
	@echo "Releasing all modules to Maven Central with version $(SPRING_VISION_VERSION)..."; \
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false -DprocessAllModules=true; \
	mvn -B deploy -X -DskipTests || ( echo "Maven deploy failed!" && exit 1 );

release-local:
	@echo "Simulating release to local repo: $(LOCAL_REPO) with version $(SPRING_VISION_VERSION) (gpg.skip=$(GPG_SKIP), skipDockerBuild=$(SKIP_DOCKER_BUILD))"; \
	mkdir -p "$(LOCAL_REPO)"; \
	# Update versions across all modules
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false -DprocessAllModules=true; \
	# Deploy to local file-based repo. By default signing is skipped for local simulation; override GPG_SKIP to false to enable signing.
	mvn -B deploy -DskipTests \
		-Dgpg.skip=$(GPG_SKIP) \
		-DskipDockerBuild=$(SKIP_DOCKER_BUILD) \
		-DskipDocker=$(SKIP_DOCKER_BUILD) \
		-Dfabric8.skip=$(SKIP_DOCKER_BUILD) \
		-Ddocker.skip=$(SKIP_DOCKER_BUILD) \
		-DaltDeploymentRepository=local::default::file://$(LOCAL_REPO) || ( echo "Local deploy failed!" && exit 1 );

docs:
	@echo "Generating javadocs and creating report..."
	mvn javadoc:javadoc > javadocs.txt 2>&1 && \
	echo "Javadocs report generated successfully in javadocs.txt" || \
	echo "Javadocs generation completed with warnings/errors - see javadocs.txt for details"
