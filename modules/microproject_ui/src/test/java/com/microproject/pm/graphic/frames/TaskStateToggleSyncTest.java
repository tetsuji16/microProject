/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.Action;

import org.junit.jupiter.api.Test;

import com.microproject.ribbon.CommandId;

/** The pressed state of the selected-task toggles follows the task model, not the click. */
class TaskStateToggleSyncTest {
	private static boolean pressed(HeadlessDocumentFixture fx, String actionId) {
		Action action = fx.frame.getMenuManager().getActionFromId(actionId);
		return Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
	}

	@Test
	void taskModeTogglesFollowSelectedTaskSchedulingMode() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second")) {
			fx.select(fx.task(0));
			fx.run(CommandId.TASK_MODE_MANUAL);
			assertEquals(true, pressed(fx, "TaskModeManual"));
			assertEquals(false, pressed(fx, "TaskModeAutomatic"));

			// Clicking the pressed button again keeps it pressed: the state is the model's.
			fx.run(CommandId.TASK_MODE_MANUAL);
			assertEquals(true, pressed(fx, "TaskModeManual"));

			fx.run(CommandId.TASK_MODE_AUTOMATIC);
			assertEquals(false, pressed(fx, "TaskModeManual"));
			assertEquals(true, pressed(fx, "TaskModeAutomatic"));

			fx.select(fx.task(0), fx.task(1));
			fx.run(CommandId.TASK_MODE_MANUAL);
			assertEquals(true, pressed(fx, "TaskModeManual"));
			fx.select(fx.task(0));
			fx.run(CommandId.TASK_MODE_AUTOMATIC);
			fx.select(fx.task(0), fx.task(1));
			fx.frame.syncTaskStateToggles();
			assertEquals(false, pressed(fx, "TaskModeManual"));
			assertEquals(false, pressed(fx, "TaskModeAutomatic"));
		}
	}

	@Test
	void inactivateToggleFollowsSelectedTaskActiveState() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second")) {
			fx.select(fx.task(0));
			assertEquals(false, pressed(fx, "InactivateTask"));
			fx.run(CommandId.INACTIVATE_TASK);
			assertEquals(true, pressed(fx, "InactivateTask"));

			fx.select(fx.task(0), fx.task(1));
			fx.frame.syncTaskStateToggles();
			assertEquals(false, pressed(fx, "InactivateTask"));

			fx.select(fx.task(1));
			fx.frame.syncTaskStateToggles();
			assertEquals(false, pressed(fx, "InactivateTask"));
			fx.select(fx.task(0));
			fx.frame.syncTaskStateToggles();
			assertEquals(true, pressed(fx, "InactivateTask"));
		}
	}

	private static String pressedPercent(HeadlessDocumentFixture fx) {
		StringBuilder pressedIds = new StringBuilder();
		for (int percent : new int[] { 0, 25, 50, 75, 100 })
			if (pressed(fx, "PercentComplete" + percent))
				pressedIds.append(percent).append(',');
		return pressedIds.toString();
	}

	@Test
	void percentCompleteTogglesPressOnlyTheMatchingValue() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second")) {
			fx.select(fx.task(0));
			fx.run(CommandId.PERCENT_COMPLETE_50);
			assertEquals("50,", pressedPercent(fx));
			fx.run(CommandId.PERCENT_COMPLETE_50);
			assertEquals("50,", pressedPercent(fx));
			fx.run(CommandId.PERCENT_COMPLETE_100);
			assertEquals("100,", pressedPercent(fx));
			fx.run(CommandId.PERCENT_COMPLETE_0);
			assertEquals("0,", pressedPercent(fx));
			fx.task(0).setPercentComplete(0.3D);
			fx.frame.syncTaskStateToggles();
			assertEquals("", pressedPercent(fx));
			fx.task(0).setPercentComplete(0.5D);
			fx.select(fx.task(0), fx.task(1));
			fx.frame.syncTaskStateToggles();
			assertEquals("", pressedPercent(fx));
		}
	}
}
