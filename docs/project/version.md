# Version Management

This project uses a centralized version management system with a single source of truth: the `VERSION` file.

## Current Version

The current version is stored in the `VERSION` file at the root of the project: **1.0.2**

## How It Works

### 1. VERSION File

- Location: `/VERSION` (root of project)
- Contains: A single line with the version number (e.g., `1.0.2`)
- This is the **single source of truth** for the project version

### 2. Maven POMs

- **Parent POM** (`pom.xml`): Contains the version that all submodules inherit from
- **Submodules**: All submodules inherit their version from the parent via `<parent><version>1.0.2</version></parent>`
- All submodule POMs automatically get the same version as the parent

### 3. Makefile

- Loads version from `VERSION` file: `SPRING_VISION_VERSION := $(shell cat VERSION)`
- Uses `$SPRING_VISION_VERSION` in all targets
- The `build` target automatically updates Maven POMs using `mvn versions:set`
- The `release` target uses `$SPRING_VISION_VERSION` for Maven Central deployment

### 4. Docker Images

- The MCP module's `pom.xml` defines: `<docker.image.name>spring-vision:${project.version}</docker.image.name>`
- Docker images are automatically tagged with the current project version
- Image name format: `spring-vision:1.0.2` (using the version from parent POM)

## Updating the Version

### Option 1: Using the Helper Script (Recommended)

```bash
./set-version.sh 1.0.3
```

This script will:

1. Update the `VERSION` file
2. Update all Maven POMs (parent and submodules) using `mvn versions:set`

### Option 2: Manual Update

1. Edit the `VERSION` file and change the version number
2. Run the build command which will sync Maven POMs:
   ```bash
   make build
   ```

## Version Usage in Project

| Component        | How Version is Applied                                            |
|------------------|-------------------------------------------------------------------|
| `VERSION` file   | Single source of truth - manually edited or via script            |
| Parent `pom.xml` | Updated via `mvn versions:set` (automated in Makefile)            |
| Submodule POMs   | Inherit from parent `<version>` automatically                     |
| Docker images    | Use `${project.version}` from Maven (e.g., `spring-vision:1.0.2`) |
| Makefile targets | Load from `VERSION` file into `$SPRING_VISION_VERSION` variable   |
| Maven release    | Uses `$SPRING_VISION_VERSION` from `VERSION` file                 |

## Build Process

When you run `make build`:

1. Makefile reads `VERSION` file → `SPRING_VISION_VERSION=1.0.2`
2. Runs `mvn versions:set -DnewVersion=1.0.2` to sync all POMs
3. Runs `mvn clean install` which:
    - Builds all modules with version 1.0.2
    - Jib plugin builds Docker image as `spring-vision:1.0.2`

## Release Process

When you run `make release`:

1. Makefile reads `VERSION` file → `SPRING_VISION_VERSION=1.0.2`
2. Updates all POMs to use this version
3. Deploys `core`, `starter`, and `mcp` modules to Maven Central with version 1.0.2

## Examples

### Check Current Version

```bash
cat VERSION
# Output: 1.0.2
```

### Update to New Version

```bash
./set-version.sh 1.0.3
```

### Build with Current Version

```bash
make build
# Builds all modules with version from VERSION file
```

### Run Application

```bash
make run
# Runs: jbang run.java
```

## Benefits

✅ **Single Source of Truth**: Only edit the `VERSION` file  
✅ **Automatic Synchronization**: Makefile ensures Maven POMs stay in sync  
✅ **Consistent Versioning**: All modules, Docker images, and releases use the same version  
✅ **Simple Updates**: Change one file or run one script to update everywhere  
✅ **No Manual Editing**: Maven submodules inherit from parent automatically

## Important Notes

- Never manually edit `<version>` tags in POMs - let the build system handle it
- Always use `make build` or `./set-version.sh` to ensure version consistency
- The VERSION file should contain only the version number, nothing else
- Docker images are automatically tagged with the Maven project version

