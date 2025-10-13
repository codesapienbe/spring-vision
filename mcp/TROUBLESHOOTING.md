# Spring Vision MCP Server - Troubleshooting Guide

## Common Issues and Solutions

### 1. Container Exits Immediately

**Symptoms:**

- Docker container starts but exits right away
- MCP client shows "connection failed" or similar error

**Cause:** Missing `-i` (interactive) flag in Docker run command

**Solution:** Ensure your MCP client configuration includes the `-i` flag:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "codesapienbe/spring-vision:latest"
      ]
    }
  }
}
```

### 2. Face Comparison Returns "No face detected"

**Causes:**

- Image quality too poor
- Face too small or at extreme angle
- Image format not supported

**Solutions:**

- Ensure images are at least 640x480 pixels
- Face should be clearly visible and front-facing
- Supported formats: JPG, PNG, BMP, TIFF
- For better accuracy, use specialized backends (InsightFace, DeepFace)

### 3. Low Similarity Scores for Same Person

**Causes:**

- Different lighting conditions
- Different angles
- Time gap between photos
- Using default OpenCV backend

**Solutions:**

- Adjust threshold parameter (lower from 0.6 to 0.5)
- Use specialized backend for better embeddings:
    - InsightFace: Best for face verification
    - DeepFace: Multiple model options
    - FaceBytes: Good balance of speed/accuracy

### 4. MCP Client Can't Connect

**Checklist:**

- ✅ Is Docker installed and running? Check: `docker --version`
- ✅ Is the image available? Check: `docker images | grep spring-vision`
- ✅ Is the MCP client config correct? (see README.md)
- ✅ Is the `-i` flag present in the args?

**Pull the image if not available:**

```bash
docker pull codesapienbe/spring-vision:latest
```

### 5. Out of Memory Errors

**Symptoms:**

```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:** Increase heap size in Docker:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "JAVA_OPTS=-Xmx2G",
        "--name",
        "spring-vision-mcp",
        "codesapienbe/spring-vision:latest"
      ]
    }
  }
}
```

### 6. Slow Performance

**Tips for optimization:**

1. Use specialized backends (InsightFace is fastest)
2. Reduce image resolution before comparison
3. Ensure Docker has sufficient resources allocated
4. Use Docker volumes for caching if processing many images

### 7. Image Download Fails

**Causes:**

- URL not accessible from Docker container
- Network restrictions
- Invalid URL format

**Solutions:**

- Test URL accessibility: `curl <image-url>`
- Ensure container has network access
- Use publicly accessible URLs
- Check firewall settings

### 8. Docker Build Fails

**If using Jib:**

```bash
# Clean and rebuild
mvn clean install -DskipTests
```

**If building manually:**

```bash
cd mcp
mvn clean package -DskipTests
docker build -t spring-vision:1.0 .
```

### 9. Wrong Image Version

**Check running version:**

```bash
docker images | grep spring-vision
```

**Pull latest version:**

```bash
docker pull codesapienbe/spring-vision:latest
```

**Force remove old images:**

```bash
docker rmi spring-vision:1.0
make build
```

### 10. Container Logs

**View container logs:**

```bash
# List running containers
docker ps

# View logs (replace <container-id> with actual ID)
docker logs <container-id>

# Follow logs in real-time
docker logs -f <container-id>
```

## Performance Benchmarks

### Default OpenCV Backend

- Face detection: ~100ms per image
- Embedding extraction: ~150ms per image
- Face comparison (2 images): ~300ms total

### InsightFace Backend

- Face detection: ~50ms per image
- Embedding extraction: ~80ms per image
- Face comparison (2 images): ~160ms total

### DeepFace Backend

- Face detection: ~200ms per image
- Embedding extraction: ~300ms per image
- Face comparison (2 images): ~600ms total

*Note: Benchmarks on Intel i7-10700K, images at 1920x1080*

## Docker Resource Requirements

### Minimum Requirements

- **Memory**: 512MB
- **CPU**: 1 core
- **Storage**: 500MB for image

### Recommended for Production

- **Memory**: 2GB
- **CPU**: 2 cores
- **Storage**: 1GB

### With Specialized Backends

- **Memory**: 4GB (InsightFace/DeepFace require more)
- **CPU**: 4 cores
- **Storage**: 2GB+

## Quick Fixes Summary

| Issue             | Quick Fix                                |
|-------------------|------------------------------------------|
| Container exits   | Add `-i` flag to docker args             |
| Can't connect     | Check docker is running and image exists |
| No faces detected | Check image quality and format           |
| Low similarity    | Lower threshold or use better backend    |
| Out of memory     | Add `-e JAVA_OPTS=-Xmx2G` to docker args |
| Slow performance  | Use InsightFace backend                  |
| Build fails       | Run `mvn clean install -DskipTests`      |

## Best Practices

1. **Always use Docker** for consistent deployment
2. **Pin image versions** in production (e.g., `:1.0` instead of `:latest`)
3. **Monitor container logs** during development
4. **Set resource limits** in production:
   ```bash
   docker run -i --rm --memory="1g" --cpus="2" --name spring-vision-mcp codesapienbe/spring-vision:latest
   ```
5. **Use specialized backends** for production face comparison
6. **Test with sample images** before production use
7. **Handle errors gracefully** in your application logic

## Getting Help

1. **Check logs**: `docker logs <container-id>`
2. **Verify image**: `docker images | grep spring-vision`
3. **Test Docker**: `docker run -i --rm --name spring-vision-test codesapienbe/spring-vision:latest`
4. **Review documentation**: README.md and [docs/roadmap.md](../docs/roadmap.md)
5. **Check GitHub issues**: https://github.com/codesapienbe/spring-vision

## Docker-Specific Commands

### Build locally

```bash
make build
```

### Push to Docker Hub

```bash
make deploy
```

### Run interactively for testing

```bash
docker run -i --rm --name spring-vision-mcp spring-vision:1.0
# Type MCP JSON-RPC messages to test
```

### Clean up

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune -a
```

---

For more information, see:

- README.md - Setup and usage guide
- docs/roadmap.md - Plans and milestones
- docs/index.md - Full documentation portal
- GitHub: https://github.com/codesapienbe/spring-vision
