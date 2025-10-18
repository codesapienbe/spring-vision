# Logging Configuration

## Overview

Spring Vision uses structured logging to help with debugging and monitoring. Logs are written to both console and files for easy troubleshooting.

## Basic Configuration

The application includes sensible default logging configuration:

- **Console**: Logs to standard output with INFO level
- **File**: Logs to `logs/spring-vision.log` with DEBUG level
- **Error File**: Errors logged to `logs/spring-vision-error.log`

## Log Levels

- **ERROR**: Critical errors that need immediate attention
- **WARN**: Warning messages for potential issues
- **INFO**: General information about application operation
- **DEBUG**: Detailed information for troubleshooting

## Configuration

To adjust log levels, add to your `application.yml`:

```yaml
logging:
  level:
    io.github.codesapienbe.springvision: DEBUG  # Enable debug logging for Spring Vision
    root: INFO  # Default log level
```

## Viewing Logs

To view application logs:

```bash
# View live logs
tail -f logs/spring-vision.log

# View only errors
tail -f logs/spring-vision-error.log

# Search for specific errors
grep "ERROR" logs/spring-vision.log
```

## Troubleshooting with Logs

Common patterns to look for:

- **Model loading errors**: Look for "Failed to load model" or "Model not found"
- **GPU issues**: Check for CUDA-related errors
- **Configuration problems**: Look for "Configuration error" messages
- **Network issues**: Check for connection timeout errors

## Getting Help

For additional troubleshooting help, see:

- [Configuration Guide](./config.md) for setup issues
- [Runtime Guide](./runtime.md) for operational problems
- [Models Guide](./models.md) for model-related errors

The logging system provides detailed information to help diagnose any issues you encounter.

