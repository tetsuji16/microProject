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
package com.microproject.pm.criticalpath;

import java.util.HashSet;

import com.microproject.document.Document;
import com.microproject.field.*;

/**
 * Manages list of fields that cause update of the Critical Path
 */
public class CriticalPathFields extends AlgorithmFieldUpdater {
	public void run() {
		super.run();
		
	}
	protected static volatile HashSet staticInputFields;
	protected static volatile HashSet staticOutputFields;
	
	static CriticalPathFields getInstance(Object eventSource, Document document) {
		return new CriticalPathFields(eventSource, document);
	}
	
	/**
	 * Build list of fields that influence the critical path
	 */
	private CriticalPathFields(Object eventSource, Document document) {
		super(eventSource, document);
		if (staticInputFields == null) {
			synchronized (CriticalPathFields.class) {
				if (staticInputFields == null) {
					inputFields = new HashSet(32);
					outputFields = new HashSet(8);
					init();
					staticInputFields = inputFields;
					staticOutputFields = outputFields;
				}
			}
		}
		inputFields = staticInputFields;
		outputFields = staticOutputFields;
		
	}
	private void init() {
		addInputField("Field.finish");
		addInputField("Field.start");
		addInputField("Field.stop");		
		addInputField("Field.resume");
		addInputField("Field.duration");
		addInputField("Field.actualFinish");
		addInputField("Field.actualStart");
		addInputField("Field.actualDuration");
		addInputField("Field.constraintDate");		
		addInputField("Field.levelingDelay");		
		addInputField("Field.remainingDuration");
		addInputField("Field.baseCalendar");
		addInputField("Field.constraintType");
		addInputField("Field.taskCalendar");		
		addInputField("Field.effortDriven");
		addInputField("Field.ignoreResourceCalendar");
		addInputField("Field.dependencyType");		
		addInputField("Field.constraintDate");
		addInputField("Field.lag");
		addInputField("Field.predecessors");		
		addInputField("Field.successors");	
		addInputField("Field.uniqueIdSuccessors");		
		addInputField("Field.uniqueIdPredecessors");
		addInputField("Field.work");	
		addInputField("Field.assignmentEntryRate");
		addInputField("Field.resourceNames");
		//Field.overtimeRate
/**		
		addOutputField("Field.end");
		addOutputField("Field.start");
		addOutputField("Field.duration");
		addOutputField("Field.actualFinish");
		addOutputField("Field.actualStart");
		addOutputField("Field.actualDuration");
		addOutputField("Field.remainingDuration");

		addOutputField("Field.cost");
		addOutputField("Field.actualCost");		
		addOutputField("Field.bcws");
		addOutputField("Field.cumulativeCost");
		addOutputField("Field.eac");
		addOutputField("Field.remainingCost");
		addOutputField("Field.remainingOvertimeCost");
		addOutputField("Field.earlyFinish");
		addOutputField("Field.earlyStart");
		addOutputField("Field.lateFinish");
		addOutputField("Field.lateStart");
		addOutputField("Field.finishSlack");
		addOutputField("Field.freeSlack");
		addOutputField("Field.negativeSlack");
		addOutputField("Field.startSlack");
		addOutputField("Field.totalSlack");
		addOutputField("Field.critical");
		addOutputField("Field.overallocated");
		addOutputField("Field.predecessors");		
		addOutputField("Field.successors");		
		addOutputField("Field.uniqueIdSuccessors");		
		addOutputField("Field.uniqueIdPredecessors");
		addOutputField("Field.wbsSuccessors");		
		addOutputField("Field.wbsPredecessors");	
		
*/		
	}
	
	
}
