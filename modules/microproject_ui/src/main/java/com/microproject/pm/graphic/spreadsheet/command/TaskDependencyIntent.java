/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.command;

import java.util.List;
import java.util.Objects;

import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.task.Task;

/** A dependency edit addressed to task identities in one visible projection. */
public record TaskDependencyIntent(Operation operation, List<ProjectionRowKey.TaskRow> tasks,
		long projectionRevision, DependencyTarget dependency) {
	public enum Operation { LINK, UNLINK }

	/** The directed task pair that uniquely identifies one dependency in a project. */
	public record DependencyTarget(long dependencyUniqueId, ProjectTaskKey predecessor, ProjectTaskKey successor) {
		public DependencyTarget {
			Objects.requireNonNull(predecessor, "predecessor");
			Objects.requireNonNull(successor, "successor");
			if (predecessor.equals(successor))
				throw new IllegalArgumentException("dependency endpoints must differ");
		}

		public static DependencyTarget from(Dependency dependency) {
			if (dependency == null || !(dependency.getPredecessor() instanceof Task predecessor)
					|| !(dependency.getSuccessor() instanceof Task successor))
				throw new IllegalArgumentException("dependency must have task endpoints");
			return new DependencyTarget(dependency.getUniqueId(), ProjectTaskKey.from(predecessor).orElseThrow(),
				ProjectTaskKey.from(successor).orElseThrow());
		}

		public boolean matches(Dependency dependency) {
			return dependency != null && dependency.getUniqueId() == dependencyUniqueId
				&& dependency.getPredecessor() instanceof Task predecessor
				&& dependency.getSuccessor() instanceof Task successor
				&& ProjectTaskKey.from(predecessor).filter(this.predecessor::equals).isPresent()
				&& ProjectTaskKey.from(successor).filter(this.successor::equals).isPresent();
		}
	}

	public TaskDependencyIntent {
		Objects.requireNonNull(operation, "operation");
		tasks = tasks == null ? List.of() : List.copyOf(tasks);
		if (tasks.isEmpty() || tasks.stream().anyMatch(Objects::isNull))
			throw new IllegalArgumentException("task rows are required");
		if (tasks.stream().map(ProjectionRowKey.TaskRow::taskKey).distinct().count() != tasks.size())
			throw new IllegalArgumentException("task rows must have unique task identities");
		if (projectionRevision < 0L)
			throw new IllegalArgumentException("projectionRevision must not be negative");
		if (operation == Operation.LINK && (tasks.size() < 2 || dependency != null))
			throw new IllegalArgumentException("link requires multiple tasks and no dependency target");
		if (operation == Operation.UNLINK && dependency != null
				&& tasks.stream().noneMatch(row -> row.taskKey().equals(dependency.predecessor())
					|| row.taskKey().equals(dependency.successor())))
			throw new IllegalArgumentException("selected dependency must touch a selected task");
	}
}
