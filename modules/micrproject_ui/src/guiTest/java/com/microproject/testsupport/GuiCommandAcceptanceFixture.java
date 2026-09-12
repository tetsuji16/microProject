/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.concurrent.Callable;

import com.microproject.ribbon.RibbonCommandResult;

/** Shared contract assertions for physical GUI command acceptance cases. */
public final class GuiCommandAcceptanceFixture {
	private GuiCommandAcceptanceFixture() {
	}

	public static void verifyMutation(String commandId, Callable<Void> physicalRoute,
		Supplier<RibbonCommandResult> result, BooleanSupplier modelAfter,
		BooleanSupplier viewAfter, Callable<Void> undo, BooleanSupplier modelBefore,
		Callable<Void> redo, BooleanSupplier modelRestored, Callable<Boolean> persistedAfter)
			throws Exception {
		Objects.requireNonNull(commandId, "commandId");
		Objects.requireNonNull(physicalRoute, "physicalRoute");
		physicalRoute.call();
		GuiAcceptanceSupport.await(modelAfter, commandId + " did not change the model");
		GuiAcceptanceSupport.await(viewAfter, commandId + " did not update the view");
		RibbonCommandResult outcome = result.get();
		assertNotNull(outcome, commandId + " did not publish a semantic result");
		assertTrue(outcome.status() == RibbonCommandResult.Status.CHANGED,
			commandId + " published " + outcome.status() + " instead of CHANGED");
		undo.call();
		GuiAcceptanceSupport.await(modelBefore, commandId + " Undo did not restore the model");
		redo.call();
		GuiAcceptanceSupport.await(modelRestored, commandId + " Redo did not restore the mutation");
		assertTrue(Boolean.TRUE.equals(persistedAfter.call()), commandId + " persistence roundtrip failed");
	}

	/** Verifies commands whose physical route first opens a modal confirmation UI. */
	public static void verifyDeferredDialog(String commandId, Callable<Void> physicalRoute,
			Supplier<RibbonCommandResult> dispatchedResult, BooleanSupplier dialogOpen,
			Callable<Void> confirmDialog, Supplier<RibbonCommandResult> changedResult,
			BooleanSupplier modelAfter, Callable<Void> undo, BooleanSupplier modelBefore,
			Callable<Void> redo, BooleanSupplier modelRestored, Callable<Boolean> persistedAfter)
				throws Exception {
		Objects.requireNonNull(commandId, "commandId");
		physicalRoute.call();
		GuiAcceptanceSupport.await(dialogOpen, commandId + " dialog did not open");
		RibbonCommandResult dispatched = dispatchedResult.get();
		assertNotNull(dispatched, commandId + " did not publish dispatched result");
		assertTrue(dispatched.status() == RibbonCommandResult.Status.DISPATCHED,
				commandId + " published " + dispatched.status() + " before confirmation");
		confirmDialog.call();
		GuiAcceptanceSupport.await(modelAfter, commandId + " confirmation did not change the model");
		RibbonCommandResult changed = changedResult.get();
		assertNotNull(changed, commandId + " did not publish changed result");
		assertTrue(changed.status() == RibbonCommandResult.Status.CHANGED,
				commandId + " published " + changed.status() + " after confirmation");
		undo.call();
		GuiAcceptanceSupport.await(modelBefore, commandId + " Undo did not restore the model");
		redo.call();
		GuiAcceptanceSupport.await(modelRestored, commandId + " Redo did not restore the mutation");
		assertTrue(Boolean.TRUE.equals(persistedAfter.call()), commandId + " persistence roundtrip failed");
	}
}
