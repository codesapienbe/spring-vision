# Package Name Migration Guide

## ⚠️ Important: Package Name Correction

The correct package name for GitHub-based Maven Central deployment should be:

- ✅ **`io.github.codesapienbe`** (matches your GitHub username)
- ❌ ~~`com.springvision`~~ (incorrect, cannot be used)

## 🚀 Quick Migration

### Step 1: Run Dry-Run First (Safe Preview)

```bash
# Preview what will change without making any modifications
./scripts/migrate-package-name.sh --dry-run
```

This shows you exactly what would be changed without touching any files.

### Step 2: Run the Migration

```bash
# Execute the actual migration
./scripts/migrate-package-name.sh
```

### Step 3: Verify the Changes

```bash
# Check what changed
git status
git diff

# Test build
mvn clean install -DskipTests

# Run tests
mvn test
```

### Step 4: Commit or Rollback

**If everything works:**

```bash
git add .
git commit -m "Migrate package from com.springvision to io.github.codesapienbe"
```

**If something broke:**

```bash
# The script creates a timestamped backup, e.g., backup-20251010-143022/
./backup-TIMESTAMP/ROLLBACK.sh

# Or manually:
git reset --hard HEAD
git clean -fd
```

## 📋 What the Script Does

The migration script performs these operations **safely**:

1. ✅ **Creates Full Backup** - Everything backed up to `backup-YYYYMMDD-HHMMSS/`
2. ✅ **Updates Package Declarations** - All `package com.springvision.*` → `package io.github.codesapienbe.*`
3. ✅ **Updates Imports** - All `import com.springvision.*` → `import io.github.codesapienbe.*`
4. ✅ **Updates POM Files** - GroupId changed in all pom.xml files
5. ✅ **Updates Configuration** - All properties, yml, yaml files
6. ✅ **Updates Documentation** - All markdown files
7. ✅ **Moves Directory Structure** - `src/main/java/com/springvision/` → `src/main/java/io/github/codesapienbe/`
8. ✅ **Generates Rollback Script** - Automatic rollback if needed

## 🔍 What Gets Changed

### Java Files

```java
// Before:
package com.springvision.core;
import com.springvision.util.ImageUtils;

// After:
package io.github.codesapienbe.core;
import io.github.codesapienbe.util.ImageUtils;
```

### POM Files

```xml
<!-- Before: -->
<groupId>com.springvision</groupId>

<!-- After: -->
<groupId>io.github.codesapienbe</groupId>
```

### Directory Structure

```
Before:
spring-vision-core/src/main/java/com/springvision/...

After:
spring-vision-core/src/main/java/io/github/codesapienbe/...
```

## 🛡️ Safety Features

- ✅ **Complete Backup** - Everything backed up before changes
- ✅ **Automatic Rollback Script** - One command to undo everything
- ✅ **Dry-Run Mode** - Preview changes before applying
- ✅ **Verification** - Checks for remaining old references
- ✅ **Git-Friendly** - Easy to review with `git diff`

## 📊 Expected Results

The script will report:

- Number of Java files updated
- Number of import statements changed
- Number of POM files modified
- Number of configuration files updated
- Number of documentation files changed

## ⚠️ Important Notes

### Maven Central Requirements

For Maven Central, your groupId MUST match your GitHub domain:

- Your GitHub: `https://github.com/codesapienbe`
- Required groupId: `io.github.codesapienbe`

### After Migration

You'll need to claim the new groupId in Sonatype OSSRH:

1. Create a JIRA ticket at https://issues.sonatype.org/
2. Request: `io.github.codesapienbe`
3. Proof of ownership: Create a repo `github.com/codesapienbe/io.github.codesapienbe`
    - Or add TXT record to your domain if you own codesapien.be

## 🔧 Manual Verification (Optional)

After migration, you can manually verify:

```bash
# Check for any remaining old package references
grep -r "com.springvision" --include="*.java" --include="*.xml" --exclude-dir=target .

# Verify new package structure exists
find . -type d -path "*/io/github/codesapienbe"

# Verify POM files
grep -r "io.github.codesapienbe" pom.xml */pom.xml
```

## 🚑 Troubleshooting

### Issue: Build fails after migration

```bash
# Clean everything and rebuild
mvn clean
rm -rf ~/.m2/repository/com/springvision
rm -rf ~/.m2/repository/io/github/codesapienbe
mvn clean install -DskipTests
```

### Issue: Tests fail

- Check test configuration files (application-test.properties)
- Verify test imports are updated
- Check @SpringBootTest annotations with basePackages

### Issue: Need to rollback

```bash
# Find your backup directory
ls -la | grep backup-

# Run the rollback script
./backup-TIMESTAMP/ROLLBACK.sh

# Clean and verify
mvn clean install -DskipTests
```

## ✅ Post-Migration Checklist

- [ ] Dry-run completed successfully
- [ ] Migration script completed without errors
- [ ] Build succeeds: `mvn clean install -DskipTests`
- [ ] Tests pass: `mvn test`
- [ ] Git diff reviewed: `git diff`
- [ ] No old package references: `grep -r "com.springvision" --include="*.java" .`
- [ ] Changes committed: `git commit -m "Migrate to io.github.codesapienbe"`
- [ ] Update GitHub secrets with new groupId
- [ ] Create OSSRH ticket for new groupId
- [ ] Update README.md with new Maven coordinates

## 📚 Updated Maven Coordinates

After migration, users will depend on your library like this:

```xml
<dependency>
    <groupId>io.github.codesapienbe</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🎯 Next Steps After Migration

1. **Test Locally**: `mvn clean install`
2. **Review Changes**: `git diff`
3. **Commit**: `git commit -m "Migrate package name"`
4. **Update OSSRH**: Request new groupId in Sonatype JIRA
5. **Update CI/CD**: GitHub secrets will use new groupId
6. **Update Docs**: Update README with new coordinates

---

**Need help?** Check the backup directory for the rollback script if anything goes wrong!

