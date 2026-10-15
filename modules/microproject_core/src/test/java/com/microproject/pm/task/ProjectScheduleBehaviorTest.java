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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.AssignmentNodeModel;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.ProjectStartDateEdit;
import com.microproject.util.Environment;
import com.microproject.workspace.SavableToWorkspace;

class ProjectScheduleBehaviorTest {
	@Test
	void assignmentOutlinePopulationIsIdempotent() {
		Project project = createProject();
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0d, 0L, this);
		AssignmentNodeModel outline = (AssignmentNodeModel) project.getTaskOutline();
		Node taskNode = outline.search(task);

		outline.addAssignments(outline.iterator());
		assertEquals(1, assignmentRows(outline, taskNode, assignment));

		outline.addAssignments(outline.iterator());
		assertEquals(1, assignmentRows(outline, taskNode, assignment));
	}

	private long assignmentRows(AssignmentNodeModel outline, Node taskNode, Assignment assignment) {
		List<Node> children = outline.getChildren(taskNode);
		if (children == null)
			return 0;
		return children.stream()
			.filter(node -> node.getImpl() == assignment)
			.count();
	}

	@Test
	void outlineLifecycleSharesAssignmentDocumentBinding() {
		Project project = createProject();
		AssignmentNodeModel assignments = (AssignmentNodeModel) project.getTaskOutline();

		assertSame(project, assignments.getDocument());
		project.disconnectOutlines();
		assertNull(assignments.getDocument());

		project.initializeOutlines();
		assertSame(project, assignments.getDocument());
	}

	@Test
	void projectTypeMetadataUsesWildcardClass() throws Exception {
		Project project = createProject();

		assertSame(Project.class, project.getType());
	}

	@Test
	void projectEqualityUsesNameOnlyForDataObjects() {
		Project project = createProject();
		Project sameName = createProject();
		project.setName("Shared project name");
		sameName.setName("Shared project name");

		assertTrue(project.equals(sameName));
		assertFalse(project.equals(new Object()));
	}

	@Test
	void childFactoryUsesParentTaskEnclosingProject() {
		Project project = createProject();
		Project otherProject = createProject();
		NormalTask parent = otherProject.createScriptedTask();

		assertSame(project, project.getFactoryToUseForChildOfParent(null));
		assertSame(project, project.getFactoryToUseForChildOfParent(new Object()));
		assertSame(otherProject, project.getFactoryToUseForChildOfParent(parent));
	}

	@Test
	void moveIntervalUpdatesProjectSpan() {
		Project project = createProject();
		long start = project.getStart();
		long end = project.getEffectiveWorkCalendar().add(start, 2L * day(), false);
		project.setStart(start);
		project.setEnd(end);
		ScheduleInterval oldInterval = new ScheduleInterval(start, end);

		long newStart = project.getEffectiveWorkCalendar().add(start, day(), false);
		long newEnd = project.getEffectiveWorkCalendar().add(end, day(), false);

		project.moveInterval(this, newStart, newEnd, oldInterval, false);

		assertEquals(newStart, project.getStart(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
		assertEquals(newEnd, project.getEnd(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
	}

	@Test
	void backupDetailRoundTripsProjectSpan() {
		Project project = createProject();
		long originalStart = project.getStart();
		long originalEnd = project.getEffectiveWorkCalendar().add(originalStart, 3L * day(), false);
		project.setStart(originalStart);
		project.setEnd(originalEnd);
		Object backup = project.backupDetail();

		assertNotNull(backup);

		long movedStart = project.getEffectiveWorkCalendar().add(originalStart, day(), false);
		long movedEnd = project.getEffectiveWorkCalendar().add(originalEnd, day(), false);
		project.setStart(movedStart);
		project.setEnd(movedEnd);

		project.restoreDetail(this, backup, false);

		assertEquals(originalStart, project.getStart(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
		assertEquals(originalEnd, project.getEnd(), "projectStart=" + project.getStart() + " projectEnd=" + project.getEnd());
	}

	@Test
	void setDirtyUpdatesProjectFlag() {
		Project project = createProject();

		project.setDirty(true);

		assertTrue(project.isDirty());
	}

	@Test
	void markingProjectTasksUnchangedClearsNormalTaskDirtyState() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		task.setDirty(true);

		project.setAllTasksAsUnchangedFromPersisted(true);

		assertFalse(task.isDirty());
		assertEquals(task.getStart(), task.getLastSavedStart());
		assertEquals(task.getEnd(), task.getLastSavedFinish());
	}

	@Test
	void uniqueIdRoundTripsThroughIdentityFacade() {
		Project project = createProject();

		project.setUniqueId(42L);

		assertEquals(42L, project.getUniqueId());
	}

	@Test
	void taskSheetProjectSummaryEditsUseEnvelope() {
		Project project = createProject();
		NormalTask rootTask = project.createScriptedTask();
		rootTask.setName("Root");
		rootTask.setDuration(2L * day());
		project.connectTask(rootTask);
		project.getTaskOutlines().addToAll(rootTask, null);
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);

		long manualStart = project.getEffectiveWorkCalendar().add(rootTask.getStart(), -day(), false);
		project.setStart(manualStart, context);
		project.setDuration(10L * day(), context);

		assertTrue(project.hasSummaryEnvelope());
		assertEquals(manualStart, project.getSummaryEnvelope().getManualStart().longValue());
		assertEquals(10L * day(), project.getSummaryEnvelope().getManualDuration().longValue());
		assertEquals(rootTask.getStart(), project.calculateRollupSpan().getStart());
		assertEquals(rootTask.getEnd(), project.calculateRollupSpan().getFinish());
	}

	@Test
	void changingForwardProjectStartRecalculatesUnconstrainedTasks() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		task.setName("Move with project");
		task.setDuration(day());
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		project.recalculate();

		long originalTaskStart = task.getStart();
		long newProjectStart = project.getEffectiveWorkCalendar().add(project.getStartDate(), day(), false);
		project.setStartDate(newProjectStart);
		project.recalculate();

		assertEquals(newProjectStart, project.getStartDate());
		assertEquals(project.getEffectiveWorkCalendar().add(originalTaskStart, day(), false), task.getStart());
	}

	@Test
	void moveProjectUndoEditRestoresAndReappliesTheProjectBoundary() {
		Project project = createProject();
		long before = project.getStartDate();
		long after = project.getEffectiveWorkCalendar().add(before, day(), false);
		project.setStartDate(after);
		ProjectStartDateEdit edit = new ProjectStartDateEdit(project, before, project.getStartDate());

		edit.undo();
		assertEquals(before, project.getStartDate());
		edit.redo();
		assertEquals(after, project.getStartDate());
	}

	@Test
	void taskLookupByIdAndUniqueIdFindTheInsertedTask() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		task.setId(42);
		task.setUniqueId(84L);

		assertSame(task, Project.findTaskById(Integer.valueOf(42), project.getTasks()));
		assertSame(task, project.findByUniqueId(84L));
	}

	@Test
	void clearSnapshotRemovesTheStoredSnapshot() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);

		Integer snapshotId = Integer.valueOf(2);
		task.saveCurrentToSnapshot(snapshotId);

		ReversibleModelChange change = project.clearSnapshot(snapshotId, false, Collections.singletonList(task));

		Object backup = task.backupDetail(snapshotId);
		assertNotNull(backup);
		assertNull(((TaskBackup) backup).snapshot);

		assertTrue(change.hasChanged());
		change.undo();
		Object restoredBackup = task.backupDetail(snapshotId);
		assertNotNull(((TaskBackup) restoredBackup).snapshot);

		change.redo();
		Object redoneBackup = task.backupDetail(snapshotId);
		assertNull(((TaskBackup) redoneBackup).snapshot);
	}

	@Test
	void clearAssignmentOnlyImportedBaselineIsUndoableAndBaselineEventsReflectProjectState() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);
		NormalTask unselectedTask = project.createScriptedTask();
		project.connectTask(unselectedTask);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0d, 0L, this);
		Integer slot = Integer.valueOf(Snapshottable.BASELINE_1);
		Assignment.BaselineValues importedValues = new Assignment.BaselineValues(1L, 2L, 3L);
		assignment.importBaselineValues(Snapshottable.BASELINE_1, importedValues.start(),
			importedValues.finish(), importedValues.work());
		List<ScheduleEvent> baselineEvents = new ArrayList<>();
		ScheduleEventListener listener = event -> {
			if (ScheduleEvent.BASELINE.equals(event.getType())) baselineEvents.add(event);
		};
		project.addScheduleListener(listener);
		try {
			ReversibleModelChange change = project.clearSnapshot(slot, false, Collections.singletonList(task));

			assertTrue(change.hasChanged());
			assertNull(assignment.getImportedBaselineValues(Snapshottable.BASELINE_1));
			assertFalse(baselineEvents.getLast().isSaveSnapshot(),
				"Assignment-only values must not advertise a Gantt task baseline row");
			change.undo();
			assertEquals(importedValues, assignment.getImportedBaselineValues(Snapshottable.BASELINE_1));
			assertFalse(baselineEvents.getLast().isSaveSnapshot(),
				"Undo restores assignment data but must keep task-row presence empty");
			change.redo();
			assertNull(assignment.getImportedBaselineValues(Snapshottable.BASELINE_1));
			assertFalse(baselineEvents.getLast().isSaveSnapshot());

			project.saveCurrentToSnapshot(slot, false, Collections.singletonList(task));
			unselectedTask.saveCurrentToSnapshot(slot);
			ReversibleModelChange clearSelected = project.clearSnapshot(slot, false, Collections.singletonList(task));
			assertTrue(clearSelected.hasChanged());
			assertTrue(baselineEvents.getLast().isSaveSnapshot(),
				"Clearing a selected task must retain the row while another project task has the baseline");
			project.clearSnapshot(slot, false, Collections.singletonList(unselectedTask));
			assertFalse(baselineEvents.getLast().isSaveSnapshot(),
				"Clearing the final task baseline must remove the project baseline row");
		} finally {
			project.removeScheduleListener(listener);
		}
	}

	@Test
	void baselineOccupancyQueryHonorsScopeAndImportedAssignmentOnlyValues() {
		Project project = createProject();
		NormalTask selected = project.createScriptedTask();
		NormalTask unselected = project.createScriptedTask();
		project.connectTask(selected);
		project.connectTask(unselected);
		unselected.saveCurrentToSnapshot(Snapshottable.BASELINE_1);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(selected, resource, 1.0d, 0L, this);
		Assignment.BaselineValues imported = new Assignment.BaselineValues(10L, 20L, 30L);
		assignment.importBaselineValues(Snapshottable.BASELINE_2,
			imported.start(), imported.finish(), imported.work());

		assertFalse(project.hasBaselineData(Snapshottable.BASELINE_1, false, List.of(selected)),
			"A baseline on a task outside the selected scope must not trigger overwrite confirmation");
		assertTrue(project.hasBaselineData(Snapshottable.BASELINE_1, false, List.of(unselected)));
		assertTrue(project.hasBaselineData(Snapshottable.BASELINE_1, true, null),
			"Entire-project scope must see an unselected task baseline");
		assertTrue(project.hasBaselineData(Snapshottable.BASELINE_2, false, List.of(selected)),
			"Imported assignment-only values count as data even without a task snapshot");
		assertFalse(project.hasBaselineData(Snapshottable.BASELINE_2, false, List.of(unselected)));
		assertFalse(project.hasBaselineData(Snapshottable.BASELINE_3, true, null),
			"A different empty slot must not be reported as occupied");
		assertNull(selected.getSnapshot(Integer.valueOf(Snapshottable.BASELINE_2)),
			"The occupancy preflight must not create a task snapshot");
		assertEquals(imported, assignment.getImportedBaselineValues(Snapshottable.BASELINE_2),
			"The occupancy preflight must not rewrite imported assignment metadata");
	}

	@Test
	void projectBaselinesRemainIndependentFromCurrentAndOtherBaselines() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);
		long originalStart = project.getStart();
		task.setStart(originalStart);
		task.setDuration(day());

		project.saveCurrentToSnapshot(Snapshottable.BASELINE, true, null);
		long baselineStart = task.getBaselineStart(Snapshottable.BASELINE);

		task.setStart(project.getEffectiveWorkCalendar().add(originalStart, day(), false));
		project.saveCurrentToSnapshot(Snapshottable.BASELINE_1, true, null);

		assertEquals(originalStart, baselineStart);
		assertEquals(baselineStart, task.getBaselineStart(Snapshottable.BASELINE));
		assertEquals(task.getStart(), task.getBaselineStart(Snapshottable.BASELINE_1));
	}

	@Test
	void savingBaselineReturnsAReversibleModelChange() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);

		ReversibleModelChange change = project.saveCurrentToSnapshot(Snapshottable.BASELINE, true, null);

		assertTrue(change.hasChanged());
		assertNotNull(task.getSnapshot(Snapshottable.BASELINE));
		change.undo();
		assertNull(task.getSnapshot(Snapshottable.BASELINE));
		change.redo();
		assertNotNull(task.getSnapshot(Snapshottable.BASELINE));
	}

	@Test
	void overwritingSelectedBaselineRestoresPriorAndAbsentTaskSnapshotsOnUndoRedo() {
		Project project = createProject();
		NormalTask existing = project.createScriptedTask();
		NormalTask previouslyUnbaselined = project.createScriptedTask();
		NormalTask outsideSelection = project.createScriptedTask();
		project.connectTask(existing);
		project.connectTask(previouslyUnbaselined);
		project.connectTask(outsideSelection);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.getStandardRate().setValue(10.0d);
		Assignment existingAssignment = AssignmentService.getInstance().newAssignment(existing, resource, 1.0d, 0L, this);
		Assignment newAssignment = AssignmentService.getInstance().newAssignment(previouslyUnbaselined, resource, 1.0d, 0L, this);
		long hour = 60L * 60L * 1000L;

		long start = project.getStartDate();
		existing.setStart(start);
		existing.setDuration(day());
		existingAssignment.setWork(8L * hour, null);
		previouslyUnbaselined.setStart(project.getEffectiveWorkCalendar().add(start, day(), false));
		previouslyUnbaselined.setDuration(day());
		newAssignment.setWork(16L * hour, null);
		outsideSelection.setStart(project.getEffectiveWorkCalendar().add(start, 2L * day(), false));
		outsideSelection.setDuration(day());

		Integer slot = Integer.valueOf(Snapshottable.BASELINE_1);
		Integer unrelatedSlot = Integer.valueOf(Snapshottable.BASELINE);
		existing.saveCurrentToSnapshot(slot);
		existing.saveCurrentToSnapshot(unrelatedSlot);
		outsideSelection.saveCurrentToSnapshot(slot);
		long oldExistingStart = existing.getBaselineStart(Snapshottable.BASELINE_1);
		double originalFixedCost = ((TaskSnapshot) existing.getSnapshot(slot)).fixedCost;
		Assignment.BaselineValues importedOverride = new Assignment.BaselineValues(oldExistingStart,
			Long.valueOf(existing.getBaselineFinish(Snapshottable.BASELINE_1)), Long.valueOf(5L * day()));
		existingAssignment.importBaselineValues(Snapshottable.BASELINE_1, importedOverride.start(),
			importedOverride.finish(), importedOverride.work());
		double oldExistingCost = existingAssignment.getBaselineCost(Snapshottable.BASELINE_1, null);
		long unrelatedStart = outsideSelection.getBaselineStart(Snapshottable.BASELINE_1);
		long unrelatedOtherSlotStart = existing.getBaselineStart(Snapshottable.BASELINE);

		existing.setStart(project.getEffectiveWorkCalendar().add(start, 3L * day(), false));
		existing.setDuration(3L * day());
		existingAssignment.setWork(24L * hour, null);
		previouslyUnbaselined.setStart(project.getEffectiveWorkCalendar().add(start, 4L * day(), false));
		previouslyUnbaselined.setDuration(4L * day());
		newAssignment.setWork(32L * hour, null);
		long replacementExistingStart = existing.getStart();
		long replacementNewStart = previouslyUnbaselined.getStart();
		long currentExistingStart = existing.getStart();
		long currentExistingWork = existingAssignment.getWork(null);
		long currentNewStart = previouslyUnbaselined.getStart();
		long currentNewWork = newAssignment.getWork(null);

		ReversibleModelChange change = project.saveCurrentToSnapshot(slot, false,
			List.of(existing, previouslyUnbaselined));
		Assignment.BaselineValues replacementExistingValues = existingAssignment.getBaselineValues(Snapshottable.BASELINE_1);
		Assignment.BaselineValues replacementNewValues = newAssignment.getBaselineValues(Snapshottable.BASELINE_1);
		double replacementExistingCost = existingAssignment.getBaselineCost(Snapshottable.BASELINE_1, null);
		double replacementFixedCost = ((TaskSnapshot) existing.getSnapshot(slot)).fixedCost;
		assertEquals(replacementExistingStart, existing.getBaselineStart(Snapshottable.BASELINE_1));
		assertEquals(replacementExistingValues, existingAssignment.getBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(replacementExistingCost, existingAssignment.getBaselineCost(Snapshottable.BASELINE_1, null));
		assertTrue(!importedOverride.equals(replacementExistingValues),
			"Overwrite must replace the imported assignment baseline values");
		assertEquals(replacementNewStart, previouslyUnbaselined.getBaselineStart(Snapshottable.BASELINE_1));
		assertEquals(replacementNewValues, newAssignment.getBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(unrelatedStart, outsideSelection.getBaselineStart(Snapshottable.BASELINE_1));

		change.undo();
		assertEquals(oldExistingStart, existing.getBaselineStart(Snapshottable.BASELINE_1));
		assertEquals(importedOverride, existingAssignment.getBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(importedOverride, existingAssignment.getImportedBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(oldExistingCost, existingAssignment.getBaselineCost(Snapshottable.BASELINE_1, null));
		assertNull(previouslyUnbaselined.getSnapshot(slot));
		assertEquals(unrelatedStart, outsideSelection.getBaselineStart(Snapshottable.BASELINE_1));
		assertEquals(unrelatedOtherSlotStart, existing.getBaselineStart(Snapshottable.BASELINE));
		assertEquals(currentExistingStart, existing.getStart(), "Undo must leave the current task schedule untouched");
		assertEquals(currentExistingWork, existingAssignment.getWork(null), "Undo must leave current assignment work untouched");
		assertEquals(currentNewStart, previouslyUnbaselined.getStart());
		assertEquals(currentNewWork, newAssignment.getWork(null));
		((TaskSnapshot) existing.getSnapshot(slot)).fixedCost = 77.0d;

		change.redo();
		assertEquals(replacementExistingStart, existing.getBaselineStart(Snapshottable.BASELINE_1));
		assertEquals(replacementExistingValues, existingAssignment.getBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(replacementExistingCost, existingAssignment.getBaselineCost(Snapshottable.BASELINE_1, null));
		assertEquals(replacementNewStart, previouslyUnbaselined.getBaselineStart(Snapshottable.BASELINE_1));
		assertEquals(replacementNewValues, newAssignment.getBaselineValues(Snapshottable.BASELINE_1));
		assertEquals(unrelatedStart, outsideSelection.getBaselineStart(Snapshottable.BASELINE_1));
		assertNull(existingAssignment.getImportedBaselineValues(Snapshottable.BASELINE_1),
			"Redo must restore the replacement slot without the overwritten imported override");
		assertEquals(currentExistingStart, existing.getStart());
		assertEquals(currentExistingWork, existingAssignment.getWork(null));
		assertEquals(replacementFixedCost, ((TaskSnapshot) existing.getSnapshot(slot)).fixedCost);
		((TaskSnapshot) existing.getSnapshot(slot)).fixedCost = 88.0d;
		change.undo();
		assertEquals(originalFixedCost, ((TaskSnapshot) existing.getSnapshot(slot)).fixedCost,
			"Undo state must remain isolated from later edits to the live baseline snapshot");
	}

	@Test
	void selectedBaselineRollupUsesLeafFrontierAndRestoresAffectedSummaries() {
		Project project = createProject();
		NormalTask root = project.createScriptedTask();
		NormalTask nested = project.createScriptedTask();
		NormalTask untouchedSummary = project.createScriptedTask();
		NormalTask first = project.createScriptedTask();
		NormalTask second = project.createScriptedTask();
		NormalTask third = project.createScriptedTask();
		NormalTask noBaseline = project.createScriptedTask();
		attachWbsChildren(root, nested, untouchedSummary);
		attachWbsChildren(nested, first, second, noBaseline);
		attachWbsChildren(untouchedSummary, third);
		long hour = 60L * 60L * 1000L;
		long start = project.getStartDate();
		setPlan(first, start, 2L * hour, 10.0d);
		setPlan(second, start + 24L * hour, 4L * hour, 20.0d);
		setPlan(third, start + 48L * hour, 6L * hour, 30.0d);
		setPlan(noBaseline, start - 72L * hour, 1L * hour, 40.0d);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment firstAssignment = AssignmentService.getInstance().newAssignment(first, resource, 1.0d, 0L, this);
		Assignment secondAssignment = AssignmentService.getInstance().newAssignment(second, resource, 1.0d, 0L, this);
		Assignment thirdAssignment = AssignmentService.getInstance().newAssignment(third, resource, 1.0d, 0L, this);
		firstAssignment.setWork(8L * hour, null);
		secondAssignment.setWork(16L * hour, null);
		thirdAssignment.setWork(24L * hour, null);
		setPlan(first, start, 2L * hour, 10.0d);
		setPlan(second, start + 24L * hour, 4L * hour, 20.0d);
		setPlan(third, start + 48L * hour, 6L * hour, 30.0d);
		int slot = Snapshottable.BASELINE_1;
		first.saveCurrentToSnapshot(Integer.valueOf(slot));
		first.saveCurrentToSnapshot(Integer.valueOf(Snapshottable.BASELINE));
		second.saveCurrentToSnapshot(Integer.valueOf(slot));
		third.saveCurrentToSnapshot(Integer.valueOf(slot));
		nested.saveCurrentToSnapshot(Integer.valueOf(slot));
		untouchedSummary.saveCurrentToSnapshot(Integer.valueOf(slot));
		root.saveCurrentToSnapshot(Integer.valueOf(slot));
		assertTrue(project.hasBaselineData(slot, false, List.of(noBaseline),
			new BaselineRollupOptions(true, false)),
			"Overwrite preflight must include an already-baselined ancestor of a newly selected leaf");
		long untouchedStart = untouchedSummary.getBaselineStart(slot);
		long otherSlotStart = first.getBaselineStart(Snapshottable.BASELINE);
		int liveAssignmentsBefore = first.getAssignments().size() + second.getAssignments().size()
			+ third.getAssignments().size();

		long replacementStart = start + 8L * hour;
		firstAssignment.setWork(12L * hour, null);
		setPlan(first, replacementStart, 2L * hour, 10.0d);
		ReversibleModelChange change = project.saveCurrentToSnapshot(Integer.valueOf(slot), false,
			List.of(first), new BaselineRollupOptions(true, false));

		assertEquals(replacementStart, nested.getBaselineStart(slot), "The affected nested summary starts at its earliest saved child");
		assertEquals(start + 28L * hour, nested.getBaselineFinish(slot));
		assertEquals(replacementStart, root.getBaselineStart(slot));
		assertEquals(start + 54L * hour, root.getBaselineFinish(slot));
		assertEquals(12L * hour + 16L * hour + 24L * hour,
			root.getBaselineWork(slot, 0L, Long.MAX_VALUE),
			"Nested summary snapshots must not be counted again alongside their leaves");
		TaskSnapshot rolledSnapshot = (TaskSnapshot) root.getSnapshot(Integer.valueOf(slot));
		assertEquals(3, rolledSnapshot.getAssignments().size(),
			"A same-resource summary must retain each independently identified child baseline profile");
		java.util.Set<Long> rolledAssignmentIds = new java.util.HashSet<>();
		for (Object value : rolledSnapshot.getAssignments()) {
			Assignment rolledAssignment = (Assignment) value;
			assertSame(root, rolledAssignment.getTask());
			assertTrue(rolledAssignment.isBaselineAssignment());
			assertTrue(rolledAssignmentIds.add(rolledAssignment.getUniqueId()),
				"Rolled assignment profiles must keep independent IDs");
		}
		Assignment firstResourceMatch = rolledSnapshot.findAssignment(resource);
		assertSame(rolledSnapshot.getAssignments().get(0), firstResourceMatch,
			"Resource lookup keeps its established first-match behavior; task-level time-phased totals iterate every profile");
		TaskSnapshot clonedSnapshot = (TaskSnapshot) rolledSnapshot.clone();
		assertEquals(3, clonedSnapshot.getAssignments().size(), "Snapshot clone must preserve all distinct roll-up profiles");
		Assignment removedProfile = (Assignment) clonedSnapshot.getAssignments().get(1);
		clonedSnapshot.removeAssignment(removedProfile);
		assertEquals(2, clonedSnapshot.getAssignments().size(), "Removing one duplicate-resource profile must preserve its sibling");
		assertEquals(36L * hour, clonedSnapshot.work(0L, Long.MAX_VALUE),
			"Removing one profile must subtract only that profile's baseline work");
		Assignment baselineContribution = (Assignment) rolledSnapshot.getAssignments().get(0);
		assertThrows(IllegalArgumentException.class,
			() -> rolledSnapshot.addBaselineRollupAssignment(root, Snapshottable.CURRENT, baselineContribution));
		assertThrows(IllegalArgumentException.class,
			() -> rolledSnapshot.addBaselineRollupAssignment(root, Snapshottable.TIMESHEET, baselineContribution));
		assertEquals(0.0d, ((TaskSnapshot) root.getSnapshot(Integer.valueOf(slot))).getFixedCost(),
			"Subtask fixed costs do not roll into the summary task's Fixed Cost field");
		assertEquals(10.0d, ((TaskSnapshot) first.getSnapshot(Integer.valueOf(slot))).getFixedCost());
		assertEquals(20.0d, ((TaskSnapshot) second.getSnapshot(Integer.valueOf(slot))).getFixedCost());
		assertEquals(30.0d, ((TaskSnapshot) third.getSnapshot(Integer.valueOf(slot))).getFixedCost());
		assertEquals(untouchedStart, untouchedSummary.getBaselineStart(slot),
			"An unselected sibling summary keeps its stored slot snapshot");
		assertEquals(otherSlotStart, first.getBaselineStart(Snapshottable.BASELINE),
			"Roll-up must leave other baseline slots unchanged");
		assertEquals(liveAssignmentsBefore, first.getAssignments().size() + second.getAssignments().size()
			+ third.getAssignments().size(), "Snapshot aggregation must not attach cloned assignments to live task/resource associations");

		change.undo();
		assertEquals(start, first.getBaselineStart(slot));
		assertEquals(untouchedStart, untouchedSummary.getBaselineStart(slot));
		assertEquals(otherSlotStart, first.getBaselineStart(Snapshottable.BASELINE));
		change.redo();
		assertEquals(replacementStart, first.getBaselineStart(slot));
		assertEquals(replacementStart, root.getBaselineStart(slot));
		assertEquals(3, ((TaskSnapshot) root.getSnapshot(Integer.valueOf(slot))).getAssignments().size(),
			"Redo snapshot copies must preserve duplicate-resource contributions");
		assertEquals(52L * hour, root.getBaselineWork(slot, 0L, Long.MAX_VALUE));

		// Reparenting changes the selected summary's live WBS frontier; the unselected
		// old parent snapshot remains stored and is not recomputed as a side effect.
		nested.setWbsChildrenNodes(List.of(NodeFactory.getInstance().createNode(first),
			NodeFactory.getInstance().createNode(second)));
		untouchedSummary.setWbsChildrenNodes(List.of());
		third.setWbsParent(nested);
		attachWbsChildren(nested, first, second, third, noBaseline);
		long oldUntouchedSummaryStart = untouchedSummary.getBaselineStart(slot);
		project.saveCurrentToSnapshot(Integer.valueOf(slot), false, List.of(nested),
			new BaselineRollupOptions(false, true));
		assertEquals(replacementStart, nested.getBaselineStart(slot));
		assertEquals(start + 54L * hour, nested.getBaselineFinish(slot));
		assertEquals(untouchedStart, untouchedSummary.getBaselineStart(slot),
			"From-subtasks updates only selected summaries after reparenting");
		assertEquals(oldUntouchedSummaryStart, untouchedSummary.getBaselineStart(slot));
	}

	@Test
	void failedBaselineRollupPreparationLeavesSelectedAndImportedStateUntouched() {
		Project project = createProject();
		NormalTask summary = project.createScriptedTask();
		NormalTask existingLeaf = project.createScriptedTask();
		NormalTask selectedLeaf = project.createScriptedTask();
		attachWbsChildren(summary, existingLeaf, selectedLeaf);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment existing = AssignmentService.getInstance().newAssignment(existingLeaf, resource, 1.0d, 0L, this);
		Assignment selected = AssignmentService.getInstance().newAssignment(selectedLeaf, resource, 1.0d, 0L, this);
		int slot = Snapshottable.BASELINE_1;
		existingLeaf.saveCurrentToSnapshot(Integer.valueOf(slot));
		summary.saveCurrentToSnapshot(Integer.valueOf(slot));
		Assignment.BaselineValues imported = new Assignment.BaselineValues(Long.valueOf(10L), Long.valueOf(20L), Long.valueOf(30L));
		selected.restoreImportedBaselineValues(slot, imported);
		TaskSnapshot existingSummarySnapshot = (TaskSnapshot) summary.getSnapshot(Integer.valueOf(slot));
		TaskSnapshot failingSnapshot = new TaskSnapshot(List.of()) {
			{
				setCurrentSchedule(existingSummarySnapshot.getCurrentSchedule());
			}
			@Override
			public Object deepCloneWithTask(Task task) {
				throw new IllegalStateException("injected baseline preparation failure");
			}
		};
		summary.setSnapshot(Integer.valueOf(slot), failingSnapshot);
		boolean selectedDirtyBefore = selectedLeaf.isDirty();
		boolean summaryDirtyBefore = summary.isDirty();
		long originalSummaryStart = summary.getBaselineStart(slot);

		assertThrows(IllegalStateException.class, () -> project.saveCurrentToSnapshot(Integer.valueOf(slot), false,
			List.of(selectedLeaf), new BaselineRollupOptions(true, false)));
		assertNull(selectedLeaf.getSnapshot(Integer.valueOf(slot)),
			"A failed preflight must not attach the selected task snapshot");
		assertEquals(originalSummaryStart, summary.getBaselineStart(slot),
			"A failed preflight must not replace an ancestor snapshot");
		assertEquals(imported, selected.getImportedBaselineValues(slot),
			"A failed preflight must preserve imported assignment-only baseline data");
		assertEquals(selectedDirtyBefore, selectedLeaf.isDirty(), "A failed preflight must preserve selected task dirty state");
		assertEquals(summaryDirtyBefore, summary.isDirty(), "A failed preflight must preserve summary dirty state");
	}

	private static void setPlan(NormalTask task, long start, long duration, double fixedCost) {
		task.getCurrentSchedule().setStart(start);
		task.getCurrentSchedule().setFinish(start + duration);
		task.getCurrentSchedule().setRawDuration(duration);
		task.setFixedCost(fixedCost);
	}

	private static void attachWbsChildren(NormalTask parent, NormalTask... children) {
		ArrayList<Node> nodes = new ArrayList<>(children.length);
		for (NormalTask child : children) {
			child.setWbsParent(parent);
			nodes.add(NodeFactory.getInstance().createNode(child));
		}
		parent.setWbsChildrenNodes(nodes);
	}

	@Test
	void workspaceUsesThisProjectsSpreadsheetFieldsWithoutLookingUpActiveWindow() {
		boolean previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		try {
			Project project = createProject();
			SpreadSheetFieldArray fields = new SpreadSheetFieldArray();
			project.setFieldArray(fields);

			Project.Workspace workspace = (Project.Workspace) project.createWorkspace(SavableToWorkspace.PERSIST);

			assertSame(fields, project.getFieldArray(), "Workspace creation must not replace this project's columns");
			assertNotNull(workspace.spreadsheetWorkspace, "The project's configured columns must be persisted");
		} finally {
			Environment.setClientSide(previousClientSide);
		}
	}

	@Test
	void settingAnUnchangedActualStartDoesNotFireDuplicateScheduleEvents() {
		Project project = createProject();
		NormalTask task = project.createScriptedTask();
		project.connectTask(task);
		int[] events = { 0 };
		ScheduleEventListener listener = event -> events[0]++;
		project.addScheduleListener(listener);

		try {
			task.setActualStart(task.getStart());
			task.setActualStart(task.getStart());
		} finally {
			project.removeScheduleListener(listener);
		}

		assertEquals(1, events[0]);
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}
}
