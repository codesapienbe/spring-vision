# Use cases: checking a Claude Desktop install

Short scenarios to confirm that the spring-vision extension works after installing
`spring-vision-latest-macos.mcpb` (Mac with Apple Silicon) or `spring-vision-latest-windows.mcpb`
(64-bit Windows) in Claude Desktop. Each one is a prompt to type into Claude Desktop.

## Before you start

- **Give Claude the file's path; don't attach the file.** The tools read the file from this computer. A file
  attached to the chat lands in Claude's own sandbox (`/mnt/user-data/uploads/…`), which spring-vision
  cannot see; it answers with "File not found … is in Claude's sandbox". Save the file locally and pass
  that path.
- **A full path or one starting with `~` both work**, spaces included: `~/Downloads/people.jpg`,
  `/Users/<you>/Desktop/Screenshot 2026-11-20 at 00.25.11.png`, or `C:\Users\<you>\Pictures\people.jpg`
  on Windows.
- **Keep the image under 10 MB.**
- **If macOS asks** whether Claude may access the folder (Downloads, Desktop, …), allow it: the server
  runs under Claude Desktop.

## 1. Count faces (works right away)

Face detection uses OpenCV's built-in Haar cascades, so it needs no download.

> Use spring-vision to count the faces in ~/Downloads/people.jpg, and tell me where each face is.

**Expect** a count and a box per face. The detector finds clear, front-facing faces best; in a large
group photo it can miss small, turned or partly hidden faces, so the count is close to the real
number rather than exact.

## 2. Setup progress

> Is spring-vision finished setting up? Show me which models are ready.

**Expect** a call to `setup_status`. Right after the install the small models (OCR languages, palm,
age and gender) are ready within seconds and the large ones (NSFW, deepfake, action recognition, face
recognition) are still downloading. All of them, about 700 MB, go to `~/.springvision/models`, once.

## 3. Read text (a downloaded model)

Take a screenshot of some text. On a Mac it lands on the Desktop as `Screenshot <date> at <time>.png`;
put its exact name in the prompt (Claude cannot list your folders).

> Use spring-vision to read the text in ~/Desktop/Screenshot 2026-11-20 at 00.25.11.png

**Expect** the text and a confidence. The OCR language files are the smallest downloads, so this
works within seconds of the first start.

## 4. Read a PDF or an Office document

> Lees met spring-vision ~/Downloads/certificate.pdf, per pagina.

Claude calls `read_document` with that path (the file name may contain spaces).

**Expect** the text per page, each page marked `pdf-text` (the PDF's own text, exact and instant) or
`ocr` (a scanned page, read with OCR in well under a second per page). Word, Excel and PowerPoint
files work the same way (`document-text`). A password-protected PDF, or one over 50 pages, is refused
with a message saying so.

## When a tool says "still setting up"

On the first run a tool whose model has not arrived yet answers that spring-vision is still setting
up and that nothing was run. That is the intended answer, not a failure: Claude checks
`setup_status` and retries once the model is in place.
