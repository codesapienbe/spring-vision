# AGENT.md

Operational notes for AI agents (Claude Code, Cursor, etc.) working on this
project. For project-wide build/test/architecture guidance see
[CLAUDE.md](CLAUDE.md).

## Watching MCP server logs

The MCP server writes all log output to `~/.springvision/` regardless of which
client launches it. When debugging a tool call, follow the live stream:

```bash
tail -f ~/.springvision/mcp.log
```

Errors only:

```bash
tail -f ~/.springvision/mcp.error.log
```

After making changes to the MCP server, the typical loop is:

1. `mvn -pl core install -DskipTests -P'!download-models' -q` (only if `core` changed)
2. `make sync` — repackages `mcp` and copies the jar to `~/.springvision/mcp-<version>.jar`
3. The user runs `/mcp` to reconnect — that starts a fresh server process with the new jar.
4. Old MCP processes do NOT auto-shut down. If logs look stale, check `ps aux | grep mcp-0.0.4.jar` — the newest PID is the one Claude is talking to.

The MCP server's working directory is whatever the parent process (Claude Code,
Cursor, etc.) is launched from, so anchoring logs under `~/.springvision/`
keeps them findable regardless of where the agent was started.

## Other persistent state under `~/.springvision/`

| Path                          | Purpose                                          |
| ----------------------------- | ------------------------------------------------ |
| `~/.springvision/mcp-*.jar`   | The MCP server jar installed by `make sync`      |
| `~/.springvision/mcp.log`     | All MCP server activity (rolled daily, 50 MB)    |
| `~/.springvision/mcp.error.log` | Errors only (rolled daily, kept 90 days)        |
| `~/.springvision/db/auth.db`  | SQLite store for enrolled face identities       |

If a tool call fails with a generic message, the underlying stack trace is in
`~/.springvision/mcp.log` — read it before guessing.
