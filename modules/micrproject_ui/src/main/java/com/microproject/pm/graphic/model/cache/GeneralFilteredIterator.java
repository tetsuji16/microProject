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
package com.microproject.pm.graphic.model.cache;

import java.util.Iterator;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.iterators.FilterIterator;

import com.microproject.grouping.core.transform.filtering.PredicatedNodeFilterIterator;

/**
 *
 * Iterator capable of treating object lists, node lists, and graphic node lists and filtering based
 * on a condition
 */
public class GeneralFilteredIterator extends FilterIterator implements PredicatedNodeFilterIterator  {
	private boolean nodeBased = false;

	public static GeneralFilteredIterator instance(Iterator baseIterator) {//, Predicate predicate, boolean nodeBased) {
		return new GeneralFilteredIterator(baseIterator);
	}
	public void setPredicate(Predicate child) {
		super.setPredicate(PredicateWrapper.instance(child));
		
	}
	
	private GeneralFilteredIterator(Iterator baseIterator) {//, Predicate predicate, boolean nodeBased) {
		super(baseIterator);
	}
	
	/**
	 * Next object.  If node based, then the returned object is a node.  Otherwise, it's an object (the impl of the node or graphic node)
	 */
	public Object next() {
		Object obj = super.next();
		if (nodeBased) {
			if (obj instanceof GraphicNode)
				obj = ((GraphicNode)obj).getNode();
		} else {
			obj = GraphicNode.getImpl(obj);
		}
		//System.out.println("GenalFilteredIterator: next()="+obj);
		return obj;
	}
	/**
	 * This class wraps a predicate so that it can treat a GraphicNode, a Node, or an Object by
	 * applying the predicate to the underying object.  Note that void nodes are always skipped.
	 */
	private static class PredicateWrapper implements Predicate {
		Predicate child;
		static Predicate instance(Predicate child) {
			if (child == null)
				return PredicateUtils.truePredicate(); // if no predicate, then accept all
			return new PredicateWrapper(child);
		}
		private PredicateWrapper(Predicate child) {
			this.child = child;
		}
		public boolean evaluate(Object obj) {
			if (GraphicNode.isVoid(obj)) // skip void nodes always
				return false;
			obj = GraphicNode.getImpl(obj);
			return child.evaluate(obj);
		}
	}
	public final void setNodeBased(boolean nodeBased) {
		this.nodeBased = nodeBased;
	}
}

