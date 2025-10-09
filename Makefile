# Download all dependencies for offline use
deps:
	mvn -B dependency:go-offline

default: build

# Variables for docker build (can be overridden on the make command line)
IMAGE ?= spring-vision-mcp:latest
DOCKER_CONTEXT ?= spring-vision-mcp
DOCKERFILE ?= $(DOCKER_CONTEXT)/Dockerfile
NO_DOCKER ?= 0

# Maven control flags
# SKIP_TESTS controls running tests during Maven builds
SKIP_TESTS ?= 0
# MAVEN_THREADS is forwarded to Maven -T (default 1C)
MAVEN_THREADS ?= 1C

# Dry run (prints planned actions instead of executing) - set DRY_RUN=1 to enable
DRY_RUN ?= 0
# Skip interactive confirm for nexus release (set SKIP_NEXUS_CONFIRM=1 to bypass prompt)
SKIP_NEXUS_CONFIRM ?= 0

# Local maven settings (can be overridden)
SETTINGS ?= $(HOME)/.m2/settings.xml

.PHONY: build run clean deps check-settings check-gpg docker-push-local maven-deploy nexus-release deploy default

# Build target (keeps existing behavior)
build:
	@echo "Starting full build: Maven -> Docker"
	@if [ "$(DRY_RUN)" = "1" ]; then \
		echo "DRY_RUN=1: would run 'mvn -B -e -T $(MAVEN_THREADS) clean install [ -DskipTests if SKIP_TESTS=1 ]' and then docker build (unless NO_DOCKER=1)"; \
		true; \
	else \
		MVN_FLAGS=""; \
		if [ "$(SKIP_TESTS)" = "1" ]; then MVN_FLAGS="-DskipTests"; fi; \
		mvn -B -e -T $(MAVEN_THREADS) clean install $$MVN_FLAGS; \
		MVN_RC=$$?; \
		if [ $$MVN_RC -ne 0 ]; then \
			echo "Maven build failed with exit code $$MVN_RC"; \
			exit $$MVN_RC; \
		fi; \
		if [ "$(NO_DOCKER)" = "0" ]; then \
			echo "Building Docker image $(IMAGE) from $(DOCKERFILE)..."; \
			docker build -f $(DOCKERFILE) -t $(IMAGE) $(DOCKER_CONTEXT); \
		else \
			echo "Skipping Docker build (NO_DOCKER=$(NO_DOCKER))"; \
		fi; \
	fi

# Run the basic-face-detection example (offline mode)
# Uses the example module directory and launches via Spring Boot
run:
	@echo "Running basic-face-detection example..."
	cd spring-vision-examples/basic-face-detection && mvn -o spring-boot:run

# Clean the project
clean:
	mvn -B clean

# ------------------------
# Deploy workflow
# ------------------------

# Quick checks that enforce local environment requirements
check-settings:
	@echo "Checking Maven settings at $(SETTINGS) for OSSRH credentials...";
	if [ "$(DRY_RUN)" = "1" ]; then \
		echo "DRY_RUN=1: skipping real settings check (would verify $(SETTINGS) contains <id>ossrh, <username>, <password>)"; \
		true; \
	else \
		if [ ! -f "$(SETTINGS)" ]; then \
			echo "ERROR: Maven settings file '$(SETTINGS)' not found. Create it or set SETTINGS=path/to/settings.xml"; exit 1; \
		fi; \
		grep -q "<id>ossrh</id>" "$(SETTINGS)" || { echo "ERROR: settings.xml does not contain <id>ossrh</id>. Add server credentials for OSSRH (see docs/maven-settings.xml.example)."; exit 1; }; \
		grep -q "<username>" "$(SETTINGS)" || { echo "ERROR: settings.xml does not contain <username> for server entries."; exit 1; }; \
		grep -q "<password>" "$(SETTINGS)" || { echo "ERROR: settings.xml does not contain <password> for server entries."; exit 1; }; \
		echo "Maven settings appear to contain OSSRH credentials."; \
	fi

check-gpg:
	@echo "Checking for GPG availability and secret key...";
	if [ "$(DRY_RUN)" = "1" ]; then \
		echo "DRY_RUN=1: skipping real GPG check (would verify gpg is installed and secret keys exist)"; \
		true; \
	else \
		if ! command -v gpg >/dev/null 2>&1; then \
			echo "ERROR: gpg not found in PATH. Install GnuPG to sign artifacts."; exit 1; \
		fi; \
		if ! gpg --list-secret-keys >/dev/null 2>&1; then \
			echo "ERROR: No GPG secret keys found. Import or generate a GPG key to sign artifacts."; exit 1; \
		fi; \
		echo "GPG available and at least one secret key present."; \
	fi

# Build and push docker image using a sane tag derived from TAG env, git tag, or POM version
docker-push-local: build check-settings check-gpg
	@echo "Determining Docker tag...";
	if [ "$(DRY_RUN)" = "1" ]; then \
		DOCKER_TAG="DRY-RUN"; \
		IMAGE_NAME=codesapienbe/spring-vision:$$DOCKER_TAG; \
		echo "DRY_RUN=1: would push Docker image $$IMAGE_NAME (no build/push performed)"; \
		printf "%s" "$$DOCKER_TAG" > .last_deploy_tag; \
	else \
		# prefer explicit TAG env \
		if [ -n "$(TAG)" ]; then \
			DOCKER_TAG="$(TAG)"; \
		else \
			# try exact git tag; leave empty on failure
			DOCKER_TAG=$$(git describe --tags --exact-match 2>/dev/null || true); \
			if [ -z "$$DOCKER_TAG" ]; then \
				# fall back to POM version
				POMVER=$$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version 2>/dev/null || true); \
				if [ -n "$$POMVER" ]; then \
					if echo "$$POMVER" | grep -q "SNAPSHOT"; then \
						DOCKER_TAG="snapshot-"$$(git rev-parse --short HEAD); \
					else \
						DOCKER_TAG="$$POMVER"; \
					fi; \
				else \
					DOCKER_TAG=$$(git rev-parse --short HEAD); \
				fi; \
			fi; \
		fi; \
		IMAGE_NAME=codesapienbe/spring-vision:$$DOCKER_TAG; \
		echo "Docker tag determined: $$DOCKER_TAG -> $$IMAGE_NAME"; \
		if [ "$(NO_DOCKER)" = "1" ]; then \
			echo "NO_DOCKER=1 set; skipping docker build/push"; \
		else \
			echo "Building Docker image $$IMAGE_NAME using $(DOCKERFILE) ..."; \
			docker build -f $(DOCKERFILE) -t $$IMAGE_NAME $(DOCKER_CONTEXT); \
			DOCKER_RC=$$?; \
			if [ $$DOCKER_RC -ne 0 ]; then \
				echo "ERROR: Docker build failed (code $$DOCKER_RC)"; exit $$DOCKER_RC; \
			fi; \
			echo "Pushing $$IMAGE_NAME to Docker registry..."; \
			docker push $$IMAGE_NAME; \
			DOCKER_PUSH_RC=$$?; \
			if [ $$DOCKER_PUSH_RC -ne 0 ]; then \
				echo "ERROR: Docker push failed (code $$DOCKER_PUSH_RC)"; exit $$DOCKER_PUSH_RC; \
			fi; \
		fi; \
		# export tag for following steps
		printf "%s" "$$DOCKER_TAG" > .last_deploy_tag; \
	fi

# Maven deploy using the provided settings.xml
maven-deploy:
	@echo "Running Maven deploy using settings $(SETTINGS)...";
	if [ "$(DRY_RUN)" = "1" ]; then \
		echo "DRY_RUN=1: would run: mvn -B -s \"$(SETTINGS)\" clean deploy"; \
	else \
		if [ ! -f "$(SETTINGS)" ]; then \
			echo "ERROR: settings.xml missing at $(SETTINGS)"; exit 1; \
		fi; \
		MVN_FLAGS=""; \
		if [ "$(SKIP_TESTS)" = "1" ]; then MVN_FLAGS="-DskipTests"; fi; \
		# Exclude the examples parent module from deploy to Maven Central
		# (this prevents the spring-vision-examples POM artifact being pushed)
		mvn -B $$MVN_FLAGS -s "$(SETTINGS)" -pl "!spring-vision-examples" clean deploy; \
		MVN_DEPLOY_RC=$$?; \
		if [ $$MVN_DEPLOY_RC -ne 0 ]; then \
			echo "ERROR: mvn deploy failed with code $$MVN_DEPLOY_RC"; exit $$MVN_DEPLOY_RC; \
		fi; \
		echo "Maven deploy finished successfully."; \
	fi

# Close & release Sonatype staging (only for non-SNAPSHOT versions)
nexus-release:
	@echo "Checking if release (non-SNAPSHOT) to perform nexus staging close/release...";
	POMVER=$$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version 2>/dev/null || true); \
	if [ -z "$$POMVER" ]; then \
		echo "WARNING: Could not determine POM version; skipping nexus staging release. Use Sonatype UI to close/release manually."; exit 0; \
	fi; \
	if echo "$$POMVER" | grep -q "SNAPSHOT"; then \
		echo "Project version $$POMVER is a SNAPSHOT; skipping nexus close/release."; exit 0; \
	fi; \
	# confirm with the user unless SKIP_NEXUS_CONFIRM=1 or DRY_RUN=1
	if [ "$(DRY_RUN)" = "1" ]; then \
		echo "DRY_RUN=1: would close & release Nexus staging for version $$POMVER"; \
		exit 0; \
	fi; \
	if [ "$(SKIP_NEXUS_CONFIRM)" != "1" ]; then \
		read -p "About to CLOSE+RELEASE staging repository for version $$POMVER. Type YES to continue: " CONFIRM; \
		if [ "$$CONFIRM" != "YES" ]; then \
			echo "Aborting nexus release (confirmation not received)."; exit 1; \
		fi; \
	fi; \
	# Attempt to close and release the staging repository (requires OSSRH credentials in settings)
	mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.13:close -DserverId=ossrh -DnexusUrl=https://s01.oss.sonatype.org/ -s "$(SETTINGS)"; \
	CLOSE_RC=$$?; \
	if [ $$CLOSE_RC -ne 0 ]; then \
		echo "ERROR: nexus-staging close failed (code $$CLOSE_RC). You may need to close and release via Sonatype UI: https://s01.oss.sonatype.org/"; exit $$CLOSE_RC; \
	fi; \
	mvn org.sonatype.plugins:nexus-staging-maven-plugin:1.6.13:release -DserverId=ossrh -DnexusUrl=https://s01.oss.sonatype.org/ -s "$(SETTINGS)"; \
	RELEASE_RC=$$?; \
	if [ $$RELEASE_RC -ne 0 ]; then \
		echo "ERROR: nexus-staging release failed (code $$RELEASE_RC). Check Sonatype UI."; exit $$RELEASE_RC; \
	fi; \
	echo "Nexus staging closed and released successfully."

# Top-level deploy: check environment, push docker image, maven deploy, and release staging
deploy: docker-push-local maven-deploy nexus-release
	@echo "Full deploy completed. Docker image tag stored in .last_deploy_tag"

# ------------------------
# End of deploy workflow
# ------------------------
