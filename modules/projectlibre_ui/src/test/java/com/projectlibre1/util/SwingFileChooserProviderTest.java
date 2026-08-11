package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.Test;

class SwingFileChooserProviderTest {
	@Test
	void fileDialogsUseTheSwingFallbackSoEscapeCancelsThem() {
		new SwingFileChooserProvider();
		assertEquals("false", System.getProperty(SwingFileChooserProvider.USE_SYSTEM_FILE_CHOOSER_PROPERTY));
	}

	@Test
	void openDialogWithoutSuggestedNameClearsThePreviousSelection() {
		assertNull(SwingFileChooserProvider.initialSelectedFile(null));
		assertEquals(new File("C:\\projects\\plan.pod"),
			SwingFileChooserProvider.initialSelectedFile("C:\\projects\\plan.pod"));
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
