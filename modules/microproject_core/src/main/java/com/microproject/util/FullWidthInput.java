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

/**
 * Converts full-width ASCII-range characters typed through a Japanese input method
 * to their half-width forms so numeric, unit, percent, money and date parsers accept
 * them. Only U+FF01-U+FF5E and the ideographic space U+3000 are touched; the result
 * has the same length as the input so ParsePosition indexes stay valid.
 */
public final class FullWidthInput {
	private FullWidthInput() {
	}

	public static String normalize(String text) {
		if (text == null)
			return null;
		StringBuilder out = null;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			char mapped = c;
			if (c >= '！' && c <= '～')
				mapped = (char) (c - 0xFEE0);
			else if (c == '　')
				mapped = ' ';
			if (mapped != c && out == null) {
				out = new StringBuilder(text.length());
				out.append(text, 0, i);
			}
			if (out != null)
				out.append(mapped);
		}
		return out == null ? text : out.toString();
	}
}
