Spring CLI Quickstart: Generate a project and add the spring-vision starter

Goal

- Show how to quickly create a new Spring Boot project using the Spring CLI.
- Add the `spring-vision` starter from this repository (the starter module) to the generated project so you can run a minimal app to test the framework.

Audience & Format

- This guide is written to be spoken through in a short video tutorial.
- It contains stepwise commands, narration cues, and verification steps.

Prerequisites

- Java JDK 21 installed and JAVA_HOME set (this repo targets Java 21).
- Maven installed (we'll use Maven here because this repository uses Maven and the repo provides a Maven wrapper `mvnw`).
- Spring CLI installed (optional; we show install steps). If you prefer, you can use https://start.spring.io/ instead of the CLI.
- A local copy of this repository (the spring-vision workspace) so you can reference the `starter` artifact produced by building the project.

Checklist (what you'll do in the video)

1. **Build the starter** with bundled models (YOLO, RetinaFace included in JAR).
2. Create a new Spring Boot project using the Spring CLI.
3. Add the `spring-vision` starter dependency with the exact coordinates.
4. Create a simple REST controller to test face detection and object detection.
5. Run and verify the app works with bundled models (no downloads needed).

High-level steps

- **Build the starter with models**: Run `mvn install -Pdownload-models` to download and bundle YOLO/RetinaFace models.
- Generate a new Spring Boot project with Spring CLI.
- Add the spring-vision starter dependency to the project.
- Create a REST controller to test face/object detection (models included in JAR).
- Run and verify - no runtime downloads needed!

Script for the video and commands

(1) Build the starter with bundled models

Narration cue: "First we build the spring-vision starter with all models bundled. This downloads YOLO and RetinaFace models during the build process."

Commands to run in the repo root:

```bash
./mvnw -q clean install -Pdownload-models
```

Explanation:

- `clean install` builds all modules and installs to local Maven repository.
- `-Pdownload-models` downloads and bundles AI models (YOLO, RetinaFace) in the JAR.
- Models are ~500MB total but give you production-ready computer vision.

Checkpoint: After the build, verify models are included:

```bash
# Check that models are bundled
jar -tf starter/target/starter-0.0.4.jar | grep "models/" | head -5
```

Expected output shows bundled models:
```
models/yolov8/yolov8n.pt
models/yolov8-pose/yolov8n-pose.pt
models/retinaface/retinaface.pt
```

(2) Install Spring CLI (if not already installed)

Narration cue: "If you don't have the Spring CLI, here's a quick install using SDKMAN or Homebrew."

SDKMAN (Linux/macOS):

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install springboot
```

Homebrew (macOS):

```bash
brew tap pivotal/tap
brew install springboot
```

Verify installation:

```bash
spring --version
```

(3) Generate a new Spring Boot project with Spring CLI

Narration cue: "Now we'll create a minimal Spring Boot project using the Spring CLI. I'll name it `vision-demo` and use Java 21 to match the starter."

Command (Maven build, Java 21, package as a jar, include web dependency):

```bash
spring init --build=maven --java-version=21 --dependencies=web --name=vision-demo --package-name=com.example.vision vision-demo
```

This creates a `vision-demo` folder with a Maven Spring Boot project.

(4) Add the `spring-vision` starter dependency

Narration cue: "Open the generated `pom.xml` and add the `spring-vision` starter using the coordinates from the repo. If you built the starter locally, Maven will resolve it from your local repository."

Open `vision-demo/pom.xml` and add a dependency in the `<dependencies>` section. Use these exact coordinates from `starter/pom.xml`:

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.4</version>
</dependency>
```

Notes:

- The starter is part of the multi-module project; its POM inherits the parent group's `groupId` and version (so the effective coordinates above are correct for the version in this workspace).
- The starter's auto-configuration/main class is `io.github.codesapienbe.springvision.starter.SampleVisionApplication` (see `starter/pom.xml` for the configured mainClass). This helps when troubleshooting which component boots when you run the starter directly.

(5) Create a simple test app that uses the starter

Narration cue: "Add a tiny REST controller to call into the starter or exercise its auto-configuration."

Create file `vision-demo/src/main/java/com/example/vision/VisionDemoApplication.java` with a typical Spring Boot main class and a simple controller. Example:

- Main class bootstraps Spring Boot.
- Controller exposes `GET /health` returning a message from the starter (or just "OK" if the starter does not provide a simple API).

(6) Run the app

Narration cue: "Run the app and show the health endpoint in the browser or use curl."

Commands:

```bash
cd vision-demo
./mvnw spring-boot:run
```

Or build and run the jar:

```bash
./mvnw package
java -jar target/vision-demo-0.0.4-SNAPSHOT.jar
```

Verification: Test the vision capabilities with bundled models:

```bash
# Health check
curl -s http://localhost:8080/actuator/health

# Test face detection (upload an image with faces)
curl -X POST -F "file=@face_image.jpg" http://localhost:8080/api/vision/faces

# Test object detection (upload any image)
curl -X POST -F "file=@any_image.jpg" http://localhost:8080/api/vision/objects
```

**Expected:** JSON responses with detections using bundled YOLO/RetinaFace models (no downloads needed!)

Troubleshooting

- **Build fails with network errors**: Ensure you have internet access for model downloads. The `-Pdownload-models` profile downloads ~500MB of models.
- **Models not found**: Verify the build completed with `-Pdownload-models`. Check `jar -tf target/*.jar | grep models/` to confirm models are bundled.
- **Maven resolution fails**: Ensure you ran the full build first: `./mvnw clean install -Pdownload-models`
- **Port 8080 in use**: Use `--server.port=8081` or set `server.port=8081` in `application.properties`
- **Out of memory**: Vision models need memory. Try `-Xmx2g` when running.
- **GPU not working**: Ensure CUDA drivers are installed and use `spring.vision.djl.device=gpu` in config.

Narration tips for the video

- Keep each command on screen for 4–7 seconds.
- Narrate what each command does briefly (one sentence).
- When editing the `pom.xml`, zoom in on the dependency block and call out the groupId/artifactId/version.
- For the run and verification steps, show the running terminal and the curl response.

Extras (optional additions you can show quickly)

- Show how to use `start.spring.io` web UI if viewers prefer not to install the Spring CLI.
- Show how to add the project to an IDE (IntelliJ IDEA) — open the `vision-demo` folder.

Where to find the starter coordinates

- Open `starter/pom.xml` in the repo and copy the `<groupId>`, `<artifactId>`, and `<version>` used by that module.

Example:

```bash
# In repo root
sed -n '1,120p' starter/pom.xml
```

Concluding narration

"That's it — in under a few minutes we generated a new Spring Boot project, added the spring-vision starter, and ran a tiny app to verify the integration."

---

If you want, I can:

- Create a sample `vision-demo` project in this workspace preconfigured with the correct starter coordinates copied from `starter/pom.xml`.
- Add a minimal controller source file here so you can run the demo immediately.
- Produce a short video script with precise narration lines for each step.

Tell me which of these extras you'd like and I'll add them now.
