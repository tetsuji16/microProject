package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwingFileChooserProviderTest {
	@Test
	void fileDialogsUseTheSwingFallbackSoEscapeCancelsThem() {
		new SwingFileChooserProvider();
		assertEquals("false", System.getProperty(SwingFileChooserProvider.USE_SYSTEM_FILE_CHOOSER_PROPERTY));
	}

	@Test
	void saveAsKeepsTheSourceProjectsSupportedFormat() {
		assertEquals("pod", SwingFileChooserProvider.preferredSaveExtension("original.pod", true));
		assertEquals("xml", SwingFileChooserProvider.preferredSaveExtension("original.xml", true));
		assertEquals("xlsx", SwingFileChooserProvider.preferredSaveExtension("original.xlsx", true));
	}

	@Test
	void saveAsFallsBackToAnEditableFormat() {
		assertEquals("pod", SwingFileChooserProvider.preferredSaveExtension("imported.mpp", true));
		assertEquals("xml", SwingFileChooserProvider.preferredSaveExtension("project.pod", false));
	}
}
