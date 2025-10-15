# Module Migration Task Template

## Overview

This template provides a step-by-step guide for migrating a standalone module into the `core` module of the Spring Vision project. Use this template when you want to consolidate functionality and reduce the number of separate Maven modules.

## Instructions for Use

1. Copy this entire template
2. Replace `{MODULE_NAME}` with the actual module name (e.g., `mediapipe`, `yolo`, `tesseract`)
3. Provide the template to the AI assistant in a new session
4. The assistant will execute all steps automatically

---

## TASK: Migrate {MODULE_NAME} Module into Core

### Context

The Spring Vision project currently has a standalone `{MODULE_NAME}` module that should be consolidated into the `core` module to simplify the project structure and reduce maintenance overhead.

### Project Structure

- **Root directory**: `/home/codesapienbe/Projects/spring-vision`
- **Source module**: `{MODULE_NAME}/`
- **Target module**: `core/`
- **Parent POM**: `pom.xml`

### Requirements

**IMPORTANT: Do NOT change the existing codebase. This is a direct migration, not a refactoring.**

#### Phase 1: Copy Module Files to Core

1. **Copy Java source files** from `{MODULE_NAME}/src/main/java/` to `core/src/main/java/`
    - Preserve the complete package structure
    - Include all subpackages (especially `config/` packages)
    - Verify all `.java` files are copied

2. **Copy resource files** from `{MODULE_NAME}/src/main/resources/` to `core/src/main/resources/`
    - Include all configuration files (`.properties`, `.yml`, `.xml`)
    - Include all model files and data files
    - Include META-INF directories

3. **Copy test files** from `{MODULE_NAME}/src/test/` to `core/src/test/`
    - Preserve the test package structure
    - Include test resources if they exist

#### Phase 2: Update Core Module Dependencies

1. **Read the module's `pom.xml`**: `{MODULE_NAME}/pom.xml`
    - Identify all dependencies in the `<dependencies>` section
    - Note any special dependencies that are marked as `<optional>true</optional>`

2. **Update `core/pom.xml`**:
    - Add all dependencies from the {MODULE_NAME} module that are NOT already in core
    - Mark module-specific dependencies as `<optional>true</optional>`
    - Add a comment indicating these dependencies came from the migrated module
    - Example format:
      ```xml
      <!-- Dependencies from {MODULE_NAME} module (migrated) -->
      <dependency>
        <groupId>com.example</groupId>
        <artifactId>example-library</artifactId>
        <version>1.0.0</version>
        <optional>true</optional>
      </dependency>
      ```

#### Phase 3: Remove the Standalone Module

1. **Delete the module directory**: `rm -rf {MODULE_NAME}/`

2. **Update the parent `pom.xml`**:
    - Remove `<module>{MODULE_NAME}</module>` from the `<modules>` section
    - Do NOT remove any other modules
    - Preserve all other configuration

#### Phase 4: Verification

1. **Verify file structure**:
    - List all Java files in the migrated package within core
    - Confirm config packages were copied
    - Confirm resource files were copied

2. **Compile the core module**:
   ```bash
   cd /home/codesapienbe/Projects/spring-vision
   mvn clean compile -pl core -DskipTests
   ```
    - Verify BUILD SUCCESS
    - Check for compilation errors

3. **Compile the entire project**:
   ```bash
   mvn clean compile -DskipTests
   ```
    - Verify BUILD SUCCESS for all modules
    - Confirm the {MODULE_NAME} module is no longer in the reactor build

### Expected File Locations After Migration

After successful migration, the following structure should exist:

```
core/src/main/java/io/github/codesapienbe/springvision/{MODULE_NAME}/
├── {MainClass}.java
└── config/
    ├── {Module}AutoConfiguration.java
    └── {Module}Properties.java

core/src/main/resources/
├── application-{MODULE_NAME}.properties.example (if applicable)
├── META-INF/ (if applicable)
└── models/ (if applicable)

core/src/test/java/io/github/codesapienbe/springvision/{MODULE_NAME}/
└── (test files if they exist)
```

### Success Criteria

- ✅ All Java source files migrated to core with correct package structure
- ✅ All resource files migrated to core
- ✅ All test files migrated to core
- ✅ Core module `pom.xml` updated with necessary dependencies
- ✅ Original `{MODULE_NAME}/` directory removed
- ✅ Parent `pom.xml` updated (module removed from reactor)
- ✅ Core module compiles successfully
- ✅ Full project compiles successfully
- ✅ No references to the old module remain

### Rollback Plan (if needed)

If issues occur during migration:

1. The original module files remain in version control (git)
2. Use `git restore` to recover deleted files
3. Revert changes to parent `pom.xml`
4. Revert changes to `core/pom.xml`

### Post-Migration Notes

Document the following after migration:

- Date of migration: _______________
- Migrated by: _______________
- Number of files migrated: _______________
- Any issues encountered: _______________
- Dependencies added to core: _______________

---

## Example: Previous Successful Migration

### Modules Previously Migrated Using This Template:

1. **mediapipe** (October 15, 2025)
    - Files: 3 Java files + config + resources
    - Dependencies added: None (uses reflection for MediaPipe)

2. **yolo** (October 15, 2025)
    - Files: 3 Java files + config + resources
    - Dependencies added: ONNX Runtime, OpenPnP OpenCV

3. **tesseract** (October 15, 2025)
    - Files: 3 Java files + config + resources
    - Dependencies added: tess4j 5.9.0

All three migrations completed successfully with BUILD SUCCESS.

---

## Quick Copy Template for AI Assistant

```
Please migrate the {MODULE_NAME} module into the core module following these steps:

1. Copy all files from {MODULE_NAME}/ to core/:
   - Java source files (src/main/java)
   - Resource files (src/main/resources)
   - Test files (src/test)

2. Update core/pom.xml with dependencies from {MODULE_NAME}/pom.xml
   - Mark module-specific dependencies as optional

3. Remove the {MODULE_NAME}/ directory

4. Update parent pom.xml:
   - Remove <module>{MODULE_NAME}</module> from the modules list

5. Verify:
   - Compile core module: mvn clean compile -pl core -DskipTests
   - Compile full project: mvn clean compile -DskipTests

IMPORTANT: Do NOT change any code. This is a direct migration only.

Project root: /home/codesapienbe/Projects/spring-vision
```

