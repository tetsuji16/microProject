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
package com.microproject.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.ListResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;

class DocumentMenuAcceleratorTest {
	private static final KeyStroke CTRL_N = KeyStroke.getKeyStroke("ctrl N");
	private static final KeyStroke CTRL_K = KeyStroke.getKeyStroke("ctrl K");

	@Test
	void stripsOnlyAcceleratorsOwnedByDocumentRootPane() {
		InputMap root = new InputMap();
		root.put(CTRL_N, "newProject");
		InputMap parent = new InputMap();
		parent.put(CTRL_K, "lookAndFeelAction");
		root.setParent(parent);
		ExtMenuFactory factory = factory(root);

		assertNull(factory.createJMenuItem("new").getAccelerator());
		assertEquals(CTRL_K, factory.createJMenuItem("other").getAccelerator());
		assertEquals("newProject", root.get(CTRL_N));
		assertEquals(1, Arrays.stream(root.keys()).filter(CTRL_N::equals).count());
	}

	@Test
	void leavesPrintPreviewStyleMenuAcceleratorAlone() {
		assertEquals(CTRL_N, factory(null).createJMenuItem("new").getAccelerator());
	}

	@Test
	void managerSetterConfiguresItsMenuFactory() {
		ProjectMenuActionMap actions = new ProjectMenuActionMap() {
			@Override public javax.swing.Action getAction(String key) { return new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) { }
			}; }
			@Override public String getStringFromAction(javax.swing.Action action) { return "newProject"; }
		};
		MenuManager manager = MenuManager.getInstance(actions);
		InputMap root = new InputMap();
		root.put(CTRL_N, "newProject");
		manager.setDocumentShortcuts(root);

		assertNull(manager.menuFactory.createJMenuItem("RibbonNewProject").getAccelerator());
	}

	private ExtMenuFactory factory(InputMap root) {
		ProjectMenuActionMap actions = new ProjectMenuActionMap() {
			@Override public javax.swing.Action getAction(String key) { return new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) { }
			}; }
			@Override public String getStringFromAction(javax.swing.Action action) { return "newProject"; }
		};
		ExtMenuFactory factory = new ExtMenuFactory(actions, new ListResourceBundle() {
			@Override protected Object[][] getContents() { return new Object[][] {
				{"new.action", "newProject"}, {"new.text", "New"}, {"new.accelerator", "Ctrl+N"},
				{"other.action", "other"}, {"other.text", "Other"}, {"other.accelerator", "Ctrl+K"}
			}; }
		});
		factory.setDocumentShortcuts(root);
		return factory;
	}
}
