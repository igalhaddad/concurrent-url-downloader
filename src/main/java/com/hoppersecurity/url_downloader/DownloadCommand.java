package com.hoppersecurity.url_downloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Spring Shell command handler for the URL download functionality.
 * 
 * <p>This class provides the command-line interface for the Hopper URL Downloader application.
 * It handles the "download" command and manages the entire download workflow including:
 * <ul>
 *   <li>Configuration file validation and loading</li>
 *   <li>JSON parsing and configuration object creation</li>
 *   <li>Download execution orchestration</li>
 *   <li>Result reporting and error handling</li>
 *   <li>Application termination management based on Spring profiles</li>
 * </ul>
 * 
 * <p>The command supports the following usage pattern:
 * <pre>
 * download --config path/to/config.json
 * </pre>
 * 
 * <p>The class integrates with Spring Boot's profile system to determine whether the application
 * should terminate after command completion. In production mode (prod profile), the application
 * terminates automatically. In test mode (test profile), it continues running to allow for
 * additional test execution.
 * 
 * <p>Configuration files must be valid JSON containing download parameters such as URLs,
 * output directory, concurrency settings, timeouts, and retry configuration.
 * 
 * @author Igal Haddad
 * @version 1.0
 * @since 1.0
 * @see DownloadConfig
 * @see ConcurrentUrlDownloader
 */
@Component
@ShellComponent
public class DownloadCommand {
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private Environment environment;

    @ShellMethod(key = "download", value = "Download URLs concurrently using a JSON configuration file")
    public String download(@ShellOption(value = "--config", help = "Path to JSON configuration file") String configPath) {
        try {
            logger.info("Starting URL downloader with config file: {}", configPath);
            
            // Validate config file exists
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                return "Error: Configuration file not found: " + configPath;
            }
            
            // Read and parse configuration
            String configContent = Files.readString(Path.of(configPath));
            DownloadConfig config = objectMapper.readValue(configContent, DownloadConfig.class);
            
            // Validate configuration
            String validationError = validateConfig(config);
            if (validationError != null) {
                return "Configuration error: " + validationError;
            }
            
            logger.info("Configuration loaded: {}", config);
            // Create and execute downloader (uses traditional approach - creates own ExecutorService)
            ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);

            // Execute downloads
            Instant startTime = Instant.now();
            List<DownloadResult> results = downloader.downloadAll();
            Instant endTime = Instant.now();
            Duration totalDuration = Duration.between(startTime, endTime);

            // Generate summary
            long successfulDownloads = results.stream().filter(DownloadResult::success).count();
            long failedDownloads = results.size() - successfulDownloads;

            StringBuilder summary = new StringBuilder();
            summary.append("\n=== Download Summary ===\n");
            summary.append(String.format("Total URLs: %d\n", results.size()));
            summary.append(String.format("Successful: %d\n", successfulDownloads));
            summary.append(String.format("Failed: %d\n", failedDownloads));
            summary.append(String.format("Total time: %dms\n", totalDuration.toMillis()));
            summary.append(String.format("Output directory: %s\n", config.getOutputDirectory()));

            if (failedDownloads > 0) {
                summary.append("\n=== Failed Downloads ===\n");
                results.stream()
                        .filter(result -> !result.success())
                        .forEach(result -> summary.append(String.format("- %s: %s\n",
                                result.url(), result.errorMessage())));
            }

            logger.info("Download process completed successfully");

            // Schedule application termination after a brief delay to allow the response to be displayed
            // Only exit if running in production mode (not test profile)
            if (shouldTerminateApplication()) {
                scheduleApplicationTermination(0, "Terminating application...");
            }

            return summary.toString();
        } catch (Exception e) {
            String errorMessage = "Error during download process: " + e.getMessage();
            logger.error(errorMessage, e);

            // Schedule application termination even on error
            // Only exit if running in production mode (not test profile)
            if (shouldTerminateApplication()) {
                scheduleApplicationTermination(1, "Terminating application due to error...");
            }

            return "Error: " + errorMessage;
        }
    }

    /**
     * Schedules application termination with a delay to allow output display.
     * 
     * @param exitCode the exit code to use (0 for success, 1 for error)
     * @param message the log message to display before termination
     */
    private void scheduleApplicationTermination(int exitCode, String message) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Give time for the message to be displayed
                logger.info(message);
                System.exit(exitCode);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.exit(exitCode);
            }
        }).start();
    }

    private String validateConfig(DownloadConfig config) {
        if (config.getUrls() == null || config.getUrls().isEmpty()) {
            return "URLs list cannot be empty";
        }
        
        if (config.getMaxDownloadTimePerUrl() <= 0) {
            return "maxDownloadTimePerUrl must be greater than 0";
        }
        
        if (config.getOutputDirectory() == null || config.getOutputDirectory().trim().isEmpty()) {
            return "outputDirectory cannot be empty";
        }
        
        if (config.getMaxConcurrentDownloads() <= 0) {
            return "maxConcurrentDownloads must be greater than 0";
        }
        
        if (config.getMaxConcurrentDownloads() > 100) {
            return "maxConcurrentDownloads cannot exceed 100";
        }
        
        if (config.getConnectTimeout() <= 0) {
            return "connectTimeout must be greater than 0";
        }
        
        if (config.getReadTimeout() <= 0) {
            return "readTimeout must be greater than 0";
        }
        
        if (config.getRetryAttempts() < 0) {
            return "retryAttempts cannot be negative";
        }
        
        return null;
    }
    
    /**
     * Determine if the application should terminate after download completion.
     * Only terminates when not running under test profile.
     */
    private boolean shouldTerminateApplication() {
        // If Environment is null (e.g., in direct instantiation tests), don't terminate
        if (environment == null) {
            return false;
        }
        
        // Check if test profile is active
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("test".equals(profile)) {
                return false; // Don't terminate during tests
            }
        }
        
        // If no profiles are active, check default profiles
        if (activeProfiles.length == 0) {
            String[] defaultProfiles = environment.getDefaultProfiles();
            for (String profile : defaultProfiles) {
                if ("test".equals(profile)) {
                    return false; // Don't terminate during tests
                }
            }
        }
        
        // Terminate in production or when no test profile is active
        return true;
    }
}
