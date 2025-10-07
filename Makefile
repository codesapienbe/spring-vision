# Download all dependencies for offline use
deps:
	mvn -B dependency:go-offline

# Build the entire multi-module project (offline mode)
build:
	mvn -B -e -T 1C clean install

# Run the basic-face-detection example (offline mode)
# Uses the example module directory and launches via Spring Boot
run:
	@echo "Running basic-face-detection example..."
	cd spring-vision-examples/basic-face-detection && mvn -o spring-boot:run

# Clean the project
clean:
	mvn -B clean

.PHONY: build run clean deps
