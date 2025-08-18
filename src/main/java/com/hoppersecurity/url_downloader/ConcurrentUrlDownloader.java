package com.hoppersecurity.url_downloader;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent URL downloader for efficient multi-threaded file downloads.
 * 
 * This class provides efficient concurrent downloading capabilities with the following features:
 * - Configurable thread pool size for concurrent downloads
 * - HTTP connection pooling for optimal resource utilization
 * - Retry logic with exponential backoff for failed downloads
 * - Real-time progress logging with completion order tracking
 * - Proper resource cleanup and shutdown procedures
 * - Thread-safe result collection and reporting
 * 
 * The downloader uses Apache HttpClient 5 for HTTP operations and manages its own
 * ExecutorService for thread pool management. Each download operation is isolated,
 * ensuring that failures in one download do not affect others.
 * 
 * Example usage:
 * <pre>{@code
 * DownloadConfig config = new DownloadConfig();
 * config.setUrls(Arrays.asList("http://example.com/file1.txt", "http://example.com/file2.txt"));
 * config.setMaxConcurrentDownloads(5);
 * config.setOutputDirectory("/path/to/downloads");
 * 
 * ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
 * List<DownloadResult> results = downloader.downloadAll();
 * }</pre>
 * 
 * Thread Safety: This class is thread-safe for concurrent access to the downloadAll() method,
 * though typically each instance is used for a single download session.
 * 
 * Resource Management: The downloader automatically manages HTTP connections and thread pool
 * resources, cleaning them up after download completion or in case of errors.
 * 
 * @author Igal Haddad
 * @since 1.0
 */
public class ConcurrentUrlDownloader {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentUrlDownloader.class);
    
    private final DownloadConfig config;
    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;
    private final BlockingQueue<DownloadResult> completionQueue;
    private final List<DownloadResult> allResults = new ArrayList<>();
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);

    /**
     * Creates a new ConcurrentUrlDownloader with the specified configuration.
     * 
     * @param config the download configuration
     */
    public ConcurrentUrlDownloader(DownloadConfig config) {
        this.config = config;
        this.completionQueue = new LinkedBlockingQueue<>();
        
        // Configure HTTP client
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(config.getMaxConcurrentDownloads() * 2);
        connectionManager.setDefaultMaxPerRoute(config.getMaxConcurrentDownloads());
        
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
        
        // Create our own ExecutorService
        this.executorService = Executors.newFixedThreadPool(config.getMaxConcurrentDownloads());
        logger.debug("Created own ExecutorService with {} threads", config.getMaxConcurrentDownloads());
    }

    public List<DownloadResult> downloadAll() {
        logger.info("Starting concurrent download of {} URLs with max {} concurrent downloads",
                config.getUrls().size(), config.getMaxConcurrentDownloads());
        
        Instant totalStartTime = Instant.now();
        
        try {
            // Create output directory
            createOutputDirectory();
            
            // Start completion logging thread
            Thread loggingThread = new Thread(this::logCompletionsInOrder, "DownloadLogger");
            loggingThread.start();
            
            // Submit all download tasks
            List<Future<?>> futures = new ArrayList<>();
            for (String url : config.getUrls()) {
                Future<?> future = executorService.submit(() -> downloadUrl(url));
                futures.add(future);
            }
            
            // Wait for all downloads to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Download interrupted", e);
                } catch (ExecutionException e) {
                    logger.error("Download task failed", e.getCause());
                }
            }
            
            // Stop the logging thread
            loggingThread.interrupt();
            
            Instant totalEndTime = Instant.now();
            Duration totalDuration = Duration.between(totalStartTime, totalEndTime);
            
            logger.info("All downloads completed in {}ms. Successful: {}, Failed: {}",
                    totalDuration.toMillis(), completedCount.get(), failedCount.get());
            
            // Return all collected results
            synchronized (allResults) {
                return new ArrayList<>(allResults);
            }
        } finally {
            // Always ensure proper cleanup
            shutdown();
        }
    }

    private void downloadUrl(String url) {
        Instant startTime = Instant.now();
        String filename = generateFilename(url);
        
        try {
            logger.debug("Starting download: {}", url);
            
            // Create HTTP request with timeout
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", config.getUserAgent());
            
            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(config.getConnectTimeout()))
                .setResponseTimeout(Timeout.ofSeconds(config.getMaxDownloadTimePerUrl()))
                .build();
            httpGet.setConfig(requestConfig);
            
            // Execute download with retry logic
            DownloadResult result = executeDownloadWithRetry(httpGet, url, filename, startTime);
            
            // Add to both completion queue for logging and results list for return
            completionQueue.put(result);
            synchronized (allResults) {
                allResults.add(result);
            }
            
            if (result.success()) {
                completedCount.incrementAndGet();
            } else {
                failedCount.incrementAndGet();
            }
        } catch (Exception e) {
            Instant endTime = Instant.now();
            DownloadResult result = DownloadResult.failure(url, e.getMessage(), startTime, endTime);
            try {
                completionQueue.put(result);
                synchronized (allResults) {
                    allResults.add(result);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            failedCount.incrementAndGet();
            logger.error("Failed to download {}: {}", url, e.getMessage());
        }
    }

    private DownloadResult executeDownloadWithRetry(HttpGet httpGet, String url, String filename, Instant startTime) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= config.getRetryAttempts(); attempt++) {
            try {
                return executeDownload(httpGet, url, filename, startTime);
            } catch (Exception e) {
                lastException = e;
                if (attempt < config.getRetryAttempts()) {
                    logger.warn("Download attempt {} failed for {}: {}. Retrying...", attempt, url, e.getMessage());
                    try {
                        Thread.sleep(1000L * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        Instant endTime = Instant.now();
        return DownloadResult.failure(url, "All retry attempts failed. Last error: " + lastException.getMessage(), startTime, endTime);
    }

    private DownloadResult executeDownload(HttpGet httpGet, String url, String filename, Instant startTime) throws IOException {
        try (ClassicHttpResponse response = httpClient.executeOpen(null, httpGet, HttpClientContext.create())) {
            int statusCode = response.getCode();
            
            if (statusCode >= 200 && statusCode < 300) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return saveToFile(entity, url, filename, startTime);
                } else {
                    throw new IOException("Empty response body");
                }
            } else {
                throw new IOException("HTTP " + statusCode + ": " + response.getReasonPhrase());
            }
        }
    }

    private DownloadResult saveToFile(HttpEntity entity, String url, String filename, Instant startTime) throws IOException {
        Path filePath = Paths.get(config.getOutputDirectory(), filename);
        
        try (InputStream inputStream = entity.getContent();
             FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            Instant endTime = Instant.now();
            return DownloadResult.success(url, filename, startTime, endTime, totalBytes);
        }
    }

    private void logCompletionsInOrder() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                DownloadResult result = completionQueue.poll(100, TimeUnit.MILLISECONDS);
                if (result != null) {
                    // Log completion as it happens for real-time feedback
                    if (result.success()) {
                        System.out.println(String.format("✓ Downloaded %s to %s (%d bytes) in %dms",
                            result.url(), result.filename(), result.fileSize(), result.duration().toMillis()));
                    } else {
                        System.out.println(String.format("✗ Failed to download %s: %s (took %dms)",
                            result.url(), result.errorMessage(), result.duration().toMillis()));
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateFilename(String url) {
        try {
            URL urlObj = new URI(url).toURL();
            String path = urlObj.getPath();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            
            if (filename.isEmpty() || filename.equals("/")) {
                filename = "index.html";
            }
            
            // Ensure filename is safe
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            
            // Add timestamp to avoid conflicts
            return System.currentTimeMillis() + "_" + filename;
        } catch (Exception e) {
            return System.currentTimeMillis() + "_download";
        }
    }

    private void createOutputDirectory() {
        try {
            Path outputPath = Paths.get(config.getOutputDirectory());
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
                logger.info("Created output directory: {}", config.getOutputDirectory());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create output directory: " + config.getOutputDirectory(), e);
        }
    }

    private void shutdown() {
        try {
            // Shutdown our ExecutorService
            logger.debug("Shutting down ExecutorService");
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
            // Close the HTTP client
            httpClient.close();
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }
}
