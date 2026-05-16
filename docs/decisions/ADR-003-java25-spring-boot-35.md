# ADR-003: Target Java 25 and Spring Boot 3.5

## Status
Accepted

## Date
2026-05-16

## Context

The project was originally built on Java 21 (LTS, September 2023) and Spring Boot 3.2.x. By May 2026:

- Java 25 (LTS, September 2025) has been GA for eight months and is the current long-term-support release.
- Spring Boot 3.5.0 is the current stable release, built on Spring Framework 6.2.
- Several dependency upgrades (ONNX Runtime 1.19, PyTorch 2.3, Spring AI 1.1) require Spring Boot 3.4+ for full compatibility.
- DJL 0.33 ships with Byte Buddy 1.14, which officially supports Java 25 (no `--add-opens` workarounds needed).

## Decision

Upgrade the compiler target (`maven.compiler.release`) and the Maven enforcer minimum to Java 25. Upgrade Spring Boot to 3.5.0 and the Spring Cloud BOM to 2024.0.0.

Key version changes:

| Dependency | Before | After |
|---|---|---|
| Java (compiler) | 21 | 25 |
| Spring Boot | 3.2.8 | 3.5.0 |
| Spring Cloud BOM | 2023.0.0 | 2024.0.0 |
| ONNX Runtime | 1.15.1 | 1.19.0 |
| PyTorch native | 2.1.1 | 2.3.0 |
| Spring AI | 1.0 | 1.1.0 |

## Alternatives Considered

### Stay on Java 21 LTS
- Java 21 will receive security patches until at least 2031.
- Staying on 21 delays access to sealed-class improvements, virtual thread stabilization, and pattern-matching enhancements landed in 22–25.
- Rejected: Java 25 is the current LTS; upgrading now is less disruptive than deferring.

### Upgrade to Java 24 (non-LTS)
- Java 24 is not an LTS; it receives only six months of support.
- No advantage over going directly to 25.
- Rejected.

## Consequences

- Build environments must have JDK 25 installed. The Maven enforcer enforces `[25,)`.
- Byte Buddy must be kept at 1.14.12+ for Java 25 support; the Surefire JVM flag `-Dnet.bytebuddy.experimental=true` is still required until a future DJL upgrade removes the constraint.
- `opencv.classifier` still defaults to `linux-x86_64` for CI; macOS developers must pass `-Dopencv.classifier=macosx-arm64` if they need OpenCV.
- GPG artifact signing is now **disabled by default** (`gpg.skip=true`). Re-enable for release builds with `-Dgpg.skip=false`. This allows local and CI builds to succeed without a GPG key configured.
