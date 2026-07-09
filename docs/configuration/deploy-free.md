[Docs Home](../index.md) · [Runtime](./runtime.md) · [MCP Setup](../getting-started/mcp-setup.md)

# Deploying the MCP Server for $0/month (Oracle Cloud Free Tier)

This guide covers running the `mcp` server (plus its mandatory Keycloak dependency) on
Oracle Cloud Infrastructure's **Always Free** Ampere A1 (ARM/aarch64) tier, so it's
reachable over HTTPS without paying for a VPS.

## Why Oracle, and why ARM

The app eagerly loads 8+ DJL models at startup (~500MB of bundled weights) plus a
mandatory Keycloak sidecar — realistically **~2.5–3GB RAM** for both processes combined.
Most other free tiers don't fit this:

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

## The ARM64 catch

This repo bundles native libraries (DJL's PyTorch engine, bytedeco's OpenCV/OpenBLAS)
that must match the host CPU architecture. The root `pom.xml` now has a `linux-aarch64`
OS-detection profile that activates automatically when Maven runs on an aarch64 Linux
host, overriding both the DJL PyTorch classifier and the OpenCV/OpenBLAS classifier
(bytedeco's own naming, `linux-arm64`, differs from DJL's `linux-aarch64` — both are set
correctly by that one profile). This means:

- **Build directly on the Oracle ARM64 instance** (or via
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
above, there is no other free tier with enough RAM for this app's current
eager-loading architecture — the realistic fallback is a cheap x86_64 VPS (a few
dollars/month, e.g. Hetzner CX22), which has zero native-library risk and reuses this
same Docker Compose setup unchanged.

## 1. Provision the instance

1. Create an Oracle Cloud account and, under **Compute → Instances → Create Instance**,
   pick the **Ampere A1 (aarch64)** shape. Request 2 OCPUs / 12GB RAM (or up to 4/24 if
   your account still shows the older allocation).
2. Under the instance's VCN **security list** (or the simpler **Network Security Group**
   view), add ingress rules for TCP `80` and `443` from `0.0.0.0/0` — Oracle blocks
   everything but SSH by default, and Caddy needs both ports for the ACME HTTP-01
   challenge and HTTPS traffic.
3. Point a DNS `A` record at the instance's public IP — Caddy (below) needs a real
   hostname to provision a Let's Encrypt certificate for.

## 2. Install Docker

```bash
ssh ubuntu@<instance-ip>
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
```

## 3. Build and run on the instance

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

## 4. Mint tokens and wire up clients

Same as local dev, but against this instance's public hostname instead of `localhost`:

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
putting Keycloak's own admin console behind the same reverse proxy or a separate
allowlist — this guide exposes it on the instance's public IP at port `8180` by default.

## Verify

- `docker compose ps` — all three services report `running`/`healthy`.
- `docker compose logs mcp | grep -i unsatisfiedlink` — should return nothing; any hit
  means a native library didn't resolve for aarch64 and the ARM64 approach needs
  revisiting (see "The ARM64 catch" above) before relying on this deployment.
- `curl -i https://your-domain.example.com/mcp` — returns `401` (not a connection error
  or `502`), confirming Caddy → the `mcp` container → Spring Security's JWT check are all
  wired up correctly end to end.
- `curl https://your-domain.example.com:8180/realms/spring-vision/.well-known/openid-configuration`
  — returns valid OIDC metadata, confirming Keycloak's realm import worked on this box.

## Not yet verified against a real box

This guide has been reasoned through against the app's actual dependencies (confirmed
native-library classifiers on Maven Central, confirmed resource footprint from the
codebase) but has **not been run end-to-end on a real Oracle Ampere A1 instance** —
there's no ARM64 hardware or outbound access to Oracle's provisioning APIs available in
the environment this guide was written in. Treat the first real run as the actual test
of this plan, and expect to iterate on it.
