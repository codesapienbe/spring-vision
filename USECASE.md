# Use cases: checking a Claude Desktop install

Short scenarios to confirm that the spring-vision extension works after installing
`spring-vision-latest-macos.mcpb` (Mac with Apple Silicon) or `spring-vision-latest-windows.mcpb`
(64-bit Windows) in Claude Desktop. Each one is a prompt to type into Claude Desktop.

## Before you start

- **Give Claude the file's path; don't attach the image.** The tools read the image from disk, and an
  image pasted into the chat does not reach them.
- **Use a full path without spaces**, for example `/Users/<you>/Downloads/people.jpg`. A path with a
  space is currently rejected as an invalid URL, and `~` is not expanded.
- **Keep the image under 10 MB.**
- **If macOS asks** whether Claude may access the folder (Downloads, Desktop, …), allow it: the server
  runs under Claude Desktop.

## 1. Count faces (works right away)

Face detection uses OpenCV's built-in Haar cascades, so it needs no download.

> Use spring-vision to count the faces in /Users/&lt;you&gt;/Downloads/people.jpg, and tell me where each face is.

**Expect** a count and a box per face. The detector finds clear, front-facing faces best; in a large
group photo it can miss small, turned or partly hidden faces, so the count is close to the real
number rather than exact.

## 2. Setup progress

> Is spring-vision finished setting up? Show me which models are ready.

**Expect** a call to `setup_status`. Right after the install the small models (OCR languages, palm,
age and gender) are ready within seconds and the large ones (NSFW, deepfake, action recognition, face
recognition) are still downloading. All of them, about 700 MB, go to `~/.springvision/models`, once.

## 3. Read text (a downloaded model)

Take a screenshot of some text and save it as `/Users/<you>/Downloads/text.png` (screenshot names
contain spaces, so rename it).

> Use spring-vision to read the text in /Users/&lt;you&gt;/Downloads/text.png.

**Expect** the text and a confidence. The OCR language files are the smallest downloads, so this
works within seconds of the first start.

## When a tool says "still setting up"

On the first run a tool whose model has not arrived yet answers that spring-vision is still setting
up and that nothing was run. That is the intended answer, not a failure: Claude checks
`setup_status` and retries once the model is in place.
