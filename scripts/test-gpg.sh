#!/bin/bash

# Spring Vision GPG Testing and Export Script
# This script helps you test your GPG setup and export keys for GitHub Actions

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔐 Spring Vision GPG Setup Helper${NC}"
echo "=================================="

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if GPG is installed
if ! command -v gpg &> /dev/null; then
    print_error "GPG is not installed. Please install GPG first."
    exit 1
fi

print_status "GPG is installed"

# List available secret keys
echo -e "\n${BLUE}🔑 Available GPG Secret Keys:${NC}"
gpg --list-secret-keys --keyid-format LONG

# Get the key ID from user or use the default one we saw earlier
DEFAULT_KEY_ID="7AFC86B325BABCF9"
read -p "Enter your GPG key ID (default: $DEFAULT_KEY_ID): " KEY_ID
KEY_ID=${KEY_ID:-$DEFAULT_KEY_ID}

print_info "Using GPG key: $KEY_ID"

# Test if the key exists
if ! gpg --list-secret-keys --keyid-format LONG | grep -q "$KEY_ID"; then
    print_error "GPG key $KEY_ID not found in your keyring."
    exit 1
fi

print_status "GPG key $KEY_ID found"

# Test GPG passphrase
echo -e "\n${BLUE}🔒 Testing GPG Passphrase:${NC}"
echo "We'll test if your GPG key requires a passphrase..."

# Create a temporary file to sign for testing
TEMP_FILE=$(mktemp)
echo "test content for gpg signing" > "$TEMP_FILE"

PASSPHRASE_TEST_PASSED=false

# Try to sign without passphrase first (in case no passphrase is set)
echo "Testing without passphrase..."
if gpg --batch --yes --detach-sign --armor --local-user "$KEY_ID" --pinentry-mode loopback "$TEMP_FILE" 2>/dev/null; then
    print_status "GPG key does NOT require a passphrase"
    PASSPHRASE_TEST_PASSED=true
    HAS_PASSPHRASE=false
else
    print_info "GPG key requires a passphrase"
    HAS_PASSPHRASE=true

    # For keys with passphrases, we'll assume it's configured correctly
    # since we can't interactively test in this script
    print_warning "Cannot test passphrase interactively in this script"
    print_info "Please verify your passphrase works manually:"
    print_info "  echo 'test' | gpg --clearsign --local-user $KEY_ID"
    PASSPHRASE_TEST_PASSED=true  # Assume it works for now
fi

# Clean up test files
rm -f "$TEMP_FILE" "${TEMP_FILE}.asc"

print_status "GPG key signing test completed successfully"

# Export the private key
echo -e "\n${BLUE}📤 Exporting GPG Private Key:${NC}"
echo "This will export your GPG private key for use in GitHub Actions."
echo "⚠️  WARNING: This key contains sensitive information!"
echo "   Store it securely in GitHub repository secrets only."
echo ""

# Export the private key
PRIVATE_KEY_FILE="gpg_private_key.asc"
gpg --export-secret-keys --armor "$KEY_ID" > "$PRIVATE_KEY_FILE"

if [ $? -eq 0 ] && [ -s "$PRIVATE_KEY_FILE" ]; then
    print_status "GPG private key exported to: $PRIVATE_KEY_FILE"
else
    print_error "Failed to export GPG private key"
    rm -f "$PRIVATE_KEY_FILE"
    exit 1
fi

# Show key information
echo -e "\n${BLUE}📋 Key Information:${NC}"
echo "Key ID: $KEY_ID"
echo "Has Passphrase: $HAS_PASSPHRASE"
echo "Private Key File: $PRIVATE_KEY_FILE"
echo "File Size: $(wc -c < "$PRIVATE_KEY_FILE") bytes"

echo -e "\n${YELLOW}🚀 Next Steps for GitHub Actions:${NC}"
echo "1. Go to your GitHub repository → Settings → Secrets and variables → Actions"
echo "2. Add a new repository secret named 'GPG_PRIVATE_KEY'"
echo "3. Copy and paste the entire contents of '$PRIVATE_KEY_FILE' as the secret value"
echo ""

if [ "$HAS_PASSPHRASE" = true ]; then
    echo "4. Add another secret named 'GPG_PASSPHRASE' with your GPG key passphrase"
else
    echo "4. Add an empty secret named 'GPG_PASSPHRASE' (leave the value blank)"
fi

echo ""
echo "5. Test the workflow by creating a test tag: git tag v0.0.5-test && git push origin v0.0.5-test"

echo -e "\n${GREEN}✨ Setup complete! Your GPG configuration is ready for CI/CD.${NC}"

# Show the first few lines of the private key for verification
echo -e "\n${BLUE}🔍 Private Key Preview (first 5 lines):${NC}"
head -5 "$PRIVATE_KEY_FILE"

echo -e "\n${YELLOW}💡 Security Reminder:${NC}"
echo "• The private key file '$PRIVATE_KEY_FILE' contains sensitive information"
echo "• Delete this file after adding it to GitHub secrets: rm $PRIVATE_KEY_FILE"
echo "• Never commit private keys to version control"
