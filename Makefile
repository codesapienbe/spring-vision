.PHONY: install run stop restart test clean format sync help jdk verify bundle mcp-sync
.PHONY: unit-tests integration-tests memory-tests
.PHONY: $(addprefix unit-,$(MODULES)) $(addprefix integration-,$(MODULES))

# Target names follow one common vocabulary (install, run, stop, restart, test, unit-tests,
# integration-tests, format, clean, sync, jdk, help), so none has to be looked up. Targets
# specific to this project (bundle, verify, memory-tests, mcp-sync) come last in `make help`.

MODULES  := core starter mcp cli

# Load version from VERSION file
SPRING_VISION_VERSION := $(shell cat VERSION)

# ─── JDK resolution ─────────────────────────────────────────────────────────
# Resolve the JDK pinned in .sdkmanrc and export it so make targets use that JDK
# regardless of the shell's default. This makes `mvn`/`jbang` self-contained: no
# `sdk env` and no sdkman_auto_env required for make to build on the pinned JDK.
# Override by passing JAVA_HOME explicitly (e.g. `make install JAVA_HOME=...`).
SDKMAN_DIR       ?= $(HOME)/.sdkman
SDKMAN_JAVA      := $(shell sed -n 's/^java=//p' .sdkmanrc 2>/dev/null)
PINNED_JAVA_HOME := $(SDKMAN_DIR)/candidates/java/$(SDKMAN_JAVA)
ifneq ($(wildcard $(PINNED_JAVA_HOME)/bin/java),)
  export JAVA_HOME := $(PINNED_JAVA_HOME)
  export PATH      := $(PINNED_JAVA_HOME)/bin:$(PATH)
else
  $(warning .sdkmanrc pins java=$(SDKMAN_JAVA) but $(PINNED_JAVA_HOME) is not installed — run 'sdk env install'. Falling back to default JDK.)
endif

# ─── build tuning ─────────────────────────────────────────────────────────────
# Heap + GC for the Maven JVM and
# Maven's own thread count, sized to leave the machine usable while the build runs.
# -T1 is a serial reactor; raise it per invocation (`make install MVN_THREADS=1C`).
# The surefire fork's heap is set in the root pom (argLine), not here.
JAVA_OPTS   ?= -Xmx4g -XX:-UseG1GC -XX:+UseParallelGC
MVN_THREADS ?= 1
export MAVEN_OPTS := $(JAVA_OPTS)

# Local signing control (can be overridden on the make command line)
GPG_SKIP ?= true

# Set GPU=true (e.g. `make install GPU=true`, `make bundle GPU=true`) to build every
# native library that has a CUDA variant — DJL PyTorch and ONNX Runtime — against it
# instead of CPU-only. One flag for both, instead of remembering `-P gpu` yourself.
# See docs/configuration/gpu.md. Requires a Linux/Windows x86_64 host — neither DJL nor
# ONNX Runtime publish macOS or ARM64 CUDA builds.
GPU ?= false
GPU_PROFILE := $(if $(filter true,$(GPU)),-Pgpu,)
comma := ,
IMAGE_TAG := spring-vision-mcp:$(SPRING_VISION_VERSION)$(if $(filter true,$(GPU)),-gpu,)

# ─── install ──────────────────────────────────────────────────────────────────

install:
	mvn versions:set -DnewVersion=$(SPRING_VISION_VERSION) -DgenerateBackupPoms=false -DprocessAllModules=true -q
	mvn clean install -DskipTests -T$(MVN_THREADS) -Dgpg.skip=$(GPG_SKIP) -Pdownload-models $(GPU_PROFILE)

# ─── run / stop / restart ─────────────────────────────────────────────────────

# The one runnable service here is the MCP server, so there is no SERVICE variable, and it
# has no Spring profiles, so there is no ENV/PROFILE either.
# `run` brings up the local Keycloak it authenticates against (keycloak/README.md),
# builds, and starts the server in the foreground with JBang.
run:
	docker compose up -d keycloak
	@$(MAKE) install GPU=$(GPU)
	jbang run.java

stop:
	docker compose stop keycloak

restart:
	@$(MAKE) stop
	@$(MAKE) run GPU=$(GPU)

# ─── staged testing ───────────────────────────────────────────────────────────

# Unit -> Integration, and memory-heavy suites are asked
# for by name (memory-tests) rather than inherited by anyone typing `make test`.
# "Integration" is the naming convention the root pom already uses for Failsafe
# (*IntegrationTest, *IT); everything else is a unit test. The memory-intensive tag
# stays excluded by the root pom's surefire config for both stages. `!*$$*` drops JUnit
# @Nested classes as separate entries: an explicit -Dtest replaces surefire's default
# `**/*$$*` exclude, and without it every nested class runs twice (once on its own, once
# inside its parent) — 422 runs instead of the 253 a plain `mvn test` does in core.
UNIT_TESTS        := !*IntegrationTest,!*IT,!*$$*
INTEGRATION_TESTS := *IntegrationTest,*IT,!*$$*
TEST_FLAGS        := -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true -B -ntp -q

test:
	@echo "🚀 Starting staged tests"
	@$(MAKE) unit-tests integration-tests
	@echo "✅ All test stages completed successfully."

unit-tests:
	@echo "🔬 Running Unit Tests..."
	@mvn test -Dtest='$(UNIT_TESTS)' $(TEST_FLAGS)

integration-tests: unit-tests
	@echo "🔗 Running Integration Tests..."
	@mvn test -Dtest='$(INTEGRATION_TESTS)' $(TEST_FLAGS)

# One module. `-am` is not optional: starter and mcp depend on core, and without building
# it from source the run resolves whatever stale artifact happens to be in ~/.m2. The
# price is that the dependencies' matching tests run too.
unit-%:
	@echo "🔬 Running Unit Tests for $*..."
	@mvn test -pl $* -am -Dtest='$(UNIT_TESTS)' $(TEST_FLAGS)

integration-%:
	@echo "🔗 Running Integration Tests for $*..."
	@mvn test -pl $* -am -Dtest='$(INTEGRATION_TESTS)' $(TEST_FLAGS)

memory-tests:
	@echo "🧠 Running memory-intensive tests..."
	@mvn test -Dgroups=memory-intensive -Dsurefire.excludedGroups= $(TEST_FLAGS)

# ─── format ───────────────────────────────────────────────────────────────────

format:
	mvn spotless:apply

# ─── clean ────────────────────────────────────────────────────────────────────

clean:
	mvn clean

# ─── sync ─────────────────────────────────────────────────────────────────────

sync:
	git fetch origin CSNET
	git merge origin/CSNET

# ─── jdk ──────────────────────────────────────────────────────────────────────

jdk:
	@echo "Pinned (.sdkmanrc): $(SDKMAN_JAVA)"
	@echo "JAVA_HOME:          $(JAVA_HOME)"
	@echo "MAVEN_OPTS:         $(MAVEN_OPTS)"
	@echo "MVN_THREADS:        $(MVN_THREADS)"
	@"$(JAVA_HOME)/bin/java" -version

# ─── spring-vision only ───────────────────────────────────────────────────────

verify:
	mvn spotless:check checkstyle:check -q

# The jar is built inside the image (see Dockerfile), so its native libraries match the image's
# platform. PLATFORM picks it, e.g. `make bundle PLATFORM=linux/amd64`; empty = Docker's default.
PLATFORM ?=
bundle:
	docker build $(if $(PLATFORM),--platform $(PLATFORM),) \
		--build-arg MAVEN_PROFILES=download-models$(if $(filter true,$(GPU)),$(comma)gpu,) \
		-t $(IMAGE_TAG) .
	@echo "Built image: $(IMAGE_TAG)"
ifeq ($(GPU),true)
	@echo "Run it with: docker run --rm --gpus all -p 8080:8080 -e KEYCLOAK_ISSUER_URI=<issuer> $(IMAGE_TAG)"
	@echo "(requires the NVIDIA driver + NVIDIA Container Toolkit on the host — see docs/configuration/gpu.md)"
else
	@echo "Run it with: docker run --rm -p 8080:8080 -e KEYCLOAK_ISSUER_URI=<issuer> $(IMAGE_TAG)"
endif

SPRINGVISION_DIR ?= $(HOME)/.springvision
MCP_JAR         := $(SPRINGVISION_DIR)/mcp-$(SPRING_VISION_VERSION).jar
# The client launches the server itself, over stdio (no Keycloak, no port); the pinned JDK is
# named explicitly because the client's PATH may hold an older java.
MCP_JAVA        := $(if $(wildcard $(PINNED_JAVA_HOME)/bin/java),$(PINNED_JAVA_HOME)/bin/java,java)
CLAUDE_CLI      := $(shell command -v claude 2>/dev/null)
MCP_ENTRY       := {"type":"stdio","command":"$(MCP_JAVA)","args":["-jar","$(MCP_JAR)","--spring.profiles.active=stdio"]}

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

# Was `make sync`; `sync` now means "merge the trunk into this branch".
mcp-sync:
	@echo "Building and syncing MCP jar for local testing..."
	mvn -pl core,mcp -am clean install -DskipTests -Dgpg.skip=$(GPG_SKIP) -Pdownload-models -q
	mkdir -p $(SPRINGVISION_DIR)
	cp mcp/target/mcp-$(SPRING_VISION_VERSION).jar $(MCP_JAR)
	@echo "Jar → $(MCP_JAR)"
	@echo "Registering spring-vision MCP server in agent configs:"
# Through the claude CLI when present, so a custom CLAUDE_CONFIG_DIR is respected.
ifneq ($(CLAUDE_CLI),)
	@claude mcp remove --scope user spring-vision >/dev/null 2>&1 || true
	@claude mcp add-json --scope user spring-vision '$(MCP_ENTRY)' >/dev/null && echo "  ✓ claude (user scope)"
else
	$(call upsert-mcp,$(HOME)/.claude.json)
endif
	$(call upsert-mcp,$(HOME)/.cursor/mcp.json)
	$(call upsert-mcp,$(HOME)/.gemini/settings.json)

# ─── help ─────────────────────────────────────────────────────────────────────

help:
	@echo ""
	@echo "Usage: make <target> [GPU=false]"
	@echo ""
	@echo "  install           versions:set from VERSION + mvn clean install -DskipTests -T$(MVN_THREADS) -Pdownload-models"
	@echo "  run               Start local Keycloak, build, run the MCP server with JBang"
	@echo "  stop              Stop the local Keycloak"
	@echo "  restart           stop + run"
	@echo "  test              Staged tests (Unit -> Integration)"
	@echo "  unit-tests        Unit tests, all modules (unit-<module> for one: $(MODULES))"
	@echo "  integration-tests Integration tests, all modules (integration-<module> for one)"
	@echo "  format            Spotless apply"
	@echo "  clean             mvn clean"
	@echo "  sync              Merge origin/CSNET into current branch"
	@echo "  jdk               Show the JDK + build tuning make resolved"
	@echo ""
	@echo "spring-vision only:"
	@echo "  verify            Spotless + Checkstyle check"
	@echo "  memory-tests      Tests tagged memory-intensive — not part of 'make test'"
	@echo "  bundle            Docker image $(IMAGE_TAG), jar built inside it (PLATFORM=linux/amd64 to pick one)"
	@echo "  mcp-sync          Build the MCP jar into ~/.springvision and register it in agent configs"
	@echo ""
	@echo "Build tuning (override on the command line):"
	@echo "  JAVA_OPTS         Maven JVM opts                           ($(JAVA_OPTS))"
	@echo "  MVN_THREADS       Maven -T value for install               ($(MVN_THREADS))"
	@echo ""
