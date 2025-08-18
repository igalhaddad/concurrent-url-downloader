package com.hoppersecurity.url_downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Simple test runner to demonstrate integration test functionality
 * This can be run manually to verify the downloader works correctly
 */
public class IntegrationTestRunner {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestRunner.class);

    public static void main(String[] args) {
        IntegrationTestServer testServer = null;
        try {
            // Start test server
            logger.info("Starting integration test server...");
            testServer = new IntegrationTestServer();
            testServer.start();
            String baseUrl = testServer.getBaseUrl();
            logger.info("Test server started at: {}", baseUrl);

            // Create temporary directory for downloads
            Path tempDir = Files.createTempDirectory("integration-test");
            logger.info("Using temporary directory: {}", tempDir);

            // Test 1: Basic successful downloads
            logger.info("\n=== Test 1: Basic Successful Downloads ===");
            testSuccessfulDownloads(baseUrl, tempDir);

            // Test 2: Mixed success and failure
            logger.info("\n=== Test 2: Mixed Success and Failure ===");
            testMixedResults(baseUrl, tempDir);

            // Test 3: Concurrent downloads
            logger.info("\n=== Test 3: Concurrent Downloads ===");
            testConcurrentDownloads(baseUrl, tempDir);

            // Test 4: Timeout handling
            logger.info("\n=== Test 4: Timeout Handling ===");
            testTimeoutHandling(baseUrl, tempDir);

            // Test 5: Retry logic
            logger.info("\n=== Test 5: Retry Logic ===");
            testRetryLogic(baseUrl, tempDir);

            // Test 6: CLI command
            logger.info("\n=== Test 6: CLI Command ===");
            testCliCommand(baseUrl, tempDir);

            logger.info("\n=== All Integration Tests Completed Successfully ===");

        } catch (Exception e) {
            logger.error("Integration test failed", e);
            System.exit(1);
        } finally {
            if (testServer != null) {
                testServer.stop();
                logger.info("Test server stopped");
            }
            // Ensure JVM terminates cleanly after all tests complete
            System.exit(0);
        }
    }

    private static void testSuccessfulDownloads(String baseUrl, Path tempDir) {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/success",
            baseUrl + "/file.txt",
            baseUrl + "/binary"
        ), tempDir.resolve("test1"));

        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();

        logger.info("Results: {} successful, {} failed", 
            results.stream().filter(DownloadResult::success).count(),
            results.stream().filter(r -> !r.success()).count());

        for (DownloadResult result : results) {
            logger.info("  {}", result);
        }
    }

    private static void testMixedResults(String baseUrl, Path tempDir) {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/success",
            baseUrl + "/notfound",
            baseUrl + "/error",
            baseUrl + "/file.txt"
        ), tempDir.resolve("test2"));

        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();

        logger.info("Results: {} successful, {} failed", 
            results.stream().filter(DownloadResult::success).count(),
            results.stream().filter(r -> !r.success()).count());

        for (DownloadResult result : results) {
            logger.info("  {}", result);
        }
    }

    private static void testConcurrentDownloads(String baseUrl, Path tempDir) {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/slow",
            baseUrl + "/success",
            baseUrl + "/file.txt",
            baseUrl + "/binary",
            baseUrl + "/large"
        ), tempDir.resolve("test3"));
        config.setMaxConcurrentDownloads(2);

        long startTime = System.currentTimeMillis();
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        long endTime = System.currentTimeMillis();

        logger.info("Concurrent download completed in {}ms", endTime - startTime);
        logger.info("Results: {} successful, {} failed", 
            results.stream().filter(DownloadResult::success).count(),
            results.stream().filter(r -> !r.success()).count());
    }

    private static void testTimeoutHandling(String baseUrl, Path tempDir) {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/timeout",
            baseUrl + "/success"
        ), tempDir.resolve("test4"));
        config.setMaxDownloadTimePerUrl(3); // 3 second timeout

        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();

        logger.info("Results: {} successful, {} failed", 
            results.stream().filter(DownloadResult::success).count(),
            results.stream().filter(r -> !r.success()).count());

        for (DownloadResult result : results) {
            logger.info("  {}", result);
        }
    }

    private static void testRetryLogic(String baseUrl, Path tempDir) {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/error", // This will fail but should be retried
            baseUrl + "/success"
        ), tempDir.resolve("test5"));
        config.setRetryAttempts(3);

        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();

        logger.info("Results: {} successful, {} failed", 
            results.stream().filter(DownloadResult::success).count(),
            results.stream().filter(r -> !r.success()).count());

        for (DownloadResult result : results) {
            logger.info("  {}", result);
        }
    }

    private static void testCliCommand(String baseUrl, Path tempDir) throws IOException {
        // Create a test configuration file
        DownloadConfig config = new DownloadConfig();
        config.setUrls(Arrays.asList(
            baseUrl + "/success",
            baseUrl + "/file.txt",
            baseUrl + "/notfound"
        ));
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory(tempDir.resolve("cli-test").toString());
        config.setMaxConcurrentDownloads(3);
        config.setRetryAttempts(1);

        Path configFile = tempDir.resolve("cli-config.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.writeValue(configFile.toFile(), config);

        // Execute the download command
        DownloadCommand downloadCommand = new DownloadCommand();
        String result = downloadCommand.download(configFile.toString());

        logger.info("CLI Command Result:");
        logger.info(result);
    }

    private static DownloadConfig createTestConfig(List<String> urls, Path outputDir) {
        DownloadConfig config = new DownloadConfig();
        config.setUrls(urls);
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory(outputDir.toString());
        config.setMaxConcurrentDownloads(5);
        config.setRetryAttempts(1);
        config.setConnectTimeout(5);
        config.setReadTimeout(10);
        return config;
    }
}
