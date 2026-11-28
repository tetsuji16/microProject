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
package com.microproject.timescale;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.microproject.util.DateTime;
import com.microproject.util.Environment;

/**
 *
 */
public class TimeScale {
	protected int minWidth;
	protected int tableMinWidth;
	protected int normalMinWidth;
	protected long minDuration;
	protected int number1;
	protected int number2;
	protected int calendarField1;
	protected int calendarField2;
	protected double ratio=-1.0;
	protected String pattern1;
	protected String pattern2;
	protected int trunc1=-1;
	protected int trunc2=-1;
	protected boolean upperCase1=false;
	protected boolean upperCase2=false;

	private final Map<String, ExtendedDateFormat> formatters = new HashMap<>();
	private Locale formatterLocale;

	/**
	 * 
	 */
	public TimeScale() {
		formatterLocale = Locale.getDefault();
	}
	
	public long floor1(long t){
		return floor(t, calendarField1, number1);
	}
	public long ceil1(long t){
		return ceil(t, calendarField1, number1);
	}
	public long floor2(long t) {
		return floor(t, calendarField2, number2);
	}
	public long ceil2(long t) {
		return ceil(t, calendarField2, number2);
	}
	private long floor(long epochMillis, int field, int number) {
		return CalendarUtil.floor(Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC), field, number)
				.toInstant().toEpochMilli();
	}
	private long ceil(long epochMillis, int field, int number) {
		long floored = floor(epochMillis, field, number);
		if (floored == epochMillis) return epochMillis;
		return CalendarUtil.increment(Instant.ofEpochMilli(floored).atZone(ZoneOffset.UTC), field, number)
				.toInstant().toEpochMilli();
	}
	public long increment1(long epochMillis){
		return CalendarUtil.increment(Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC), calendarField1, number1)
				.toInstant().toEpochMilli();
	}
	public long increment2(long epochMillis){
		return CalendarUtil.increment(Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC), calendarField2, number2)
				.toInstant().toEpochMilli();
	}
	public int getCalendarField1() {
		return calendarField1;
	}
	public int getCalendarField2() {
		return calendarField2;
	}
	public int getNumber1() {
		return number1;
	}
	public int getNumber2() {
		return number2;
	}
	
	
	
	public String getText1(long t){
		ensureFormatterLocale();
		String r=formatter(localizedPattern(pattern1)).format(t);
		if (trunc1>=0) {
			// patch for Chinese week display
			if (Environment.isChinese()) {
				int len = r.length();
				if (len>2)
					return r.substring(len-trunc1);
			}
			r=r.substring(0,trunc1);
		}
		if (upperCase1) r=r.toUpperCase(Locale.ROOT);
		return r;
	}
	public String getText2(long t){
		ensureFormatterLocale();
		String r=formatter(localizedPattern(pattern2)).format(t);
		if (trunc2>=0) r=r.substring(0,trunc2);
		if (upperCase2) r=r.toUpperCase(Locale.ROOT);
		return r;
	}

	/**
	 * Rebuilds the formatter when the application locale is changed at runtime.
	 * TimeScale instances are shared by Gantt and export views, so keeping this
	 * check here prevents one view from retaining the old language.
	 */
	private void ensureFormatterLocale() {
		Locale current = Locale.getDefault();
		if (!current.equals(formatterLocale)) {
			formatterLocale = current;
			formatters.clear();
		}
	}

	/**
	 * Returns the formatter for a pattern, built once per pattern and locale.
	 * A repaint formats many labels, so the pattern is not recompiled while the
	 * pattern and the locale stay the same.
	 */
	private ExtendedDateFormat formatter(String pattern) {
		return formatters.computeIfAbsent(String.valueOf(pattern),
				p -> DateTime.extendedUtcDateFormatInstance(p, formatterLocale));
	}

	/**
	 * The built-in scale patterns predate Japanese support and are intentionally
	 * compact English-oriented patterns. Keep custom patterns intact, but use
	 * natural compact Japanese labels for the built-in patterns.
	 */
	private String localizedPattern(String pattern) {
		if (pattern == null || !"ja".equals(formatterLocale.getLanguage())) return pattern;
		return switch (pattern) {
		case "E d MMM" -> "E d";
		case "d MMM yy" -> "M月d日 yy年";
		case "MMM y" -> "M月 y年";
		case "MMM" -> "M月";
		default -> pattern;
		};
	}
	
	
	
	public void toggleWidth(boolean normal){
		if ((minWidth==normalMinWidth)!=normal){
			if (normal) minWidth=normalMinWidth;
			else minWidth=tableMinWidth;
			updateRatio();
		}
	}
	
	public double getRatio() {
		if (ratio==-1.0) updateRatio();
		return ratio;
	}
	
	private void updateRatio(){
		minDuration=CalendarUtil.getMinDuration(calendarField1)*number1;
		ratio=((double)minWidth)/((double)minDuration);
	}
	
	public double toTime(double x){
		return x/getRatio();
	}
	
	public double toX(double t){
		return t*getRatio();
	}
	
	/**
	 * @param calendarField1 The calendarField1 to set.
	 */
	public void setCalendarField1(int calendarField1) {
		this.calendarField1 = calendarField1;
	}
	/**
	 * @param calendarField2 The calendarField2 to set.
	 */
	public void setCalendarField2(int calendarField2) {
		this.calendarField2 = calendarField2;
	}
	/**
	 * @param minWidth The minWidth to set.
	 */
	public void setNormalMinWidth(int normalMinWidth) {
		this.normalMinWidth = normalMinWidth;
		minWidth=normalMinWidth;
	}
	/**
	 * @param minWidth The minWidth to set.
	 */
	public void setNormalMinWidthChinese(int normalMinWidth) {
		if (Environment.isChinese()){ //overrides because it's called after setNormalMinWidth()
			this.normalMinWidth = normalMinWidth;
			minWidth=normalMinWidth;
		}
	}
	/**
	 * @param number1 The number1 to set.
	 */
	public void setNumber1(int number1) {
		this.number1 = number1;
	}
	/**
	 * @param number2 The number2 to set.
	 */
	public void setNumber2(int number2) {
		this.number2 = number2;
	}
	/**
	 * @param pattern1 The pattern1 to set.
	 */
	public void setPattern1(String pattern1) {
		this.pattern1 = pattern1;
	}
	/**
	 * @param pattern2 The pattern2 to set.
	 */
	public void setPattern2(String pattern2) {
		this.pattern2 = pattern2;
	}
	/**
	 * @param trunc1 The trunc1 to set.
	 */
	public void setTrunc1(int trunc1) {
		this.trunc1 = trunc1;
	}
	/**
	 * @param trunc2 The trunc2 to set.
	 */
	public void setTrunc2(int trunc2) {
		this.trunc2 = trunc2;
	}
	/**
	 * @param uppercase1 The uppercase1 to set.
	 */
	public void setUpperCase1(boolean upperCase1) {
		this.upperCase1 = upperCase1;
	}
	/**
	 * @param uppercase2 The uppercase2 to set.
	 */
	public void setUpperCase2(boolean upperCase2) {
		this.upperCase2 = upperCase2;
	}
	
	
	
	public void setTableMinWidth(int tableMinWidth) {
		this.tableMinWidth = tableMinWidth;
	}
	
	
	public Object clone(){
		TimeScale t=new TimeScale();
		t.minWidth=minWidth;
		t.tableMinWidth=tableMinWidth;
		t.normalMinWidth=normalMinWidth;
		t.minDuration=minDuration;
		t.number1=number1;
		t.number2=number2;
		t.calendarField1=calendarField1;
		t.calendarField2=calendarField2;
		t.ratio=ratio;
		t.pattern1=pattern1;
		t.pattern2=pattern2;
		t.trunc1=trunc1;
		t.trunc2=trunc2;
		t.upperCase1=upperCase1;
		t.upperCase2=upperCase2;
		return t;
	}
	
	/**
	 * Get millis between smallest intervals
	 * @return
	 */	
		public long getIntervalDuration() {
			return CalendarUtil.getMinDuration(calendarField1)*number1;
		}
	public int getMinWidth() {
		return minWidth;
	}
}
