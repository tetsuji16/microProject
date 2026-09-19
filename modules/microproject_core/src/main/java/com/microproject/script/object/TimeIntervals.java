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
package com.microproject.script.object;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

import org.apache.commons.lang.time.DateUtils;

import com.microproject.timescale.CalendarUtil;


public class TimeIntervals implements Serializable,Cloneable{
	static final long serialVersionUID = 18828392223063L;

	public static final int DAY=1;
	public static final int WEEK=2;
	public static final int MONTH=3;
	public static final int QUARTER=4;
	public static final int YEAR=5;
	public static final int ETERNITY=1000000;

	public static final int MIN_SCALE=1;
	public static final int MAX_SCALE=2;//3;

	protected static final int DEFAULT_WINDOW_COUNT=3;
	protected static final int WINDOW_INTERVALS=50;

	protected LinkedList<TimeWindow> win=new LinkedList<TimeWindow>();
	protected int scale=MIN_SCALE;
	protected int translation;
	protected int winId;
	protected float center;
	protected LinkedList<TimeWindow> history=new LinkedList<TimeWindow>();
	protected long start,end;

	public int getScale() {
		return scale;
	}
	public void setScale(int scale) {
		if (scale>=MIN_SCALE&&scale<=MAX_SCALE)
		this.scale = scale;
	}

	public LinkedList<TimeWindow> getWin() {
		return win;
	}

	public long getStart(){
		if (win.size()==0) return 0;
		else return win.getFirst().getS();
	}
	public long getEnd(){
		if (win.size()==0) return Long.MAX_VALUE;
		else return win.getLast().getE();
	}
	public TimeWindow getCenterWin(){
		int count=win.size();
		if (count==0) return null;
		int i=count/2;
		if (count%2==1) return win.get(i);
		else{
			TimeWindow t=new TimeWindow();
			t.setS(win.get(i-1).calculateCenter());
			t.setE(win.get(i).calculateCenter());
			return t;
		}

	}

	public int getTranslation() {
		return translation;
	}
	public void setTranslation(int translation) {
		this.translation = translation;
	}

	public float getCenter() {
		return center;
	}
	public void setCenter(float center) {
		this.center = center;
	}

	public void update() {
		if (win.size()>0) update(win.getFirst().getS(),end);
	}
	public void update(long start,long end) {
		update(start,end,DEFAULT_WINDOW_COUNT);
	}
	public void update(long start,long end,int winCount) { //winCount positive in this case
//		System.out.println("TimeIntervals.update");
		this.start=start;
		this.end=end;
		win.clear();
		history.clear();
		generateWindows(scale,start,0,Long.MAX_VALUE,winCount,win);
		indexWindows(winId,win);
		history.addAll(win);
	}

	protected static void indexWindows(int winId,LinkedList<TimeWindow> win){
		int i=winId;
		for (TimeWindow w:win) w.setId(i++);
	}

	public TimeIntervals translate(int windowCount) {
		TimeIntervals translated = new TimeIntervals();
		translated.setScale(scale);
		if (windowCount == 0 || win.isEmpty())
			return translated;

		int currentSize = win.size();
		int newFirstId = winId + windowCount;
		int newLastId = newFirstId + currentSize - 1;
		ensureHistoryContains(newFirstId, newLastId);

		int enteringFirstId = windowCount > 0 ? winId + currentSize : newFirstId;
		int enteringLastId = windowCount > 0 ? newLastId : winId - 1;
		translated.winId = enteringFirstId;
		translated.win.addAll(historyRange(enteringFirstId, enteringLastId));
		translated.translation = windowCount;

		win.clear();
		win.addAll(historyRange(newFirstId, newLastId));
		winId = newFirstId;
		return translated;
	}

	private void ensureHistoryContains(int firstId, int lastId) {
		while (history.getFirst().getId() > firstId) {
			LinkedList<TimeWindow> generated = new LinkedList<>();
			generateWindows(scale, history.getFirst().getS(), start, end, -1, generated);
			indexWindows(history.getFirst().getId() - 1, generated);
			history.addAll(0, generated);
		}
		while (history.getLast().getId() < lastId) {
			LinkedList<TimeWindow> generated = new LinkedList<>();
			generateWindows(scale, history.getLast().getE(), start, end, 1, generated);
			indexWindows(history.getLast().getId() + 1, generated);
			history.addAll(generated);
		}
	}

	private LinkedList<TimeWindow> historyRange(int firstId, int lastId) {
		LinkedList<TimeWindow> result = new LinkedList<>();
		for (TimeWindow window : history) {
			if (window.getId() >= firstId && window.getId() <= lastId)
				result.add(window);
		}
		return result;
	}






	public static int generateWindows(int scale, long ref,long start,long end,int winCount,LinkedList<TimeWindow> windows) {
		TimeWindow win,lastWin=null;
		if (winCount>0){
			for (int i=0;i<=winCount;i++){
				win=generateWindow(ref, scale, 1);
				//if (win.getS()>end) return i;
				if (lastWin!=null){
					lastWin.setE(win.getS());
					windows.add(lastWin);
				}
				ref=win.getE();
				lastWin=win;
			}
		}else{
			for (int i=0;i>=winCount;i--){
				win=generateWindow(ref, scale, -1);
				//if (win.getE()<start) return i;
				if (lastWin!=null){
					lastWin.setS(win.getE());
					windows.addFirst(lastWin);
				}
				ref=win.getS();
				lastWin=win;
			}
		}
		return winCount;
	}

	//not idempotent, need history to undo
	public static TimeWindow generateWindow(long start,int scale,int sign) {
		int timeType,timeType2=0,number2;
		int timeIncrement=1,timeIncrement2=1;
		switch (scale) {
		case TimeIntervals.DAY:
			timeType=Calendar.DAY_OF_MONTH;
			timeType2=Calendar.WEEK_OF_YEAR;
			break;
		case TimeIntervals.WEEK:
			timeType=Calendar.WEEK_OF_YEAR;
			timeType2=Calendar.MONTH;
			break;
		case TimeIntervals.MONTH:
			timeType=Calendar.MONTH;
			timeType2=Calendar.MONTH;
			timeIncrement2=3;
			break;
		case TimeIntervals.QUARTER:
			timeType=Calendar.MONTH;
			timeType2=Calendar.YEAR;
			timeIncrement=3;
			break;
		case TimeIntervals.YEAR:
			timeType=Calendar.YEAR;
			timeType2=Calendar.YEAR;
			break;
		default:
			return null;
		}

		Calendar c=Calendar.getInstance(DateUtils.UTC_TIME_ZONE, Locale.US);//DateTime.calendarInstance();
		c.setTimeInMillis(start);

		//adapt start
		floorCal(scale, c);
		long s1=c.getTimeInMillis();
		floorCal(scale+1, c);
		long s2=c.getTimeInMillis();

		c.setTimeInMillis(s1);
		long s;
		while ((s=c.getTimeInMillis())>=s2){ //can occur with week, month scale
			s1=s;
			c.add(timeType, -timeIncrement);
		}

		//set approximative end
		c.setTimeInMillis(s1);
		c.add(timeType,sign*timeIncrement*WINDOW_INTERVALS);
		TimeWindow win=new TimeWindow();
		if (sign>0) win.setS(s1);
		else win.setE(s1);
		if (sign>0) win.setE(c.getTimeInMillis());
		else win.setS(c.getTimeInMillis());
		return win;
	}



	private static void floorCal(int scale,Calendar c){
		switch (scale) {
		case TimeIntervals.DAY:
			CalendarUtil.dayFloor(c);
			break;
		case TimeIntervals.WEEK:
			CalendarUtil.weekFloor(c);
			break;
		case TimeIntervals.MONTH:
			CalendarUtil.monthFloor(c);
			break;
		case TimeIntervals.QUARTER:
			CalendarUtil.monthFloor(c);
			c.set(Calendar.MONTH,(c.get(Calendar.MONTH)/3)*3);
			break;
		case TimeIntervals.YEAR:
			CalendarUtil.yearFloor(c);
			break;
		}
	}


	public Object clone(){
		try {
			TimeIntervals t=(TimeIntervals)super.clone();
			t.win=new LinkedList<TimeWindow>();
			t.history=new LinkedList<TimeWindow>();
			for (TimeWindow w:win) t.win.add(w);
			for (TimeWindow w:history) t.history.add(w);
			return t;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}

	}


}
