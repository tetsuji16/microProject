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
package com.microproject.interval;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;

/**
 *
 */
public abstract class ValueObjectForIntervalTable implements NodeModelDataFactory, Serializable, Cloneable {
	static final long serialVersionUID = 7728399282882L;
	private static final Logger logger = Logger.getLogger(ValueObjectForIntervalTable.class.getName());
	protected ArrayList valueObjects = new ArrayList();
	protected String name;
	public List getList() {
		return Collections.unmodifiableList(valueObjects);
	}
	public ArrayList getValueObjects(){ //serialization
		return valueObjects;
	}
	public ValueObjectForIntervalTable() {
		
	}
	public ValueObjectForIntervalTable(String name, ArrayList valueObjects) { //serialization
		this.name=name;
		this.valueObjects=valueObjects;
	}
	public ValueObjectForIntervalTable(String name) {
		this.name = name;
		valueObjects.add(createValueObject(ValueObjectForInterval.NA_TIME)); // put in default one
	}
	
	protected abstract ValueObjectForInterval createValueObject(long date);
	
	/**
	 * A factory method returning a new value at a given date
	 * @param start
	 * @return
	 * @throws InvalidValueObjectForIntervalException
	 */	
	public ValueObjectForInterval newValueObject(long start) throws InvalidValueObjectForIntervalException {
		ValueObjectForInterval newOne = createValueObject(start);

		int index = Collections.binarySearch(valueObjects, newOne, newOne); // find where to insert
		if (index < 0) { // if doesn't already exist
			ValueObjectForInterval previous = (ValueObjectForInterval)valueObjects.get(-index-2); // get previous element
			valueObjects.add(-index-1, newOne); // add new in place
			newOne.setEnd( previous.getEnd()); //set new one's end to prevous end
			previous.setEnd(start); // set previous end to this start
		} else { // not allowed to make duplicate, so send back error
			throw new InvalidValueObjectForIntervalException(Messages.getString("ValueObjectForIntervalTable.ThatEffectiveDateIsAlreadyInTheTable")); //$NON-NLS-1$
		}
		return newOne;
	}
	
	public long getEnd() {
		long end = 0;
		Iterator i = valueObjects.iterator();
		while (i.hasNext()) {
			end = Math.max(end,((ValueObjectForInterval)i.next()).getEnd());
		}
		return end;
	}
	
	/**
	 * Adjust the start date of a value object.  Assure that it is in valid range, and adjust previous element's end as well as this one's start
	 * @param newStart
	 * @param valueObject
	 * @throws InvalidValueObjectForIntervalException
	 */
	public void adjustStart(long newStart, ValueObjectForInterval valueObject) throws InvalidValueObjectForIntervalException  {
		int index = valueObjects.indexOf(valueObject);
		if (index == 0)
			return;
		ValueObjectForInterval previous = (ValueObjectForInterval) valueObjects.get(index -1);
		if (newStart <= previous.getStart())
			throw new InvalidValueObjectForIntervalException(Messages.getString("ValueObjectForIntervalTable.ThisDateMustBeAfter")); //$NON-NLS-1$
		if (newStart >= valueObject.getEnd()) // see if this would disappear
			throw new InvalidValueObjectForIntervalException(Messages.getString("ValueObjectForIntervalTable.ThisDateMustBeBefore")); //$NON-NLS-1$
				
		previous.setEnd(newStart);
		valueObject.setStart(newStart);
	}
	
	
	public long getStart() {
		long start = DateTime.getMaxDate().getTime();
		Iterator i = valueObjects.iterator();
		while (i.hasNext()) {
			start = Math.min(start,((ValueObjectForInterval)i.next()).getStart());
		}
		return start;
	}
		
/**
 * Remove an entry from the table
 * @param interval object
 * @throws InvalidValueObjectForIntervalException if it's the first element
 */
	public void remove(ValueObjectForInterval removeMe) throws InvalidValueObjectForIntervalException {
		if (removeMe.isFirst()) // don't allow removal of first value
			throw new InvalidValueObjectForIntervalException(Messages.getString("ValueObjectForIntervalTable.YouCannotRemoveTheFirst"));			 //$NON-NLS-1$
		int index = valueObjects.indexOf(removeMe);
		ValueObjectForInterval previous = (ValueObjectForInterval) valueObjects.get(index-1); // set previous end to this end
		previous.setEnd(removeMe.getEnd());
		valueObjects.remove(removeMe);
	}
	
	
	private int findActiveIndex(long date) {
		ValueObjectForInterval find = createValueObject(date);		
		int index = Collections.binarySearch(valueObjects, find,find); // find it
		if (index < 0) // binary search is weird.  The element before is -index - 2
			index = -index-2; // gets index of element before
		return index;
	}
	
	/**
	 * Finds the Rate/Availability which is on or before a date
	 * @param date
	 * @return null when the table is empty or the date precedes the first entry
	 *         (callers such as ResourceAvailabilityFunctor already handle null)
	 */
	public ValueObjectForInterval findActive(long date) {
		int index = findActiveIndex(date);
		if (index < 0 || index >= valueObjects.size()) // issue #167: get(-1) used to throw
			return null;
		return (ValueObjectForInterval) valueObjects.get(index);
	}
	
	public ValueObjectForInterval findCurrent() {
		return findActive(System.currentTimeMillis());
	}
	public String getName() {
		return name;
	}

	/**
	 * Create a new entry one year later
	 */		
	public Object createUnvalidatedObject(NodeModel nodeModel, Object parent) {
		long baseDate = DateTime.midnightToday();
		ValueObjectForInterval last = (ValueObjectForInterval) valueObjects.get(valueObjects.size()-1); // get last one
		baseDate = Math.max(baseDate,last.getStart()); // latest of today or last entry
		GregorianCalendar cal = DateTime.calendarInstance();
		cal.setTimeInMillis(baseDate);
		cal.roll(GregorianCalendar.YEAR,true); // one year later than last one's start or today
		long date = cal.getTimeInMillis();
		try {
			return newValueObject(date);
		} catch (InvalidValueObjectForIntervalException e) {
			logger.log(Level.WARNING, "Failed to create unvalidated interval value object", e);
			return null;
		}
	}
	public void addUnvalidatedObject(Object object,NodeModel nodeModel, Object parent) {
		
	}
	public NodeModelDataFactory getFactoryToUseForChildOfParent(Object impl) {
		return this;
	}
	
	
	public void rollbackUnvalidated(NodeModel nodeModel, Object object) {
			remove(object,nodeModel,false,true,true);
	}


	public void remove(Object toRemove, NodeModel nodeModel,boolean deep,boolean undo,boolean removeDependencies){
		try {
			remove((ValueObjectForInterval) toRemove);
		} catch (InvalidValueObjectForIntervalException e) {
			return;
//			Alert.error(e.getMessage());
//			throw new NodeException(e);
		}

	}
	public void validateObject(Object newlyCreated, NodeModel nodeModel,
		Object eventSource, Object hierarchyInfo,boolean isNew) {
	}
//	public void fireCreated(Object newlyCreated){}

	public void serialize(ObjectOutputStream s) throws IOException {
	    s.writeObject(name);
	    s.writeObject(valueObjects);
	}
	
	
	protected static ValueObjectForIntervalTable deserialize(ObjectInputStream s,ValueObjectForIntervalTable v) throws IOException, ClassNotFoundException  {
		v.name=(String)s.readObject();
		v.valueObjects=(ArrayList) s.readObject();
		return v;
	}
	
	public Object clone(){ 
		try {
			ValueObjectForIntervalTable v=(ValueObjectForIntervalTable)super.clone();
			v.name=(name==null)?null:new String(name);
			ArrayList newList = new ArrayList();
			for (Iterator i=valueObjects.iterator();i.hasNext();){
				newList.add(((ValueObjectForInterval)i.next()).clone());
			}
			v.valueObjects=newList;
			return v;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	public void initAfterCloning(){
		for (Iterator i=valueObjects.iterator();i.hasNext();){
			((ValueObjectForInterval)i.next()).setTable(this);
		}
		
	}

}
