# CONTRIBUTING: New Module Guidelines

This document defines mandatory contribution rules for any new module added to the Spring Vision monorepo. New modules MUST follow these rules exactly to ensure consistent quality, discoverability, testability, and safe integration with the `spring-vision-core` platform.

Use this checklist as part of your pull request. CI will verify many of these items.

---

## Quick checklist (must-pass)

- [ ] Module uses the parent `pom.xml` (is a child module of the repository POM).
- [ ] Module artifact id follows the naming convention: `spring-vision-<feature>-<impl?>` (lowercase, hyphens).
- [ ] Module adds a `VisionBackend` implementation or registers a core capability SPI adapter when appropriate.
- [ ] Module implements or advertises capability support via `com.springvision.core.capabilities` (where applicable).
- [ ] Module provides a minimal, documented README (how to use, config, required datasets/credentials).
- [ ] Module contains unit tests and integration tests demonstrating the feature; test coverage >= 90% enforced by Jacoco.
- [ ] Code follows project style and check rules (Spotless / Google Java Format and Checkstyle).
- [ ] All public APIs documented (Javadoc), and the module includes API usage examples or an example integration in `spring-vision-examples`.
- [ ] No secrets, credentials, or protected data are committed.
- [ ] License header and compatibility with the repo license (Apache 2.0) are clear.

---

## Required project structure

A new module should include at minimum:

- `pom.xml` that parents to the repository's root `pom.xml` via `<parent>`.
- `src/main/java` with implementation classes.
- `src/test/java` with unit and integration tests.
- `README.md` explaining purpose, quick start, configuration, and usage examples.
- If the module provides a pluggable backend, register a service provider: `META-INF/services/com.springvision.core.VisionBackend` containing the fully-qualified implementation class name.

Naming conventions
- Module artifactId: `spring-vision-<name>`.
- Java package root: `com.springvision.<name>` or `com.springvision.<feature>.<impl>` for implementation modules.

---

## Mandatory technical rules

1. VisionBackend or Capability
   - Any module that implements runtime vision functionality MUST either:
     - Provide a `VisionBackend` implementation (implement `com.springvision.core.VisionBackend`) and register it with the Java SPI mechanism, or
     - Provide an adapter implementation of the relevant core capability interface (for example `com.springvision.core.capabilities.HeartRateCapability`, `FallDetectionCapability`, `StressAnalysisCapability`, or other capability SPIs).
   - Backends should advertise supported detection types by returning an appropriate `Set<DetectionType>` from `getSupportedDetectionTypes()`.

2. Core integration and `VisionTemplate`
   - Integration code should prefer wiring through `com.springvision.core.VisionTemplate` and should avoid adding new public entry points that bypass the core template unless there is a compelling, documented reason.

3. Inputs and outputs
   - Use the core `ImageData` and `Detection` types for image I/O and result interchange wherever possible.
   - For health/time-series features, accept `List<ImageData>` (temporal frames) as the canonical input. Return a `List<Detection>` with well-defined metadata keys (see "Result metadata conventions").

4. Result metadata conventions (recommended keys)
   - Heart-rate detectors: `bpm_min`, `bpm_max`, `bpm_avg`, `confidence`.
   - Stress detectors: `stress_score` (0.0..1.0), `confidence`.
   - Fall detection: `event` (e.g., "fall"), `frame_index`, `confidence`, optional `subject_id`.
   - Tumor classification: `predicted_label`, `confidence`, `per_class_probabilities` (encoded JSON or map).

5. Tests and coverage
   - Unit tests: cover happy path and at least two edge cases (null/empty input, invalid formats). Prefer pure unit tests that do not require native libs.
   - Integration tests: a small end-to-end test that exercises service wiring (can use a small synthetic dataset or mock). Integration tests should be identifiable with surefire/failsafe naming conventions.
   - Jacoco: modules must declare jacoco coverage checks consistent with the root project configuration and must achieve overall coverage >= 90% (adjust CI to exclude heavy native code tests if required but justify exclusions in PR).

6. Static analysis and formatting
   - Run Spotless/Google Java Format and fix formatting issues before submitting a PR.
   - Fix Checkstyle violations. The root repo enforces Checkstyle during `validate` phase; new modules must not break it.

7. Dependency hygiene
   - Avoid adding transitive or unnecessary dependencies to the root `dependencyManagement`. New modules should add their own dependencies in their `pom.xml` and prefer scoped or optional where appropriate for native or heavy libs (ONNX, native OpenCV binaries).
   - Prefer widely used, well-maintained libraries and pin versions. Document native dependency requirements in the README.

8. Configuration and properties
   - Provide a clear set of configuration properties (Spring Boot `application.yml` keys if Spring-based). Use namespaced keys under `spring.vision.<module>`.
   - Do not rely on non-deterministic defaults (e.g., environment-specific file paths). Document overrides.

9. Metrics & health
   - If the module runs runtime tasks, expose Micrometer metrics when applicable and provide a Spring Actuator `HealthIndicator` in the implementation module so the backend health can be surfaced.

10. Security & privacy
   - Ensure sensitive data is never logged. Mask PII when present.
   - For medical datasets (MRI, BRISC, etc.), clearly state licensing and privacy implications in the README and do not include datasets in the repository.

11. Licensing
   - The module must be compatible with the repository license (Apache 2.0). Add an explicit license header if the module exposes code in a different license.

---

## CI, build and release rules

- Make sure the module builds under the repository root:

```bash
# from repo root
mvn -DskipTests verify
```

- Tests must pass in CI. Coverage gate must pass (>= 90%).
- If a module requires native binaries (ONNX runtime, native OpenCV), provide a separate CI job that runs optional integration tests with those artifacts and mark those tests as optional or behind a CI flag.

---

## Pull Request checklist

When opening a PR for a new module, include the following in the PR description:

- Short summary of what the module does and why it belongs in the repository.
- Which `DetectionType`/capabilities it provides and how it wires into `VisionTemplate`.
- A list of configuration properties and required environment variables.
- Links to dataset licensing (if the module relies on third‑party datasets) and any special setup instructions.
- A description of the test plan, and where synthetic test data is located.
- CI status: show that `mvn -DskipTests verify` and jacoco checks pass.

A reviewer will verify the PR against the Quick checklist above; address any review comments and update tests before merging.

---

## Example: minimal `pom.xml` snippet

Include this in your module POM and update coordinates as required:

```xml
<parent>
  <groupId>com.springvision</groupId>
  <artifactId>spring-vision</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>spring-vision-foo</artifactId>
<name>Spring Vision - Foo</name>

<dependencies>
  <dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-core</artifactId>
  </dependency>
  <!-- Add only required dependencies here -->
</dependencies>
```

Also add the module to the root `pom.xml` modules list.

---

## Example: registering a `VisionBackend` via Java SPI

Create file `src/main/resources/META-INF/services/com.springvision.core.VisionBackend` containing a single line with the fully-qualified class name of your implementation:

```
com.example.vision.backend.MyFancyBackend
```

Your `MyFancyBackend` should implement `com.springvision.core.VisionBackend` and advertise supported `DetectionType` entries.

---

## Example: capability adapter

If your module implements a capability instead of a full `VisionBackend`, implement the appropriate interface in `com.springvision.core.capabilities` and ensure your backend returns the correct behavior. For example, to implement heart-rate detection:

```java
public class MyBackend implements VisionBackend, com.springvision.core.capabilities.HeartRateCapability {
    @Override
    public List<Detection> detectHeartRate(List<ImageData> imageDataList) {
        // implementation
    }
}
```

---

## Documentation & examples

- Add a short example under `spring-vision-examples/<module-name>` demonstrating how to invoke the capability through `VisionTemplate`.
- Add usage code snippets to the module `README.md` showing configuration and typical inputs/outputs.

---

## Datasets & model artifacts

- DO NOT commit large datasets or pre-trained models to the repo. Instead:
  - Provide a script or instructions to download datasets into a developer-only folder.
  - Document the dataset license, citation, and any restrictions in the README.

---

## Support & maintenance

- A module owner (or small team) must be listed in the module README with contact details.
- Maintenance expectations: address critical bugs and security advisories in a timely manner; keep dependencies reasonably up-to-date.

---

## Requirements coverage (summary)

- VisionBackend / Capability SPI: Done (module must implement and register) — Required
- Image-list input contract: Done (modules must accept `List<ImageData>` where applicable) — Required
- Tests: Unit + Integration; Coverage >= 90% enforced — Required
- Formatting & style (Spotless/Checkstyle): Required
- Jacoco coverage gate: match repo (>= 90%): Required
- README & usage examples: Required
- SPI registration / `META-INF/services` when applicable: Required

---

If you want, I can also generate a minimal skeleton module in this repository that demonstrates all of the above (a tiny, deterministic `VisionBackend` that implements the heart-rate/fall/stress capability methods and includes tests). Reply with **"create skeleton module"** to have me add it now.
