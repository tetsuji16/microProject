package com.projectlibre1.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.functor.IntervalConsumer;
import com.projectlibre1.undo.DataFactoryUndoController;

class ScheduleServiceSplitTest {
	@Test
	void splittingProjectDoesNotCreateUndoEditWhenNothingChanges() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);

		long originalStart = project.getStart();
		long originalEnd = project.getEnd();

		ScheduleService.getInstance().split(this, project, originalStart, originalStart, undoController.getEditSupport());

		assertEquals(originalStart, project.getStart());
		assertEquals(originalEnd, project.getEnd());
		assertFalse(undoController.canUndo());
	}

	@Test
	void splitReturnsFalseForReadOnlySchedules() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setExternal(true);

		long originalStart = task.getStart();
		long originalEnd = task.getEnd();

		boolean changed = ScheduleService.getInstance().split(this, task, originalStart, originalStart, undoController.getEditSupport());

		assertFalse(changed);
		assertEquals(originalStart, task.getStart());
		assertEquals(originalEnd, task.getEnd());
		assertFalse(undoController.canUndo());
	}

	@Test
	void setCompletedReturnsFalseForReadOnlySchedules() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setExternal(true);

		long originalCompleted = task.getCompletedThrough();
		boolean changed = ScheduleService.getInstance().setCompleted(this, task, originalCompleted + 24L * 60L * 60L * 1000L,
				undoController.getEditSupport());

		assertFalse(changed);
		assertEquals(originalCompleted, task.getCompletedThrough());
		assertFalse(undoController.canUndo());
	}

	@Test
	void consumeIntervalsRecoversAfterConsumerThrows() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		assertThrows(RuntimeException.class, () ->
				ScheduleService.getInstance().consumeIntervals(task, new IntervalConsumer() {
					public void consumeInterval(ScheduleInterval interval) {
						throw new RuntimeException("boom");
					}
				}));

		AtomicInteger count = new AtomicInteger();
		ScheduleService.getInstance().consumeIntervals(task, new IntervalConsumer() {
			public void consumeInterval(ScheduleInterval interval) {
				count.incrementAndGet();
			}
		});

		assertEquals(1, count.get());
	}
}
