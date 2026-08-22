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
package com.microproject.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.pm.calendar.InvalidCalendarIntersectionException;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.transaction.MultipleTransaction;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Regression test for issue #346: the "結果作業時間が無効です" (invalid calendar
 * intersection) dialog used to be raised synchronously inside the scheduling /
 * recalculation path. Because the task's intersection calendar is reset to null
 * on every recompute ({@code AssignmentDetail.invalidateAssignmentCalendar()}),
 * the same invalid state re-triggered the modal alert on every recalc pass, so
 * dismissing it with OK immediately popped it up again — an endless OK-loop that
 * froze the UI.
 *
 * <p>The fix ({@code AssignmentCalendarSupport}) raises the alert at most once
 * per task/session via a one-shot guard and defers the modal to the EDT so it
 * never re-enters the scheduling call stack. This test reproduces the recompute
 * loop and asserts the alert fires exactly once, not repeatedly.
 */
class InvalidIntersectionFreezeTest {

	@AfterEach
	void tearDown() {
		// Restore production behavior and clear the one-shot warning history so
		// the static guard does not leak across tests.
		AssignmentCalendarSupport.resetNotifier();
		AssignmentCalendarSupport.resetWarningHistory();
	}

	@Test
	void invalidIntersectionWarnsAtMostOnceAcrossRecomputeLoop() {
		// Build two disjoint working calendars so intersectWith throws.
		// getStandardBasedInstance() is based on the standard Mon-Fri calendar
		// (all seven week days defined), so getDerivedWeekDay never returns null.
		WorkingCalendar taskCal = WorkingCalendar.getStandardBasedInstance();
		WorkingCalendar resourceCal = WorkingCalendar.getStandardBasedInstance();
		// taskCal works Monday-Wednesday only.
		taskCal.setWeekDay(Calendar.THURSDAY - 1, new WorkDay());
		taskCal.setWeekDay(Calendar.FRIDAY - 1, new WorkDay());
		// resourceCal works Thursday-Friday only.
		resourceCal.setWeekDay(Calendar.MONDAY - 1, new WorkDay());
		resourceCal.setWeekDay(Calendar.TUESDAY - 1, new WorkDay());
		resourceCal.setWeekDay(Calendar.WEDNESDAY - 1, new WorkDay());

		// Precondition: the two calendars genuinely have no intersection, so
		// resolve() throws InvalidCalendarIntersectionException without the guard.
		assertThrows(InvalidCalendarIntersectionException.class,
				() -> taskCal.intersectWith(resourceCal));

		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		task.setWorkCalendar(taskCal);
		resource.setWorkCalendar(resourceCal);

		Assignment assignment = AssignmentService.getInstance()
				.newAssignment(task, resource, 1.0D, 0L, this);

		AtomicInteger alertCount = new AtomicInteger();
		// Override the production notifier (which shows a Swing modal) with a
		// counter so the test is headless-safe and observable.
		Consumer<Task> countingNotifier = t -> alertCount.incrementAndGet();
		AssignmentCalendarSupport.notifier = countingNotifier;

		// Simulate the freeze loop: repeated recalculation that invalidates the
		// cached intersection calendar and re-resolves. Before the fix this
		// re-entered the modal alert on every pass and froze the UI.
		for (int i = 0; i < 20; i++) {
			assignment.invalidateAssignmentCalendar();
			assignment.getEffectiveWorkCalendar();
		}

		assertEquals(1, alertCount.get(),
				"the invalid-intersection alert must fire exactly once per task/session, not in a loop");
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
}
