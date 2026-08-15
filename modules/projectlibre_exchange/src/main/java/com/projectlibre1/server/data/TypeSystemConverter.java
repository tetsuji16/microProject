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

package com.projectlibre1.server.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;

import com.projectlibre1.field.Field;
import com.projectlibre1.grouping.core.model.NodeModel;

public interface TypeSystemConverter {
	public Map<String,Object> convertFieldsAndCustomAttributes(Object obj, List filtredFieldArray, NodeModel model,Predicate extraFieldFilter, boolean includeNulls);
	public Map<String,Object> convertFieldsAndCustomAttributes(Object obj, Collection filtredExtraFields, List filtredFieldArray, NodeModel model, boolean includeNulls);
	public Collection getDirtyExtraFields(Object obj,Predicate fieldFilter);
	public List getExposedAssignmentFields(Predicate fieldFilter);
	public List getExposedTaskFields(Predicate fieldFilter);
	public List getExposedProjectFields(Predicate fieldFilter);
	public void convertFields(Object obj, Collection fieldArray, boolean shortNames, Map<String,Object> attrs,Transformer typeConverter, NodeModel model, boolean includeNulls);
	public void convertField(Object obj, Field field, String id, Map<String,Object> attrs, NodeModel model, boolean includeNulls);

}
