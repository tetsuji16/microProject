package com.projectlibre1.server.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projectlibre1.util.Environment;

class DistributionConverterTest {
	@Test
	void standaloneConverterReturnsAnEmptyResultInsteadOfNull() {
		boolean previous = Environment.getStandAlone();
		Environment.setStandAlone(true);
		try {
			DistributionConverter converter = new DistributionConverter();
			assertNotNull(converter.createDistributionData(null, false));
			assertTrue(converter.createDistributionData(null, false).isEmpty());
		} finally {
			Environment.setStandAlone(previous);
		}
	}
}
