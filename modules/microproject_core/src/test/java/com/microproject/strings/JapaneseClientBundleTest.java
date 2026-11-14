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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

class JapaneseClientBundleTest {
	private static Properties load(String resource) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = JapaneseClientBundleTest.class.getResourceAsStream(resource);
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}

	@Test
	void japaneseBundleContainsEveryBaseMessageKey() throws IOException {
		Properties base = load("/com/microproject/strings/client.properties");
		Properties japanese = load("/com/microproject/strings/client_ja.properties");
		List<String> missing = new ArrayList<>();
		for (String key : base.stringPropertyNames())
			if (!japanese.containsKey(key))
				missing.add(key);
		assertTrue(missing.isEmpty(), "client_ja.properties is missing " + missing);
	}

	@Test
	void projectAndSharedPoolLabelsResolveFromJapaneseBundle() {
		ResourceBundle base = ResourceBundle.getBundle("com.microproject.strings.client", Locale.ROOT);
		ResourceBundle japanese = ResourceBundle.getBundle("com.microproject.strings.client", Locale.JAPANESE);

		assertEquals("プロジェクト", japanese.getString("File.projects"));
		assertEquals("共有リソースプール", japanese.getString("SharedResourcePool.title"));
		assertEquals("未設定", japanese.getString("StatusDateDialog.NotSet"));
		assertNotEquals(base.getString("File.projects"), japanese.getString("File.projects"));
	}
}
