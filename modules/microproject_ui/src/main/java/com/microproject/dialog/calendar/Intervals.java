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
package com.microproject.dialog.calendar;

import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jdesktop.swing.calendar.DateSpan;

import com.microproject.contrib.calendar.ContribIntervals;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.util.DateTime;

/**
 *
 */
public class Intervals extends TreeSet implements HasStartAndEnd{

	/**
	 *
	 */
	public Intervals(ContribIntervals c) {
		super(new Comparator(){
			public int compare(Object o1, Object o2) {
				HasStartAndEnd d1=(HasStartAndEnd)o1; //Only want to compare DateSpan no need to use instanceof
				HasStartAndEnd d2=(HasStartAndEnd)o2;
				if (d1.getStart()<d2.getStart()||(d1.getStart()==d2.getStart()&&d1.getEnd()<d2.getEnd())) return -1;
				else if (d1.getStart()>d2.getStart()||(d1.getStart()==d2.getStart()&&d1.getEnd()>d2.getEnd())) return 1;
				else return 0;
			}
		});
		if (c!=null)
			for (Iterator i=c.iterator();i.hasNext();){
				DateSpan d=(DateSpan)i.next();
				if (super.add(new CalendarInterval(d.getStart(),d.getEnd())));
			}
	}


	protected HasStartAndEnd createInterval(long start,long end) {
		return new CalendarInterval(start,end);
	}
	protected HasStartAndEnd mergeIntervals(HasStartAndEnd i1,HasStartAndEnd i2) {
		return new CalendarInterval(Math.min(i1.getStart(),i2.getStart()),Math.max(i1.getEnd(),i2.getEnd()));
	}


	public boolean add(Object o) {
		HasStartAndEnd toAdd=(HasStartAndEnd)o;
		for (Iterator i=iterator(); i.hasNext();) {
			HasStartAndEnd interval=(HasStartAndEnd)i.next();
			if (interval.getEnd()<toAdd.getStart()) {
				continue;
			}
			if (interval.getStart()>toAdd.getEnd()) {
				break;
			}
			toAdd = mergeIntervals(toAdd, interval);
			i.remove();
		}
		return super.add(toAdd);
	}

	public boolean addAll(Collection c) {
		if (c==null) return false;
		boolean added=false;
		for (Iterator i=c.iterator();i.hasNext();){
			if (super.add(i.next())) added=true;
		}
		return added;
	}

	public long getEnd() {
		return (size()==0)?-1:((HasStartAndEnd)last()).getEnd();
	}
	public long getStart() {
		return (size()==0)?-1:((HasStartAndEnd)first()).getStart();
	}

	public boolean containsDate(long date){
		for (Iterator i=iterator();i.hasNext();){ //a more optimized version can be found
			HasStartAndEnd interval=(HasStartAndEnd)i.next();
			if (interval.getStart()<=date&&date<=interval.getEnd()) return true;
		}
		return false;
	}

	void eliminateWeekdayDuplicates(boolean weekDays[]) {
		Calendar cal = DateTime.calendarInstance();
		for (Iterator i=iterator();i.hasNext();){ //a more optimized version can be found
			HasStartAndEnd interval=(HasStartAndEnd)i.next();
			cal.setTimeInMillis(interval.getStart());
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) -1;

			// remove any days which correspond to a selected week day
			for (int d = 0; d < 7; d++) {
				if (weekDays[d] && d == dayOfWeek) {
					i.remove();
				}
			}
		}
	}

	/*public void xorAdd(HasStartAndEnd o,Consumer<Object> removeFunctor,Consumer<Object> addFunctor){
		HasStartAndEnd toAdd=o;
		SortedSet set=headSet(o);
		if (set.size()>0){
			HasStartAndEnd previous=(HasStartAndEnd)set.last();
			if (previous.getEnd()>=toAdd.getStart()){
				remove(previous);
				if (previous.getEnd()==toAdd.getStart()){
					toAdd=createInterval(previous.getStart(),toAdd.getEnd());
				}else{
					if (previous.getStart()<toAdd.getStart())
						super.add(createInterval(previous.getStart(),toAdd.getStart()));
					removeFunctor.execute(createInterval(toAdd.getStart(),previous.getEnd()));
					toAdd=createInterval(previous.getEnd(),toAdd.getEnd());
				}
			}
		}

		set=tailSet(o);
		if (set.size()>0){
			HasStartAndEnd next=(HasStartAndEnd)set.first();
			if (next.getStart()<=toAdd.getEnd()){
				remove(next);
				if (next.getStart()==toAdd.getEnd()){
					createInterval(toAdd.getStart(),next.getEnd());
				}else{
					if (next.getEnd()>toAdd.getEnd())
						super.add(createInterval(toAdd.getEnd(),next.getEnd()));
					removeFunctor.execute(createInterval(next.getStart(),toAdd.getEnd()));
					toAdd=createInterval(toAdd.getStart(),next.getStart());
				}
			}
		}

		if (toAdd.getStart()<=toAdd.getEnd()){
			super.add(toAdd);
			addFunctor.execute(toAdd);
		}

	}*/
}

