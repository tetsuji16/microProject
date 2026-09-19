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
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An interval generator which is itself contains a collection of one or more other generators
 */
public class IntervalGeneratorSet implements IntervalGenerator {
	private IntervalGenerator currentIntervalGenerator = null;
	private List<IntervalGenerator> generators = null;
	private static final Logger logger = Logger.getLogger(IntervalGeneratorSet.class.getName());
	private boolean sameEarliestEnding = true; // flag if more than one of the earliest generators has the same start time
	public Collection<IntervalGenerator> getGenerators() {
		return generators;
	}
	public static IntervalGenerator extractUnshared(Collection<IntervalGenerator> generatorList) {
		Iterator<IntervalGenerator> i = generatorList.iterator();
		IntervalGenerator current;
		while (i.hasNext()) {
			current = i.next();
			if (!current.canBeShared()) {
				i.remove();
				if (generatorList.isEmpty()) // list can't be empty. The fields themselves wouldn't be evalulated
					generatorList.add(RangeIntervalGenerator.continuous());
				return current;
			}
		}
		return null;
	}

	public static IntervalGeneratorSet getInstance(List<IntervalGenerator> intervalGenerators) {
		return new IntervalGeneratorSet(intervalGenerators);
	}

	public static IntervalGeneratorSet getInstance(IntervalGenerator intervalGenerator) {
		return new IntervalGeneratorSet(intervalGenerator);
	}

	public static IntervalGeneratorSet getInstance(IntervalGenerator intervalGenerator1, IntervalGenerator intervalGenerator2) {
		return new IntervalGeneratorSet(intervalGenerator1, intervalGenerator2);
	}

	/**
	 * The earliest ending interval is returned. In case of a tie, priority goes to the groupBy generator, and then
	 * the order of the from list
	 * @return
	 */
	protected IntervalGenerator earliestEndingGenerator() {
		long minEnd = Long.MAX_VALUE;
		IntervalGenerator result = null;
		if (generators != null) {
			long generatorEnd;
			Iterator<IntervalGenerator> i = generators.iterator();
			IntervalGenerator current;
			while (i.hasNext()) {
				current = i.next();
				generatorEnd = current.currentEnd();
				if (generatorEnd == minEnd)
					sameEarliestEnding = true;
				if (generatorEnd < minEnd) {
					minEnd = generatorEnd;
					result = current;
				}
			}
		}
	
		return result;
	}
	
//	private long earliestStart() {
//		long minStart = Long.MAX_VALUE;
//		if (generators != null) {
//			long generatorStart;
//			Iterator i = generators.iterator();
//			IntervalGenerator current;
//			while (i.hasNext()) {
//				current = (IntervalGenerator) i.next();
//				generatorStart = current.currentStart();
//				sameStart = generatorStart == minStart; // keep track if there is more than one with same start
//				if (generatorStart < minStart) {
//					minStart = generatorStart;
//				}
//			}
//		}
//		return minStart;
//	}		
	
	/**
	 * 
	 */
	private IntervalGeneratorSet(List<IntervalGenerator> intervalGenerators) {
		super();
		generators = intervalGenerators;
		initialize();
	}

	private IntervalGeneratorSet(IntervalGenerator intervalGenerator) {
		if (generators == null)
			generators = new LinkedList<>();
		generators.add(intervalGenerator);
		initialize();
	}

	private IntervalGeneratorSet(IntervalGenerator intervalGenerator1, IntervalGenerator intervalGenerator2) {
		if (generators == null)
			generators = new LinkedList<>();
		generators.add(intervalGenerator1);
		generators.add(intervalGenerator2);		
		initialize();		
	}
	
	public void remove(IntervalGenerator removeMe) {
		generators.remove(removeMe);
	}

	public boolean isEmpty() {
		return generators.isEmpty();
	}
	
	private void initialize() {
		currentIntervalGenerator = earliestEndingGenerator();
		if (currentIntervalGenerator == null)
			logger.log(Level.FINE, "No current interval generator after initialization");
	}
	public Object current() {
		return currentIntervalGenerator.current();
	}

	public long currentEnd() {
		return currentIntervalGenerator.currentEnd();		
	}

	public long currentStart() {
		return currentIntervalGenerator.currentStart();
	}

	public boolean isCurrentActive() {
		return currentIntervalGenerator.isCurrentActive();
	}

	public boolean hasNext() {
		return currentIntervalGenerator != null;
	}

	public boolean canBeShared() {
		return false;
	}

	public boolean evaluate(Object arg0) {
		boolean result = true;
		currentIntervalGenerator = earliestEndingGenerator();
		if (currentIntervalGenerator == null)
			return false;
		
		// it is fairly common that two or more generators share the same endpoint.  If so, they all must be evaluated
		if (sameEarliestEnding) {
			long earliestEnd = currentIntervalGenerator.currentEnd();
			Iterator<IntervalGenerator> i = generators.iterator();
			IntervalGenerator current;
			while (i.hasNext()) {
				current = i.next();
				if (current.currentEnd() == earliestEnd) { // see if this generator is at the earliest end point too
					if (!current.evaluate(arg0))
						result = false;
				}
			}
		} else { // only one generator
			result = currentIntervalGenerator.evaluate(arg0);	
		}
		return result;
	}
}
		
		

