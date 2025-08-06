# Spring Vision OpenCV Installer

This module ensures that the required OpenCV native libraries are present and working before the main Spring Vision build proceeds.

## How it Works
- **Automatic Check:** On every Maven build, this module runs first.
- **Native Load:** It attempts to load the OpenCV native library using Bytedeco's JavaCV bindings.
- **Logging:** Success or failure is logged in `application.log` (JSON format, project root) for monitoring and troubleshooting.
- **Fail Fast:** If the native library cannot be loaded, the build fails immediately, preventing downstream errors.

## Usage
- **No manual steps required.**
- The check runs automatically as part of the Maven build lifecycle.
- You can also run it directly with:
  ```sh
  mvn exec:java -Dexec.mainClass="com.springvision.opencvinstaller.OpenCVInstaller"
  ```

## Troubleshooting
- If your build fails at the installer step:
  - Check `application.log` for detailed error messages.
  - Ensure your system is compatible with Bytedeco OpenCV binaries (see [Bytedeco platform docs](https://bytedeco.org/)).
  - Make sure you are using a supported OS and architecture.
- If you need to override the native library path, see the main project documentation for advanced configuration.

## Security & Monitoring
- All logs are written in structured JSON for easy parsing by monitoring tools.
- Sensitive data is sanitized from logs.

## Contact
For issues or support, please open a ticket in the [main Spring Vision repository](https://github.com/spring-vision/spring-vision). 
