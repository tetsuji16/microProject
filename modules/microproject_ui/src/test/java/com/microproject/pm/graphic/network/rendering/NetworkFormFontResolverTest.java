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
package com.microproject.pm.graphic.network.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Font;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.FormBoxLayout;
import com.microproject.util.Environment;

class NetworkFormFontResolverTest {
	@Test
	void resolvesConfiguredNetworkFormFontStylesWithoutChangingTheirValues() {
		String previousFont = Environment.getFont(Environment.NETWORK_FONT);
		Environment.setFont("Dialog PLAIN 11", Environment.NETWORK_FONT);
		try {
			FormBoxLayout layout = new FormBoxLayout();
			layout.setTitleFont("_Default_ BOLD 10");
			layout.setLabelFont("Dialog ITALIC 12");
			layout.setValueFont(null);

			assertEquals(Font.BOLD, NetworkFormFontResolver.resolve(layout, "title").getStyle());
			assertEquals(9, NetworkFormFontResolver.resolve(layout, "title").getSize());
			assertEquals(Font.ITALIC, NetworkFormFontResolver.resolve(layout, "label").getStyle());
			assertEquals(12, NetworkFormFontResolver.resolve(layout, "label").getSize());
			assertEquals(Font.decode("Dialog PLAIN 11"), NetworkFormFontResolver.resolve(layout, "value"));
			assertNull(NetworkFormFontResolver.resolve(layout, "unknown"));
		} finally {
			Environment.setFont(previousFont, Environment.NETWORK_FONT);
		}
	}
}
