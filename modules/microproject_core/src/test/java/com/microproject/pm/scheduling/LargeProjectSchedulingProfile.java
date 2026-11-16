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
package com.microproject.pm.scheduling;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Opt-in profiling harness for the core scheduler on large synthetic projects.
 * Run with {@code -Dmicroproject.profile=true}; skipped in the normal test task.
 * Optional: {@code -Dmicroproject.profile.scales=1000,2000,5000} and
 * {@code -Dmicroproject.profile.phaseSize=100}. See
 * docs/performance/scheduler-large-project-profile.md.
 */
@EnabledIfSystemProperty(named = "microproject.profile", matches = "true")
class LargeProjectSchedulingProfile {
	private static final int RESOURCES = 50;

	@Test
	void profileScales() throws Exception {
		int phaseSize = Integer.getInteger("microproject.profile.phaseSize", 100);
		String[] scales = System.getProperty("microproject.profile.scales", "1000,2000,5000").split(",");
		// warm up the JIT on a small project so the first scale is not dominated by class loading
		run(300, 50, false);
		System.out.println("PROFILE tasks,phases,nodesMs,depsMs,assignMs,buildMs,fullRecalcMs,editMs,undoMs");
		for (String scale : scales) run(Integer.parseInt(scale.trim()), phaseSize, true);
	}

	private void run(int taskCount, int phaseSize, boolean report) throws Exception {
		long day = CalendarOption.getInstance().getMillisPerDay();
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("profile", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		long t0 = System.nanoTime();
		List<ResourceImpl> resources = new ArrayList<>();
		for (int i = 0; i < RESOURCES; i++) { ResourceImpl r = pool.newResourceInstance(); r.setName("R" + i); resources.add(r); }
		List<NormalTask> tasks = new ArrayList<>();
		int phases = (taskCount + phaseSize - 1) / phaseSize;
		for (int p = 0; p < phases; p++) {
			var phaseNode = project.createLocalTaskNode(null);
			((NormalTask) phaseNode.getImpl()).setName("Phase " + p);
			for (int i = 0; i < phaseSize && tasks.size() < taskCount; i++) {
				NormalTask task = (NormalTask) project.createLocalTaskNode(phaseNode).getImpl();
				task.setName("T" + tasks.size());
				task.setDuration((1 + tasks.size() % 5) * day);
				tasks.add(task);
			}
		}
		long tNodes = System.nanoTime();
		// finish-to-start chain across everything, plus cross links every 7th task jumping back 20 tasks
		for (int i = 1; i < tasks.size(); i++) {
			DependencyService.getInstance().newDependency(tasks.get(i - 1), tasks.get(i), DependencyType.FS, 0L, this);
			if (i % 7 == 0 && i >= 20) DependencyService.getInstance().newDependency(tasks.get(i - 20), tasks.get(i), DependencyType.FS, 0L, this);
		}
		long tDeps = System.nanoTime();
		for (int i = 0; i < tasks.size(); i++)
			AssignmentService.getInstance().newAssignment(tasks.get(i), resources.get(i % RESOURCES), 1D, 0L, this);
		long t1 = System.nanoTime();
		project.recalculate();
		long t2 = System.nanoTime();
		NormalTask early = tasks.get(5);
		long start = early.getStart(); long end = early.getEnd();
		long lastFinishBefore = tasks.get(tasks.size() - 1).getEnd();
		undo.beginUpdate();
		ScheduleService.getInstance().setInterval(this, early, start, early.getEffectiveWorkCalendar().add(end, 3 * day, false),
			new ScheduleInterval(start, end), undo.getEditSupport());
		undo.endUpdate();
		long t3 = System.nanoTime();
		long lastFinishAfter = tasks.get(tasks.size() - 1).getEnd();
		undo.undo();
		long t4 = System.nanoTime();
		if (report) {
			System.out.printf("PROFILE %d,%d,%d,%d,%d,%d,%d,%d,%d (last finish moved: %b, restored: %b)%n", tasks.size(), phases,
				(tNodes - t0) / 1_000_000, (tDeps - tNodes) / 1_000_000, (t1 - tDeps) / 1_000_000, (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (t3 - t2) / 1_000_000, (t4 - t3) / 1_000_000,
				lastFinishAfter != lastFinishBefore, tasks.get(tasks.size() - 1).getEnd() == lastFinishBefore);
		}
	}
}
