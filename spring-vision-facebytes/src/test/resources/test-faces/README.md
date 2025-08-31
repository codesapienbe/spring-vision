# Test Resources for FaceBytes

This directory contains test resources for the FaceBytes integration tests.

## Directory Structure

- `test-faces/` - Contains test face images for unit and integration testing
- `haar-cascades/` - OpenCV Haar cascade classifier files for face detection testing

## Test Images

The test images are generated programmatically during test execution to ensure:
1. Consistent test environment
2. No dependency on external image files
3. Reproducible test results
4. No copyright issues with test data

## Usage

Tests create synthetic images with different colors and text labels to simulate:
- Different face characteristics
- Various image formats and sizes
- Edge cases for face detection algorithms

## Notes

- Test images are 200x200 pixels for fast processing
- Colors are used to differentiate between test cases
- Text labels help identify test scenarios in logs
- All test images are cleaned up automatically after test execution 