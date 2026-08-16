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

import com.microproject.session.LocalSession;
import com.microproject.session.LoadOptions;

class ProjectLoadWorkflowTest {
	@Test
	void preparesLoadOptionsForProjectLibreCollaborationFile() {
		LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions("sample.pod", true, "alice");

		assertEquals("sample.pod", options.getFileName());
		assertEquals(LocalSession.LOCAL_PROJECT_IMPORTER, options.getImporter());
		assertTrue(options.isCollaborationEnabled());
		assertEquals("alice", options.getCollaborationUserKey());
		assertTrue(options.getSidecarFileName().endsWith(".projectlibre-sync.json"));
	}

	@Test
	void preparesLoadOptionsForMicrosoftFile() {
		LoadOptions options = ProjectLoadWorkflow.prepareLoadOptions("sample.mpp", false, "alice");

		assertEquals("sample.mpp", options.getFileName());
		assertEquals(LocalSession.MICROSOFT_PROJECT_IMPORTER, options.getImporter());
		assertFalse(options.isCollaborationEnabled());
	}
}
