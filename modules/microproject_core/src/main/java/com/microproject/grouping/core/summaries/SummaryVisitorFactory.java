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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.microproject.datatype.DurationFormat;
import com.microproject.strings.Messages;


/**
 *
 */
public class SummaryVisitorFactory implements SummaryNames {
	private static final SummaryVisitor NULL_SUMMARY_VISITOR = new SummaryVisitor() {
		public Object getSummary() {
			return null;
		}

		public void addToSummary(Object value) {
		}

		public void reset() {
		}
	};
	
	public static SummaryVisitor getInstance(int type, Class clazz, boolean forceDeep) {
		if (type==SAME){
		    return new ShallowChildWalker(new Same());
		}
		if (type == THIS) {
			return new NodeWalker(new ThisValueSummaryVisitor()) {
				public void accept(Object arg0) {
					visitor.accept(arg0);
				}
			};
		}

	    
		if (clazz == Boolean.class) {
			switch (type) {
				case OR:
					return new LeafWalker(new Maximum());
				case AND:
					return new LeafWalker(new Minimum());
			}
		} else {
	
			switch (type) {
				case MAXIMUM:
					return new LeafWalker(new Maximum());
				case MINIMUM:
					return new LeafWalker(new Minimum());
				case COUNT_ALL:
					return new DeepChildWalker(new Count(),true);
				case COUNT_NONSUMMARIES:
					return new CountNonsummariesWalker(new Count());
				case SUM:
					if (forceDeep)
						return new DeepChildWalker(new Sum(),true);
					else
						return new LeafWalker(new Sum());
				case AVERAGE:
					return new LeafWalker(new Average());
				case AVERAGE_FIRST_SUBLEVEL:
					return new ShallowChildWalker(new Average());
				case COUNT_FIRST_SUBLEVEL:				
					return new ShallowChildWalker(new Count());
				case LIST:
					return new LeafWalker(new ConcatTextSummaryVisitor());
				case NONE:
				default:	
			}
		}
		return NULL_SUMMARY_VISITOR;
	}


	private static final BidiMap COST_SUMMARY_MAP = immutableMap(
			entry("Summary.None", NONE), entry("Summary.Average", AVERAGE),
			entry("Summary.AverageFirstSublevel", AVERAGE_FIRST_SUBLEVEL), entry("Summary.Maximum", MAXIMUM),
			entry("Summary.Minimum", MINIMUM), entry("Summary.Sum", SUM));
	private static final BidiMap DATE_SUMMARY_MAP = immutableMap(
			entry("Summary.None", NONE), entry("Summary.Maximum", MAXIMUM), entry("Summary.Minimum", MINIMUM));
	private static final BidiMap DURATION_SUMMARY_MAP = COST_SUMMARY_MAP;
	private static final BidiMap FLAG_SUMMARY_MAP = immutableMap(
			entry("Summary.None", NONE), entry("Summary.OR", OR), entry("Summary.AND", AND));
	private static final BidiMap NUMBER_SUMMARY_MAP = immutableMap(
			entry("Summary.None", NONE), entry("Summary.Average", AVERAGE),
			entry("Summary.AverageFirstSublevel", AVERAGE_FIRST_SUBLEVEL), entry("Summary.CountAll", COUNT_ALL),
			entry("Summary.CountFirstSublevel", COUNT_FIRST_SUBLEVEL),
			entry("Summary.CountNonsummaries", COUNT_NONSUMMARIES), entry("Summary.Maximum", MAXIMUM),
			entry("Summary.Minimum", MINIMUM), entry("Summary.Sum", SUM));
	private static final BidiMap TEXT_SUMMARY_MAP = immutableMap(
			entry("Summary.None", NONE), entry("Summary.List", LIST));

	private static Object[] entry(String key, int value) {
		return new Object[] { Messages.getString(key), Integer.valueOf(value) };
	}

	private static BidiMap immutableMap(Object[]... entries) {
		BidiMap map = new DualHashBidiMap();
		for (Object[] entry : entries)
			map.put(entry[0], entry[1]);
		return UnmodifiableBidiMap.unmodifiableBidiMap(map);
	}
	
	private static final Map<String, Integer> ALL_SUMMARY_MAP;
	static {
		Map<String, Integer> m = new HashMap<>();
		m.put("None", Integer.valueOf(NONE));
		m.put("This", Integer.valueOf(THIS));
		m.put("List", Integer.valueOf(LIST));
		m.put("Average", Integer.valueOf(AVERAGE));
		m.put("AverageFirstSublevel", Integer.valueOf(AVERAGE_FIRST_SUBLEVEL));
		m.put("CountAll", Integer.valueOf(COUNT_ALL));
		m.put("CountFirstSublevel", Integer.valueOf(COUNT_FIRST_SUBLEVEL));
		m.put("CountNonsummaries", Integer.valueOf(COUNT_NONSUMMARIES));
		m.put("Maximum", Integer.valueOf(MAXIMUM));
		m.put("Minimum", Integer.valueOf(MINIMUM));
		m.put("Sum", Integer.valueOf(SUM));
		m.put("OR", Integer.valueOf(OR));
		m.put("AND", Integer.valueOf(AND));
		m.put("Same", Integer.valueOf(SAME));
		ALL_SUMMARY_MAP = Map.copyOf(m);
	}
 
/**
 * Used when reading in config file to transform a summary name into an id
 * @param name
 * @return
 */	public static int getSummaryId(String name) {
		Integer id = (Integer) ALL_SUMMARY_MAP.get(name);
		if (id == null)
			return NONE;
		return id.intValue();
	}
	
	public static BidiMap getMap(Class clazz, boolean cost) {
		if (clazz == Double.class) {
			return cost ? COST_SUMMARY_MAP : NUMBER_SUMMARY_MAP;
		} else if (clazz == Date.class) {
			return DATE_SUMMARY_MAP;
		} else if (clazz == DurationFormat.class) {
			return DURATION_SUMMARY_MAP;
		} else if (clazz == String.class) {
			return TEXT_SUMMARY_MAP;
		} else if (clazz == Boolean.class) {
			return FLAG_SUMMARY_MAP;
		}
			
		return null;

	}  


}
