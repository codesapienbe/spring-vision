#!/bin/bash
# Safe deployment script with retry logic for Maven Central
# Usage: ./deploy-to-central.sh [module-name]

set -e

MODULE="${1:-}"
MAX_RETRIES=3
RETRY_DELAY=10

echo "==================================="
echo "Maven Central Deployment Script"
echo "==================================="

if [ -n "$MODULE" ]; then
    echo "Deploying module: $MODULE"
    cd "$MODULE"
fi

# Function to deploy with retry
deploy_with_retry() {
    local attempt=1

    while [ $attempt -le $MAX_RETRIES ]; do
        echo ""
        echo "Deployment attempt $attempt of $MAX_RETRIES..."

        if mvn clean deploy -Prelease -DskipTests -Dgpg.skip=false; then
            echo "✅ Deployment successful!"
            return 0
        else
            local exit_code=$?
            echo "❌ Deployment attempt $attempt failed with exit code $exit_code"

            if [ $attempt -lt $MAX_RETRIES ]; then
                echo "⏳ Waiting ${RETRY_DELAY}s before retry..."
                sleep $RETRY_DELAY
                # Exponential backoff
                RETRY_DELAY=$((RETRY_DELAY * 2))
            fi

            attempt=$((attempt + 1))
        fi
    done

    echo "❌ All deployment attempts failed!"
    return 1
}

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven."
    exit 1
fi

if ! command -v gpg &> /dev/null; then
    echo "❌ GPG not found. Please install GPG."
    exit 1
fi

# Check GPG key
if ! gpg --list-secret-keys | grep -q "sec"; then
    echo "❌ No GPG secret key found. Please configure GPG signing."
    exit 1
fi

echo "✅ Prerequisites OK"

# Deploy
if deploy_with_retry; then
    echo ""
    echo "==================================="
    echo "✅ Deployment completed successfully!"
    echo "==================================="
    exit 0
else
    echo ""
    echo "==================================="
    echo "❌ Deployment failed after $MAX_RETRIES attempts"
    echo "==================================="
    echo ""
    echo "Troubleshooting tips:"
    echo "1. Check network connectivity"
    echo "2. Verify Maven Central credentials in ~/.m2/settings.xml"
    echo "3. Ensure GPG key is properly configured"
    echo "4. Check if artifacts are too large (may need to split deployment)"
    echo "5. Try deploying individual modules: ./deploy-to-central.sh <module>"
    echo ""
    exit 1
fi

