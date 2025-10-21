package io.github.codesapienbe.springvision.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@SpringBootApplication
@CommandLine.Command(
    name = "spring-vision-installer",
    description = "Spring Vision MCP Server Installer - Automated setup for computer vision capabilities",
    subcommands = {
        InstallCommand.class,
        UninstallCommand.class,
        StatusCommand.class,
        UpdateCommand.class
    },
    mixinStandardHelpOptions = true,
    version = "Spring Vision Installer v0.0.1"
)
public class SpringVisionInstaller implements Callable<Integer>, CommandLineRunner {

    private final CommandLine.IFactory factory;

    public SpringVisionInstaller(CommandLine.IFactory factory) {
        this.factory = factory;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringVisionInstaller.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Create and run the command line interface
        CommandLine commandLine = new CommandLine(this, factory);
        commandLine.setExecutionStrategy(new CommandLine.RunLast());
        int exitCode = commandLine.execute(args);

        // Exit with the appropriate code
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // If no subcommand is specified, show help
        CommandLine.usage(this, System.out);
        return 0;
    }
}
