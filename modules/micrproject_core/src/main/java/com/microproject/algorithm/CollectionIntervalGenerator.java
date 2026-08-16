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
package com.microproject.algorithm;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.time.HasStartAndEnd;


/**
 * An abstract class for an interval generator that wraps a collection. 
 */
public class CollectionIntervalGenerator implements IntervalGenerator, HasStartAndEnd {
	private static final Logger logger = Logger.getLogger(CollectionIntervalGenerator.class.getName());
	protected Collection<? extends HasStartAndEnd> collection;
	HasStartAndEnd current = null;
	protected boolean finished = false;
	long start = 0;
	boolean active = false;
	Iterator<? extends HasStartAndEnd> iterator;
	/**
	 * 
	 */
	protected CollectionIntervalGenerator(Collection<? extends HasStartAndEnd> collection) {
		this.collection = collection;
		initialize();
	}
	
	public static CollectionIntervalGenerator getInstance(Collection<? extends HasStartAndEnd> collection) {
		return new CollectionIntervalGenerator(collection); 
	}
		
	public static CollectionIntervalGenerator getInstance(HasStartAndEnd interval) {
		LinkedList<HasStartAndEnd> list = new LinkedList<>();
		list.add(interval);
		return getInstance(list);
	}	

	protected Iterator<? extends HasStartAndEnd> makeIterator() {
		if (collection instanceof List)
			return ((List<? extends HasStartAndEnd>) collection).listIterator();
		else
			return collection.iterator();
	}
	protected void initialize() {
		iterator = makeIterator();
		if (iterator.hasNext()) {
			current = iterator.next();
			updateActiveState();
		}
			
		
	}
	
	private void updateActiveState() {
		active = current.getStart() == start;
	}
	
	public Object current() {
		if (active)
			return current;
		else
			return this;
	}

	public boolean evaluate(Object obj) {
		start = currentEnd(); // move start ahead		
		if (active) { // active implies that the value comes from the collection 
			if (!iterator.hasNext()) {
				current = null;
				finished = true;
				active = false;
				return false;
			}
			current =  iterator.next();
		}
//		start = currentEnd(); // move start ahead
		updateActiveState(); // will set to active if the current item in collecition starts at start
		return true;
	}

	
	public int compareTo(Object arg0) {
		return 0;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}
	
	public boolean isCurrentActive() {
		return active;
	}

	public long currentEnd() {
		long curEnd = (current == null) ? Long.MAX_VALUE : current.getEnd();
		
		if (curEnd == 1)
			logger.log(Level.FINE, "Unexpected current end value: {0}", curEnd);
		return active ? curEnd : current.getStart();		
	}

	public long currentStart() {
		return start;
//		return (current == null) ? lastEnd : ((HasStartAndEnd)current).getStart();
	}

	
	/**
	 * @return Returns the finished.
	 */
	public boolean isFinished() {
		return finished;
	}
	public long getEnd() {
		return currentEnd();
	}
	public long getStart() {
		return currentStart();
	}
	
	public boolean canBeShared() {
		return true;
	}	
}
