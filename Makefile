# Download all dependencies for offline use
default: build

.PHONY: build run clean deploy release docs default

# Build target: Maven package and optional Docker image build
build:
	@echo "Building project: Maven install (will also build the docker image)"
	mvn clean install -DskipTests

verify:
	@echo "Testing project: Maven test"
	mvn clean test

# Run the MCP server
run:
	@echo "Finding an available port..."
	@PORT=$$(python3 -c "import socket; s=socket.socket(); s.bind((\'\',0)); print(s.getsockname()[1]); s.close()"); \
	echo "Running MCP server on port $$PORT"; \
	cd mcp && mvn spring-boot:run -Dserver.port=$$PORT

# Clean the project
clean:
	mvn clean -q && docker image rm spring-vision:latest;

# Deploy: push Docker image to registry (assumes docker is logged in and IMAGE is set to the full repo:tag)
deploy:
	@echo "Pushing Docker image spring-vision:latest to registry...";
	docker tag spring-vision:1.0 docker.io/codesapienbe/spring-vision:latest &&\
	docker push docker.io/codesapienbe/spring-vision:latest

# Release: deploy to Maven Central via Sonatype
release:
	@echo "Releasing artifacts to Maven Central..."
	mvn clean deploy -P release

# Generate javadocs and create report
docs:
	@echo "Generating javadocs and creating report..."
	mvn javadoc:javadoc > javadocs.txt 2>&1 && \
	echo "Javadocs report generated successfully in javadocs.txt" || \
	echo "Javadocs generation completed with warnings/errors - see javadocs.txt for details"
