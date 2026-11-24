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
package com.microproject.collaboration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.DependencyType;
import com.microproject.options.CalendarOption;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.transaction.MultipleTransactionManager;
import com.microproject.undo.DataFactoryUndoController;

class MpoTaskOperationServiceTest {
	@Test
	void replaysOrderedDependencyBatchWithOneFinalCalculation() throws Exception {
		Project project = createProject();
		NormalTask first = createTask(project, 101L);
		NormalTask second = createTask(project, 102L);
		NormalTask third = createTask(project, 103L);
		String firstId = UUID.randomUUID().toString();
		String actorId = UUID.randomUUID().toString();
		OperationLog.Operation firstLink = dependencyAdd(firstId, actorId, 1L, Set.of(), first, second);
		OperationLog.Operation secondLink = dependencyAdd(UUID.randomUUID().toString(), actorId, 2L,
			Set.of(firstId), second, third);
		int calculationStateBefore = project.getSchedulingAlgorithm().getCalculationStateCount();
		int transactionDepthBefore = MultipleTransactionManager.depth;

		new MpoTaskOperationService().apply(project, List.of(secondLink, firstLink));

		assertNotNull(first.getSuccessorList().findRight(second));
		assertNotNull(second.getSuccessorList().findRight(third));
		assertEquals(3, project.getSchedulingAlgorithm().getCalculationStateCount() - calculationStateBefore,
			"a replay batch should calculate once after all ordered operations have been applied");
		assertEquals(transactionDepthBefore, MultipleTransactionManager.depth);
	}

	@Test
	void partialReplayFailureKeepsEarlierOperationsAndRecalculatesBeforeThrowing() {
		Project project = createProject();
		NormalTask first = createTask(project, 201L);
		NormalTask second = createTask(project, 202L);
		String firstId = UUID.randomUUID().toString();
		String actorId = UUID.randomUUID().toString();
		OperationLog.Operation firstLink = dependencyAdd(firstId, actorId, 1L, Set.of(), first, second);
		OperationLog.Operation invalidLink = new OperationLog.Operation(UUID.randomUUID().toString(), actorId, 2L,
			Set.of(firstId), "dependency.add", UUID.randomUUID().toString(), Map.of(
				"predecessorLegacyUniqueId", second.getUniqueId(),
				"successorLegacyUniqueId", 999999L,
				"dependencyType", DependencyType.Kind.FS.code(), "lag", 0L));
		int calculationStateBefore = project.getSchedulingAlgorithm().getCalculationStateCount();
		int transactionDepthBefore = MultipleTransactionManager.depth;

		assertThrows(IOException.class,
			() -> new MpoTaskOperationService().apply(project, List.of(firstLink, invalidLink)));

		assertNotNull(first.getSuccessorList().findRight(second), "successful earlier replay operations remain applied");
		assertEquals(3, project.getSchedulingAlgorithm().getCalculationStateCount() - calculationStateBefore,
			"partial replay must recalculate its retained model state before propagating failure");
		assertEquals(transactionDepthBefore, MultipleTransactionManager.depth,
			"failed replay must release the outer scheduling transaction");
	}

	@Test
	void replayWithSummaryProgressKeepsTheScheduleBoundaryBetweenOperations() throws Exception {
		SummaryFixture expected = createSummaryFixture();
		SummaryFixture replayed = createSummaryFixture();
		String linkId = UUID.randomUUID().toString();
		String actorId = UUID.randomUUID().toString();
		OperationLog.Operation link = dependencyAdd(linkId, actorId, 1L, Set.of(),
			replayed.predecessor(), replayed.child());
		OperationLog.Operation summaryProgress = new OperationLog.Operation(UUID.randomUUID().toString(), actorId, 2L,
			Set.of(linkId), "task.update", UUID.randomUUID().toString(), Map.of(
				"legacyUniqueId", replayed.summary().getUniqueId(), "percentComplete", 1D));

		com.microproject.pm.dependency.DependencyService.getInstance().newDependency(expected.predecessor(),
			expected.child(), DependencyType.Kind.FS, 0L, null);
		expected.summary().setPercentComplete(1D);
		new MpoTaskOperationService().apply(replayed.project(), List.of(summaryProgress, link));

		assertEquals(expected.summary().getStart(), replayed.summary().getStart());
		assertEquals(expected.summary().getEnd(), replayed.summary().getEnd());
		assertEquals(expected.child().getStart(), replayed.child().getStart());
		assertEquals(expected.child().getStop(), replayed.child().getStop());
		assertEquals(expected.child().getActualStart(), replayed.child().getActualStart());
	}

	private static Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("mpo replay", undoController);
		Project project = Project.createProject(pool, undoController);
		project.initialize(false, false);
		return project;
	}

	private static NormalTask createTask(Project project, long uniqueId) {
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setUniqueId(uniqueId);
		return task;
	}

	private static SummaryFixture createSummaryFixture() {
		Project project = createProject();
		long day = CalendarOption.getInstance().getMillisPerDay();
		NormalTask predecessor = createTask(project, 301L);
		predecessor.setDuration(4L * day);
		com.microproject.node.Node summaryNode = project.createLocalTaskNode(null);
		NormalTask summary = (NormalTask) summaryNode.getImpl();
		summary.setUniqueId(302L);
		NormalTask child = (NormalTask) project.createLocalTaskNode(summaryNode).getImpl();
		child.setUniqueId(303L);
		child.setDuration(2L * day);
		project.recalculate();
		return new SummaryFixture(project, predecessor, summary, child);
	}

	private record SummaryFixture(Project project, NormalTask predecessor, NormalTask summary, NormalTask child) { }

	private static OperationLog.Operation dependencyAdd(String id, String actorId, long sequence,
		Set<String> parents, NormalTask predecessor, NormalTask successor) {
		return new OperationLog.Operation(id, actorId, sequence, parents, "dependency.add", UUID.randomUUID().toString(),
			Map.of("predecessorLegacyUniqueId", predecessor.getUniqueId(),
				"successorLegacyUniqueId", successor.getUniqueId(),
				"dependencyType", DependencyType.Kind.FS.code(), "lag", 0L));
	}
}
