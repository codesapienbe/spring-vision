# 🚀 Quick Start Guide

[Docs Home](../index.md) · [MCP Setup](./mcp-setup.md) · [MCP Testing](./mcp-testing.md) · [API Usage](../development/API_USAGE.md)

Welcome! This guide helps you go from zero to your first AI detection in minutes. No PhD in AI required! 😉

You have two awesome ways to use Spring Vision. Choose your adventure:

---

## 🦸‍♂️ Option 1: Use as an AI Assistant Tool (MCP Server)
*Recommended if you want Claude, Cursor, or other AI agents to "see" images.*

The easiest way to get started is using Spring Vision as an MCP (Model Context Protocol) server.

### 1. The Magic One-Liner 🪄
Run our setup tool directly in your terminal. It downloads and configures everything automatically!

```bash
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar
```

The tool will grab the latest server (it's about ~1GB because of the AI models) and save it to `~/.springvision/`. 

### 2. Configure Your AI Client
The CLI tool will print out a configuration snippet. Just copy and paste it into your MCP client (like Claude Desktop or Cursor). It looks like this:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": ["/home/youruser/.springvision/mcp-0.0.4.jar"]
    }
  }
}
```

### 3. Test it out!
Restart your AI client and try asking it: *"Count the number of faces in this image: [URL]"*

---

## 🛠️ Option 2: Add to Your Spring Boot App (The Easy Way)
*Recommended if you want to build computer vision directly into your own code!*

If you want to make your own Spring Boot application "see", follow these steps. 

### Prerequisites
- **Java 25+** (We use the latest Java features for top performance!)
- **Spring Boot 3.2+**
- A sense of adventure!

### Step 1: The Magic Dependency 📦
Just like adding a database driver, we add the Spring Vision Starter. 

First, tell Maven where to find it (we use GitHub Packages) in your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/codesapienbe/spring-vision</url>
    </repository>
</repositories>
```

Next, add the dependency:

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.4</version>
</dependency>
```

> **💡 Junior Tip:** The `starter` package automatically brings in everything you need, including the core vision logic and web capabilities!

### Step 2: Tell Spring It Exists ⚙️
Open your `application.yml` and turn on the AI engine:

```yaml
spring:
  vision:
    djl:
      enabled: true      # Turn on the AI engine!
```

### Step 3: Your First Vision Service 🧠
The heart of Spring Vision is the `VisionTemplate`. It works just like `RestTemplate` or `JdbcTemplate`.

Create a new Service class:

```java
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;

@Service
public class MyAwesomeVisionService {

    // 1. Inject the template!
    private final VisionTemplate visionTemplate;

    public MyAwesomeVisionService(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }

    // 2. Write a method to process an image
    public void findFacesInImage(String filePath) throws Exception {
        
        // A. Load your image into our special ImageData wrapper
        byte[] bytes = Files.readAllBytes(new File(filePath).toPath());
        ImageData image = ImageData.fromBytes(bytes);

        System.out.println("🤖 Processing image...");

        // B. Ask the VisionTemplate to detect faces
        VisionResult result = visionTemplate.detectFaces(image);

        // C. Print out the results!
        if (result.hasDetections()) {
            System.out.println("🎉 Found " + result.detectionCount() + " faces!");
            result.detections().forEach(face -> {
                System.out.println("- Face confidence: " + (face.confidence() * 100) + "%");
            });
        }
    }
}
```

### 🦸‍♂️ What Else Can `VisionTemplate` Do?
Just type `visionTemplate.` in your IDE to see the magic:
- `.detectObjects(image)` -> Finds dogs, cars, cups, etc.
- `.extractText(image)` -> Reads text from images (OCR).
- `.detectEmotions(image)` -> Is the person happy or sad?
- `.scanBarcodes(image)` -> Reads QR codes and barcodes.

---

## 🌟 Specialized Detections (For your first project!)
Since you are starting a project that needs deep analysis, here is how to use our most advanced features:

### 1. National ID & Driver Licenses 🪪
Extract structured data from Belgian or Dutch ID cards and licenses.

```java
// Pass "BE" or "NL" as a hint, or null to auto-detect
VisionResult idResult = visionTemplate.recognizeIdentityCard(image, "BE");

idResult.detections().forEach(doc -> {
    Map<String, Object> attrs = doc.attributes();
    System.out.println("Document: " + attrs.get("documentType"));
    System.out.println("Name: " + attrs.get("surname") + " " + attrs.get("givenNames"));
    System.out.println("Valid: " + attrs.get("isValid"));
});
```

### 2. Vehicle Detection 🚗
Detects cars, trucks, motorcycles, and more with specific categories.

```java
VisionResult vehicles = visionTemplate.detectVehicles(image);

vehicles.detections().forEach(v -> {
    String type = (String) v.attributes().get("vehicleType"); // e.g., "truck"
    String category = (String) v.attributes().get("vehicleCategory"); // e.g., "commercial_vehicle"
    System.out.println("Found a " + type + " (" + category + ")");
});
```

### 3. Vehicle Damage Analysis 💥
Our "Magic" feature: it first finds vehicles and then looks for scratches, dents, or broken glass.

```java
VisionResult damageResult = visionTemplate.detectVehicleDamages(image);

damageResult.detections().forEach(dmg -> {
    String damage = (String) dmg.attributes().get("damageType");
    String severity = (String) dmg.attributes().get("severity"); // MINOR, MODERATE, SEVERE
    System.out.println("⚠️ Damage detected: " + damage + " | Severity: " + severity);
});
```

---

## ⚠️ Common Gotchas (Troubleshooting)

1. **"It's taking a long time to start!"**
   - *Why?* The first time you run your app, Spring Vision downloads the AI models (around 1GB). Be patient on the first boot!
   
2. **`OutOfMemoryError`**
   - *Why?* AI models need room to breathe.
   - *Fix:* Increase your memory limit. Add `-Xmx4g` to your JVM arguments to give it 4GB of RAM.

3. **`UnsupportedClassVersionError`**
   - *Fix:* Update your JDK to version 25 or higher!

---

**Next Steps:** Check out the [API Usage Guide](../development/API_USAGE.md) for more advanced tricks and techniques! Happy coding! 🎈
