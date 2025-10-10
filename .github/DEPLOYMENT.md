# Deployment Guide for Spring Vision

This guide explains how to configure and use the CI/CD pipelines for deploying Spring Vision to Maven Central.

## Prerequisites

Before you can deploy to Maven Central, you need:

1. **Sonatype OSSRH Account**
    - Register at https://issues.sonatype.org/
    - Create a JIRA ticket to claim your groupId (com.springvision)
    - Get your username and password/token

2. **GPG Key for Signing**
    - Generate a GPG key pair if you don't have one
    - Upload your public key to a key server

3. **GitHub Repository Secrets**
    - Configure the required secrets in your repository

## Setting Up GitHub Secrets

Go to your repository settings → Secrets and variables → Actions, and add these secrets:

### Required Secrets

1. **OSSRH_USERNAME**
    - Your Sonatype OSSRH username (JIRA username)
    - Example: `your-sonatype-username`

2. **OSSRH_TOKEN**
    - Your Sonatype OSSRH password or token
    - Alternatively, use your JIRA password
    - For better security, generate a user token from https://s01.oss.sonatype.org/

3. **GPG_PRIVATE_KEY**
    - Your GPG private key in ASCII armor format
    - Export it using: `gpg --armor --export-secret-keys YOUR_KEY_ID`
    - Copy the entire output including `-----BEGIN PGP PRIVATE KEY BLOCK-----` and `-----END PGP PRIVATE KEY BLOCK-----`

4. **GPG_PASSPHRASE**
    - The passphrase for your GPG key
    - Leave empty if your key doesn't have a passphrase

## Generating a GPG Key

If you don't have a GPG key, generate one:

```bash
# Generate a new key
gpg --full-generate-key

# Choose:
# - RSA and RSA (default)
# - 4096 bits
# - Key does not expire (or set an expiration)
# - Enter your name and email
# - Set a strong passphrase

# List your keys to get the key ID
gpg --list-secret-keys --keyid-format=long

# Export your private key (replace YOUR_KEY_ID)
gpg --armor --export-secret-keys YOUR_KEY_ID

# Upload your public key to a key server
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

## CI/CD Workflows

### 1. Build and Test (Automatic)

**Workflow**: `.github/workflows/build.yml`

Runs automatically on:

- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches

**What it does**:

- Builds the project
- Runs all tests
- Checks code formatting with Spotless
- Uploads test results and coverage reports

### 2. Deploy Snapshot (Automatic)

**Workflow**: `.github/workflows/deploy-snapshot.yml`

Runs automatically on:

- Push to `main` branch (excluding maven-release-plugin commits)
- Manual trigger via workflow dispatch

**What it does**:

- Ensures version ends with `-SNAPSHOT`
- Builds and signs artifacts
- Deploys to Maven Central snapshot repository
- Available at: https://s01.oss.sonatype.org/content/repositories/snapshots/

### 3. Release to Maven Central (Manual)

**Workflow**: `.github/workflows/release.yml`

Runs on:

- Manual trigger via workflow dispatch (specify version)
- Push of version tags (e.g., `v1.0.0`)

**What it does**:

- Builds all artifacts
- Signs with GPG
- Deploys to Maven Central staging
- Auto-releases to Maven Central (if autoReleaseAfterClose=true)
- Creates GitHub release with artifacts

**To trigger manually**:

1. Go to Actions tab in GitHub
2. Select "Release to Maven Central" workflow
3. Click "Run workflow"
4. Enter the release version (e.g., `1.0.0`)
5. Click "Run workflow"

### 4. Release via Nexus Staging API (Advanced)

**Workflow**: `.github/workflows/release-staging-api.yml`

This workflow provides more control over the staging process:

**What it does**:

- Deploys to staging without auto-release
- Retrieves staging repository ID
- Explicitly closes the staging repository
- Waits for validation
- Releases to Maven Central
- Creates GitHub release

**Use this when**:

- You want to verify artifacts before release
- You need to control the release timing
- You want to manually test the staging repository

## Deployment Methods

### Method 1: Automatic Snapshot Deployment

Every push to `main` automatically deploys a SNAPSHOT version:

```bash
git push origin main
```

Users can then consume snapshots:

```xml

<repositories>
    <repository>
        <id>ossrh-snapshots</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
<groupId>com.springvision</groupId>
<artifactId>spring-vision-starter</artifactId>
<version>1.0-SNAPSHOT</version>
</dependency>
```

### Method 2: Release via Git Tag

```bash
# Update version in pom.xml to release version (remove -SNAPSHOT)
mvn versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false

# Commit and create tag
git add pom.xml */pom.xml
git commit -m "Release version 1.0.0"
git tag -a v1.0.0 -m "Release 1.0.0"

# Push tag to trigger release
git push origin v1.0.0
```

### Method 3: Manual Workflow Trigger

1. Go to GitHub Actions
2. Select "Release to Maven Central"
3. Click "Run workflow"
4. Enter version: `1.0.0`
5. Click "Run workflow"

## Local Deployment (Manual)

You can also deploy locally if you have credentials configured:

```bash
# Create ~/.m2/settings.xml with your credentials
cat > ~/.m2/settings.xml << 'EOF'
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>YOUR_OSSRH_USERNAME</username>
      <password>YOUR_OSSRH_TOKEN</password>
    </server>
  </servers>
</settings>
EOF

# Deploy with the release profile
mvn clean deploy -Prelease -DskipTests -pl "!spring-vision-examples"
```

## Troubleshooting

### "401 Unauthorized" during deploy

- Verify your OSSRH credentials are correct
- Check that secrets are properly configured in GitHub
- Ensure your OSSRH account has permission for groupId `com.springvision`

### "gpg: signing failed: No such file or directory"

- Verify GPG_PRIVATE_KEY secret is properly set
- Ensure the key includes the complete ASCII armor format
- Check that GPG_PASSPHRASE matches your key

### "Repository validation failed"

- Ensure all required metadata is present (licenses, developers, scm)
- Verify all artifacts are signed
- Check that javadoc and sources jars are generated

### Artifacts not appearing in Maven Central

- Check staging repository at https://s01.oss.sonatype.org/
- Look for validation errors in the Nexus UI
- Wait 10-30 minutes for sync to Maven Central
- Check https://search.maven.org/ after sync

### Build fails with "encoding" errors

The pom.xml is already configured with UTF-8 encoding. If you still see issues:

- Ensure your source files are UTF-8 encoded
- Check for BOM (Byte Order Mark) in files

## Monitoring Deployments

### Check Deployment Status

1. **Sonatype Nexus**: https://s01.oss.sonatype.org/
    - Login with your OSSRH credentials
    - Check "Staging Repositories" for in-progress releases
    - View activity tab for validation results

2. **Maven Central**: https://search.maven.org/
    - Search for `com.springvision`
    - Can take 10-30 minutes to sync after release

3. **GitHub Actions**:
    - Check workflow runs in the Actions tab
    - View logs for detailed information

## Post-Release Steps

After a successful release:

1. **Update version to next SNAPSHOT**
   ```bash
   mvn versions:set -DnewVersion=1.1.0-SNAPSHOT -DgenerateBackupPoms=false
   git add pom.xml */pom.xml
   git commit -m "Prepare for next development iteration"
   git push origin main
   ```

2. **Announce the release**
    - Update README.md with new version
    - Create release notes
    - Notify users via GitHub Discussions

3. **Verify the release**
    - Check Maven Central search
    - Test dependency resolution in a sample project
    - Verify documentation is correct

## Security Best Practices

1. **Never commit credentials** to the repository
2. **Use GitHub Secrets** for all sensitive information
3. **Rotate tokens regularly** (at least yearly)
4. **Use strong GPG passphrases**
5. **Limit access** to repository secrets to trusted maintainers
6. **Monitor deployment logs** for suspicious activity

## Additional Resources

- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)
- [Nexus Staging Maven Plugin](https://github.com/sonatype/nexus-maven-plugins/tree/main/staging/maven-plugin)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

