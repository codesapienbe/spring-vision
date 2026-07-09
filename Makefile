# Download all dependencies for offline use
default: install

.PHONY: install clean release test sync verify format bundle default run

# Load version from VERSION file
SPRING_VISION_VERSION := $(shell cat VERSION)

# Local simulation repo and signing control (can be overridden on the make command line)
LOCAL_REPO ?= $(CURDIR)/target/local-repo
GPG_SKIP ?= true

# Set GPU=true (e.g. `make install GPU=true`, `make bundle GPU=true`) to build every
# native library that has a CUDA variant — DJL PyTorch and ONNX Runtime — against it
# instead of CPU-only. One flag for both, instead of remembering `-P gpu` yourself.
# See docs/configuration/gpu.md. Requires a Linux/Windows x86_64 host — neither DJL nor
# ONNX Runtime publish macOS or ARM64 CUDA builds.
GPU ?= false
GPU_PROFILE := $(if $(filter true,$(GPU)),-Pgpu,)
IMAGE_TAG := spring-vision-mcp:$(SPRING_VISION_VERSION)$(if $(filter true,$(GPU)),-gpu,)

clean:
	mvn clean -q

install:
	@echo "Building project: Maven install - Version: $(SPRING_VISION_VERSION) (GPU=$(GPU))";
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false -DprocessAllModules=true;
	mvn clean install -DskipTests -Dgpg.skip=$(GPG_SKIP) -Pdownload-models $(GPU_PROFILE) || ( echo "Maven install failed!" && exit 1 );

run:
	@echo "Starting local dev services (Keycloak, see keycloak/README.md)...";
	docker compose up -d keycloak || ( echo "Failed to start Keycloak" && exit 1 );
	@echo "Running Spring Vision MCP server locally with JBang (GPU=$(GPU))";
	# Ensure the project is built
	$(MAKE) install GPU=$(GPU) || ( echo "Build failed" && exit 1 );
	# Run the MCP server using JBang runner
	jbang run.java;

bundle:
	@echo "Building mcp module and bundling it into a Docker image (GPU=$(GPU))...";
	$(MAKE) install GPU=$(GPU) || ( echo "Build failed" && exit 1 );
	docker build --build-arg SPRING_VISION_VERSION=$(SPRING_VISION_VERSION) -t $(IMAGE_TAG) .;
	@echo "Built image: $(IMAGE_TAG)";
ifeq ($(GPU),true)
	@echo "Run it with: docker run --rm --gpus all -p 8080:8080 -e KEYCLOAK_ISSUER_URI=<issuer> $(IMAGE_TAG)"
	@echo "(requires the NVIDIA driver + NVIDIA Container Toolkit on the host — see docs/configuration/gpu.md)"
else
	@echo "Run it with: docker run --rm -p 8080:8080 -e KEYCLOAK_ISSUER_URI=<issuer> $(IMAGE_TAG)"
endif

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
	mvn -pl core,mcp -am clean install -DskipTests -Dgpg.skip=$(GPG_SKIP) -q || ( echo "MCP build failed!" && exit 1 )
	mkdir -p $(SPRINGVISION_DIR)
	cp mcp/target/mcp-$(SPRING_VISION_VERSION).jar $(MCP_JAR)
	@echo "Jar → $(MCP_JAR)"
	@echo "Registering spring-vision MCP server in agent configs:"
	$(call upsert-mcp,$(HOME)/.claude.json)
	$(call upsert-mcp,$(HOME)/.cursor/mcp.json)
	$(call upsert-mcp,$(HOME)/.gemini/settings.json)
