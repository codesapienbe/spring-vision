# 🎯 Package Migration - Complete Guide

## What You Need to Know

You're absolutely right - you cannot use `com.springvision` for Maven Central because:

1. ❌ You don't own the `springvision.com` domain
2. ❌ Maven Central requires proof of domain ownership for `com.*` groupIds
3. ✅ You CAN use `io.github.codesapienbe` (matches your GitHub username)

## 🚀 Safe Migration Process (3 Steps)

### Step 1: Preview Changes (Safe - No Modifications)

```bash
cd /path/to/spring-vision
./scripts/migrate-package-name.sh --dry-run
```

This will show you:

- ✅ How many Java files will be updated
- ✅ How many imports will change
- ✅ How many POM files will be modified
- ✅ What directory moves will happen
- ❌ **No actual changes are made**

### Step 2: Run the Migration

```bash
./scripts/migrate-package-name.sh
```

The script will:

1. **Create timestamped backup** → `backup-20251010-HHMMSS/`
2. **Update all package declarations** → `com.springvision.*` → `io.github.codesapienbe.*`
3. **Update all imports** → Changes throughout codebase
4. **Update POM files** → Changes groupId in all modules
5. **Update configuration files** → Properties, YAML, XML
6. **Move directory structure** → `com/springvision/` → `io/github/codesapienbe/`
7. **Create rollback script** → Automatic undo if needed

**Estimated time: 5-10 seconds**

### Step 3: Verify and Test

```bash
# Check what changed
git status
git diff | head -100

# Test the build (this is the critical test)
mvn clean install -DskipTests

# If build succeeds, run tests
mvn test
```

## 🛡️ Built-in Safety Features

### 1. Complete Backup

Every file is backed up before changes:

```
backup-20251010-143022/
├── spring-vision-core-src-backup/
├── spring-vision-starter-src-backup/
├── pom.xml
├── .github/
└── ROLLBACK.sh  ← One command to undo everything
```

### 2. Easy Rollback

If anything breaks:

```bash
# Find your backup
ls -la | grep backup-

# Run the rollback script
./backup-TIMESTAMP/ROLLBACK.sh

# Everything is restored to original state
```

### 3. Git Integration

All changes are unstaged, so you can review before committing:

```bash
git diff          # Review all changes
git add .         # Only when you're satisfied
git commit -m "Migrate package"
```

## 📊 What Gets Changed

### Example Transformations

**Java Package Declaration:**

```java
// Before:
package com.springvision.core.detection;

// After:
package io.github.codesapienbe.core.detection;
```

**Java Imports:**

```java
// Before:
import com.springvision.core.FaceDetector;
import com.springvision.util.ImageUtils;

// After:
import io.github.codesapienbe.core.FaceDetector;
import io.github.codesapienbe.util.ImageUtils;
```

**POM Files:**

```xml
<!-- Before: -->
<groupId>com.springvision</groupId>
<artifactId>spring-vision-core</artifactId>

<!-- After: -->
<groupId>io.github.codesapienbe</groupId>
<artifactId>spring-vision-core</artifactId>
```

**Directory Structure:**

```
Before:
spring-vision-core/
└── src/main/java/
    └── com/springvision/core/
        ├── FaceDetector.java
        └── ImageProcessor.java

After:
spring-vision-core/
└── src/main/java/
    └── io/github/codesapienbe/core/
        ├── FaceDetector.java
        └── ImageProcessor.java
```

## ✅ Complete Migration Checklist

```bash
# 1. Dry-run first (safe preview)
./scripts/migrate-package-name.sh --dry-run

# 2. Review what will change
# Read the output carefully

# 3. Run the migration
./scripts/migrate-package-name.sh

# 4. Verify build works
mvn clean install -DskipTests

# 5. Run tests
mvn test

# 6. Review changes in Git
git status
git diff

# 7. If everything works, commit
git add .
git commit -m "Migrate package from com.springvision to io.github.codesapienbe"

# 8. If something broke, rollback
./backup-TIMESTAMP/ROLLBACK.sh
```

## 🔧 Troubleshooting

### Issue: Build fails with "package does not exist"

**Cause:** IDE cache or Maven cache
**Fix:**

```bash
mvn clean
rm -rf ~/.m2/repository/com/springvision
rm -rf ~/.m2/repository/io/github/codesapienbe
mvn clean install -DskipTests
```

### Issue: Tests fail after migration

**Check these locations:**

- `src/test/resources/application-test.properties`
- Test classes with `@ComponentScan` or `@SpringBootTest`
- Mock or test configuration files

### Issue: IntelliJ IDEA shows errors

```bash
# Invalidate caches
File → Invalidate Caches → Invalidate and Restart
```

### Issue: Want to undo everything

```bash
# Find your backup
ls -d backup-*

# Run the rollback
./backup-TIMESTAMP/ROLLBACK.sh

# Verify rollback worked
mvn clean install -DskipTests
```

## 📋 After Migration: Update Sonatype OSSRH

Once migration is complete and tested, you need to claim the new groupId:

### Option 1: GitHub Proof (Easiest)

1. Go to https://issues.sonatype.org/
2. Create a new ticket:
    - **Project:** Community Support - Open Source Project Repository Hosting
    - **Issue Type:** New Project
    - **Summary:** "Request for io.github.codesapienbe"
    - **Group Id:** `io.github.codesapienbe`
    - **Project URL:** `https://github.com/codesapienbe/spring-vision`
    - **SCM URL:** `https://github.com/codesapienbe/spring-vision.git`
3. Create a temporary public repo to prove ownership:
    - Create: `https://github.com/codesapienbe/OSSRH-XXXXX` (replace XXXXX with your ticket number)
4. Wait for approval (usually within 2 business days)

### Option 2: Domain Proof (If you own codesapien.be)

1. Add TXT record to your domain:
   ```
   TXT @ "sonatype=your-ticket-number"
   ```
2. Request `be.codesapien` groupId instead

## 🎯 Summary

**What the script does:**

- ✅ Updates ~100+ Java files
- ✅ Changes all import statements
- ✅ Modifies all POM files
- ✅ Updates configuration files
- ✅ Moves directory structure
- ✅ Creates automatic rollback

**Time required:**

- Script execution: < 10 seconds
- Build verification: 2-5 minutes
- Total: < 10 minutes

**Safety:**

- ✅ Full backup created
- ✅ One-command rollback
- ✅ Git-friendly (easy to review)
- ✅ Dry-run mode available

**Risk level:** ⬇️ Very Low

- Everything is backed up
- Easy to rollback
- Git makes it reversible
- No manual editing needed

---

## 🚀 Ready to Migrate?

Run these commands in order:

```bash
# 1. Preview (safe, no changes)
./scripts/migrate-package-name.sh --dry-run

# 2. Execute migration
./scripts/migrate-package-name.sh

# 3. Test build
mvn clean install -DskipTests

# 4. If it works, commit
git add .
git commit -m "Migrate to io.github.codesapienbe"

# 5. If it breaks, rollback
./backup-*/ROLLBACK.sh
```

**You're ready! The script is safe to run.** 🚀

