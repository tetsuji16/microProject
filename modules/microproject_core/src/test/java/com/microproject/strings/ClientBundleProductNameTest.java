/*
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
 */
package com.microproject.strings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The welcome dialog and application message boxes take their title from these
 * keys. Their product name is translated text, so every locale must name the
 * current product rather than the upstream ProjectLibre brand (issue #865).
 */
class ClientBundleProductNameTest {
	private static final List<String> PRODUCT_TITLE_KEYS = List.of(
		"Text.welcomeToPod", "Text.ApplicationTitle", "Text.ShortTitle",
		"Title.MicroProjectError", "Title.MicroProjectWarning");

	@Test
	void everyClientLocaleNamesTheCurrentProductInWelcomeAndMessageTitles() throws Exception {
		URL base = ClientBundleProductNameTest.class.getResource("client.properties");
		assertEquals("file", base.getProtocol(), "client bundles must be scanned from the resource directory");
		List<Path> bundles;
		try (Stream<Path> files = Files.list(Path.of(base.toURI()).getParent())) {
			bundles = files.filter(path -> path.getFileName().toString().matches("client(_.+)?\\.properties"))
				.sorted().toList();
		}
		assertTrue(bundles.size() > 30, "expected every shipped client locale bundle, found " + bundles);
		List<String> stale = new ArrayList<>();
		for (Path bundle : bundles) {
			ResourceBundle values;
			try (InputStream input = Files.newInputStream(bundle)) {
				values = new PropertyResourceBundle(input);
			}
			for (String key : PRODUCT_TITLE_KEYS) {
				assertTrue(values.containsKey(key), bundle.getFileName() + " is missing " + key);
				String value = values.getString(key);
				if (value.contains("ProjectLibre") || !value.contains("microProject"))
					stale.add(bundle.getFileName() + ": " + key + "=" + value);
			}
			assertFalse(values.containsKey("File.projectlibre"),
				bundle.getFileName() + " retains the obsolete native file-filter key");
			if (values.containsKey("File.nativeProject")) {
				String nativeProjectLabel = values.getString("File.nativeProject");
				if (nativeProjectLabel.contains("ProjectLibre") || !nativeProjectLabel.contains("microProject"))
					stale.add(bundle.getFileName() + ": File.nativeProject=" + nativeProjectLabel);
			}
		}
		assertTrue(stale.isEmpty(), "product titles must use microProject: " + stale);
	}

	@Test
	void japaneseWelcomeDialogTitleMatchesMainWindowProductName() {
		ResourceBundle base = ResourceBundle.getBundle("com.microproject.strings.client", Locale.ROOT);
		ResourceBundle japanese = ResourceBundle.getBundle("com.microproject.strings.client", Locale.JAPANESE);

		assertEquals("Welcome to microProject", base.getString("Text.welcomeToPod"));
		assertEquals("microProject へようこそ", japanese.getString("Text.welcomeToPod"));
		assertEquals("microProject エラー", japanese.getString("Title.MicroProjectError"));
		assertEquals("microProject 警告", japanese.getString("Title.MicroProjectWarning"));
		assertFalse(japanese.getString("Text.welcomeToPod").contains("ProjectLibre"));
	}
}
