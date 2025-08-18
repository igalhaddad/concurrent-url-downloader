package com.hoppersecurity.url_downloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DownloadCommandIntegrationTest {

    private IntegrationTestServer testServer;
    private String baseUrl;
    private DownloadCommand downloadCommand;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        testServer = new IntegrationTestServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();
        downloadCommand = new DownloadCommand();
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.resetRequestCount(); // Reset request count for next test
            testServer.stop();
        }
    }

    @Test
    void testDownloadCommandWithValidConfig() throws IOException {
        // Create a test configuration file
        DownloadConfig config = new DownloadConfig();
        config.setUrls(Arrays.asList(
            baseUrl + "/success",
            baseUrl + "/file.txt"
        ));
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory(tempDir.resolve("cli-downloads").toString());
        config.setMaxConcurrentDownloads(2);
        config.setRetryAttempts(1);
        
        Path configFile = tempDir.resolve("test-config.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);
        
        // Execute the download command
        String result = downloadCommand.download(configFile.toString());
        
        // Verify the command executed successfully
        assertFalse(result.startsWith("Error:"));
        assertFalse(result.startsWith("Configuration error:"));
        
        // Verify the output contains expected information
        assertTrue(result.contains("Download Summary"));
        assertTrue(result.contains("Total URLs: 2"));
        assertTrue(result.contains("Successful: 2"));
        assertTrue(result.contains("Failed: 0"));
        
        // Verify files were downloaded
        Path downloadDir = tempDir.resolve("cli-downloads");
        assertTrue(Files.exists(downloadDir));
        assertTrue(Files.list(downloadDir).count() >= 2);
        
        // Verify request count
        assertEquals(2, testServer.getRequestCount());
    }

    @Test
    void testDownloadCommandWithMixedResults() throws IOException {
        // Create a test configuration file with some failing URLs
        DownloadConfig config = new DownloadConfig();
        config.setUrls(Arrays.asList(
            baseUrl + "/success",
            baseUrl + "/notfound",
            baseUrl + "/error"
        ));
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory(tempDir.resolve("cli-mixed").toString());
        config.setMaxConcurrentDownloads(3);
        config.setRetryAttempts(1);
        
        Path configFile = tempDir.resolve("mixed-config.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);
        
        // Execute the download command
        String result = downloadCommand.download(configFile.toString());
        
        // Verify the command executed successfully
        assertFalse(result.startsWith("Error:"));
        assertFalse(result.startsWith("Configuration error:"));
        
        // Verify the output contains expected information
        assertTrue(result.contains("Download Summary"));
        assertTrue(result.contains("Total URLs: 3"));
        assertTrue(result.contains("Successful: 1"));
        assertTrue(result.contains("Failed: 2"));
        assertTrue(result.contains("Failed Downloads"));
        
        // Verify request count
        assertEquals(3, testServer.getRequestCount());
    }

    @Test
    void testDownloadCommandWithInvalidConfigFile() {
        // Test with non-existent config file
        String result = downloadCommand.download("non-existent-file.json");
        
        // Verify the command failed with appropriate error message
        assertTrue(result.startsWith("Error:"));
        assertTrue(result.contains("Configuration file not found"));
    }

    @Test
    void testDownloadCommandWithInvalidJson() throws IOException {
        // Create an invalid JSON file
        Path invalidConfigFile = tempDir.resolve("invalid-config.json");
        Files.write(invalidConfigFile, "invalid json content".getBytes());
        
        String result = downloadCommand.download(invalidConfigFile.toString());
        
        // Debug output
        System.out.println("Result: '" + result + "'");
        System.out.println("Result starts with 'Error:': " + result.startsWith("Error:"));
        System.out.println("Result contains 'Error during download process': " + result.contains("Error during download process"));
        
        // Verify the command failed with appropriate error message
        assertTrue(result.startsWith("Error:"));
        assertTrue(result.contains("Error during download process"));
    }

    @Test
    void testDownloadCommandWithEmptyUrlList() throws IOException {
        // Create a config with empty URL list
        DownloadConfig config = new DownloadConfig();
        config.setUrls(List.of());
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory(tempDir.resolve("cli-empty").toString());
        config.setMaxConcurrentDownloads(3);
        
        Path configFile = tempDir.resolve("empty-config.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);
        
        String result = downloadCommand.download(configFile.toString());
        
        // Should fail due to validation
        assertTrue(result.startsWith("Configuration error:"));
        assertTrue(result.contains("URLs list cannot be empty"));
    }

    @Test
    void testDownloadCommandWithEmptyConfig() throws IOException {
        // Create a configuration file with empty URLs
        DownloadConfig config = new DownloadConfig();
        config.setUrls(List.of()); // Empty list
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory(tempDir.resolve("cli-empty").toString());
        config.setMaxConcurrentDownloads(2);
        
        Path configFile = tempDir.resolve("empty-config.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);
        
        // Execute the download command
        String result = downloadCommand.download(configFile.toString());
        
        // Verify the command failed with appropriate error
        assertTrue(result.startsWith("Configuration error:"));
        assertTrue(result.contains("URLs list cannot be empty"));
        
        // Verify no requests were made
        assertEquals(0, testServer.getRequestCount());
    }

    @Test
    void testConfigurationValidationErrors() throws IOException {
        // Test maxDownloadTimePerUrl <= 0
        DownloadConfig config1 = new DownloadConfig();
        config1.setUrls(Arrays.asList(baseUrl + "/success"));
        config1.setMaxDownloadTimePerUrl(0); // Invalid
        config1.setOutputDirectory(tempDir.resolve("test").toString());
        config1.setMaxConcurrentDownloads(3);
        
        Path configFile1 = tempDir.resolve("invalid-time-config.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile1.toFile(), config1);
        
        String result1 = downloadCommand.download(configFile1.toString());
        assertTrue(result1.startsWith("Configuration error:"));
        assertTrue(result1.contains("maxDownloadTimePerUrl must be greater than 0"));
        
        // Test null output directory
        DownloadConfig config2 = new DownloadConfig();
        config2.setUrls(Arrays.asList(baseUrl + "/success"));
        config2.setMaxDownloadTimePerUrl(30);
        config2.setOutputDirectory(null); // Invalid
        config2.setMaxConcurrentDownloads(3);
        
        Path configFile2 = tempDir.resolve("null-dir-config.json");
        mapper.writeValue(configFile2.toFile(), config2);
        
        String result2 = downloadCommand.download(configFile2.toString());
        assertTrue(result2.startsWith("Configuration error:"));
        assertTrue(result2.contains("outputDirectory cannot be empty"));
        
        // Test maxConcurrentDownloads > 100
        DownloadConfig config3 = new DownloadConfig();
        config3.setUrls(Arrays.asList(baseUrl + "/success"));
        config3.setMaxDownloadTimePerUrl(30);
        config3.setOutputDirectory(tempDir.resolve("test").toString());
        config3.setMaxConcurrentDownloads(150); // Invalid
        
        Path configFile3 = tempDir.resolve("high-concurrency-config.json");
        mapper.writeValue(configFile3.toFile(), config3);
        
        String result3 = downloadCommand.download(configFile3.toString());
        assertTrue(result3.startsWith("Configuration error:"));
        assertTrue(result3.contains("maxConcurrentDownloads cannot exceed 100"));
        
        // Test negative retry attempts
        DownloadConfig config4 = new DownloadConfig();
        config4.setUrls(Arrays.asList(baseUrl + "/success"));
        config4.setMaxDownloadTimePerUrl(30);
        config4.setOutputDirectory(tempDir.resolve("test").toString());
        config4.setMaxConcurrentDownloads(3);
        config4.setRetryAttempts(-1); // Invalid
        
        Path configFile4 = tempDir.resolve("negative-retry-config.json");
        mapper.writeValue(configFile4.toFile(), config4);
        
        String result4 = downloadCommand.download(configFile4.toString());
        assertTrue(result4.startsWith("Configuration error:"));
        assertTrue(result4.contains("retryAttempts cannot be negative"));
    }

    @Test
    void testDownloadCommandWithInvalidConfiguration() throws IOException {
        // Create a config with invalid values
        DownloadConfig config = new DownloadConfig();
        config.setUrls(Arrays.asList(baseUrl + "/success"));
        config.setMaxDownloadTimePerUrl(-1); // Invalid
        config.setOutputDirectory(tempDir.resolve("cli-invalid").toString());
        config.setMaxConcurrentDownloads(0); // Invalid
        
        Path configFile = tempDir.resolve("invalid-values-config.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);
        
        String result = downloadCommand.download(configFile.toString());
        
        // Should fail due to validation
        assertTrue(result.startsWith("Configuration error:"));
    }

    @Test
    void testDownloadCommandWithTimeout() throws IOException {
        // Create a config with timeout
        DownloadConfig config = new DownloadConfig();
        config.setUrls(Arrays.asList(
            baseUrl + "/timeout",
            baseUrl + "/success"
        ));
        config.setMaxDownloadTimePerUrl(3); // Short timeout
        config.setOutputDirectory(tempDir.resolve("cli-timeout").toString());
        config.setMaxConcurrentDownloads(2);
        config.setRetryAttempts(1);
        
        Path configFile = tempDir.resolve("timeout-config.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);
        
        String result = downloadCommand.download(configFile.toString());
        
        // Verify the command executed successfully
        assertFalse(result.startsWith("Error:"));
        assertFalse(result.startsWith("Configuration error:"));
        
        // Verify the output contains expected information
        assertTrue(result.contains("Download Summary"));
        assertTrue(result.contains("Total URLs: 2"));
        assertTrue(result.contains("Successful: 1"));
        assertTrue(result.contains("Failed: 1"));
        
        // Verify request count
        assertEquals(2, testServer.getRequestCount());
    }

    @Test
    void testDownloadCommandWithRetryLogic() throws IOException {
        // Create a config with retry attempts
        DownloadConfig config = new DownloadConfig();
        config.setUrls(Arrays.asList(
            baseUrl + "/error", // This will fail but should be retried
            baseUrl + "/success"
        ));
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory(tempDir.resolve("cli-retry").toString());
        config.setMaxConcurrentDownloads(2);
        config.setRetryAttempts(3); // Multiple retries
        
        Path configFile = tempDir.resolve("retry-config.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);
        
        String result = downloadCommand.download(configFile.toString());
        
        // Verify the command executed successfully
        assertFalse(result.startsWith("Error:"));
        assertFalse(result.startsWith("Configuration error:"));
        
        // Verify the output contains expected information
        assertTrue(result.contains("Download Summary"));
        assertTrue(result.contains("Total URLs: 2"));
        assertTrue(result.contains("Successful: 1"));
        assertTrue(result.contains("Failed: 1"));
        
        // Verify retry attempts (should be 1 + 3 retries = 4 total requests for the error endpoint)
        // Plus 1 for the success endpoint = 5 total
        assertTrue(testServer.getRequestCount() >= 4);
    }
}
