[Docs Home](../index.md) · [Runtime](./runtime.md) · [MCP Setup](../getting-started/mcp-setup.md)

# Deploying the MCP Server Without Paying for Hosting

This guide covers running the `mcp` server (plus its mandatory Keycloak dependency)
somewhere other than your own laptop, at no recurring hosting cost. There are two
routes, and **which one needs zero code changes depends entirely on CPU architecture**
— pick the hardware first, then follow the matching section below.

## Pick your hardware first

This repo bundles native libraries (DJL's PyTorch engine, bytedeco's OpenCV/OpenBLAS)
that must match the host CPU architecture. The defaults in `pom.xml` are hardcoded to
`linux-x86_64` — that's not a placeholder to fill in, it's the classifier that actually
ships unless an aarch64 OS-detection profile overrides it.

| Hardware | Architecture | Code changes needed | Verified on real hardware? |
|---|---|---|---|
| Any amd64/x86_64 machine — a spare PC, a NUC, an **Intel-based Mac mini** (2018 or earlier — the last Intel generation), any x86_64 cloud VM | x86_64 | **None.** Matches the repo's hardcoded defaults exactly. | Yes — this is the same architecture the project is built and tested on. |
| Oracle Cloud Always Free Ampere A1 | ARM64/aarch64 | None *left to do* — a `linux-aarch64` Maven profile (`pom.xml`) already sets both the PyTorch and OpenCV/OpenBLAS native classifiers correctly. | **No** — reasoned through against Maven Central's published artifacts, not run on real ARM64 hardware yet. |
| **Apple Silicon Mac mini** (M1 and later) run via Docker | ARM64/aarch64 | Same as Oracle — Docker Desktop's Linux VM on Apple Silicon is aarch64, so it hits the identical `linux-aarch64` profile path. | **No**, same caveat as Oracle. |

If you have (or are willing to buy/repurpose) any amd64 machine, **that's the option
with no remaining engineering risk** — everything below in Option A is exactly the same
Docker Compose setup already used in local dev and CI, just running somewhere that stays
on. Oracle's free ARM64 tier (Option B) is the right call if you specifically want
zero-hardware-cost cloud hosting and are willing to be the first real test of the ARM64
native-library path.

## Option A: Self-hosted amd64 hardware you already own

A Mac mini is a good concrete example precisely because Apple sells (or has sold) both
architectures under the same product name — **check which one you actually have**:

- **Intel Mac mini** (any model through the 2018 refresh): x86_64. Docker Desktop's
  Linux VM defaults to `linux/amd64`, matching this repo's hardcoded defaults with zero
  profile activation, zero verification risk.
- **Apple Silicon Mac mini** (M1/M2/M3/M4, 2020 onward): aarch64. This is architecturally
  the *same risk profile as Oracle's ARM64 tier* (see Option B's "ARM64 catch"), not the
  zero-code-change path — don't assume "it's a Mac mini" alone answers the question.

Any other amd64 box (an old desktop, a NUC, a decommissioned office PC) works exactly
the same way as an Intel Mac mini here — the architecture is what matters, not the form
factor.

### 1. Install Docker

- **macOS (Intel):** install Docker Desktop.
- **Linux:** `curl -fsSL https://get.docker.com | sh && sudo usermod -aG docker $USER`.

### 2. Build and run

```bash
git clone https://github.com/codesapienbe/spring-vision.git
cd spring-vision

export SPRING_VISION_VERSION=$(cat VERSION)
export SPRING_VISION_DOMAIN=your-domain.example.com

docker compose up -d --build
```

Same three services as any other deployment (`keycloak`, `mcp`, `caddy` — see
`docker-compose.yml`); on amd64 hardware the `mcp` image builds with the repo's default
native-library classifiers, no OS-detection profile needs to activate.

### 3. Networking — the part that's different from a cloud VM

Unlike a cloud instance, this machine almost certainly doesn't have a public IP or open
inbound ports by default:

- **Port-forward 80/443** on your home router/firewall to this machine's local IP, or
- **Use a tunnel** (e.g., a Cloudflare Tunnel) instead of opening router ports at all —
  in that case the tunnel terminates public TLS, so Caddy's automatic HTTPS becomes
  redundant and the `caddy` service can be dropped in favor of pointing the tunnel
  directly at the `mcp` service's port.
- **Dynamic DNS** if your ISP doesn't hand out a static IP — Caddy's automatic HTTPS
  (`SPRING_VISION_DOMAIN` in the Caddyfile) needs a hostname that keeps resolving to the
  current IP, so a plain static DNS `A` record isn't enough unless your IP is static too.

Keep the machine powered on and networked the same way you'd keep any "always free"
cloud instance running — the cost here is electricity, not a monthly bill.

## Option B: Oracle Cloud Always Free Ampere A1 (ARM64)

The app eagerly loads 8+ DJL models at startup (~500MB of bundled weights) plus a
mandatory Keycloak sidecar — realistically **~2.5–3GB RAM** for both processes combined.
Most free cloud tiers don't fit this, which is why Oracle is the cloud option here at
all:

- Fly.io no longer has a real no-card free tier.
- Render/Railway free web services cap around 512MB RAM and sleep after ~15 minutes
  idle — sleep-on-idle breaks a long-lived MCP Streamable-HTTP session, since a client
  reconnecting after idle time would hit a cold, slowly-waking, model-loading process.
- Cloud Run's free quota is request/GB-second based, not a fixed always-on box — a poor
  fit for a service meant to stay connected to MCP clients and reload 8+ models per cold
  start.

Oracle's Always Free Ampere A1 shape is a real VM with root/Docker access, not a
constrained PaaS container. **Oracle reduced the Always Free A1 allocation from 4
OCPUs/24GB RAM to 2 OCPUs/12GB RAM around June 15, 2026, without a formal
announcement** — plan for the smaller figure. It still comfortably fits this app's
~3GB footprint. Regional capacity for new A1 signups can also be constrained
("out of host capacity") in high-demand regions; if that happens, try a different
region or retry after a few hours/days — this is a known, recurring friction point with
Oracle's free tier, not something specific to this project.

### The ARM64 catch

The root `pom.xml` has a `linux-aarch64` OS-detection profile that activates
automatically when Maven runs on an aarch64 Linux host, overriding both the DJL PyTorch
classifier and the OpenCV/OpenBLAS classifier (bytedeco's own naming, `linux-arm64`,
differs from DJL's `linux-aarch64` — both are set correctly by that one profile). This
means:

- **Build directly on the ARM64 instance** (or via
  `docker buildx build --platform linux/arm64` with QEMU) — don't cross-compile the JAR
  on an x86_64 Mac or CI runner and copy it over, or the wrong native libraries get
  bundled and the app will fail with `UnsatisfiedLinkError` at model-load time.
- The `com.microsoft.onnxruntime:onnxruntime` jar bundles natives for multiple platforms
  in one artifact, so it's lower-risk, but hasn't been independently verified against
  aarch64 in this repo's pinned version.
- **Smoke-test before trusting this**: confirm the image builds and all ~8+ models load
  without native-library errors on the actual box (see Verify, below) before treating an
  ARM64 deployment as done.

If this turns out not to work for some model/native-library combination not covered
above, fall back to Option A above, or a cheap x86_64 VPS (a few dollars/month, e.g.
Hetzner CX22) — both have zero native-library risk and reuse this same Docker Compose
setup unchanged.

### 1. Provision the instance

1. Create an Oracle Cloud account and, under **Compute → Instances → Create Instance**,
   pick the **Ampere A1 (aarch64)** shape. Request 2 OCPUs / 12GB RAM (or up to 4/24 if
   your account still shows the older allocation).
2. Under the instance's VCN **security list** (or the simpler **Network Security Group**
   view), add ingress rules for TCP `80` and `443` from `0.0.0.0/0` — Oracle blocks
   everything but SSH by default, and Caddy needs both ports for the ACME HTTP-01
   challenge and HTTPS traffic.
3. Point a DNS `A` record at the instance's public IP — Caddy needs a real hostname to
   provision a Let's Encrypt certificate for.

### 2. Install Docker

```bash
ssh ubuntu@<instance-ip>
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
```

### 3. Build and run on the instance

```bash
git clone https://github.com/codesapienbe/spring-vision.git
cd spring-vision

export SPRING_VISION_VERSION=$(cat VERSION)
export SPRING_VISION_DOMAIN=your-domain.example.com

docker compose up -d --build
```

This brings up all three services defined in `docker-compose.yml`:

- `keycloak` — issues the JWTs the app validates (see `keycloak/README.md`).
- `mcp` — built from the root `Dockerfile` on this ARM64 box, so the `linux-aarch64`
  Maven profile activates and the correct native libraries get bundled.
- `caddy` — reverse-proxies `SPRING_VISION_DOMAIN` to the `mcp` service over HTTPS,
  auto-provisioning and renewing its Let's Encrypt certificate.

`make run`'s local-dev flow (`docker compose up -d keycloak` + `jbang run.java`) is
unaffected — it only ever targets the `keycloak` service by name.

## Mint tokens and wire up clients (both options)

Same as local dev, but against your server's public hostname instead of `localhost`:

```bash
curl -s -X POST https://your-domain.example.com:8180/realms/spring-vision/protocol/openid-connect/token \
  -d grant_type=client_credentials \
  -d client_id=spring-vision-mcp-desktop \
  -d client_secret=<rotated-secret-see-keycloak-readme> \
  | jq -r .access_token
```

Then follow the client-wiring instructions in
[MCP Setup](../getting-started/mcp-setup.md#%EF%B8%8F-mcp-client-configuration),
substituting `https://your-domain.example.com/mcp` for `http://localhost:8080/mcp`.

**Before going beyond your own testing**: rotate the placeholder Keycloak client
secrets in `keycloak/realm-spring-vision.json` (see `keycloak/README.md`) and consider
putting Keycloak's own admin console behind the same reverse proxy (or tunnel) or a
separate allowlist — this guide exposes it at port `8180` by default.

## Verify

- `docker compose ps` — all three services report `running`/`healthy`.
- `docker compose logs mcp | grep -i unsatisfiedlink` — should return nothing on ARM64;
  any hit means a native library didn't resolve and the ARM64 approach needs revisiting
  (see "The ARM64 catch" above) before relying on that deployment. Not a concern on
  amd64 hardware, since no OS-detection profile is involved there.
- `curl -i https://your-domain.example.com/mcp` — returns `401` (not a connection error
  or `502`), confirming the reverse proxy/tunnel → the `mcp` container → Spring
  Security's JWT check are all wired up correctly end to end.
- `curl https://your-domain.example.com:8180/realms/spring-vision/.well-known/openid-configuration`
  — returns valid OIDC metadata, confirming Keycloak's realm import worked on this box.

## Not yet verified against a real box

Option B (Oracle ARM64) has been reasoned through against the app's actual dependencies
(confirmed native-library classifiers on Maven Central, confirmed resource footprint
from the codebase) but has **not been run end-to-end on real ARM64 hardware** — there's
no ARM64 hardware or outbound access to Oracle's provisioning APIs available in the
environment this guide was written in. Option A (amd64) carries no equivalent
architecture risk, since it's the same platform this repo is already built and tested
on — but the specific self-hosted networking steps (port-forwarding, tunnels, dynamic
DNS) haven't been walked through end-to-end on a real home network either. Treat the
first real run of either option as the actual test of this guide, and expect to iterate
on it.
