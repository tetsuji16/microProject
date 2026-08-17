/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject that the Software is furnished to "AS IS", WITHOUT
 * WARRANTY OF ANY KIND, either express or implied. INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.frames.workspace.NamedFrame;
import com.microproject.pm.graphic.frames.workspace.Workspace;
import com.microproject.workspace.WorkspaceSetting;

/**
 * Regression coverage for issue #47: the Microsoft Project keyboard shortcuts must
 * resolve on the document window's root pane. In the ribbon UI the menu-item
 * accelerators never dispatch, so the global (root-pane) shortcut layer is the only
 * path that makes Ctrl+X/C/V/Delete/Link/Unlink/Indent/Outdent/Information etc. work
 * from the keyboard.
 *
 * <p>The wiring is exercised through {@code GraphicManager.applyMicrosoftShortcuts},
 * which writes to an arbitrary InputMap/ActionMap (no window required). This test
 * drives that seam directly so it runs headless.
 */
class MicrosoftShortcutsRootPaneTest {

	/** A plain Swing component supplies the InputMap/ActionMap without a window. */
	private static final class ShortcutHarness {
		final JPanel panel = new JPanel();
		final InputMapCapturingGraphicManager manager;

		ShortcutHarness() {
			manager = new InputMapCapturingGraphicManager(panel);
		}

		Object bindingFor(KeyStroke key) {
			return panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key);
		}
	}

	private static final class InputMapCapturingGraphicManager extends GraphicManager {
		private final FrameManager frameManager = new StubFrameManager();

		InputMapCapturingGraphicManager(JPanel panel) {
			super(panel);
			// MenuManager is lazily created by the production wiring; resolve it so the
			// shortcut action lookup (menuManager.getActionFromId) has something to read.
			getMenuManager();
		}

		@Override
		public FrameManager getFrameManager() {
			return frameManager;
		}
	}

	// Minimal FrameManager stub so the document window can be activated without a real desktop.
	private static final class StubFrameManager implements FrameManager {
		private static final long serialVersionUID = 1L;
		private final Workspace workspace = new Workspace();

		@Override public void showFrame(NamedFrame frame) { }
		@Override public void addFrame(NamedFrame frame) { }
		@Override public void removeFrame(NamedFrame frame) { }
		@Override public Workspace getWorkspace() { return workspace; }
		@Override public void activateFrame(NamedFrame frame) { }
		@Override public java.awt.Component getSelectedFrame() { return null; }
		@Override public void setTabTitle(NamedFrame frame, String tabTitle) { }
		@Override public void update() { }
		@Override public java.util.AbstractList getAllFrames() {
			return new java.util.AbstractList<Object>() {
				@Override public Object get(int index) { return null; }
				@Override public int size() { return 0; }
			};
		}
		@Override public WorkspaceSetting createWorkspace(int arg) { return null; }
		@Override public void restoreWorkspace(WorkspaceSetting setting, int context) { }
	}

	@Test
	void microsoftShortcutsResolveOnRootPane() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ShortcutHarness harness = new ShortcutHarness();
			harness.manager.applyMicrosoftShortcuts(
					harness.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW),
					harness.panel.getActionMap());

			int ctrl = InputEvent.CTRL_DOWN_MASK;

			assertEquals(MenuActionConstants.ACTION_CUT,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_X, ctrl)), "Ctrl+X must cut");
			assertEquals(MenuActionConstants.ACTION_COPY,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrl)), "Ctrl+C must copy");
			assertEquals(MenuActionConstants.ACTION_PASTE,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_V, ctrl)), "Ctrl+V must paste");
			assertEquals(MenuActionConstants.ACTION_FILL_DOWN,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_D, ctrl)), "Ctrl+D must fill down");
			assertEquals(MenuActionConstants.ACTION_LINK,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_F2, ctrl)), "Ctrl+F2 must link");
			assertEquals(MenuActionConstants.ACTION_UNLINK,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_F2, ctrl | InputEvent.SHIFT_DOWN_MASK)),
					"Ctrl+Shift+F2 must unlink");

			assertEquals(MenuActionConstants.ACTION_GOTO,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)), "F5 must go to");
			assertEquals(MenuActionConstants.ACTION_FIND,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK)),
					"Shift+F5 must find");
			assertEquals(MenuActionConstants.ACTION_NEW,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0)), "Insert must add a task");
			assertEquals(MenuActionConstants.ACTION_DELETE,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)), "Delete must delete");
			assertEquals("EditField",
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)), "F2 must edit the field");
			assertEquals(MenuActionConstants.ACTION_INFORMATION,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.SHIFT_DOWN_MASK)),
					"Shift+F2 must open information");

			// Microsoft Project outline keys
			assertEquals(MenuActionConstants.ACTION_INDENT,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
							InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)),
					"Alt+Shift+Right must indent");
			assertEquals(MenuActionConstants.ACTION_OUTDENT,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
							InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)),
					"Alt+Shift+Left must outdent");
			assertEquals(MenuActionConstants.ACTION_EXPAND,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
							InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)),
					"Alt+Shift+= must expand");
			assertEquals(MenuActionConstants.ACTION_COLLAPSE,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
							InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)),
					"Alt+Shift+- must collapse");

			// Microsoft Project selection / row shortcuts
			assertEquals("SelectRow",
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK)),
					"Ctrl+Space must select the row");
			assertEquals("SelectColumn",
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK)),
					"Shift+Space must select the column");
			assertEquals("SelectAll",
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)),
					"Ctrl+Shift+Space must select the whole sheet");
			assertEquals(MenuActionConstants.ACTION_DELETE,
					harness.bindingFor(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK)),
					"Ctrl+Minus must delete the selected row");

			// The bound action objects must actually exist in the action map.
			assertNotNull(harness.panel.getActionMap().get(MenuActionConstants.ACTION_CUT));
			assertNotNull(harness.panel.getActionMap().get(MenuActionConstants.ACTION_PASTE));
			assertNotNull(harness.panel.getActionMap().get(MenuActionConstants.ACTION_LINK));
			assertNotNull(harness.panel.getActionMap().get(MenuActionConstants.ACTION_INDENT));
		});
	}
}
