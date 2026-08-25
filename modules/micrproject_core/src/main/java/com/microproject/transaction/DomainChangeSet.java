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
package com.microproject.transaction;

import java.util.Set;
import java.util.UUID;

import com.microproject.pm.task.ProjectTaskKey;

/** Immutable description of one committed domain mutation. */
public record DomainChangeSet(UUID transactionId, long domainRevision, Origin origin,
		Set<ProjectTaskKey> affectedTasks, Set<String> affectedFieldIds, TopologyImpact topologyImpact,
		boolean scheduleCascade, boolean dependencyCascade) {
	public enum Origin {
		COMMAND,
		UNDO,
		REDO,
		IMPORT,
		COLLABORATION,
		LEGACY
	}

	public enum TopologyImpact {
		NONE,
		ROWS,
		FULL_PROJECTION_INVALIDATION
	}

	public DomainChangeSet {
		if (transactionId == null || origin == null || topologyImpact == null)
			throw new IllegalArgumentException("transaction metadata is required");
		if (domainRevision <= 0L)
			throw new IllegalArgumentException("domainRevision must be positive");
		affectedTasks = affectedTasks == null ? Set.of() : Set.copyOf(affectedTasks);
		affectedFieldIds = affectedFieldIds == null ? Set.of() : Set.copyOf(affectedFieldIds);
	}

	public record Draft(UUID transactionId, Origin origin, Set<ProjectTaskKey> affectedTasks,
			Set<String> affectedFieldIds, TopologyImpact topologyImpact, boolean scheduleCascade,
			boolean dependencyCascade) {
		public Draft {
			if (transactionId == null || origin == null || topologyImpact == null)
				throw new IllegalArgumentException("transaction metadata is required");
			affectedTasks = affectedTasks == null ? Set.of() : Set.copyOf(affectedTasks);
			affectedFieldIds = affectedFieldIds == null ? Set.of() : Set.copyOf(affectedFieldIds);
		}

		public static Draft fullInvalidation(Origin origin) {
			return new Draft(UUID.randomUUID(), origin, Set.of(), Set.of(),
					TopologyImpact.FULL_PROJECTION_INVALIDATION, true, true);
		}
	}
}
