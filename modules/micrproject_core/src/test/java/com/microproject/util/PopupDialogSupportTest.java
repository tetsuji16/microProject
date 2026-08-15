package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

class PopupDialogSupportTest {
	@Test
	void bindEscapeRegistersFocusedWindowKeyBindingAndRunsCallback() {
		JRootPane rootPane = new JRootPane();
		AtomicBoolean invoked = new AtomicBoolean(false);

		PopupDialogSupport.bindEscape(rootPane, new Runnable() {
			public void run() {
				invoked.set(true);
			}
		});

		Object bindingKey = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
		assertEquals(PopupDialogSupport.ESCAPE_ACTION_KEY, bindingKey);

		Action action = rootPane.getActionMap().get(PopupDialogSupport.ESCAPE_ACTION_KEY);
		assertNotNull(action);
		action.actionPerformed(new ActionEvent(rootPane, ActionEvent.ACTION_PERFORMED, "escape"));

		assertTrue(invoked.get());
	}

	@Test
	void normalizeOptionPaneValueFallsBackToEscapeResultWhenUnset() {
		assertEquals(JOptionPane.CANCEL_OPTION, PopupDialogSupport.normalizeOptionPaneValue(null, null, JOptionPane.CANCEL_OPTION));
		assertEquals(JOptionPane.CANCEL_OPTION,
			PopupDialogSupport.normalizeOptionPaneValue(JOptionPane.UNINITIALIZED_VALUE, null, JOptionPane.CANCEL_OPTION));
		assertEquals(JOptionPane.YES_OPTION,
			PopupDialogSupport.normalizeOptionPaneValue(Integer.valueOf(JOptionPane.YES_OPTION), null, JOptionPane.CANCEL_OPTION));
		assertEquals(1,
			PopupDialogSupport.normalizeOptionPaneValue("Discard My Changes", new Object[] { "Restore and Save", "Discard My Changes", "Save Copy" }, JOptionPane.CANCEL_OPTION));
	}
}
