package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.RecurringTaskSpec;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

class RecurringTaskInsertionServiceTest {
	@Test
	void insertsSummaryImmediatelyAfterAnchorAndAddsOccurrencesAsChildren() {
		Project project = createProject();
		NodeModel nodeModel = project.getTaskModel();
		NormalTask anchorTask = project.createScriptedTask();
		anchorTask.setName("Anchor");
		Node anchorNode = nodeModel.search(anchorTask);
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Sprint Review",
			start(2026, Calendar.JUNE, 1),
			0L,
			RecurringTaskSpec.PatternType.DAILY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			2,
			null);

		Node summaryNode = new RecurringTaskInsertionService().insertRecurringTasks(
			project,
			nodeModel,
			anchorNode,
			project.getUndoController().getEditSupport(),
			spec);

		assertNotNull(summaryNode);
		assertSame(anchorNode.getParent(), summaryNode.getParent());
		assertEquals(anchorNode.getParent().getIndex(anchorNode) + 1, summaryNode.getParent().getIndex(summaryNode));
		assertEquals(spec.getName(), ((NormalTask) summaryNode.getImpl()).getName());
		assertTrue(((NormalTask) summaryNode.getImpl()).isSummary());
		List children = nodeModel.getChildren(summaryNode);
		assertEquals(2, children.size());
		assertEquals(start(2026, Calendar.JUNE, 1), ((NormalTask) ((Node) children.get(0)).getImpl()).getStart());
		assertEquals(start(2026, Calendar.JUNE, 2), ((NormalTask) ((Node) children.get(1)).getImpl()).getStart());
		assertTrue(((NormalTask) ((Node) children.get(1)).getImpl()).getStart()
			> ((NormalTask) ((Node) children.get(0)).getImpl()).getStart());
	}

	@Test
	void insertsAtRootWhenNoAnchorIsProvided() {
		Project project = createProject();
		NodeModel nodeModel = project.getTaskModel();
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Weekly Sync",
			start(2026, Calendar.JUNE, 1),
			0L,
			RecurringTaskSpec.PatternType.WEEKLY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			3,
			Set.of(Integer.valueOf(Calendar.MONDAY)));

		Node summaryNode = new RecurringTaskInsertionService().insertRecurringTasks(
			project,
			nodeModel,
			null,
			project.getUndoController().getEditSupport(),
			spec);

		List rootChildren = nodeModel.getChildren((Node) nodeModel.getRoot());
		assertEquals(summaryNode, rootChildren.get(rootChildren.size() - 1));
		assertEquals(3, nodeModel.getChildren(summaryNode).size());
	}

	@Test
	void insertsWithoutUndoSupport() {
		Project project = createProject();
		NodeModel nodeModel = project.getTaskModel();
		RecurringTaskSpec spec = new RecurringTaskSpec(
			"Milestone",
			start(2026, Calendar.JUNE, 1),
			0L,
			RecurringTaskSpec.PatternType.DAILY,
			RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
			0L,
			1,
			null);

		Node summaryNode = new RecurringTaskInsertionService().insertRecurringTasks(
			project,
			nodeModel,
			null,
			null,
			spec);

		assertNotNull(summaryNode);
		assertEquals(1, nodeModel.getChildren(summaryNode).size());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("recurring-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private long start(int year, int month, int dayOfMonth) {
		return CalendarOption.getInstance().makeValidStart(
			DateTime.calendarInstance(year, month, dayOfMonth).getTimeInMillis(),
			true);
	}
}
