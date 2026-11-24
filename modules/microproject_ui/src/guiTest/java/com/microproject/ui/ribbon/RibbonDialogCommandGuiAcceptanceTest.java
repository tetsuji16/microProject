/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.RibbonGuiSupport;
import com.microproject.ui.dialog.ResourceInformationDialog;
import com.microproject.ui.dialog.TaskInspectorPanel;
import com.microproject.ui.view.views.ResourceView;

/**
 * Physically exercises commands declared with {@code activation=DIALOG}. Most must open a
 * dialog/window; commands whose real contract is a view transition are checked against the active
 * view. The list comes from the resource, so additions are covered without copying the command list.
 */
class RibbonDialogCommandGuiAcceptanceTest {
	/** Commands that open something other than a Swing window; each needs a stated reason. */
	private static final Map<String, String> NOT_A_SWING_WINDOW = Map.of(
		"RibbonMicroProjectDocumentation", "opens the documentation in the system browser");

	/** Commands whose result is a pane docked in the document frame, not a window; the pane is the observable result. */
	private static final Map<String, Class<? extends java.awt.Component>> DOCKED_PANE = Map.of(
		"RibbonTaskInspector", TaskInspectorPanel.class);

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
					fixture.restoreGanttView();
					if ("RibbonInsertProject".equals(commandId)) fixture.saveCurrentProject();
					if ("RibbonUseResourcePool".equals(commandId)) fixture.prepareSavedResourcePoolProject();
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
		if ("RibbonResourceInformation".equals(commandId) || "RibbonResourceNotes".equals(commandId)) {
			AbstractButton resourcesView = fixture.showCommand("RibbonResources");
			assertNotNull(resourcesView, "Resource View command is unavailable");
			if (!fixture.isSelected(resourcesView)) RibbonGuiSupport.clickSplitPrimary(fixture.robot, resourcesView);
			GuiAcceptanceSupport.await(() -> fixture.manager.getCurrentFrame().getActiveTopView() instanceof ResourceView,
				"the physical Resource View command did not activate the resource sheet");
			fixture.clickRow(0);
		}
		AbstractButton button = fixture.showCommand(commandId);
		if (button == null) {
			assertTrue(RibbonCommandGuiFixture.isContextualOrPopupOnly(commandId),
				"standard ribbon command is missing: " + commandId);
			return; // Contextual and popup commands have separate physical-route coverage.
		}
		if (!fixture.isEnabled(button)) {
			fixture.clickRow(0);
			button = fixture.showCommand(commandId);
		}
		if (!fixture.isEnabled(button)) {
			// A resource command needs a selected resource.
			AbstractButton resources = fixture.showCommand("RibbonResources");
			RibbonGuiSupport.clickSplitPrimary(fixture.robot, resources);
			GuiAcceptanceSupport.await(() -> fixture.manager.getCurrentFrame().getActiveTopView() instanceof ResourceView,
				"the physical Resource View command did not activate the resource sheet");
			fixture.clickRow(0);
			button = fixture.showCommand(commandId);
		}
		if (!fixture.isEnabled(button)) {
			throw new AssertionError("the button stays disabled with a task or a resource selected");
		}
		Set<Window> before = fixture.showingWindows();
		ResourceInformationDialog resourceDialogBefore = "RibbonResourceNotes".equals(commandId)
			? fixture.visibleResourceInformationDialog() : null;
		String viewBefore = fixture.manager.getTopViewId();
		fixture.click(button);
		AbstractButton clicked = button;
		boolean viewSwitch = "RibbonDelegateTasks".equals(commandId) || "RibbonCustomReport".equals(commandId);
		if ("RibbonDelegateTasks".equals(commandId)) {
			GuiAcceptanceSupport.await(() -> "DelegateTasks".equals(fixture.manager.getTopViewId()),
				"Delegate Tasks did not activate the Team Planner view");
			assertFalse("DelegateTasks".equals(viewBefore), "the test must begin outside Team Planner");
		} else if ("RibbonCustomReport".equals(commandId)) {
			GuiAcceptanceSupport.await(() -> "Report".equals(fixture.manager.getTopViewId()),
				"Custom Report did not activate the Report view");
			assertFalse("Report".equals(viewBefore), "the test must begin outside Report");
		} else if ("RibbonUseResourcePool".equals(commandId)) {
			Window chooser = waitForWindow(fixture, before, "resource-pool project chooser did not open");
			fixture.pressKey(java.awt.event.KeyEvent.VK_ENTER);
			GuiAcceptanceSupport.await(() -> !showing(fixture, chooser), "the resource-pool chooser did not accept its saved project");
			Window precedence = waitForWindow(fixture, before, "the resource precedence dialog did not open");
			fixture.pressKey(java.awt.event.KeyEvent.VK_ENTER);
			GuiAcceptanceSupport.await(() -> fixture.manager.getCurrentFrame().getProject().getSharedResourcePoolFile() != null,
				"Use Resource Pool did not link the sharer to the saved pool");
			GuiAcceptanceSupport.await(() -> !showing(fixture, precedence), "the resource precedence dialog did not close");
			fixture.closeNewWindows(before);
		} else if ("RibbonResourceNotes".equals(commandId)) {
			GuiAcceptanceSupport.await(() -> {
				try {
					ResourceInformationDialog dialog = fixture.visibleResourceInformationDialog();
					return dialog != null && fixture.isResourceNotesTabSelected(dialog);
				} catch (Exception failure) {
					throw new AssertionError(failure);
				}
			}, "Resource Notes did not show the Resource Information dialog on its Notes tab");
			ResourceInformationDialog resourceDialog = fixture.visibleResourceInformationDialog();
			if (resourceDialogBefore != null) assertSame(resourceDialogBefore, resourceDialog,
				"Resource Notes should reuse the existing Resource Information dialog");
			fixture.closeWindow(resourceDialog);
			fixture.closeNewWindows(before);
		} else if (DOCKED_PANE.containsKey(commandId)) {
			Class<? extends java.awt.Component> pane = DOCKED_PANE.get(commandId);
			GuiAcceptanceSupport.await(() -> dockedPaneShowing(fixture, pane),
				commandId + " did not show its docked " + pane.getSimpleName());
			fixture.click(button);
			GuiAcceptanceSupport.await(() -> !dockedPaneShowing(fixture, pane),
				commandId + " did not hide its docked " + pane.getSimpleName() + " on the second click");
		} else {
			awaitWindow(fixture, before, "no dialog or window opened");
			fixture.closeNewWindows(before);
		}
		if (viewSwitch) {
			if ("RibbonDelegateTasks".equals(commandId))
				assertTrue(fixture.isSelected(clicked), "Team Planner must remain selected for its active view");
			else
				assertFalse(fixture.isSelected(clicked), "the Custom Report action item must release after switching views");
		} else {
			GuiAcceptanceSupport.await(() -> !pressed(fixture, clicked), "the button stays pressed after its dialog closed");
		}
	}

	private static Window waitForWindow(RibbonCommandGuiFixture fixture, Set<Window> before, String message) throws Exception {
		awaitWindow(fixture, before, message);
		try {
			return fixture.newWindow(before);
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
	}

	private static void awaitWindow(RibbonCommandGuiFixture fixture, Set<Window> before, String message) throws Exception {
		try {
			GuiAcceptanceSupport.await(() -> opened(fixture, before), message);
		} catch (AssertionError failure) {
			throw new AssertionError(message + "; showing windows opened by the route: "
				+ fixture.describeWindows(before) + "; " + fixture.describeActiveSelection() + "; "
				+ fixture.describeLastRouteDiagnostics(), failure);
		}
	}

	private static boolean dockedPaneShowing(RibbonCommandGuiFixture fixture, Class<? extends java.awt.Component> pane) {
		try {
			return fixture.onEdt(() -> com.microproject.ui.menu.testsupport.UiComponentWalker.flatten(fixture.window).stream()
				.anyMatch(component -> pane.isInstance(component) && component.isShowing()));
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
	}

	private static boolean showing(RibbonCommandGuiFixture fixture, Window window) {
		try {
			return fixture.isShowing(window);
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
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
