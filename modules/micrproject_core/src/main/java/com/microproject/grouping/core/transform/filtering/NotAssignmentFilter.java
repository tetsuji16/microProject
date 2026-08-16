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
package com.microproject.grouping.core.transform.filtering;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.VoidNodeImpl;
import com.microproject.pm.assignment.Assignment;
import com.microproject.util.ClassUtils;

/**
 * For internal use only
 */
public class NotAssignmentFilter extends NodeFilter {
	private boolean writableOnly = false;
	public boolean evaluate(Object obj) {
		Node node=(Node)obj;
		Object impl = node.getImpl();
		if (impl == null)
			return false;
		if (impl instanceof VoidNodeImpl)
			return false;
		if  (impl instanceof Assignment)
			return false;
		return (!writableOnly || !ClassUtils.isObjectReadOnly(impl));
	}
	private static NotAssignmentFilter instance = null;
	private static NotAssignmentFilter writableInstance = null;
	private NotAssignmentFilter(boolean writableOnly) {
		super();
		this.writableOnly = writableOnly;
	}

	
	public static NotAssignmentFilter getInstance() {
		if (instance == null)
			instance = new NotAssignmentFilter(false);
		return instance;
	}
	public static NotAssignmentFilter getWritableInstance() {
		if (writableInstance == null)
			writableInstance = new NotAssignmentFilter(true);
		return writableInstance;
	}
}
