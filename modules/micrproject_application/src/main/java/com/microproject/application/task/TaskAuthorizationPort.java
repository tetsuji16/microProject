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
package com.microproject.application.task;

import java.util.Set;

import com.microproject.pm.task.ProjectTaskKey;

/** Collaboration/authorization boundary; implementations may acquire a lock. */
@FunctionalInterface
public interface TaskAuthorizationPort {
	enum Decision {
		ALLOWED,
		READ_ONLY,
		LOCK_DENIED
	}

	/** Acquires one authorization scope for the complete command target set. */
	AuthorizationLease acquire(Set<ProjectTaskKey> keys, TaskCommandType commandType);

	interface AuthorizationLease extends AutoCloseable {
		Decision decision();
		boolean validateAtCommit();
		@Override default void close() { }
	}

	static TaskAuthorizationPort allowAll() {
		return (keys, commandType) -> fixed(Decision.ALLOWED);
	}

	static AuthorizationLease fixed(Decision decision) {
		return new AuthorizationLease() {
			@Override public Decision decision() { return decision; }
			@Override public boolean validateAtCommit() { return decision == Decision.ALLOWED; }
		};
	}
}
