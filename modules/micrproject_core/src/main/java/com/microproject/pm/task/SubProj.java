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
package com.microproject.pm.task;

import java.util.Map;

public interface SubProj {
	enum LoadStatus {
		NOT_LOADED,
		OPEN,
		MISSING,
		INVALID,
		ACCESS_DENIED,
		UNAVAILABLE,
		CYCLE
	}
	Project getSubproject();

	boolean isSubprojectOpen();

	boolean isValidAndOpen();

	boolean isWritable();

	long getSubprojectUniqueId();

	/** Stable identity of this reference inside its master project. */
	default String getReferenceId() {
		return null;
	}

	default void setReferenceId(String referenceId) {
	}

	/** Portable persisted path, relative to the master where that is possible. */
	default String getStoredSubprojectPath() { return getSubprojectFile(); }
	default void setStoredSubprojectPath(String storedSubprojectPath) { }
	/** Canonical absolute path observed when the child was last successfully loaded. */
	default String getCanonicalSubprojectPath() { return getSubprojectFile(); }
	default void setCanonicalSubprojectPath(String canonicalSubprojectPath) { }
	/** Stable identity reported by the child when it was last successfully loaded. */
	default String getLastKnownProjectId() { return null; }
	default void setLastKnownProjectId(String lastKnownProjectId) { }
	default long getLastKnownModifiedTime() { return 0L; }
	default void setLastKnownModifiedTime(long lastKnownModifiedTime) { }

	/** Persisted source path, when the subproject has not been opened yet. */
	default String getSubprojectFile() {
		return null;
	}

	void setFetching(boolean b);

	boolean isValid();

	void setSubprojectFieldValues(Map subprojectFieldValues);

	void setSubprojectUniqueId(long subprojectId);

	default void setSubprojectReadOnly(boolean subprojectReadOnly) {
	}

	/** A persisted-reference recovery state suitable for displaying in the master outline. */
	default LoadStatus getLoadStatus() {
		return LoadStatus.NOT_LOADED;
	}

	default void setLoadStatus(LoadStatus loadStatus) {
	}

	void setSchedulesFromSubprojectFieldValues();

}
