package com.projectlibre1.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.projectlibre1.graphic.configuration.GanttBarFormatOverrides;
import com.projectlibre1.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.pm.task.ProjectFactory;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.undo.DataFactoryUndoController;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.model.NodeModel;

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
