package com.hoppersecurity.url_downloader;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents the result of a URL download operation, containing both success and failure information.
 * 
 * <p>This immutable class encapsulates all relevant information about a download attempt, including:
 * <ul>
 *   <li>The original URL that was downloaded</li>
 *   <li>Success or failure status</li>
 *   <li>Timing information (start time, end time, duration)</li>
 *   <li>File information for successful downloads (filename, file size)</li>
 *   <li>Error details for failed downloads</li>
 * </ul>
 * 
 * <p>The class provides factory methods for creating success and failure results:
 * <ul>
 *   <li>{@link #success(String, String, Instant, Instant, long)} - for successful downloads</li>
 *   <li>{@link #failure(String, String, Instant, Instant)} - for failed downloads</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Successful download
 * DownloadResult success = DownloadResult.success(
 *     "http://example.com/file.txt", 
 *     "downloaded_file.txt", 
 *     startTime, 
 *     endTime, 
 *     1024L
 * );
 * 
 * // Failed download
 * DownloadResult failure = DownloadResult.failure(
 *     "http://example.com/missing.txt", 
 *     "HTTP 404: Not Found", 
 *     startTime, 
 *     endTime
 * );
 * }</pre>
 * 
 * <p>All instances are immutable and thread-safe. The duration is automatically calculated
 * from the start and end times during construction.
 * 
 * @author Igal Haddad
 * @version 1.0
 * @since 1.0
 */
public record DownloadResult(
        String url,
        String filename,
        boolean success,
        Instant startTime,
        Instant endTime,
        String errorMessage,
        long fileSize
) {
    // Compact constructor for basic validation
    public DownloadResult {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
    }

    // Factory methods to mirror previous API
    public static DownloadResult success(String url, String filename,
                                         Instant startTime, Instant endTime, long fileSize) {
        return new DownloadResult(url, filename, true, startTime, endTime, null, fileSize);
    }

    public static DownloadResult failure(String url, String errorMessage,
                                         Instant startTime, Instant endTime) {
        return new DownloadResult(url, null, false, startTime, endTime, errorMessage, 0);
    }

    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("✓ Downloaded %s to %s (%d bytes) in %dms",
                    url, filename, fileSize, duration().toMillis());
        } else {
            return String.format("✗ Failed to download %s: %s (took %dms)",
                    url, errorMessage, duration().toMillis());
        }
    }
}
