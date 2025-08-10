# Hopper URL Downloader

A high-performance CLI-based concurrent URL downloader built with Java 21 and Spring Shell. This application downloads multiple URLs simultaneously with advanced features like automatic retry, real-time progress tracking, and intelligent error handling.

## Features

- **Concurrent Downloads**: Download multiple URLs simultaneously with configurable thread pools
- **Auto-Termination**: Application automatically exits after downloads complete (no Ctrl+C needed)
- **Real-Time Progress**: Live progress tracking with ✓/✗ symbols and timing information
- **Smart Retry Logic**: Automatic retry with exponential backoff for failed downloads
- **Completion Order Logging**: Shows downloads as they complete, not in start order
- **Robust Error Handling**: Individual failures don't affect other downloads
- **Platform Scripts**: Includes Windows (.bat) and Unix (.sh) convenience scripts
- **Comprehensive Testing**: Full integration test suite with WireMock-based HTTP server
- **Spring Profile Support**: Different behavior for production vs test environments
- **JSON Configuration**: Simple, flexible configuration format
- **Cross-platform**: Runs on Windows, macOS, and Linux

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Building the Application

```bash
# Build the application
mvn clean package

# Run tests (includes integration tests with WireMock)
mvn test
```

This creates an executable JAR file in the `target` directory and runs the comprehensive test suite.

## Usage

### Quick Start with Platform Scripts

**Windows:**
```cmd
# Use the provided batch script
download.bat [config-file.json]

# Or use the example config
download.bat
```

**Linux/macOS:**
```bash
# Use the provided shell script
./download.sh [config-file.json]

# Or use the example config
./download.sh
```

### Manual Execution

```bash
# Start the application
java -jar target/url-downloader-0.0.1-SNAPSHOT.jar

# Execute download command
download --config config.json
```

**Note:** The application automatically terminates after downloads complete - no need to press Ctrl+C!

### Configuration File Format

The configuration file should be a JSON file with the following structure:

```json
{
  "urls": [
    "https://httpbin.org/get",
    "https://jsonplaceholder.typicode.com/posts/1",
    "https://httpbin.org/json"
  ],
  "maxDownloadTimePerUrl": 30,
  "outputDirectory": "./downloads",
  "maxConcurrentDownloads": 3,
  "userAgent": "Hopper-URL-Downloader/1.0",
  "retryAttempts": 3,
  "connectTimeout": 30,
  "readTimeout": 60
}
```

**Example Configuration:** An `example-config.json` file is included in `src/main/resources/` for quick testing.

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

### Option 1: Using Platform Scripts (Recommended)

**Windows:**
```cmd
# Quick start with example config
download.bat

# Or with custom config
download.bat my-config.json
```

**Linux/macOS:**
```bash
# Quick start with example config
./download.sh

# Or with custom config
./download.sh my-config.json
```

### Option 2: Manual Execution

1. Create a configuration file `my-config.json`:

```json
{
  "urls": [
    "https://httpbin.org/get",
    "https://httpbin.org/json",
    "https://jsonplaceholder.typicode.com/posts/1"
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
2025-01-10T15:30:45.123  INFO --- Starting concurrent download of 3 URLs with max 2 concurrent downloads
2025-01-10T15:30:45.124  INFO --- Created output directory: ./my-downloads
✓ Downloaded https://httpbin.org/json to 1704901845124_json (429 bytes) in 187ms
✓ Downloaded https://httpbin.org/get to 1704901845125_get (312 bytes) in 203ms
✓ Downloaded https://jsonplaceholder.typicode.com/posts/1 to 1704901845330_1 (292 bytes) in 156ms
2025-01-10T15:30:45.489  INFO --- All downloads completed in 365ms. Successful: 3, Failed: 0

=== Download Summary ===
Total URLs: 3
Successful: 3
Failed: 0
Total time: 365ms
Output directory: ./my-downloads

2025-01-10T15:30:46.489  INFO --- Terminating application...
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

- **Concurrency Benefits**: Total download time is significantly less than sequential downloads
- **Memory Efficiency**: Optimized thread pool management with proper resource cleanup
- **Network Optimization**: Intelligent connection pooling and timeout handling
- **System Resources**: Consider your system's capabilities when setting `maxConcurrentDownloads` (1-100)
- **Auto-Termination**: Application exits cleanly after completion, freeing all resources

## Logging

The application uses SLF4J with Logback for comprehensive logging:

- **Real-time Progress**: Live download status with ✓/✗ symbols
- **Detailed Timing**: Individual and total download times
- **Error Details**: Comprehensive error messages and retry information
- **Debug Mode**: Enable detailed logging for troubleshooting

```bash
# Enable debug logging
java -jar target/url-downloader-0.0.1-SNAPSHOT.jar --logging.level.com.hoppersecurity=DEBUG

# Or with platform scripts
download.bat my-config.json --logging.level.com.hoppersecurity=DEBUG
```

## Testing

The project includes comprehensive testing:

```bash
# Run all tests (unit + integration)
mvn test

# Run integration tests standalone
mvn test -Dtest=*IntegrationTest
```

**Test Infrastructure:**
- **WireMock Integration**: HTTP server simulation for reliable testing
- **38 Test Cases**: Covering concurrency, error handling, timeouts, and edge cases
- **No Mockito**: Clean test architecture without mocking frameworks
- **Real Scenarios**: Tests mirror actual usage patterns

## Troubleshooting

### Common Issues

1. **Permission Denied**: Ensure the output directory is writable
2. **Network Timeouts**: Increase `connectTimeout` and `readTimeout` values
3. **Memory Issues**: Reduce `maxConcurrentDownloads` if experiencing out-of-memory errors
4. **Application Hanging**: The app auto-terminates; if it doesn't, check for configuration errors

### Debug Mode

Enable debug logging to see detailed information:

```bash
# Manual execution
java -jar target/url-downloader-0.0.1-SNAPSHOT.jar --logging.level.com.hoppersecurity=DEBUG

# With platform scripts
./download.sh my-config.json --logging.level.com.hoppersecurity=DEBUG
```

## Technical Details

- **Java Version**: 21 (LTS)
- **Framework**: Spring Boot 3.5.4 + Spring Shell 3.4.1
- **HTTP Client**: Apache HttpClient 5
- **JSON Processing**: Jackson
- **Testing**: JUnit 5 + WireMock 3.13.1
- **Build Tool**: Maven 3.6+
- **Architecture**: Clean separation of concerns with comprehensive error handling

## License

This project is part of the Hopper Home Assignment.
