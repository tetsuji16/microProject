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
package com.microproject.pm.graphic.frames.workspace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.frames.GraphicManager;

class DefaultFrameManagerTest {
	@Test
	void projectSelectorActivatesTheSelectedOpenProjectFrame() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			TrackingPanel container = new TrackingPanel();
			container.setLayout(new BorderLayout());
			JPanel emptyPanel = new JPanel();
			GraphicManager graphicManager = new GraphicManager(new JPanel());
			DefaultFrameManager frameManager = new DefaultFrameManager(container, emptyPanel, graphicManager);
			TestNamedFrame firstProject = new TestNamedFrame("first", "First project");
			TestNamedFrame secondProject = new TestNamedFrame("second", "Second project");

			frameManager.addFrame(firstProject);
			frameManager.addFrame(secondProject);

			JComboBox projectSelector = (JComboBox)frameManager.getProjectComboPanel().getComponent(0);
			assertTrue(frameManager.getProjectComboPanel().isVisible());
			assertSame(secondProject, frameManager.getSelectedFrame());
			assertTrue(secondProject.isActive());
			assertFalse(firstProject.isActive());

			container.resetRepaintCount();
			projectSelector.setSelectedItem(firstProject);

			assertSame(firstProject, frameManager.getSelectedFrame());
			assertTrue(firstProject.isActive());
			assertTrue(firstProject.isVisible());
			assertFalse(secondProject.isActive());
			assertFalse(secondProject.isVisible());
			assertSame(firstProject, container.getComponent(0));
			assertTrue(container.repaintCount > 0, "Switching open projects must repaint the document container");
		});
	}

	@Test
	void arrangeAllTilesOpenProjectsAndProjectSelectionReturnsToSingleFrame() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			TrackingPanel container = new TrackingPanel();
			container.setLayout(new BorderLayout());
			DefaultFrameManager manager = new DefaultFrameManager(container, new JPanel(), new GraphicManager(new JPanel()));
			TestNamedFrame first = new TestNamedFrame("first", "First");
			TestNamedFrame second = new TestNamedFrame("second", "Second");
			manager.addFrame(first);
			manager.addFrame(second);

			manager.arrangeAll();

			assertTrue(container.getComponent(0) instanceof JPanel);
			assertEquals(2, ((JPanel) container.getComponent(0)).getComponentCount());
			assertTrue(first.isVisible());
			assertTrue(second.isVisible());
			assertTrue(second.isActive());

			manager.activateFrame(first);

			assertSame(first, container.getComponent(0));
			assertTrue(first.isActive());
			assertFalse(second.isVisible());
		});
	}

	private static final class TrackingPanel extends JPanel {
		private int repaintCount;

		@Override
		public void repaint() {
			repaintCount++;
			super.repaint();
		}

		private void resetRepaintCount() {
			repaintCount = 0;
		}
	}

	private static final class TestNamedFrame extends NamedFrame {
		private TestNamedFrame(String id, String title) {
			super(id, new ImageIcon());
			setTabTitle(title);
		}
	}
}
