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

public class DefaultSubProj extends NormalTask implements SubProj {
	private static final long serialVersionUID = 1L;
	private long subprojectUniqueId;
	private boolean fetching;
	private Map subprojectFieldValues;

	public DefaultSubProj(Project dummy, Long id) {
		super(dummy);
		setSubprojectUniqueId(id == null ? 0L : id.longValue());
	}

	public DefaultSubProj() {
		super();
	}

	@Override
	public boolean isSubproject() {
		return true;
	}
	public Project getSubproject() {
		if (!isValid())
			return null;
		return ProjectFactory.getInstance().findFromId(subprojectUniqueId);
	}

	public long getSubprojectUniqueId() {
		return subprojectUniqueId;
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

	public boolean isWritable() {
		Project subproject = getSubproject();
		return isSubprojectOpen() && subproject != null && !subproject.isReadOnly();
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
