package com.hoppersecurity.url_downloader;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrentUrlDownloaderTest {

    @Test
    void testDownloadConfigValidation() {
        DownloadConfig config = new DownloadConfig();
        config.setUrls(Arrays.asList("https://httpbin.org/get"));
        config.setMaxDownloadTimePerUrl(30);
        config.setOutputDirectory("./test-downloads");
        config.setMaxConcurrentDownloads(3);
        
        assertNotNull(config.getUrls());
        assertEquals(1, config.getUrls().size());
        assertEquals(30, config.getMaxDownloadTimePerUrl());
        assertEquals("./test-downloads", config.getOutputDirectory());
        assertEquals(3, config.getMaxConcurrentDownloads());
    }

    @Test
    void testDownloadResultCreation() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(1);
        
        DownloadResult success = DownloadResult.success(
            "https://example.com/test.txt",
            "test.txt",
            start,
            end,
            1024L
        );
        
        assertTrue(success.success());
        assertEquals("https://example.com/test.txt", success.url());
        assertEquals("test.txt", success.filename());
        assertEquals(1024L, success.fileSize());
        assertNull(success.errorMessage());
        assertEquals(start, success.startTime());
        assertEquals(end, success.endTime());
        assertEquals(1000L, success.duration().toMillis());
        
        DownloadResult failure = DownloadResult.failure(
            "https://example.com/fail.txt",
            "Connection timeout",
            start,
            end
        );
        
        assertFalse(failure.success());
        assertEquals("https://example.com/fail.txt", failure.url());
        assertEquals("Connection timeout", failure.errorMessage());
        assertEquals(0L, failure.fileSize());
        assertNull(failure.filename());
        assertEquals(1000L, failure.duration().toMillis());
    }

    @Test
    void testDownloadConfigDefaults() {
        DownloadConfig config = new DownloadConfig();
        
        // Test default values (some fields have 0 as default, set via JSON or setters)
        assertEquals(0, config.getMaxDownloadTimePerUrl()); // Default is 0, set via configuration
        assertEquals(0, config.getMaxConcurrentDownloads()); // Default is 0, set via configuration
        assertEquals("Hopper-URL-Downloader/1.0", config.getUserAgent());
        assertEquals(3, config.getRetryAttempts());
        assertEquals(30, config.getConnectTimeout());
        assertEquals(60, config.getReadTimeout());
    }

    @Test
    void testDownloadConfigSettersAndGetters() {
        DownloadConfig config = new DownloadConfig();
        
        List<String> urls = Arrays.asList("http://test1.com", "http://test2.com");
        config.setUrls(urls);
        assertEquals(urls, config.getUrls());
        
        config.setMaxDownloadTimePerUrl(60);
        assertEquals(60, config.getMaxDownloadTimePerUrl());
        
        config.setOutputDirectory("/tmp/downloads");
        assertEquals("/tmp/downloads", config.getOutputDirectory());
        
        config.setMaxConcurrentDownloads(5);
        assertEquals(5, config.getMaxConcurrentDownloads());
        
        config.setUserAgent("Custom-Agent/2.0");
        assertEquals("Custom-Agent/2.0", config.getUserAgent());
        
        config.setRetryAttempts(5);
        assertEquals(5, config.getRetryAttempts());
        
        config.setConnectTimeout(15);
        assertEquals(15, config.getConnectTimeout());
        
        config.setReadTimeout(45);
        assertEquals(45, config.getReadTimeout());
    }

    @Test
    void testDownloadConfigToString() {
        DownloadConfig config = new DownloadConfig();
        config.setUrls(Arrays.asList("http://test.com"));
        config.setOutputDirectory("./downloads");
        config.setMaxConcurrentDownloads(2);
        
        String toString = config.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("http://test.com"));
        assertTrue(toString.contains("./downloads"));
        assertTrue(toString.contains("maxConcurrentDownloads=2"));
    }

    @Test
    void testDownloadResultToString() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(2);
        
        DownloadResult success = DownloadResult.success(
            "https://example.com/test.txt",
            "test.txt",
            start,
            end,
            2048L
        );
        
        String toString = success.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("https://example.com/test.txt"));
        assertTrue(toString.contains("test.txt"));
        assertTrue(toString.contains("2048"));
        // Note: DownloadResult.toString() doesn't include success flag directly
        
        DownloadResult failure = DownloadResult.failure(
            "https://example.com/fail.txt",
            "Network error",
            start,
            end
        );
        
        String failureToString = failure.toString();
        
        assertNotNull(failureToString);
        assertTrue(failureToString.contains("https://example.com/fail.txt"));
        assertTrue(failureToString.contains("Network error"));
        // Note: DownloadResult.toString() doesn't include success flag directly
    }

    @Test
    void testDownloadResultDurationCalculation() {
        Instant start = Instant.now();
        Instant end = start.plusMillis(1500); // 1.5 seconds
        
        DownloadResult result = DownloadResult.success(
            "https://example.com/test.txt",
            "test.txt",
            start,
            end,
            1024L
        );
        
        assertEquals(1500L, result.duration().toMillis());
        assertEquals(1L, result.duration().getSeconds());
    }
}
