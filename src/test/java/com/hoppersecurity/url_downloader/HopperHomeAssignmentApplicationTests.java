package com.hoppersecurity.url_downloader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class HopperHomeAssignmentApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		// Verify Spring context loads successfully
		assertNotNull(applicationContext, "Application context should not be null");
		
		// Verify core application components are loaded
		assertTrue(applicationContext.containsBean("downloadCommand"), 
				"DownloadCommand bean should be present");
		assertTrue(applicationContext.containsBean("hopperHomeAssignmentApplication"), 
				"Main application bean should be present");
	}

	@Test
	void concurrentUrlDownloaderCanBeCreated() {
		// Test that ConcurrentUrlDownloader can be created with a valid configuration
		DownloadConfig config = new DownloadConfig();
		config.setUrls(Arrays.asList("http://example.com/test.txt"));
		config.setMaxConcurrentDownloads(3);
		config.setOutputDirectory(System.getProperty("java.io.tmpdir"));
		
		// This should not throw an exception
		assertDoesNotThrow(() -> {
			ConcurrentUrlDownloader downloader = new ConcurrentUrlDownloader(config);
			assertNotNull(downloader, "ConcurrentUrlDownloader should be created successfully");
		}, "ConcurrentUrlDownloader should be created without exceptions");
	}

}
