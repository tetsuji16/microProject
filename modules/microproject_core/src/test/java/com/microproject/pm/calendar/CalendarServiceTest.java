/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.document.ObjectEvent;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

class CalendarServiceTest {
	@Test
	void calendarServiceHandlesScratchCopyAndApplyBoundaries() {
		CalendarService service = CalendarService.getInstance();
		WorkingCalendar original = WorkingCalendar.getInstance();
		original.setName("CalendarServiceTest-original");
		original.getConcreteInstance();

		assertEquals(null, service.getBaseCalendar(null));
		WorkingCalendar scratch = service.makeScratchCopy(original);
		scratch.setName("CalendarServiceTest-scratch");
		assertTrue(service.commitCalendar(original, scratch));
		assertEquals("CalendarServiceTest-scratch", original.getName());

		assertTrue(original.isInvalid());
		service.invalidate(null);
		assertThrows(IllegalArgumentException.class, () -> service.makeScratchCopy(null));
		assertThrows(IllegalArgumentException.class, () -> service.commitCalendar(original, null));
		assertFalse(service.commitCalendar(original, original));
	}

	/**
	 * Issue #355 / upstream SF #112: editing a base calendar must invalidate
	 * the merged concrete calendar used by derived resource calendars.
	 */
	@Test
	void derivedCalendarConcreteReflectsBaseEdit() throws WorkRangeException {
		CalendarService service = CalendarService.getInstance();
		WorkingCalendar base = WorkingCalendar.getInstance();
		base.setName("Issue355-base");
		base.setUniqueId(-9000001L);

		WorkDay working = new WorkDay();
		working.getWorkingHours().setInterval(0, WorkingHours.hourTime(8), WorkingHours.hourTime(12));
		working.getWorkingHours().setInterval(1, WorkingHours.hourTime(13), WorkingHours.hourTime(17));
		WorkDay nonWorking = new WorkDay();
		nonWorking.getWorkingHours().setNonWorking();
		base.setWeekDays(working);
		base.setWeekends(nonWorking);
		service.add(base);

		WorkingCalendar derived = WorkingCalendar.getInstanceBasedOn(base);
		derived.setName("Issue355-derived");
		derived.setUniqueId(-9000002L);
		WorkingCalendar indirectlyDerived = WorkingCalendar.getInstanceBasedOn(derived);
		indirectlyDerived.setName("Issue355-indirect-derived");
		indirectlyDerived.setUniqueId(-9000003L);

		derived.getConcreteInstance();
		indirectlyDerived.getConcreteInstance();
		assertFalse(derived.isInvalid(), "derived concrete should be cached before the base edit");
		assertFalse(indirectlyDerived.isInvalid(), "indirect derived concrete should be cached before the base edit");
		assertTrue(!derived.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking(),
				"Saturday must be non-working before the base edit");

		WorkingHours saturdayHours = new WorkingHours();
		saturdayHours.setInterval(0, WorkingHours.hourTime(8), WorkingHours.hourTime(12));
		saturdayHours.setInterval(1, WorkingHours.hourTime(13), WorkingHours.hourTime(17));
		base.setWeekDayWorkingHours(Calendar.SATURDAY, saturdayHours);
		base.invalidate();

		assertTrue(derived.isInvalid(), "base edit must invalidate the derived concrete cache");
		assertTrue(indirectlyDerived.isInvalid(), "base edit must invalidate indirect derived caches");
		assertTrue(derived.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking(),
				"derived calendar must reflect the base edit");
		assertTrue(indirectlyDerived.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking(),
				"indirect derived calendar must reflect the base edit");
	}

	@Test
	void baseCalendarCommitInvalidatesAllOwnersPublishesAfterUnlockAndKeepsUndoWithEditor() throws WorkRangeException {
		DataFactoryUndoController firstUndo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("calendar-owner", firstUndo);
		Project first = Project.createProject(pool, firstUndo);
		first.initialize(false, false);
		DataFactoryUndoController secondUndo = new DataFactoryUndoController();
		Project second = Project.createProject(pool, secondUndo);
		second.initialize(false, false);
		pool.addProject(second);

		WorkingCalendar base = WorkingCalendar.getStandardBasedInstance();
		base.setName("CalendarServiceTest-shared-base-before");
		WorkingCalendar firstDerived = WorkingCalendar.getInstanceBasedOn(base);
		WorkingCalendar secondDerived = WorkingCalendar.getInstanceBasedOn(base);
		first.setWorkCalendar(firstDerived);
		second.setWorkCalendar(secondDerived);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setWorkCalendar(base);
		NormalTask task = new NormalTask(first);
		first.connectTask(task);
		first.getSchedulingAlgorithm().addObject(task);
		task.setWorkCalendar(firstDerived);
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0D, 0L, this);
		// Keep the fixture in the future: unstarted work is not scheduled before the
		// current date, so a past Friday would move the finish with the wall clock.
		java.util.GregorianCalendar fridayStart = DateTime.calendarInstance(2099, Calendar.OCTOBER, 16);
		fridayStart.set(Calendar.HOUR_OF_DAY, 8);
		long fridayAhead = fridayStart.getTimeInMillis();
		task.setScheduleConstraint(com.microproject.pm.scheduling.ConstraintType.Kind.MSO, fridayAhead);
		task.getCurrentSchedule().setStart(fridayAhead);
		assignment.setWork(2L * 8L * 60L * 60L * 1000L, null);
		task.setDuration(2L * 8L * 60L * 60L * 1000L);
		first.recalculate();
		long taskFinishBefore = task.getEnd();
		long assignmentFinishBefore = assignment.getEnd();
		long expectedFinishBefore = calendarTime(2099, Calendar.OCTOBER, 19, 17);
		long expectedFinishAfter = calendarTime(2099, Calendar.OCTOBER, 17, 17);
		assertEquals(expectedFinishBefore, taskFinishBefore, "two workdays from Friday end Monday without Saturday work");
		assertEquals(expectedFinishBefore, assignmentFinishBefore);
		firstUndo.clear();
		secondUndo.clear();
		firstDerived.getConcreteInstance();
		secondDerived.getConcreteInstance();

		AtomicBoolean callbacksAfterUnlock = new AtomicBoolean(true);
		AtomicInteger projectEvents = new AtomicInteger();
		AtomicInteger poolEvents = new AtomicInteger();
		List<Object> poolUpdatedObjects = new ArrayList<>();
		List<Long> taskEndsObservedByProjectListener = new ArrayList<>();
		List<Long> assignmentEndsObservedByProjectListener = new ArrayList<>();
		ObjectEvent.Listener projectListener = event -> {
			projectEvents.incrementAndGet();
			if (event.getObject() == first) {
				taskEndsObservedByProjectListener.add(task.getEnd());
				assignmentEndsObservedByProjectListener.add(assignment.getEnd());
			}
			callbacksAfterUnlock.compareAndSet(true, !first.getMutationState().isHeldByCurrentThread()
				&& !second.getMutationState().isHeldByCurrentThread());
		};
		ObjectEvent.Listener poolListener = event -> {
			poolEvents.incrementAndGet();
			if (event.isUpdate()) poolUpdatedObjects.add(event.getObject());
			callbacksAfterUnlock.compareAndSet(true, !first.getMutationState().isHeldByCurrentThread()
				&& !second.getMutationState().isHeldByCurrentThread());
		};
		first.addObjectListener(projectListener);
		second.addObjectListener(projectListener);
		pool.addObjectListener(poolListener);

		long firstRevision = first.getMutationState().revision();
		long secondRevision = second.getMutationState().revision();
		long poolRevision = pool.getCoordinationRevision();
		WorkingCalendar staged = CalendarService.getInstance().makeScratchCopy(base);
		staged.setName("CalendarServiceTest-shared-base-after");
		WorkingHours saturdayHours = new WorkingHours();
		saturdayHours.setInterval(0, WorkingHours.hourTime(8), WorkingHours.hourTime(12));
		saturdayHours.setInterval(1, WorkingHours.hourTime(13), WorkingHours.hourTime(17));
		WorkDay workingSaturday = new WorkDay();
		workingSaturday.setWorkingHours(saturdayHours);
		staged.setWeekDay(Calendar.SATURDAY - 1, workingSaturday);
		assertTrue(CalendarService.getInstance().commitCalendars(
			java.util.Map.of(base, staged), firstUndo.getEditSupport()));

		assertEquals(firstRevision + 1, first.getMutationState().revision());
		assertEquals(secondRevision + 1, second.getMutationState().revision());
		assertEquals(poolRevision + 1, pool.getCoordinationRevision());
		assertTrue(firstDerived.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking(),
			"committing Saturday hours must change the schedule calendar used by its project");
		long taskFinishAfter = task.getEnd();
		long assignmentFinishAfter = assignment.getEnd();
		assertEquals(expectedFinishAfter, taskFinishAfter, "Saturday work must move the task finish to Saturday at 17:00");
		assertEquals(expectedFinishAfter, assignmentFinishAfter);
		assertTrue(taskEndsObservedByProjectListener.contains(expectedFinishAfter),
			"project callbacks must observe the recalculated task date after commit");
		assertTrue(assignmentEndsObservedByProjectListener.contains(expectedFinishAfter),
			"project callbacks must observe the recalculated assignment date after commit");
		assertTrue(projectEvents.get() > 0);
		assertTrue(poolEvents.get() > 0);
		assertTrue(poolUpdatedObjects.stream().anyMatch(object -> object == resource),
			"pool cache listeners must receive the affected resource identity, not only the pool document");
		assertTrue(callbacksAfterUnlock.get());
		assertTrue(firstUndo.canUndo());
		assertFalse(secondUndo.canUndo(), "calendar Undo remains with the document that committed the dialog");

		firstUndo.undo();
		assertEquals("CalendarServiceTest-shared-base-before", base.getName());
		assertFalse(firstDerived.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking());
		assertEquals(taskFinishBefore, task.getEnd());
		assertEquals(assignmentFinishBefore, assignment.getEnd());
		assertTrue(firstUndo.canRedo());
		assertFalse(secondUndo.canUndo());
		firstUndo.redo();
		assertEquals("CalendarServiceTest-shared-base-after", base.getName());
		assertTrue(firstDerived.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking());
		assertEquals(taskFinishAfter, task.getEnd());
		assertEquals(assignmentFinishAfter, assignment.getEnd());
		assertSame(resource.getWorkCalendar(), base);
		HasCalendar failingOwner = new HasCalendar() {
			@Override public void setWorkCalendar(WorkCalendar workCalendar) { }
			@Override public WorkCalendar getWorkCalendar() { return base; }
			@Override public WorkCalendar getEffectiveWorkCalendar() { return base; }
			@Override public com.microproject.document.Document getCalendarOwnerDocument() { return first; }
			@Override public com.microproject.document.Document invalidateCalendar() {
				throw new IllegalStateException("injected calendar invalidation failure");
			}
		};
		base.addObjectUsing(failingOwner);
		assertThrows(CalendarService.CommittedMutationException.class, firstUndo::undo);
		assertEquals("CalendarServiceTest-shared-base-before", base.getName(),
			"the Undo model mutation remains committed when an owner invalidation fails");
		assertTrue(firstUndo.canRedo(), "Undo history advances with the already committed model state");
		assertFalse(firstUndo.canUndo());
		first.removeObjectListener(projectListener);
		second.removeObjectListener(projectListener);
		pool.removeObjectListener(poolListener);
	}

	private static long calendarTime(int year, int month, int day, int hour) {
		java.util.GregorianCalendar calendar = DateTime.calendarInstance(year, month, day);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		return calendar.getTimeInMillis();
	}

	@Test
	void equalStagedCalendarIsANoopWithoutRevisionOrUndoAndOwnersUseIdentityMembership() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("calendar-noop", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		WorkingCalendar base = WorkingCalendar.getInstance();
		project.setWorkCalendar(base);
		HasCalendar first = equalHasCalendar(base, project);
		HasCalendar second = equalHasCalendar(base, project);
		base.addObjectUsing(first);
		base.addObjectUsing(second);
		assertTrue(base.getObjectsUsing().stream().anyMatch(owner -> owner == first));
		assertTrue(base.getObjectsUsing().stream().anyMatch(owner -> owner == second));

		long projectRevision = project.getMutationState().revision();
		long poolRevision = pool.getCoordinationRevision();
		WorkingCalendar unchangedScratch = CalendarService.getInstance().makeScratchCopy(base);
		assertFalse(CalendarService.getInstance().commitCalendars(
			java.util.Map.of(base, unchangedScratch), undo.getEditSupport()));
		assertEquals(projectRevision, project.getMutationState().revision());
		assertEquals(poolRevision, pool.getCoordinationRevision());
		assertFalse(undo.canUndo(), "a semantic no-op must not create a history item");
	}

	@Test
	void committedPublicationFailureStillDeliversReservedCalendarUndo() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("calendar-publication-failure", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		WorkingCalendar calendar = WorkingCalendar.getInstance();
		calendar.setName("CalendarServiceTest-publication-before");
		project.setWorkCalendar(calendar);
		ObjectEvent.Listener failingListener = event -> {
			if (event.isUpdate()) throw new IllegalStateException("injected post-commit observer failure");
		};
		project.addObjectListener(failingListener);
		WorkingCalendar staged = CalendarService.getInstance().makeScratchCopy(calendar);
		staged.setName("CalendarServiceTest-publication-after");
		assertThrows(CalendarService.CommittedMutationException.class,
			() -> CalendarService.getInstance().commitCalendars(
				java.util.Map.of(calendar, staged), undo.getEditSupport()));
		assertEquals("CalendarServiceTest-publication-after", calendar.getName());
		assertTrue(undo.canUndo(), "a committed edit reserves and delivers history even when publication throws");
		project.removeObjectListener(failingListener);
		undo.undo();
		assertEquals("CalendarServiceTest-publication-before", calendar.getName());
	}

	@Test
	void orphanPoolPublishesOneRefreshForEachAffectedResource() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("orphan-calendar-pool", undo);
		ResourceImpl resource = pool.newResourceInstance();
		WorkingCalendar calendar = WorkingCalendar.getInstance();
		resource.setWorkCalendar(calendar);
		AtomicInteger resourceUpdates = new AtomicInteger();
		pool.addObjectListener(event -> {
			if (event.isUpdate() && event.getObject() == resource) resourceUpdates.incrementAndGet();
		});
		WorkingCalendar staged = CalendarService.getInstance().makeScratchCopy(calendar);
		staged.setName("orphan-calendar-updated");
		long revision = pool.getCoordinationRevision();

		assertTrue(CalendarService.getInstance().commitCalendar(calendar, staged));

		assertEquals(1, resourceUpdates.get(), "orphan pool owner receives one resource refresh");
		assertEquals(revision + 1, pool.getCoordinationRevision());
	}

	private static HasCalendar equalHasCalendar(WorkingCalendar calendar, Project project) {
		return new HasCalendar() {
			@Override public void setWorkCalendar(WorkCalendar value) { }
			@Override public WorkCalendar getWorkCalendar() { return calendar; }
			@Override public WorkCalendar getEffectiveWorkCalendar() { return calendar; }
			@Override public com.microproject.document.Document getCalendarOwnerDocument() { return project; }
			@Override public com.microproject.document.Document invalidateCalendar() { return project; }
			@Override public boolean equals(Object other) { return other instanceof HasCalendar; }
			@Override public int hashCode() { return 1; }
		};
	}
}
