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
package com.microproject.pm.costing;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import com.microproject.configuration.Settings;
import com.microproject.datatype.Rate;
import com.microproject.field.FieldContext;
import com.microproject.interval.InvalidValueObjectForIntervalException;

/**
 * 
 */
public class CostRateTables implements Cost, Serializable, Cloneable {
	public static final int DEFAULT = 0;
	protected CostRateTable[] costRateTableArray;
	String[] names = null;
	
	String getName(int index) {
		if (names == null) {
			names = Settings.COST_RATE_NAMES.split(";", -1);
		}
		return names[index];
	}
	public CostRateTable getCostRateTable(int index) {
		if (costRateTableArray[index] == null)
			costRateTableArray[index] = new CostRateTable(getName(index));
		return costRateTableArray[index];
	}
	
	public Object clone(){ 
		try {
			CostRateTables c=(CostRateTables)super.clone();
			if (names!=null) c.names=new String[names.length];
			else for (int i=0;i<names.length;i++){
				c.names[i]=(names[i]==null)?null:new String(names[i]);
			}
			if (costRateTableArray!=null){
				c.costRateTableArray=new CostRateTable[costRateTableArray.length];
				for (int i=0;i<costRateTableArray.length;i++){
					c.costRateTableArray[i]=(costRateTableArray[i]==null)?null:(CostRateTable)costRateTableArray[i].clone();
					if (c.costRateTableArray[i]!=null) c.costRateTableArray[i].initAfterCloning();
				}
			}
			return c;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	
	public void setCostRateTable(int index, CostRateTable t) {
		costRateTableArray[index] = t;
	}
	
	private CostRate getCurrent() {
		return (CostRate)costRateTableArray[DEFAULT].findCurrent();
	}
	public double getCostPerUse() {
		return getCurrent().getCostPerUse();
	}

	public Rate getOvertimeRate() {
		return getCurrent().getOvertimeRate();
	}

	public Rate getStandardRate() {
		return getCurrent().getStandardRate();
	}



	public void setCostPerUse(double costPerUse) {
		getCurrent().setCostPerUse(costPerUse);		
	}



	public void setOvertimeRate(Rate overtimeRate) {
		getCurrent().setOvertimeRate(overtimeRate);		
	}



	public void setStandardRate(Rate standardRate) {
		getCurrent().setStandardRate(standardRate);
	}
	
	
	/**
	 * 
	 * 
	 */
	public CostRateTables() {
		super();
		costRateTableArray = new CostRateTable[Settings.NUM_COST_RATES]; // initialize array
		costRateTableArray[DEFAULT] = new CostRateTable(getName(DEFAULT)); //add default element
//		java.util.GregorianCalendar start1 = new java.util.GregorianCalendar(2003,java.util.GregorianCalendar.JANUARY,4,0,0);
//		java.util.GregorianCalendar start2 = new java.util.GregorianCalendar(2005,java.util.GregorianCalendar.JANUARY,7,0,0);	
//		try {
//			CostRate test;
//			test = costRateTableArray[DEFAULT].newRate(start1.getTimeInMillis());
//			test.setStandardRate(100.0/(1000*60*60*8));
//			test.setOvertimeRate(110.0/(1000*60*60*8));
//			test.setCostPerUse(450);
//			test = costRateTableArray[DEFAULT].newRate(start2.getTimeInMillis());
//			test.setStandardRate(13);
//			test.setOvertimeRate(1300);
	}
	public long getEffectiveDate() {
		return getCurrent().getEffectiveDate();
	}
	public void setEffectiveDate(long effectiveDate) throws InvalidValueObjectForIntervalException {
		getCurrent().setEffectiveDate(effectiveDate);
	}
	public boolean isReadOnlyEffectiveDate(FieldContext fieldContext) {
		return getCurrent().isReadOnlyEffectiveDate(fieldContext);
	}
	
	public void serialize(ObjectOutputStream s) throws IOException {
	    s.writeObject(names);
	    
	    ArrayList[] costRates=new ArrayList[costRateTableArray.length];
	    for (int i=0;i<costRates.length;i++){
	    	costRates[i]=(costRateTableArray[i]==null)?null:costRateTableArray[i].getValueObjects();
	    }
	    s.writeObject(costRates);
	}
	
	public static CostRateTables deserialize(ObjectInputStream s) throws IOException, ClassNotFoundException  {
		CostRateTables t=new CostRateTables();
		t.names=(String[])s.readObject();
		ArrayList[] costRates=(ArrayList[])s.readObject();
		t.costRateTableArray=new CostRateTable[costRates.length];
	    for (int i=0;i<costRates.length;i++){
	    	t.costRateTableArray[i]=(costRates[i]==null)?null:new CostRateTable(t.names[i],costRates[i]);
	    }
		return t;
	}
	public boolean fieldHideOvertimeRate(FieldContext fieldContext) {
		return getCurrent().fieldHideOvertimeRate(fieldContext);
	}


}
