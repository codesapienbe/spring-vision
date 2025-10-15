# MCP Server Logging Configuration

## Overview

The Spring Vision MCP Server uses **structured JSON logging** to ensure all logs are machine-parseable and will never interfere with the MCP protocol communication over stdio (standard input/output).

## Key Design Principles

### 1. **Stdio Transport Separation**
- **stdout**: Reserved EXCLUSIVELY for MCP JSON-RPC protocol messages
- **stderr**: All application logs (JSON formatted)
- **Files**: Persistent JSON logs for debugging and monitoring

This separation is **critical** because the MCP protocol uses stdin/stdout for communication. Any non-protocol output to stdout would corrupt the JSON-RPC message stream and cause parsing failures in MCP clients.

### 2. **JSON Format for All Logs**
All logs are in JSON format using the `logstash-logback-encoder` library. This ensures:
- **No parsing ambiguity**: Every log entry is valid JSON
- **Structured data**: Easy to parse, filter, and analyze
- **Consistency**: Same format across all log levels and appenders

### 3. **Structured Arguments**
All log statements use `StructuredArguments` from `net.logstash.logback.argument.StructuredArguments` to provide:
- Machine-readable key-value pairs
- Consistent field names across the application
- Easy integration with log aggregation systems (ELK, Splunk, etc.)

## Logging Configuration Files

### Logback Configuration (`logback-spring.xml`)

The logging configuration defines three appenders:

1. **CONSOLE Appender**
   - Target: `System.err` (stderr)
   - Format: JSON
   - Use: Real-time monitoring during development and debugging
   
2. **FILE Appender**
   - File: `logs/mcp.json.log`
   - Format: JSON
   - Rotation: Daily and size-based (50MB max)
   - Retention: 30 days, max 1GB total
   - Use: Persistent storage of all application logs

3. **ERROR_FILE Appender**
   - File: `logs/mcp-error.json.log`
   - Format: JSON
   - Filter: ERROR level and above
   - Rotation: Daily and size-based (50MB max)
   - Retention: 90 days, max 2GB total
   - Use: Quick access to error logs for troubleshooting

### JSON Log Format

Each log entry contains the following fields:

```json
{
  "timestamp": "2025-10-15T15:20:30.123Z",
  "level": "INFO",
  "logger": "io.github.codesapienbe.springvision.mcp.StartupLogger",
  "thread": "main",
  "message": "Spring Vision MCP Server starting",
  "event": "mcp_server_startup",
  "server_name": "spring-vision",
  "server_version": "1.0.5",
  "transport": "stdio",
  "protocol": "stdio"
}
```

### Structured Logging Examples

#### Example 1: Startup Event
```java
log.info("Spring Vision MCP Server starting", 
    StructuredArguments.keyValue("event", "mcp_server_startup"),
    StructuredArguments.keyValue("server_name", serverName),
    StructuredArguments.keyValue("server_version", serverVersion),
    StructuredArguments.keyValue("transport", transport)
);
```

#### Example 2: Face Detection Success
```java
log.info("countFaces completed successfully",
    StructuredArguments.keyValue("event", "count_faces_success"),
    StructuredArguments.keyValue("face_count", faceCount),
    StructuredArguments.keyValue("avg_confidence", avgConfidence),
    StructuredArguments.keyValue("processing_time_ms", duration)
);
```

#### Example 3: Error Logging
```java
log.error("Failed to download image",
    StructuredArguments.keyValue("event", "image_download_error"),
    StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)),
    StructuredArguments.keyValue("error", e.getMessage())
);
```

## Event Types

The MCP server uses standardized event types for easy log filtering and monitoring:

| Event Type | Description | Level |
|------------|-------------|-------|
| `mcp_server_startup` | Server initialization started | INFO |
| `mcp_server_ready` | Server ready to accept MCP requests | INFO |
| `mcp_capabilities` | Server capabilities loaded | INFO |
| `transport_config` | Transport configuration details | INFO |
| `vision_tool_init` | Vision tool initialization | INFO |
| `count_faces_start` | Face counting operation started | INFO |
| `count_faces_success` | Face counting completed successfully | INFO |
| `count_faces_validation_error` | Input validation failed | WARN |
| `image_download_start` | Image download initiated | DEBUG |
| `image_download_success` | Image downloaded successfully | DEBUG |
| `image_download_error` | Image download failed | ERROR |
| `face_detection_complete` | Face detection operation completed | DEBUG |
| `face_detection_error` | Face detection failed | ERROR |
| `count_faces_unexpected_error` | Unexpected error in face counting | ERROR |
| `tool_registry_found` | Spring AI ToolRegistry found | INFO |
| `tool_registry_inspection` | ToolRegistry inspection results | INFO |
| `tool_registry_inspection_failed` | ToolRegistry inspection failed | WARN |

## Log Levels by Environment

### Default Profile (Development)
- Application logs: INFO
- Spring Framework: WARN
- Spring AI MCP: INFO
- Root: INFO

### Dev Profile
- Application logs: DEBUG
- Spring AI MCP: DEBUG
- Root: DEBUG

### Production Profile
- Application logs: WARN
- Spring AI MCP: WARN
- Console logging: DISABLED (file only)
- Root: WARN

## Viewing Logs

### Real-time Console Logs (stderr)
When running the MCP server, stderr shows JSON logs in real-time:

```bash
docker run -i codesapienbe/spring-vision:latest 2>&1 | jq
```

The `2>&1` redirects stderr to stdout so you can pipe it to `jq` for pretty-printing.

### File Logs
```bash
# View all logs
tail -f logs/mcp.json.log | jq

# View errors only
tail -f logs/mcp-error.json.log | jq

# Filter by event type
jq 'select(.event == "count_faces_success")' logs/mcp.json.log

# Count events
jq 'select(.event == "count_faces_start")' logs/mcp.json.log | wc -l

# Average processing time
jq -s '[.[] | select(.event == "count_faces_success")] | map(.processing_time_ms) | add / length' logs/mcp.json.log
```

## Best Practices

### 1. Always Use Structured Arguments
❌ **Don't do this:**
```java
log.info("Face count: " + faceCount);
```

✅ **Do this:**
```java
log.info("Face counting completed",
    StructuredArguments.keyValue("event", "count_faces_success"),
    StructuredArguments.keyValue("face_count", faceCount)
);
```

### 2. Include Event Type
Always include an `event` field to categorize the log entry:
```java
StructuredArguments.keyValue("event", "descriptive_event_name")
```

### 3. Sanitize Sensitive Data
Remove or mask sensitive information before logging:
```java
private String sanitizeUrlForLogging(String url) {
    if (url == null) return "null";
    int queryIndex = url.indexOf('?');
    return queryIndex > 0 ? url.substring(0, queryIndex) + "?..." : url;
}
```

### 4. Use Appropriate Log Levels
- **DEBUG**: Detailed information for debugging (image sizes, intermediate steps)
- **INFO**: Important events (operation start/success, initialization)
- **WARN**: Recoverable issues (validation errors, missing optional data)
- **ERROR**: Errors that prevent operation completion

### 5. Include Context Information
Provide enough context to understand what happened:
```java
log.error("Face detection failed",
    StructuredArguments.keyValue("event", "face_detection_error"),
    StructuredArguments.keyValue("error", e.getMessage()),
    StructuredArguments.keyValue("error_type", e.getClass().getSimpleName()),
    StructuredArguments.keyValue("image_size_bytes", imageBytes.length)
);
```

## Troubleshooting

### Problem: MCP client reports "Invalid JSON-RPC message"
**Cause**: Something is writing to stdout instead of stderr.

**Solution**: 
1. Check that `logback-spring.xml` has `<target>System.err</target>` for the CONSOLE appender
2. Search for any `System.out.println()` statements in the code
3. Ensure Spring Boot banner is disabled: `spring.main.banner-mode=off`

### Problem: Logs are not in JSON format
**Cause**: Missing or incorrect encoder configuration.

**Solution**: Ensure `logback-spring.xml` uses `LoggingEventCompositeJsonEncoder`:
```xml
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
  <providers>
    <timestamp/>
    <logLevel/>
    <loggerName/>
    <message/>
    <mdc/>
    <stackTrace/>
    <logstashMarkers/>
    <arguments/>
  </providers>
</encoder>
```

### Problem: Log files growing too large
**Cause**: Log rotation not working or too much DEBUG logging in production.

**Solution**:
1. Check `maxFileSize` and `maxHistory` in `logback-spring.xml`
2. Use production profile to reduce log level: `--spring.profiles.active=prod`
3. Review and adjust retention policies

## Dependencies

The JSON logging functionality requires:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>7.4</version>
</dependency>
```

This dependency is already included in the MCP module's `pom.xml`.

## Summary

The Spring Vision MCP Server's logging architecture ensures:

✅ **No stdout pollution**: All logs go to stderr/files, keeping stdout clean for MCP protocol  
✅ **Machine-parseable**: Every log is valid JSON  
✅ **Structured data**: Easy to filter, search, and analyze  
✅ **Production-ready**: Rotation, retention, and performance optimized  
✅ **Developer-friendly**: Readable with `jq`, works with standard log aggregation tools  

This design guarantees that the MCP server will work reliably with any MCP client while providing comprehensive observability for operations and debugging.

