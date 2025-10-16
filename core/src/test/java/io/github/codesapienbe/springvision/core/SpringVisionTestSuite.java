package io.github.codesapienbe.springvision.core;

import org.junit.platform.suite.api.*;

/**
 * Main test suite for Spring Vision Core module.
 *
 * <p>This suite organizes tests into categories:
 * <ul>
 *   <li>Unit Tests - Fast, isolated tests</li>
 *   <li>Integration Tests - Tests with Spring context</li>
 *   <li>Performance Tests - Benchmarking (disabled by default)</li>
 * </ul>
 * <p>
 * Run with: ./mvnw test -Dtest=SpringVisionTestSuite
 */
@Suite
@SuiteDisplayName("Spring Vision Core Test Suite")
@SelectPackages({
    "io.github.codesapienbe.springvision.core.djl",
    "io.github.codesapienbe.springvision.core.capabilities"
})
@IncludeTags("unit")
public class SpringVisionTestSuite {
    // Test suite configuration
}
