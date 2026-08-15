package com.microproject.application;

import com.microproject.session.FileHelper;
import com.microproject.session.LoadOptions;
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;

public final class ProjectFilePolicies {
	private ProjectFilePolicies() {
	}

	public static boolean isProjectLibreFile(String fileName) {
		return FileHelper.isProjectLibreFile(fileName);
	}

	public static String resolveLoadImporter(String fileName, boolean localOnlySession) {
		if (isProjectLibreFile(fileName)) {
			return localOnlySession ? LocalSession.LOCAL_PROJECT_IMPORTER : LocalSession.SERVER_LOCAL_PROJECT_IMPORTER;
		}
		return LocalSession.MICROSOFT_PROJECT_IMPORTER;
	}

	public static String resolveSaveImporter(String fileName) {
		return isProjectLibreFile(fileName) ? LocalSession.LOCAL_PROJECT_IMPORTER : LocalSession.MICROSOFT_PROJECT_IMPORTER;
	}

	public static void configureLoadOptions(LoadOptions options, String fileName, boolean localOnlySession) {
		if (options == null) {
			return;
		}
		options.setFileName(fileName);
		options.setImporter(resolveLoadImporter(fileName, localOnlySession));
	}

	public static void configureSaveOptions(SaveOptions options, String fileName) {
		if (options == null) {
			return;
		}
		options.setFileName(fileName);
		options.setImporter(resolveSaveImporter(fileName));
	}
}
