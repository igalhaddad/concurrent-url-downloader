package com.hoppersecurity.url_downloader;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrentUrlDownloaderIntegrationTest {

    private IntegrationTestServer testServer;
    private String baseUrl;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        testServer = new IntegrationTestServer();
        testServer.start();
        baseUrl = testServer.getBaseUrl();
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.resetRequestCount(); // Reset request count for next test
            testServer.stop();
        }
    }

    @Test
    void testSuccessfulDownloads() throws IOException {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/success",
            baseUrl + "/file.txt",
            baseUrl + "/binary"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(3, results.size());
        assertEquals(3, results.stream().filter(DownloadResult::success).count());
        assertEquals(0, results.stream().filter(r -> !r.success()).count());
        
        // Verify files were created
        assertTrue(Files.exists(tempDir.resolve("downloads")));
        assertTrue(Files.list(tempDir.resolve("downloads")).count() >= 3);
        
        // Verify request count
        assertEquals(3, testServer.getRequestCount());
    }

    @Test
    void testMixedSuccessAndFailure() {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/success",
            baseUrl + "/notfound",
            baseUrl + "/error",
            baseUrl + "/file.txt"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(4, results.size());
        assertEquals(2, results.stream().filter(DownloadResult::success).count());
        assertEquals(2, results.stream().filter(r -> !r.success()).count());
        
        // Verify specific failures
        List<DownloadResult> failures = results.stream()
            .filter(r -> !r.success())
            .toList();
        
        assertTrue(failures.stream().anyMatch(r -> r.url().contains("/notfound")));
        assertTrue(failures.stream().anyMatch(r -> r.url().contains("/error")));
        
        // Verify request count
        assertEquals(4, testServer.getRequestCount());
    }

    @Test
    void testConcurrentDownloads() {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/slow",
            baseUrl + "/success",
            baseUrl + "/file.txt",
            baseUrl + "/binary",
            baseUrl + "/large"
        ));
        config.setMaxConcurrentDownloads(2);
        
        Instant startTime = Instant.now();
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        Instant endTime = Instant.now();
        
        Duration totalTime = Duration.between(startTime, endTime);
        
        assertEquals(5, results.size());
        assertEquals(5, results.stream().filter(DownloadResult::success).count());
        
        // Verify concurrency: total time should be less than sum of individual times
        // The slow endpoint takes 2 seconds, so total time should be around 2-3 seconds
        // rather than 2 + 0.1 + 0.1 + 0.1 + 0.5 = ~2.8 seconds if sequential
        assertTrue(totalTime.toMillis() < 4000, 
            "Total time (" + totalTime.toMillis() + "ms) should be less than 4000ms due to concurrency");
        
        // Verify request count
        assertEquals(5, testServer.getRequestCount());
    }

    @Test
    void testTimeoutHandling() {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/timeout",
            baseUrl + "/success"
        ));
        config.setMaxDownloadTimePerUrl(3); // 3 second timeout
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(2, results.size());
        assertEquals(1, results.stream().filter(DownloadResult::success).count());
        assertEquals(1, results.stream().filter(r -> !r.success()).count());
        
        // Verify timeout failure
        DownloadResult timeoutResult = results.stream()
            .filter(r -> !r.success())
            .findFirst()
            .orElse(null);
        
        assertNotNull(timeoutResult);
        assertTrue(timeoutResult.url().contains("/timeout"));
        assertTrue(timeoutResult.errorMessage().contains("timeout") ||
                  timeoutResult.errorMessage().contains("Timeout") ||
                  timeoutResult.errorMessage().contains("Read timed out"));
        
        // Verify request count
        assertEquals(2, testServer.getRequestCount());
    }

    @Test
    void testRetryLogic() {
        // Create a config with retry attempts
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/error", // This will fail but should be retried
            baseUrl + "/success"
        ));
        config.setRetryAttempts(2);
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(2, results.size());
        assertEquals(1, results.stream().filter(DownloadResult::success).count());
        assertEquals(1, results.stream().filter(r -> !r.success()).count());
        
        // Verify retry attempts (should be 1 + 2 retries = 3 total requests for the error endpoint)
        // Plus 1 for the success endpoint = 4 total
        assertTrue(testServer.getRequestCount() >= 3);
    }

    @Test
    void testLargeFileDownload() throws IOException {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/large"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(1, results.size());
        DownloadResult result = results.getFirst();
        
        assertTrue(result.success());
        assertEquals(1024 * 1024, result.fileSize()); // 1MB
        
        // Verify file was created with correct size
        Path downloadDir = tempDir.resolve("downloads");
        assertTrue(Files.exists(downloadDir));
        
        long fileCount = Files.list(downloadDir).count();
        assertTrue(fileCount >= 1);
        
        // Find the downloaded file and verify its size
        Files.list(downloadDir)
            .filter(path -> path.toString().contains("large"))
            .findFirst()
            .ifPresent(file -> {
                try {
                    assertEquals(1024 * 1024, Files.size(file));
                } catch (IOException e) {
                    fail("Failed to get file size: " + e.getMessage());
                }
            });
    }

    @Test
    void testBinaryFileDownload() throws IOException {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/binary"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(1, results.size());
        DownloadResult result = results.getFirst();
        
        assertTrue(result.success());
        assertEquals(1024, result.fileSize()); // 1KB
        
        // Verify binary file was created
        Path downloadDir = tempDir.resolve("downloads");
        assertTrue(Files.exists(downloadDir));
        
        Files.list(downloadDir)
            .filter(path -> path.toString().contains("binary"))
            .findFirst()
            .ifPresent(file -> {
                try {
                    assertEquals(1024, Files.size(file));
                    // Verify it's actually binary data
                    byte[] content = Files.readAllBytes(file);
                    assertTrue(content.length > 0);
                } catch (IOException e) {
                    fail("Failed to read binary file: " + e.getMessage());
                }
            });
    }

    @Test
    void testCompletionOrderLogging() {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/slow",  // Takes 2 seconds
            baseUrl + "/success", // Fast
            baseUrl + "/file.txt"  // Fast
        ));
        config.setMaxConcurrentDownloads(3);
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(3, results.size());
        assertEquals(3, results.stream().filter(DownloadResult::success).count());
        
        // Verify that fast downloads completed before slow ones
        // The results should be ordered by completion time, not start time
        List<DownloadResult> orderedResults = results.stream()
            .sorted((r1, r2) -> r1.endTime().compareTo(r2.endTime()))
            .toList();
        
        // Fast downloads should complete before slow ones
        assertTrue(orderedResults.get(0).duration().toMillis() < 1000);
        assertTrue(orderedResults.get(1).duration().toMillis() < 1000);
        assertTrue(orderedResults.get(2).duration().toMillis() >= 2000);
    }

    @Test
    void testEmptyUrlList() {
        DownloadConfig config = createTestConfig(List.of());
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(0, results.size());
        assertEquals(0, testServer.getRequestCount());
    }

    @Test
    void testInvalidUrls() {
        DownloadConfig config = createTestConfig(Arrays.asList(
            "http://invalid-domain-that-does-not-exist-12345.com/file.txt",
            baseUrl + "/success"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(2, results.size());
        assertEquals(1, results.stream().filter(DownloadResult::success).count());
        assertEquals(1, results.stream().filter(r -> !r.success()).count());
        
        // Verify the invalid URL failed
        DownloadResult failure = results.stream()
            .filter(r -> !r.success())
            .findFirst()
            .orElse(null);
        
        assertNotNull(failure);
        assertTrue(failure.url().contains("invalid-domain"));
        
        // Verify request count (only the valid URL should have been requested)
        assertEquals(1, testServer.getRequestCount());
    }

    @Test
    void testMaxConcurrentDownloadsLimit() {
        // Test that max concurrent downloads is respected
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/slow",  // 2 seconds each
            baseUrl + "/slow",
            baseUrl + "/slow",
            baseUrl + "/slow",
            baseUrl + "/slow"   // 5 slow downloads
        ));
        config.setMaxConcurrentDownloads(2); // Limit to 2 concurrent
        
        Instant startTime = Instant.now();
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        Instant endTime = Instant.now();
        
        Duration totalTime = Duration.between(startTime, endTime);
        
        assertEquals(5, results.size());
        assertEquals(5, results.stream().filter(DownloadResult::success).count());
        
        // With 2 concurrent downloads and 5 URLs taking 2 seconds each,
        // total time should be around 6 seconds (3 batches of 2 seconds each)
        // rather than 2 seconds if all were concurrent
        assertTrue(totalTime.toMillis() >= 5000, 
            "Total time should be at least 5 seconds due to concurrency limit");
        assertTrue(totalTime.toMillis() <= 8000, 
            "Total time should not exceed 8 seconds");
    }

    @Test
    void testUserAgentHeader() {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/success"
        ));
        config.setUserAgent("Custom-Test-Agent/1.0");
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(1, results.size());
        assertTrue(results.getFirst().success());
        
        // Verify request was made (user agent verification would require server-side logging)
        assertEquals(1, testServer.getRequestCount());
    }

    @Test
    void testDifferentTimeoutSettings() {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/timeout"
        ));
        config.setMaxDownloadTimePerUrl(1); // Very short timeout
        config.setConnectTimeout(1);
        config.setReadTimeout(1);
        
        Instant startTime = Instant.now();
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        Instant endTime = Instant.now();
        
        Duration totalTime = Duration.between(startTime, endTime);
        
        assertEquals(1, results.size());
        assertFalse(results.getFirst().success());
        
        // Should timeout quickly (within 2-3 seconds including retry delays)
        assertTrue(totalTime.toMillis() < 5000, 
            "Should timeout quickly with short timeout settings");
    }

    @Test
    void testFilenameGeneration() throws IOException {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/file.txt",
            baseUrl + "/success",
            baseUrl + "/binary"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(3, results.size());
        assertEquals(3, results.stream().filter(DownloadResult::success).count());
        
        // Verify all results have non-null filenames
        results.forEach(result -> {
            assertNotNull(result.filename());
            assertFalse(result.filename().isEmpty());
        });
        
        // Verify files were created with generated names
        Path downloadDir = tempDir.resolve("downloads");
        assertTrue(Files.exists(downloadDir));
        assertEquals(3, Files.list(downloadDir).count());
    }

    @Test
    void testResourceCleanup() {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/success",
            baseUrl + "/error"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(2, results.size());
        
        // Test that we can create another downloader (resources were cleaned up)
        ConcurrentUrlDownloader downloader2 = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results2 = downloader2.downloadAll();
        
        assertEquals(2, results2.size());
        
        // Verify total requests (2 from first downloader + 2 from second)
        assertEquals(4, testServer.getRequestCount());
    }

    @Test
    void testFilenameGenerationEdgeCases() {
        // Add a test endpoint that returns different URL patterns
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/path/with/special-chars!@#$%^&*()file.txt",
            baseUrl + "/",  // Root path
            baseUrl + "/no-extension",
            baseUrl + "/path/ending/with/slash/",
            baseUrl + "/file.with.multiple.dots.txt"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        // All should succeed (our test server handles these paths)
        assertEquals(5, results.size());
        
        // Verify all results have valid filenames
        results.forEach(result -> {
            if (result.success()) {
                assertNotNull(result.filename());
                assertFalse(result.filename().isEmpty());
                // Filename should contain timestamp and be filesystem-safe
                assertTrue(result.filename().matches("\\d+_.*"));
                // Should not contain unsafe characters
                assertFalse(result.filename().matches(".*[!@#$%^&*()].*"));
            }
        });
        
        // Verify files were created
        Path downloadDir = tempDir.resolve("downloads");
        assertTrue(Files.exists(downloadDir));
    }

    @Test
    void testDirectoryCreationFailure() {
        // Test with an invalid directory path (on Windows, this would be invalid)
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/success"
        ));
        
        // Set an invalid output directory path
        String invalidPath = System.getProperty("os.name").toLowerCase().contains("win") 
            ? "C:\\invalid\\path\\with\\null\\char\0\\test"  // Invalid on Windows
            : "/proc/invalid/readonly/path";  // Invalid on Unix-like systems
        
        config.setOutputDirectory(invalidPath);
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        
        // This should throw a RuntimeException due to directory creation failure
        assertThrows(RuntimeException.class, downloader::downloadAll);
    }

    @Test
    void testInterruptionHandling() throws InterruptedException {
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/slow",  // 2-second delay
            baseUrl + "/slow",
            baseUrl + "/slow"
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        
        // Start downloads in a separate thread
        Thread downloadThread = new Thread(() -> {
            try {
                downloader.downloadAll();
            } catch (Exception e) {
                // Expected due to interruption
            }
        });
        
        downloadThread.start();
        
        // Let it start, then interrupt
        Thread.sleep(500);
        downloadThread.interrupt();
        
        // Wait for thread to finish
        downloadThread.join(5000);
        
        // Thread should have been interrupted and finished
        assertFalse(downloadThread.isAlive());
    }

    @Test
    void testZeroByteFileDownload() {
        // Test downloading a zero-byte file
        DownloadConfig config = createTestConfig(Arrays.asList(
            baseUrl + "/empty"  // This endpoint should return empty content
        ));
        
        ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
        List<DownloadResult> results = downloader.downloadAll();
        
        assertEquals(1, results.size());
        DownloadResult result = results.getFirst();
        
        // Should succeed even with zero bytes
        assertTrue(result.success());
        assertEquals(0, result.fileSize());
        
        // Verify file was created
        Path downloadDir = tempDir.resolve("downloads");
        assertTrue(Files.exists(downloadDir));
        
        // Find the downloaded file and verify its size
        // Since filename generation uses timestamp, we can't predict exact name
        // Just verify that exactly one file was created and it has zero size
        try {
            long fileCount = Files.list(downloadDir).count();
            assertEquals(1, fileCount, "Expected exactly one file to be downloaded");
            
            Files.list(downloadDir)
                .findFirst()
                .ifPresent(file -> {
                    try {
                        assertEquals(0, Files.size(file));
                    } catch (IOException e) {
                        fail("Failed to get file size: " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            fail("Failed to list download directory: " + e.getMessage());
        }
    }

    private DownloadConfig createTestConfig(List<String> urls) {
        DownloadConfig config = new DownloadConfig();
        config.setUrls(urls);
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory(tempDir.resolve("downloads").toString());
        config.setMaxConcurrentDownloads(5);
        config.setRetryAttempts(1);
        config.setConnectTimeout(5);
        config.setReadTimeout(10);
        return config;
    }
}
