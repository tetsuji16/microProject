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
package com.microproject.pm.resource;

import com.microproject.datatype.CanSupplyRateUnit;
import com.microproject.datatype.RateFormat;
import com.microproject.field.CustomFields;
import com.microproject.grouping.core.hierarchy.BelongsToHierarchy;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.assignment.HasTimeDistributedData;
import com.microproject.pm.assignment.TimeDistributedFields;
import com.microproject.pm.assignment.timesheet.UpdatesFromTimesheet;
import com.microproject.pm.availability.AvailabilityTable;
import com.microproject.pm.calendar.HasBaseCalendar;
import com.microproject.pm.calendar.HasCalendar;
import com.microproject.pm.costing.Cost;
import com.microproject.pm.costing.CostRateTable;
import com.microproject.pm.costing.EarnedValueFields;
import com.microproject.pm.costing.EarnedValueValues;
import com.microproject.pm.key.HasKey;
import com.microproject.pm.task.BelongsToDocument;
import com.microproject.pm.task.HasNotes;
import com.microproject.server.data.DataObject;


/**
 * @stereotype thing 
 */
public interface Resource extends HasCalendar, HasKey, BelongsToDocument, Cost, HasAssignments, ResourceSpecificFields, HasTimeDistributedData, TimeDistributedFields,EarnedValueValues, EarnedValueFields, HasNotes, HasBaseCalendar, DataObject, CanSupplyRateUnit,Cloneable, BelongsToHierarchy, CustomFields, UpdatesFromTimesheet {
    /**
     * @associates <{com.microproject.pm.resource.ResourceType}> 
     */
    public int getResourceType();
    public void setResourceType(int resourceType);
    public int getAccrueAt();
    public void setAccrueAt(int accrueAt);
    
    public CostRateTable getCostRateTable(int costRateIndex);
    public void addAssignment(Assignment assignment);
    public void removeAssignment(Assignment assignment);
    public double getMaximumUnits();
    /**
     * @link aggregation
     * @supplierCardinality 0..* 
     */
    /*# com.microproject.pm.assignment.Assignment lnkAssignment; */
    /*# com.microproject.pm.costing.Accrual lnkAccrual; */
	public AvailabilityTable getAvailabilityTable();
	public RateFormat getRateFormat(); 
	public boolean isAssignment(); // for formulas
	public String getResourceName(); // for formulas
	public boolean isUser();
	boolean isAssignedToSomeProject();

}
