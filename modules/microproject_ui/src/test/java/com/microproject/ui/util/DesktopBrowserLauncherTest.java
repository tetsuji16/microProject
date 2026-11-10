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
package com.microproject.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DesktopBrowserLauncherTest {
	private final List<String> events = new ArrayList<>();
	private final List<String> asked = new ArrayList<>();
	private final List<String> notices = new ArrayList<>();
	private File base = new File(System.getProperty("java.io.tmpdir"), "plans").getAbsoluteFile();

	private DesktopBrowserLauncher launcher(boolean answer) {
		DesktopBrowserLauncher.Opener opener = new DesktopBrowserLauncher.Opener() {
			@Override
			public void browse(URI uri) {
				events.add("browse " + uri);
			}

			@Override
			public void open(File file) {
				events.add("open " + file.getPath());
			}
		};
		return new DesktopBrowserLauncher(opener, path -> {
			asked.add(path);
			return answer;
		}, notices::add, () -> base);
	}

	@Test
	void webAndMailAddressesOpenWithoutAsking() {
		DesktopBrowserLauncher launcher = launcher(false);

		launcher.displayURL("https://example.test/help");
		launcher.displayURL("mailto:a@example.test");

		assertEquals(List.of("browse https://example.test/help", "browse mailto:a@example.test"), events);
		assertTrue(asked.isEmpty());
	}

	@Test
	void fileOpensOnlyAfterTheUserAgrees() {
		File target = new File(base, "scope.docx").getAbsoluteFile();

		launcher(false).displayURL(target.toURI().toString());
		assertTrue(events.isEmpty());
		assertEquals(List.of(target.getPath()), asked);

		launcher(true).displayURL(target.toURI().toString());
		assertEquals(List.of("open " + target.getPath()), events);
	}

	@Test
	void relativeFileIsResolvedAgainstTheProjectFolder() {
		launcher(true).displayURL("docs/scope.docx");

		assertEquals(List.of("open " + new File(base, "docs/scope.docx").getPath()), events);
	}

	@Test
	void relativeFileWithoutProjectFolderIsNotOpened() {
		base = null;

		launcher(true).displayURL("docs/scope.docx");

		assertTrue(events.isEmpty());
		assertTrue(asked.isEmpty());
		assertEquals(1, notices.size());
	}

	@Test
	void rejectedAddressIsNotOpenedAndTheUserIsTold() {
		launcher(true).displayURL("ms-msdt:/id");

		assertTrue(events.isEmpty());
		assertTrue(asked.isEmpty());
		assertEquals(1, notices.size());
		assertTrue(notices.get(0).contains("ms-msdt:/id"));
	}
}
