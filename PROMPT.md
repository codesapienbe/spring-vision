# Spring Vision MCP — Sample Prompts

Sample prompts to exercise every tool exposed by the spring-vision MCP server, plus advice on what kind of media to use for each. You source the media yourself — the notes describe what makes a good test image.

Every capability has two variants:
- `*_b` — raw image bytes (drop a local file path; the client uploads bytes)
- `*_u` — image URL (any publicly reachable `https://` URL)

When prompting, just give a URL or file path. The model will pick the right tool.

---

## Faces & people

### `count_faces`
**Prompt:** "How many faces are in this image? `<url-or-path>`"

**Good test media:**
- Single front-facing portrait (sanity check: should return 1)
- Group photo (5–20 faces, mixed angles)
- Crowd photo (50+ faces) to see the upper limit
- Photo with **no** people (should return 0)
- Side-profile / partial-face shot to test detector robustness

### `extract_face_embeddings`
**Prompt:** "Extract face embeddings from `<url>` and tell me the vector dimensions."

**Good test media:**
- Two clear portraits of the **same** person from different angles — embeddings should be similar
- Two portraits of **different** people — embeddings should diverge
- A photo with multiple faces — should return one embedding per face

### `detect_emotions`
**Prompt:** "What emotions are visible in `<url>`?"

**Good test media:**
- Stock photos labeled "happy/sad/angry/surprised" face — easy ground truth
- Movie still with strong emotional expression
- Neutral-expression portrait as a control

### `detect_demographics`
**Prompt:** "Estimate age and gender for the people in `<path>`."

**Good test media:**
- Mixed-age group photo (child, adult, elderly)
- Single portraits across age groups
- Note: these models are statistically biased — don't use for anything consequential

### `detect_deepfake`
**Prompt:** "Is this image a deepfake? `<url>`"

**Good test media:**
- Real photo of a public figure (control — should classify as real)
- Known deepfake samples from FaceForensics++ or the DFDC dataset
- AI-generated portrait from thispersondoesnotexist.com (interesting edge case — synthetic but not a face-swap)

### `authenticate_access`
**Prompt:** "Run access authentication on `<path>`."

**Good test media:**
- Clear front-facing portrait, eyes open, neutral light
- Same person in poor lighting / partial occlusion (test rejection)
- Photo with no face (should deny)

---

## Detection & recognition

### `detect_objects`
**Prompt:** "What objects are in `<url>`? Give me bounding boxes."

**Good test media:**
- Street scene (cars, pedestrians, bicycles, traffic lights) — COCO classes
- Kitchen photo (bowl, cup, fork, microwave)
- Living room (sofa, tv, laptop, person, cat/dog)
- Sports photo (ball, racket, person)
- Indoor photo with no recognizable COCO objects (should return empty/low confidence)

### `detect_hands`
**Prompt:** "Find hands in `<path>` and return their positions."

**Good test media:**
- Sign-language photo
- Hands holding objects (close-up)
- Two-person handshake
- Photo with no hands visible (control)

### `detect_poses`
**Prompt:** "Detect human poses in `<url>` and list the joint positions."

**Good test media:**
- Yoga pose, single person, plain background
- Athletic action (running, jumping, kicking)
- Dance still
- Multiple people in frame
- Partially occluded body (test joint inference)

### `recognize_actions`
**Prompt:** "What action is happening in `<url>`?"

**Good test media:**
- Sports action stills (basketball dunk, soccer kick, swimming)
- Cooking / typing / writing — everyday actions
- Static portrait (control — should be low confidence or "standing")

### `detect_threats`
**Prompt:** "Scan `<path>` for security threats."

**Good test media:**
- Stock photos of weapons (knife, firearm) — clearly visible
- Airport-style luggage X-ray images (if you can find labeled samples)
- Crowd photo with no threats (control)
- ⚠️ Use only legally-sourced/stock imagery

### `classify_image`
**Prompt:** "Classify `<url>` and give me the top 5 predictions with confidence."

**Good test media:**
- Single dominant subject (one animal, one object) — ImageNet class
- Iconic ImageNet examples: golden retriever, espresso cup, sports car, banana
- Ambiguous/empty scene (sky, wall) to see low-confidence behavior

---

## Text & metadata

### `extract_text` (OCR)
**Prompt:** "Read all text in `<url>`."

**Good test media:**
- Screenshot of a webpage or document
- Photo of a printed page (book, receipt, business card)
- Street sign / shop signage
- Handwritten note (test handwriting limits)
- Multi-language sample (English + non-Latin script)
- Low-contrast / rotated text to test robustness

### `scan_barcode`
**Prompt:** "Scan the barcode in `<path>` and decode it."

**Good test media:**
- QR code (generate one at qr-code-generator.com pointing to any URL)
- Product barcode (EAN-13/UPC) from a packaged item
- Boarding pass PDF417
- Multiple barcodes in one image
- Blurry / partially occluded barcode (test ZXing's tolerance)

### `extract_image_metadata`
**Prompt:** "Extract EXIF and GPS metadata from `<path>`."

**Good test media:**
- Photo taken with a smartphone with location services enabled (rich EXIF + GPS)
- DSLR shot (camera make/model, lens, exposure data)
- Screenshot or downloaded JPEG from a CDN (typically stripped — should return minimal metadata)
- PNG file (limited EXIF support — useful negative test)

---

## Health & safety

### `detect_fall`
**Prompt:** "Assess fall risk in `<url>`."

**Good test media:**
- Standing person, upright posture (control — low risk)
- Person lying on the floor (high-risk classification)
- Person sitting on the ground (ambiguous case)
- Sports action photo where body is horizontal mid-air (false-positive test)

### `analyze_stress`
**Prompt:** "Analyze stress level in `<path>`."

**Good test media:**
- Portrait with relaxed, smiling expression (low)
- Portrait with furrowed brow, tense expression (high)
- Stock "stressed at desk" photo
- Note: this is heuristic — treat output as a vibes-check, not a clinical signal

### `detect_nsfw`
**Prompt:** "Is `<url>` safe for work?"

**Good test media:**
- Family-friendly stock photo (control — should be `normal`)
- Beach/swimsuit photo (edge case for the model's threshold)
- ⚠️ Don't source actual NSFW for testing in shared environments — trust the `normal` classification on benign inputs as your primary signal

### `estimate_heart_rate_f`
**Prompt:** "Estimate heart rate from this sequence of frames: `[<path1>, <path2>, ..., <pathN>]`"

**Good test media:**
- 30–300 consecutive frames extracted from a short video (1–10 seconds at 30fps) of a person's face holding still
- Use `ffmpeg -i input.mp4 -vf fps=30 frame_%04d.png` to extract frames
- Subject should be well-lit, face visible, minimal motion
- Test with a recording where you know the reference HR (smartwatch reading)
- ⚠️ rPPG is sensitive — expect noisy results without a proper recording setup

---

## Quick sources for test media

- **Generic photos / stock:** Unsplash, Pexels, Pixabay
- **Faces (synthetic, ethics-safe):** thispersondoesnotexist.com
- **Object detection benchmarks:** COCO dataset sample images (cocodataset.org)
- **Pose / action:** MPII Human Pose, AVA, Kinetics sample frames
- **OCR:** Take a phone photo of any printed page; or use ICDAR sample images
- **Barcodes:** Generate at qr-code-generator.com / barcode.tec-it.com
- **EXIF-rich photos:** Anything straight off your phone (avoid CDN downloads — they strip metadata)
- **Deepfakes:** FaceForensics++ samples (research access required)

---

## Caveat — current platform state

The local jar runs on macOS arm64, but the bundled PyTorch native library is `linux-x86_64`. Tools that rely on PyTorch models (object detection, segmentation, pose, RetinaFace) currently fall back to synthetic results or generic detectors. The following are not affected and work fully:

- OCR (`extract_text`)
- Barcode (`scan_barcode`)
- EXIF/GPS metadata (`extract_image_metadata`)
- ONNX-based YOLO detectors where bundled models are used

If a tool consistently returns suspiciously generic output, suspect the PyTorch native fallback rather than the input image.
