# Download all dependencies for offline use
default: build

.PHONY: build run clean deploy release docs default

# Load version from VERSION file
SPRING_VISION_VERSION := $(shell cat VERSION)

build:
	@echo "Building project: Maven install (will also build the docker image) - Version: $(SPRING_VISION_VERSION)";
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false;
	mvn clean install -DskipTests;

verify:
	@echo "Testing project: Maven test";
	mvn clean test;

run:
	@echo "Running MCP server on Docker..."
	docker run -p "8081:8081" --name spring-vision-mcp --detach codesapienbe/spring-vision:$(SPRING_VISION_VERSION);

clean:
	mvn clean -q && docker image rm spring-vision:$(SPRING_VISION_VERSION) spring-vision:latest || true

deploy:
	@echo "Pushing Docker image spring-vision:$(SPRING_VISION_VERSION) to registry...";
	docker tag spring-vision:$(SPRING_VISION_VERSION) docker.io/codesapienbe/spring-vision:latest;
	docker tag spring-vision:$(SPRING_VISION_VERSION) docker.io/codesapienbe/spring-vision:$(SPRING_VISION_VERSION);
	docker push docker.io/codesapienbe/spring-vision:latest;
	docker push docker.io/codesapienbe/spring-vision:$(SPRING_VISION_VERSION)

release:
	@echo "Releasing core,starter,mcp modules to Maven Central with version $(SPRING_VISION_VERSION)..."; \
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false; \
	mvn -B -pl core,starter,mcp -am clean deploy -DskipTests || \
		( echo "Maven deploy failed; you can re-run './deploy-to-central.sh' to release all modules" && exit 1 )

docs:
	@echo "Generating javadocs and creating report..."
	mvn javadoc:javadoc > javadocs.txt 2>&1 && \
	echo "Javadocs report generated successfully in javadocs.txt" || \
	echo "Javadocs generation completed with warnings/errors - see javadocs.txt for details"
