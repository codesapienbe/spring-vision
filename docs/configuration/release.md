# Setting Up GitHub App Permissions for Releases

This document explains how to configure a GitHub App with the necessary permissions to create releases and upload assets in your repository.

## Prerequisites

- Owner access to the GitHub repository
- GitHub App created (or create one if needed)

## Step 1: Create or Access Your GitHub App

1. Go to [GitHub Apps](https://github.com/settings/apps)
2. Click **"New GitHub App"** (or select an existing app)
3. Fill in the basic information:
    - **GitHub App name**: Choose a descriptive name (e.g., "Spring Vision Release Bot")
    - **Homepage URL**: Your repository URL or a placeholder
    - **Description**: Brief description of the app's purpose

## Step 2: Configure Repository Permissions

In the GitHub App settings, scroll to **"Repository permissions"** and set:

- **Contents**: `Read and write`
    - This allows the app to create releases and upload release assets

All other permissions can remain at their default settings (`No access` or `Read-only`).

## Step 3: Generate and Download Private Key

1. In the GitHub App settings, scroll to **"Private keys"**
2. Click **"Generate a private key"**
3. Download the `.pem` file (keep it secure!)

## Step 4: Install the App in Your Repository

1. In the GitHub App settings, scroll to **"Install App"**
2. Click **"Install"** next to your repository
3. Select the repository and choose **"All repositories"** or specific ones
4. Complete the installation

## Step 5: Configure Repository Secrets and Variables

In your repository settings (`Settings` → `Secrets and Variables` → `Actions`):

### Repository Variables

- **Name**: `APP_ID`
- **Value**: The App ID from your GitHub App settings (found under the app name)

### Repository Secrets

- **Name**: `APP_PRIVATE_KEY`
- **Value**: The entire content of the downloaded `.pem` file (including `-----BEGIN RSA PRIVATE KEY-----` and `-----END RSA PRIVATE KEY-----`)

## Step 6: Verify Configuration

1. Push a version tag to trigger the release workflow:
   ```bash
   git tag v0.0.4
   git push origin v0.0.4
   ```

2. Check the Actions tab to ensure the workflow runs without permission errors

## Troubleshooting

### "Resource not accessible by integration" Error

- Ensure the GitHub App has "Contents: Read and write" permission
- Verify the app is installed in the repository
- Check that `APP_ID` and `APP_PRIVATE_KEY` are correctly set

### App Not Found Error

- Confirm the `APP_ID` matches exactly (no extra spaces)
- Ensure the private key is the full content of the `.pem` file

### Permission Denied

- Reinstall the app in the repository after changing permissions
- Wait a few minutes for permission changes to propagate

## Security Notes

- Keep the private key secure and never commit it to the repository
- Regularly rotate the private key if needed
- Only grant the minimum required permissions

## Support

If you encounter issues, check the GitHub Actions logs for detailed error messages and ensure all steps above are completed correctly.
