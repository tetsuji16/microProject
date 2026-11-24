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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.microproject.options.CalendarOption;
import com.microproject.node.Node;
import com.microproject.node.NodeFactory;
import com.microproject.node.model.NodeModel;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.BulkScheduleBuilder;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Opt-in profiling harness for the core scheduler on large synthetic projects.
 * Run with {@code -Dmicroproject.profile=true}; skipped in the normal test task.
 * Optional: {@code -Dmicroproject.profile.scales=1000,2000,5000},
 * {@code -Dmicroproject.profile.phaseSize=100}, and
 * {@code -Dmicroproject.profile.mode=bulk|production-batch|sequential|builder}. See
 * docs/performance/scheduler-large-project-profile.md.
 */
@EnabledIfSystemProperty(named = "microproject.profile", matches = "true")
class LargeProjectSchedulingProfile {
	private static final int RESOURCES = 50;

	@Test
	void profileScales() throws Exception {
		int phaseSize = Integer.getInteger("microproject.profile.phaseSize", 100);
		String[] scales = System.getProperty("microproject.profile.scales", "1000,2000,5000").split(",");
		String mode = System.getProperty("microproject.profile.mode", "bulk");
		boolean builderMode = "builder".equalsIgnoreCase(mode);
		boolean bulkApis = !"sequential".equalsIgnoreCase(mode);
		boolean wholeBuildTransaction = "bulk".equalsIgnoreCase(mode);
		if (!List.of("bulk", "production-batch", "sequential", "builder").contains(mode.toLowerCase(java.util.Locale.ROOT)))
			throw new IllegalArgumentException("microproject.profile.mode must be bulk, production-batch, sequential or builder");
		// warm up the JIT on a small project so the first scale is not dominated by class loading
		run(300, 50, false, mode);
		System.out.println("PROFILE mode,tasks,phases,nodesMs,depsMs,assignMs,buildMs,fullRecalcMs,editMs,undoMs,nodePasses,dependencyPasses,assignmentPasses,fullRecalcPasses,editPasses,undoPasses");
		for (String scale : scales) run(Integer.parseInt(scale.trim()), phaseSize, true, mode);
	}

	private void run(int taskCount, int phaseSize, boolean report, String mode) throws Exception {
		boolean builderMode = "builder".equalsIgnoreCase(mode);
		boolean bulkApis = !"sequential".equalsIgnoreCase(mode);
		boolean wholeBuildTransaction = "bulk".equalsIgnoreCase(mode);
		long day = CalendarOption.getInstance().getMillisPerDay();
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("profile", undo);
		Project project = Project.createProject(pool, undo);
		if (!builderMode) project.initialize(false, false);
		long t0 = System.nanoTime();
		List<ResourceImpl> resources = new ArrayList<>();
		for (int i = 0; i < RESOURCES; i++) { ResourceImpl r = pool.newResourceInstance(); r.setName("R" + i); resources.add(r); }
		List<NormalTask> tasks = new ArrayList<>();
		int phases = (taskCount + phaseSize - 1) / phaseSize;
		int schedulingTransaction = wholeBuildTransaction ? project.fireMultipleTransaction(0, true) : 0;
		int calculationPassesAtStart = project.getSchedulingAlgorithm().getCalculationStateCount();
		if (bulkApis) {
			List<Node> phaseNodes = new ArrayList<>();
			for (int p = 0; p < phases; p++) {
				NormalTask phase = new NormalTask(project);
				phase.setName("Phase " + p);
				project.connectTask(phase);
				phaseNodes.add(NodeFactory.getInstance().createNode(phase));
			}
			project.getTaskModel().add(null, phaseNodes, NodeModel.NORMAL);
			for (int p = 0; p < phases; p++) {
				Node phaseNode = phaseNodes.get(p);
				List<Node> taskNodes = new ArrayList<>();
				for (int i = 0; i < phaseSize && tasks.size() < taskCount; i++) {
					NormalTask task = new NormalTask(project);
					project.connectTask(task);
					task.setName("T" + tasks.size());
					if (builderMode) task.setRawDuration((1 + tasks.size() % 5) * day);
					else task.setDuration((1 + tasks.size() % 5) * day);
					tasks.add(task);
					taskNodes.add(NodeFactory.getInstance().createNode(task));
				}
				project.getTaskModel().add(phaseNode, taskNodes, NodeModel.NORMAL);
			}
			if (!builderMode) project.getSchedulingAlgorithm().initialize(project);
		} else {
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
		}
		long tNodes = System.nanoTime();
		int calculationPassesAfterNodes = project.getSchedulingAlgorithm().getCalculationStateCount();
		if (bulkApis) {
			// This is the bulk sequential-link operation used by the task-link workflow.
			DependencyService.getInstance().connect(new ArrayList<>(tasks), this, null);
		} else {
			for (int i = 1; i < tasks.size(); i++)
				DependencyService.getInstance().newDependency(tasks.get(i - 1), tasks.get(i), DependencyType.FS, 0L, this);
		}
		long tDeps = System.nanoTime();
		int calculationPassesAfterDependencies = project.getSchedulingAlgorithm().getCalculationStateCount();
		if (bulkApis) {
			for (int resourceIndex = 0; resourceIndex < resources.size(); resourceIndex++) {
				List<NormalTask> resourceTasks = new ArrayList<>();
				for (int i = resourceIndex; i < tasks.size(); i += resources.size())
					resourceTasks.add(tasks.get(i));
				AssignmentService.getInstance().newAssignments(resourceTasks, List.of(resources.get(resourceIndex)),
					1D, 0L, this, true);
			}
		} else {
			for (int i = 0; i < tasks.size(); i++)
				AssignmentService.getInstance().newAssignment(tasks.get(i), resources.get(i % RESOURCES), 1D, 0L, this);
		}
		if (builderMode) BulkScheduleBuilder.initialize(project, false, false);
		else if (wholeBuildTransaction) project.fireMultipleTransaction(schedulingTransaction, false);
		long t1 = System.nanoTime();
		int calculationPassesAfterAssignments = project.getSchedulingAlgorithm().getCalculationStateCount();
		project.recalculate();
		long t2 = System.nanoTime();
		int calculationPassesAfterFullRecalc = project.getSchedulingAlgorithm().getCalculationStateCount();
		NormalTask early = tasks.get(5);
		long start = early.getStart(); long end = early.getEnd();
		long lastFinishBefore = tasks.get(tasks.size() - 1).getEnd();
		undo.beginUpdate();
		ScheduleService.getInstance().setInterval(this, early, start, early.getEffectiveWorkCalendar().add(end, 3 * day, false),
			new ScheduleInterval(start, end), undo.getEditSupport());
		undo.endUpdate();
		long t3 = System.nanoTime();
		int calculationPassesAfterEdit = project.getSchedulingAlgorithm().getCalculationStateCount();
		long lastFinishAfter = tasks.get(tasks.size() - 1).getEnd();
		if (report && !builderMode) assertTrue(lastFinishAfter > lastFinishBefore,
			"The schedule edit should move the last task finish in " + mode + " mode at " + taskCount + " tasks");
		undo.undo();
		long t4 = System.nanoTime();
		int calculationPassesAfterUndo = project.getSchedulingAlgorithm().getCalculationStateCount();
		if (report) assertEquals(lastFinishBefore, tasks.get(tasks.size() - 1).getEnd(),
			"Undo should restore the last task finish in " + mode + " mode at " + taskCount + " tasks");
		if (report) {
			System.out.printf("PROFILE %s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d (last finish moved: %b, restored: %b)%n", mode, tasks.size(), phases,
				(tNodes - t0) / 1_000_000, (tDeps - tNodes) / 1_000_000, (t1 - tDeps) / 1_000_000, (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (t3 - t2) / 1_000_000, (t4 - t3) / 1_000_000,
				calculationPassesAfterNodes - calculationPassesAtStart,
				calculationPassesAfterDependencies - calculationPassesAfterNodes,
				calculationPassesAfterAssignments - calculationPassesAfterDependencies,
				calculationPassesAfterFullRecalc - calculationPassesAfterAssignments,
				calculationPassesAfterEdit - calculationPassesAfterFullRecalc,
				calculationPassesAfterUndo - calculationPassesAfterEdit,
				lastFinishAfter != lastFinishBefore, tasks.get(tasks.size() - 1).getEnd() == lastFinishBefore);
		}
	}
}
