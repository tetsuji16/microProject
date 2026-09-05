/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.diagnostics;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
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
	}
}
