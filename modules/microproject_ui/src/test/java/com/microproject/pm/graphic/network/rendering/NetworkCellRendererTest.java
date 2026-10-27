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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.BarStyles;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.model.cache.GraphicNode;

class NetworkCellRendererTest {
	@Test
	void skipsNodeWithNoMainBarFormat() {
		BarStyles noMainFormats = new BarStyles();
		assertTrue(noMainFormats.getRows().isEmpty(), "fixture must contain no main or alternate formats");
		GraphParams params = (GraphParams) Proxy.newProxyInstance(GraphParams.class.getClassLoader(),
			new Class<?>[] { GraphParams.class }, (proxy, method, arguments) -> switch (method.getName()) {
				case "getBarStyles" -> noMainFormats;
				case "useTextures" -> false;
				default -> null;
			});
		GraphicNode node = new GraphicNode(NodeFactory.getInstance().createNode(new Object()), 0);

		assertNull(new FormatSelector(params).getForm(node, 0, false),
			"FormatSelector must preserve its documented null result when no main format matches");
		assertNull(new NetworkCellRenderer(params).getRendererComponent(node, 0));
	}
}
