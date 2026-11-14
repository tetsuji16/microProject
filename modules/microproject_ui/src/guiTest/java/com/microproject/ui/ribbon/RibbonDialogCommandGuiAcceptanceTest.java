/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.GuiAcceptanceSupport;

/**
 * Every command that {@code menuInternal.properties} declares with {@code activation=DIALOG} must
 * open a dialog or window when clicked, and its button must not stay pressed afterwards. The list of
 * commands comes from the resource, so a new DIALOG command is covered without editing this test.
 */
class RibbonDialogCommandGuiAcceptanceTest {
	/** Commands that open something other than a Swing window; each needs a stated reason. */
	private static final Map<String, String> NOT_A_SWING_WINDOW = Map.of(
		"RibbonMicroProjectDocumentation", "opens the documentation in the system browser");

	@Test
	void everyDialogCommandOpensAWindowAndReleasesItsButton() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required");
		List<String> failures = new ArrayList<>();
		List<String> commands = RibbonCommandGuiFixture.commandsWithActivation("DIALOG");
		assertFalse(commands.isEmpty(), "menuInternal.properties declares no DIALOG command");
		try (RibbonCommandGuiFixture fixture = RibbonCommandGuiFixture.open(3)) {
			for (String commandId : commands) {
				if (NOT_A_SWING_WINDOW.containsKey(commandId)) continue;
				try {
					check(fixture, commandId);
				} catch (AssertionError | Exception failure) {
					failures.add(commandId + ": " + failure.getMessage());
					fixture.closeNewWindows(Set.of(fixture.window));
				}
			}
		}
		assertTrue(failures.isEmpty(), "DIALOG commands that did not behave:\n" + String.join("\n", failures));
	}

	private static void check(RibbonCommandGuiFixture fixture, String commandId) throws Exception {
		AbstractButton button = fixture.showCommand(commandId);
		if (button == null) return; // only on a contextual tab that this view does not show
		if (!fixture.isEnabled(button)) {
			fixture.clickRow(0);
			button = fixture.showCommand(commandId);
		}
		if (!fixture.isEnabled(button)) {
			// A resource command needs a selected resource.
			AbstractButton resources = fixture.showCommand("RibbonResources");
			fixture.click(resources);
			fixture.clickRow(0);
			button = fixture.showCommand(commandId);
		}
		if (!fixture.isEnabled(button)) {
			throw new AssertionError("the button stays disabled with a task or a resource selected");
		}
		Set<Window> before = fixture.showingWindows();
		fixture.click(button);
		AbstractButton clicked = button;
		GuiAcceptanceSupport.await(() -> opened(fixture, before), "no dialog or window opened");
		fixture.closeNewWindows(before);
		GuiAcceptanceSupport.await(() -> !pressed(fixture, clicked), "the button stays pressed after its dialog closed");
	}

	private static boolean opened(RibbonCommandGuiFixture fixture, Set<Window> before) {
		try {
			return fixture.newWindow(before) != null;
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
	}

	private static boolean pressed(RibbonCommandGuiFixture fixture, AbstractButton button) {
		try {
			return fixture.isSelected(button);
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
	}
}
