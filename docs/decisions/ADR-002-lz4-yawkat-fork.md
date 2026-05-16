# ADR-002: Use yawkat fork of lz4-java instead of upstream

## Status
Accepted — pending CVE documentation (see Consequences)

## Date
2026-05-16

## Context

Spring Vision's `core` module uses LZ4 compression for efficient in-process index storage (via Chronicle Map). The canonical library is `org.lz4:lz4-java`.

A security issue was identified in the `org.lz4:lz4-java` upstream that had not been released in a public patch at the time this decision was made. The `at.yawk.lz4:lz4-java` fork maintained by [@yawkat](https://github.com/yawkat) contained the fix.

## Decision

Replace `org.lz4:lz4-java:1.8.0` with `at.yawk.lz4:lz4-java:1.10.1`.

The fork is API-compatible; no code changes are required.

## Alternatives Considered

### Stay on upstream `org.lz4:lz4-java`
- Risk: the known vulnerability is present until upstream ships a fix.
- Rejected (temporarily): until upstream releases a patch, the fork is the safer choice.

### Remove LZ4 compression entirely
- LZ4 is used by Chronicle Map for its internal storage; swapping it out requires a different persistence library.
- Rejected: disproportionate scope change.

## Consequences

- **Supply-chain risk**: `at.yawk.lz4` is a personal fork, not an official release from the `org.lz4` group. Its Maven artifacts must be trusted independently.
- **Action required before the next release**: document the specific CVE or vulnerability that motivated this change, and add a note to the release checklist to check whether upstream `org.lz4:lz4-java` has shipped a fix. If upstream has released a patched version, revert to it.
- If `at.yawk.lz4` stops receiving updates, re-evaluate. Check: `https://github.com/yawkat/lz4-java`.
