# COCO8 Ground Truth

Reference truth for the 8 sample images bundled under `datasets/coco8/`. Use
these counts to evaluate `detect_objects` / `classify_image` output. Bounding
boxes are in YOLO format (cx, cy, w, h, all normalized to [0,1]) inside the
`labels/<split>/<basename>.txt` file for each image.

Source: [Ultralytics COCO8](https://docs.ultralytics.com/datasets/detect/coco8/)
— first 8 images from COCO `train2017` (AGPL-3.0 — see `coco8/LICENSE`).

| Image                                | Expected objects                          |
|--------------------------------------|-------------------------------------------|
| `images/train/000000000009.jpg`      | bowl×3, orange×4, broccoli×1              |
| `images/train/000000000025.jpg`      | giraffe×2                                 |
| `images/train/000000000030.jpg`      | potted plant×1, vase×1                    |
| `images/train/000000000034.jpg`      | zebra×1                                   |
| `images/val/000000000036.jpg`        | person×1, umbrella×1                      |
| `images/val/000000000042.jpg`        | dog×1                                     |
| `images/val/000000000049.jpg`        | person×6, horse×2, potted plant×1         |
| `images/val/000000000061.jpg`        | person×3, elephant×2                      |

## Usage

The MCP `_b` (bytes) tool variants take a local path. From a test session:

```
detect_objects datasets/coco8/images/val/000000000049.jpg
# expect: 6 persons + 2 horses + 1 potted plant (or close to it,
# depending on the active backend + confidence threshold)
```

For a URL-based variant, serve the file via any local HTTP server first.

## What the current backend actually finds

The active backend is SSD MobileNet from the DJL model zoo at confidence
threshold 0.5. It is significantly weaker than the ground truth:

| Image                                | Ground truth                            | SSD MobileNet (observed)        |
|--------------------------------------|-----------------------------------------|---------------------------------|
| `train/000000000025.jpg`             | giraffe×2                               | giraffe×1 @ 0.999               |
| `val/000000000036.jpg`               | person×1, umbrella×1                    | (not measured)                  |
| `val/000000000042.jpg`               | dog×1                                   | nothing                         |
| `val/000000000049.jpg`               | person×6, horse×2, potted plant×1       | horse×1 @ 0.86                  |
| `val/000000000061.jpg`               | person×3, elephant×2                    | nothing                         |

Treat the dataset as ground-truth oracle: misses are model/threshold issues,
not dataset bugs. A future swap to a proper ONNX YOLOv8 backend should close
most of this gap.

## Re-downloading

The dataset is checked into the repo (~508 KB). If it ever goes missing,
run `make coco8` to re-fetch from the upstream release.
