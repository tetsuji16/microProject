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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class NetworkCellEditorTest {
	@Test
	void disablesDoubleBufferingOnlyWhilePaintingAndRestoresIt() throws Exception {
		BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		JPanel container = new JPanel() {
			@Override
			public Graphics getGraphics() {
				return image.createGraphics();
			}
		};
		TestEditor editor = new TestEditor(container);
		AtomicBoolean paintedWithoutDoubleBuffering = new AtomicBoolean();
		JPanel child = new JPanel() {
			@Override
			public void paint(Graphics graphics) {
				paintedWithoutDoubleBuffering.set(!isDoubleBuffered());
			}
		};

		assertTrue(child.isDoubleBuffered());
		SwingUtilities.invokeAndWait(() -> editor.paint(child));

		assertTrue(paintedWithoutDoubleBuffering.get());
		assertTrue(child.isDoubleBuffered());
	}

	private static final class TestEditor extends NetworkCellEditor {
		private TestEditor(JPanel container) {
			super(null, container);
		}

		private void paint(Component component) {
			paintComponentApart(component, new Rectangle(0, 0, 20, 20));
		}
	}
}
