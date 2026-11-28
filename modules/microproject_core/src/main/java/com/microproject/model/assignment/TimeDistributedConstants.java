/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.assignment;

import com.microproject.configuration.Configuration;
import com.microproject.i18n.Messages;

/**
 * Stable field identifiers and localized labels used by time-distributed assignment views.
 * Configuration-backed values must continue to resolve through their existing field IDs.
 */
public interface TimeDistributedConstants {
	/** Localized label for allocation percentage values. */
	public static final Object PERCENT_ALLOC = Messages.getString("Field.percentAlloc");
	/** Localized marker for an overallocated period. */
	public static final Object OVERALLOCATED = Messages.getString("Text.Overallocated");
	/** Localized marker for the selected project. */
	public static final Object SELECTED = Messages.getString("Text.Selected");
	/** Localized marker for values belonging to other projects. */
	public static final Object OTHER_PROJECTS = Messages.getString("Text.OtherProjects");
	/** Localized marker for values belonging to this project. */
	public static final Object THIS_PROJECT = Messages.getString("Text.ThisProject");
	
	/** Resource availability field identifier. */
	public static final Object AVAILABILITY = Configuration.getFieldFromId("Field.resourceAvailability");
	
	/** Assignment work field identifier. */
	public static final Object WORK = Configuration.getFieldFromId("Field.work");
	/** Actual assignment work field identifier. */
	public static final Object ACTUAL_WORK = Configuration.getFieldFromId("Field.actualWork");
	/** Localized label for remaining work. */
	public static final Object REMAINING_WORK = Messages.getString("Field.remainingWork");	
	//TODO add overtime work
	/** Baseline assignment work field identifier. */
	public static final Object BASELINE_WORK = Configuration.getFieldFromId("Field.baselineWork");
	
	/** Assignment cost field identifier. */
	public static final Object COST = Configuration.getFieldFromId("Field.cost");
	/** Actual assignment cost field identifier. */
	public static final Object ACTUAL_COST = Configuration.getFieldFromId("Field.actualCost");
	/** Fixed assignment cost field identifier. */
	public static final Object FIXED_COST = Configuration.getFieldFromId("Field.fixedCost");
	/** Actual fixed assignment cost field identifier. */
	public static final Object ACTUAL_FIXED_COST = Configuration.getFieldFromId("Field.actualFixedCost");
	/** Localized label for remaining cost. */
	public static final Object REMAINING_COST = Messages.getString("Field.remainingCost");	
	/** Baseline assignment cost field identifier. */
	public static final Object BASELINE_COST = Configuration.getFieldFromId("Field.baselineCost");
	/** Actual Cost of Work Performed field identifier. */
	public static final Object ACWP = Configuration.getFieldFromId("Field.acwp");	
	/** Budgeted Cost of Work Performed field identifier. */
	public static final Object BCWP = Configuration.getFieldFromId("Field.bcwp");
	/** Budgeted Cost of Work Scheduled field identifier. */
	public static final Object BCWS = Configuration.getFieldFromId("Field.bcws");
	/** First baseline assignment cost field identifier. */
	public static final Object BASELINE1_COST = Configuration.getFieldFromId("Field.baseline1Cost");
	/** Second baseline assignment cost field identifier. */
	public static final Object BASELINE2_COST = Configuration.getFieldFromId("Field.baseline2Cost");
	/** Third baseline assignment cost field identifier. */
	public static final Object BASELINE3_COST = Configuration.getFieldFromId("Field.baseline3Cost");
	/** Fourth baseline assignment cost field identifier. */
	public static final Object BASELINE4_COST = Configuration.getFieldFromId("Field.baseline4Cost");
	/** Fifth baseline assignment cost field identifier. */
	public static final Object BASELINE5_COST = Configuration.getFieldFromId("Field.baseline5Cost");
	/** Sixth baseline assignment cost field identifier. */
	public static final Object BASELINE6_COST = Configuration.getFieldFromId("Field.baseline6Cost");
	/** Seventh baseline assignment cost field identifier. */
	public static final Object BASELINE7_COST = Configuration.getFieldFromId("Field.baseline7Cost");
	/** Eighth baseline assignment cost field identifier. */
	public static final Object BASELINE8_COST = Configuration.getFieldFromId("Field.baseline8Cost");
	/** Ninth baseline assignment cost field identifier. */
	public static final Object BASELINE9_COST = Configuration.getFieldFromId("Field.baseline9Cost");
	/** Tenth baseline assignment cost field identifier. */
	public static final Object BASELINE10_COST = Configuration.getFieldFromId("Field.baseline10Cost");

	/** First baseline assignment work field identifier. */
	public static final Object BASELINE1_WORK = Configuration.getFieldFromId("Field.baseline1Work");
	/** Second baseline assignment work field identifier. */
	public static final Object BASELINE2_WORK = Configuration.getFieldFromId("Field.baseline2Work");
	/** Third baseline assignment work field identifier. */
	public static final Object BASELINE3_WORK = Configuration.getFieldFromId("Field.baseline3Work");
	/** Fourth baseline assignment work field identifier. */
	public static final Object BASELINE4_WORK = Configuration.getFieldFromId("Field.baseline4Work");
	/** Fifth baseline assignment work field identifier. */
	public static final Object BASELINE5_WORK = Configuration.getFieldFromId("Field.baseline5Work");
	/** Sixth baseline assignment work field identifier. */
	public static final Object BASELINE6_WORK = Configuration.getFieldFromId("Field.baseline6Work");
	/** Seventh baseline assignment work field identifier. */
	public static final Object BASELINE7_WORK = Configuration.getFieldFromId("Field.baseline7Work");
	/** Eighth baseline assignment work field identifier. */
	public static final Object BASELINE8_WORK = Configuration.getFieldFromId("Field.baseline8Work");
	/** Ninth baseline assignment work field identifier. */
	public static final Object BASELINE9_WORK = Configuration.getFieldFromId("Field.baseline9Work");
	/** Tenth baseline assignment work field identifier. */
	public static final Object BASELINE10_WORK = Configuration.getFieldFromId("Field.baseline10Work");

}
