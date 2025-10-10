#!/bin/bash
# Safe Package Migration Script
# Migrates from com.springvision to io.github.codesapienbe
#
# This script:
# 1. Creates a full backup
# 2. Updates all package declarations in Java files
# 3. Updates all imports
# 4. Updates pom.xml files
# 5. Updates configuration files
# 6. Moves directory structure to match new package
# 7. Verifies the changes
#
# To rollback: ./scripts/rollback-package-migration.sh

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
OLD_PACKAGE="com.springvision"
NEW_PACKAGE="io.github.codesapienbe"
OLD_PATH="com/springvision"
NEW_PATH="io/github/codesapienbe"
BACKUP_DIR="./backup-$(date +%Y%m%d-%H%M%S)"
DRY_RUN=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help)
            echo "Usage: $0 [--dry-run] [--help]"
            echo ""
            echo "Options:"
            echo "  --dry-run    Show what would be changed without making changes"
            echo "  --help       Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         Package Migration: Safe Refactoring Script             ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${CYAN}Old Package:${NC} $OLD_PACKAGE"
echo -e "${CYAN}New Package:${NC} $NEW_PACKAGE"
echo -e "${CYAN}Old Path:${NC}    $OLD_PATH"
echo -e "${CYAN}New Path:${NC}    $NEW_PATH"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN MODE - No changes will be made${NC}"
    echo ""
fi

# Function to log actions
log_action() {
    echo -e "${GREEN}✓${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    log_error "Not in project root directory. Please run from spring-vision root."
    exit 1
fi

# Step 1: Create backup
echo -e "${BLUE}[1/8] Creating Backup...${NC}"
if [ "$DRY_RUN" = false ]; then
    mkdir -p "$BACKUP_DIR"

    # Backup source files
    log_info "Backing up source directories..."
    for module in spring-vision-*/; do
        if [ -d "$module/src" ]; then
            cp -r "$module/src" "$BACKUP_DIR/${module%/}-src-backup"
        fi
    done

    # Backup pom files
    log_info "Backing up POM files..."
    find . -name "pom.xml" -exec cp --parents {} "$BACKUP_DIR/" \;

    # Backup other configuration files
    log_info "Backing up configuration files..."
    find . -name "*.properties" -o -name "*.yml" -o -name "*.yaml" | while read file; do
        cp --parents "$file" "$BACKUP_DIR/" 2>/dev/null || true
    done

    # Backup documentation
    cp -r .github "$BACKUP_DIR/" 2>/dev/null || true
    cp *.md "$BACKUP_DIR/" 2>/dev/null || true
    cp Makefile "$BACKUP_DIR/" 2>/dev/null || true

    log_action "Backup created: $BACKUP_DIR"

    # Create rollback script
    cat > "$BACKUP_DIR/ROLLBACK.sh" << 'EOF'
#!/bin/bash
echo "Rolling back package migration..."
BACKUP_DIR=$(dirname "$0")
PROJECT_ROOT=$(cd "$BACKUP_DIR/.." && pwd)

# Restore source files
for backup in "$BACKUP_DIR"/*-src-backup; do
    if [ -d "$backup" ]; then
        module=$(basename "$backup" | sed 's/-src-backup$//')
        echo "Restoring $module/src..."
        rm -rf "$PROJECT_ROOT/$module/src"
        cp -r "$backup" "$PROJECT_ROOT/$module/src"
    fi
done

# Restore pom files
find "$BACKUP_DIR" -name "pom.xml" | while read pom; do
    relative_path=${pom#$BACKUP_DIR/}
    echo "Restoring $relative_path..."
    cp "$pom" "$PROJECT_ROOT/$relative_path"
done

# Restore other files
if [ -d "$BACKUP_DIR/.github" ]; then
    echo "Restoring .github/..."
    rm -rf "$PROJECT_ROOT/.github"
    cp -r "$BACKUP_DIR/.github" "$PROJECT_ROOT/"
fi

for file in "$BACKUP_DIR"/*.md "$BACKUP_DIR"/Makefile; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "Restoring $filename..."
        cp "$file" "$PROJECT_ROOT/"
    fi
done

echo "✓ Rollback complete!"
echo "Please run: mvn clean install -DskipTests"
EOF
    chmod +x "$BACKUP_DIR/ROLLBACK.sh"
    log_action "Rollback script created: $BACKUP_DIR/ROLLBACK.sh"
else
    log_info "Skipping backup (dry-run mode)"
fi
echo ""

# Step 2: Update package declarations in Java files
echo -e "${BLUE}[2/8] Updating Package Declarations in Java Files...${NC}"
JAVA_FILES=$(find . -type f -name "*.java" -not -path "*/target/*" -not -path "*/.git/*")
JAVA_COUNT=0

for file in $JAVA_FILES; do
    if grep -q "package $OLD_PACKAGE" "$file" 2>/dev/null; then
        if [ "$DRY_RUN" = false ]; then
            sed -i "s/package $OLD_PACKAGE/package $NEW_PACKAGE/g" "$file"
        fi
        log_info "Updated package declaration: $file"
        ((JAVA_COUNT++))
    fi
done
log_action "Updated $JAVA_COUNT Java files with new package declaration"
echo ""

# Step 3: Update imports in Java files
echo -e "${BLUE}[3/8] Updating Import Statements...${NC}"
IMPORT_COUNT=0

for file in $JAVA_FILES; do
    if grep -q "import $OLD_PACKAGE" "$file" 2>/dev/null; then
        if [ "$DRY_RUN" = false ]; then
            sed -i "s/import $OLD_PACKAGE/import $NEW_PACKAGE/g" "$file"
            sed -i "s/import static $OLD_PACKAGE/import static $NEW_PACKAGE/g" "$file"
        fi
        log_info "Updated imports: $file"
        ((IMPORT_COUNT++))
    fi
done
log_action "Updated imports in $IMPORT_COUNT Java files"
echo ""

# Step 4: Update pom.xml files
echo -e "${BLUE}[4/8] Updating POM Files...${NC}"
POM_FILES=$(find . -type f -name "pom.xml" -not -path "*/target/*")
POM_COUNT=0

for pom in $POM_FILES; do
    if grep -q "$OLD_PACKAGE" "$pom" 2>/dev/null; then
        if [ "$DRY_RUN" = false ]; then
            sed -i "s|<groupId>$OLD_PACKAGE</groupId>|<groupId>$NEW_PACKAGE</groupId>|g" "$pom"
            sed -i "s|<artifactId>spring-vision|<artifactId>spring-vision|g" "$pom"
            sed -i "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$pom"
        fi
        log_info "Updated POM: $pom"
        ((POM_COUNT++))
    fi
done
log_action "Updated $POM_COUNT POM files"
echo ""

# Step 5: Update configuration files
echo -e "${BLUE}[5/8] Updating Configuration Files...${NC}"
CONFIG_FILES=$(find . -type f \( -name "*.properties" -o -name "*.yml" -o -name "*.yaml" -o -name "*.xml" \) -not -path "*/target/*" -not -path "*/.git/*" -not -path "*/pom.xml")
CONFIG_COUNT=0

for file in $CONFIG_FILES; do
    if grep -q "$OLD_PACKAGE" "$file" 2>/dev/null; then
        if [ "$DRY_RUN" = false ]; then
            sed -i "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$file"
        fi
        log_info "Updated config: $file"
        ((CONFIG_COUNT++))
    fi
done
log_action "Updated $CONFIG_COUNT configuration files"
echo ""

# Step 6: Update documentation files
echo -e "${BLUE}[6/8] Updating Documentation...${NC}"
DOC_FILES=$(find . -type f -name "*.md" -not -path "*/target/*" -not -path "*/.git/*")
DOC_COUNT=0

for file in $DOC_FILES; do
    if grep -q "$OLD_PACKAGE" "$file" 2>/dev/null; then
        if [ "$DRY_RUN" = false ]; then
            sed -i "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$file"
        fi
        log_info "Updated docs: $file"
        ((DOC_COUNT++))
    fi
done
log_action "Updated $DOC_COUNT documentation files"
echo ""

# Step 7: Move directory structure
echo -e "${BLUE}[7/8] Restructuring Directories...${NC}"
MODULES=$(find . -maxdepth 1 -type d -name "spring-vision-*" -not -path "*/target/*")

for module in $MODULES; do
    SRC_MAIN_JAVA="$module/src/main/java"
    SRC_TEST_JAVA="$module/src/test/java"

    # Handle main source
    if [ -d "$SRC_MAIN_JAVA/$OLD_PATH" ]; then
        log_info "Processing $module (main)..."
        if [ "$DRY_RUN" = false ]; then
            # Create new package structure
            mkdir -p "$SRC_MAIN_JAVA/$NEW_PATH"

            # Move files
            if [ -d "$SRC_MAIN_JAVA/$OLD_PATH" ]; then
                # Copy contents to new location
                cp -r "$SRC_MAIN_JAVA/$OLD_PATH"/* "$SRC_MAIN_JAVA/$NEW_PATH/" 2>/dev/null || true

                # Remove old structure
                rm -rf "$SRC_MAIN_JAVA/com"
            fi

            log_action "Moved main sources: $module"
        else
            log_info "Would move: $SRC_MAIN_JAVA/$OLD_PATH -> $SRC_MAIN_JAVA/$NEW_PATH"
        fi
    fi

    # Handle test source
    if [ -d "$SRC_TEST_JAVA/$OLD_PATH" ]; then
        log_info "Processing $module (test)..."
        if [ "$DRY_RUN" = false ]; then
            # Create new package structure
            mkdir -p "$SRC_TEST_JAVA/$NEW_PATH"

            # Move files
            if [ -d "$SRC_TEST_JAVA/$OLD_PATH" ]; then
                # Copy contents to new location
                cp -r "$SRC_TEST_JAVA/$OLD_PATH"/* "$SRC_TEST_JAVA/$NEW_PATH/" 2>/dev/null || true

                # Remove old structure
                rm -rf "$SRC_TEST_JAVA/com"
            fi

            log_action "Moved test sources: $module"
        else
            log_info "Would move: $SRC_TEST_JAVA/$OLD_PATH -> $SRC_TEST_JAVA/$NEW_PATH"
        fi
    fi
done
echo ""

# Step 8: Verify changes
echo -e "${BLUE}[8/8] Verifying Changes...${NC}"

if [ "$DRY_RUN" = false ]; then
    # Check for any remaining old package references
    log_info "Checking for remaining old package references..."
    REMAINING=$(grep -r "$OLD_PACKAGE" --include="*.java" --include="*.xml" --include="*.properties" --include="*.yml" --exclude-dir=target --exclude-dir=.git . 2>/dev/null | wc -l)

    if [ "$REMAINING" -gt 0 ]; then
        log_warning "Found $REMAINING files still referencing old package:"
        grep -r "$OLD_PACKAGE" --include="*.java" --include="*.xml" --include="*.properties" --include="*.yml" --exclude-dir=target --exclude-dir=.git . | head -10
        echo ""
        log_warning "You may need to manually review these files"
    else
        log_action "No remaining references to old package found"
    fi

    # Verify new package structure exists
    log_info "Verifying new package structure..."
    FOUND_NEW=false
    for module in $MODULES; do
        if [ -d "$module/src/main/java/$NEW_PATH" ] || [ -d "$module/src/test/java/$NEW_PATH" ]; then
            FOUND_NEW=true
            log_action "✓ New package structure exists in $module"
        fi
    done

    if [ "$FOUND_NEW" = false ]; then
        log_error "No new package structure found! Something went wrong."
        exit 1
    fi
else
    log_info "Dry-run complete. No changes were made."
fi
echo ""

# Summary
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                    Migration Complete!                         ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

if [ "$DRY_RUN" = false ]; then
    echo -e "${GREEN}Package migration completed successfully!${NC}"
    echo ""
    echo "Summary:"
    echo "  • Java files updated: $JAVA_COUNT"
    echo "  • Import statements updated: $IMPORT_COUNT"
    echo "  • POM files updated: $POM_COUNT"
    echo "  • Configuration files updated: $CONFIG_COUNT"
    echo "  • Documentation files updated: $DOC_COUNT"
    echo ""
    echo "Next Steps:"
    echo ""
    echo "1. ${YELLOW}Review the changes:${NC}"
    echo "   git status"
    echo "   git diff"
    echo ""
    echo "2. ${YELLOW}Test the build:${NC}"
    echo "   mvn clean install -DskipTests"
    echo ""
    echo "3. ${YELLOW}Run tests:${NC}"
    echo "   mvn test"
    echo ""
    echo "4. ${YELLOW}If everything works:${NC}"
    echo "   git add ."
    echo "   git commit -m \"Migrate package from $OLD_PACKAGE to $NEW_PACKAGE\""
    echo ""
    echo "5. ${YELLOW}If something broke:${NC}"
    echo "   $BACKUP_DIR/ROLLBACK.sh"
    echo ""
    echo "Backup location: $BACKUP_DIR"
else
    echo -e "${YELLOW}This was a dry run. Run without --dry-run to apply changes.${NC}"
fi
echo ""

