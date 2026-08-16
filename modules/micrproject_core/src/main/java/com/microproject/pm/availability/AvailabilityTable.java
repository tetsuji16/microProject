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
package com.microproject.pm.availability;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import com.microproject.field.FieldContext;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.interval.InvalidValueObjectForIntervalException;
import com.microproject.interval.ValueObjectForInterval;
import com.microproject.interval.ValueObjectForIntervalTable;
import com.microproject.undo.DataFactoryUndoController;

public class AvailabilityTable extends ValueObjectForIntervalTable implements HasAvailability, Serializable {
	static final long serialVersionUID = 56638382299384L;
	public AvailabilityTable(String name) {
		super(name);
//		initUndo();
	}
	public AvailabilityTable() {
		super();
//		initUndo();
	}
	
	public String getCategory() {
		return "availability";
	}

	protected ValueObjectForInterval createValueObject(long date) {
		return new Availability(this,date);
	}
	
	public double getMaximumUnits() {
		return ((Availability)findCurrent()).getMaximumUnits();
	}
	public void setMaximumUnits(double maxUnits) {
		((Availability)findCurrent()).setMaximumUnits(maxUnits);
	}

	public long getAvailableFrom() {
		return ((Availability)findCurrent()).getAvailableFrom();
	}
	public long getAvailableTo() {
		return ((Availability)findCurrent()).getAvailableTo();
	}

	public void setAvailableFrom(long availableFrom) throws InvalidValueObjectForIntervalException {
		((Availability)findCurrent()).setAvailableFrom(availableFrom);
	}
	public void setAvailableTo(long availableTo) {
		((Availability)findCurrent()).setAvailableTo(availableTo);
		
	}

	public boolean isReadOnlyAvailableFrom(FieldContext fieldContext) {
		return false;
	}

	public boolean isReadOnlyAvailableTo(FieldContext fieldContext) {
		return false;
	}
	
	public static AvailabilityTable deserialize(ObjectInputStream s) throws IOException, ClassNotFoundException  {
		return (AvailabilityTable)deserialize(s,new AvailabilityTable(null));
	}
	public boolean fieldHideMaximumUnits(FieldContext fieldContext) {
		return false;
	}
	
	protected boolean isGroupDirty=false;
	public final boolean isGroupDirty() {
		return isGroupDirty;
	}
	public final void setGroupDirty(boolean isGroupDirty) {
		this.isGroupDirty = isGroupDirty;
	}
	
	//Undo
	protected transient DataFactoryUndoController undoController;
	protected void initUndo(){
		undoController=new DataFactoryUndoController(this);
	}
	public DataFactoryUndoController getUndoController() {
		return undoController;
	}

	public void initOutline(NodeModel nodeModel){
		
	}
	
	public boolean containsAssignments(){return false;}
}
