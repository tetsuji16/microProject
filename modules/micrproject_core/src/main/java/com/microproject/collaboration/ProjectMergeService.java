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
package com.microproject.collaboration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.OutlineCollection;
import com.microproject.exchange.FileImporter;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourcePoolFactory;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.session.LocalSession;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

public class ProjectMergeService {
	private static final String PROJECT_LIBRE_FILE_SEPARATOR = "@@@@@@@@@@ProjectLibreSeparator_MSXML@@@@@@@@@@";

	public static class TaskState {
		private final long taskId;
		private final long id;
		private final String name;
		private final long start;
		private final long finish;
		private final long duration;
		private final long parentId;
		private final int outlineLevel;
		private final String predecessors;
		private final String resourceAssignments;
		private final double percentComplete;
		private final String notes;

		private TaskState(long taskId, long id, String name, long start, long finish, long duration, long parentId, int outlineLevel,
				String predecessors, String resourceAssignments, double percentComplete, String notes) {
			this.taskId = taskId;
			this.id = id;
			this.name = name;
			this.start = start;
			this.finish = finish;
			this.duration = duration;
			this.parentId = parentId;
			this.outlineLevel = outlineLevel;
			this.predecessors = predecessors;
			this.resourceAssignments = resourceAssignments;
			this.percentComplete = percentComplete;
			this.notes = notes;
		}

		public static TaskState capture(Task task) {
			if (task == null) {
				return null;
			}
			String assignments = "";
			if (task instanceof NormalTask) {
				assignments = ((NormalTask) task).getResourceNames();
			}
			return new TaskState(task.getUniqueId(), task.getId(), task.getName(), task.getStart(), task.getEnd(), task.getDuration(),
				task.getParentId(OutlineCollection.DEFAULT_OUTLINE), task.getOutlineLevel(), task.getUniqueIdPredecessors(),
				assignments, task.getPercentComplete(), task.getNotes());
		}

		public boolean matches(TaskState other) {
			if (other == null) {
				return false;
			}
			return id == other.id
				&& start == other.start
				&& finish == other.finish
				&& duration == other.duration
				&& parentId == other.parentId
				&& outlineLevel == other.outlineLevel
				&& Double.compare(percentComplete, other.percentComplete) == 0
				&& Objects.equals(name, other.name)
				&& Objects.equals(predecessors, other.predecessors)
				&& Objects.equals(resourceAssignments, other.resourceAssignments)
				&& Objects.equals(notes, other.notes);
		}
	}

	public static class ConflictResult {
		private final Set<Long> deletedTaskIds = new LinkedHashSet<Long>();
		private final Set<Long> changedTaskIds = new LinkedHashSet<Long>();

		public boolean hasConflicts() {
			return !deletedTaskIds.isEmpty() || !changedTaskIds.isEmpty();
		}

		public Set<Long> getDeletedTaskIds() {
			return deletedTaskIds;
		}

		public Set<Long> getChangedTaskIds() {
			return changedTaskIds;
		}
	}

	public static class ApplyResult {
		private int updatedTaskCount;
		private final Set<Long> skippedLockedTaskIds = new LinkedHashSet<Long>();

		public int getUpdatedTaskCount() {
			return updatedTaskCount;
		}

		public Set<Long> getSkippedLockedTaskIds() {
			return skippedLockedTaskIds;
		}

		public boolean hasChanges() {
			return updatedTaskCount > 0 || !skippedLockedTaskIds.isEmpty();
		}
	}

	public Project loadExternalProject(String fileName) {
		if (fileName == null) {
			return null;
		}
		try {
			if (fileName.toLowerCase(Locale.ROOT).endsWith(".pod")) {
				return loadPodProject(fileName);
			}
			if (fileName.toLowerCase(Locale.ROOT).endsWith(".mpo")) {
				return loadMpoProject(fileName);
			}
			try (InputStream in = new FileInputStream(fileName)) {
				return loadMicrosoftProject(fileName, in);
			}
		} catch (Exception e) {
			return null;
		}
	}

	private Project loadMpoProject(String fileName) throws Exception {
		FileImporter importer = LocalSession.getImporter(LocalSession.MPO_PROJECT_IMPORTER);
		importer.setFileName(fileName);
		importer.setProjectFactory(ProjectFactory.getInstance());
		try (InputStream in = new FileInputStream(fileName)) {
			return importer.loadProject(in);
		}
	}

	private Project loadPodProject(String fileName) throws Exception {
		InputStream embeddedXml = openEmbeddedPodXml(fileName);
		if (embeddedXml != null) {
			try {
				Project project = loadMicrosoftProject(fileName, embeddedXml);
				if (project != null) {
					return project;
				}
			} catch (Exception e) {
				// Fall through to the serialized POD reader for files without usable XML.
			}
		}
		return loadSerializedPodProject(fileName);
	}

	private Project loadSerializedPodProject(String fileName) throws Exception {
		com.microproject.exchange.LocalFileImporter importer = new com.microproject.exchange.LocalFileImporter();
		importer.setFileName(fileName);
		importer.setProjectFactory(ProjectFactory.getInstance());
		importer.importFile();
		return importer.getProject();
	}

	private Project loadMicrosoftProject(String fileName, InputStream in) throws Exception {
		FileImporter importer = LocalSession.getImporter(LocalSession.MICROSOFT_PROJECT_IMPORTER);
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePoolFactory.getInstance().createResourcePool("", undoController);
		resourcePool.setLocal(true);
		Project project = Project.createProject(resourcePool, undoController);
		importer.setFileName(fileName);
		importer.setProject(project);
		importer.setProjectFactory(ProjectFactory.getInstance());
		return importer.loadProject(in);
	}

	private InputStream openEmbeddedPodXml(String fileName) throws Exception {
		byte[] bytes = Files.readAllBytes(new File(fileName).toPath());
		byte[] separator = PROJECT_LIBRE_FILE_SEPARATOR.getBytes(StandardCharsets.UTF_8);
		int start = indexOf(bytes, separator);
		if (start < 0) {
			return null;
		}
		start += separator.length;
		if (start < 0 || start >= bytes.length) {
			return null;
		}
		return new ByteArrayInputStream(bytes, start, bytes.length - start);
	}

	private int indexOf(byte[] bytes, byte[] pattern) {
		if (pattern.length == 0 || bytes.length < pattern.length) {
			return -1;
		}
		for (int i = 0; i <= bytes.length - pattern.length; i++) {
			boolean found = true;
			for (int j = 0; j < pattern.length; j++) {
				if (bytes[i + j] != pattern[j]) {
					found = false;
					break;
				}
			}
			if (found) {
				return i;
			}
		}
		return -1;
	}

	public Map<Long, TaskState> captureTaskStates(Iterable<Task> tasks) {
		Map<Long, TaskState> states = new LinkedHashMap<Long, TaskState>();
		if (tasks == null) {
			return states;
		}
		for (Task task : tasks) {
			TaskState state = TaskState.capture(task);
			if (state != null) {
				states.put(Long.valueOf(task.getUniqueId()), state);
			}
		}
		return states;
	}

	public Set<Long> findDeletedTasks(String fileName, Set<Long> lockedTaskIds) {
		Set<Long> deleted = new LinkedHashSet<Long>();
		if (lockedTaskIds == null || lockedTaskIds.isEmpty()) {
			return deleted;
		}
		Project external = loadExternalProject(fileName);
		if (external == null) {
			return deleted;
		}
		for (Long taskId : lockedTaskIds) {
			Task task = external.findByUniqueId(taskId.longValue());
			if (task == null) {
				deleted.add(taskId);
			}
		}
		return deleted;
	}

	public ConflictResult findTaskConflicts(String fileName, Map<Long, TaskState> baselineStates) {
		ConflictResult result = new ConflictResult();
		if (baselineStates == null || baselineStates.isEmpty()) {
			return result;
		}
		Project external = loadExternalProject(fileName);
		if (external == null) {
			return result;
		}
		for (Map.Entry<Long, TaskState> entry : baselineStates.entrySet()) {
			Long taskId = entry.getKey();
			TaskState baseline = entry.getValue();
			Task externalTask = findMatchingTask(external, taskId.longValue(), baseline);
			if (externalTask == null) {
				result.getDeletedTaskIds().add(taskId);
				continue;
			}
			TaskState externalState = TaskState.capture(externalTask);
			if (!baseline.matches(externalState)) {
				result.getChangedTaskIds().add(taskId);
			}
		}
		return result;
	}

	private Task findMatchingTask(Project external, long uniqueId, TaskState baseline) {
		if (external == null) {
			return null;
		}
		Task task = external.findByUniqueId(uniqueId);
		if (task != null) {
			return task;
		}
		return findTaskByIdFallback(external, baseline);
	}

	public ApplyResult applyExternalTaskUpdates(Project target, String fileName, Set<Long> lockedTaskIds) {
		ApplyResult result = new ApplyResult();
		if (target == null || fileName == null) {
			return result;
		}
		Project external = loadExternalProject(fileName);
		if (external == null) {
			return result;
		}
		boolean wasDirty = target.needsSaving();
		boolean wasImporting = Environment.isImporting();
		List<Node> changedNodes = new ArrayList<Node>(external.getTaskList().size());
		try {
			Environment.setImporting(true);
			applyExternalTaskUpdates(target, external, lockedTaskIds, result, changedNodes);
		} finally {
			Environment.setImporting(wasImporting);
			if (!wasDirty) {
				target.setGroupDirty(false);
			}
		}
		fireTaskUpdates(target, changedNodes);
		return result;
	}

	private void applyExternalTaskUpdates(Project target, Project external, Set<Long> lockedTaskIds, ApplyResult result, List<Node> changedNodes) {
		for (Iterator i = external.getTaskOutlineIterator(); i.hasNext();) {
			Task externalTask = (Task) i.next();
			TaskState incoming = TaskState.capture(externalTask);
			if (incoming == null) {
				continue;
			}
			Task localTask = findMatchingTask(target, incoming.taskId, incoming);
			if (localTask == null) {
				continue;
			}
			if (isLocked(localTask, lockedTaskIds)) {
				result.skippedLockedTaskIds.add(Long.valueOf(localTask.getUniqueId()));
				continue;
			}
			Node changedNode = applyExternalTaskValues(target, localTask, externalTask, incoming);
			if (changedNode != null) {
				result.updatedTaskCount++;
				changedNodes.add(changedNode);
			}
		}
	}

	private Node applyExternalTaskValues(Project target, Task localTask, Task externalTask, TaskState incoming) {
		TaskState before = TaskState.capture(localTask);
		if (before == null || before.matches(incoming)) {
			return null;
		}
		applyTaskValues(localTask, externalTask);
		TaskState after = TaskState.capture(localTask);
		if (before.matches(after)) {
			return null;
		}
		return findTaskNode(target, localTask);
	}

	private boolean isLocked(Task task, Set<Long> lockedTaskIds) {
		return task != null && lockedTaskIds != null && lockedTaskIds.contains(Long.valueOf(task.getUniqueId()));
	}

	private Task findTaskByIdFallback(Project project, TaskState state) {
		if (project == null || state == null) {
			return null;
		}
		return Project.findTaskById(Long.valueOf(state.id), project.getTaskList());
	}

	private Node findTaskNode(Project target, Task task) {
		return target == null || target.getTaskOutline() == null ? null : target.getTaskOutline().search(task);
	}

	private void applyTaskValues(Task localTask, Task externalTask) {
		localTask.setName(externalTask.getName());
		localTask.setNotes(externalTask.getNotes());
		if (localTask instanceof NormalTask && externalTask instanceof NormalTask) {
			NormalTask localNormal = (NormalTask) localTask;
			NormalTask externalNormal = (NormalTask) externalTask;
			localNormal.setDuration(externalNormal.getDuration());
			localNormal.setEnd(externalNormal.getEnd());
			localNormal.setPercentComplete(externalNormal.getPercentComplete());
		}
		try {
			localTask.setUniqueIdPredecessors(externalTask.getUniqueIdPredecessors());
		} catch (FieldParseException e) {
			// Keep the refresh best-effort; malformed external links will still be caught on save.
		}
		localTask.markTaskAsNeedingRecalculation();
	}

	private void fireTaskUpdates(Project target, List<Node> changedNodes) {
		if (changedNodes == null || changedNodes.isEmpty()) {
			return;
		}
		NodeModel model = target.getTaskOutline();
		model.getHierarchy().fireUpdate((Node[]) changedNodes.toArray(new Node[changedNodes.size()]));
		target.fireScheduleChanged(this, null);
	}
}
