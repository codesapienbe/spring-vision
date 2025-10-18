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

1. Build the `starter` artifact from the repo so we can add it as a dependency.
2. Create a new Spring Boot project using the Spring CLI.
3. Add the `spring-vision` starter dependency to the generated project's `pom.xml` using the exact coordinates from this repo.
4. Create a tiny application class that uses the starter to exercise the framework.
5. Run and verify the app.

High-level steps

- Build the starter locally (so Maven can resolve it from the local repository).
- Generate a new project with Spring CLI.
- Modify `pom.xml` to include the starter dependency (groupId/artifactId/version from this repo's `starter/pom.xml`).
- Add a minimal controller or runner to exercise the integration.
- Run and show successful output.

Script for the video and commands

(1) Build the starter artifact

Narration cue: "First we build the starter artifact from the spring-vision repo so we can add it to our new project as a dependency."

Commands to run in the repo root (assumes the Maven wrapper is present):

```bash
./mvnw -q -pl starter -am clean install
```

Explanation:

- `-pl starter` builds the `starter` module only.
- `-am` also builds modules that the starter depends on.
- `clean install` puts the built starter into your local Maven repository so other projects can consume it.

Checkpoint: After the build, you should see the starter artifact installed to your local Maven repo (usually `~/.m2/repository/...`). The starter coordinates in this repo are:

- groupId: io.github.codesapienbe.springvision
- artifactId: starter
- version: 0.0.1

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
    <version>0.0.1</version>
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
java -jar target/vision-demo-0.0.1-SNAPSHOT.jar
```

Verification: In another terminal, run:

```bash
curl -s http://localhost:8080/health
```

You should see `OK` or a message coming from the starter's auto-configuration.

Troubleshooting

- If Maven fails to resolve the `spring-vision` starter, ensure you ran the `./mvnw -pl starter -am clean install` step and that the coordinates you added match the `starter/pom.xml`.
- If the `starter` depends on additional modules, the initial local build should have installed those as well because of `-am`.
- If port 8080 is in use for the demo, run with `--server.port=8081` or set `server.port` in `application.properties`.

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
