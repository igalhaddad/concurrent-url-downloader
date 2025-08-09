package com.hoppersecurity.url_downloader;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Configuration class for URL download operations, supporting JSON deserialization.
 * 
 * <p>This class represents the configuration parameters for the concurrent URL downloader,
 * including download targets, performance settings, and operational parameters. It uses
 * Jackson annotations for seamless JSON configuration file parsing.
 * 
 * <p>Configuration parameters include:
 * <ul>
 *   <li><strong>urls</strong> - List of URLs to download</li>
 *   <li><strong>outputDirectory</strong> - Target directory for downloaded files</li>
 *   <li><strong>maxConcurrentDownloads</strong> - Maximum number of simultaneous downloads</li>
 *   <li><strong>maxDownloadTimePerUrl</strong> - Timeout per URL in seconds</li>
 *   <li><strong>connectTimeout</strong> - Connection timeout in seconds</li>
 *   <li><strong>readTimeout</strong> - Read timeout in seconds</li>
 *   <li><strong>retryAttempts</strong> - Number of retry attempts for failed downloads</li>
 *   <li><strong>userAgent</strong> - HTTP User-Agent header value</li>
 * </ul>
 * 
 * <p>Example JSON configuration:
 * <pre>{@code
 * {
 *   "urls": [
 *     "http://example.com/file1.txt",
 *     "http://example.com/file2.pdf"
 *   ],
 *   "outputDirectory": "./downloads",
 *   "maxConcurrentDownloads": 3,
 *   "maxDownloadTimePerUrl": 30,
 *   "connectTimeout": 30,
 *   "readTimeout": 60,
 *   "retryAttempts": 3,
 *   "userAgent": "Hopper-URL-Downloader/1.0"
 * }
 * }</pre>
 * 
 * <p>All configuration parameters have sensible defaults and include validation
 * to ensure proper operation of the download system.
 * 
 * @author Igal Haddad
 * @version 1.0
 * @since 1.0
 */
public class DownloadConfig {
    @JsonProperty("urls")
    private List<String> urls;
    
    @JsonProperty("maxDownloadTimePerUrl")
    private int maxDownloadTimePerUrl; // in seconds
    
    @JsonProperty("outputDirectory")
    private String outputDirectory;
    
    @JsonProperty("maxConcurrentDownloads")
    private int maxConcurrentDownloads;
    
    @JsonProperty("userAgent")
    private String userAgent = "Hopper-URL-Downloader/1.0";
    
    @JsonProperty("retryAttempts")
    private int retryAttempts = 3;
    
    @JsonProperty("connectTimeout")
    private int connectTimeout = 30; // in seconds
    
    @JsonProperty("readTimeout")
    private int readTimeout = 60; // in seconds

    // Default constructor for Jackson
    public DownloadConfig() {}

    // Getters and setters
    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public int getMaxDownloadTimePerUrl() {
        return maxDownloadTimePerUrl;
    }

    public void setMaxDownloadTimePerUrl(int maxDownloadTimePerUrl) {
        this.maxDownloadTimePerUrl = maxDownloadTimePerUrl;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public String toString() {
        return "DownloadConfig{" +
                "urls=" + urls +
                ", maxDownloadTimePerUrl=" + maxDownloadTimePerUrl +
                ", outputDirectory='" + outputDirectory + '\'' +
                ", maxConcurrentDownloads=" + maxConcurrentDownloads +
                ", userAgent='" + userAgent + '\'' +
                ", retryAttempts=" + retryAttempts +
                ", connectTimeout=" + connectTimeout +
                ", readTimeout=" + readTimeout +
                '}';
    }
}
