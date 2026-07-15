package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwingFileChooserProviderTest {
	@Test
	void fileDialogsUseTheSwingFallbackSoEscapeCancelsThem() {
		new SwingFileChooserProvider();
		assertEquals("false", System.getProperty(SwingFileChooserProvider.USE_SYSTEM_FILE_CHOOSER_PROPERTY));
	}
}
