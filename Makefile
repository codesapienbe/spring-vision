# Download all dependencies for offline use
default: build

.PHONY: build clean release docs test sync default

# Load version from VERSION file
SPRING_VISION_VERSION := $(shell cat VERSION)

# Local simulation repo and signing control (can be overridden on the make command line)
LOCAL_REPO ?= $(CURDIR)/target/local-repo
GPG_SKIP ?= true

clean:
	mvn clean -q

build:
	@echo "Building project: Maven install - Version: $(SPRING_VISION_VERSION)";
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false -DprocessAllModules=true;
	mvn clean install -DskipTests -Dgpg.skip=$(GPG_SKIP) -Pdownload-models || ( echo "Maven install failed!" && exit 1 );

run:
	@echo "Running Spring Vision MCP server locally with JBang";
	# Ensure the project is built
	$(MAKE) build || ( echo "Build failed" && exit 1 );
	# Run the MCP server using JBang runner
	jbang run.java;

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
	@echo "Running DjlVisionBackend integration tests (core module) and VisionTool integration test (mcp module)..."
	# Run only in the core and mcp modules to avoid failing other modules that don't contain these tests
	mvn -pl core,mcp -am -q -Dtest=io.github.codesapienbe.springvision.core.djl.DjlVisionBackendIntegrationTest,io.github.codesapienbe.springvision.core.djl.DjlVisionBackendModelAvailabilityTest,io.github.codesapienbe.springvision.mcp.VisionToolIntegrationTest test || \
	( echo "Integration tests failed" && exit 1 )

sync:
	@echo "Building and syncing MCP jar for local testing..."
	# Build the mcp module
	mvn -pl mcp clean package -DskipTests -q || ( echo "MCP build failed!" && exit 1 )
	# Create the target directory if it doesn't exist
	mkdir -p /home/codesapienbe/.springvision
	# Copy the compiled jar
	cp mcp/target/mcp-$(SPRING_VISION_VERSION).jar /home/codesapienbe/.springvision/mcp-$(SPRING_VISION_VERSION).jar
	@echo "MCP jar synced to /home/codesapienbe/.springvision/mcp-$(SPRING_VISION_VERSION).jar"
	# Update version in .cursor/mcp.json if it exists
	@if [ -f /home/codesapienbe/.cursor/mcp.json ]; then \
		echo "Updating version in .cursor/mcp.json..."; \
		jq --arg version "$(SPRING_VISION_VERSION)" '.mcpServers."spring-vision".args[0] = "/home/codesapienbe/.springvision/mcp-" + $$version + ".jar"' /home/codesapienbe/.cursor/mcp.json > /tmp/mcp.json.tmp && mv /tmp/mcp.json.tmp /home/codesapienbe/.cursor/mcp.json; \
		echo "Updated .cursor/mcp.json with version $(SPRING_VISION_VERSION)"; \
	else \
		echo ".cursor/mcp.json not found, skipping version update"; \
	fi
