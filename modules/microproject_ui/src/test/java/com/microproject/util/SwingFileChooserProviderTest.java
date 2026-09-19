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
import java.util.List;

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

			assertTrue(chooser.isMultiSelectionEnabled(), "open dialogs must allow selecting multiple projects");
			assertTrue(chooser.getFileFilter().getDescription().contains("*.mpo"));
			assertArrayEquals(new String[] { "mpo" },
				((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions());
			assertTrue(chooser.isAcceptAllFileFilterUsed());
			assertEquals(8, chooser.getChoosableFileFilters().length);
			assertTrue(findFilter(chooser, "pod").getDescription().contains("*.pod"));

			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			provider.selectOpenFileFilter(chooser);
			assertTrue(chooser.getFileFilter().getDescription().contains("*.mpo"));
		} finally {
			Environment.setStandAlone(previousStandalone);
		}
	}

	private static FileNameExtensionFilter findFilter(SystemFileChooser chooser, String extension) {
		return java.util.Arrays.stream(chooser.getChoosableFileFilters())
			.filter(FileNameExtensionFilter.class::isInstance)
			.map(FileNameExtensionFilter.class::cast)
			.filter(filter -> java.util.Arrays.asList(filter.getExtensions()).contains(extension))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Missing filter for *." + extension));
	}

	@Test
	void openKeepsTheSelectedFileNameWhileSaveAsUsesTheDefaultMpoFormat() {
		boolean previousStandalone = Environment.getStandAlone();
		try {
			Environment.setStandAlone(true);
			SwingFileChooserProvider provider = new SwingFileChooserProvider();

			SystemFileChooser openChooser = provider.prepareFileChooser(false, "C:\\projects\\imported.mpp");
			assertEquals(new File("C:\\projects\\imported.mpp"), openChooser.getSelectedFile());
			assertTrue(openChooser.getFileFilter().getDescription().contains("*.mpo"));

			SystemFileChooser saveChooser = provider.prepareFileChooser(true, "C:\\projects\\imported.mpp");
			assertEquals(new File("C:\\projects\\imported.mpo"), saveChooser.getSelectedFile());
			assertTrue(!saveChooser.isMultiSelectionEnabled(), "save dialogs must remain single-selection");
			assertTrue(saveChooser.getFileFilter().getDescription().contains("*.mpo"));
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
			assertTrue(chooser.isMultiSelectionEnabled());
			assertArrayEquals(new String[] { "mpo", "pod", "xml", "xlsx", "planner", "mpp", "mpx" },
				((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions());
		} finally {
			Environment.setStandAlone(previousStandalone);
		}
	}

	@Test
	void saveAsKeepsTheSourceProjectsSupportedFormat() {
		assertEquals("pod", SwingFileChooserProvider.preferredSaveExtension("original.pod", true));
		assertEquals("pod", SwingFileChooserProvider.preferredSaveExtension("original.POD", true));
		assertEquals("xml", SwingFileChooserProvider.preferredSaveExtension("original.xml", true));
		assertEquals("xlsx", SwingFileChooserProvider.preferredSaveExtension("original.xlsx", true));
	}

	@Test
	void hostedSaveConvertsLegacyPodToXmlRegardlessOfExtensionCase() {
		assertEquals("C:\\projects\\original.xml",
			SwingFileChooserProvider.normalizeHostedSelectedFileName("C:\\projects\\original.POD", false));
		assertEquals("C:\\projects\\original.POD",
			SwingFileChooserProvider.normalizeHostedSelectedFileName("C:\\projects\\original.POD", true));
		assertNull(SwingFileChooserProvider.normalizeHostedSelectedFileName(null, false));
	}

	@Test
	void saveAsFallsBackToAnEditableFormat() {
		assertEquals("mpo", SwingFileChooserProvider.preferredSaveExtension("imported.mpp", true));
		assertEquals("mpo", SwingFileChooserProvider.preferredSaveExtension("project.mpo", false));
		assertEquals("xml", SwingFileChooserProvider.preferredSaveExtension("project.pod", false));
	}

	@Test
	void openSelectionPreservesEveryNonNullFileInOrder() {
		assertEquals(List.of("C:\\projects\\first.mpo", "C:\\projects\\second.mpp"),
			SwingFileChooserProvider.selectedFileNames(new File[] {
				new File("C:\\projects\\first.mpo"), null, new File("C:\\projects\\second.mpp")
			}));
		assertTrue(SwingFileChooserProvider.selectedFileNames(new File[0]).isEmpty());
	}
}
