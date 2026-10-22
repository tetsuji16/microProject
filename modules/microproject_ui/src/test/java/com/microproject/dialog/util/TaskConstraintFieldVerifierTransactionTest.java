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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
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
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.undo.DataFactoryUndoController;

class TaskConstraintFieldVerifierTransactionTest {
	@Test
	void constraintDateNormalizationNoopDoesNotPublishOrCreateUndo() throws Exception {
		AtomicInteger observerCalls = new AtomicInteger();
		long[] candidate = new long[1];
		long[] revision = new long[2];
		boolean[][] dirtyUndoRedo = new boolean[2][3];
		SwingUtilities.invokeAndWait(() -> {
			DataFactoryUndoController undo = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("constraint-date-normalization-test", undo);
			Project project = Project.createProject(pool, undo);
			project.initialize(false, false);
			NormalTask task = project.createScriptedTask();
			task.setName("Normalized constraint date");
			project.connectTask(task);
			long original = CalendarOption.getInstance().makeValidEnd(4L * day(), false);
			task.setScheduleConstraint(ConstraintType.Kind.SNET, original);
			task.setDirty(false);
			Field field = Configuration.getFieldFromId("Field.constraintDate");
			ObjectRef ref = objectRef(task, project.getTaskOutline().getDataFactory());
			ExtDateField dateField = ComponentFactory.createDateField(field);
			long hour = 60L * 60L * 1000L;
			for (long offset = -24L * hour; offset <= 24L * hour; offset += 30L * 60L * 1000L) {
				long raw = original + offset;
				if (raw != original && raw != 0L
						&& dateField.getDateFormat().format(new Date(raw)).equals(dateField.getDateFormat().format(new Date(original)))
						&& task.getEffectiveWorkCalendar().adjustInsideCalendar(raw, false) == original) {
					candidate[0] = raw;
					break;
				}
			}
			assertNotEquals(0L, candidate[0], "fixture must find a distinct raw date normalized to the current constraint");
			dateField.setValue(new Date(original));
			JTextField input = dateField.getFormattedTextField();
			FieldVerifier verifier = new FieldVerifier(field, ref, new Date(original));
			input.setInputVerifier(verifier);
			project.addObjectListener(event -> observerCalls.incrementAndGet());
			revision[0] = project.getMutationState().revision();
			dirtyUndoRedo[0][0] = task.isDirty();
			dirtyUndoRedo[0][1] = undo.canUndo();
			dirtyUndoRedo[0][2] = undo.canRedo();
			dateField.setValue(new Date(candidate[0]));
			revision[1] = project.getMutationState().revision();
			dirtyUndoRedo[1][0] = task.isDirty();
			dirtyUndoRedo[1][1] = undo.canUndo();
			dirtyUndoRedo[1][2] = undo.canRedo();
			assertEquals(original, task.getConstraintDate(), "normalization no-op candidate=" + candidate[0]
				+ ", normalized=" + task.getEffectiveWorkCalendar().adjustInsideCalendar(candidate[0], false)
				+ ", verifierValue=" + verifier.value + ", inputValue=" + dateField.getDateValue()
				+ ", constraintType=" + task.getConstraintType());
		});
		assertEquals(0, observerCalls.get());
		assertEquals(revision[0], revision[1]);
		assertEquals(dirtyUndoRedo[0][0], dirtyUndoRedo[1][0]);
		assertFalse(dirtyUndoRedo[1][0]);
		assertEquals(dirtyUndoRedo[0][1], dirtyUndoRedo[1][1]);
		assertFalse(dirtyUndoRedo[1][1]);
		assertEquals(dirtyUndoRedo[0][2], dirtyUndoRedo[1][2]);
		assertFalse(dirtyUndoRedo[1][2]);
	}

	@Test
	void unchangedBlankDateFieldDoesNotCommitZeroDate() throws Exception {
		AtomicInteger observerCalls = new AtomicInteger();
		boolean[] verified = new boolean[1];
		long[] revision = new long[2];
		boolean[][] dirtyUndoRedo = new boolean[2][3];
		SwingUtilities.invokeAndWait(() -> {
			DataFactoryUndoController undo = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("blank-date-verifier-test", undo);
			Project project = Project.createProject(pool, undo);
			project.initialize(false, false);
			NormalTask task = project.createScriptedTask();
			task.setName("Blank deadline");
			project.connectTask(task);
			task.setDirty(false);
			Field field = Configuration.getFieldFromId("Field.deadline");
			ObjectRef ref = objectRef(task, project.getTaskOutline().getDataFactory());
			ExtDateField dateField = (ExtDateField) ComponentFactory.componentFor(field, ref, 0);
			JTextField input = dateField.getFormattedTextField();
			project.addObjectListener(event -> observerCalls.incrementAndGet());
			revision[0] = project.getMutationState().revision();
			dirtyUndoRedo[0][0] = task.isDirty();
			dirtyUndoRedo[0][1] = undo.canUndo();
			dirtyUndoRedo[0][2] = undo.canRedo();
			verified[0] = input.getInputVerifier().verify(input);
			revision[1] = project.getMutationState().revision();
			dirtyUndoRedo[1][0] = task.isDirty();
			dirtyUndoRedo[1][1] = undo.canUndo();
			dirtyUndoRedo[1][2] = undo.canRedo();
			assertEquals(0L, task.getDeadline());
		});
		assertTrue(verified[0]);
		assertEquals(0, observerCalls.get());
		assertEquals(revision[0], revision[1]);
		assertEquals(dirtyUndoRedo[0][0], dirtyUndoRedo[1][0]);
		assertFalse(dirtyUndoRedo[1][0]);
		assertEquals(dirtyUndoRedo[0][1], dirtyUndoRedo[1][1]);
		assertFalse(dirtyUndoRedo[1][1]);
		assertEquals(dirtyUndoRedo[0][2], dirtyUndoRedo[1][2]);
		assertFalse(dirtyUndoRedo[1][2]);
	}

	@Test
	void throwingObserverDuringConstraintDateVerificationRollsBackTransaction() throws Exception {
		AtomicInteger observerCalls = new AtomicInteger();
		RuntimeException injected = new IllegalStateException("injected constraint observer failure");
		RuntimeException[] observedFailure = new RuntimeException[1];
		long[][] state = new long[2][9];
		boolean[][] dirty = new boolean[2][2];
		boolean[][] undoRedo = new boolean[2][2];

		SwingUtilities.invokeAndWait(() -> {
			DataFactoryUndoController undo = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("constraint-verifier-test", undo);
			Project project = Project.createProject(pool, undo);
			project.initialize(false, false);
			NormalTask task = project.createScriptedTask();
			task.setName("Constraint target");
			project.connectTask(task);
			NormalTask unrelated = project.createScriptedTask();
			unrelated.setName("Unrelated task");
			project.connectTask(unrelated);
			long initialDate = CalendarOption.getInstance().makeValidEnd(4L * day(), false);
			task.setScheduleConstraint(ConstraintType.Kind.SNET, initialDate);
			task.setDirty(false);
			unrelated.setDirty(false);
			Field field = Configuration.getFieldFromId("Field.constraintDate");
			DateFormat format = EditOption.getInstance().getDateFormat();
			String initialText = format.format(new Date(task.getConstraintDate()));
			String newText = format.format(new Date(task.getConstraintDate() + 2L * day()));
			JTextField input = new JTextField(initialText);
			input.setInputVerifier(new FieldVerifier(field, objectRef(task, project.getTaskOutline().getDataFactory()), initialText));
			project.addObjectListener(event -> {
				observerCalls.incrementAndGet();
				throw injected;
			});
			capture(state[0], dirty[0], undoRedo[0], task, unrelated, project, undo);
			input.setText(newText);
			try {
				input.getInputVerifier().verify(input);
			} catch (RuntimeException failure) {
				observedFailure[0] = failure;
			}
			capture(state[1], dirty[1], undoRedo[1], task, unrelated, project, undo);
		});

		assertSame(injected, observedFailure[0]);
		assertEquals(1, observerCalls.get());
		assertEquals(state[0][0], state[1][0], "constraint type rolls back");
		assertEquals(state[0][1], state[1][1], "constraint date rolls back");
		assertEquals(state[0][2], state[1][2], "target start rolls back");
		assertEquals(state[0][3], state[1][3], "target finish rolls back");
		assertEquals(state[0][4], state[1][4], "unrelated task schedule remains unchanged");
		assertEquals(state[0][5], state[1][5], "unrelated task finish remains unchanged");
		assertEquals(state[0][6], state[1][6], "unrelated task constraint type remains unchanged");
		assertEquals(state[0][7], state[1][7], "unrelated task constraint date remains unchanged");
		assertEquals(state[0][8], state[1][8], "project revision remains unchanged");
		assertEquals(dirty[0][0], dirty[1][0]);
		assertEquals(dirty[0][1], dirty[1][1]);
		assertEquals(undoRedo[0][0], undoRedo[1][0]);
		assertEquals(undoRedo[0][1], undoRedo[1][1]);
	}

	@Test
	void successfulConstraintDateVerificationSupportsUndoRedoAndMpoReload() throws Exception {
		long[][] dates = new long[3][2];
		long[] identity = new long[1];
		long[] unrelatedStart = new long[1];
		boolean[] verified = new boolean[1];
		boolean[][] undoRedo = new boolean[3][2];
		long[] reloadedDate = new long[1];
		SwingUtilities.invokeAndWait(() -> {
			DataFactoryUndoController undo = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("constraint-verifier-undo-test", undo);
			Project project = Project.createProject(pool, undo);
			project.initialize(false, false);
			NormalTask task = project.createScriptedTask();
			task.setName("Constraint target");
			project.connectTask(task);
			NormalTask unrelated = project.createScriptedTask();
			unrelated.setName("Unrelated task");
			project.connectTask(unrelated);
			long initialDate = CalendarOption.getInstance().makeValidEnd(4L * day(), false);
			task.setScheduleConstraint(ConstraintType.Kind.SNET, initialDate);
			task.setDirty(false);
			unrelated.setDirty(false);
			identity[0] = task.getUniqueId();
			unrelatedStart[0] = unrelated.getStart();
			Field field = Configuration.getFieldFromId("Field.constraintDate");
			DateFormat format = EditOption.getInstance().getDateFormat();
			String oldText = format.format(new Date(task.getConstraintDate()));
			String newText = format.format(new Date(task.getConstraintDate() + 2L * day()));
			JTextField input = new JTextField(oldText);
			input.setInputVerifier(new FieldVerifier(field, objectRef(task, project.getTaskOutline().getDataFactory()), oldText));
			dates[0][0] = task.getConstraintDate();
			dates[0][1] = task.getStart();
			input.setText(newText);
			verified[0] = input.getInputVerifier().verify(input);
			dates[1][0] = task.getConstraintDate();
			dates[1][1] = task.getStart();
			undoRedo[0][0] = undo.canUndo();
			undoRedo[0][1] = undo.canRedo();
			undo.undo();
			dates[2][0] = task.getConstraintDate();
			dates[2][1] = task.getConstraintType();
			undoRedo[1][0] = undo.canUndo();
			undoRedo[1][1] = undo.canRedo();
			undo.redo();
			assertEquals(dates[1][0], task.getConstraintDate());
			assertEquals(ConstraintType.Kind.SNET.code(), task.getConstraintType());
			assertEquals(unrelatedStart[0], unrelated.getStart());
			try {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				MpoFileImporter importer = new MpoFileImporter();
				assertTrue(importer.saveProject(project, bytes));
				Project reopened = importer.loadProject(new ByteArrayInputStream(bytes.toByteArray()));
				for (var reopenedTask : reopened.getTaskList()) {
					if (identity[0] == reopenedTask.getUniqueId() && task.getName().equals(reopenedTask.getName())) {
						reloadedDate[0] = reopenedTask.getConstraintDate();
						break;
					}
				}
			} catch (Exception failure) {
				throw new AssertionError("MPO constraint date round-trip failed", failure);
			}
			undoRedo[2][0] = undo.canUndo();
			undoRedo[2][1] = undo.canRedo();
		});
		assertTrue(verified[0]);
		assertNotEquals(dates[0][0], dates[1][0]);
		assertEquals(dates[0][0], dates[2][0]);
		assertEquals(ConstraintType.Kind.SNET.code(), (int) dates[2][1]);
		assertEquals(dates[1][0], reloadedDate[0]);
		assertTrue(undoRedo[0][0]);
		assertFalse(undoRedo[0][1]);
		assertTrue(undoRedo[1][1]);
		assertTrue(undoRedo[2][0]);
	}

	private static void capture(long[] state, boolean[] dirty, boolean[] undoRedo, NormalTask task,
			NormalTask unrelated, Project project, DataFactoryUndoController undo) {
		state[0] = task.getConstraintType();
		state[1] = task.getConstraintDate();
		state[2] = task.getStart();
		state[3] = task.getEnd();
		state[4] = unrelated.getStart();
		state[5] = unrelated.getEnd();
		state[6] = unrelated.getConstraintType();
		state[7] = unrelated.getConstraintDate();
		dirty[0] = task.isDirty();
		dirty[1] = unrelated.isDirty();
		undoRedo[0] = undo.canUndo();
		undoRedo[1] = undo.canRedo();
		state[8] = project.getMutationState().revision();
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
