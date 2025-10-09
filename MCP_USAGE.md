# 🚀 Using Spring Vision with Your AI Assistant 🤖

Welcome! This guide will show you how to connect the Spring Vision service to your favorite AI assistants and development frameworks. This will give your assistant the power of computer vision! 👁️

This guide is for everyone—from beginners to experienced developers! 😊

---

## 🤔 What is Spring Vision?

In simple terms, **Spring Vision gives your AI assistant eyes**. 

Once connected, your AI can analyze images to detect faces, objects, and text. It's like giving your assistant a new set of superpowers! 🦸

---

## 🏁 Step 1: Start the Spring Vision Service 🐳

First, you need to get the Spring Vision service running on your computer. We'll use Docker to make this super simple.

1.  **Open your terminal**:
    *   On Mac: `Applications > Utilities > Terminal`
    *   On Windows: `PowerShell` or `Command Prompt`

2.  **Run the command**:
    Copy and paste the following command into your terminal and press **Enter**.

    ```bash
    docker run -d -p 8080:8080 --name spring-vision-mcp docker.io/codesapienbe/spring-vision:latest
    ```

3.  **Check if it's running** ✅
    To make sure it's working, run this command:

    ```bash
    curl http://localhost:8080/api/vision/health
    ```

    You should see this happy message, which means the service is ready:

    ```json
    {
      "status": "UP",
      "service": "Spring Vision MCP Server",
      "version": "1.0.0"
    }
    ```

---

## 🔌 Step 2: How to Connect Spring Vision

### An Important Note: Why Aren't Claude, Cursor, or VSCode on This List?

This is the most common question we get. The answer is that most popular AI assistants like **Claude.ai**, **Cursor**, the **IntelliJ AI Assistant**, and the **VSCode AI Assistant** are **closed products**. 

Think of them like a smart TV—you can use the apps the manufacturer provides (Netflix, YouTube), but you can't install a custom video player you built yourself. 

These AI assistants do not have a feature in their settings to add a custom tool server like Spring Vision. The methods below work because they use **open systems** (like Ollama and various developer frameworks) that are designed to be connected to other tools.

Here are the top ways to connect Spring Vision to an AI model using these open systems.

### Method 1: Ollama + Open WebUI (The Easiest Way) ✨

This is the most user-friendly approach. You run a powerful open-source model on your machine with **Ollama** and chat with it through a beautiful web interface that can connect to our vision tool.

1.  **Install Ollama:** Follow the instructions on the [Ollama website](https://ollama.com/).
2.  **Install Open WebUI:** Follow their official instructions (usually a simple Docker command).
3.  **Open Settings:** In the Open WebUI interface, click your profile and go to **Settings** > **Tools**.
4.  **Enable MCP:** Toggle the switch to **Enable Model Context Protocol (MCP)**.
5.  **Enter the URL:** In the `MCP Endpoint URL` field, paste the Spring Vision server address:
    `http://localhost:8080`
6.  **Save and Chat!** Your local AI model now has vision!

### Method 2: Langflow (Visual AI Builder) 🎨

[Langflow](https://www.langflow.org/) is a fantastic low-code tool that lets you build AI applications by dragging and dropping components. It's a great way to visually connect models and tools.

1.  **Install Langflow:** Follow their official installation guide.
2.  **Create a New Flow:** Start a new project on the Langflow canvas.
3.  **Add a Custom Tool:** From the components menu, search for and add the "CustomTool" component.
4.  **Configure the Tool:** In the tool's settings, you will define the function that calls our Spring Vision API. You would typically specify the API endpoint (`http://localhost:8080/api/vision/detect/base64`) and the expected inputs (like `image` and `detectionType`).
5.  **Connect to a Model:** Drag an LLM component (like `Ollama` or `OpenAI`) onto the canvas and connect your custom tool to it.
6.  **Chat:** Use the chat interface to interact with your new vision-enabled flow.

### Method 3: LangChain (For Python Developers) 🐍

[LangChain](https://www.langchain.com/) is the most popular framework for developers building AI applications. You can easily add the Spring Vision server as a custom tool in your Python code.

1.  **Define a Custom Tool:** In your Python script, you create a `Tool` object.
2.  **Implement the Function:** The tool's function will make an HTTP POST request to the Spring Vision server. You can use a library like `requests` to call `http://localhost:8080/api/vision/detect/base64`.
3.  **Create an Agent:** Combine your tool with an LLM (from OpenAI, Anthropic, Ollama, etc.) to create a LangChain "agent."

    ```python
    # Example of the function inside the tool
    def detect_vision_objects(image_b64: str, detection_type: str) -> str:
        response = requests.post(
            "http://localhost:8080/api/vision/detect/base64",
            params={"detectionType": detection_type},
            json={"image": image_b64}
        )
        return response.json()
    ```

### Method 4: LlamaIndex (For Data-Focused Developers) 🦙

[LlamaIndex](https://www.llamaindex.ai/) is another powerful framework, often used for building applications that work with your own data (RAG). It also supports custom tools.

1.  **Create a FunctionTool:** LlamaIndex makes it easy to convert any Python function into a tool the LLM can use.
2.  **Write the Wrapper Function:** Write a Python function that calls the Spring Vision API endpoint, similar to the LangChain example.
3.  **Attach to an Agent:** Create a LlamaIndex agent (like `OpenAIAgent`) and give it your new function tool.

    ```python
    # Example of the wrapper function
    from llama_index.core.tools import FunctionTool

    def vision_tool_func(image_file: str, detection_type: str) -> dict:
        # Code to read image file, base64 encode it, and call the API
        # ...
        return {}

    vision_tool = FunctionTool.from_defaults(fn=vision_tool_func)
    ```

### Method 5: Custom App + Any LLM API (The DIY / Claude Method) 🛠️

This is the most flexible method and the **only way** to use Spring Vision with a closed provider like **Claude** or **OpenAI**.

You write your own code to act as the bridge between the LLM provider's API and the Spring Vision server.

1.  **Choose an LLM API:** Pick your favorite provider (e.g., Anthropic for Claude).
2.  **Make the First API Call:** Send your prompt and the definitions of the available vision tools to the LLM.
3.  **Check the Response:** The LLM will respond with a special message if it wants to use a tool.
4.  **Call the Vision Server:** If the LLM wants to use a tool, your code will call the `http://localhost:8080` endpoint with the right parameters.
5.  **Make the Second API Call:** Send the result from the vision server back to the LLM. It will then use that information to give you a final, human-readable answer.

---

## 🎉 Step 3: Time to Play!

Now that you've connected Spring Vision using your chosen method, it's time for the fun part! Ask your AI to analyze an image:

> "How many faces are in this image?"

> "What objects do you see in this picture?"

Enjoy your new, super-powered AI assistant! ✨
