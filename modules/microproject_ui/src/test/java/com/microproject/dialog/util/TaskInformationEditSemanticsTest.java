/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.field.ObjectRef;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.WalkersNodeModel;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.UndoController;

/** Values edited in the information dialogs follow the task-sheet scheduling rules and the OK/Cancel contract. */
class TaskInformationEditSemanticsTest {
	private static final class Fixture {
		final DataFactoryUndoController undo = new DataFactoryUndoController();
		final Project project;
		final NormalTask task;

		Fixture() {
			ResourcePool pool = ResourcePool.createRourcePool("edit-semantics", undo);
			project = Project.createProject(pool, undo);
			project.initialize(false, false);
			task = new NormalTask(project);
			project.connectTask(task);
			project.getSchedulingAlgorithm().addObject(task);
			task.setDuration(2L * day());
			ResourceImpl resource = pool.newResourceInstance();
			AssignmentService.getInstance().newAssignment(task, resource, 1.0d, 0L, this);
			project.recalculate();
			undo.discardAllEdits();
		}

		ObjectRef ref() {
			NodeModelDataFactory factory = project.getTaskOutline().getDataFactory();
			return new ObjectRef() {
				@Override public Node getNode() { return null; }
				@Override public WalkersNodeModel getNodeModel() { return null; }
				@Override public Object getObject() { return task; }
				@Override public Collection<?> getCollection() { return null; }
				@Override public NodeModelDataFactory getDataFactory() { return factory; }
			};
		}

		JComponent component(String fieldId) {
			Field field = Configuration.getFieldFromId(fieldId);
			ObjectRef ref = ref();
			JComponent component = ComponentFactory.componentFor(field, ref, 0);
			ComponentFactory.updateValueOfComponent(component, field, ref);
			return component;
		}
	}

	private static boolean verify(JComponent component) {
		JComponent verified = ComponentFactory.verifiedComponent(component);
		return verified.getInputVerifier().verify(verified);
	}

	private static long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}

	private static JTextField spinnerText(JComponent spinner) {
		return ((JSpinner.DefaultEditor) ((JSpinner) spinner).getEditor()).getTextField();
	}

	private static void onEdt(Runnable body) throws Exception {
		Throwable[] failure = new Throwable[1];
		SwingUtilities.invokeAndWait(() -> {
			try {
				body.run();
			} catch (Throwable t) {
				failure[0] = t;
			}
		});
		if (failure[0] instanceof Error error)
			throw error;
		if (failure[0] != null)
			throw new AssertionError(failure[0]);
	}

	@Test
	void durationEditedInDialogRecalculatesWorkLikeTheTaskSheet() throws Exception {
		onEdt(() -> {
			Fixture f = new Fixture();
			double work = f.task.getWork();
			assertTrue(work > 0.0d);
			JTextField input = (JTextField) f.component("Field.duration");
			input.setText("4d");
			assertTrue(verify(input));
			assertEquals(4L * day(), f.task.getDuration());
			assertEquals(2.0d * work, f.task.getWork(), 1.0d, "Fixed Units: Duration edit scales Work");
		});
	}

	@Test
	void finishEditedInDialogKeepsStartAndChangesDuration() throws Exception {
		onEdt(() -> {
			Fixture f = new Fixture();
			long start = f.task.getStart();
			long duration = f.task.getDuration();
			long newFinish = f.task.getEffectiveWorkCalendar().add(f.task.getEnd(), 2L * day(), false);
			JComponent finish = f.component("Field.finish");
			((ExtDateField) finish).setValue(new Date(com.microproject.util.DateTime.dayFloor(newFinish)));
			verify(finish);
			assertEquals(start, f.task.getStart());
			assertNotEquals(duration, f.task.getDuration());
			assertTrue(f.task.getDuration() > duration);
			Calendar end = com.microproject.util.DateTime.calendarInstance();
			end.setTimeInMillis(f.task.getEnd());
			Calendar expected = CalendarOption.getInstance().getDefaultEndTime();
			assertEquals(expected.get(Calendar.HOUR_OF_DAY), end.get(Calendar.HOUR_OF_DAY),
				"a day-precision Finish takes the default finish time");
		});
	}

	@Test
	void focusingAndLeavingFinishWithoutChangesKeepsTheStoredTime() throws Exception {
		onEdt(() -> {
			Fixture f = new Fixture();
			long end = f.task.getEnd();
			long start = f.task.getStart();
			JComponent finish = f.component("Field.finish");
			ExtDateField dd = (ExtDateField) finish;

			assertTrue(verify(finish));

			assertTrue(verify(finish));
			assertEquals(end, f.task.getEnd());
			assertEquals(start, f.task.getStart());
			assertFalse(f.undo.canUndo(), "no edit must be recorded");
		});
	}

	@Test
	void invalidSpinnerTextIsRejectedAndTheValueIsUnchanged() throws Exception {
		onEdt(() -> {
			Fixture f = new Fixture();
			double percent = f.task.getPercentComplete();
			JComponent spinner = f.component("Field.percentComplete");
			assertTrue(spinner instanceof JSpinner);
			JTextField text = spinnerText(spinner);
			text.setText("abc");
			assertFalse(verify(text));
			assertEquals(percent, f.task.getPercentComplete(), 0.0d);
			assertFalse(f.undo.canUndo());
		});
	}

	@Test
	void cancelRestoresTheOriginalScheduleAndOkIsOneUndoStep() throws Exception {
		onEdt(() -> {
			Fixture f = new Fixture();
			long duration = f.task.getDuration();
			double work = f.task.getWork();

			UndoController.Session cancelled = f.undo.beginSession();
			JTextField input = (JTextField) f.component("Field.duration");
			input.setText("4d");
			assertTrue(verify(input));
			assertEquals(4L * day(), f.task.getDuration());
			cancelled.rollback();
			assertEquals(duration, f.task.getDuration());
			assertEquals(work, f.task.getWork(), 1.0d);
			assertFalse(f.undo.canRedo(), "a cancelled dialog leaves nothing to redo");
			assertFalse(f.undo.canUndo());

			UndoController.Session accepted = f.undo.beginSession();
			input = (JTextField) f.component("Field.duration");
			input.setText("4d");
			assertTrue(verify(input));
			JTextField percent = spinnerText(f.component("Field.percentComplete"));
			percent.setText("50%");
			assertTrue(verify(percent));
			accepted.commit();
			assertEquals(4L * day(), f.task.getDuration());
			f.undo.undo();
			assertEquals(duration, f.task.getDuration(), "one undo reverts every edit made in the dialog");
			assertEquals(work, f.task.getWork(), 1.0d);
			assertEquals(0.0d, f.task.getPercentComplete(), 0.0d);
			assertFalse(f.undo.canUndo(), "OK must create exactly one undo step");
			f.undo.redo();
			assertEquals(4L * day(), f.task.getDuration());
		});
	}

	@Test
	void sessionRecordsOnlyDialogEditsAndRollbackKeepsForeignEdits() throws Exception {
		onEdt(() -> {
			Fixture f = new Fixture();
			long duration = f.task.getDuration();
			javax.swing.undo.UndoableEdit foreign = new javax.swing.undo.AbstractUndoableEdit() { };

			UndoController.Session session = f.undo.beginSession();
			JTextField input = (JTextField) f.component("Field.duration");
			input.setText("4d");
			assertTrue(verify(input));
			f.undo.getEditSupport().postEdit(foreign); // made outside the dialog (sheet or Gantt)
			session.rollback();
			assertEquals(4L * day(), f.task.getDuration(), "a foreign edit on top stops the rollback");
			assertTrue(f.undo.canUndo());

			f.undo.undo(); // the foreign edit
			session = f.undo.beginSession();
			f.undo.getEditSupport().postEdit(new javax.swing.undo.AbstractUndoableEdit() { });
			assertFalse(session.hasEdits(), "edits outside the dialog scope are not recorded");
			session.rollback();
			assertEquals(4L * day(), f.task.getDuration());
		});
	}
}
