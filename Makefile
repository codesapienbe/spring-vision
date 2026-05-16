# Download all dependencies for offline use
default: build

.PHONY: build clean release test sync verify format default

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

# Run only the DjlVisionBackend integration test
test:
	@echo "Running DjlVisionBackend integration tests (core module) and VisionTool integration test (mcp module)..."
	# Run only in the core and mcp modules to avoid failing other modules that don't contain these tests
	mvn -pl core,mcp -am -q test || \
	( echo "Integration tests failed" && exit 1 )


verify: test
	@echo "Verifying project with Spotless and Checkstyle..."
	mvn spotless:check checkstyle:check -q || ( echo "Verification failed" && exit 1 )
	@echo "Verification completed successfully"

format:
	@echo "Formatting project with Spotless..."
	mvn spotless:apply -q || ( echo "Formatting failed" && exit 1 )
	@echo "Formatting completed successfully"

SPRINGVISION_DIR ?= $(HOME)/.springvision
MCP_JAR         := $(SPRINGVISION_DIR)/mcp-$(SPRING_VISION_VERSION).jar
MCP_ENTRY       := {"command":"java","args":["-jar","$(MCP_JAR)"]}

# Helper: upsert spring-vision entry into any JSON file that has a top-level mcpServers object
define upsert-mcp
	@if [ -f $(1) ]; then \
		jq --argjson entry '$(MCP_ENTRY)' '.mcpServers["spring-vision"] = $$entry' \
			$(1) > /tmp/_mcp_sync.tmp && mv /tmp/_mcp_sync.tmp $(1); \
		echo "  ✓ $(1)"; \
	else \
		echo "  – $(1) not found, skipping"; \
	fi
endef

sync:
	@echo "Building and syncing MCP jar for local testing..."
	mvn -pl mcp clean package -DskipTests -q || ( echo "MCP build failed!" && exit 1 )
	mkdir -p $(SPRINGVISION_DIR)
	cp mcp/target/mcp-$(SPRING_VISION_VERSION).jar $(MCP_JAR)
	@echo "Jar → $(MCP_JAR)"
	@echo "Registering spring-vision MCP server in agent configs:"
	$(call upsert-mcp,$(HOME)/.claude.json)
	$(call upsert-mcp,$(HOME)/.cursor/mcp.json)
	$(call upsert-mcp,$(HOME)/.gemini/settings.json)
