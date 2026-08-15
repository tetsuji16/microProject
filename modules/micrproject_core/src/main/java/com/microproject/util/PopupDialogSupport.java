package com.microproject.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
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
		return showOptionDialog(parentComponent, message, title, optionType, messageType, null, null, null, escapeResult);
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
		dialog.setVisible(true);
		int result = normalizeOptionPaneValue(optionPane.getValue(), options, escapeResult);
		dialog.dispose();
		return result;
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
