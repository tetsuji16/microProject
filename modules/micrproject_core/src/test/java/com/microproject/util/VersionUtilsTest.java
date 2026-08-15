package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VersionUtilsTest {
	@Test
	void reportsTheGradleReleaseVersionFromTheProcessedResource() {
		assertEquals(System.getProperty("projectlibre.test.releaseVersion"), VersionUtils.getVersion());
	}
}
