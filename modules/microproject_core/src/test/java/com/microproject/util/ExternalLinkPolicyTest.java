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
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.microproject.util.ExternalLinkPolicy.Decision;

class ExternalLinkPolicyTest {
	@ParameterizedTest
	@ValueSource(strings = { "https://example.test/help", "http://example.test", "HTTPS://EXAMPLE.TEST",
			"mailto:someone@example.test", "  https://example.test/padded  " })
	void opensWebAndMailAddresses(String address) {
		assertEquals(Decision.OPEN, ExternalLinkPolicy.classify(address));
	}

	@ParameterizedTest
	@ValueSource(strings = { "file:///C:/plans/spec.docx", "FILE:///tmp/spec.txt", "C:\\plans\\spec.docx",
			"c:/plans/spec.docx", "\\\\server\\share\\spec.docx", "docs/spec.txt", "spec.docx" })
	void asksBeforeOpeningFiles(String address) {
		assertEquals(Decision.CONFIRM, ExternalLinkPolicy.classify(address));
	}

	@ParameterizedTest
	@ValueSource(strings = { "javascript:alert(1)", "ms-msdt:/id", "smb://server/share", "jar:file:///a.jar!/b",
			"ftp://example.test/a", "data:text/html,x", "search-ms:query=x", "https://example.test/a b",
			"https://example.test/a\0b", "https://example.test/a\tb" })
	void rejectsOtherAddresses(String address) {
		assertEquals(Decision.REJECT, ExternalLinkPolicy.classify(address));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   ", "\t" })
	void rejectsMissingAddresses(String address) {
		assertEquals(Decision.REJECT, ExternalLinkPolicy.classify(address));
	}

	@Test
	void treatsDriveLetterWithoutRootAsUnknownScheme() {
		assertEquals(Decision.REJECT, ExternalLinkPolicy.classify("c:a.txt"));
	}
}
