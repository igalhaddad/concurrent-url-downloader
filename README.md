# Hopper URL Downloader

A CLI-based concurrent URL downloader built with Java and Spring Shell. This application allows you to download multiple URLs concurrently with configurable options for timeouts, retries, and concurrency limits.

## Features

- **Concurrent Downloads**: Download multiple URLs simultaneously to minimize total execution time
- **Configurable Timeouts**: Set maximum download time per URL and connection timeouts
- **Retry Logic**: Automatic retry with exponential backoff for failed downloads
- **Completion Order Logging**: Logs download completions in the order they finish, not the order they start
- **Error Handling**: Individual download failures don't affect other downloads
- **Comprehensive Logging**: Detailed logging of download progress, errors, and system messages
- **JSON Configuration**: Simple JSON-based configuration file
- **Cross-platform**: Runs on macOS, Linux, and Windows

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Building the Application

```bash
mvn clean package
```

This will create an executable JAR file in the `target` directory.

## Usage

### Running the Application

```bash
java -jar target/url-downloader-0.0.1-SNAPSHOT.jar
```

### Download Command

Once the application starts, use the `download` command with a JSON configuration file:

```bash
download --config config.json
```

### Configuration File Format

The configuration file should be a JSON file with the following structure:

```json
{
  "urls": [
    "https://example.com/file1.txt",
    "https://example.com/file2.jpg",
    "https://example.com/file3.pdf"
  ],
  "maxDownloadTimePerUrl": 30,
  "outputDirectory": "./downloads",
  "maxConcurrentDownloads": 3,
  "userAgent": "Hopper-URL-Downloader/1.0",
  "retryAttempts": 3,
  "connectTimeout": 10,
  "readTimeout": 30
}
```

### Configuration Options

| Option | Type | Required | Default | Description |
|--------|------|----------|---------|-------------|
| `urls` | Array of strings | Yes | - | List of URLs to download |
| `maxDownloadTimePerUrl` | Integer | Yes | - | Maximum time (seconds) allowed per download |
| `outputDirectory` | String | Yes | - | Directory where downloaded files will be saved |
| `maxConcurrentDownloads` | Integer | Yes | - | Maximum number of concurrent downloads (1-100) |
| `userAgent` | String | No | "Hopper-URL-Downloader/1.0" | User-Agent header for HTTP requests |
| `retryAttempts` | Integer | No | 3 | Number of retry attempts for failed downloads |
| `connectTimeout` | Integer | No | 30 | Connection timeout in seconds |
| `readTimeout` | Integer | No | 60 | Read timeout in seconds |

## Example Usage

1. Create a configuration file `my-config.json`:

```json
{
  "urls": [
    "https://httpbin.org/get",
    "https://httpbin.org/json",
    "https://httpbin.org/xml"
  ],
  "maxDownloadTimePerUrl": 30,
  "outputDirectory": "./my-downloads",
  "maxConcurrentDownloads": 2
}
```

2. Run the downloader:

```bash
java -jar target/url-downloader-0.0.1-SNAPSHOT.jar
```

3. Execute the download command:

```bash
download --config my-config.json
```

## Output

The application will:

1. Create the output directory if it doesn't exist
2. Download files concurrently with the specified configuration
3. Log each download completion in real-time as they finish
4. Display a summary of successful and failed downloads
5. Show total execution time

### Sample Output

```
✓ Downloaded https://httpbin.org/get to 1703123456789_get (1234 bytes) in 245ms
✓ Downloaded https://httpbin.org/json to 1703123456790_json (567 bytes) in 189ms
✗ Failed to download https://httpbin.org/xml: HTTP 404: Not Found (took 156ms)

=== Download Summary ===
Total URLs: 3
Successful: 2
Failed: 1
Total time: 456ms
Output directory: ./my-downloads

=== Failed Downloads ===
- https://httpbin.org/xml: HTTP 404: Not Found
```

## File Naming

Downloaded files are named using the following pattern:
- `{timestamp}_{original_filename}`
- If the original filename cannot be extracted from the URL, it defaults to `{timestamp}_download`
- Special characters in filenames are replaced with underscores

## Error Handling

- Individual download failures don't stop other downloads
- Failed downloads are retried up to the specified number of attempts
- All errors are logged with detailed information
- The application continues even if some downloads fail

## Performance Considerations

- The total download time will be significantly less than the sum of individual download times due to concurrency
- Memory usage scales with the number of concurrent downloads
- Network bandwidth is shared among concurrent downloads
- Consider your system's capabilities when setting `maxConcurrentDownloads`

## Logging

The application uses SLF4J for logging. Log levels can be configured by setting the `logging.level` property:

```bash
java -jar target/url-downloader-0.0.1-SNAPSHOT.jar --logging.level.com.hoppersecurity=DEBUG
```

## Troubleshooting

### Common Issues

1. **Permission Denied**: Ensure the output directory is writable
2. **Network Timeouts**: Increase `connectTimeout` and `readTimeout` values
3. **SSL Errors**: Set `validateSSL` to `false` for self-signed certificates (use with caution)
4. **Memory Issues**: Reduce `maxConcurrentDownloads` if experiencing out-of-memory errors

### Debug Mode

Enable debug logging to see detailed information about each download:

```bash
java -jar target/url-downloader-0.0.1-SNAPSHOT.jar --logging.level.com.hoppersecurity=DEBUG
```

## License

This project is part of the Hopper Home Assignment.
