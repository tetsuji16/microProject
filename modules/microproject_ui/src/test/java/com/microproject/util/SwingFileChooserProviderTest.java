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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter;

class SwingFileChooserProviderTest {
	@TempDir
	Path userHome;

	@Test
	void initialDirectoryKeepsTheUsersLastDirectory() throws Exception {
		Path canonical = Files.createDirectories(userHome.resolve("microProject"));
		Path lastDirectory = Files.createDirectories(userHome.resolve("last-selected"));

		assertEquals(lastDirectory.toFile(),
			SwingFileChooserProvider.initialProjectDirectory(userHome.toFile(), lastDirectory.toString()));
		assertTrue(Files.isDirectory(canonical));
	}

	@Test
	void initialDirectoryPrefersExistingCanonicalFolderToLegacyFolder() throws Exception {
		Path canonical = Files.createDirectories(userHome.resolve("microProject"));
		Files.createDirectories(userHome.resolve("ProjectLibre"));

		assertEquals(canonical.toFile(), SwingFileChooserProvider.initialProjectDirectory(userHome.toFile(), null));
	}

	@Test
	void initialDirectoryUsesExistingLegacyFolderAsReadOnlyFallback() throws Exception {
		Path legacy = Files.createDirectories(userHome.resolve("ProjectLibre"));

		assertEquals(legacy.toFile(), SwingFileChooserProvider.initialProjectDirectory(userHome.toFile(), null));
	}

	@Test
	void initialDirectoryFallsBackToHomeWithoutCreatingApplicationFolders() {
		assertEquals(userHome.toFile(), SwingFileChooserProvider.initialProjectDirectory(userHome.toFile(), null));
		assertFalse(Files.exists(userHome.resolve("microProject")));
		assertFalse(Files.exists(userHome.resolve("ProjectLibre")));
	}

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
	void specializedChooserUsesOnePurposeFilterAndNormalizesItsSaveExtension() {
		SwingFileChooserProvider provider = new SwingFileChooserProvider();
		SystemFileChooser chooser = new SystemFileChooser();
		UiServices.FileChooserOptions options = new UiServices.FileChooserOptions(true, "ccpm-report.csv",
			"Export CCPM report", "CSV (*.csv)", "csv");
		provider.configureSpecializedChooser(chooser, options);
		assertTrue(!chooser.isMultiSelectionEnabled(), "specialized file selection must select one file");
		assertEquals(2, chooser.getChoosableFileFilters().length, "only the purpose filter and All Files should be shown");
		assertTrue(chooser.getFileFilter().getDescription().contains("*.csv"));
		SystemFileChooser saveChooser = provider.prepareFileChooser(true, options.selectedFileName());
		provider.configureSpecializedChooser(saveChooser, options);
		saveChooser.setSelectedFile(SwingFileChooserProvider.initialSelectedFile(options.selectedFileName()));
		assertEquals(new File("ccpm-report.csv"), saveChooser.getSelectedFile(),
			"specialized save must keep its report filename instead of the project's default format");
		assertEquals("C:\\reports\\ccpm-report.csv",
			provider.normalizeSpecializedSaveFileName("C:\\reports\\ccpm-report", "csv"));
		assertEquals("C:\\reports\\ccpm-report.csv",
			provider.normalizeSpecializedSaveFileName("C:\\reports\\ccpm-report.csv", "csv"));
	}

	@Test
	void openDialogDefaultsToMicroProjectAndKeepsOtherFormatsAvailable() {
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
	}

	@Test
	void podFilterRemainsAvailableForOpenAndIsAbsentFromSave() {
		SwingFileChooserProvider provider = new SwingFileChooserProvider();
		SystemFileChooser openChooser = new SystemFileChooser();
		provider.configureFileChooser(openChooser, false);
		assertTrue(findFilter(openChooser, "pod").getDescription().contains("*.pod"));

		SystemFileChooser saveChooser = new SystemFileChooser();
		provider.configureFileChooser(saveChooser, true);
		assertFalse(java.util.Arrays.stream(saveChooser.getChoosableFileFilters())
			.filter(FileNameExtensionFilter.class::isInstance)
			.map(FileNameExtensionFilter.class::cast)
			.anyMatch(filter -> java.util.Arrays.asList(filter.getExtensions()).contains("pod")));
		assertEquals(4, saveChooser.getChoosableFileFilters().length,
			"Save should show MPO, XML, XLSX, and All Files filters");
		java.util.Set<String> saveExtensions = java.util.Arrays.stream(saveChooser.getChoosableFileFilters())
			.filter(FileNameExtensionFilter.class::isInstance)
			.map(FileNameExtensionFilter.class::cast)
			.flatMap(filter -> java.util.Arrays.stream(filter.getExtensions()))
			.collect(java.util.stream.Collectors.toSet());
		assertEquals(java.util.Set.of("mpo", "xml", "xlsx"), saveExtensions,
			"internal MPX, Planner, PMXML, JSON, and SDEF writers must not become Save formats");
		assertEquals(new File("C:\\projects\\source.mpo"),
			provider.prepareFileChooser(true, "C:\\projects\\source.pod").getSelectedFile());
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
		SwingFileChooserProvider provider = new SwingFileChooserProvider();

		SystemFileChooser openChooser = provider.prepareFileChooser(false, "C:\\projects\\imported.mpp");
		assertEquals(new File("C:\\projects\\imported.mpp"), openChooser.getSelectedFile());
		assertTrue(openChooser.getFileFilter().getDescription().contains("*.mpo"));

		SystemFileChooser saveChooser = provider.prepareFileChooser(true, "C:\\projects\\imported.mpp");
		assertEquals(new File("C:\\projects\\imported.mpo"), saveChooser.getSelectedFile());
		assertTrue(!saveChooser.isMultiSelectionEnabled(), "save dialogs must remain single-selection");
		assertTrue(saveChooser.getFileFilter().getDescription().contains("*.mpo"));
	}

	@Test
	void saveAsDefaultsLegacyPodSourcesToMpoAndKeepsExplicitExchangeFormats() {
		assertEquals("mpo", SwingFileChooserProvider.preferredSaveExtension("original.pod"));
		assertEquals("mpo", SwingFileChooserProvider.preferredSaveExtension("original.POD"));
		assertEquals("xml", SwingFileChooserProvider.preferredSaveExtension("original.xml"));
		assertEquals("xlsx", SwingFileChooserProvider.preferredSaveExtension("original.xlsx"));
	}

	@Test
	void saveAsFallsBackToAnEditableFormat() {
		assertEquals("mpo", SwingFileChooserProvider.preferredSaveExtension("imported.mpp"));
		assertEquals("mpo", SwingFileChooserProvider.preferredSaveExtension("project.mpo"));
		assertEquals("mpo", SwingFileChooserProvider.preferredSaveExtension("project.pod"));
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
