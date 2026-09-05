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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.AccessDeniedException;
import java.util.zip.ZipException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger logger = Logger.getLogger(ProjectMergeService.class.getName());

	public enum LoadStatus {
		SUCCESS,
		NOT_FOUND,
		ACCESS_DENIED,
		TRANSIENT_FAILURE,
		INVALID_FILE
	}

	public static class ExternalProjectLoadResult {
		private final LoadStatus status;
		private final Project project;
		private final Exception cause;

		private ExternalProjectLoadResult(LoadStatus status, Project project, Exception cause) {
			this.status = status;
			this.project = project;
			this.cause = cause;
		}

		public LoadStatus getStatus() { return status; }
		public Project getProject() { return project; }
		public Exception getCause() { return cause; }
		public boolean isSuccess() { return status == LoadStatus.SUCCESS && project != null; }
	}

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
		private LoadStatus loadStatus = LoadStatus.SUCCESS;
		private Exception loadFailure;

		public boolean hasConflicts() {
			return !deletedTaskIds.isEmpty() || !changedTaskIds.isEmpty();
		}

		public Set<Long> getDeletedTaskIds() {
			return deletedTaskIds;
		}

		public Set<Long> getChangedTaskIds() {
			return changedTaskIds;
		}

		public LoadStatus getLoadStatus() { return loadStatus; }
		public Exception getLoadFailure() { return loadFailure; }
		public boolean hasLoadFailure() { return loadStatus != LoadStatus.SUCCESS; }
	}

	public static class ApplyResult {
		private int updatedTaskCount;
		private final Set<Long> skippedLockedTaskIds = new LinkedHashSet<Long>();
		private LoadStatus loadStatus = LoadStatus.SUCCESS;
		private Exception loadFailure;

		public int getUpdatedTaskCount() {
			return updatedTaskCount;
		}

		public Set<Long> getSkippedLockedTaskIds() {
			return skippedLockedTaskIds;
		}

		public boolean hasChanges() {
			return updatedTaskCount > 0 || !skippedLockedTaskIds.isEmpty();
		}

		public LoadStatus getLoadStatus() { return loadStatus; }
		public Exception getLoadFailure() { return loadFailure; }
		public boolean hasLoadFailure() { return loadStatus != LoadStatus.SUCCESS; }
	}

	/** Result of checking for deleted locked tasks without turning a load failure into an empty set. */
	public static class DeletedTasksResult {
		private final Set<Long> deletedTaskIds = new LinkedHashSet<Long>();
		private LoadStatus loadStatus = LoadStatus.SUCCESS;
		private Exception loadFailure;

		public Set<Long> getDeletedTaskIds() { return deletedTaskIds; }
		public LoadStatus getLoadStatus() { return loadStatus; }
		public Exception getLoadFailure() { return loadFailure; }
		public boolean hasLoadFailure() { return loadStatus != LoadStatus.SUCCESS; }
	}

	/** Creates a load failure result for callers that need to preserve a recoverable UI state. */
	public static ApplyResult failedLoad(LoadStatus status, Exception cause) {
		ApplyResult result = new ApplyResult();
		result.loadStatus = status == null ? LoadStatus.INVALID_FILE : status;
		result.loadFailure = cause;
		return result;
	}

	/**
	 * Loads an external project while preserving the historical Project-returning
	 * API used by exchange and collaboration callers.  Detailed failure
	 * information is available from {@link #loadExternalProjectResult(String)}.
	 */
	public Project loadExternalProject(String fileName) {
		ExternalProjectLoadResult result = loadExternalProjectResult(fileName);
		return result.isSuccess() ? result.getProject() : null;
	}

	/** Loads an external project and reports a stable status for UI/recovery code. */
	public ExternalProjectLoadResult loadExternalProjectResult(String fileName) {
		if (fileName == null) {
			return new ExternalProjectLoadResult(LoadStatus.NOT_FOUND, null, null);
		}
		try {
			File file = new File(fileName);
			if (!file.isFile()) {
				return new ExternalProjectLoadResult(LoadStatus.NOT_FOUND, null, null);
			}
			Project project;
			if (fileName.toLowerCase(Locale.ROOT).endsWith(".pod")) {
				project = loadPodProject(fileName);
			} else if (fileName.toLowerCase(Locale.ROOT).endsWith(".mpo")) {
				project = loadMpoProject(fileName);
			} else {
				try (InputStream in = new FileInputStream(fileName)) {
					project = loadMicrosoftProject(fileName, in);
				}
			}
			if (project == null) {
				IllegalStateException cause = new IllegalStateException("Importer returned no project");
				logger.log(Level.WARNING, "Invalid external project " + fileName, cause);
				return new ExternalProjectLoadResult(LoadStatus.INVALID_FILE, null, cause);
			}
			return new ExternalProjectLoadResult(LoadStatus.SUCCESS, project, null);
		} catch (java.io.IOException e) {
			LoadStatus status = loadFailureStatus(e);
			logger.log(Level.WARNING, "Unable to load external project " + fileName + " (" + status + ")", e);
			return new ExternalProjectLoadResult(status, null, e);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "Unable to access external project " + fileName, e);
			return new ExternalProjectLoadResult(LoadStatus.ACCESS_DENIED, null, e);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Invalid external project " + fileName, e);
			return new ExternalProjectLoadResult(LoadStatus.INVALID_FILE, null, e);
		}
	}

	/** Maps filesystem access failures separately from malformed project content. */
	static LoadStatus loadFailureStatus(Exception failure) {
		if (failure instanceof SecurityException || failure instanceof AccessDeniedException)
			return LoadStatus.ACCESS_DENIED;
		if (failure instanceof NoSuchFileException || failure instanceof FileNotFoundException)
			return LoadStatus.NOT_FOUND;
		if (failure instanceof ZipException)
			return LoadStatus.INVALID_FILE;
		return failure instanceof java.io.IOException ? LoadStatus.TRANSIENT_FAILURE : LoadStatus.INVALID_FILE;
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
		try (InputStream embeddedXml = openEmbeddedPodXml(fileName)) {
			if (embeddedXml != null) {
				Project project = loadMicrosoftProject(fileName, embeddedXml);
				if (project != null) {
					return project;
				}
			}
		} catch (Exception e) {
			// Fall through to the serialized POD reader for files without usable XML.
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

	InputStream openEmbeddedPodXml(String fileName) throws Exception {
		long fileSize = new File(fileName).length();
		long startedAt = System.nanoTime();
		EmbeddedPodXmlInputStream stream = new EmbeddedPodXmlInputStream(
			new BufferedInputStream(new FileInputStream(fileName), EmbeddedPodXmlInputStream.BUFFER_SIZE),
			PROJECT_LIBRE_FILE_SEPARATOR.getBytes(StandardCharsets.UTF_8));
		if (!stream.locateSeparator()) {
			stream.close();
			logger.log(Level.FINE, "POD XML separator not found: {0} bytes scanned in {1} ms",
				new Object[] { Long.valueOf(stream.getBytesScanned()), Long.valueOf(elapsedMillis(startedAt)) });
			return null;
		}
		logger.log(Level.FINE, "POD XML separator found: {0} byte file scanned in {1} ms",
			new Object[] { Long.valueOf(fileSize), Long.valueOf(elapsedMillis(startedAt)) });
		return stream;
	}

	private static long elapsedMillis(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000L;
	}

	/** Streams the bytes after the embedded XML separator without copying the POD. */
	private static final class EmbeddedPodXmlInputStream extends InputStream {
		private static final int BUFFER_SIZE = 64 * 1024;
		private final InputStream source;
		private final byte[] separator;
		private final int[] failureTable;
		private final byte[] buffer = new byte[BUFFER_SIZE];
		private int bufferPosition;
		private int bufferLimit;
		private int separatorPosition;
		private long bytesScanned;
		private boolean located;

		private EmbeddedPodXmlInputStream(InputStream source, byte[] separator) {
			this.source = source;
			this.separator = separator;
			this.failureTable = buildFailureTable(separator);
		}

		private boolean locateSeparator() throws java.io.IOException {
			if (located) {
				return true;
			}
			while (true) {
				int value = readSourceByte();
				if (value < 0) {
					return false;
				}
				bytesScanned++;
				while (separatorPosition > 0 && (byte) value != separator[separatorPosition]) {
					separatorPosition = failureTable[separatorPosition - 1];
				}
				if ((byte) value == separator[separatorPosition]) {
					separatorPosition++;
					if (separatorPosition == separator.length) {
						located = true;
						return true;
					}
				}
			}
		}

		private int readSourceByte() throws java.io.IOException {
			if (bufferPosition >= bufferLimit) {
				bufferLimit = source.read(buffer);
				bufferPosition = 0;
				if (bufferLimit < 0) {
					return -1;
				}
			}
			return buffer[bufferPosition++] & 0xff;
		}

		private long getBytesScanned() {
			return bytesScanned;
		}

		@Override
		public int read() throws java.io.IOException {
			if (!located && !locateSeparator()) {
				return -1;
			}
			return readSourceByte();
		}

		@Override
		public int read(byte[] target, int offset, int length) throws java.io.IOException {
			if (!located && !locateSeparator()) {
				return -1;
			}
			if (length == 0) {
				return 0;
			}
			int count = 0;
			while (count < length) {
				int value = readSourceByte();
				if (value < 0) {
					return count == 0 ? -1 : count;
				}
				target[offset + count++] = (byte) value;
			}
			return count;
		}

		@Override
		public void close() throws java.io.IOException {
			source.close();
		}

		private static int[] buildFailureTable(byte[] pattern) {
			int[] table = new int[pattern.length];
			for (int i = 1, prefix = 0; i < pattern.length;) {
				if (pattern[i] == pattern[prefix]) {
					table[i++] = ++prefix;
				} else if (prefix > 0) {
					prefix = table[prefix - 1];
				} else {
					i++;
				}
			}
			return table;
		}
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
		return findDeletedTasksResult(fileName, lockedTaskIds).getDeletedTaskIds();
	}

	public DeletedTasksResult findDeletedTasksResult(String fileName, Set<Long> lockedTaskIds) {
		DeletedTasksResult result = new DeletedTasksResult();
		if (lockedTaskIds == null || lockedTaskIds.isEmpty()) {
			return result;
		}
		ExternalProjectLoadResult load = loadExternalProjectResult(fileName);
		result.loadStatus = load.getStatus();
		result.loadFailure = load.getCause();
		if (!load.isSuccess()) {
			return result;
		}
		Project external = load.getProject();
		for (Long taskId : lockedTaskIds) {
			Task task = external.findByUniqueId(taskId.longValue());
			if (task == null) {
				result.deletedTaskIds.add(taskId);
			}
		}
		return result;
	}

	public ConflictResult findTaskConflicts(String fileName, Map<Long, TaskState> baselineStates) {
		ConflictResult result = new ConflictResult();
		if (baselineStates == null || baselineStates.isEmpty()) {
			return result;
		}
		ExternalProjectLoadResult load = loadExternalProjectResult(fileName);
		result.loadStatus = load.getStatus();
		result.loadFailure = load.getCause();
		if (!load.isSuccess()) {
			return result;
		}
		Project external = load.getProject();
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
		ExternalProjectLoadResult load = loadExternalProjectResult(fileName);
		result.loadStatus = load.getStatus();
		result.loadFailure = load.getCause();
		if (!load.isSuccess()) {
			return result;
		}
		Project external = load.getProject();
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
