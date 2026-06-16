package com.projectlibre1.pm.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.projectlibre1.association.InvalidAssociationException;
import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.scheduling.ScheduleService;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;

class DependencyServiceTest {
	@Test
	void newDependencyCreatesFsZeroLagLink() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		Dependency dependency = DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		assertEquals(DependencyType.FS, dependency.getDependencyType());
		assertEquals(0L, dependency.getLag());
	}

	@Test
	void newDependencyRejectsSelfLink() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		assertThrows(InvalidAssociationException.class, () -> DependencyService.getInstance().newDependency(task, task, DependencyType.FS, 0L, this));
	}

	@Test
	void resizingSuccessorFromEndKeepsDependencyAlignedStart() throws InvalidAssociationException {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask predecessor = new NormalTask(project);
		NormalTask successor = new NormalTask(project);
		project.connectTask(predecessor);
		project.connectTask(successor);

		long day = CalendarOption.getInstance().getMillisPerDay();
		predecessor.setEnd(predecessor.getEffectiveWorkCalendar().add(predecessor.getStart(), day, false));
		successor.setEnd(successor.getEffectiveWorkCalendar().add(successor.getStart(), day, false));
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);

		long successorStartBeforeResize = successor.getStart();
		long successorEndBeforeResize = successor.getEnd();
		long successorEndAfterResize = CalendarOption.getInstance().makeValidEnd(
				successor.getEffectiveWorkCalendar().add(successorEndBeforeResize, day, false), true);

		ScheduleService.getInstance().setInterval(this, successor, successorStartBeforeResize, successorEndAfterResize,
				new ScheduleInterval(successorStartBeforeResize, successorEndBeforeResize), undoController.getEditSupport());

		assertEquals(successorStartBeforeResize, successor.getStart());
		assertEquals(successorEndAfterResize, successor.getEnd());
	}
}
