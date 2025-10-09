# Using Spring Vision as an MCP Server

This guide demonstrates how to run the `spring-vision-mcp` server and integrate its computer vision capabilities as tools within a Spring AI application, following the Model Context Protocol (MCP) principles.

The `spring-vision-mcp` module acts as a remote tool provider, exposing computer vision functions over a REST API.

## 1. Run the Spring Vision MCP Server

First, you need to run the `spring-vision-mcp` server. We'll use the pre-built Docker image available on Docker Hub.

Open your terminal and run the following command to start the server in a Docker container:

```bash
docker run -d -p 8080:8080 --name spring-vision-mcp docker.io/codesapienbe/spring-vision:latest
```

This command will:
- Download the `spring-vision` image from Docker Hub.
- Start a container named `spring-vision-mcp`.
- Map port `8080` of the container to port `8080` on your local machine.

### Verify the Server is Running

You can check the server's health by sending a GET request to its health endpoint:

```bash
curl http://localhost:8080/api/vision/health
```

You should see a response like this, confirming the server is up and running:

```json
{
  "status": "UP",
  "service": "Spring Vision MCP Server",
  "version": "1.0.0"
}
```

## 2. Integrate with a Spring AI Application

Now, let's configure a Spring AI application to use the computer vision tools from our running server.

### Step 2.1: Add Dependencies

In your Spring AI project's `pom.xml`, ensure you have the necessary dependencies for Spring AI and a chat model (like OpenAI):

```xml
<dependencies>
    <!-- Spring AI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- For making REST calls to the vision server -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

### Step 2.2: Configure Application Properties

Add your OpenAI API key to `src/main/resources/application.properties`:

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

### Step 2.3: Create a Tool Configuration

The core of the integration is to define Spring beans that represent the remote tools. These beans will handle the communication with the `spring-vision-mcp` server.

Create a new configuration class, `VisionToolConfiguration.java`, to define these tools.

```java
package com.example.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class VisionToolConfiguration {

    @Bean
    @Description("Detects objects, faces, or text in a given image. The image must be provided as a Base64 encoded string.")
    public Function<VisionToolRequests.DetectRequest, Map> detect(WebClient.Builder webClientBuilder) {
        return request -> {
            WebClient webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
            
            // The server expects a JSON body with an "image" field
            Map<String, String> requestBody = Map.of("image", request.base64Image());

            return webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/vision/detect/base64")
                            .queryParam("detectionType", request.detectionType())
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        };
    }
}
```

### Step 2.4: Define a Request Record

Create a record to define the input for our tool function. This makes the function's signature clear to the AI model.

```java
package com.example.ai.config;

public class VisionToolRequests {
    /**
     * Request for the vision detection tool.
     * @param base64Image The Base64 encoded string of the image to analyze.
     * @param detectionType The type of detection to perform. Can be 'FACE', 'OBJECT', or 'TEXT'.
     */
    public record DetectRequest(String base64Image, String detectionType) {}
}
```

## 3. Use the Tool in Your Code

With the tool configured, you can now inject `ChatClient` into your services and ask the AI model to perform computer vision tasks. The model will automatically know when to call your `detect` function.

Here's an example of how you could use it in a controller. For a real-world scenario, you would get the image data from a file upload, convert it to Base64, and then pass it in the prompt.

```java
@RestController
public class VisionAiController {

    private final ChatClient chatClient;

    public VisionAiController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultOptions(OpenAiChatOptions.builder()
                        .withFunction("detect") // Register our tool function
                        .build())
                .build();
    }

    @GetMapping("/ai/vision/count-faces")
    public String countFacesInImage() {
        // In a real app, you'd get this from a file upload and encode it.
        String base64ImageData = "... your base64 encoded image data ...";

        String prompt = """
            Please count how many faces are in the provided image.
            Image data is: %s
        """.formatted(base64ImageData);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
```

When you call the `/ai/vision/count-faces` endpoint, Spring AI will:
1.  Send the prompt and the `detect` function definition to the LLM.
2.  The LLM will recognize that it needs to call the `detect` function with the image data and `detectionType='FACE'`.
3.  Spring AI will execute your `detect` bean, which calls the `http://localhost:8080/api/vision/detect/base64` endpoint on the `spring-vision-mcp` server.
4.  The server's response is sent back to the LLM.
5.  The LLM uses the tool's output to formulate the final answer, such as "There are 3 faces in the image."

This setup effectively uses your `spring-vision-mcp` server as a remote tool provider for your AI applications, paving the way for a full MCP-compliant architecture.