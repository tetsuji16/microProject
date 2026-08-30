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

import java.awt.Component;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemFileChooser.FileFilter;
import com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter;
import com.microproject.session.FileHelper;
import com.microproject.preference.ConfigurationFile;
import com.microproject.strings.Messages;

public final class SwingFileChooserProvider implements UiServices.FileChooserProvider {
	private static final String DEFAULT_FILE_EXTENSION = FileHelper.DEFAULT_FILE_EXTENSION;
	private static final String LEGACY_POD_FILE_EXTENSION = FileHelper.POD_FILE_EXTENSION;
	/**
	 * Native (OS) file dialogs are enabled by default. Users can still force
	 * the Swing fallback via -Dflatlaf.useSystemFileChooser=false, e.g. when
	 * running on a platform without FlatLaf native library support.
	 */
	static final String USE_SYSTEM_FILE_CHOOSER_PROPERTY = "flatlaf.useSystemFileChooser";

	static {
		SystemFileChooser.setStateStore(new SystemFileChooser.StateStore() {
			private static final String KEY_PREFIX = "fileChooser.";
			private final java.util.prefs.Preferences state = Preferences.userNodeForPackage(SwingFileChooserProvider.class);

			@Override
			public String get(String key, String def) {
				return state.get(KEY_PREFIX + key, def);
			}

			@Override
			public void put(String key, String value) {
				if (value != null)
					state.put(KEY_PREFIX + key, value);
				else
					state.remove(KEY_PREFIX + key);
			}
		});
	}

	private SystemFileChooser fileChooser;
	private String chooserConfigurationSignature;
	private Boolean chooserConfigurationSaveMode;
	private FileFilter projectlibreFilter;
	private FileFilter legacyPodFilter;
	private FileFilter microsoftFilter;
	private FileFilter microsoftXMLFilter;
	private FileFilter microsoftXlsxFilter;
	private FileFilter plannerFilter;
	private FileFilter projectFilter;

	@Override
	public synchronized String chooseFileName(boolean save, String selectedFileName, Object parent) {
		Component fileChooserParent = parent instanceof Component ? (Component) parent : null;
		if (save) {
			selectedFileName = normalizeHostedSelectedFileName(selectedFileName, Environment.getStandAlone());
		}
		SystemFileChooser chooser = prepareFileChooser(save, selectedFileName);
		if (selectedFileName == null) {
			try {
				String initialDirName = Preferences.userNodeForPackage(FileHelper.class).get("lastDirectory", System.getProperty("user.home") + File.separator + "ProjectLibre");
				chooser.setCurrentDirectory(new File(initialDirName));
			} catch (Exception e) {
			}
		}
		int result = save ? chooser.showSaveDialog(fileChooserParent) : chooser.showOpenDialog(fileChooserParent);
		if (result != SystemFileChooser.APPROVE_OPTION) {
			return null;
		}
		File file = chooser.getSelectedFile();
		String fileName = file.toString();
		FileFilter currentFilter = chooser.getFileFilter();
		if (save) {
			fileName = normalizeSelectedSaveFileName(fileName, currentFilter);
		}
		Preferences.userNodeForPackage(FileHelper.class).put("lastDirectory", file.getParent());
		return fileName;
	}

	@Override
	public synchronized List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
		if (save) {
			String selected = chooseFileName(true, selectedFileName, parent);
			return selected == null ? List.of() : List.of(selected);
		}
		SystemFileChooser chooser = prepareFileChooser(false, selectedFileName);
		Component fileChooserParent = parent instanceof Component ? (Component) parent : null;
		int result = chooser.showOpenDialog(fileChooserParent);
		if (result != SystemFileChooser.APPROVE_OPTION) {
			return List.of();
		}
		File[] files = chooser.getSelectedFiles();
		if (files == null || files.length == 0) {
			File single = chooser.getSelectedFile();
			return single == null ? List.of() : List.of(single.toString());
		}
		Preferences.userNodeForPackage(FileHelper.class).put("lastDirectory", files[0].getParent());
		return selectedFileNames(files);
	}

	static List<String> selectedFileNames(File[] files) {
		if (files == null || files.length == 0) return List.of();
		List<String> names = new ArrayList<>(files.length);
		for (File file : files) {
			if (file != null) names.add(file.toString());
		}
		return List.copyOf(names);
	}

	static String normalizeHostedSelectedFileName(String selectedFileName, boolean standalone) {
		if (!standalone && LEGACY_POD_FILE_EXTENSION.equals(FileHelper.getFileExtension(selectedFileName))) {
			return FileHelper.changeFileExtension(selectedFileName, "xml");
		}
		return selectedFileName;
	}

	SystemFileChooser prepareFileChooser(boolean save, String selectedFileName) {
		SystemFileChooser chooser = getFileChooser();
		chooser.setSelectedFile(initialSelectedFile(selectedFileName));
		ensureFileChooserConfigured(save);
		if (save) {
			selectSaveFileFilter(chooser, selectedFileName);
			applySaveFileFilterDefaults(chooser);
			if (selectedFileName != null) {
				chooser.setSelectedFile(new File(getSuggestedSaveFileName(selectedFileName, chooser.getFileFilter())));
			}
		} else {
			selectOpenFileFilter(chooser);
		}
		return chooser;
	}

	static File initialSelectedFile(String selectedFileName) {
		return selectedFileName == null ? null : new File(selectedFileName);
	}

	private SystemFileChooser getFileChooser() {
		if (fileChooser == null) {
			fileChooser = new SystemFileChooser();
			fileChooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
			fileChooser.setAcceptAllFileFilterUsed(true);
		}
		return fileChooser;
	}

	private String getChooserConfigurationSignature() {
		Preferences pref = Preferences.userNodeForPackage(ConfigurationFile.class);
		boolean useExternalLocales = pref.getBoolean("useExternalLocales", false);
		String externalLocalesDirectory = pref.get("externalLocalesDirectory", "");
		return Locale.getDefault().toString() + "|" + useExternalLocales + "|" + externalLocalesDirectory;
	}

	void configureFileChooser(SystemFileChooser chooser, final boolean save) {
		chooser.setMultiSelectionEnabled(!save);
		projectlibreFilter = new FileNameExtensionFilter(
			formatFilterLabel(Messages.getString("File.projectlibre"), "*." + DEFAULT_FILE_EXTENSION),
			DEFAULT_FILE_EXTENSION);
		legacyPodFilter = new FileNameExtensionFilter(
			formatFilterLabel(Messages.getString("File.projectlibrePod"), "*." + LEGACY_POD_FILE_EXTENSION),
			LEGACY_POD_FILE_EXTENSION);
		microsoftFilter = new FileNameExtensionFilter(
			formatFilterLabel(Messages.getString("File.microsoft"), "*.mpp, *.mpx"),
			"mpp", "mpx");
		microsoftXMLFilter = new FileNameExtensionFilter(
			formatFilterLabel(Messages.getString("File.microsoftXML"), "*.xml"),
			"xml");
		microsoftXlsxFilter = new FileNameExtensionFilter(
			"Excel Workbook (*.xlsx)",
			"xlsx");
		plannerFilter = new FileNameExtensionFilter(
			formatFilterLabel(Messages.getString("File.planner"), "*.planner"),
			"planner");
		projectFilter = new FileNameExtensionFilter(
			Messages.getString("File.projects"),
			DEFAULT_FILE_EXTENSION, LEGACY_POD_FILE_EXTENSION, "xml", "xlsx", "planner", "mpp", "mpx");

		chooser.resetChoosableFileFilters();
		chooser.setAcceptAllFileFilterUsed(true);
		if (save) {
			if (Environment.getStandAlone()) {
				chooser.addChoosableFileFilter(projectlibreFilter);
				chooser.addChoosableFileFilter(legacyPodFilter);
			}
			chooser.addChoosableFileFilter(microsoftXMLFilter);
			chooser.addChoosableFileFilter(microsoftXlsxFilter);
		} else {
			if (Environment.getStandAlone()) {
				chooser.addChoosableFileFilter(projectlibreFilter);
				chooser.addChoosableFileFilter(legacyPodFilter);
			}
			chooser.addChoosableFileFilter(microsoftFilter);
			chooser.addChoosableFileFilter(microsoftXMLFilter);
			chooser.addChoosableFileFilter(microsoftXlsxFilter);
			chooser.addChoosableFileFilter(plannerFilter);
			chooser.addChoosableFileFilter(projectFilter);
		}
	}

	static String formatFilterLabel(String label, String extensionPattern) {
		return Messages.format("Format.fileFilter", label, extensionPattern);
	}

	void selectOpenFileFilter(SystemFileChooser chooser) {
		chooser.setFileFilter(Environment.getStandAlone() ? projectlibreFilter : projectFilter);
	}

	private void selectSaveFileFilter(SystemFileChooser chooser, String selectedFileName) {
		String extension = preferredSaveExtension(selectedFileName, Environment.getStandAlone());
		if (DEFAULT_FILE_EXTENSION.equals(extension) && projectlibreFilter != null) {
			chooser.setFileFilter(projectlibreFilter);
		} else if (LEGACY_POD_FILE_EXTENSION.equals(extension)) {
			chooser.setFileFilter(legacyPodFilter);
		} else if ("xlsx".equals(extension)) {
			chooser.setFileFilter(microsoftXlsxFilter);
		} else {
			chooser.setFileFilter(microsoftXMLFilter);
		}
	}

	static String preferredSaveExtension(String selectedFileName, boolean standalone) {
		String extension = selectedFileName == null ? null : FileHelper.getFileExtension(selectedFileName);
		if ("xlsx".equals(extension)) {
			return "xlsx";
		}
		if ("xml".equals(extension)) {
			return "xml";
		}
		if (DEFAULT_FILE_EXTENSION.equals(extension)) {
			return DEFAULT_FILE_EXTENSION;
		}
		if (standalone && LEGACY_POD_FILE_EXTENSION.equals(extension)) {
			return extension;
		}
		return standalone ? DEFAULT_FILE_EXTENSION : "xml";
	}

	private void ensureFileChooserConfigured(final boolean save) {
		String signature = getChooserConfigurationSignature();
		if (chooserConfigurationSignature == null
				|| chooserConfigurationSaveMode == null
				|| chooserConfigurationSaveMode.booleanValue() != save
				|| !chooserConfigurationSignature.equals(signature)) {
			configureFileChooser(getFileChooser(), save);
			chooserConfigurationSignature = signature;
			chooserConfigurationSaveMode = Boolean.valueOf(save);
		}
	}

	private String getSuggestedSaveFileName(String baseFileName, FileFilter filter) {
		if (baseFileName == null) {
			return null;
		}
		if (filter == microsoftXlsxFilter) {
			return changeFileExtension(baseFileName, "xlsx");
		}
		if (filter == microsoftXMLFilter) {
			return changeFileExtension(baseFileName, "xml");
		}
		if (filter == projectlibreFilter) {
			return changeFileExtension(baseFileName, DEFAULT_FILE_EXTENSION);
		}
		if (filter == legacyPodFilter) {
			return changeFileExtension(baseFileName, LEGACY_POD_FILE_EXTENSION);
		}
		return baseFileName;
	}

	private String normalizeSelectedSaveFileName(String fileName, FileFilter filter) {
		String extension = getSaveExtension(filter);
		if (extension == null) {
			return FileHelper.isFileNameAllowed(fileName, true) ? fileName : changeFileExtension(fileName, DEFAULT_FILE_EXTENSION);
		}
		String currentExtension = FileHelper.getFileExtension(fileName);
		if (extension.equals(currentExtension)) {
			return fileName;
		}
		return changeFileExtension(fileName, extension);
	}

	private String getSaveExtension(FileFilter filter) {
		if (filter == microsoftXlsxFilter) {
			return "xlsx";
		}
		if (filter == microsoftXMLFilter) {
			return "xml";
		}
		if (filter == projectlibreFilter) {
			return DEFAULT_FILE_EXTENSION;
		}
		if (filter == legacyPodFilter) {
			return LEGACY_POD_FILE_EXTENSION;
		}
		return null;
	}

	private void applySaveFileFilterDefaults(SystemFileChooser chooser) {
		String extension = getSaveExtension(chooser.getFileFilter());
		chooser.putPlatformProperty(SystemFileChooser.WINDOWS_DEFAULT_EXTENSION, extension);
	}

	private String changeFileExtension(String fileName, String extension) {
		if (fileName == null) {
			return null;
		}
		int i = fileName.lastIndexOf('.');
		if (i <= 0) {
			return fileName + "." + extension;
		}
		return fileName.substring(0, i) + "." + extension;
	}
}
