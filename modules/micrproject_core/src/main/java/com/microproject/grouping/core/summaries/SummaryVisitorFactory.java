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

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

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


	private static BidiMap COST_SUMMARY_MAP = new DualHashBidiMap();
	static {
		COST_SUMMARY_MAP.put(Messages.getString("Summary.None"), Integer.valueOf(NONE));
		COST_SUMMARY_MAP.put(Messages.getString("Summary.Average"), Integer.valueOf(AVERAGE));
		COST_SUMMARY_MAP.put(Messages.getString("Summary.AverageFirstSublevel"), Integer.valueOf(AVERAGE_FIRST_SUBLEVEL));
		COST_SUMMARY_MAP.put(Messages.getString("Summary.Maximum"), Integer.valueOf(MAXIMUM));
		COST_SUMMARY_MAP.put(Messages.getString("Summary.Minimum"), Integer.valueOf(MINIMUM));
		COST_SUMMARY_MAP.put(Messages.getString("Summary.Sum"), Integer.valueOf(SUM));
	}

	private static BidiMap DATE_SUMMARY_MAP = new DualHashBidiMap();
	static {
		DATE_SUMMARY_MAP.put(Messages.getString("Summary.None"), Integer.valueOf(NONE));
		DATE_SUMMARY_MAP.put(Messages.getString("Summary.Maximum"), Integer.valueOf(MAXIMUM));
		DATE_SUMMARY_MAP.put(Messages.getString("Summary.Minimum"), Integer.valueOf(MINIMUM));
	}

	private static BidiMap DURATION_SUMMARY_MAP = COST_SUMMARY_MAP;
	
	private static BidiMap FLAG_SUMMARY_MAP = new DualHashBidiMap();
	static {
		FLAG_SUMMARY_MAP.put(Messages.getString("Summary.None"), Integer.valueOf(NONE));
		FLAG_SUMMARY_MAP.put(Messages.getString("Summary.OR"), Integer.valueOf(OR));
		FLAG_SUMMARY_MAP.put(Messages.getString("Summary.AND"), Integer.valueOf(AND));
	}

	private static BidiMap NUMBER_SUMMARY_MAP = new DualHashBidiMap();
	static {
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.None"), Integer.valueOf(NONE));
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.Average"), Integer.valueOf(AVERAGE));
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.AverageFirstSublevel"), Integer.valueOf(AVERAGE_FIRST_SUBLEVEL));
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.CountAll"), Integer.valueOf(COUNT_ALL));
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.CountFirstSublevel"), Integer.valueOf(COUNT_FIRST_SUBLEVEL));
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.CountNonsummaries"), Integer.valueOf(COUNT_NONSUMMARIES));
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.Maximum"), Integer.valueOf(MAXIMUM));
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.Minimum"), Integer.valueOf(MINIMUM));
		NUMBER_SUMMARY_MAP.put(Messages.getString("Summary.Sum"), Integer.valueOf(SUM));
	}
	
	private static BidiMap TEXT_SUMMARY_MAP = new DualHashBidiMap();
	static {
		TEXT_SUMMARY_MAP.put(Messages.getString("Summary.None"), Integer.valueOf(NONE));
		TEXT_SUMMARY_MAP.put(Messages.getString("Summary.List"), Integer.valueOf(LIST));
	}
	
	private static HashMap ALL_SUMMARY_MAP = new HashMap<>();
	static {
		ALL_SUMMARY_MAP.put("None", Integer.valueOf(NONE));
		ALL_SUMMARY_MAP.put("This", Integer.valueOf(THIS));
		ALL_SUMMARY_MAP.put("List", Integer.valueOf(LIST));
		ALL_SUMMARY_MAP.put("Average", Integer.valueOf(AVERAGE));
		ALL_SUMMARY_MAP.put("AverageFirstSublevel", Integer.valueOf(AVERAGE_FIRST_SUBLEVEL));
		ALL_SUMMARY_MAP.put("CountAll", Integer.valueOf(COUNT_ALL));
		ALL_SUMMARY_MAP.put("CountFirstSublevel", Integer.valueOf(COUNT_FIRST_SUBLEVEL));
		ALL_SUMMARY_MAP.put("CountNonsummaries", Integer.valueOf(COUNT_NONSUMMARIES));
		ALL_SUMMARY_MAP.put("Maximum", Integer.valueOf(MAXIMUM));
		ALL_SUMMARY_MAP.put("Minimum", Integer.valueOf(MINIMUM));
		ALL_SUMMARY_MAP.put("Sum", Integer.valueOf(SUM));
		ALL_SUMMARY_MAP.put("OR", Integer.valueOf(OR));
		ALL_SUMMARY_MAP.put("AND", Integer.valueOf(AND));

		ALL_SUMMARY_MAP.put("Same", Integer.valueOf(SAME));
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
