package com.projectlibre1.exchange;

import junit.framework.TestCase;

public class ServerLocalFileImporterTest extends TestCase {
	public void testDirectImportIsExplicitlyUnsupported() {
		ServerLocalFileImporter importer = new ServerLocalFileImporter();

		try {
			importer.importFile();
			fail("Expected UnsupportedOperationException");
		} catch (UnsupportedOperationException expected) {
			assertTrue(expected.getMessage().contains("local file imports"));
		} catch (Exception e) {
			fail("Unexpected exception: " + e);
		}
	}

	public void testDirectExportIsExplicitlyUnsupported() {
		ServerLocalFileImporter importer = new ServerLocalFileImporter();

		try {
			importer.exportFile();
			fail("Expected UnsupportedOperationException");
		} catch (UnsupportedOperationException expected) {
			assertTrue(expected.getMessage().contains("not implemented"));
		} catch (Exception e) {
			fail("Unexpected exception: " + e);
		}
	}
}
