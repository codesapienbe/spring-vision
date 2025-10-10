# Quick CI/CD Reference

## GitHub Secrets Required

```
OSSRH_USERNAME      - Your Sonatype OSSRH username
OSSRH_TOKEN         - Your Sonatype OSSRH token/password  
GPG_PRIVATE_KEY     - Your GPG private key (ASCII armor)
GPG_PASSPHRASE      - Your GPG key passphrase
```

## Quick Setup

```bash
# 1. Export your GPG key
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc

# 2. Add secrets to GitHub:
# Settings → Secrets and variables → Actions → New repository secret

# 3. Test locally first
mvn clean deploy -Prelease -DskipTests -pl "!spring-vision-examples"
```

## Deployment Commands

### Deploy Snapshot (Automatic)

```bash
git push origin main  # Auto-deploys SNAPSHOT
```

### Release via Tag

```bash
mvn versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false
git commit -am "Release 1.0.0"
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

### Manual Release (GitHub UI)

1. Actions → "Release to Maven Central"
2. Run workflow → Enter version
3. Click "Run workflow"

## Check Deployment

- Staging: https://s01.oss.sonatype.org/
- Maven Central: https://search.maven.org/ (10-30 min delay)

## Workflows

- **build.yml** - Build & test on PR/push
- **deploy-snapshot.yml** - Auto-deploy snapshots to Maven Central
- **release.yml** - Release to Maven Central (manual or tag)
- **release-staging-api.yml** - Advanced release with manual staging control

