# 🚀 First-Time Initialization Guide

## Quick Start (TL;DR)

```bash
# Run the automated setup script
./scripts/setup-ci-cd.sh

# Then add GitHub secrets and push
git add .gitignore
git commit -m "Configure CI/CD"
git push origin main
```

---

## Detailed Step-by-Step Guide

### Prerequisites Check

Ensure you have these installed:

- ✅ **Java 21+** - `java -version`
- ✅ **Maven 3.8+** - `mvn -version`
- ✅ **GPG** - `gpg --version`
- ✅ **Git** - `git --version`

Install missing tools:

```bash
# Ubuntu/Debian
sudo apt-get install openjdk-21-jdk maven gnupg git

# macOS
brew install openjdk@21 maven gnupg git

# Arch Linux
sudo pacman -S jdk21-openjdk maven gnupg git
```

---

## Step 1: Run the Setup Script (Easiest)

```bash
cd /path/to/spring-vision
./scripts/setup-ci-cd.sh
```

This script will:

1. ✅ Check all prerequisites
2. ✅ Generate or use existing GPG key
3. ✅ Upload GPG public key to keyserver
4. ✅ Export GPG private key
5. ✅ Collect Sonatype OSSRH credentials
6. ✅ Create Maven settings.xml
7. ✅ Generate GitHub secrets reference file

**Follow the prompts and save the output!**

---

## Step 2: Manual Setup (Alternative)

If you prefer manual setup or the script fails:

### 2.1. Generate GPG Key

```bash
# Generate a new key
gpg --full-generate-key

# Choose these options:
# - Type: (1) RSA and RSA
# - Key size: 4096
# - Expiration: 0 (no expiration)
# - Real name: Your Name
# - Email: your-email@example.com
# - Passphrase: [choose a strong passphrase]

# List your keys to get the key ID
gpg --list-secret-keys --keyid-format=long

# Output will look like:
# sec   rsa4096/ABCD1234EFGH5678 2025-10-10 [SC]
#                ^^^^^^^^^^^^^^^^
#                This is your KEY_ID

# Upload public key to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# Export private key (save this for GitHub)
gpg --armor --export-secret-keys YOUR_KEY_ID > gpg-private-key.asc
```

### 2.2. Register with Sonatype OSSRH

```bash
# 1. Go to: https://issues.sonatype.org/
# 2. Create an account (click "Sign up")
# 3. Login and create a new issue:
#    - Project: Community Support - Open Source Project Repository Hosting (OSSRH)
#    - Issue Type: New Project
#    - Summary: Request for com.springvision
#    - Group Id: com.springvision
#    - Project URL: https://github.com/codesapienbe/spring-vision
#    - SCM URL: https://github.com/codesapienbe/spring-vision.git
# 4. Wait for approval (usually 1-2 business days)
# 5. You'll receive a JIRA ticket number (e.g., OSSRH-12345)
```

### 2.3. Create Maven Settings

Create `~/.m2/settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <servers>
        <server>
            <id>ossrh</id>
            <username>YOUR_SONATYPE_USERNAME</username>
            <password>YOUR_SONATYPE_PASSWORD_OR_TOKEN</password>
        </server>
    </servers>

    <profiles>
        <profile>
            <id>ossrh</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>gpg</gpg.executable>
                <gpg.keyname>YOUR_GPG_KEY_ID</gpg.keyname>
                <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
            </properties>
        </profile>
    </profiles>
</settings>
```

---

## Step 3: Configure GitHub Secrets

Go to your GitHub repository:
**Settings → Secrets and variables → Actions → New repository secret**

Add these 4 secrets:

| Secret Name         | Where to Get the Value                                                         |
|---------------------|--------------------------------------------------------------------------------|
| **OSSRH_USERNAME**  | Your Sonatype JIRA username                                                    |
| **OSSRH_TOKEN**     | Your Sonatype JIRA password or [generate token](https://s01.oss.sonatype.org/) |
| **GPG_PRIVATE_KEY** | Contents of `gpg-private-key.asc` (entire file including headers)              |
| **GPG_PASSPHRASE**  | The passphrase you set when creating the GPG key                               |

### Getting the GPG Private Key:

```bash
# View the key (copy ALL of this output)
cat gpg-private-key.asc

# Or copy to clipboard:
# macOS:
cat gpg-private-key.asc | pbcopy

# Linux (with xclip):
cat gpg-private-key.asc | xclip -selection clipboard
```

**Important**: Copy the ENTIRE key including:

```
-----BEGIN PGP PRIVATE KEY BLOCK-----
...
-----END PGP PRIVATE KEY BLOCK-----
```

---

## Step 4: Test Locally (Optional but Recommended)

```bash
# Test build
mvn clean install -DskipTests

# Test deployment (dry-run, won't actually deploy without GPG passphrase in prompt)
mvn clean deploy -Prelease -DskipTests -pl "!spring-vision-examples" -DaltDeploymentRepository=local::file:./target/staging-deploy

# If everything looks good, test real deployment
mvn clean deploy -Prelease -DskipTests -pl "!spring-vision-examples"
```

---

## Step 5: Commit and Push

```bash
# Add .ci-secrets to .gitignore (if not already there)
echo ".ci-secrets/" >> .gitignore

# Commit the CI/CD configuration
git add .github/ .gitignore Makefile pom.xml scripts/
git commit -m "Configure CI/CD for Maven Central deployment"
git push origin main
```

**This will automatically**:

- ✅ Run build and tests
- ✅ Deploy SNAPSHOT to Maven Central (if version ends with -SNAPSHOT)

---

## Step 6: Create Your First Release

### Option A: GitHub UI (Recommended)

1. Go to **GitHub Actions** tab
2. Select **"Release to Maven Central"** workflow
3. Click **"Run workflow"**
4. Enter version (e.g., `1.0.0`)
5. Click **"Run workflow"**
6. Wait 5-10 minutes for completion

### Option B: Git Tag

```bash
# Set release version
mvn versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false

# Commit and tag
git add pom.xml */pom.xml
git commit -m "Release version 1.0.0"
git tag -a v1.0.0 -m "Release 1.0.0"

# Push tag (this triggers the release workflow)
git push origin v1.0.0
```

### Option C: Local Deployment

```bash
# Deploy locally (uses settings.xml)
make maven-deploy
```

---

## Step 7: Verify Deployment

### Check Staging Repository

1. Go to: https://s01.oss.sonatype.org/
2. Login with your Sonatype credentials
3. Click **"Staging Repositories"** on the left
4. Look for `comspringvision-XXXX` repository
5. Check the **"Activity"** tab for validation results

### Check Maven Central

1. Wait 10-30 minutes for sync
2. Go to: https://search.maven.org/
3. Search for: `com.springvision`
4. Your artifacts should appear!

### Test the Dependency

Create a test project:

```xml

<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Troubleshooting

### Issue: "401 Unauthorized"

- ✅ Check GitHub secrets are correctly set
- ✅ Verify OSSRH credentials at https://s01.oss.sonatype.org/
- ✅ Ensure you have permission for groupId `com.springvision`

### Issue: "gpg: signing failed"

- ✅ Verify GPG_PRIVATE_KEY includes full key (headers + footer)
- ✅ Check GPG_PASSPHRASE is correct
- ✅ Test locally: `echo "test" | gpg --clearsign`

### Issue: "No public key"

- ✅ Upload key to keyserver: `gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID`
- ✅ Wait 5-10 minutes for keyserver propagation
- ✅ Try alternative keyserver: `keys.openpgp.org`

### Issue: "Repository validation failed"

- ✅ Check pom.xml has `<licenses>`, `<developers>`, `<scm>` (already configured)
- ✅ Ensure all artifacts are signed
- ✅ Verify javadoc and sources JARs are generated

### Need Help?

- 📖 See `.github/DEPLOYMENT.md` for complete troubleshooting
- 🔍 Check workflow logs in GitHub Actions
- 💬 Review Sonatype Nexus activity logs

---

## Quick Reference

| Action              | Command                            |
|---------------------|------------------------------------|
| **Setup**           | `./scripts/setup-ci-cd.sh`         |
| **Build**           | `mvn clean install`                |
| **Test**            | `mvn test`                         |
| **Deploy Snapshot** | `git push origin main` (automatic) |
| **Release**         | GitHub Actions → Run workflow      |
| **Local Deploy**    | `make maven-deploy`                |

---

## Summary

✅ **Initial Setup**: 30-45 minutes (mostly waiting for Sonatype approval)
✅ **Subsequent Deploys**: < 10 minutes (fully automated)
✅ **Required Once**: GPG key, Sonatype account, GitHub secrets
✅ **Required Per Release**: Just trigger the workflow!

**You're now ready to deploy to Maven Central! 🚀**

