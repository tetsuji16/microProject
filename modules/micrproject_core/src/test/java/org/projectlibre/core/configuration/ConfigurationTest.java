package org.projectlibre.core.configuration;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ConfigurationTest {
	@Test
	void missingClasspathResourceReturnsNull() {
		Configuration configuration = new Configuration();

		assertNull(configuration.load("missing/projectlibre-configuration.xml", ConfigurationFile.class));
	}
}
