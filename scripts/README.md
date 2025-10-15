# Spring Vision Scripts

This directory contains utility scripts for the Spring Vision project.

## download-models.sh

Downloads all required OpenCV and face detection models to `core/src/main/resources/models/`.

### Usage

```bash
# From project root
./scripts/download-models.sh

# Or from anywhere
cd /path/to/spring-vision
./scripts/download-models.sh
```

### What it downloads

1. **Haar Cascade Classifiers** (XML)
    - `haarcascade_frontalface_default.xml` - Front-facing face detection
    - `haarcascade_eye.xml` - Eye detection
    - `haarcascade_profileface.xml` - Side profile face detection

2. **LBP Cascade Classifiers** (XML)
    - `lbpcascade_frontalface.xml` - LBP-based frontal face detection

3. **DNN Face Detector** (Caffe)
    - `deploy.prototxt` - Network architecture definition

4. **YuNet Face Detector** (ONNX) - **Recommended**
    - `face_detection_yunet_2023mar.onnx` - Modern, accurate face detector

5. **SFace Face Recognition** (ONNX)
    - `face_recognition_sface_2021dec.onnx` - Face recognition/embedding model (~37MB)
    - Optional: INT8 quantized versions (smaller, faster)

### Features

- ✓ Skips already downloaded files
- ✓ Shows progress and file sizes
- ✓ Uses HuggingFace mirror for large files (more reliable)
- ✓ Interactive prompt for optional quantized models
- ✓ Error handling with descriptive messages

### After Download

Models are automatically included in the JAR during Maven build:

```bash
mvn clean package
```

No internet connection needed during build - all models are bundled!

### Maintenance

To update models:

1. Delete specific model files from `core/src/main/resources/models/`
2. Re-run the script to download latest versions

To re-download all models:

```bash
rm -rf core/src/main/resources/models/*
./scripts/download-models.sh
```

### Offline Usage

Once downloaded, you can:

- Build without internet connection
- Commit models to Git (if you want, though they're large)
- Copy `models/` directory to share with team members
- Back up models for air-gapped environments

---

## validate-models.sh

Validates all downloaded models to ensure they're valid and not corrupted.

### Usage

```bash
# From project root
./scripts/validate-models.sh

# Or validate after downloading
./scripts/download-models.sh && ./scripts/validate-models.sh
```

### What it checks

1. **File Existence** - Verifies all required files are present
2. **File Size** - Checks files are not empty (0 bytes) and meet minimum size requirements
3. **Content Type** - Detects HTML downloads (failed downloads that returned error pages)
4. **Format Validation**:
    - XML files: Checks for valid XML headers and OpenCV cascade markers
    - ONNX files: Validates ONNX/protobuf magic bytes
    - Prototxt files: Verifies Caffe network definition format
    - Caffemodel files: Ensures binary format (not ASCII/HTML)

### Features

- ✓ Color-coded output (green=valid, red=invalid, yellow=warning)
- ✓ Detailed validation summary with counts
- ✓ Exit code 0 on success, 1 on failure (CI/CD friendly)
- ✓ Helpful error messages with remediation steps
- ✓ Validates both required and optional models

### Example Output

```
===============================
Spring Vision Model Validator
===============================

Validating models...

--- Haar Cascade Classifiers ---
✓ Haar Cascade - Frontal Face
   Status: VALID
   Size: 912K

✗ Haar Cascade - Eye
   Status: INVALID - HTML content detected (download failed)
   Path: /path/to/haarcascade_eye.xml
   Size: 4.2K

--- YuNet Face Detector (ONNX) ---
✓ YuNet Face Detector
   Status: VALID
   Size: 228K

===============================
Validation Summary
===============================

Total models checked: 7
Valid:   6
Invalid: 1
Missing: 0

❌ Validation FAILED

Action required:
  - Delete invalid files (they contain HTML or corrupted data)
  - Re-run: ./scripts/download-models.sh
```

### When to Use

- **After initial download** - Verify all models downloaded correctly
- **Before building** - Ensure models are valid before packaging into JAR
- **CI/CD pipelines** - Automated validation in build workflows
- **Troubleshooting** - When application fails to load models at runtime
- **After git pull** - Verify team members downloaded models correctly

### Integration with Build

Add to your Maven build (optional):

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <executions>
    <execution>
      <phase>validate</phase>
      <goals>
        <goal>exec</goal>
      </goals>
      <configuration>
        <executable>${project.basedir}/scripts/validate-models.sh</executable>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Common Issues

**HTML content detected**

- Download failed and received an error page instead
- Solution: Delete the file and re-run download script

**File is empty (0 bytes)**

- Download was interrupted
- Solution: Delete the file and re-run download script

**File smaller than expected**

- Partial download or wrong file
- Solution: Delete and re-download

**Missing files**

- Models not downloaded yet
- Solution: Run `./scripts/download-models.sh`
