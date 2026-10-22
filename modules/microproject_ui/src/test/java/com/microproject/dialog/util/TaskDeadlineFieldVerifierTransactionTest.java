/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.field.Field;
import com.microproject.field.ObjectRef;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.options.CalendarOption;
import com.microproject.options.EditOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class TaskDeadlineFieldVerifierTransactionTest {
	@Test
	void throwingObserverDuringDeadlineFieldVerificationRollsBackPartialEdit() throws Exception {
		AtomicInteger observerCalls = new AtomicInteger();
		RuntimeException injected = new IllegalStateException("injected deadline observer failure");
		RuntimeException[] observedFailure = new RuntimeException[1];
		long[] deadlineValues = new long[2];
		boolean[] dirtyValues = new boolean[2];
		long[] revisions = new long[2];
		boolean[] undoValues = new boolean[2];
		boolean[] redoValues = new boolean[2];

		SwingUtilities.invokeAndWait(() -> {
			DataFactoryUndoController undoController = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("deadline-verifier-test", undoController);
			Project project = Project.createProject(pool, undoController);
			project.initialize(false, false);
			NormalTask task = project.createScriptedTask();
			project.connectTask(task);
			long initialDeadline = CalendarOption.getInstance().makeValidEnd(4L * day(), false);
			task.setDeadline(initialDeadline);
			task.setDirty(false);
			Field field = Configuration.getFieldFromId("Field.deadline");
			DateFormat dateFormat = EditOption.getInstance().getDateFormat();
			String initialText = dateFormat.format(new Date(task.getDeadline()));
			String changedText = dateFormat.format(new Date(task.getDeadline() + 2L * day()));
			JTextField input = new JTextField(initialText);
			ObjectRef objectRef = objectRef(task, project.getTaskOutline().getDataFactory());
			input.setInputVerifier(new FieldVerifier(field, objectRef, initialText));
			project.addObjectListener(event -> {
				observerCalls.incrementAndGet();
				throw injected;
			});

			deadlineValues[0] = task.getDeadline();
			dirtyValues[0] = task.isDirty();
			revisions[0] = project.getMutationState().revision();
			undoValues[0] = undoController.canUndo();
			redoValues[0] = undoController.canRedo();
			input.setText(changedText);
			try {
				input.getInputVerifier().verify(input);
			} catch (RuntimeException failure) {
				observedFailure[0] = failure;
			}
			deadlineValues[1] = task.getDeadline();
			dirtyValues[1] = task.isDirty();
			revisions[1] = project.getMutationState().revision();
			undoValues[1] = undoController.canUndo();
			redoValues[1] = undoController.canRedo();
		});

		assertSame(injected, observedFailure[0]);
		assertEquals(1, observerCalls.get());
		assertNotEquals(0L, deadlineValues[0]);
		assertEquals(deadlineValues[0], deadlineValues[1], "failed verifier restores the deadline");
		assertFalse(dirtyValues[0]);
		assertEquals(dirtyValues[0], dirtyValues[1], "failed verifier restores dirty state");
		assertEquals(revisions[0], revisions[1]);
		assertEquals(undoValues[0], undoValues[1]);
		assertEquals(redoValues[0], redoValues[1]);
	}

	@Test
	void successfulDeadlineVerificationPostsUndoableFieldEdit() throws Exception {
		long[] deadlines = new long[3];
		boolean[] canUndo = new boolean[3];
		boolean[] canRedo = new boolean[3];
		boolean[] verified = new boolean[1];
		long[] reloadedDeadline = new long[1];
		SwingUtilities.invokeAndWait(() -> {
			DataFactoryUndoController undoController = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("deadline-verifier-undo-test", undoController);
			Project project = Project.createProject(pool, undoController);
			project.initialize(false, false);
			NormalTask task = project.createScriptedTask();
			project.connectTask(task);
			long initialDeadline = CalendarOption.getInstance().makeValidEnd(4L * day(), false);
			task.setDeadline(initialDeadline);
			task.setDirty(false);
			Field field = Configuration.getFieldFromId("Field.deadline");
			DateFormat dateFormat = EditOption.getInstance().getDateFormat();
			String initialText = dateFormat.format(new Date(task.getDeadline()));
			String changedText = dateFormat.format(new Date(task.getDeadline() + 2L * day()));
			JTextField input = new JTextField(initialText);
			ObjectRef objectRef = objectRef(task, project.getTaskOutline().getDataFactory());
			input.setInputVerifier(new FieldVerifier(field, objectRef, initialText));
			deadlines[0] = task.getDeadline();
			input.setText(changedText);
			verified[0] = input.getInputVerifier().verify(input);
			deadlines[1] = task.getDeadline();
			canUndo[0] = undoController.canUndo();
			canRedo[0] = undoController.canRedo();
			undoController.undo();
			deadlines[2] = task.getDeadline();
			canUndo[1] = undoController.canUndo();
			canRedo[1] = undoController.canRedo();
			undoController.redo();
			assertEquals(deadlines[1], task.getDeadline());
			try {
				ByteArrayOutputStream mpo = new ByteArrayOutputStream();
				MpoFileImporter importer = new MpoFileImporter();
				assertTrue(importer.saveProject(project, mpo));
				Project reopened = importer.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
				for (var reopenedTask : reopened.getTaskList()) {
					if (task.getName().equals(reopenedTask.getName())) {
						reloadedDeadline[0] = reopenedTask.getDeadline();
						break;
					}
				}
			} catch (Exception failure) {
				throw new AssertionError("MPO deadline round-trip failed", failure);
			}
			canUndo[2] = undoController.canUndo();
			canRedo[2] = undoController.canRedo();
		});
		assertTrue(verified[0]);
		assertNotEquals(deadlines[0], deadlines[1]);
		assertEquals(deadlines[0], deadlines[2]);
		assertEquals(deadlines[1], reloadedDeadline[0], "MPO reload preserves the verified deadline");
		assertTrue(canUndo[0]);
		assertFalse(canRedo[0]);
		assertTrue(canRedo[1]);
		assertTrue(canUndo[2]);
	}

	private static ObjectRef objectRef(Object object, NodeModelDataFactory dataFactory) {
		return new ObjectRef() {
			@Override public Node getNode() { return null; }
			@Override public WalkersNodeModel getNodeModel() { return null; }
			@Override public Object getObject() { return object; }
			@Override public Collection<?> getCollection() { return null; }
			@Override public NodeModelDataFactory getDataFactory() { return dataFactory; }
		};
	}

	private static long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}
}
