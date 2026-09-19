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

import java.util.function.Consumer;


import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.WalkersNodeModel;

/**
 * Traversal functor which goes recursively thru children
 */
public abstract class NodeWalker extends SummaryVisitor {
	protected SummaryVisitor visitor;
	protected Consumer<Object> closure;
	protected WalkersNodeModel nodeModel;
	private Node node = null;
	protected NodeWalker(Consumer<Object> closure) {
		this.closure = closure;
		if (closure instanceof SummaryVisitor)
			this.visitor = (SummaryVisitor)closure;
	}
	
	public void setNodeModel(WalkersNodeModel nodeModel) {
		this.nodeModel = nodeModel;
	}
	
	public void addToSummary(Object value) {
		visitor.addToSummary(value);
	}

	public Object getSummary() {
		accept(node);
		return visitor.getSummary();
	}

	public void reset() {
		visitor.reset();
	}
	
	/**
	 * @param node The node to set.
	 */
	public void setNode(Node node) {
		this.node = node;
	}
	public void setField(Field field) {
		super.setField(field);
		visitor.setField(field);
	}
	public void setContext(FieldContext context) {
		super.setContext(context);
		visitor.setContext(context);
	}
}
