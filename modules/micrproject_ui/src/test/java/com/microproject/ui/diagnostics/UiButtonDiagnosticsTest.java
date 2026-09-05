/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.diagnostics;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UiButtonDiagnosticsTest {
	@AfterEach
	void clearDebugProperty() {
		System.clearProperty("microproject.ui.debug");
	}

	@Test
	void leavesActionsUntouchedOutsideDebugMode() {
		Action delegate = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent event) { }
		};

		assertSame(delegate, UiButtonDiagnostics.wrapAction("TestButton", delegate));
	}

	@Test
	void delegatesSuccessfulAndFailingButtonActionsInDebugMode() {
		System.setProperty("microproject.ui.debug", "true");
		Logger logger = Logger.getLogger(UiButtonDiagnostics.class.getName());
		StringBuilder messages = new StringBuilder();
		Handler handler = new Handler() {
			@Override public void publish(LogRecord record) { messages.append(record.getMessage()).append('\n'); }
			@Override public void flush() { }
			@Override public void close() { }
		};
		logger.addHandler(handler);
		logger.setLevel(Level.FINE);
		try {
			AtomicBoolean invoked = new AtomicBoolean();
			Action successful = new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) { invoked.set(true); }
			};

			Action traced = UiButtonDiagnostics.wrapAction("TestButton", successful);
			assertNotSame(successful, traced);
			traced.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "TestButton"));
			assertTrue(invoked.get());

			Action failing = new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) { throw new IllegalStateException("expected"); }
			};
			assertThrows(IllegalStateException.class, () -> UiButtonDiagnostics.wrapAction("FailingButton", failing)
				.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "FailingButton")));
			assertTrue(messages.toString().contains("UI_BUTTON action-start id=TestButton"));
			assertTrue(messages.toString().contains("command=TestButton"));
			assertTrue(messages.toString().contains("stateChanged=false"));
			assertTrue(messages.toString().contains("UI_COMMAND id=TestButton"));
			assertTrue(messages.toString().contains("modelBefore="));
			assertTrue(messages.toString().contains("viewAfter="));
			assertTrue(messages.toString().contains("modelChanged=false"));
			assertTrue(messages.toString().contains("viewChanged=false"));
			assertTrue(messages.toString().contains("undoChanged=false"));
			assertTrue(messages.toString().contains("UI_COMMAND_FAILURE id=TestButton reason=no-observable-state-change"));
			assertTrue(messages.toString().contains("UI_BUTTON_FAILURE id=FailingButton"));
		} finally {
			logger.removeHandler(handler);
		}
	}

	@Test
	void doesNotDelegateWhenThePhysicalSourceIsDisabled() {
		System.setProperty("microproject.ui.debug", "true");
		AtomicBoolean invoked = new AtomicBoolean();
		Action delegate = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent event) { invoked.set(true); }
		};
		javax.swing.JButton button = new javax.swing.JButton();
		button.setEnabled(false);
		UiButtonDiagnostics.wrapAction("DisabledButton", delegate)
			.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "DisabledButton"));
		assertFalse(invoked.get());
	}
}
