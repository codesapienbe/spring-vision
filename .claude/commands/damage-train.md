# damage-train

Fine-tune the vehicle damage model using images in `datasets/vehicle-damage/staging/`. For each image, run the current model first; if it misses or is uncertain, use your own vision to label the damage, then ask for approval before training.

---

## Step 0 — Preflight

Check that the `ultralytics` package is available:

```bash
python3 -c "import ultralytics; print(ultralytics.__version__)" 2>/dev/null || echo "MISSING"
```

If missing, tell the user to run `pip install ultralytics` and stop.

Find all staging images:

```bash
find datasets/vehicle-damage/staging -type f \( -iname "*.png" -o -iname "*.jpg" -o -iname "*.jpeg" \) | sort
```

If none found, tell the user to drop images into `datasets/vehicle-damage/staging/` and stop.

---

## Step 1 — Process each image (one at a time)

For each staging image, work through steps 2–6 fully before moving to the next.

---

## Step 2 — Run the current model

Get the image as base64:

```bash
python3 -c "import base64; print(base64.b64encode(open('IMAGEPATH','rb').read()).decode())"
```

Call the `detect_vehicle_damages_b` MCP tool with that base64 data.

Evaluate the result:
- **Confident detection** (any result without `review_flag: LOW_CONFIDENCE` and confidence > 0.5): note the top detection — class name and bounding box. Skip to Step 4.
- **Low confidence or no detections**: proceed to Step 3.

---

## Step 3 — Claude visual analysis

Read the image file (multimodal). Examine it carefully and determine:

1. **Which damage class** best matches what you see — pick from the taxonomy below. Use only one class (the dominant damage).
2. **Bounding box** — estimate the pixel coordinates of the damaged region as `[x_min, y_min, x_max, y_max]`.

Get the image dimensions:

```bash
python3 -c "from PIL import Image; w,h=Image.open('IMAGEPATH').size; print(w,h)"
```

Convert to YOLO normalised format (all values 0–1):
- `cx = (x_min + x_max) / 2 / width`
- `cy = (y_min + y_max) / 2 / height`
- `w  = (x_max - x_min) / width`
- `h  = (y_max - y_min) / height`

**Damage class taxonomy:**

| Index | Name                    | Index | Name             |
|-------|-------------------------|-------|------------------|
| 0     | Front-windscreen-damage | 11    | quaterpanel-dent |
| 1     | Headlight-damage        | 12    | rear-bumper-dent |
| 2     | Rear-windscreen-Damage  | 13    | roof-dent        |
| 3     | Runningboard-Damage     | 14    | scratch          |
| 4     | Sidemirror-Damage       | 15    | paint-damage     |
| 5     | Taillight-Damage        | 16    | broken-component |
| 6     | bonnet-dent             | 17    | missing-panel    |
| 7     | boot-dent               | 18    | flood-damage     |
| 8     | doorouter-dent          | 19    | burn-damage      |
| 9     | fender-dent             | 20    | flat-tire        |
| 10    | front-bumper-dent       | 21    | cracked-bumper   |

---

## Step 4 — Ask for approval

Use `AskUserQuestion` (load via ToolSearch if needed). Present exactly:

```
Image:    <filename>
Analysis: <"model (confidence: X%)" or "Claude vision">
Class:    <index> — <name>
Box:      cx=<cx>  cy=<cy>  w=<w>  h=<h>

[visual description of what was found, 1–2 sentences]

Add to training set and fine-tune? (yes / no / skip)
  yes   — add label, run training
  no    — discard image, move to staging/rejected/
  skip  — leave in staging for later
```

---

## Step 5 — Execute (if approved)

### 5a — Add to dataset

Copy image:
```bash
cp IMAGEPATH datasets/vehicle-damage/images/train/FILENAME
```

Write YOLO label file (`datasets/vehicle-damage/labels/train/BASENAME.txt`):
```
CLASS_INDEX CX CY W H
```
Use 4 decimal places.

### 5b — Fine-tune

Pick the starting weights:
```bash
[ -f datasets/vehicle-damage/weights/best.pt ] \
  && echo "datasets/vehicle-damage/weights/best.pt" \
  || echo "yolov11n.pt"
```

Run training from the repo root:
```bash
yolo train \
  model=WEIGHTS \
  data=datasets/vehicle-damage/dataset.yaml \
  imgsz=640 \
  epochs=50 \
  patience=15 \
  batch=4 \
  project=datasets/vehicle-damage/runs \
  name=finetune \
  exist_ok=True
```

### 5c — Export to ONNX

```bash
yolo export \
  model=datasets/vehicle-damage/runs/finetune/weights/best.pt \
  format=onnx \
  imgsz=640 \
  simplify=True
```

### 5d — Install new model

```bash
mkdir -p datasets/vehicle-damage/weights
cp datasets/vehicle-damage/runs/finetune/weights/best.pt  datasets/vehicle-damage/weights/best.pt
cp datasets/vehicle-damage/runs/finetune/weights/best.onnx core/models/vehicle-damage/yolov11n-car-damage.onnx
```

### 5e — Archive processed image

```bash
mkdir -p datasets/vehicle-damage/staging/processed
mv IMAGEPATH datasets/vehicle-damage/staging/processed/
```

---

## Step 6 — Rejected images

If the user answered **no**:
```bash
mkdir -p datasets/vehicle-damage/staging/rejected
mv IMAGEPATH datasets/vehicle-damage/staging/rejected/
```

---

## Step 7 — Summary

After all images are processed, report:
- How many were approved and trained
- How many were rejected
- How many were skipped
- Current training image count: `ls datasets/vehicle-damage/images/train/ | wc -l`
- Remind the user to run `make sync` to deploy the updated model to their local MCP client, or `make release` to publish a new version.
