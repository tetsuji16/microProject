/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.application.task;

import java.util.List;

import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.pm.task.ProjectTaskKey;

/** Immutable input vocabulary for task use cases. */
public final class TaskCommands {
	private TaskCommands() { }

	public record TaskDeleteCommand(List<ProjectTaskKey> tasks, long expectedDomainRevision) {
		public TaskDeleteCommand { tasks = List.copyOf(tasks); }
	}

	public record TaskDependencyBatchCommand(Operation operation, List<ProjectTaskKey> tasks, long expectedDomainRevision) {
		public enum Operation { LINK, UNLINK }
		public TaskDependencyBatchCommand {
			if (operation == null || tasks == null) throw new IllegalArgumentException("operation and tasks are required");
			tasks = List.copyOf(tasks);
			revision(expectedDomainRevision);
		}
	}

	public record TaskDependencyCommand(ProjectTaskKey predecessor, ProjectTaskKey successor,
			int dependencyType, long lag, long expectedDomainRevision) {
		public TaskDependencyCommand { endpoints(predecessor, successor); revision(expectedDomainRevision); }
	}

	public record TaskDependencyDeleteCommand(ProjectTaskKey predecessor, ProjectTaskKey successor,
			long expectedLag, int expectedType, long expectedDomainRevision) {
		public TaskDependencyDeleteCommand { endpoints(predecessor, successor); revision(expectedDomainRevision); }
	}

	public record TaskDependencyUpdateCommand(ProjectTaskKey predecessor, ProjectTaskKey successor,
			long expectedLag, int expectedType, long proposedLag, int proposedType, long expectedDomainRevision) {
		public TaskDependencyUpdateCommand { endpoints(predecessor, successor); revision(expectedDomainRevision); }
	}

	public record TaskFieldBatchEditCommand(List<TaskFieldEditCommand> edits, long expectedDomainRevision) {
		public TaskFieldBatchEditCommand { edits = edits == null ? List.of() : List.copyOf(edits); revision(expectedDomainRevision); }
	}

	public record TaskFieldEditCommand(ProjectTaskKey taskKey, String fieldId, Object expectedValue,
			Object proposedValue, FieldContext fieldContext) {
		public record Text(String value) { }
		public TaskFieldEditCommand {
			if (taskKey == null || fieldId == null || fieldId.isBlank())
				throw new IllegalArgumentException("taskKey and fieldId are required");
		}
	}

	public record TaskHierarchyIndentCommand(List<ProjectTaskKey> tasks, int deltaLevel, long expectedDomainRevision) {
		public TaskHierarchyIndentCommand {
			tasks = requiredTasks(tasks);
			if (deltaLevel != -1 && deltaLevel != 1) throw new IllegalArgumentException("deltaLevel must be -1 or 1");
			revision(expectedDomainRevision);
		}
	}

	public record TaskHierarchyMoveCommand(List<ProjectTaskKey> tasks, int direction, long expectedDomainRevision) {
		public TaskHierarchyMoveCommand {
			tasks = requiredTasks(tasks);
			if (direction != -1 && direction != 1) throw new IllegalArgumentException("direction must be -1 or 1");
			revision(expectedDomainRevision);
		}
	}

	public record TaskHierarchyRelocateCommand(List<ProjectTaskKey> tasks, ProjectTaskKey parent, int position,
			long expectedDomainRevision) {
		public TaskHierarchyRelocateCommand { tasks = requiredTasks(tasks); TaskCommands.position(position); revision(expectedDomainRevision); }
	}

	/** Detached core nodes are a compatibility payload; no UI/cache handle crosses this boundary. */
	public record TaskPasteCommand(ProjectTaskKey parent, int position, List<Node> detachedNodes, long expectedDomainRevision) {
		public TaskPasteCommand {
			detachedNodes = detachedNodes == null ? List.of() : List.copyOf(detachedNodes);
			if (detachedNodes.isEmpty()) throw new IllegalArgumentException("detachedNodes are required");
			TaskCommands.position(position);
			revision(expectedDomainRevision);
		}
	}

	public record TaskProgressCommand(ProjectTaskKey task, long expectedCompleted, long proposedCompleted,
			long expectedDomainRevision) {
		public TaskProgressCommand { TaskCommands.task(task); revision(expectedDomainRevision); }
	}

	public record TaskScheduleDragCommand(ProjectTaskKey task, int intervalIndex, long expectedStart, long expectedEnd,
			long proposedStart, long proposedEnd, int expectedConstraintType, long expectedConstraintDate,
			boolean updateConstraint, int proposedConstraintType, long expectedDomainRevision) {
		public TaskScheduleDragCommand {
			TaskCommands.task(task);
			if (intervalIndex < 0) throw new IllegalArgumentException("intervalIndex must not be negative");
			revision(expectedDomainRevision);
		}
	}

	public record TaskSplitCommand(ProjectTaskKey task, long expectedStart, long expectedEnd, long splitAt,
			long expectedDomainRevision) {
		public TaskSplitCommand { TaskCommands.task(task); revision(expectedDomainRevision); }
	}

	private static List<ProjectTaskKey> requiredTasks(List<ProjectTaskKey> tasks) {
		List<ProjectTaskKey> copy = tasks == null ? List.of() : List.copyOf(tasks);
		if (copy.isEmpty()) throw new IllegalArgumentException("tasks are required");
		return copy;
	}
	private static void endpoints(ProjectTaskKey predecessor, ProjectTaskKey successor) {
		if (predecessor == null || successor == null) throw new IllegalArgumentException("dependency endpoints are required");
	}
	private static void task(ProjectTaskKey task) {
		if (task == null) throw new IllegalArgumentException("task is required");
	}
	private static void position(int position) {
		if (position < 0) throw new IllegalArgumentException("position must not be negative");
	}
	private static void revision(long revision) {
		if (revision < 0L) throw new IllegalArgumentException("revision must not be negative");
	}
}
