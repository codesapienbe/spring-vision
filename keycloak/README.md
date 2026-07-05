# Local Keycloak (dev only)

`make keycloak-up` starts a Keycloak container (`start-dev` mode, no external database)
and auto-imports `realm-spring-vision.json`, which defines:

- Realm `spring-vision`
- Client `spring-vision-mcp-desktop` — client-credentials grant, 1 hour access tokens.
  Used by Claude Desktop / Claude Code, which can't refresh tokens automatically, so
  re-fetch a new one (see `docs/getting-started/mcp-setup.md`) roughly hourly.
- Client `spring-vision-mcp-mobile` — client-credentials grant, 24 hour access tokens.
  Google AI Edge Gallery only supports pasting a static bearer token with no refresh
  flow, so this client trades token lifetime for practicality. Treat this as a
  deliberate, scoped tradeoff for that one client — not a default to copy elsewhere.
- Realm role `mcp-user`, assigned to both clients' service accounts. Groundwork for
  future per-tool RBAC; not yet read or enforced by the `mcp` module.

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
