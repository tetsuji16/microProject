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
package com.microproject.pm.graphic.model.cache;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.microproject.grouping.core.Node;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;

/** Creates durable keys when possible and collision-free session keys otherwise. */
final class ProjectionRowKeyResolver {
	private final Map<Object, Long> runtimeIds = new IdentityHashMap<>();
	private final Map<String, Long> semanticRuntimeIds = new HashMap<>();
	private long nextRuntimeId = 1L;

	ProjectionRowKey resolve(GraphicNode graphicNode) {
		return resolve(graphicNode, null);
	}

	ProjectionRowKey resolve(GraphicNode graphicNode, String syntheticGroupIdentity) {
		if (graphicNode == null)
			return runtime(ProjectionRowKey.Kind.OTHER, this);
		Object node = graphicNode.getNode();
		Object value = graphicNode.getNode() == null ? null : graphicNode.getNode().getImpl();
		if (graphicNode.isVoid())
			return runtime(ProjectionRowKey.Kind.VOID, node == null ? graphicNode : node);
		if (graphicNode.isGroup())
			return semanticRuntime(ProjectionRowKey.Kind.GROUP,
					syntheticGroupIdentity == null ? groupPath(graphicNode) : syntheticGroupIdentity);
		if (value instanceof Assignment assignment) {
			Task task = assignment.getTask();
			ProjectTaskKey taskKey = ProjectTaskKey.from(task).orElse(null);
			if (taskKey != null && assignment.getUniqueId() > 0L)
				return new ProjectionRowKey(ProjectionRowKey.Kind.ASSIGNMENT, taskKey, assignment.getUniqueId(), 0L);
			return runtime(ProjectionRowKey.Kind.ASSIGNMENT, value);
		}
		if (value instanceof Task task) {
			ProjectionRowKey.Kind kind = task instanceof SubProj
					? ProjectionRowKey.Kind.SUBPROJECT_PROXY
					: ProjectionRowKey.Kind.TASK;
			ProjectTaskKey key = ProjectTaskKey.from(task).orElse(null);
			return key == null
					? runtime(kind, value)
					: new ProjectionRowKey(kind, key, task.getUniqueId(), 0L);
		}
		if (value instanceof Project project && project.getUniqueId() > 0L) {
			ProjectTaskKey projectKey = new ProjectTaskKey(project.getUniqueId(), project.getUniqueId());
			return new ProjectionRowKey(ProjectionRowKey.Kind.PROJECT_SUMMARY, projectKey, project.getUniqueId(), 0L);
		}
		return runtime(ProjectionRowKey.Kind.OTHER, value == null ? graphicNode : value);
	}

	private ProjectionRowKey runtime(ProjectionRowKey.Kind kind, Object identity) {
		long id = runtimeIds.computeIfAbsent(identity, ignored -> nextRuntimeId++);
		return new ProjectionRowKey(kind, null, 0L, id);
	}

	private ProjectionRowKey semanticRuntime(ProjectionRowKey.Kind kind, String identity) {
		long id = semanticRuntimeIds.computeIfAbsent(kind.name() + ':' + identity, ignored -> nextRuntimeId++);
		return new ProjectionRowKey(kind, null, 0L, id);
	}

	private static String groupPath(GraphicNode graphicNode) {
		StringBuilder path = new StringBuilder().append(graphicNode.getLevel());
		Node current = graphicNode.getNode();
		while (current != null) {
			path.insert(0, '/').insert(0, String.valueOf(current.getImpl()));
			current = current.getParent() instanceof Node parent ? parent : null;
		}
		return path.toString();
	}
}
