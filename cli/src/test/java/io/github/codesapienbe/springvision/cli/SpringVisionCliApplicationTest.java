package io.github.codesapienbe.springvision.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class SpringVisionCliApplicationTest {

    @Test
    void testCliApplicationCreation() {
        SpringVisionCliApplication app = new SpringVisionCliApplication();
        assertNotNull(app);
    }

    @Test
    void testCommandLineParsing() {
        SpringVisionCliApplication app = new SpringVisionCliApplication();
        CommandLine cmd = new CommandLine(app);

        // Test help option
        int exitCode = cmd.execute("--help");
        // Help returns 0
        assertEquals(0, exitCode);
    }

    @Test
    void testVersionCommand() {
        SpringVisionCliApplication app = new SpringVisionCliApplication();
        CommandLine cmd = new CommandLine(app);

        // Test version option
        int exitCode = cmd.execute("--version");
        // Version returns 0
        assertEquals(0, exitCode);
    }
}
