# CI/CD Configuration Checklist

## ✅ Completed Setup

### GitHub Actions Workflows

- [x] `.github/workflows/build.yml` - Build and test on PR/push
- [x] `.github/workflows/deploy-snapshot.yml` - Auto-deploy snapshots
- [x] `.github/workflows/release.yml` - Release to Maven Central
- [x] `.github/workflows/release-staging-api.yml` - Advanced staging control

### Maven Configuration

- [x] Added `release` profile in pom.xml
- [x] Configured maven-source-plugin
- [x] Configured maven-javadoc-plugin
- [x] Configured maven-gpg-plugin
- [x] Configured nexus-staging-maven-plugin
- [x] Set up distributionManagement for OSSRH

### Documentation

- [x] `.github/DEPLOYMENT.md` - Complete deployment guide
- [x] `.github/QUICK_REFERENCE.md` - Quick reference
- [x] `.github/settings.xml.template` - Settings template

### Makefile Updates

- [x] `make maven-deploy` - Deploy with release profile
- [x] `make release` - Full release process
- [x] `make snapshot` - Deploy snapshot version

## 🔧 Required Setup (Action Items)

### 1. GitHub Repository Secrets

Add these secrets in GitHub Settings → Secrets and variables → Actions:

- [ ] `OSSRH_USERNAME` - Your Sonatype OSSRH username
- [ ] `OSSRH_TOKEN` - Your Sonatype OSSRH token/password
- [ ] `GPG_PRIVATE_KEY` - Your GPG private key (ASCII armor format)
- [ ] `GPG_PASSPHRASE` - Your GPG key passphrase

### 2. Sonatype OSSRH Account

- [ ] Register at https://issues.sonatype.org/
- [ ] Create JIRA ticket to claim groupId `com.springvision`
- [ ] Wait for approval (usually 1-2 business days)

### 3. GPG Key

- [ ] Generate GPG key if you don't have one
- [ ] Upload public key to keyserver.ubuntu.com
- [ ] Export private key for GitHub secret

### 4. Local Testing (Optional but Recommended)

- [ ] Create `~/.m2/settings.xml` with credentials
- [ ] Test local deployment: `mvn clean deploy -Prelease -DskipTests`
- [ ] Verify artifacts appear in staging repository

## 📝 Usage Examples

### Deploy SNAPSHOT (Automatic)

```bash
git push origin main
# Automatically deploys to snapshots repository
```

### Release New Version

```bash
# Method 1: Via GitHub Actions
# Go to Actions → "Release to Maven Central" → Run workflow → Enter version

# Method 2: Via Git Tag
mvn versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false
git commit -am "Release 1.0.0"
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0

# Method 3: Via Makefile (local)
make maven-deploy SETTINGS=~/.m2/settings.xml
```

## 🔍 Verification Steps

After deployment:

1. [ ] Check staging repository: https://s01.oss.sonatype.org/
2. [ ] Wait 10-30 minutes for Maven Central sync
3. [ ] Verify on Maven Central: https://search.maven.org/
4. [ ] Test dependency in sample project

## 🐛 Common Issues & Solutions

### Issue: 401 Unauthorized

**Solution**: Verify OSSRH credentials are correct in GitHub secrets

### Issue: GPG signing failed

**Solution**: Ensure GPG_PRIVATE_KEY includes complete ASCII armor format

### Issue: Repository validation failed

**Solution**: Check pom.xml has licenses, developers, and scm sections (already configured)

### Issue: Artifacts not in Maven Central

**Solution**: Wait 10-30 minutes, check staging repository for errors

## 📚 Resources

- Deployment Guide: `.github/DEPLOYMENT.md`
- Quick Reference: `.github/QUICK_REFERENCE.md`
- Settings Template: `.github/settings.xml.template`
- Sonatype Guide: https://central.sonatype.org/publish/publish-guide/

<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <!-- Sonatype OSSRH Server Configuration -->
    <server>
      <id>ossrh</id>
      <username>${env.OSSRH_USERNAME}</username>
      <password>${env.OSSRH_TOKEN}</password>
    </server>

    <!-- Snapshot Repository (optional, uses same credentials) -->
    <server>
      <id>ossrh-snapshots</id>
      <username>${env.OSSRH_USERNAME}</username>
      <password>${env.OSSRH_TOKEN}</password>
    </server>

  </servers>

  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <!-- GPG Configuration -->
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>${env.GPG_PASSPHRASE}</gpg.passphrase>
        <gpg.useagent>true</gpg.useagent>
      </properties>
    </profile>
  </profiles>
</settings>

