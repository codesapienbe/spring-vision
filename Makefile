# Download all dependencies for offline use
default: build

.PHONY: build clean deploy release docs test installer default

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
	mvn clean install -DskipTests || ( echo "Maven install failed!" && exit 1 );

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

# Build the CLI installer JAR
installer:
	@echo "Building Spring Vision CLI installer..."
	mvn clean package -pl cli -am -DskipTests || ( echo "CLI installer build failed!" && exit 1 )
	@echo "CLI installer built: cli/target/cli-0.0.1.jar"
	@echo "To use: java -jar cli/target/cli-0.0.1.jar --help"

# Run only the DjlVisionBackend integration test
test:
	@echo "Running DjlVisionBackend integration tests (core module)..."
	# Run only in the core module to avoid failing other modules that don't contain these tests
	mvn -pl core -am -q -Dtest=io.github.codesapienbe.springvision.core.djl.DjlVisionBackendIntegrationTest,io.github.codesapienbe.springvision.core.djl.DjlVisionBackendModelAvailabilityTest test || \
	( echo "Integration tests failed" && exit 1 )
