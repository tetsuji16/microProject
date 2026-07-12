package com.projectlibre1.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.availability.Availability;
import com.projectlibre1.pm.assignment.contour.ContourFactory;
import com.projectlibre1.pm.assignment.contour.ContourBucketIntervalGenerator;
import com.projectlibre1.pm.assignment.contour.PersonalContourBucket;
import com.projectlibre1.pm.assignment.functor.ResourceAvailabilityFunctor;
import com.projectlibre1.pm.resource.ResourceImpl;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.time.MutableInterval;
import com.projectlibre1.undo.DataFactoryUndoController;

class AssignmentContourBehaviorTest {
	@Test
	void assignmentChildrenToRollupIsEmptyForLeafAssignments() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();

		Assignment assignment = Assignment.getInstance(task, resource, 1.0D, 0);
		Collection children = assignment.childrenToRollup();

		assertTrue(children.isEmpty());
	}

	@Test
	void contourFactoryRejectsUnknownContourTypes() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> ContourFactory.getInstance(-1));

		assertEquals("Unknown contour type: -1", exception.getMessage());
	}

	@Test
	void personalContourBucketRejectsNegativeDuration() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> PersonalContourBucket.getInstance(-1L, 0.5D));

		assertEquals("Negative contour bucket duration: -1", exception.getMessage());
	}

	@Test
	void assignmentDetailSetWorkCalendarOverridesEffectiveCalendar() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = Assignment.getInstance(task, resource, 1.0D, 0);

		assignment.detail.setWorkCalendar(project.getWorkCalendar());

		assertEquals(project.getWorkCalendar(), assignment.getEffectiveWorkCalendar());
	}

	@Test
	void assignmentResourceAvailabilityRespectsAvailabilityTable() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project);
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(2L * day);
		Assignment assignment = firstAssignment(task);
		ResourceImpl resource = (ResourceImpl) assignment.getResource();
		long split = assignment.getEffectiveWorkCalendar().add(assignment.getStart(), day, false);
		Availability halfRate = (Availability) resource.getAvailabilityTable().newValueObject(split);
		halfRate.setMaximumUnits(0.5D);

		assertEquals((long) (1.5D * day), assignment.getResourceAvailability());
	}

	@Test
	void resourceAvailabilityFunctorUsesTimeScaledAvailability() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project);
		long day = CalendarOption.getInstance().getMillisPerDay();
		Assignment assignment = Assignment.getInstance(task, project.getResourcePool().newResourceInstance(), 1.0D, 0);
		ResourceImpl resource = (ResourceImpl) assignment.getResource();
		long split = assignment.getEffectiveWorkCalendar().add(assignment.getStart(), day, false);
		Availability halfRate = (Availability) resource.getAvailabilityTable().newValueObject(split);
		halfRate.setMaximumUnits(0.5D);

		ResourceAvailabilityFunctor functor = ResourceAvailabilityFunctor.getInstance(resource);
		functor.initialize();
		functor.execute(new MutableInterval(assignment.getStart(), split));
		functor.execute(new MutableInterval(split, assignment.getEffectiveWorkCalendar().add(split, day, false)));

		assertEquals((long) (1.5D * day), (long) functor.getValue());
	}

	@Test
	void assignmentDetailSplitShiftsTheRemainingWork() {
		Project project = createProject();
		NormalTask task = createTask(project);
		long duration = 2L * CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(duration);
		Assignment assignment = firstAssignment(task);
		long splitFrom = assignment.getStart();
		long splitTo = assignment.getEffectiveWorkCalendar().add(splitFrom, CalendarOption.getInstance().getMillisPerDay(), false);

		assignment.detail.split(null, splitFrom, splitTo);

		assertTrue(assignment.getWorkContour().isPersonal());
	}

	@Test
	void assignmentCloneCopiesDetailState() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = Assignment.getInstance(task, resource, 1.0D, 0);
		assignment.detail.setPercentComplete(0.25D);
		assignment.detail.setWorkCalendar(project.getWorkCalendar());

		Assignment cloned = (Assignment) assignment.clone();

		assertTrue(assignment != cloned);
		assertTrue(assignment.detail != cloned.detail);
		assertEquals(0.25D, cloned.getPercentComplete());
		assertEquals(project.getWorkCalendar(), cloned.getEffectiveWorkCalendar());
		assignment.detail.setPercentComplete(0.5D);
		assertEquals(0.25D, cloned.getPercentComplete());
	}

	@Test
	void assignmentPercentCompleteIsBoundedAndZeroDurationDoesNotProduceNaN() {
		Project project = createProject();
		NormalTask task = createTask(project);
		Assignment assignment = Assignment.getInstance(task, project.getResourcePool().newResourceInstance(), 1.0D, 0);

		assignment.detail.setPercentComplete(-0.5D);
		assertEquals(0.0D, assignment.getPercentComplete());
		assignment.detail.setPercentComplete(1.5D);
		assertEquals(1.0D, assignment.getPercentComplete());
		assignment.detail.setRemainingDuration(0L);
		assertTrue(Double.isFinite(assignment.getPercentComplete()));
	}

	@Test
	void assignmentPercentCompleteRejectsNonFiniteValues() {
		Project project = createProject();
		NormalTask task = createTask(project);
		Assignment assignment = Assignment.getInstance(task, project.getResourcePool().newResourceInstance(), 1.0D, 0);

		assertThrows(IllegalArgumentException.class, () -> assignment.detail.setPercentComplete(Double.NaN));
		assertThrows(IllegalArgumentException.class, () -> assignment.detail.setPercentComplete(Double.POSITIVE_INFINITY));
	}

	@Test
	void makingAContourPersonalPreservesWorkState() {
		Project project = createProject();
		NormalTask task = createTask(project);
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(2L * day);
		Assignment assignment = firstAssignment(task);
		assignment.detail.setPercentComplete(0.5D);

		long work = assignment.getWork(null);
		long actualWork = assignment.getActualWork(null);
		long remainingWork = assignment.getRemainingWork();
		double units = assignment.getUnits();

		assignment.makeContourPersonal();

		assertEquals(work, assignment.getWork(null));
		assertEquals(actualWork, assignment.getActualWork(null));
		assertEquals(remainingWork, assignment.getRemainingWork());
		assertEquals(units, assignment.getUnits());
	}

	@Test
	void contourGeneratorTraversesReverseScheduledAssignments() {
		Project project = createProject();
		NormalTask task = createTask(project);
		long day = CalendarOption.getInstance().getMillisPerDay();
		task.setDuration(2L * day);
		project.setForward(false);
		Assignment assignment = firstAssignment(task);
		ContourBucketIntervalGenerator generator = assignment.contourGeneratorInstance(HasTimeDistributedData.WORK);
		long initialEnd = generator.getEnd();

		assertTrue(generator.evaluate(generator));
		assertTrue(generator.current() != null);
		assertTrue(generator.getEnd() != initialEnd);
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}

	private Assignment firstAssignment(NormalTask task) {
		return (Assignment) task.getAssignments().iterator().next();
	}
}
