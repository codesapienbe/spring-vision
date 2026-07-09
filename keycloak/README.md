# Local Keycloak (dev only)

`make run` starts a Keycloak container (`start-dev` mode, no external database) before
running the server (or run `docker compose up -d keycloak` directly). It auto-imports
`realm-spring-vision.json`, which defines:

- Realm `spring-vision`
- Client `spring-vision-mcp-desktop` — client-credentials grant, 1 hour access tokens,
  granted both `mcp-user` and `mcp-admin` realm roles. Used by Claude Desktop / Claude
  Code, which can't refresh tokens automatically, so re-fetch a new one (see
  `docs/getting-started/mcp-setup.md`) roughly hourly.
- Client `spring-vision-mcp-mobile` — client-credentials grant, 24 hour access tokens,
  granted `mcp-user` only (not `mcp-admin` — see below). Google AI Edge Gallery only
  supports pasting a static bearer token with no refresh flow, so this client trades
  token lifetime for practicality. Treat the long lifespan as a deliberate, scoped
  tradeoff for that one client — not a default to copy elsewhere.
- Two realm roles:
  - `mcp-user` — general-purpose vision tools (object/pose/OCR/barcode/vehicle detection,
    etc.).
  - `mcp-admin` — everything `mcp-user` gets, plus sensitive tools: biometric
    authentication/enrollment (`authenticate_access_*`, `store_identity_u`,
    `delete_identity`, `list_identities`), threat detection, deepfake detection, face
    verification/1:N lookup, raw face embeddings, and ID/driver-license/license-plate
    recognition. Enforced in `VisionTool` via a `requireAdminRole()` check on each of
    those methods — **not** via `@PreAuthorize`, because Spring AI's
    `MethodToolCallbackProvider` does not reliably enforce method-security AOP on
    `@Tool` methods (see [spring-projects/spring-ai#2356](https://github.com/spring-projects/spring-ai/issues/2356),
    [#3272](https://github.com/spring-projects/spring-ai/issues/3272)).
  - Mobile is deliberately capped to `mcp-user` — its 24h token is a bigger blast radius
    if leaked, so keeping biometric/threat tools off it is a real security boundary, not
    an arbitrary restriction.

See [`docs/rbac/index.html`](../docs/rbac/index.html) for the full per-tool access matrix
and a breakdown of exactly which JWT claims the app reads versus ignores.

For running this (and the `mcp` app) somewhere other than your own machine, see
[Deploying for Free](../docs/configuration/deploy-free.md).

## Client secrets

The secrets in `realm-spring-vision.json` (`desktop-dev-secret-change-me`,
`mobile-dev-secret-change-me`) are placeholders for local dev only. Rotate them in the
Keycloak admin console (http://localhost:8180, login `admin`/`admin`) under
**Clients → &lt;client&gt; → Credentials** before using this anywhere beyond your own
machine.

## Production

This setup is dev-only. A production Keycloak instance is a separate follow-up — the
`mcp` module only needs `KEYCLOAK_ISSUER_URI` pointed at whichever Keycloak realm is
authoritative, so switching later is a config change, not a code change.
