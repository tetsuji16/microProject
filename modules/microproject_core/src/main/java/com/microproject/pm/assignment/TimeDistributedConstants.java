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
package com.microproject.pm.assignment;

import com.microproject.configuration.Configuration;
import com.microproject.strings.Messages;

public interface TimeDistributedConstants {
	public static final Object PERCENT_ALLOC = Messages.getString("Field.percentAlloc");
	public static final Object OVERALLOCATED = Messages.getString("Text.Overallocated");
	public static final Object SELECTED = Messages.getString("Text.Selected");
	public static final Object OTHER_PROJECTS = Messages.getString("Text.OtherProjects");
	public static final Object THIS_PROJECT = Messages.getString("Text.ThisProject");
	
	public static final Object AVAILABILITY = Configuration.getFieldFromId("Field.resourceAvailability");
	
	public static final Object WORK = Configuration.getFieldFromId("Field.work");
	public static final Object ACTUAL_WORK = Configuration.getFieldFromId("Field.actualWork");
	public static final Object REMAINING_WORK = Messages.getString("Field.remainingWork");	
	//TODO add overtime work
	public static final Object BASELINE_WORK = Configuration.getFieldFromId("Field.baselineWork");
	
	public static final Object COST = Configuration.getFieldFromId("Field.cost");
	public static final Object ACTUAL_COST = Configuration.getFieldFromId("Field.actualCost");
	public static final Object FIXED_COST = Configuration.getFieldFromId("Field.fixedCost");
	public static final Object ACTUAL_FIXED_COST = Configuration.getFieldFromId("Field.actualFixedCost");
	public static final Object REMAINING_COST = Messages.getString("Field.remainingCost");	
	public static final Object BASELINE_COST = Configuration.getFieldFromId("Field.baselineCost");
	public static final Object ACWP = Configuration.getFieldFromId("Field.acwp");	
	public static final Object BCWP = Configuration.getFieldFromId("Field.bcwp");
	public static final Object BCWS = Configuration.getFieldFromId("Field.bcws");
	public static final Object BASELINE1_COST = Configuration.getFieldFromId("Field.baseline1Cost");
	public static final Object BASELINE2_COST = Configuration.getFieldFromId("Field.baseline2Cost");
	public static final Object BASELINE3_COST = Configuration.getFieldFromId("Field.baseline3Cost");
	public static final Object BASELINE4_COST = Configuration.getFieldFromId("Field.baseline4Cost");
	public static final Object BASELINE5_COST = Configuration.getFieldFromId("Field.baseline5Cost");
	public static final Object BASELINE6_COST = Configuration.getFieldFromId("Field.baseline6Cost");
	public static final Object BASELINE7_COST = Configuration.getFieldFromId("Field.baseline7Cost");
	public static final Object BASELINE8_COST = Configuration.getFieldFromId("Field.baseline8Cost");
	public static final Object BASELINE9_COST = Configuration.getFieldFromId("Field.baseline9Cost");
	public static final Object BASELINE10_COST = Configuration.getFieldFromId("Field.baseline10Cost");

	public static final Object BASELINE1_WORK = Configuration.getFieldFromId("Field.baseline1Work");
	public static final Object BASELINE2_WORK = Configuration.getFieldFromId("Field.baseline2Work");
	public static final Object BASELINE3_WORK = Configuration.getFieldFromId("Field.baseline3Work");
	public static final Object BASELINE4_WORK = Configuration.getFieldFromId("Field.baseline4Work");
	public static final Object BASELINE5_WORK = Configuration.getFieldFromId("Field.baseline5Work");
	public static final Object BASELINE6_WORK = Configuration.getFieldFromId("Field.baseline6Work");
	public static final Object BASELINE7_WORK = Configuration.getFieldFromId("Field.baseline7Work");
	public static final Object BASELINE8_WORK = Configuration.getFieldFromId("Field.baseline8Work");
	public static final Object BASELINE9_WORK = Configuration.getFieldFromId("Field.baseline9Work");
	public static final Object BASELINE10_WORK = Configuration.getFieldFromId("Field.baseline10Work");

}
