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
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FindDialogCleanupAuditTest {
	@Test
	void findDialogRemovesGlobalListenersOnDispose() throws Exception {
		String source = source("modules/micrproject_ui/src/main/java/com/microproject/dialog/FindDialog.java");

		assertTrue(source.contains("public void dispose()"));
		assertTrue(source.contains("DocumentSelectedEvent.removeListener(this);"));
		assertTrue(source.contains("documentFrame.getProject().removeObjectListener(this);"));
		assertTrue(source.contains("FlatUiSupport.styleDialogButton(next, false);"));
		assertTrue(source.contains("FlatUiSupport.styleDialogButton(previous, false);"));
		assertFalse(source.contains("private static FindDialog instance = null;"));
		assertFalse(source.contains("TODO set minimum size"));
		assertFalse(source.contains("TODO Auto-generated method stub"));
	}

	private static String source(String relativePath) throws IOException {
		for (Path current = Path.of("").toAbsolutePath(); current != null; current = current.getParent()) {
			Path candidate = current.resolve(relativePath).normalize();
			if (Files.exists(candidate)) {
				return Files.readString(candidate, StandardCharsets.UTF_8);
			}
		}
		throw new java.nio.file.NoSuchFileException(relativePath);
	}
}
