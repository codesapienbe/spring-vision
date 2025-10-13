# Download all dependencies for offline use
default: build

.PHONY: build run dev clean deploy release docs default

# Which modules to release by default (comma-separated list for Maven -pl)
RELEASE_MODULES := core,starter

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

clean:
	mvn clean -q && docker image rm spring-vision:latest;

deploy:
	@echo "Pushing Docker image spring-vision:latest to registry...";
	docker tag spring-vision:1.0 docker.io/codesapienbe/spring-vision:latest &&\
	docker push docker.io/codesapienbe/spring-vision:latest

release:
	@echo "Releasing only '$(RELEASE_MODULES)' modules to Maven Central..."
	# If you have a release profile, append -Prelease or similar as needed.
	@mvn -B -pl $(RELEASE_MODULES) -am clean deploy -DskipTests || \
		( echo "Maven deploy failed; you can re-run './deploy-to-central.sh' to release all modules" && exit 1 )

docs:
	@echo "Generating javadocs and creating report..."
	mvn javadoc:javadoc > javadocs.txt 2>&1 && \
	echo "Javadocs report generated successfully in javadocs.txt" || \
	echo "Javadocs generation completed with warnings/errors - see javadocs.txt for details"
