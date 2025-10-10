╔════════════════════════════════════════════════════════════════╗
║            IMPORTANT: Package Migration Required               ║
╚════════════════════════════════════════════════════════════════╝
BEFORE setting up CI/CD, you MUST migrate the package name!
Current:  com.springvision          ❌ (cannot be used)
Required: io.github.codesapienbe    ✅ (matches your GitHub)
═══════════════════════════════════════════════════════════════
QUICK START (3 commands):
1. Preview what will change (safe, no modifications):
   ./scripts/migrate-package-name.sh --dry-run
2. Run the migration:
   ./scripts/migrate-package-name.sh
3. Test and commit:
   mvn clean install -DskipTests
   git add .
   git commit -m "Migrate package to io.github.codesapienbe"
═══════════════════════════════════════════════════════════════
SAFETY FEATURES:
✓ Full backup created automatically
✓ One-command rollback if needed
✓ Dry-run mode to preview changes
✓ Git-friendly (easy to review/revert)
DOCUMENTATION:
• Quick guide: PACKAGE_MIGRATION_QUICK_START.md
• Full details: PACKAGE_MIGRATION.md
• First-time setup: FIRST_TIME_SETUP.md
═══════════════════════════════════════════════════════════════
The script is SAFE to run. Everything is backed up!
To rollback: ./backup-TIMESTAMP/ROLLBACK.sh
