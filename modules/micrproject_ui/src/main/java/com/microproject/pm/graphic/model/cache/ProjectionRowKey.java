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

import com.microproject.pm.task.ProjectTaskKey;

/**
 * Identity of one row in a view projection.
 *
 * <p>Durable task and assignment rows are stable across reordering. Synthetic
 * rows are deliberately scoped to the projection session and must never be
 * persisted to an MPO file or workspace.</p>
 */
public record ProjectionRowKey(Kind kind, ProjectTaskKey taskKey, long entityId, long runtimeId) {
	public enum Kind {
		TASK,
		ASSIGNMENT,
		PROJECT_SUMMARY,
		SUBPROJECT_PROXY,
		GROUP,
		VOID,
		OTHER
	}

	public ProjectionRowKey {
		if (kind == null)
			throw new IllegalArgumentException("kind is required");
		if (taskKey == null && runtimeId <= 0L)
			throw new IllegalArgumentException("a durable or runtime identity is required");
		if (taskKey != null && (kind == Kind.GROUP || kind == Kind.VOID || kind == Kind.OTHER))
			throw new IllegalArgumentException("synthetic rows cannot carry durable identities");
		if (taskKey != null && entityId <= 0L)
			throw new IllegalArgumentException("durable entityId must be positive");
	}

	public boolean isDurable() {
		return taskKey != null && runtimeId == 0L;
	}
}
