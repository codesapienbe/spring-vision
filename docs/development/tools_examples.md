# MCP Tools — Example Usage

This document provides example JSON-RPC messages and Docker `run` commands to exercise the MCP tools exposed by the `spring-vision` MCP server.

Notes:
- Replace `codesapienbe/spring-vision:latest` with your image name if different.
- All examples use `echo 'JSON' | docker run -i --rm codesapienbe/spring-vision:latest` to send a single JSON-RPC request over stdin.

1) Count faces (`countFaces`)

Request:

```json
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"countFaces","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}
```

Run:

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"countFaces","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' | docker run -i --rm codesapienbe/spring-vision:latest
```

2) Extract embeddings (`extractEmbeddings`)

Request:

```json
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"extractEmbeddings","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}
```

Run:

```bash
echo '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"extractEmbeddings","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' | docker run -i --rm codesapienbe/spring-vision:latest
```

3) Store a face (URL or base64) (`storeFaceEmbedding`)

Request (URL only):

```json
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"storeFaceEmbedding","arguments":{"personId":"alice","imageUrl":"https://example.com/alice.jpg","modelName":"sface-v1","confidence":0.92}}}
```

Run:

```bash
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"storeFaceEmbedding","arguments":{"personId":"alice","imageUrl":"https://example.com/alice.jpg","modelName":"sface-v1","confidence":0.92}}}' | docker run -i --rm codesapienbe/spring-vision:latest
```

The response will include `id` (vector store id) and may include `galleryId` when the backend gallery accepted the embedding.

4) Lookup faces by image URL (`lookupFacesByImageUrl`)

Request:

```json
{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"lookupFacesByImageUrl","arguments":{"imageUrl":"https://example.com/query.jpg","modelName":"sface-v1","metric":"cosine","limit":5}}}
```

Run:

```bash
echo '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"lookupFacesByImageUrl","arguments":{"imageUrl":"https://example.com/query.jpg","modelName":"sface-v1","metric":"cosine","limit":5}}}' | docker run -i --rm codesapienbe/spring-vision:latest
```

5) Add a gallery entry from URL (MCP tool) (`addGalleryEntryFromUrl`)

Request:

```json
{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"addGalleryEntryFromUrl","arguments":{"personId":"bob","imageUrl":"https://example.com/bob.jpg","modelName":"sface-v1"}}}
```

Run:

```bash
echo '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"addGalleryEntryFromUrl","arguments":{"personId":"bob","imageUrl":"https://example.com/bob.jpg","modelName":"sface-v1"}}}' | docker run -i --rm codesapienbe/spring-vision:latest
```

6) Remove a gallery entry (MCP tool) (`removeGalleryEntry`)

Request (replace `<galleryId>` with the id returned when adding/storing):

```json
{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"removeGalleryEntry","arguments":{"galleryId":"<galleryId>"}}}
```

Run:

```bash
echo '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"removeGalleryEntry","arguments":{"galleryId":"<galleryId>"}}}' | docker run -i --rm codesapienbe/spring-vision:latest
```

Tips and troubleshooting

- If you don't see `galleryId` in `storeFaceEmbedding` response, the backend may not support the gallery API in the currently wired backend.
- Use `addGalleryEntryFromUrl` to explicitly add runtime-only gallery vectors when testing ANN behavior.
- For production/high-scale workloads, use a dedicated Vector DB and remove the in-memory gallery.


