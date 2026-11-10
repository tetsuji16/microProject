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
package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HyperlinkTest {
	@Test
	void toStringEscapesAddressAndLabel() {
		Hyperlink link = new Hyperlink("<b>plan</b> & \"more\"", "https://example.test/?a=1&b=\"2\"><script>");

		assertEquals("<html><a href=\"https://example.test/?a=1&amp;b=&quot;2&quot;&gt;&lt;script&gt;\">"
				+ "&lt;b&gt;plan&lt;/b&gt; &amp; &quot;more&quot;</a></html>", link.toString());
	}

	@Test
	void toStringKeepsPlainValuesReadable() {
		assertEquals("<html><a href=\"https://example.test/help\">Help</a></html>",
				new Hyperlink("Help", "https://example.test/help").toString());
	}
}
