# Testing Documentation

This document describes the comprehensive test suite for the Hopper URL Downloader, including unit tests, integration tests, and a mock HTTP server for testing various download scenarios.

## Test Structure

### Unit Tests

- **`ConcurrentUrlDownloaderTest`**: Basic unit tests for configuration validation and result creation
- **`HopperHomeAssignmentApplicationTests`**: Spring Boot application context tests

### Integration Tests

- **`ConcurrentUrlDownloaderIntegrationTest`**: Comprehensive integration tests using a mock HTTP server
- **`DownloadCommandIntegrationTest`**: CLI command integration tests
- **`IntegrationTestRunner`**: Manual test runner for demonstration purposes

### Mock HTTP Server

- **`IntegrationTestServer`**: A mock HTTP server that simulates various download scenarios

## Mock HTTP Server Endpoints

The integration test server provides the following endpoints to test different scenarios:

| Endpoint | Description | Response |
|----------|-------------|----------|
| `/success` | Simple successful response | JSON response with status |
| `/file.txt` | Text file with specific filename | Plain text content |
| `/binary` | Binary file response | 1KB binary data |
| `/large` | Large file response | 1MB text content |
| `/slow` | Slow response | 2-second delay before response |
| `/timeout` | Timeout scenario | Never responds (causes timeout) |
| `/notfound` | 404 error | HTTP 404 response |
| `/error` | 500 error | HTTP 500 response |
| `/redirect` | Redirect response | HTTP 302 redirect to `/success` |

## Running Tests

### Unit Tests

```bash
# Run all unit tests
./mvnw test

# Run specific unit test class
./mvnw test -Dtest=ConcurrentUrlDownloaderTest

# Run specific test method
./mvnw test -Dtest=ConcurrentUrlDownloaderTest#testDownloadConfigValidation
```

### Integration Tests

```bash
# Run integration tests
./mvnw test -Dtest=ConcurrentUrlDownloaderIntegrationTest

# Run CLI integration tests
./mvnw test -Dtest=DownloadCommandIntegrationTest
```

### Manual Integration Test Runner

```bash
# Run the manual test runner
./mvnw exec:java -Dexec.mainClass="com.hoppersecurity.url_downloader.IntegrationTestRunner"
```

## Test Scenarios

### 1. Successful Downloads

**Test**: `testSuccessfulDownloads`
**Purpose**: Verify that successful downloads work correctly
**Endpoints**: `/success`, `/file.txt`, `/binary`
**Expected**: All downloads succeed, files are created with correct content

### 2. Mixed Success and Failure

**Test**: `testMixedSuccessAndFailure`
**Purpose**: Verify that individual failures don't affect other downloads
**Endpoints**: `/success`, `/notfound`, `/error`, `/file.txt`
**Expected**: 2 successful, 2 failed downloads

### 3. Concurrent Downloads

**Test**: `testConcurrentDownloads`
**Purpose**: Verify that downloads happen concurrently and total time is reduced
**Endpoints**: `/slow`, `/success`, `/file.txt`, `/binary`, `/large`
**Expected**: Total time < sum of individual times due to concurrency

### 4. Timeout Handling

**Test**: `testTimeoutHandling`
**Purpose**: Verify that timeouts are handled correctly
**Endpoints**: `/timeout`, `/success`
**Expected**: Timeout download fails, successful download completes

### 5. Retry Logic

**Test**: `testRetryLogic`
**Purpose**: Verify that failed downloads are retried
**Endpoints**: `/error`, `/success`
**Expected**: Error endpoint is retried multiple times, success endpoint completes

### 6. Large File Downloads

**Test**: `testLargeFileDownload`
**Purpose**: Verify that large files are downloaded correctly
**Endpoints**: `/large`
**Expected**: 1MB file downloaded with correct size

### 7. Binary File Downloads

**Test**: `testBinaryFileDownload`
**Purpose**: Verify that binary files are handled correctly
**Endpoints**: `/binary`
**Expected**: 1KB binary file downloaded correctly

### 8. Completion Order Logging

**Test**: `testCompletionOrderLogging`
**Purpose**: Verify that downloads are logged in completion order, not start order
**Endpoints**: `/slow`, `/success`, `/file.txt`
**Expected**: Fast downloads logged before slow ones

### 9. Empty URL List

**Test**: `testEmptyUrlList`
**Purpose**: Verify handling of empty URL lists
**Expected**: No downloads attempted, no errors

### 10. Invalid URLs

**Test**: `testInvalidUrls`
**Purpose**: Verify handling of invalid URLs
**Endpoints**: Invalid domain, `/success`
**Expected**: Invalid URL fails, valid URL succeeds

## CLI Command Tests

### 1. Valid Configuration

**Test**: `testDownloadCommandWithValidConfig`
**Purpose**: Verify CLI command with valid JSON configuration
**Expected**: Successful execution with proper output

### 2. Mixed Results

**Test**: `testDownloadCommandWithMixedResults`
**Purpose**: Verify CLI command with mixed success/failure scenarios
**Expected**: Proper error reporting and summary

### 3. Invalid Configuration File

**Test**: `testDownloadCommandWithInvalidConfigFile`
**Purpose**: Verify CLI command with non-existent config file
**Expected**: Appropriate error message

### 4. Invalid JSON

**Test**: `testDownloadCommandWithInvalidJson`
**Purpose**: Verify CLI command with malformed JSON
**Expected**: JSON parsing error message

### 5. Empty URL List

**Test**: `testDownloadCommandWithEmptyUrlList`
**Purpose**: Verify CLI command with empty URL list
**Expected**: Validation error message

### 6. Invalid Configuration Values

**Test**: `testDownloadCommandWithInvalidConfiguration`
**Purpose**: Verify CLI command with invalid configuration values
**Expected**: Validation error message

### 7. Timeout Handling

**Test**: `testDownloadCommandWithTimeout`
**Purpose**: Verify CLI command with timeout scenarios
**Expected**: Proper timeout handling and reporting

### 8. Retry Logic

**Test**: `testDownloadCommandWithRetryLogic`
**Purpose**: Verify CLI command with retry scenarios
**Expected**: Proper retry behavior and reporting

## Test Configuration

### Test Properties

The test configuration is defined in `src/test/resources/application-test.properties`:

```properties
# Test configuration for integration tests
logging.level.com.hoppersecurity=DEBUG
logging.level.org.apache.http=WARN
logging.level.org.springframework.shell=WARN

# Disable web server for CLI tests
spring.main.web-application-type=none

# Test-specific settings
spring.shell.interactive.enabled=false
```

### Test Dependencies

The integration tests use the following dependencies:

- **JUnit 5**: Testing framework
- **Spring Boot Test**: Integration testing support
- **SLF4J**: Logging for test output
- **Java HTTP Server**: Built-in mock server

## Test Output

### Successful Test Run

```
=== Test 1: Basic Successful Downloads ===
Results: 3 successful, 0 failed
  ✓ Downloaded http://localhost:12345/success to 1703123456789_success (1234 bytes) in 245ms
  ✓ Downloaded http://localhost:12345/file.txt to 1703123456790_file.txt (567 bytes) in 189ms
  ✓ Downloaded http://localhost:12345/binary to 1703123456791_binary (1024 bytes) in 156ms

=== Test 2: Mixed Success and Failure ===
Results: 2 successful, 2 failed
  ✓ Downloaded http://localhost:12345/success to 1703123456792_success (1234 bytes) in 245ms
  ✗ Failed to download http://localhost:12345/notfound: HTTP 404: Not Found (took 156ms)
  ✗ Failed to download http://localhost:12345/error: HTTP 500: Internal Server Error (took 145ms)
  ✓ Downloaded http://localhost:12345/file.txt to 1703123456793_file.txt (567 bytes) in 189ms
```

### Test Summary

The integration test suite provides comprehensive coverage of:

- ✅ **Concurrent Downloads**: Verifies that downloads happen simultaneously
- ✅ **Error Handling**: Tests various failure scenarios
- ✅ **Timeout Management**: Validates timeout behavior
- ✅ **Retry Logic**: Ensures failed downloads are retried
- ✅ **File Handling**: Tests different file types and sizes
- ✅ **CLI Integration**: Tests the command-line interface
- ✅ **Configuration Validation**: Verifies JSON configuration parsing
- ✅ **Completion Order**: Ensures downloads are logged in completion order
- ✅ **Resource Management**: Validates proper cleanup and shutdown

## Running Tests in CI/CD

The tests are designed to run in continuous integration environments:

```yaml
# Example GitHub Actions workflow
- name: Run Tests
  run: |
    ./mvnw clean test
    ./mvnw test -Dtest=ConcurrentUrlDownloaderIntegrationTest
    ./mvnw test -Dtest=DownloadCommandIntegrationTest
```

## Troubleshooting

### Common Issues

1. **Port Conflicts**: The mock server uses random ports to avoid conflicts
2. **File Permissions**: Tests use temporary directories for file operations
3. **Network Issues**: All tests use localhost to avoid network dependencies
4. **Timeout Issues**: Some tests have intentional timeouts for validation

### Debug Mode

Enable debug logging for detailed test output:

```bash
./mvnw test -Dlogging.level.com.hoppersecurity=DEBUG
```

### Test Isolation

Each test is isolated and uses:
- Separate temporary directories
- Independent mock server instances
- Clean state between tests
- Proper resource cleanup
