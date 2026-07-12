/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.util;

import java.awt.Component;
import java.io.File;
import java.util.Locale;
import java.util.prefs.Preferences;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.util.SystemFileChooser.FileFilter;
import com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter;
import com.projectlibre1.session.FileHelper;
import com.projectlibre1.preference.ConfigurationFile;
import com.projectlibre1.strings.Messages;

public final class SwingFileChooserProvider implements UiServices.FileChooserProvider {
	private static final String DEFAULT_FILE_EXTENSION = "pod";

	private SystemFileChooser fileChooser;
	private String chooserConfigurationSignature;
	private Boolean chooserConfigurationSaveMode;
	private FileFilter projectlibreFilter;
	private FileFilter microsoftFilter;
	private FileFilter microsoftXMLFilter;
	private FileFilter microsoftXlsxFilter;
	private FileFilter plannerFilter;
	private FileFilter projectFilter;

	@Override
	public synchronized String chooseFileName(boolean save, String selectedFileName, Object parent) {
		Component fileChooserParent = parent instanceof Component ? (Component) parent : null;
		if (!Environment.getStandAlone() && save && selectedFileName != null && selectedFileName.endsWith("." + DEFAULT_FILE_EXTENSION)) {
			selectedFileName = changeFileExtension(selectedFileName, "xml");
		}
		SystemFileChooser chooser = getFileChooser();
		ensureFileChooserConfigured(save);
		if (save) {
			applySaveFileFilterDefaults(chooser);
		}
		if (selectedFileName != null) {
			chooser.setSelectedFile(new File(getSuggestedSaveFileName(selectedFileName, chooser.getFileFilter())));
		}
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

	private void configureFileChooser(SystemFileChooser chooser, final boolean save) {
		projectlibreFilter = new FileNameExtensionFilter(
			Messages.getString("File.projectlibre") + " (*." + DEFAULT_FILE_EXTENSION + ")",
			DEFAULT_FILE_EXTENSION);
		microsoftFilter = new FileNameExtensionFilter(
			Messages.getString("File.microsoft") + " (*.mpp, *.mpx)",
			"mpp", "mpx");
		microsoftXMLFilter = new FileNameExtensionFilter(
			Messages.getString("File.microsoftXML") + " (*.xml)",
			"xml");
		microsoftXlsxFilter = new FileNameExtensionFilter(
			"Excel Workbook (*.xlsx)",
			"xlsx");
		plannerFilter = new FileNameExtensionFilter(
			Messages.getString("File.planner") + " (*.planner)",
			"planner");
		projectFilter = new FileNameExtensionFilter(
			Messages.getString("File.projects"),
			DEFAULT_FILE_EXTENSION, "xml", "xlsx", "planner", "mpp", "mpx");

		chooser.resetChoosableFileFilters();
		chooser.setAcceptAllFileFilterUsed(true);
		if (save) {
			File selectedFile = chooser.getSelectedFile();
			String selectedExtension = selectedFile != null ? FileHelper.getFileExtension(selectedFile.getName()) : null;
			if ("xlsx".equals(selectedExtension) || "mpp".equals(selectedExtension) || "mpx".equals(selectedExtension)) {
				if (Environment.getStandAlone()) {
					chooser.addChoosableFileFilter(projectlibreFilter);
				}
				chooser.addChoosableFileFilter(microsoftXMLFilter);
				chooser.addChoosableFileFilter(microsoftXlsxFilter);
			} else {
				chooser.addChoosableFileFilter(microsoftXMLFilter);
				chooser.addChoosableFileFilter(microsoftXlsxFilter);
				if (Environment.getStandAlone()) {
					chooser.addChoosableFileFilter(projectlibreFilter);
				}
			}
		} else {
			if (Environment.getStandAlone()) {
				chooser.addChoosableFileFilter(projectlibreFilter);
			}
			chooser.addChoosableFileFilter(microsoftFilter);
			chooser.addChoosableFileFilter(microsoftXMLFilter);
			chooser.addChoosableFileFilter(microsoftXlsxFilter);
			chooser.addChoosableFileFilter(plannerFilter);
			chooser.addChoosableFileFilter(projectFilter);
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
		}
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

