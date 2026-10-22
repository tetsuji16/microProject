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
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
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
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;

class TaskConstraintTypeFieldVerifierTransactionTest {
	@Test
	void observerFailureRestoresConstraintTypeAndAllTransactionState() throws Exception {
		RuntimeException injected = new IllegalStateException("injected constraint type observer failure");
		RuntimeException[] observed = new RuntimeException[1];
		AtomicInteger observerCalls = new AtomicInteger();
		long[][] beforeAfter = new long[2][13];
		boolean[][] dirty = new boolean[2][2];
		boolean[][] undoRedo = new boolean[2][2];
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = new Fixture("constraint-type-failure");
			fixture.target.setScheduleConstraint(ConstraintType.Kind.SNET,
					CalendarOption.getInstance().makeValidEnd(System.currentTimeMillis() + 14L * day(), false));
			fixture.target.setActualStart(CalendarOption.getInstance().makeValidEnd(2L * day(), false));
			fixture.resetDirty();
			fixture.undo.discardAllEdits();
			JComboBox<?> input = fixture.input();
			fixture.project.addObjectListener(event -> { observerCalls.incrementAndGet(); throw injected; });
			capture(beforeAfter[0], dirty[0], undoRedo[0], fixture);
			input.setSelectedItem(fixture.option(ConstraintType.Kind.MSO));
			try { input.getInputVerifier().verify(input); } catch (RuntimeException failure) { observed[0] = failure; }
			capture(beforeAfter[1], dirty[1], undoRedo[1], fixture);
		});
		assertSame(injected, observed[0]);
		assertEquals(1, observerCalls.get());
		assertState(beforeAfter[0], beforeAfter[1]);
		assertEquals(dirty[0][0], dirty[1][0]);
		assertEquals(dirty[0][1], dirty[1][1]);
		assertEquals(undoRedo[0][0], undoRedo[1][0]);
		assertEquals(undoRedo[0][1], undoRedo[1][1]);
	}

	@Test
	void successfulAdvancedFieldEditSupportsUndoRedoAndMpoReload() throws Exception {
		long[] values = new long[8];
		boolean[] verified = new boolean[1];
		boolean[][] undoRedo = new boolean[3][2];
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = new Fixture("constraint-type-success");
			fixture.target.setScheduleConstraint(ConstraintType.Kind.SNET,
					CalendarOption.getInstance().makeValidEnd(System.currentTimeMillis() + 14L * day(), false));
			fixture.resetDirty();
			fixture.undo.discardAllEdits();
			long initialDate = fixture.target.getConstraintDate();
			long unrelatedStart = fixture.unrelated.getStart();
			long unrelatedEnd = fixture.unrelated.getEnd();
			long identity = fixture.target.getUniqueId();
			JComboBox<?> input = fixture.input();
			input.setSelectedItem(fixture.option(ConstraintType.Kind.MSO));
			verified[0] = input.getInputVerifier().verify(input);
			values[0] = fixture.target.getConstraintType();
			values[1] = fixture.target.getConstraintDate();
			values[2] = fixture.target.getStart();
			values[3] = fixture.target.getEnd();
			values[4] = fixture.unrelated.getStart();
			values[5] = fixture.unrelated.getEnd();
			assertEquals(unrelatedStart, values[4], "type edit does not move unrelated task start");
			assertEquals(fixture.unrelated.getConstraintType(), ConstraintType.Kind.ASAP.code());
			assertEquals(fixture.unrelated.getActualStart(), 0L);
			undoRedo[0][0] = fixture.undo.canUndo(); undoRedo[0][1] = fixture.undo.canRedo();
			fixture.undo.undo();
			values[6] = fixture.target.getConstraintType();
			assertEquals(initialDate, fixture.target.getConstraintDate());
			assertEquals(unrelatedStart, fixture.unrelated.getStart());
			undoRedo[1][0] = fixture.undo.canUndo(); undoRedo[1][1] = fixture.undo.canRedo();
			fixture.undo.redo();
			values[7] = fixture.target.getConstraintType();
			assertEquals(values[1], fixture.target.getConstraintDate(), "type edit retains the existing constraint date");
			assertEquals(values[2], fixture.target.getStart());
			assertEquals(values[3], fixture.target.getEnd());
			assertEquals(unrelatedStart, fixture.unrelated.getStart());
			try {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				MpoFileImporter importer = new MpoFileImporter();
				assertTrue(importer.saveProject(fixture.project, bytes));
				Project reopened = importer.loadProject(new ByteArrayInputStream(bytes.toByteArray()));
				NormalTask reopenedTarget = reopened.getTaskList().stream().filter(task -> task.getUniqueId() == identity && fixture.target.getName().equals(task.getName()))
						.map(task -> (NormalTask) task).findFirst().orElseThrow();
				assertEquals(values[0], reopenedTarget.getConstraintType());
				assertEquals(values[1], reopenedTarget.getConstraintDate());
			} catch (Exception failure) { throw new AssertionError("MPO constraint type round-trip failed", failure); }
			undoRedo[2][0] = fixture.undo.canUndo(); undoRedo[2][1] = fixture.undo.canRedo();
		});
		assertTrue(verified[0]);
		assertNotEquals(ConstraintType.Kind.SNET.code(), values[0]);
		assertEquals(ConstraintType.Kind.SNET.code(), values[6]);
		assertEquals(ConstraintType.Kind.MSO.code(), values[7]);
		assertTrue(undoRedo[0][0]); assertFalse(undoRedo[0][1]);
		assertTrue(undoRedo[1][1]); assertTrue(undoRedo[2][0]);
	}

	@Test
	void asapTransitionUndoRedoRestoresConstraintDateAndMpoState() throws Exception {
		long[][] states = new long[3][7];
		long[] initialConstraintDate = new long[1];
		boolean[] verified = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			Fixture fixture = new Fixture("constraint-type-asap-undo");
			long constraintDate = CalendarOption.getInstance().makeValidEnd(
				fixture.project.getStart() + 5L * day(), false);
			initialConstraintDate[0] = constraintDate;
			fixture.target.setScheduleConstraint(ConstraintType.Kind.SNET, constraintDate);
			fixture.project.recalculate();
			fixture.undo.discardAllEdits();
			long unrelatedStart = fixture.unrelated.getStart();
			long unrelatedFinish = fixture.unrelated.getEnd();
			long identity = fixture.target.getUniqueId();
			JComboBox<?> input = fixture.input();
			input.setSelectedItem(fixture.option(ConstraintType.Kind.ASAP));
			verified[0] = input.getInputVerifier().verify(input);
			captureConstraintTypeState(states[0], fixture);
			assertEquals(0L, fixture.target.getConstraintDate(), "ASAP clears the SNET constraint date");
			assertEquals(unrelatedStart, fixture.unrelated.getStart());
			assertEquals(unrelatedFinish, fixture.unrelated.getEnd());
			fixture.undo.undo();
			captureConstraintTypeState(states[1], fixture);
			fixture.undo.redo();
			captureConstraintTypeState(states[2], fixture);
			assertEquals(unrelatedStart, fixture.unrelated.getStart());
			assertEquals(unrelatedFinish, fixture.unrelated.getEnd());
			try {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				MpoFileImporter importer = new MpoFileImporter();
				assertTrue(importer.saveProject(fixture.project, bytes));
				Project reopened = importer.loadProject(new ByteArrayInputStream(bytes.toByteArray()));
				NormalTask reopenedTarget = fixture.targetByUidAndName(reopened, identity);
				assertEquals(states[2][0], reopenedTarget.getConstraintType());
				assertEquals(states[2][1], reopenedTarget.getConstraintDate());
				assertEquals(states[2][2], reopenedTarget.getStart());
				assertEquals(states[2][3], reopenedTarget.getEnd());
			} catch (Exception failure) { throw new AssertionError("MPO ASAP constraint type round-trip failed", failure); }
		});
		assertTrue(verified[0]);
		assertEquals(ConstraintType.Kind.ASAP.code(), states[0][0]);
		assertEquals(0L, states[0][1]);
		assertEquals(ConstraintType.Kind.SNET.code(), states[1][0]);
		assertEquals(initialConstraintDate[0], states[1][1], "Undo must restore the complete original constraint date");
		assertEquals(ConstraintType.Kind.ASAP.code(), states[2][0]);
		assertEquals(0L, states[2][1]);
		assertEquals(states[0][2], states[2][2], "Redo must restore the edited target start");
		assertEquals(states[0][3], states[2][3], "Redo must restore the edited target finish");
	}

	private static void captureConstraintTypeState(long[] state, Fixture fixture) {
		state[0] = fixture.target.getConstraintType();
		state[1] = fixture.target.getConstraintDate();
		state[2] = fixture.target.getStart();
		state[3] = fixture.target.getEnd();
		state[4] = fixture.unrelated.getStart();
		state[5] = fixture.unrelated.getEnd();
		state[6] = fixture.project.getMutationState().revision();
	}

	private static void capture(long[] state, boolean[] dirty, boolean[] undoRedo, Fixture fixture) {
		state[0] = fixture.target.getConstraintType(); state[1] = fixture.target.getConstraintDate();
		state[2] = fixture.target.getStart(); state[3] = fixture.target.getEnd();
		state[4] = fixture.target.getActualStart(); state[5] = fixture.target.getActualFinish();
		state[6] = fixture.unrelated.getConstraintType(); state[7] = fixture.unrelated.getConstraintDate();
		state[8] = fixture.unrelated.getStart(); state[9] = fixture.unrelated.getEnd();
		state[10] = fixture.unrelated.getActualStart(); state[11] = fixture.unrelated.getActualFinish();
		state[12] = fixture.project.getMutationState().revision();
		dirty[0] = fixture.target.isDirty(); dirty[1] = fixture.unrelated.isDirty();
		undoRedo[0] = fixture.undo.canUndo(); undoRedo[1] = fixture.undo.canRedo();
	}

	private static void assertState(long[] expected, long[] actual) {
		for (int i = 0; i <= 12; i++) assertEquals(expected[i], actual[i], "state index " + i + " is restored");
	}

	private static long day() { return CalendarOption.getInstance().getMillisPerDay(); }

	private static final class Fixture {
		final DataFactoryUndoController undo = new DataFactoryUndoController();
		final Project project;
		final NormalTask target;
		final NormalTask unrelated;
		final Field field = Configuration.getFieldFromId("Field.constraintType");
		final ObjectRef ref;
		Fixture(String name) {
			ResourcePool pool = ResourcePool.createRourcePool(name, undo);
			project = Project.createProject(pool, undo); project.initialize(false, false);
			target = project.createScriptedTask(); target.setName("Constraint target"); project.connectTask(target);
			unrelated = project.createScriptedTask(); unrelated.setName("Unrelated task"); project.connectTask(unrelated);
			ref = objectRef(target, project.getTaskOutline().getDataFactory());
		}
		JComboBox<?> input() {
			JComboBox<?> combo = (JComboBox<?>) ComponentFactory.componentFor(field, ref, 0);
			for (ActionListener listener : combo.getActionListeners()) combo.removeActionListener(listener);
			return combo;
		}
		Object option(ConstraintType.Kind kind) {
			String key = switch (kind) {
				case ASAP -> "ConstraintType.ASAP"; case ALAP -> "ConstraintType.ALAP";
				case MSO -> "ConstraintType.MSO"; case MFO -> "ConstraintType.MFO";
				case SNET -> "ConstraintType.SNET"; case SNLT -> "ConstraintType.SNLT";
				case FNET -> "ConstraintType.FNET"; case FNLT -> "ConstraintType.FNLT";
				default -> throw new AssertionError("Unsupported constraint kind: " + kind);
			};
			Object label = Messages.getString(key);
			for (Object item : field.getOptions(target)) if (label.equals(item)) return item;
			throw new AssertionError("Missing constraint option: " + kind);
		}
		void resetDirty() { target.setDirty(false); unrelated.setDirty(false); }
		NormalTask targetByUidAndName(Project reopened, long uid) {
			return reopened.getTaskList().stream()
				.filter(task -> task.getUniqueId() == uid && target.getName().equals(task.getName()))
				.map(task -> (NormalTask) task).findFirst().orElseThrow();
		}
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
}
