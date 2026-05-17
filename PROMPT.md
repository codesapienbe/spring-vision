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

**Verified URLs:**
- 1 face (female): https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600
- 1 face (male): https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=600
- Group of friends: https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=1000
- Team meeting: https://images.unsplash.com/photo-1431540015161-0bf868a2d407?w=1000
- Crowd (concert): https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=1200
- Crowd (stadium): https://images.unsplash.com/photo-1542652694-40abf526446e?w=1200
- No people (landscape): https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1000
- Side profile: https://images.unsplash.com/photo-1554151228-14d9def656e4?w=600

### `extract_face_embeddings`
**Prompt:** "Extract face embeddings from `<url>` and tell me the vector dimensions."

**Good test media:**
- Two clear portraits of the **same** person from different angles — embeddings should be similar
- Two portraits of **different** people — embeddings should diverge
- A photo with multiple faces — should return one embedding per face

**Verified URLs:**
- Single portrait A: https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600
- Single portrait B (different person): https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=600
- Multiple faces: https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=1000

### `detect_emotions`
**Prompt:** "What emotions are visible in `<url>`?"

**Good test media:**
- Stock photos labeled "happy/sad/angry/surprised" face — easy ground truth
- Movie still with strong emotional expression
- Neutral-expression portrait as a control

**Verified URLs:**
- Happy / smiling: https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=600
- Sad: https://images.unsplash.com/photo-1521119989659-a83eee488004?w=600
- Surprised: https://images.unsplash.com/photo-1542596594-649edbc13630?w=600
- Neutral (control): https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=600

### `detect_demographics`
**Prompt:** "Estimate age and gender for the people in `<path>`."

**Good test media:**
- Mixed-age group photo (child, adult, elderly)
- Single portraits across age groups
- Note: these models are statistically biased — don't use for anything consequential

**Verified URLs:**
- Multi-generation family: https://images.unsplash.com/photo-1611042553365-9b101441c135?w=1000
- Family group portrait: https://images.unsplash.com/photo-1606216794074-735e91aa2c92?w=1000
- Elderly: https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?w=600
- Child: https://images.unsplash.com/photo-1602992708529-c9fdb12905c9?w=600
- Adult female: https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600
- Adult male: https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=600

### `detect_deepfake`
**Prompt:** "Is this image a deepfake? `<url>`"

**Good test media:**
- Real photo of a public figure (control — should classify as real)
- Known deepfake samples from FaceForensics++ or the DFDC dataset
- AI-generated portrait from thispersondoesnotexist.com (interesting edge case — synthetic but not a face-swap)

**Verified URLs:**
- Real photo (control): https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=600
- Real photo (alt): https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600
- Synthetic face (regenerated each load): https://thispersondoesnotexist.com/

### `authenticate_access`
**Prompt:** "Run access authentication on `<path>`."

**Good test media:**
- Clear front-facing portrait, eyes open, neutral light
- Same person in poor lighting / partial occlusion (test rejection)
- Photo with no face (should deny)

**Verified URLs:**
- Clear front-facing portrait: https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600
- Clear front-facing portrait (alt): https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=600
- No face (landscape): https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1000

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

**Verified URLs:**
- Kitchen: https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800
- Living room: https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800
- Office desk (laptop, phone): https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=800
- Sports car: https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=1000
- Soccer (player, ball): https://images.unsplash.com/photo-1543351611-58f69d7c1781?w=800
- Basketball game: https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800
- No COCO objects (landscape): https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1000

### `detect_hands`
**Prompt:** "Find hands in `<path>` and return their positions."

**Good test media:**
- Sign-language photo
- Hands holding objects (close-up)
- Two-person handshake
- Photo with no hands visible (control)

**Verified URLs:**
- Sign-language style hands: https://images.unsplash.com/photo-1518152006812-edab29b069ac?w=800
- Hand close-up: https://images.unsplash.com/photo-1577741314755-048d8525d31e?w=800
- Handshake: https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=800
- No hands (control): https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1000

### `detect_poses`
**Prompt:** "Detect human poses in `<url>` and list the joint positions."

**Good test media:**
- Yoga pose, single person, plain background
- Athletic action (running, jumping, kicking)
- Dance still
- Multiple people in frame
- Partially occluded body (test joint inference)

**Verified URLs:**
- Yoga pose: https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800
- Running: https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800
- Athletic single: https://images.unsplash.com/photo-1547347298-4074fc3086f0?w=800
- Multiple people: https://images.unsplash.com/photo-1517649763962-0c623066013b?w=1000

### `recognize_actions`
**Prompt:** "What action is happening in `<url>`?"

**Good test media:**
- Sports action stills (basketball dunk, soccer kick, swimming)
- Cooking / typing / writing — everyday actions
- Static portrait (control — should be low confidence or "standing")

**Verified URLs:**
- Basketball play: https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800
- Soccer kick: https://images.unsplash.com/photo-1543351611-58f69d7c1781?w=800
- Cooking: https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800
- Typing at desk: https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=800
- Static portrait (control): https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=600

### `detect_threats`
**Prompt:** "Scan `<path>` for security threats."

**Good test media:**
- Stock photos of weapons (knife, firearm) — clearly visible
- Airport-style luggage X-ray images (if you can find labeled samples)
- Crowd photo with no threats (control)
- ⚠️ Use only legally-sourced/stock imagery

**Verified URLs:**
- Kitchen knife (legal stock): https://images.unsplash.com/photo-1593618998160-e34014e67546?w=800
- Crowd, no threats (control): https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=1200
- Landscape, no threats (control): https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1000

### `classify_image`
**Prompt:** "Classify `<url>` and give me the top 5 predictions with confidence."

**Good test media:**
- Single dominant subject (one animal, one object) — ImageNet class
- Iconic ImageNet examples: golden retriever, espresso cup, sports car, banana
- Ambiguous/empty scene (sky, wall) to see low-confidence behavior

**Verified URLs:**
- Golden retriever: https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=800
- Dog (alt): https://images.unsplash.com/photo-1517022812141-23620dba5c23?w=800
- Cat: https://images.unsplash.com/photo-1574158622682-e40e69881006?w=800
- Espresso cup: https://images.unsplash.com/photo-1571167530149-c1105da4c2c7?w=800
- Banana: https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=800
- Sports car: https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=1000
- Ambiguous (landscape): https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1000

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

**Verified URLs:**
- Open book / printed page: https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=800
- Receipt: https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=800
- Neon sign: https://images.unsplash.com/photo-1517842645767-c639042777db?w=800
- Handwriting: https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?w=800

### `scan_barcode`
**Prompt:** "Scan the barcode in `<path>` and decode it."

**Good test media:**
- QR code (generate one at qr-code-generator.com pointing to any URL)
- Product barcode (EAN-13/UPC) from a packaged item
- Boarding pass PDF417
- Multiple barcodes in one image
- Blurry / partially occluded barcode (test ZXing's tolerance)

**Verified URLs:**
- QR code (on phone): https://images.unsplash.com/photo-1529778873920-4da4926a72c2?w=600
- QR code (generated, decodes to "hello"): https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=hello
- QR code (generated, decodes to a URL): https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=https%3A%2F%2Fgithub.com%2Fcodesapienbe%2Fspring-vision

### `extract_image_metadata`
**Prompt:** "Extract EXIF and GPS metadata from `<path>`."

**Good test media:**
- Photo taken with a smartphone with location services enabled (rich EXIF + GPS)
- DSLR shot (camera make/model, lens, exposure data)
- Screenshot or downloaded JPEG from a CDN (typically stripped — should return minimal metadata)
- PNG file (limited EXIF support — useful negative test)

**Verified URLs:**
- Canon DSLR EXIF (full camera tags): https://raw.githubusercontent.com/ianare/exif-samples/master/jpg/Canon_40D.jpg
- EXIF with GPS coordinates: https://raw.githubusercontent.com/ianare/exif-samples/master/jpg/gps/DSCN0010.jpg
- EXIF with orientation tag: https://raw.githubusercontent.com/ianare/exif-samples/master/jpg/orientation/landscape_1.jpg
- CDN-stripped (negative control): https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600

---

## Health & safety

### `detect_fall`
**Prompt:** "Assess fall risk in `<url>`."

**Good test media:**
- Standing person, upright posture (control — low risk)
- Person lying on the floor (high-risk classification)
- Person sitting on the ground (ambiguous case)
- Sports action photo where body is horizontal mid-air (false-positive test)

**Verified URLs:**
- Standing upright (control): https://images.unsplash.com/photo-1521119989659-a83eee488004?w=600
- Athletic horizontal (false-positive test): https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800
- Sitting / typing (ambiguous case): https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=800

### `analyze_stress`
**Prompt:** "Analyze stress level in `<path>`."

**Good test media:**
- Portrait with relaxed, smiling expression (low)
- Portrait with furrowed brow, tense expression (high)
- Stock "stressed at desk" photo
- Note: this is heuristic — treat output as a vibes-check, not a clinical signal

**Verified URLs:**
- Relaxed expression: https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=600
- Stressed at desk: https://images.unsplash.com/photo-1573497019418-b400bb3ab074?w=600
- Smiling (low stress): https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=600

### `detect_nsfw`
**Prompt:** "Is `<url>` safe for work?"

**Good test media:**
- Family-friendly stock photo (control — should be `normal`)
- Beach/swimsuit photo (edge case for the model's threshold)
- ⚠️ Don't source actual NSFW for testing in shared environments — trust the `normal` classification on benign inputs as your primary signal

**Verified URLs:**
- Family-friendly (control): https://images.unsplash.com/photo-1606216794074-735e91aa2c92?w=1000
- Landscape (control): https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1000
- Beach scene, clothed (edge case): https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=800

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
