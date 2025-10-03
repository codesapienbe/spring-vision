# Cursor IDE Rules (project shorthand)

This file contains a lightweight pointer for contributors and CI to shared coding rules and quick checks.

Please refer to `docs/CONTRIBUTING.md` for the full contributor guidelines and code standards. The key rules enforced by the project are summarized below:

- Use Java 21+ language features idiomatically.
- Run Spotless to format code before submitting: `mvn spotless:apply`.
- Run Checkstyle: `mvn checkstyle:check`.
- Avoid TODO/FIXME comments unless they reference an issue/PR; run `scripts/find_todos.sh` to generate `docs/TODO_SCAN.md` when preparing a PR.
- Keep public APIs stable; document breaking changes in the PR description and migration guide.

This is intentionally minimal — the canonical contributor guidance lives in `docs/CONTRIBUTING.md`.

