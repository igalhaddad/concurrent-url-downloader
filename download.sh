#!/bin/bash
# Hopper URL Downloader - Linux/Mac Shell Script
# Usage: ./download.sh [config-file]
# Example: ./download.sh src/main/resources/example-config.json

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_error() {
    echo -e "${RED}Error: $1${NC}" >&2
}

print_success() {
    echo -e "${GREEN}$1${NC}"
}

print_info() {
    echo -e "${YELLOW}$1${NC}"
}

# Check if Java is available
if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    echo "Please install Java 21 or higher"
    exit 1
fi

# Check Java version (basic check)
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ] 2>/dev/null; then
    print_error "Java 21 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

# Check if JAR file exists
JAR_FILE="target/url-downloader-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    print_error "JAR file not found: $JAR_FILE"
    echo "Please run 'mvn clean package' first to build the application"
    exit 1
fi

# Set default config file if not provided
CONFIG_FILE="${1:-src/main/resources/example-config.json}"

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    print_error "Configuration file not found: $CONFIG_FILE"
    echo "Please provide a valid JSON configuration file"
    echo "Usage: ./download.sh [config-file]"
    exit 1
fi

print_info "Starting Hopper URL Downloader..."
print_info "Using configuration: $CONFIG_FILE"
echo

# Run the application
java -jar "$JAR_FILE" download --config "$CONFIG_FILE"
