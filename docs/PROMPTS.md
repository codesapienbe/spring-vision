PROMPTS for Spring Vision MCP Tools

This file contains example, copy-ready prompts you can use when calling the Vision MCP tools (as exposed in `mcp/src/main/java/io/github/codesapienbe/springvision/mcp/VisionTool.java`).

Notes

- Most tools accept an image URL (HTTP/HTTPS). Images larger than 10MB will be rejected. The tool uses a 30s request timeout.
- For "FromBytes" tools provide raw bytes (commonly sent as base64 in API clients). When in doubt, send images as base64-encoded byte payloads.

1) countFaces
   Description: Count faces in an image from a URL. Returns the number of faces detected, average confidence, and processing time.
   Sample prompts:

- "Count the faces in this image: https://example.com/group-photo.jpg"
- "How many faces are in https://example.com/photo.jpg? Return only the count and averageConfidence."
- "Count faces and return a short message (e.g., 'Detected N faces'). Image: <IMAGE_URL>"

2) extractEmbeddings
   Description: Extract face embeddings from an image URL. Returns a list of embeddings (serialized/base64) and metadata per face.
   Sample prompts:

- "Extract face embeddings from https://example.com/person.jpg. Return each embedding as base64 and include its vector length."
- "Get embeddings for the faces in this image and return the top embedding only: https://example.com/photo.jpg"
- "Extract embeddings (base64) from this image and include processing time and face count: <IMAGE_URL>"

3) extractText (OCR)
   Description: Extract text from an image using OCR. Returns the full concatenated text, individual detections with confidences and bounding boxes.
   Sample prompts:

- "Perform OCR on https://example.com/invoice.jpg and return the full extracted text and an array of detected lines with confidence and boundingBox."
- "Extract all text from this screenshot and list detections sorted by confidence: <IMAGE_URL>"
- "Read the visible text on https://example.com/menu.png and return only the plain text (no bounding boxes)."

4) classifyImage
   Description: Classify an image into categories. Returns top predictions with confidence scores. Accepts optional `topK` (defaults to 5).
   Sample prompts:

- "Classify https://example.com/object.jpg and return the top 3 labels with confidences." (topK=3)
- "What is this image? https://example.com/animal.jpg — give the top prediction and confidence."
- "Classify the image and include the full list of predictions (topK=7): <IMAGE_URL>"

5) detectObjects
   Description: Detect objects in an image. Returns detected objects with bounding boxes and confidence scores.
   Sample prompts:

- "Detect objects in https://example.com/street.jpg and return each object's label, confidence and boundingBox."
- "Find all vehicles in this photo and return only detections with confidence >= 0.6: <IMAGE_URL>" (post-filter client-side)
- "Detect objects and return a summary: how many persons, cars, and bicycles are present: <IMAGE_URL>"

6) detectPoses
   Description: Detect human poses in an image. Returns detected poses with joint positions and confidence scores.
   Sample prompts:

- "Estimate human poses in https://example.com/people.jpg and return joint coordinates and confidence per pose."
- "Detect poses and return only the top pose with its joint list: <IMAGE_URL>"
- "Run pose estimation on this image and indicate if a person is raising their right hand: <IMAGE_URL>"

7) recognizeActions
   Description: Detect and recognize actions in an image. Returns recognized actions with confidence scores.
   Sample prompts:

- "Recognize actions in https://example.com/sports.jpg and list actions and confidences (e.g., running, jumping)."
- "Analyze this frame and tell me the most likely action(s): <IMAGE_URL>"
- "Check whether the person in this image is 'sitting' or 'standing' and return the top action and confidence: <IMAGE_URL>"

8) verifyFaces
   Description: Verify if two face images (by URL) belong to the same person. Returns similarity metrics, a combined similarity score, and an isMatch boolean (based on configured threshold).
   Sample prompts:

- "Verify if https://example.com/person1.jpg and https://example.com/person2.jpg are the same person. Return similarity and isMatch."
- "Compare these two images and return cosineSimilarity, euclideanDistance and the final match decision: <SOURCE_URL>, <TARGET_URL>"
- "Check match for these URLs and include sourceFacesCount and targetFacesCount in the response: <SOURCE_URL>, <TARGET_URL>"

9) verifyFacesFromBytes
   Description: Verify if two face images (uploaded as raw bytes) belong to the same person. Use base64-encoded bytes when sending via JSON.
   Sample prompts:

- "Verify two uploaded images (base64) are the same person. Return similarity metrics and isMatch."
- "Here are two base64 images: {source: 'data:image/jpeg;base64,...', target: 'data:image/jpeg;base64,...'}. Are they the same person?"
- "Compare the uploaded face files and return a short message and similarity: <sourceImageBytes>, <targetImageBytes>"

10) lookupFaces
    Description: Lookup matching faces in a dataset (provided as a set of image URLs). Returns matches above threshold sorted by similarity.
    Sample prompts:

- "Find matches for https://example.com/query.jpg in this dataset: https://host/a.jpg,https://host/b.jpg,https://host/c.jpg. Return matching image URLs and similarity."
- "Lookup faces for source image and return only the top 5 matches with similarities and facesDetected: <SOURCE_URL> + [dataset URLs]"
- "Search a small dataset and list matches sorted by similarity (highest first): <SOURCE_URL> and dataset URLs"

11) lookupFacesFromBytes
    Description: Lookup matching faces in a dataset where images are provided as raw bytes (file uploads). Returns matches sorted by similarity.
    Sample prompts:

- "Find dataset matches for an uploaded base64 source image against uploaded dataset images (all base64). Return matches with similarity scores."
- "Lookup faces using file uploads: provide source image bytes and a list of dataset image bytes and return matches above threshold."

Tips and examples

- If you send images as base64 in JSON, include just the raw base64 (without the data URI prefix) unless your client requires the prefix.
- For any URL-based prompt include full HTTP/HTTPS links. The MCP tool validates the URL scheme.
- For calls that accept arrays or sets (dataset inputs), provide a JSON array of URLs or base64 images depending on the endpoint.

Example JSON-style call (pseudocode)
{
"tool": "verifyFaces",
"params": {
"sourceImageUrl": "https://example.com/source.jpg",
"targetImageUrl": "https://example.com/target.jpg"
}
}

That’s it — use these prompts as a starting point and adapt them to your application (e.g., narrow by confidence thresholds, request particular fields, or post-filter results client-side).

