#!/bin/bash
# Spring Vision CI/CD First-Time Setup Script
# This script helps you set up CI/CD for Maven Central deployment

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║   Spring Vision CI/CD - First-Time Setup                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${BLUE}[1/7] Checking Prerequisites...${NC}"
echo ""

# Check Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo -e "${GREEN}✓${NC} Java $JAVA_VERSION found"
    else
        echo -e "${RED}✗${NC} Java 21+ required (found Java $JAVA_VERSION)"
        exit 1
    fi
else
    echo -e "${RED}✗${NC} Java not found. Please install Java 21+"
    exit 1
fi

# Check Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -1 | cut -d' ' -f3)
    echo -e "${GREEN}✓${NC} Maven $MVN_VERSION found"
else
    echo -e "${RED}✗${NC} Maven not found. Please install Maven 3.8+"
    exit 1
fi

# Check GPG
if command -v gpg &> /dev/null; then
    GPG_VERSION=$(gpg --version | head -1 | cut -d' ' -f3)
    echo -e "${GREEN}✓${NC} GPG $GPG_VERSION found"
else
    echo -e "${YELLOW}⚠${NC} GPG not found. Install with: sudo apt-get install gnupg (or brew install gnupg)"
fi

# Check Git
if command -v git &> /dev/null; then
    GIT_VERSION=$(git --version | cut -d' ' -f3)
    echo -e "${GREEN}✓${NC} Git $GIT_VERSION found"
else
    echo -e "${RED}✗${NC} Git not found"
    exit 1
fi

echo ""
echo -e "${BLUE}[2/7] Checking GPG Key...${NC}"
echo ""

# Check if GPG key exists
GPG_KEYS=$(gpg --list-secret-keys --keyid-format=long 2>/dev/null | grep -c "sec" || echo "0")
if [ "$GPG_KEYS" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Found $GPG_KEYS GPG key(s)"
    gpg --list-secret-keys --keyid-format=long
    echo ""
    read -p "Do you want to use an existing key? (y/n): " USE_EXISTING
    if [ "$USE_EXISTING" != "y" ]; then
        CREATE_NEW_KEY=true
    else
        CREATE_NEW_KEY=false
        echo "Enter your GPG Key ID (from the output above, e.g., ABCD1234EFGH5678):"
        read GPG_KEY_ID
    fi
else
    echo -e "${YELLOW}⚠${NC} No GPG keys found"
    CREATE_NEW_KEY=true
fi

if [ "$CREATE_NEW_KEY" = true ]; then
    echo ""
    echo "Creating a new GPG key..."
    echo "Please follow the prompts:"
    echo "  - Choose: (1) RSA and RSA"
    echo "  - Key size: 4096"
    echo "  - Expiration: 0 (no expiration) or your preference"
    echo "  - Enter your name and email"
    echo "  - Choose a strong passphrase"
    echo ""
    read -p "Press Enter to start GPG key generation..."
    gpg --full-generate-key

    # Get the new key ID
    GPG_KEY_ID=$(gpg --list-secret-keys --keyid-format=long | grep "sec" | tail -1 | awk '{print $2}' | cut -d'/' -f2)
    echo -e "${GREEN}✓${NC} New GPG key created: $GPG_KEY_ID"
fi

echo ""
echo -e "${BLUE}[3/7] Uploading GPG Public Key to Keyserver...${NC}"
echo ""

echo "Uploading public key to keyserver.ubuntu.com..."
if gpg --keyserver keyserver.ubuntu.com --send-keys "$GPG_KEY_ID" 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Public key uploaded successfully"
else
    echo -e "${YELLOW}⚠${NC} Failed to upload to keyserver.ubuntu.com, trying keys.openpgp.org..."
    gpg --keyserver keys.openpgp.org --send-keys "$GPG_KEY_ID" || echo -e "${YELLOW}⚠${NC} Manual upload may be needed"
fi

echo ""
echo -e "${BLUE}[4/7] Exporting GPG Private Key...${NC}"
echo ""

# Export private key
mkdir -p .ci-secrets
GPG_PRIVATE_KEY_FILE=".ci-secrets/gpg-private-key.asc"
gpg --armor --export-secret-keys "$GPG_KEY_ID" > "$GPG_PRIVATE_KEY_FILE"
echo -e "${GREEN}✓${NC} Private key exported to: $GPG_PRIVATE_KEY_FILE"

# Get passphrase
echo ""
echo "Enter your GPG passphrase (will not be shown):"
read -s GPG_PASSPHRASE
echo ""

echo ""
echo -e "${BLUE}[5/7] Sonatype OSSRH Credentials...${NC}"
echo ""

echo "Do you have a Sonatype OSSRH account? (y/n)"
read HAS_OSSRH

if [ "$HAS_OSSRH" != "y" ]; then
    echo ""
    echo -e "${YELLOW}You need to create a Sonatype OSSRH account:${NC}"
    echo "1. Go to: https://issues.sonatype.org/"
    echo "2. Create an account"
    echo "3. Create a JIRA ticket to claim groupId 'com.springvision'"
    echo "4. Wait for approval (usually 1-2 business days)"
    echo ""
    echo "Come back and run this script again after approval."
    exit 0
fi

echo "Enter your Sonatype OSSRH username:"
read OSSRH_USERNAME

echo "Enter your Sonatype OSSRH password/token (will not be shown):"
read -s OSSRH_TOKEN
echo ""

echo ""
echo -e "${BLUE}[6/7] Creating Local Maven Settings...${NC}"
echo ""

# Create Maven settings.xml
mkdir -p ~/.m2
SETTINGS_FILE="$HOME/.m2/settings.xml"
BACKUP_FILE="$HOME/.m2/settings.xml.backup-$(date +%Y%m%d-%H%M%S)"

if [ -f "$SETTINGS_FILE" ]; then
    echo "Backing up existing settings.xml to: $BACKUP_FILE"
    cp "$SETTINGS_FILE" "$BACKUP_FILE"
fi

cat > "$SETTINGS_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <server>
      <id>ossrh</id>
      <username>${OSSRH_USERNAME}</username>
      <password>${OSSRH_TOKEN}</password>
    </server>
    <server>
      <id>ossrh-snapshots</id>
      <username>${OSSRH_USERNAME}</username>
      <password>${OSSRH_TOKEN}</password>
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
        <gpg.keyname>${GPG_KEY_ID}</gpg.keyname>
        <gpg.passphrase>${GPG_PASSPHRASE}</gpg.passphrase>
        <gpg.useagent>true</gpg.useagent>
      </properties>
    </profile>
  </profiles>
</settings>
EOF

echo -e "${GREEN}✓${NC} Maven settings.xml created at: $SETTINGS_FILE"

echo ""
echo -e "${BLUE}[7/7] GitHub Secrets Configuration...${NC}"
echo ""

echo "You need to add the following secrets to your GitHub repository:"
echo ""
echo -e "${YELLOW}Go to: Settings → Secrets and variables → Actions → New repository secret${NC}"
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  Secret Name         │  Secret Value                           ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║  OSSRH_USERNAME      │  $OSSRH_USERNAME"
echo "║  OSSRH_TOKEN         │  [your password/token]"
echo "║  GPG_KEY_ID          │  $GPG_KEY_ID"
echo "║  GPG_PASSPHRASE      │  [your GPG passphrase]"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "For GPG_PRIVATE_KEY, copy the ENTIRE contents of this file:"
echo "  $GPG_PRIVATE_KEY_FILE"
echo ""
echo "To view the file contents:"
echo "  cat $GPG_PRIVATE_KEY_FILE"
echo ""

# Save secrets to a file for reference
SECRETS_FILE=".ci-secrets/github-secrets.txt"
cat > "$SECRETS_FILE" << EOF
GitHub Secrets Configuration
=============================

Add these secrets in your GitHub repository:
Settings → Secrets and variables → Actions → New repository secret

1. OSSRH_USERNAME
   Value: $OSSRH_USERNAME

2. OSSRH_TOKEN
   Value: [paste your Sonatype OSSRH password/token]

3. GPG_PRIVATE_KEY
   Value: [paste ENTIRE contents of file below]
   File: $GPG_PRIVATE_KEY_FILE

   To copy the file contents:
   cat $GPG_PRIVATE_KEY_FILE | pbcopy  # macOS
   cat $GPG_PRIVATE_KEY_FILE | xclip -selection clipboard  # Linux

4. GPG_PASSPHRASE
   Value: [paste your GPG passphrase]

GPG Key ID: $GPG_KEY_ID
EOF

echo -e "${GREEN}✓${NC} Secrets reference saved to: $SECRETS_FILE"
echo ""

# Add .ci-secrets to .gitignore
if [ -f .gitignore ]; then
    if ! grep -q ".ci-secrets" .gitignore; then
        echo ".ci-secrets/" >> .gitignore
        echo -e "${GREEN}✓${NC} Added .ci-secrets/ to .gitignore"
    fi
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                    Setup Complete! 🎉                          ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Next Steps:"
echo ""
echo "1. ${YELLOW}Add GitHub Secrets${NC} (see above for values)"
echo "   Reference file: $SECRETS_FILE"
echo ""
echo "2. ${YELLOW}Test Local Build${NC}:"
echo "   mvn clean install -DskipTests"
echo ""
echo "3. ${YELLOW}Test Local Deployment${NC} (optional):"
echo "   mvn clean deploy -Prelease -DskipTests -pl \"!spring-vision-examples\""
echo ""
echo "4. ${YELLOW}Commit and Push${NC}:"
echo "   git add .gitignore"
echo "   git commit -m \"Configure CI/CD for Maven Central deployment\""
echo "   git push origin main"
echo ""
echo "5. ${YELLOW}Create Your First Release${NC}:"
echo "   - Go to GitHub Actions tab"
echo "   - Select 'Release to Maven Central' workflow"
echo "   - Click 'Run workflow'"
echo "   - Enter version (e.g., 1.0.0)"
echo ""
echo "For more information, see: .github/DEPLOYMENT.md"
echo ""

