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
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * The customization pages sit in a scroll pane of the preferences dialog. A page that keeps its preferred
 * width in a narrow window pushes its right-hand column (the Up/Down buttons and the action row) out of view,
 * which is what users saw as an insufficient window width at 125% and 150% display scaling.
 */
class CustomizePageWidthTest {
	/** The page area of a preferences dialog on a 1024x720 work area at 125% scaling, in logical pixels. */
	private static final int VIEWPORT_WIDTH = 600;
	private static final int VIEWPORT_HEIGHT = 420;

	@Test
	void ribbonCustomizationPageKeepsEveryColumnInsideANarrowWindow() throws Exception {
		assertPageNarrows(new RibbonCustomizationPanel(), "ribbonCustomizeCommandsScroll",
			"ribbonCustomizeTreeScroll", "ribbonCustomizeAdd", "ribbonCustomizeUp", "ribbonCustomizeNewTab",
			"ribbonCustomizeReset");
	}

	@Test
	void quickAccessPageKeepsEveryColumnInsideANarrowWindow() throws Exception {
		assertPageNarrows(new QuickAccessCustomizationPanel(), "quickAccessAvailableScroll",
			"quickAccessChosenScroll", "quickAccessAdd", "quickAccessUp", "quickAccessReset");
	}

	private static void assertPageNarrows(CustomizeSupport.Page page, String... requiredControls) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(page.getScrollableTracksViewportWidth(),
				"the page must take the width of its scroll pane instead of its preferred width");
			assertTrue(page.getPreferredSize().width > VIEWPORT_WIDTH,
				"the fixture must be narrower than the page's preferred width, or it proves nothing");
			// The scroll pane hands the page exactly this width when its viewport is narrower than the page.
			page.setSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
			page.doLayout();
			for (String name : requiredControls) {
				JComponent control = (JComponent) find(page, name);
				assertNotNull(control, name + " must exist on the page");
				Rectangle bounds = control.getBounds();
				Point origin = SwingUtilities.convertPoint(control.getParent(), bounds.getLocation(), page);
				assertTrue(origin.x >= 0 && origin.x + bounds.width <= VIEWPORT_WIDTH,
					name + " must stay inside the page width " + VIEWPORT_WIDTH + " but is at " + origin
						+ " size " + bounds.width);
			}
		});
	}

	private static Component find(Container root, String name) {
		for (Component child : root.getComponents()) {
			if (name.equals(child.getName())) return child;
			if (child instanceof Container container) {
				Component found = find(container, name);
				if (found != null) return found;
			}
		}
		return null;
	}
}
