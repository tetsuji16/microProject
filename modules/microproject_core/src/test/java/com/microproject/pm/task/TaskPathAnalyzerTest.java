/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Task path analysis on a small project:
 * A -> B -> D and A -> C -> D where C finishes earlier than B (the C -> D link is not driving), and
 * A -> H with a five day lag next to A -> I -> H (the lagged link drives H, the I -> H link does not).
 */
class TaskPathAnalyzerTest {
	private Project project;
	private NormalTask a;
	private NormalTask b;
	private NormalTask c;
	private NormalTask d;
	private NormalTask h;
	private NormalTask i;
	private Dependency cToD;
	private Dependency aToH;
	private Dependency iToH;

	@BeforeEach
	void buildProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("task-path-test", undo);
		project = Project.createProject(pool, undo);
		project.initialize(false, false);
		project.setWorkCalendar(((WorkingCalendar) project.getWorkCalendar()).makeScratchCopy());
		long start = CalendarOption.getInstance().makeValidStart(
			java.time.LocalDate.of(2030, 6, 3).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), true);
		project.setStart(start);
		project.setStartConstraint(start);
		a = task("A", 1);
		b = task("B", 3);
		c = task("C", 1);
		d = task("D", 1);
		h = task("H", 1);
		i = task("I", 1);
		link(a, b, 0);
		link(a, c, 0);
		link(b, d, 0);
		cToD = link(c, d, 0);
		aToH = link(a, h, 5 * day());
		link(a, i, 0);
		iToH = link(i, h, 0);
		project.recalculate();
	}

	private static long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}

	private NormalTask task(String name, int days) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(days * day());
		return task;
	}

	private Dependency link(Task from, Task to, long lag) {
		try {
			return DependencyService.getInstance().newDependency(from, to, DependencyType.FS, lag, this);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	@Test
	void nonDrivingLinkHasSlackAndLaggedLinkDrives() {
		assertFalse(TaskPathAnalyzer.isDriving(cToD), "C finishes before B, so C -> D has slack");
		assertTrue(TaskPathAnalyzer.isDriving(aToH), "the lagged link alone determines H");
		assertFalse(TaskPathAnalyzer.isDriving(iToH), "I finishes long before A + lag");
	}

	@Test
	void predecessorsFollowEveryLinkTransitively() {
		assertEquals(Set.of(a, b, c), TaskPathAnalyzer.path(d, TaskPathAnalyzer.Kind.PREDECESSORS));
		assertEquals(Set.of(a, i), TaskPathAnalyzer.path(h, TaskPathAnalyzer.Kind.PREDECESSORS));
		assertTrue(TaskPathAnalyzer.path(a, TaskPathAnalyzer.Kind.PREDECESSORS).isEmpty());
	}

	@Test
	void drivingPredecessorsSkipLinksWithSlackAndKeepLaggedDrivers() {
		assertEquals(Set.of(a, b), TaskPathAnalyzer.path(d, TaskPathAnalyzer.Kind.DRIVING_PREDECESSORS));
		assertEquals(Set.of(a), TaskPathAnalyzer.path(h, TaskPathAnalyzer.Kind.DRIVING_PREDECESSORS));
	}

	@Test
	void successorsFollowEveryLinkTransitively() {
		assertEquals(Set.of(b, c, d, h, i), TaskPathAnalyzer.path(a, TaskPathAnalyzer.Kind.SUCCESSORS));
		assertEquals(Set.of(d), TaskPathAnalyzer.path(c, TaskPathAnalyzer.Kind.SUCCESSORS));
		assertTrue(TaskPathAnalyzer.path(d, TaskPathAnalyzer.Kind.SUCCESSORS).isEmpty());
	}

	@Test
	void drivenSuccessorsSkipLinksWithSlack() {
		assertTrue(TaskPathAnalyzer.path(c, TaskPathAnalyzer.Kind.DRIVEN_SUCCESSORS).isEmpty());
		assertEquals(Set.of(b, c, d, h, i), TaskPathAnalyzer.path(a, TaskPathAnalyzer.Kind.DRIVEN_SUCCESSORS));
		assertEquals(Set.of(), TaskPathAnalyzer.path(i, TaskPathAnalyzer.Kind.DRIVEN_SUCCESSORS));
	}

	@Test
	void selectionIsExcludedAndEmptyInputsAreEmpty() {
		assertEquals(Set.of(), TaskPathAnalyzer.path(java.util.List.of(), TaskPathAnalyzer.Kind.SUCCESSORS));
		assertEquals(Set.of(h, i), TaskPathAnalyzer.path(java.util.List.of(a, b, c, d), TaskPathAnalyzer.Kind.SUCCESSORS));
	}
}
