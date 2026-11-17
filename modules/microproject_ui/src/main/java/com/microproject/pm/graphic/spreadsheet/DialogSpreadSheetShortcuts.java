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
package com.microproject.pm.graphic.spreadsheet;

import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;

import com.microproject.menu.MenuActionConstants;

/**
 * Editing keys for spreadsheet tables hosted by a dialog.
 * <p>
 * The document shortcuts (Delete, Ctrl+X, Ctrl+C, Ctrl+V) are installed once on the
 * document root pane with {@code WHEN_IN_FOCUSED_WINDOW}
 * ({@code GraphicManager.applyMicrosoftShortcuts}). That map belongs to the document
 * window, so a table inside a {@code JDialog} never sees those keys: keys pressed in a
 * dialog resolve against the dialog's own window only. Microsoft Project still deletes,
 * cuts, copies and pastes rows of the tables it shows in dialogs (Resource Information,
 * Task Information), so this installs the same four keys on the dialog root pane and
 * routes each one to the dialog table's own canonical action, which is the same action
 * the row popup uses.
 * <p>
 * Keys are bound only on the dialog root pane, so the document registration stays the
 * single owner for document tables and no key fires twice.
 */
public final class DialogSpreadSheetShortcuts {
	private DialogSpreadSheetShortcuts() {
	}

	/** Installs the dialog table keys on {@code dialog}'s root pane. */
	public static void install(RootPaneContainer dialog) {
		if (dialog != null)
			install(dialog.getRootPane());
	}

	/** Installs the dialog table keys on a root pane; a dialog is a root-pane container. */
	public static void install(JRootPane rootPane) {
		if (rootPane == null)
			return;
		InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = rootPane.getActionMap();
		bind(inputMap, actionMap, rootPane, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
				MenuActionConstants.ACTION_DELETE);
		int menuMask = menuShortcutMask();
		bind(inputMap, actionMap, rootPane, KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask),
				MenuActionConstants.ACTION_CUT);
		bind(inputMap, actionMap, rootPane, KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask),
				MenuActionConstants.ACTION_COPY);
		bind(inputMap, actionMap, rootPane, KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask),
				MenuActionConstants.ACTION_PASTE);
	}

	private static void bind(InputMap inputMap, ActionMap actionMap, JRootPane rootPane, KeyStroke key,
			String actionId) {
		String command = "dialogSpreadSheet." + actionId;
		inputMap.put(key, command);
		actionMap.put(command, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				SpreadSheet sheet = activeSheet(rootPane);
				if (sheet != null && !sheet.isEditing())
					sheet.executeAction(actionId);
			}
		});
	}

	/**
	 * Resolves the table the keys act on: the focused table, or the table the dialog
	 * currently shows when focus sits on a button or a field of the dialog.
	 */
	static SpreadSheet activeSheet(JRootPane rootPane) {
		SpreadSheet focused = sheetAncestor(
				KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
		if (focused != null)
			return focused;
		return firstVisibleSheet(rootPane);
	}

	static SpreadSheet sheetAncestor(Component component) {
		for (Component candidate = component; candidate != null; candidate = candidate.getParent()) {
			if (candidate instanceof SpreadSheet sheet)
				return sheet;
		}
		return null;
	}

	private static SpreadSheet firstVisibleSheet(Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof SpreadSheet sheet && sheet.isVisible())
				return sheet;
			if (child instanceof Container nested) {
				SpreadSheet found = firstVisibleSheet(nested);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	/** The platform menu shortcut (Ctrl, or Command on macOS); headless-safe. */
	private static int menuShortcutMask() {
		try {
			return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		} catch (HeadlessException e) {
			return InputEvent.CTRL_DOWN_MASK;
		}
	}

}
