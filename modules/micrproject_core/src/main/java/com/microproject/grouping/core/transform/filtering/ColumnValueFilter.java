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

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import com.microproject.field.Field;
import com.microproject.grouping.core.Node;

/**
 * Per-column auto-filter (issue #205): keeps the nodes whose field value is one
 * of the accepted values. An empty accepted set means "no filtering" so the
 * filter is inactive and the view keeps all rows.
 */
public class ColumnValueFilter extends NodeFilter {
	protected Field field;
	protected Set<String> acceptedValues = new HashSet<>();
	protected boolean caseSensitive;
	protected Consumer<Object> callback;

	public ColumnValueFilter(Field field) {
		this.field = field;
	}

	public Field getField() {
		return field;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	@Override
	public boolean isActive() {
		return !acceptedValues.isEmpty();
	}

	public void setAcceptedValues(Collection<String> values, boolean caseSensitive) {
		setAcceptedValues(values, caseSensitive, true);
	}

	public void setAcceptedValues(Collection<String> values, boolean caseSensitive, boolean needCallback) {
		this.caseSensitive = caseSensitive;
		Set<String> normalized = new HashSet<>();
		if (values != null) {
			for (String value : values) {
				if (value == null)
					continue;
				normalized.add(caseSensitive ? value : value.toUpperCase(Locale.ROOT));
			}
		}
		this.acceptedValues = normalized;
		if (needCallback && callback != null)
			callback.accept(this);
	}

	public boolean evaluate(Object o) {
		if (!isActive())
			return true;
		if (!(o instanceof Node node))
			return true;
		return matchesImpl(node.getImpl());
	}

	/**
	 * Package-visible so tests can exercise the value matching without building
	 * a full {@link Node} hierarchy.
	 */
	boolean matchesImpl(Object impl) {
		if (!isActive())
			return true;
		if (impl == null)
			return true;
		String value = field.getText(impl, null);
		if (value == null)
			value = "";
		return acceptedValues.contains(caseSensitive ? value : value.toUpperCase(Locale.ROOT));
	}

	@Override
	public void setRedefinitionCallBack(Consumer<Object> callback) {
		this.callback = callback;
	}

	@Override
	public ColumnValueFilter copyForSession() {
		ColumnValueFilter copy = new ColumnValueFilter(field);
		copy.setShowEmptyLines(isShowEmptyLines());
		copy.setShowEndEmptyLines(isShowEndEmptyLines());
		copy.setShowSummary(isShowSummary());
		copy.setShowEmptySummaries(isShowEmptySummaries());
		copy.setShowAssignments(isShowAssignments());
		copy.setPreserveHierarchy(isPreserveHierarchy());
		copy.setAcceptedValues(acceptedValues, caseSensitive, false);
		return copy;
	}
}
