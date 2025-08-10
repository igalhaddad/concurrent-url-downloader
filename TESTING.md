# Testing Documentation

This document describes the comprehensive test suite for the Hopper URL Downloader, featuring 38 test cases with WireMock-based HTTP simulation, complete integration testing, and zero-dependency mocking architecture.

## Test Structure

### Unit Tests

- **`ConcurrentUrlDownloaderTest`**: Basic unit tests for configuration validation and result creation
- **`HopperHomeAssignmentApplicationTests`**: Spring Boot application context tests

### Integration Tests

- **`ConcurrentUrlDownloaderIntegrationTest`**: Comprehensive integration tests using a mock HTTP server
- **`DownloadCommandIntegrationTest`**: CLI command integration tests
- **`IntegrationTestRunner`**: Manual test runner for demonstration purposes

### Mock HTTP Server

- **`IntegrationTestServer`**: WireMock-based HTTP server that simulates various download scenarios with industry-standard mocking

## WireMock HTTP Server Endpoints

The WireMock-based integration test server provides the following endpoints to test different scenarios:

| Endpoint | Description | Response | WireMock Feature |
|----------|-------------|----------|------------------|
| `/success` | Simple successful response | JSON response with status | Basic stub |
| `/file.txt` | Text file with specific filename | Plain text content | Content-Disposition header |
| `/binary` | Binary file response | 1KB binary data | Binary content handling |
| `/large` | Large file response | 1MB text content | Large response simulation |
| `/slow` | Slow response | 2-second delay before response | Fixed delay (2000ms) |
| `/timeout` | Timeout scenario | 10-second delay (causes timeout) | Long delay (10000ms) |
| `/notfound` | 404 error | HTTP 404 response | Error status simulation |
| `/error` | 500 error | HTTP 500 response | Server error simulation |
| `/redirect` | Redirect response | HTTP 302 redirect to `/success` | Redirect handling |
| `/empty` | Zero-byte response | Empty content | Edge case testing |
| `/path/with/special-chars*` | Special characters | Pattern matching | URL pattern matching |

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
# Compile and run the manual test runner
./mvnw compile exec:java -Dexec.mainClass="com.hoppersecurity.url_downloader.IntegrationTestRunner" -Dexec.classpathScope=test

# Or run after building
./mvnw clean package
java -cp "target/classes:target/test-classes:target/dependency/*" com.hoppersecurity.url_downloader.IntegrationTestRunner
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

The test configuration is defined in `src/test/resources/application.properties`:

```properties
# Test configuration for integration tests
spring.profiles.active=test
logging.level.com.hoppersecurity=DEBUG
logging.level.org.apache.http=WARN
logging.level.org.springframework.shell=WARN

# Disable web server for CLI tests
spring.main.web-application-type=none

# Test-specific settings
spring.shell.interactive.enabled=false
```

**Note:** Tests use Spring profiles (`test` profile) to prevent application auto-termination during testing.

### Test Dependencies

The integration tests use the following dependencies:

- **JUnit 5**: Modern testing framework with comprehensive assertions
- **Spring Boot Test**: Integration testing support (with Mockito excluded)
- **WireMock 3.13.1**: Industry-standard HTTP service simulation
- **SLF4J + Logback**: Comprehensive logging for test output
- **Jackson**: JSON processing for test configurations

**Architecture Decision:** No Mockito usage - clean integration testing with real objects and WireMock for HTTP simulation.

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

- ✅ **Concurrent Downloads**: Verifies that downloads happen simultaneously with thread pool management
- ✅ **Error Handling**: Tests various failure scenarios with isolated error handling
- ✅ **Timeout Management**: Validates timeout behavior with configurable timeouts
- ✅ **Retry Logic**: Ensures failed downloads are retried with exponential backoff
- ✅ **File Handling**: Tests different file types, sizes, and edge cases (zero-byte files)
- ✅ **CLI Integration**: Tests the command-line interface with auto-termination
- ✅ **Configuration Validation**: Verifies JSON configuration parsing and validation
- ✅ **Completion Order**: Ensures downloads are logged in completion order with real-time progress
- ✅ **Resource Management**: Validates proper ExecutorService and HttpClient cleanup
- ✅ **Spring Integration**: Tests Spring profile-based behavior and bean configuration
- ✅ **Filename Generation**: Tests filename extraction and special character handling
- ✅ **Directory Creation**: Tests output directory creation and permission handling
- ✅ **User Agent**: Validates custom User-Agent header functionality
- ✅ **Thread Interruption**: Tests graceful handling of thread interruption
- ✅ **WireMock Integration**: Reliable HTTP simulation without network dependencies

## Running Tests in CI/CD

The tests are designed to run reliably in continuous integration environments:

```yaml
# Example GitHub Actions workflow
- name: Run All Tests
  run: |
    ./mvnw clean test
    
- name: Run Specific Test Suites
  run: |
    ./mvnw test -Dtest=ConcurrentUrlDownloaderIntegrationTest
    ./mvnw test -Dtest=DownloadCommandIntegrationTest
    ./mvnw test -Dtest=HopperHomeAssignmentApplicationTests
    
- name: Generate Test Report
  run: |
    ./mvnw surefire-report:report
```

**CI/CD Benefits:**
- **No Network Dependencies**: All tests use WireMock on localhost
- **No Mockito Warnings**: Clean build output without agent loading warnings
- **Deterministic**: WireMock provides consistent, repeatable test behavior
- **Fast Execution**: Optimized test suite completes in under 30 seconds

## Troubleshooting

### Common Issues

1. **Port Conflicts**: WireMock uses dynamic ports to avoid conflicts
2. **File Permissions**: Tests use temporary directories with proper cleanup
3. **Network Issues**: All tests use WireMock on localhost (no external dependencies)
4. **Timeout Issues**: Some tests have intentional timeouts for validation
5. **Memory Issues**: Large file tests (1MB) may require adequate heap space
6. **Thread Cleanup**: Tests ensure proper ExecutorService shutdown

### Debug Mode

Enable debug logging for detailed test output:

```bash
./mvnw test -Dlogging.level.com.hoppersecurity=DEBUG
```

### Test Isolation

Each test is isolated and uses:
- **Separate temporary directories**: JUnit `@TempDir` for clean file operations
- **Independent WireMock instances**: Fresh server for each test class
- **Clean state between tests**: Automatic reset of WireMock stubs
- **Proper resource cleanup**: ExecutorService and HttpClient shutdown
- **Spring test contexts**: Isolated application contexts for integration tests
- **No shared state**: Thread-safe test execution with parallel capability
