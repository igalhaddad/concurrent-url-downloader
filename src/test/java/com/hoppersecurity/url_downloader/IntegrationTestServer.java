package com.hoppersecurity.url_downloader;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class IntegrationTestServer {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestServer.class);
    
    private WireMockServer wireMockServer;
    private int port;
    
    public void start() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        
        // Setup all the mock endpoints
        setupMockEndpoints();
        
        wireMockServer.start();
        port = wireMockServer.port();
        logger.info("Test server started on port {}", port);
    }
    
    public void stop() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            logger.info("Test server stopped");
        }
    }
    
    public int getPort() {
        return port;
    }
    
    public String getBaseUrl() {
        return "http://localhost:" + port;
    }
    
    public int getRequestCount() {
        return wireMockServer.getAllServeEvents().size();
    }
    
    public void resetRequestCount() {
        wireMockServer.resetAll();
        setupMockEndpoints(); // Re-setup endpoints after reset
    }
    
    private void setupMockEndpoints() {
        // Simple success response
        wireMockServer.stubFor(get(urlEqualTo("/success"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"success\", \"message\": \"Hello World\"}")));
        
        // Large file response (1MB)
        wireMockServer.stubFor(get(urlEqualTo("/large"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody(generateLargeContent(1024 * 1024))));
        
        // Slow response (2 second delay)
        wireMockServer.stubFor(get(urlEqualTo("/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Slow response")
                        .withFixedDelay(2000)));
        
        // 404 error
        wireMockServer.stubFor(get(urlEqualTo("/notfound"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));
        
        // 500 error
        wireMockServer.stubFor(get(urlEqualTo("/error"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
        
        // Timeout response (10 second delay to cause timeout)
        wireMockServer.stubFor(get(urlEqualTo("/timeout"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("This should timeout")
                        .withFixedDelay(10000)));
        
        // Empty response (zero bytes)
        wireMockServer.stubFor(get(urlEqualTo("/empty"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("")));
        
        // Redirect response
        wireMockServer.stubFor(get(urlEqualTo("/redirect"))
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader("Location", "/success")));
        
        // Binary file response (1KB)
        wireMockServer.stubFor(get(urlEqualTo("/binary"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody(generateBinaryData(1024))));
        
        // File with specific filename
        wireMockServer.stubFor(get(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("Content-Disposition", "attachment; filename=\"test-file.txt\"")
                        .withBody("This is a text file")));
        
        // Special characters in path (for filename generation tests)
        wireMockServer.stubFor(get(urlMatching("/path/with/special-chars.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("File with special characters in path")));
    }
    
    private String generateLargeContent(int size) {
        return "A".repeat(Math.max(0, size));
    }
    
    private byte[] generateBinaryData(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }
}
