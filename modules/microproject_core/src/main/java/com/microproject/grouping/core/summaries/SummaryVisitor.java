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
package com.microproject.grouping.core.summaries;


import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeVisitor;

/**
 *
 */
public abstract class SummaryVisitor implements NodeVisitor {
	protected Field field;
	private FieldContext context = null;
	public SummaryVisitor() {
	}
	/*public SummaryVisitor(NodeField field) {
		this.field=field;
	}*/
	
	/**
	 * @param field The field to set.
	 */
	public void setField(Field field) {
		this.field = field;
		reset();
	}
	
	
	public void accept(Object node) {
	    Object nodeImpl=((Node)node).getImpl();
		Object value = field.getValue(nodeImpl,context);
		if (value != null)
			addToSummary(value);
	}
	
	public abstract  Object getSummary();
	
	public  abstract void addToSummary(Object value); //summary+=((Integer)value).intValue(); for extense

	public void setContext(FieldContext context) {
		this.context = context;
	}
}
