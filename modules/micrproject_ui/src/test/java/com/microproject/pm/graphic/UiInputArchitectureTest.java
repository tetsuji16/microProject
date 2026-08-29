/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.graphic;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class UiInputArchitectureTest {
	@Test
	void spreadsheetPackageDoesNotContainAnIndependentKeyboardInputSandbox() throws Exception {
		Path sourceRoot = findProjectRoot().resolve(
			"modules/micrproject_ui/src/main/java/com/microproject/pm/graphic/spreadsheet");
		try (var files = Files.walk(sourceRoot)) {
			assertFalse(files.anyMatch(path -> path.getFileName().toString().equals("SpreadsheetImeSandbox.java")),
				"Production spreadsheet input must use the document shortcut and editor paths, not an isolated sandbox");
		}
	}

	private static Path findProjectRoot() throws java.io.IOException {
		for (Path current = Path.of("").toAbsolutePath(); current != null; current = current.getParent()) {
			if (Files.isDirectory(current.resolve("modules/micrproject_ui")))
				return current;
		}
		throw new java.io.IOException("Project root was not found");
	}
}
