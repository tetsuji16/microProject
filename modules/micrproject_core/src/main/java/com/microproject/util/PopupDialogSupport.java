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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public final class PopupDialogSupport {
	static final String ESCAPE_ACTION_KEY = "projectlibre.closeOnEscape";

	private PopupDialogSupport() {
	}

	public static void bindEscapeToDispose(JDialog dialog) {
		bindEscape(dialog.getRootPane(), new Runnable() {
			public void run() {
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
	}

	public static void bindEscapeToOptionPane(JDialog dialog, JOptionPane optionPane, int escapeResult) {
		bindEscape(dialog.getRootPane(), new Runnable() {
			public void run() {
				optionPane.setValue(Integer.valueOf(escapeResult));
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
	}

	public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
		return showConfirmDialog(parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE, JOptionPane.CLOSED_OPTION);
	}

	public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType, int messageType) {
		return showConfirmDialog(parentComponent, message, title, optionType, messageType, JOptionPane.CLOSED_OPTION);
	}

	public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType, int messageType, int escapeResult) {
		Object[] options = createConfirmationOptions(optionType);
		if (options != null) {
			// Preserve the established safe default: confirmation dialogs focus No rather
			// than accepting a destructive action when Enter is pressed.
			return showOptionDialog(parentComponent, message, title, optionType, messageType, null, options, options[1], escapeResult);
		}
		return showOptionDialog(parentComponent, message, title, optionType, messageType, null, null, null, escapeResult);
	}

	private static Object[] createConfirmationOptions(int optionType) {
		if (optionType != JOptionPane.YES_NO_OPTION && optionType != JOptionPane.YES_NO_CANCEL_OPTION) {
			return null;
		}
		JButton yes = confirmationButton("dialog.yes", "Yes (Y)", KeyEvent.VK_Y);
		JButton no = confirmationButton("dialog.no", "No (N)", KeyEvent.VK_N);
		if (optionType == JOptionPane.YES_NO_CANCEL_OPTION) {
			return new Object[] { yes, no, confirmationButton("dialog.cancel", "Cancel (C)", KeyEvent.VK_C) };
		}
		return new Object[] { yes, no };
	}

	private static JButton confirmationButton(String key, String fallback, int mnemonic) {
		JButton button = new JButton(localized(key, fallback));
		button.setMnemonic(mnemonic);
		return button;
	}

	private static String localized(String key, String fallback) {
		try {
			return java.util.ResourceBundle.getBundle("com.microproject.dialog.usability").getString(key);
		} catch (Exception ignored) {
			return fallback;
		}
	}

	public static int showOptionDialog(Component parentComponent, Object message, String title, int optionType, int messageType,
		Icon icon, Object[] options, Object initialValue) {
		return showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue, JOptionPane.CLOSED_OPTION);
	}

	public static int showOptionDialog(Component parentComponent, Object message, String title, int optionType, int messageType,
		Icon icon, Object[] options, Object initialValue, int escapeResult) {
		JOptionPane optionPane = new JOptionPane(message, messageType, optionType, icon, options, initialValue);
		JDialog dialog = optionPane.createDialog(parentComponent, title);
		bindEscapeToOptionPane(dialog, optionPane, escapeResult);
		bindConfirmationMnemonics(dialog.getRootPane(), options);
		dialog.setVisible(true);
		int result = normalizeOptionPaneValue(optionPane.getValue(), options, escapeResult);
		dialog.dispose();
		return result;
	}

	/**
	 * Makes the conventional Windows/MS Project confirmation keys available at the
	 * dialog root. JButton mnemonics normally supply these bindings, but installing
	 * them here keeps Alt+Y/Alt+N reliable across look-and-feels and custom option
	 * panes that place the buttons in a nested container. The unmodified key is
	 * also accepted, matching the traditional Windows/MS Project confirmation
	 * behavior indicated by labels such as {@code Yes (Y)} and {@code No (N)}.
	 */
	static void bindConfirmationMnemonics(JRootPane rootPane, Object[] options) {
		if (options == null) return;
		for (Object option : options) {
			if (!(option instanceof JButton)) continue;
			JButton button = (JButton) option;
			int mnemonic = button.getMnemonic();
			if (mnemonic == KeyEvent.VK_UNDEFINED) continue;
			String actionKey = "microproject.option." + mnemonic;
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(mnemonic, 0), actionKey);
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(mnemonic, InputEvent.ALT_DOWN_MASK), actionKey);
			rootPane.getActionMap().put(actionKey, new javax.swing.AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent event) {
					if (button.isEnabled()) button.doClick();
				}
			});
		}
	}

	public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
		JOptionPane optionPane = new JOptionPane(message, messageType, JOptionPane.DEFAULT_OPTION);
		JDialog dialog = optionPane.createDialog(parentComponent, title);
		bindEscapeToOptionPane(dialog, optionPane, JOptionPane.CLOSED_OPTION);
		dialog.setVisible(true);
		dialog.dispose();
	}

	static void bindEscape(JRootPane rootPane, Runnable onEscape) {
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCAPE_ACTION_KEY);
		rootPane.getActionMap().put(ESCAPE_ACTION_KEY, new javax.swing.AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				onEscape.run();
			}
		});
	}

	static int normalizeOptionPaneValue(Object value, Object[] options, int escapeResult) {
		if (value == null || value == JOptionPane.UNINITIALIZED_VALUE) {
			return escapeResult;
		}
		if (value instanceof Integer) {
			return ((Integer) value).intValue();
		}
		if (options != null) {
			for (int i = 0; i < options.length; i++) {
				Object option = options[i];
				if (value == option || (value != null && value.equals(option))) {
					return i;
				}
			}
		}
		return escapeResult;
	}
}
