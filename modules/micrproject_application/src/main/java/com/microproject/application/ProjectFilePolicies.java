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
		if (FileHelper.isPodxFile(fileName)) {
			return LocalSession.PODX_PROJECT_IMPORTER;
		}
		if (isProjectLibreFile(fileName)) {
			return localOnlySession ? LocalSession.LOCAL_PROJECT_IMPORTER : LocalSession.SERVER_LOCAL_PROJECT_IMPORTER;
		}
		return LocalSession.MICROSOFT_PROJECT_IMPORTER;
	}

	public static String resolveSaveImporter(String fileName) {
		if (FileHelper.isPodxFile(fileName)) {
			return LocalSession.PODX_PROJECT_IMPORTER;
		}
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
