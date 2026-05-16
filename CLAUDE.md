# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Spring Vision** is a computer vision framework for Spring Boot, built on Deep Java Library (DJL). Core models (YOLO, RetinaFace) are bundled in the JAR. It also ships as an MCP server so LLM clients can call vision capabilities as tools.

## Modules

| Module | Artifact | Role |
|--------|----------|------|
| `core` | `spring-vision-core` | Domain model, `VisionBackend` SPI, `VisionTemplate`, DJL backend, capability interfaces |
| `starter` | `spring-vision-starter` | Spring Boot auto-configuration, REST API (`/api/vision/*`), async config |
| `mcp` | `spring-vision-mcp` | MCP server application; exposes `VisionTool` via Spring AI `@Tool` |
| `cli` | `spring-vision-cli` | JBang-runnable CLI that downloads and configures the MCP server JAR |

Version is controlled by the `VERSION` file at the root.

## Build & Development Commands

```bash
# Full build (includes model download, skips tests and GPG)
make build

# Run only core + mcp tests (integration tests need network for DJL model zoo)
make test
# Equivalent: mvn -pl core,mcp -am test

# Run a single test class
mvn -pl core test -Dtest=DjlVisionBackendIntegrationTest

# Run only unit tests (tagged, skips memory-intensive group)
mvn -pl core test -Dgroups="!memory-intensive"

# Format code (Spotless)
make format
# or: mvn spotless:apply

# Check formatting + Checkstyle
make verify

# Build only the MCP module and copy JAR for local MCP client testing
make sync

# Run the MCP server locally (builds first, then uses jbang run.java)
make run

# Skip GPG signing during local builds (already default in Makefile)
mvn clean install -DskipTests -Dgpg.skip=true -Pdownload-models
```

## Architecture

### Capability-Based Backend Pattern

`VisionBackend` is a pure SPI (metadata + health only). All detection work is done through granular **capability interfaces** in `core/src/main/java/.../core/capabilities/`:

- `FaceDetectionCapability`, `ObjectDetectionCapability`, `OcrCapability`, `PoseEstimationCapability`, etc.
- `AnnotationCapability` (draw/obscure/tag on image)
- `EmbeddingCapability`, `VectorStoreCapability`

`DjlVisionBackend` implements all of these. Adding a new backend means implementing `VisionBackend` + whichever capability interfaces it supports.

`VisionTemplate` (a Java `record`) is the consumer-facing API. It uses `instanceof`-pattern-matched capability checks and throws `VisionUnsupportedException` when the active backend doesn't implement a requested capability.

```
VisionTemplate
    └── delegates via capability instanceof checks to:
            └── DjlVisionBackend (implements VisionBackend + all capabilities)
                    ├── YoloModelLoader / YoloLoader — loads bundled YOLO ONNX models
                    ├── DJL Predictor<Image, DetectedObjects> — face/object detection
                    ├── ZXing — barcode scanning
                    └── metadata-extractor — EXIF/GPS extraction
```

### MCP Layer (`mcp` module)

`VisionTool` is a Spring `@Component` annotated with Spring AI `@Tool` methods. `ToolCallbackConfiguration` registers these as MCP callbacks. The server runs as a standalone Spring Boot app (`SpringVisionMcpServerApplication`), communicating over stdio (MCP transport).

### REST Layer (`starter` module)

`VisionController` maps `/api/vision/*` to `VisionTemplate` calls, with async support via `AsyncConfig`. DTOs live in `.../starter/web/dto/`.

## Code Quality

- **Checkstyle** (`checkstyle.xml`): no star imports, `NeedBraces` required, `EmptyBlock` forbidden. Runs at `validate` phase.
- **Spotless**: removes unused imports, enforces import order (`java,javax,org,com`). Run `mvn spotless:apply` before committing.
- **JaCoCo**: 90% instruction/branch/line coverage enforced at `verify` phase.
- Java 21+ required (enforced by `maven-enforcer-plugin`); source compiled to `--release 21`.
- Tests needing heavy memory use JUnit 5 tag `memory-intensive` and are excluded by default.

## Key Conventions

- All configuration properties use prefix `spring.vision.*` (e.g., `spring.vision.djl.enabled`).
- Integration tests (`*IntegrationTest.java`, `*IT.java`) are handled by Failsafe; unit tests by Surefire.
- Synthetic/fallback results are returned in offline/test mode so tests pass without network access (`ai.djl.offline=false` is set in Surefire but models may be absent).
- The `download-models` Maven profile downloads YOLO and RetinaFace models into the JAR during build; skip it with `-P!download-models` if models are already present.
- GPU support is opt-in via `-P gpu` profile (requires NVIDIA CUDA).
