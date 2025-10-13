# Download all dependencies for offline use
default: build

.PHONY: build run dev clean deploy release docs default

build:
	@echo "Building project: Maven install (will also build the docker image)"
	mvn clean install -DskipTests

verify:
	@echo "Testing project: Maven test"
	mvn clean test

run:
	@echo "Running MCP server on Docker..."
	docker run -i --rm codesapienbe/spring-vision:latest

dev:
	@echo "Running MCP server on dev..."
	java -jar /home/codesapienbe/Projects/spring-vision/mcp/target/mcp-1.0.jar

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
	@echo "Releasing artifacts to Maven Central via deploy-to-central.sh..."
	@./deploy-to-central.sh

# Generate javadocs and create report
docs:
	@echo "Generating javadocs and creating report..."
	mvn javadoc:javadoc > javadocs.txt 2>&1 && \
	echo "Javadocs report generated successfully in javadocs.txt" || \
	echo "Javadocs generation completed with warnings/errors - see javadocs.txt for details"
