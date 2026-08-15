/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
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
