# Download all dependencies for offline use
default: build

.PHONY: build clean deploy release docs default

# Load version from VERSION file
SPRING_VISION_VERSION := $(shell cat VERSION)

clean:
	mvn clean -q && docker image rm spring-vision-mcp:$(SPRING_VISION_VERSION) spring-vision-mcp:latest || true

build:
	@echo "Building project: Maven install (will also build the docker image) - Version: $(SPRING_VISION_VERSION)";
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false;
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
	@echo "Releasing core,starter,mcp modules to Maven Central with version $(SPRING_VISION_VERSION)..."; \
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false; \
	mvn -B -pl core,starter -am deploy -X -DskipTests || ( echo "Maven deploy failed!" && exit 1 );

docs:
	@echo "Generating javadocs and creating report..."
	mvn javadoc:javadoc > javadocs.txt 2>&1 && \
	echo "Javadocs report generated successfully in javadocs.txt" || \
	echo "Javadocs generation completed with warnings/errors - see javadocs.txt for details"
