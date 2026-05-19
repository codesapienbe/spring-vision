#!/usr/bin/env python3
"""Convert Kaggle COCO car-damage annotations to YOLO bbox format.

Produces:
  datasets/vehicle-damage/images/train/<id>.jpg
  datasets/vehicle-damage/labels/train/<id>.txt
  datasets/vehicle-damage/images/val/<id>.jpg
  datasets/vehicle-damage/labels/val/<id>.txt

Class indices appended to dataset.yaml (22-27):
  22 headlamp
  23 rear_bumper
  24 door
  25 hood
  26 front_bumper
  27 damaged-area
"""
import json
import os
import shutil
from pathlib import Path

DATASET_ROOT = Path(__file__).parent.parent  # datasets/vehicle-damage/
KAGGLE_ROOT = Path(__file__).parent          # datasets/vehicle-damage/kaggle/

# Class index offset — first 22 classes (0-21) are already defined in dataset.yaml
PARTS_CLASS_MAP = {
    1: 22,  # headlamp
    2: 23,  # rear_bumper
    3: 24,  # door
    4: 25,  # hood
    5: 26,  # front_bumper
}
DAMAGE_CLASS_ID = 27  # generic damaged-area


def coco_bbox_to_yolo(bbox, img_w, img_h):
    """Convert COCO [x, y, w, h] (absolute, top-left) to YOLO [cx, cy, w, h] (normalized)."""
    x, y, w, h = bbox
    cx = (x + w / 2) / img_w
    cy = (y + h / 2) / img_h
    nw = w / img_w
    nh = h / img_h
    return cx, cy, nw, nh


def convert_split(anno_file, parts_anno_file, img_src_dir, split):
    out_img_dir = DATASET_ROOT / "images" / split
    out_lbl_dir = DATASET_ROOT / "labels" / split
    out_img_dir.mkdir(parents=True, exist_ok=True)
    out_lbl_dir.mkdir(parents=True, exist_ok=True)

    with open(anno_file) as f:
        damage_data = json.load(f)
    with open(parts_anno_file) as f:
        parts_data = json.load(f)

    # Build image id → filename + size map from damage file (both files share the same images)
    images = {img["id"]: img for img in damage_data["images"]}

    # Collect all annotations per image_id
    labels = {}  # image_id -> list of "class cx cy w h" strings

    for anno in damage_data["annotations"]:
        img = images[anno["image_id"]]
        iw, ih = img["width"], img["height"]
        cx, cy, nw, nh = coco_bbox_to_yolo(anno["bbox"], iw, ih)
        entry = f"{DAMAGE_CLASS_ID} {cx:.6f} {cy:.6f} {nw:.6f} {nh:.6f}"
        labels.setdefault(anno["image_id"], []).append(entry)

    for anno in parts_data["annotations"]:
        img = images[anno["image_id"]]
        iw, ih = img["width"], img["height"]
        yolo_class = PARTS_CLASS_MAP.get(anno["category_id"])
        if yolo_class is None:
            continue
        cx, cy, nw, nh = coco_bbox_to_yolo(anno["bbox"], iw, ih)
        entry = f"{yolo_class} {cx:.6f} {cy:.6f} {nw:.6f} {nh:.6f}"
        labels.setdefault(anno["image_id"], []).append(entry)

    copied, skipped = 0, 0
    for img_id, img_meta in images.items():
        src = img_src_dir / img_meta["file_name"]
        if not src.exists():
            skipped += 1
            continue
        stem = src.stem
        dst_img = out_img_dir / f"kaggle_{stem}.jpg"
        dst_lbl = out_lbl_dir / f"kaggle_{stem}.txt"
        shutil.copy2(src, dst_img)
        lines = labels.get(img_id, [])
        with open(dst_lbl, "w") as f:
            f.write("\n".join(lines) + ("\n" if lines else ""))
        copied += 1

    return copied, skipped


if __name__ == "__main__":
    results = {}

    # Train split
    copied, skipped = convert_split(
        anno_file=KAGGLE_ROOT / "train" / "COCO_train_annos.json",
        parts_anno_file=KAGGLE_ROOT / "train" / "COCO_mul_train_annos.json",
        img_src_dir=KAGGLE_ROOT / "train",
        split="train",
    )
    results["train"] = (copied, skipped)

    # Val split
    copied, skipped = convert_split(
        anno_file=KAGGLE_ROOT / "val" / "COCO_val_annos.json",
        parts_anno_file=KAGGLE_ROOT / "val" / "COCO_mul_val_annos.json",
        img_src_dir=KAGGLE_ROOT / "val",
        split="val",
    )
    results["val"] = (copied, skipped)

    for split, (c, s) in results.items():
        print(f"{split}: {c} images converted, {s} skipped (source not found)")

    print("\nDone. Remember to update dataset.yaml with the new classes 22-27.")
