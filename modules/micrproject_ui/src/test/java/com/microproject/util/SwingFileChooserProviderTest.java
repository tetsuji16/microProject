/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
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
	void fileDialogsDefaultToNativeSystemDialogsUnlessDisabled() {
		new SwingFileChooserProvider();
		String override = System.getProperty(SwingFileChooserProvider.USE_SYSTEM_FILE_CHOOSER_PROPERTY);
		assertTrue(override == null || Boolean.parseBoolean(override),
				"the provider must not force the Swing fallback; native dialogs are the default");
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

			assertEquals("ProjectLibre (*.podx)", chooser.getFileFilter().getDescription());
			assertArrayEquals(new String[] { "podx" },
				((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions());
			assertTrue(chooser.isAcceptAllFileFilterUsed());
			assertEquals(8, chooser.getChoosableFileFilters().length);

			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			provider.selectOpenFileFilter(chooser);
			assertEquals("ProjectLibre (*.podx)", chooser.getFileFilter().getDescription());
		} finally {
			Environment.setStandAlone(previousStandalone);
		}
	}

	@Test
	void openKeepsTheSelectedFileNameWhileSaveAsUsesTheDefaultPodxFormat() {
		boolean previousStandalone = Environment.getStandAlone();
		try {
			Environment.setStandAlone(true);
			SwingFileChooserProvider provider = new SwingFileChooserProvider();

			SystemFileChooser openChooser = provider.prepareFileChooser(false, "C:\\projects\\imported.mpp");
			assertEquals(new File("C:\\projects\\imported.mpp"), openChooser.getSelectedFile());
			assertEquals("ProjectLibre (*.podx)", openChooser.getFileFilter().getDescription());

			SystemFileChooser saveChooser = provider.prepareFileChooser(true, "C:\\projects\\imported.mpp");
			assertEquals(new File("C:\\projects\\imported.podx"), saveChooser.getSelectedFile());
			assertEquals("ProjectLibre (*.podx)", saveChooser.getFileFilter().getDescription());
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
			assertArrayEquals(new String[] { "podx", "pod", "xml", "xlsx", "planner", "mpp", "mpx" },
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
		assertEquals("podx", SwingFileChooserProvider.preferredSaveExtension("imported.mpp", true));
		assertEquals("podx", SwingFileChooserProvider.preferredSaveExtension("project.podx", false));
		assertEquals("xml", SwingFileChooserProvider.preferredSaveExtension("project.pod", false));
	}
}
