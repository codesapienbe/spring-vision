#!/bin/bash
# Script to validate OSSRH credentials without attempting a full deployment

set -e

SETTINGS_FILE="${1:-$HOME/.m2/settings.xml}"

if [ ! -f "$SETTINGS_FILE" ]; then
    echo "ERROR: Settings file not found: $SETTINGS_FILE"
    exit 1
fi

echo "Validating OSSRH credentials from: $SETTINGS_FILE"
echo "================================================"

# Extract username and password from settings.xml using grep and sed
# Find the ossrh server block and extract username/password
USERNAME=$(grep -A 3 '<id>ossrh</id>' "$SETTINGS_FILE" | grep '<username>' | sed 's/.*<username>\(.*\)<\/username>.*/\1/' | tr -d ' ')
PASSWORD=$(grep -A 3 '<id>ossrh</id>' "$SETTINGS_FILE" | grep '<password>' | sed 's/.*<password>\(.*\)<\/password>.*/\1/' | tr -d ' ')

if [ -z "$USERNAME" ] || [ -z "$PASSWORD" ]; then
    echo "ERROR: Could not extract username or password from settings.xml"
    echo "Make sure the <server> entry with id 'ossrh' exists and has <username> and <password> tags"
    echo ""
    echo "Expected structure:"
    echo "  <server>"
    echo "    <id>ossrh</id>"
    echo "    <username>YOUR_USERNAME</username>"
    echo "    <password>YOUR_PASSWORD</password>"
    echo "  </server>"
    exit 1
fi

echo "Username: $USERNAME"
echo "Password: ${PASSWORD:0:5}... (masked)"
echo ""
echo "Testing authentication against Sonatype OSSRH..."

# Test authentication with a simple API call
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -u "$USERNAME:$PASSWORD" \
    "https://s01.oss.sonatype.org/service/local/status")

echo "HTTP Response Code: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ SUCCESS: Credentials are valid!"
    exit 0
elif [ "$HTTP_CODE" = "401" ]; then
    echo "✗ FAILURE: Authentication failed (401 Unauthorized)"
    echo ""
    echo "Possible causes:"
    echo "  1. Username or password/token is incorrect"
    echo "  2. Token has expired (Sonatype tokens expire after a period of inactivity)"
    echo "  3. Account has been deactivated"
    echo ""
    echo "Next steps:"
    echo "  1. Log in to https://s01.oss.sonatype.org/"
    echo "  2. Go to your profile (top right) -> User Token"
    echo "  3. Generate a new user token"
    echo "  4. Update ~/.m2/settings.xml with the new token"
    exit 1
else
    echo "✗ UNEXPECTED: Received HTTP $HTTP_CODE"
    echo "This may indicate a network issue or service problem"
    exit 1
fi
