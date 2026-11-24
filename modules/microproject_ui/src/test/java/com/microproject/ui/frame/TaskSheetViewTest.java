/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.ui.menu.MenuActionConstants;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/** The Task Sheet is the Gantt task table with the chart collapsed; it must survive a save and reopen of the workspace. */
class TaskSheetViewTest {
	@Test
	void taskSheetCollapsesTheChartAndGanttRestoresIt() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First")) {
			assertFalse(fx.frame.getGanttView().isTaskSheet());
			HeadlessDocumentFixture.onEdt(() -> assertTrue(fx.frame.activateView(MenuActionConstants.ACTION_TASK_SHEET)));
			assertTrue(fx.frame.getGanttView().isTaskSheet());
			assertFalse(fx.frame.getGanttView().getRightScrollPane().isVisible());
			assertEquals(0, fx.frame.getGanttView().getDividerSize());
			assertEquals(MenuActionConstants.ACTION_TASK_SHEET, fx.frame.getTopViewId());

			HeadlessDocumentFixture.onEdt(fx.frame::activateGanttView);
			assertFalse(fx.frame.getGanttView().isTaskSheet());
			assertTrue(fx.frame.getGanttView().getRightScrollPane().isVisible());
			assertTrue(fx.frame.getGanttView().getDividerSize() > 0);
			assertEquals(MenuActionConstants.ACTION_GANTT, fx.frame.getTopViewId());
		}
	}

	@Test
	void taskSheetIsRestoredFromTheSerializedWorkspace() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First")) {
			HeadlessDocumentFixture.onEdt(() -> fx.frame.activateView(MenuActionConstants.ACTION_TASK_SHEET));
			AtomicReference<WorkspaceSetting> saved = new AtomicReference<>();
			HeadlessDocumentFixture.onEdt(() -> saved.set(fx.frame.createWorkspace(SavableToWorkspace.PERSIST)));

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
				out.writeObject(saved.get());
			}
			WorkspaceSetting loaded;
			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
				loaded = (WorkspaceSetting) in.readObject();
			}

			HeadlessDocumentFixture.onEdt(fx.frame::activateGanttView);
			assertFalse(fx.frame.getGanttView().isTaskSheet());
			HeadlessDocumentFixture.onEdt(() -> fx.frame.restoreWorkspace(loaded, SavableToWorkspace.PERSIST));
			assertEquals(MenuActionConstants.ACTION_TASK_SHEET, fx.frame.getTopViewId());
			assertTrue(fx.frame.getGanttView().isTaskSheet());
			assertFalse(fx.frame.getGanttView().getRightScrollPane().isVisible());
		}
	}
}
