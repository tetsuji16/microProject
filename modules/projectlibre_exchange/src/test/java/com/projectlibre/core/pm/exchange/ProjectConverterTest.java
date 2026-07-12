package com.projectlibre.core.pm.exchange;

import junit.framework.TestCase;

public class ProjectConverterTest extends TestCase {
	public void testUnsupportedFormatFailsExplicitly() throws Exception {
		ProjectConverter converter = ProjectConverter.getInstance();

		try {
			converter.convert("bogus", ProjectConverter.Type.PROJECT, true, null, null, null);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("Unsupported import/export format"));
		}
	}

	public void testNullFormatFailsExplicitly() throws Exception {
		ProjectConverter converter = ProjectConverter.getInstance();

		try {
			converter.convert(null, ProjectConverter.Type.PROJECT, true, null, null, null);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("Unsupported import/export format"));
		}
	}
}
