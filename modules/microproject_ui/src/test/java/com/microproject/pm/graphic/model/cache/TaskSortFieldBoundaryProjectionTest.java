/*
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
 */
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.microproject.configuration.Dictionary;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.DefaultNodeModel;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.sorting.TaskSortSpec;
import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Display-sort boundary matrix for the M2.1 gate (#794, #408): every sortable
 * task field type x both directions, composed with an active filter and grouper.
 *
 * <p>Document-derived rules (not measured MSP behavior). Microsoft documents
 * Sort as a view operation with up to three keys and ascending/descending order,
 * and states that sorting does not renumber IDs unless the user explicitly asks
 * ("Renumber tasks permanently"): https://support.microsoft.com/en-us/project/sort-data-in-project
 * and https://learn.microsoft.com/en-us/office/vba/api/project.application.sort.
 * Microsoft does not specify tie order or empty-value placement; the product
 * rules asserted here are: exact ties keep their original ID order in BOTH
 * directions (stable sort, the comparator returns 0); text and numeric empty
 * values (empty name, zero duration/percent/cost) are ordinary smallest values;
 * missing (zero-sentinel) dates always sort last in either direction (covered
 * by TaskSortSpecTest). Task table and Gantt consume the same visible-node
 * projection, so one row order is asserted for both.
 */
class TaskSortFieldBoundaryProjectionTest {
	private static final long DAY_MS = CalendarOption.getInstance().getMillisPerDay();
	/** Whole weeks keep every start on the project start's weekday; equal durations make Finish order follow Start. */
	private static final long WEEK_MS = 7 * DAY_MS;

	/** Values for tasks A..E (created in that ID order); A and C tie exactly. */
	private record FieldCase(String name, String sorterId, BiConsumer<Task, Integer> apply,
			String ascending, String descending) { }

	/** Writes the schedule directly (as other schedule-fixture tests do): constraints need a CPM pass. */
	private static void placeOnWeek(Task task, int week) {
		long start = task.getProject().getStart() + WEEK_MS * week;
		task.getCurrentSchedule().setStart(start);
		task.getCurrentSchedule().setEnd(start + DAY_MS);
	}

	static Stream<Arguments> fieldCases() {
		List<FieldCase> cases = List.of(
			new FieldCase("Name", "Sorter.Name", (task, i) -> task.setName(new String[] { "Mango", "Apple", "Mango", "", "Zebra" }[i]),
					"DBACE", "EACBD"),
			new FieldCase("Start", "Sorter.StartDate", (task, i) -> placeOnWeek(task, new int[] { 3, 1, 3, 5, 2 }[i]),
					"BEACD", "DACEB"),
			new FieldCase("Finish", "Sorter.FinishDate", (task, i) -> placeOnWeek(task, new int[] { 3, 1, 3, 5, 2 }[i]),
					"BEACD", "DACEB"),
			new FieldCase("Duration", "Sorter.Duration", (task, i) -> ((NormalTask) task).setDuration(DAY_MS * new int[] { 2, 1, 2, 0, 3 }[i]),
					"DBACE", "EACBD"),
			new FieldCase("PercentComplete", "Sorter.PercentComplete",
					(task, i) -> {
						((NormalTask) task).setDuration(DAY_MS * 4);
						((NormalTask) task).setPercentComplete(new double[] { .5, .25, .5, 0, .75 }[i]);
					},
					"DBACE", "EACBD"),
			new FieldCase("Cost", "Sorter.Cost", (task, i) -> ((NormalTask) task).setFixedCost(new double[] { 200, 100, 200, 0, 300 }[i]),
					"DBACE", "EACBD"));
		return cases.stream().flatMap(c -> Stream.of(Arguments.of(c, true), Arguments.of(c, false)));
	}

	@ParameterizedTest(name = "{0} ascending={1}")
	@MethodSource("fieldCases")
	void sortOrdersFilteredGroupedRowsStablyWithoutMutatingTheModel(FieldCase fieldCase, boolean ascending) throws Exception {
		Dictionary.getInstance();
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("sort-boundary", undo), undo);
		project.initialize(false, false);
		Map<Task, String> labels = new IdentityHashMap<>();
		for (int i = 0; i < 5; i++) {
			NormalTask task = project.createScriptedTask();
			task.setName("T" + i);
			project.connectTask(task);
			project.getTaskOutlines().addToAll(task, null);
			fieldCase.apply().accept(task, i);
			labels.put(task, String.valueOf((char) ('A' + i)));
		}
		NormalTask hidden = project.createScriptedTask();
		hidden.setName("Hidden complete");
		project.connectTask(hidden);
		project.getTaskOutlines().addToAll(hidden, null);
		hidden.setDuration(DAY_MS);
		hidden.setPercentComplete(1.0d);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		List<Task> originalOrder = List.copyOf(project.getTaskList());
		List<Long> originalIds = originalOrder.stream().map(Task::getId).toList();
		List<Integer> originalLevels = originalOrder.stream().map(Task::getOutlineLevel).toList();
		boolean dirty = project.isDirty();
		boolean canUndo = undo.canUndo();
		boolean canRedo = undo.canRedo();
		long revision = project.getMutationState().revision();

		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, model);
		ViewNodeModelCache view = (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(
			reference, "sort-boundary-" + fieldCase.name() + ascending, transformer -> {
				ViewTransformer viewTransformer = (ViewTransformer) transformer;
				viewTransformer.setUserFilterId("Filter.IncompleteTasks");
				viewTransformer.setUserGrouperId("Grouper.CompleteIncomplete");
				viewTransformer.setTaskSortSpec(new TaskSortSpec(
						List.of(new TaskSortSpec.Key(fieldCase.sorterId(), ascending)), true));
			});
		reference.update();
		try {
			List<GraphicNode> rows = view.getVisibleNodes().getElements().stream()
				.filter(GraphicNode.class::isInstance).map(GraphicNode.class::cast)
				.filter(node -> !node.isGroup() && node.getNode().getImpl() instanceof Task).toList();
			StringBuilder order = new StringBuilder();
			for (GraphicNode row : rows) order.append(labels.getOrDefault((Task) row.getNode().getImpl(), "?"));
			assertFalse(order.toString().contains("?"), "the active filter must hide the completed task");
			assertEquals(ascending ? fieldCase.ascending() : fieldCase.descending(), order.toString(),
				fieldCase.name() + (ascending ? " ascending" : " descending")
					+ ": ordered, ties in original ID order, filtered task absent");
			// Table and Gantt share this projection: every row resolves to the same index.
			List<GraphicNode> all = view.getVisibleNodes().getElements();
			for (GraphicNode row : rows) {
				int index = view.getVisibleNodes().getProjectionIndex().rowForNode(row);
				assertSame(row, all.get(index), "projection row index must resolve back to the identical task node");
			}
			// Selection identity: the same task objects are selectable after the sort.
			for (Task task : labels.keySet()) {
				GraphicNode node = reference.getGraphicNode((Node) model.search(task));
				assertSame(task, node.getNode().getImpl());
			}
			assertEquals(originalOrder, project.getTaskList(), "display sort must not reorder tasks");
			assertEquals(originalIds, project.getTaskList().stream().map(Task::getId).toList(), "display sort must not renumber IDs");
			assertEquals(originalLevels, project.getTaskList().stream().map(Task::getOutlineLevel).toList(), "outline unchanged");
			assertEquals(dirty, project.isDirty(), "display sort must not dirty the project");
			assertEquals(canUndo, undo.canUndo(), "display sort must not add Undo history");
			assertEquals(canRedo, undo.canRedo());
			assertEquals(revision, project.getMutationState().revision(), "display sort must not mutate the model");
		} finally {
			view.close();
			reference.close();
		}
	}
}
