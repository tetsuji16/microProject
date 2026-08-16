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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.session.LoadOptions;
import com.microproject.session.LocalSession;
import com.microproject.session.SaveOptions;

class ProjectFilePoliciesTest {
	@Test
	void resolvesImporterForProjectLibreLoadWhenLocalOnly() {
		assertEquals(LocalSession.LOCAL_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.pod", true));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.xml", true));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.xlsx", true));
	}

	@Test
	void resolvesImporterForProjectLibreLoadWhenServerBacked() {
		assertEquals(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.pod", false));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.xml", false));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.xlsx", false));
	}

	@Test
	void resolvesMicrosoftImporterForNonProjectLibreFiles() {
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.mpp", true));
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, ProjectFilePolicies.resolveLoadImporter("plan.mpp", false));
		assertFalse(ProjectFilePolicies.isProjectLibreFile("plan.mpp"));
		assertTrue(ProjectFilePolicies.isProjectLibreFile("plan.pod"));
	}

	@Test
	void configuresLoadAndSaveOptions() {
		LoadOptions loadOptions = new LoadOptions();
		ProjectFilePolicies.configureLoadOptions(loadOptions, "plan.pod", false);
		assertEquals("plan.pod", loadOptions.getFileName());
		assertEquals(LocalSession.SERVER_LOCAL_PROJECT_IMPORTER, loadOptions.getImporter());

		SaveOptions saveOptions = new SaveOptions();
		ProjectFilePolicies.configureSaveOptions(saveOptions, "plan.mpp");
		assertEquals("plan.mpp", saveOptions.getFileName());
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, saveOptions.getImporter());
	}
}
