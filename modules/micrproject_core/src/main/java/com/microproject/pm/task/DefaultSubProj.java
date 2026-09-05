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

import java.io.File;
import java.util.Map;

import com.microproject.field.FieldContext;
import com.microproject.grouping.core.LazyParent;
import com.microproject.grouping.core.Node;

public class DefaultSubProj extends NormalTask implements SubProj, LazyParent {
	private static final long serialVersionUID = 1L;
	private long subprojectUniqueId;
	private boolean fetching;
	private Map subprojectFieldValues;
	/** Canonical linked project path, persisted with the master project. */
	private String subprojectFile;
	/** Portable stored path, relative to the master project whenever possible. */
	private String storedSubprojectPath;
	/** Canonical absolute path observed when the child was last loaded. */
	private String canonicalSubprojectPath;
	/** Stable identity observed from the child project at the last successful load. */
	private String lastKnownProjectId;
	private long lastKnownModifiedTime;
	/** The insertion mode selected by the master-project author. */
	private boolean subprojectReadOnly;
	/** Last recovery result for a path-persisted child. */
	private LoadStatus loadStatus = LoadStatus.NOT_LOADED;
	/** Stable master-local identity; unlike a child project id this identifies the reference itself. */
	private String referenceId = java.util.UUID.randomUUID().toString();

	public DefaultSubProj(Project dummy, Long id) {
		super(dummy);
		// Serialized legacy projects can contain signed/hash-derived IDs. Keep that
		// value during deserialization; newly assigned IDs still go through the
		// validated setter below.
		this.subprojectUniqueId = id == null ? 0L : id.longValue();
	}

	public DefaultSubProj() {
		super();
	}

	@Override
	public boolean isSubproject() {
		return true;
	}

	@Override
	public String getName() {
		String name = super.getName();
		if (name != null && !name.isBlank())
			return displayName(name);
		Project subproject = getSubproject();
		if (subproject != null)
			return displayName(ProjectFactory.getDisplayNameForSavePrompt(subproject));
		return displayName(subprojectFile == null || subprojectFile.isBlank() ? name : new File(subprojectFile).getName());
	}

	private String displayName(String name) {
		return switch (getLoadStatus()) {
		case MISSING -> (name == null ? "" : name) + " [Missing subproject]";
		case INVALID -> (name == null ? "" : name) + " [Invalid subproject]";
		case ACCESS_DENIED -> (name == null ? "" : name) + " [Access denied]";
		case UNAVAILABLE -> (name == null ? "" : name) + " [Unavailable subproject]";
		case CYCLE -> (name == null ? "" : name) + " [Circular subproject reference]";
		default -> name;
		};
	}

	@Override
	public String getName(FieldContext fieldContext) {
		return getName();
	}

	public Project getSubproject() {
		if (!isValid())
			return null;
		return ProjectFactory.getInstance().findFromId(subprojectUniqueId);
	}

	public long getSubprojectUniqueId() {
		return subprojectUniqueId;
	}

	@Override
	public String getReferenceId() {
		if (referenceId == null || referenceId.isBlank())
			referenceId = java.util.UUID.randomUUID().toString();
		return referenceId;
	}

	@Override
	public void setReferenceId(String referenceId) {
		if (referenceId == null || referenceId.isBlank()) {
			this.referenceId = java.util.UUID.randomUUID().toString();
			return;
		}
		this.referenceId = java.util.UUID.fromString(referenceId).toString();
	}

	public boolean isSubprojectOpen() {
		return !fetching && getSubproject() != null;
	}

	public boolean isValid() {
		return subprojectUniqueId > 0L;
	}

	public boolean isValidAndOpen() {
		return isValid() && isSubprojectOpen();
	}

	@Override
	public boolean isDataFetched() {
		return isValidAndOpen();
	}

	@Override
	public boolean fetchData(Node node) {
		if (isDataFetched())
			return true;
		Project parent = getProject();
		if (parent == null || node == null || !isValid())
			return false;
		ProjectFactory.getInstance().openSubproject(parent, node, false);
		return false;
	}

	public boolean isWritable() {
		Project subproject = getSubproject();
		return isSubprojectOpen() && subproject != null && !isSubprojectReadOnly() && !subproject.isReadOnly();
	}

	@Override
	public String getSubprojectFile() {
		return subprojectFile;
	}

	@Override
	public boolean fieldHideSubprojectFile(FieldContext fieldContext) {
		return false;
	}

	@Override
	public void setSubprojectFile(String subprojectFile) {
		this.subprojectFile = subprojectFile;
		this.loadStatus = LoadStatus.NOT_LOADED;
	}

	@Override public String getStoredSubprojectPath() { return storedSubprojectPath == null || storedSubprojectPath.isBlank() ? subprojectFile : storedSubprojectPath; }
	@Override public void setStoredSubprojectPath(String value) { storedSubprojectPath = value; }
	@Override public String getCanonicalSubprojectPath() { return canonicalSubprojectPath == null || canonicalSubprojectPath.isBlank() ? subprojectFile : canonicalSubprojectPath; }
	@Override public void setCanonicalSubprojectPath(String value) { canonicalSubprojectPath = value; }
	@Override public String getLastKnownProjectId() { return lastKnownProjectId; }
	@Override public void setLastKnownProjectId(String value) { lastKnownProjectId = value; }
	@Override public long getLastKnownModifiedTime() { return lastKnownModifiedTime; }
	@Override public void setLastKnownModifiedTime(long value) { lastKnownModifiedTime = Math.max(0L, value); }

	@Override
	public boolean isSubprojectReadOnly() {
		Project subproject = getSubproject();
		return subprojectReadOnly || (subproject != null && subproject.isReadOnly());
	}

	@Override
	public boolean fieldHideSubprojectReadOnly(FieldContext fieldContext) {
		return false;
	}

	public void setSubprojectReadOnly(boolean subprojectReadOnly) {
		this.subprojectReadOnly = subprojectReadOnly;
	}

	@Override
	public LoadStatus getLoadStatus() {
		// A persisted load failure must remain visible even if an unrelated
		// in-memory project happens to share the legacy numeric id.  A successful
		// reload explicitly resets this to OPEN.
		return loadStatus == LoadStatus.NOT_LOADED && isSubprojectOpen() ? LoadStatus.OPEN : loadStatus;
	}

	/** A display-safe status for the master projection; the enum is kept as model state. */
	@Override
	public String getSubprojectStatus() {
		return getLoadStatus().name();
	}

	@Override
	public boolean fieldHideSubprojectStatus(FieldContext fieldContext) {
		return false;
	}

	@Override
	public void setLoadStatus(LoadStatus loadStatus) {
		this.loadStatus = loadStatus == null ? LoadStatus.NOT_LOADED : loadStatus;
	}

	public void setFetching(boolean b) {
		fetching = b;
	}

	public void setSchedulesFromSubprojectFieldValues() {
		// Schedule fields are applied by the project loader after the subproject opens.
	}

	public void setSubprojectFieldValues(Map subprojectFieldValues) {
		this.subprojectFieldValues = subprojectFieldValues;
	}

	public void setSubprojectUniqueId(long subprojectId) {
		if (subprojectId < 0L)
			throw new IllegalArgumentException("Subproject id cannot be negative: " + subprojectId);
		this.subprojectUniqueId = subprojectId;
	}

}
