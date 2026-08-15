package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter;

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
	void openDialogDefaultsToProjectLibreAndKeepsOtherFormatsAvailable() {
		boolean previousStandalone = Environment.getStandAlone();
		try {
			Environment.setStandAlone(true);
			SwingFileChooserProvider provider = new SwingFileChooserProvider();
			SystemFileChooser chooser = new SystemFileChooser();
			provider.configureFileChooser(chooser, false);
			provider.selectOpenFileFilter(chooser);

			assertEquals("ProjectLibre (*.pod)", chooser.getFileFilter().getDescription());
			assertArrayEquals(new String[] { "pod" },
				((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions());
			assertTrue(chooser.isAcceptAllFileFilterUsed());
			assertEquals(7, chooser.getChoosableFileFilters().length);

			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			provider.selectOpenFileFilter(chooser);
			assertEquals("ProjectLibre (*.pod)", chooser.getFileFilter().getDescription());
		} finally {
			Environment.setStandAlone(previousStandalone);
		}
	}

	@Test
	void openKeepsTheSelectedFileNameWhileSaveAsUsesTheDefaultPodFormat() {
		boolean previousStandalone = Environment.getStandAlone();
		try {
			Environment.setStandAlone(true);
			SwingFileChooserProvider provider = new SwingFileChooserProvider();

			SystemFileChooser openChooser = provider.prepareFileChooser(false, "C:\\projects\\imported.mpp");
			assertEquals(new File("C:\\projects\\imported.mpp"), openChooser.getSelectedFile());
			assertEquals("ProjectLibre (*.pod)", openChooser.getFileFilter().getDescription());

			SystemFileChooser saveChooser = provider.prepareFileChooser(true, "C:\\projects\\imported.mpp");
			assertEquals(new File("C:\\projects\\imported.pod"), saveChooser.getSelectedFile());
			assertEquals("ProjectLibre (*.pod)", saveChooser.getFileFilter().getDescription());
		} finally {
			Environment.setStandAlone(previousStandalone);
		}
	}

	@Test
	void hostedOpenDialogDefaultsToAllSupportedProjectFormats() {
		boolean previousStandalone = Environment.getStandAlone();
		try {
			Environment.setStandAlone(false);
			SwingFileChooserProvider provider = new SwingFileChooserProvider();
			SystemFileChooser chooser = new SystemFileChooser();
			provider.configureFileChooser(chooser, false);
			provider.selectOpenFileFilter(chooser);

			assertEquals("Projects", chooser.getFileFilter().getDescription());
			assertArrayEquals(new String[] { "pod", "xml", "xlsx", "planner", "mpp", "mpx" },
				((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions());
		} finally {
			Environment.setStandAlone(previousStandalone);
		}
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
