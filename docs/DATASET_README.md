# Dataset Download Guide

This document explains how to download the DISC21 face recognition dataset using the Maven dataset profile.

## Overview

The DISC21 dataset contains 2.1 million images for face recognition research. The dataset is approximately 350GB and includes:

- **Reference images**: 2.1M images split into 42 zip files (50k images each)
- **Query images**: Development and test query sets
- **Ground truth**: CSV files with matching information
- **Attributions**: Source image metadata

## Quick Start

To download the complete dataset, run:

```bash
mvn generate-resources -Pdataset
```

This will download all dataset files to the `./dataset/` directory.

## Dataset Structure

The downloaded files will be organized as follows:

```
dataset/
├── dev_queries_50k_0.zip              # Development query images
├── dev_queries_groundtruth.csv        # Development ground truth
├── test_queries_50k_0.zip             # Test query images  
├── test_queries_groundtruth.csv       # Test ground truth
├── disc21_testset_yfcc_attributions.csv  # Test set attributions
├── disc21_yfcc_attributions.csv       # Full dataset attributions
├── refs_50k_0.zip                     # Reference images (batch 0)
├── refs_50k_01.zip                    # Reference images (batch 1)
├── refs_50k_02.zip                    # Reference images (batch 2)
├── ...
├── refs_50k_19.zip                    # Reference images (batch 19)
├── train_50k_0.zip                    # Training images (batch 0)
└── train_50k_1.zip                    # Training images (batch 1)
```

## Download Options

### Download All Files
```bash
mvn generate-resources -Pdataset
```

### Download Specific Files
You can modify the `pom.xml` to comment out specific executions if you only need certain files.

### Resume Downloads
If downloads are interrupted, you can safely re-run the command. Maven will skip files that already exist.

## File Details

### Reference Images (refs_50k_*.zip)
- **20 files**: refs_50k_0.zip through refs_50k_19.zip
- **Content**: 50,000 face images per file
- **Total**: 1,000,000 reference images
- **Size**: ~8-10GB per file

### Query Images
- **dev_queries_50k_0.zip**: Development query set (50k images)
- **test_queries_50k_0.zip**: Test query set (50k images)

### Ground Truth Files
- **dev_queries_groundtruth.csv**: Development set matching information
- **test_queries_groundtruth.csv**: Test set matching information

### Attribution Files
- **disc21_testset_yfcc_attributions.csv**: Test set source metadata
- **disc21_yfcc_attributions.csv**: Full dataset source metadata

### Training Images
- **train_50k_0.zip**: Training set batch 0 (50k images)
- **train_50k_1.zip**: Training set batch 1 (50k images)

## Usage Notes

1. **Storage Requirements**: Ensure you have at least 400GB of free disk space
2. **Network**: Downloads may take several hours depending on your connection
3. **Resume**: Downloads can be resumed if interrupted
4. **Verification**: Check file sizes after download to ensure completeness

## License

This dataset is provided under the Image Similarity Dataset License Agreement. Please review the terms in `DOWNLOADS.md` before use.

## Troubleshooting

### Download Failures
If downloads fail, check:
- Available disk space
- Network connectivity
- Firewall settings

### Partial Downloads
If some files fail to download:
1. Delete the incomplete files
2. Re-run the download command
3. Maven will only download missing files

### Memory Issues
For large downloads, consider:
- Running with increased JVM memory: `mvn -Xmx4g generate-resources -Pdataset`
- Downloading files in smaller batches by modifying the profile

## Integration with Spring Vision

The downloaded dataset can be used with the Spring Vision face recognition components:

```java
@Autowired
private FaceRecognitionEngine faceRecognitionEngine;

// Load reference images from dataset directory
FaceDatabaseBuilder builder = new FaceDatabaseBuilder(
    visionBackend, embeddingIndex, qualityAssessor, config);
builder.buildFromDirectory(Paths.get("./dataset/"));
```

## Support

For issues with the dataset download, please:
1. Check the Maven logs for specific error messages
2. Verify network connectivity to the download URLs
3. Ensure sufficient disk space is available 