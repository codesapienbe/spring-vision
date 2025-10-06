# Build the entire multi-module project
build:
	mvn -B -e -T 1C clean install

# Run the basic-face-detection example
# Uses the example module directory and launches via Spring Boot
run:
	@echo "Running basic-face-detection example..."
	cd spring-vision-examples/basic-face-detection && mvn spring-boot:run

# Clean the project
clean:
	mvn -B clean

.PHONY: build run clean
