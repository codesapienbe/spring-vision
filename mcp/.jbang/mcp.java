/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS io.github.codesapienbe.springvision:mcp:1.0
//JAVA_OPTIONS -Xms64m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication

/**
 * JBang script to run Spring Vision MCP Server
 * <p>
 * Usage:
 * jbang mcp.java
 * <p>
 * This script automatically fetches the Spring Vision MCP server from Maven Central
 * and runs it with optimized JVM settings for MCP stdio communication.
 */
public class mcp {
    public static void main(String[] args) {
        // mcp/src/main/java/io/github/codesapienbe/springvision/mcp/SpringVisionMcpServerApplication.java
        io.github.codesapienbe.springvision.mcp.SpringVisionMcpServerApplication.main(args);
    }
}

