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

import com.microproject.grouping.core.Node;
import com.microproject.pm.task.ProjectTaskKey;

/** Detached core nodes are a compatibility payload; no UI/cache handle crosses this boundary. */
public record TaskPasteCommand(ProjectTaskKey parent, int position, List<Node> detachedNodes,
		long expectedDomainRevision) {
	public TaskPasteCommand {
		detachedNodes = detachedNodes == null ? List.of() : List.copyOf(detachedNodes);
		if (detachedNodes.isEmpty()) throw new IllegalArgumentException("detachedNodes are required");
		if (position < 0) throw new IllegalArgumentException("position must not be negative");
		if (expectedDomainRevision < 0L) throw new IllegalArgumentException("expectedDomainRevision must not be negative");
	}
}
