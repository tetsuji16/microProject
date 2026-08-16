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
package com.microproject.grouping.core;

import java.util.Collection;
import java.util.Iterator;

import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Task;

/**
 * An Iterator for Node lists which will treat it like an impl list and consider elements of a certain type.
 * Useful for applying a fuction to a selection
 */
public class TypedNodeIterator implements Iterator {
	public static TypedNodeIterator getInstance(Collection collection, Class clazz) {
		return new TypedNodeIterator(collection,clazz);
	}
	public static TypedNodeIterator getTaskInstance(Collection collection) {
		return new TypedNodeIterator(collection,Task.class);
	}
	public static TypedNodeIterator getResourceInstance(Collection collection) {
		return new TypedNodeIterator(collection,Resource.class);
	}
	
	private Iterator i;
	private Class clazz;
	private Object next = null;
	
	private Object nextOfType() {
		Object cur = null;
		while (i.hasNext()) {
			cur = ((Node)i.next()).getImpl();
			if (clazz.isAssignableFrom(cur.getClass()))
				return cur;
		}
		return null;
	}
	private TypedNodeIterator(Collection collection, Class clazz) {
		i = collection.iterator();
		this.clazz = clazz;
		if (i.hasNext())
			next = nextOfType();
	}
	public boolean equals(Object arg0) {
		return i.equals(arg0);
	}
	public int hashCode() {
		return i.hashCode();
	}
	public boolean hasNext() {
		return next != null;
	}
	public Object next() {
		Object result = next;
		next = nextOfType();
		return result;
	}
	public void remove() {
		i.remove();
	}
	public String toString() {
		return i.toString();
	}
}
