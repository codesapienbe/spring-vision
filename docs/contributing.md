# Contributing

[Docs Home](./index.md) · [Roadmap](./roadmap.md) · [Modules](./modules.md)

We welcome contributions! This guide outlines how to propose changes and add modules.

## How to Contribute

- Discuss ideas in GitHub Issues/Discussions
- Fork the repo and create a topic branch
- Write tests for new behavior
- Update or add docs where applicable
- Open a PR with a clear description and rationale

## Code Standards

- Java 21+, Spring Boot 3.2+
- Follow module property conventions in [Modules Overview](./modules.md)
- Prefer VisionTemplate in examples over direct backend wiring
- Clean resource and thread management (see [Runtime](./runtime.md))

## Documentation

- Keep examples minimal and runnable
- Cross-link related docs
- Update module README and docs when behavior changes

## Development Quickstart

```bash
mvn -q -T1C -DskipTests package
mvn test
```

GPU build: `mvn clean install -P gpu`

## Areas Needing Contribution

- New backends (state-of-the-art models)
- Performance & benchmarking
- Documentation, tutorials, and examples
- Integration tests and samples
- Security & privacy features

## Release Checklist (Maintainers)

- Build and tests green
- Docs updated and cross-linked
- Models bundled or download scripts updated
- Version bumped and tags created

