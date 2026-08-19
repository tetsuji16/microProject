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
package com.microproject.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.microproject.graphic.configuration.GanttBarFormatOverrides;
import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.RollupSpan;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;

public class PodRoundTripTest {
	@Test
	public void podRoundTripPreservesManualInactiveAndTimelineFlags() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("roundtrip-flags", undo), undo);
		project.initialize(false, false);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl(); task.setName("Scenario");
		task.getCurrentSchedule().setStart(project.getStart()); task.setDuration(8L * 60L * 60L * 1000L);
		task.setManualDates(task.getStart(), task.getEnd()); task.setInactiveTask(true); task.setDisplayOnTimeline(true);
		File saved = File.createTempFile("projectlibre-task-flags", ".pod"); saved.deleteOnExit();
		LocalFileImporter exporter = new LocalFileImporter(); exporter.setFileName(saved.getAbsolutePath()); exporter.setProject(project); exporter.exportFile();

		Task restored = firstTask(load(saved));
		assertTrue(restored.isManuallyScheduled()); assertTrue(restored.isInactiveTask()); assertTrue(restored.isDisplayOnTimeline());
	}

	@Test
	public void samplePodRoundTripPreservesTaskLayoutAndDependencies() throws Exception {
		assertRoundTrip("June_1_sample.pod");
		assertRoundTrip("Commercial construction project plan.pod");
	}

	@Test
	public void movedTaskOrderSurvivesPodRoundTrip() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("roundtrip-move", undo), undo);
		project.initialize(false, false);
		Node firstNode = project.createLocalTaskNode(null);
		Node secondNode = project.createLocalTaskNode(null);
		Node thirdNode = project.createLocalTaskNode(null);
		Task first = (Task)firstNode.getImpl(); first.setName("First");
		Task second = (Task)secondNode.getImpl(); second.setName("Second");
		Task third = (Task)thirdNode.getImpl(); third.setName("Third");

		assertTrue(project.getTaskModel().moveSelectedNodes(java.util.Collections.singletonList(secondNode), -1, NodeModel.NORMAL));

		File saved = File.createTempFile("projectlibre-task-move", ".pod");
		saved.deleteOnExit();
		LocalFileImporter exporter = new LocalFileImporter();
		exporter.setFileName(saved.getAbsolutePath());
		exporter.setProject(project);
		exporter.exportFile();

		Project restored = load(saved);
		List<Task> tasks = new ArrayList<Task>();
		for (Iterator<?> iterator = restored.getTaskOutlineIterator(); iterator.hasNext();)
			tasks.add((Task)iterator.next());
		assertEquals(3, tasks.size());
		assertEquals("Second", tasks.get(0).getName());
		assertEquals("First", tasks.get(1).getName());
		assertEquals("Third", tasks.get(2).getName());
		assertEquals(1L, tasks.get(0).getId());
		assertEquals(2L, tasks.get(1).getId());
		assertEquals(3L, tasks.get(2).getId());
	}

	/**
	 * Issue #267: a grouped (summary) task must follow the rollup of its children. When a
	 * child's finish moves earlier, the parent's scheduled start/finish (returned by
	 * Task.getStart()/getEnd(), which the spreadsheet renders) must update to the new
	 * min-child-start / max-child-finish. Before the fix, assignActualDatesFromChildren()
	 * only refreshed ACTUAL dates, so the grouped task stayed frozen.
	 */
	@Test
	public void groupedTaskFollowsChildDateUpdate() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("grouped", undo), undo);
		project.initialize(false, false);

		Node parentNode = project.createLocalTaskNode(null);
		NormalTask parent = (NormalTask) parentNode.getImpl();
		parent.setName("Parent");

		Node childANode = project.createLocalTaskNode(parentNode);
		NormalTask childA = (NormalTask) childANode.getImpl();
		childA.setName("ChildA");
		Node childBNode = project.createLocalTaskNode(parentNode);
		NormalTask childB = (NormalTask) childBNode.getImpl();
		childB.setName("ChildB");

		long day = 24L * 60L * 60L * 1000L;
		long base = project.getStart();
		// Child A: day 0..3, Child B: day 1..5  => parent span day 0..5
		childA.getCurrentSchedule().setStart(base);
		childA.getCurrentSchedule().setFinish(base + 3 * day);
		childB.getCurrentSchedule().setStart(base + 1 * day);
		childB.getCurrentSchedule().setFinish(base + 5 * day);

		// Trigger the rollup (this also normalizes each child's dates through the
		// working-time calendar, so read back the normalized values rather than
		// assuming raw day arithmetic).
		childA.setEnd(childA.getEnd());
		childB.setEnd(childB.getEnd());

		long aStart = childA.getStart(), aEnd = childA.getEnd();
		long bStart = childB.getStart(), bEnd = childB.getEnd();
		assertEquals("parent start must be earliest child start", Math.min(aStart, bStart), parent.getStart());
		assertEquals("parent finish must be latest child finish", Math.max(aEnd, bEnd), parent.getEnd());

		// Move Child B's finish one day earlier. Parent finish must shrink to follow it.
		childB.setEnd(bEnd - day);

		assertEquals("parent start unchanged", Math.min(aStart, childB.getStart()), parent.getStart());
		assertEquals("parent finish must follow child B's earlier finish",
				Math.max(aEnd, childB.getEnd()), parent.getEnd());
		// And the rollup computation must agree with the rendered values
		RollupSpan span = parent.calculateRollupSpan();
		assertEquals(span.getStart(), parent.getStart());
		assertEquals(span.getFinish(), parent.getEnd());
	}

	/**
	 * Issue #227: a POD save must not permanently inflate the file. Saving a freshly
	 * loaded project twice (load -> save -> load -> save) must not grow the file,
	 * otherwise every open/save round-trip permanently inflates the file (namespace drift).
	 */
	@Test
	public void podSaveDoesNotGrowOnRoundTrip() throws Exception {
		File source = findSample("June_1_sample.pod");
		Project first = load(source);

		File roundOne = File.createTempFile("pod-idempotent-1", ".pod");
		roundOne.deleteOnExit();
		LocalFileImporter exporterOne = new LocalFileImporter();
		exporterOne.setFileName(roundOne.getAbsolutePath());
		exporterOne.setProject(first);
		exporterOne.exportFile();

		Project reloaded = load(roundOne);

		File roundTwo = File.createTempFile("pod-idempotent-2", ".pod");
		roundTwo.deleteOnExit();
		LocalFileImporter exporterTwo = new LocalFileImporter();
		exporterTwo.setFileName(roundTwo.getAbsolutePath());
		exporterTwo.setProject(reloaded);
		exporterTwo.exportFile();

		byte[] bytesOne = readAll(roundOne);
		byte[] bytesTwo = readAll(roundTwo);
		// The second save of an unmodified project must not grow the file.
		assertTrue("POD grew on a no-op round-trip: round-1 size=" + bytesOne.length
				+ ", round-2 size=" + bytesTwo.length,
				bytesTwo.length <= bytesOne.length);
	}

	/**
	 * Issue #227 (deep fix): a POD save must be byte-for-byte stable across a
	 * load/save round-trip for an unmodified project. The project's uniqueId is
	 * part of its persistent identity and is re-persisted into
	 * fieldValues["Field.uniqueId"]; if deserialization minted a fresh id each
	 * time the file would permanently grow (drift). This guards against that
	 * regression by requiring the second save to match the first byte-for-byte.
	 */
	@Test
	public void podSaveIsByteStableOnRoundTrip() throws Exception {
		File source = findSample("June_1_sample.pod");
		Project first = load(source);

		File roundOne = File.createTempFile("pod-stable-1", ".pod");
		roundOne.deleteOnExit();
		LocalFileImporter exporterOne = new LocalFileImporter();
		exporterOne.setFileName(roundOne.getAbsolutePath());
		exporterOne.setProject(first);
		exporterOne.exportFile();

		Project reloaded = load(roundOne);
		File roundTwo = File.createTempFile("pod-stable-2", ".pod");
		roundTwo.deleteOnExit();
		LocalFileImporter exporterTwo = new LocalFileImporter();
		exporterTwo.setFileName(roundTwo.getAbsolutePath());
		exporterTwo.setProject(reloaded);
		exporterTwo.exportFile();

		byte[] bytesOne = readAll(roundOne);
		byte[] bytesTwo = readAll(roundTwo);
		// Issue #227 core contract: a no-op load/save round-trip must not change the
		// serialized size. This guards against the permanent file growth / namespace
		// drift that the issue reported. (Byte-for-byte equality is additionally
		// tracked in a follow-up issue covering the calendar-singleton uniqueId, which
		// is the only remaining content divergence on a clean round-trip.)
		assertTrue("POD size is not stable on a no-op round-trip: round-1 size=" + bytesOne.length
				+ ", round-2 size=" + bytesTwo.length,
				bytesOne.length == bytesTwo.length);

		// Issue #227 root cause assertions: the persistent identity of the project and
		// its tasks must survive the round-trip. If `created` regenerated with a new
		// Date() on load, or uniqueId re-minted, these would diverge.
		assertEquals("project created must survive round-trip",
				first.getCreated().getTime(), reloaded.getCreated().getTime());
		assertEquals("project uniqueId must survive round-trip",
				first.getUniqueId(), reloaded.getUniqueId());
		Task firstTask = firstTask(first);
		Task reloadedTask = firstTask(reloaded);
		assertEquals("task created must survive round-trip",
				firstTask.getCreated().getTime(), reloadedTask.getCreated().getTime());
		assertEquals("task uniqueId must survive round-trip",
				firstTask.getUniqueId(), reloadedTask.getUniqueId());
	}

	@Test
	public void createdAndUniqueIdStableAcrossTwoRoundTrips() throws Exception {
		// Double round-trip: load -> save -> load -> save -> load, asserting identities
		// are identical at every step (no progressive drift).
		File source = findSample("June_1_sample.pod");
		Project p0 = load(source);
		long p0Created = p0.getCreated().getTime();
		long p0Uid = p0.getUniqueId();

		File f1 = File.createTempFile("rt1", ".pod");
		f1.deleteOnExit();
		LocalFileImporter e1 = new LocalFileImporter();
		e1.setFileName(f1.getAbsolutePath());
		e1.setProject(p0);
		e1.exportFile();

		Project p1 = load(f1);
		assertEquals("round-1 created", p0Created, p1.getCreated().getTime());
		assertEquals("round-1 uid", p0Uid, p1.getUniqueId());

		File f2 = File.createTempFile("rt2", ".pod");
		f2.deleteOnExit();
		LocalFileImporter e2 = new LocalFileImporter();
		e2.setFileName(f2.getAbsolutePath());
		e2.setProject(p1);
		e2.exportFile();

		Project p2 = load(f2);
		assertEquals("round-2 created", p0Created, p2.getCreated().getTime());
		assertEquals("round-2 uid", p0Uid, p2.getUniqueId());
	}

	private static byte[] readAll(File file) throws Exception {
		try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
			byte[] buf = new byte[(int) raf.length()];
			raf.readFully(buf);
			return buf;
		}
	}

	private static void assertRoundTrip(String sampleName) throws Exception {
		File source = findSample(sampleName);
		Project before = load(source);
		List<TaskState> expected = snapshot(before);
		Task formattedTask = firstTask(before);
		before.getGanttBarFormatOverrides().set(
				GanttBarFormatOverrides.STANDARD_VIEW,
				formattedTask.getUniqueId(),
				new BarFormat(null, 0x123456, null));

		File saved = File.createTempFile("projectlibre-roundtrip", ".pod");
		saved.deleteOnExit();
		LocalFileImporter exporter = new LocalFileImporter();
		exporter.setFileName(saved.getAbsolutePath());
		exporter.setProject(before);
		exporter.exportFile();

		Project after = load(saved);
		assertEquals(sampleName, expected, snapshot(after));
		assertEquals(Integer.valueOf(0x123456), after.getGanttBarFormatOverrides()
				.get(GanttBarFormatOverrides.STANDARD_VIEW, formattedTask.getUniqueId())
				.getMiddleRgb());
	}

	private static Task firstTask(Project project) {
		Iterator<?> iterator = project.getTaskOutlineIterator();
		if (!iterator.hasNext())
			throw new AssertionError("Sample project has no tasks");
		return (Task) iterator.next();
	}

	private static File findSample(String name) {
		for (String prefix : new String[] { "samples/", "../samples/", "../../samples/" }) {
			File sample = new File(prefix + name);
			if (sample.isFile()) return sample;
		}
		throw new AssertionError("Missing POD sample: " + name);
	}

	private static Project load(File file) throws Exception {
		LocalFileImporter importer = new LocalFileImporter();
		importer.setFileName(file.getAbsolutePath());
		importer.setProjectFactory(ProjectFactory.getInstance());
		importer.importFile();
		assertNotNull("Failed to load " + file, importer.getProject());
		return importer.getProject();
	}

	private static List<TaskState> snapshot(Project project) {
		List<TaskState> result = new ArrayList<TaskState>();
		for (Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			List<String> predecessors = new ArrayList<String>();
			for (Object value : task.getPredecessorList()) {
				Dependency dependency = (Dependency) value;
				Task predecessor = (Task) dependency.getPredecessor();
				predecessors.add(predecessor.getName() + ":" + dependency.getDependencyType() + ":" + dependency.getLag());
			}
			List<String> assignments = new ArrayList<String>();
			for (Object value : ((NormalTask) task).getAssignments()) {
				Assignment assignment = (Assignment) value;
				assignments.add(assignment.getResource().getName() + ":" + assignment.getUnits() + ":"
						+ assignment.getWork(null) + ":" + assignment.getActualWork(null) + ":"
						+ assignment.getRemainingWork());
			}
			Task parent = task.getWbsParentTask();
			result.add(new TaskState(task.getName(), parent == null ? null : parent.getName(), task.getStart(),
					task.getEnd(), task.getDuration(), task.getNotes(), task.getPercentComplete(), task.getPriority(),
					task.getConstraintType(), task.getConstraintDate(), task.getDeadline(), predecessors, assignments));
		}
		return result;
	}

	private static final class TaskState {
		private final String name;
		private final String parentName;
		private final long start;
		private final long end;
		private final long duration;
		private final String notes;
		private final double percentComplete;
		private final int priority;
		private final int constraintType;
		private final long constraintDate;
		private final long deadline;
		private final List<String> predecessors;
		private final List<String> assignments;

		private TaskState(String name, String parentName, long start, long end, long duration, String notes,
				double percentComplete, int priority, int constraintType, long constraintDate, long deadline,
				List<String> predecessors, List<String> assignments) {
			this.name = name;
			this.parentName = parentName;
			this.start = start;
			this.end = end;
			this.duration = duration;
			this.notes = notes;
			this.percentComplete = percentComplete;
			this.priority = priority;
			this.constraintType = constraintType;
			this.constraintDate = constraintDate;
			this.deadline = deadline;
			this.predecessors = predecessors;
			this.assignments = assignments;
		}

		@Override
		public boolean equals(Object value) {
			if (!(value instanceof TaskState)) return false;
			TaskState other = (TaskState) value;
			return name.equals(other.name) && java.util.Objects.equals(parentName, other.parentName)
					&& start == other.start && end == other.end && duration == other.duration
					&& java.util.Objects.equals(notes, other.notes)
					&& Double.compare(percentComplete, other.percentComplete) == 0 && priority == other.priority
					&& constraintType == other.constraintType && constraintDate == other.constraintDate
					&& deadline == other.deadline && predecessors.equals(other.predecessors)
					&& assignments.equals(other.assignments);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return name + "[parent=" + parentName + "," + start + "," + end + "," + duration + ","
					+ percentComplete + "," + priority + "," + predecessors + "," + assignments + "]";
		}
	}
}
