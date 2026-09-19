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
package com.microproject.datatype;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.microproject.options.EditOption;
import com.microproject.options.ScheduleOption;
import com.microproject.strings.Messages;

import java.text.Format;

/**
 * Instead of creating a true Duration type, I use unused bits of the long which
 * stores durations. The idea is that the algorithms will run faster because
 * there is no object churn and fewer function calls.
 */
public class DurationFormat extends Format {
	private boolean showPlusSign = false;
	private boolean isWork = false;
	private boolean canBeNonTemporal = false;
	private static Format instance = null;
	public static Format getInstance() {
		if (instance == null)
			instance = new DurationFormat(false);
		return instance;
	}
	private static Format signedInstance = null;
	public static Format getSignedInstance() {
		if (signedInstance == null)
			signedInstance = new DurationFormat(true);
		return signedInstance;
	}
	private static Format workInstance = null;
	public static Format getWorkInstance() {
		if (workInstance == null) {
			workInstance = new DurationFormat(false);
			((DurationFormat)workInstance).isWork = true;
		}
		return workInstance;
	}
	
	private static Format nonTemporalWorkInstance = null;
	public static Format getNonTemporalWorkInstance() {
		if (nonTemporalWorkInstance == null) {
			nonTemporalWorkInstance = new DurationFormat(false);
			((DurationFormat)nonTemporalWorkInstance).isWork = true;
			((DurationFormat)nonTemporalWorkInstance).canBeNonTemporal = true;
		}
		return nonTemporalWorkInstance;
	}
	
	// these strings are themselves parts of string ids in properties file and as such must be hard coded as below
	private static String[] types = {"minute", "hour", "day", "week", "month",
			"year", "percent", "eminute", "ehour", "eday", "eweek", "emonth",
			"eyear", "epercent"};
	
	private static final int SINGULAR = 0;
	private static final int PLURAL = 1;
	private static final String multiple[] = {".singular", ".plural"};
	private static int TYPE_COUNT = types.length;
	private static int NAME_COUNT = 4;
	private static String[][][] typesArray = new String[NAME_COUNT][multiple.length][TYPE_COUNT];
	private static Pattern[] pattern = new Pattern[TYPE_COUNT];
	private static String estimatedSymbol = Messages.getString("Units.estimatedSymbol");
	
	
	//private constructor initializes values.
	private DurationFormat(boolean showPlusSign) {
		this.showPlusSign = showPlusSign;
		String estimated = Messages.getString("Units.estimatedSymbolRegex"); // Like ?
		
		// a bunch of init code which reads the possible  values for durations from localized messages
		for (int i = 0; i < TYPE_COUNT; i++) {
			String singularNames=null;
			String pluralNames=null;
			for (int j = 0; j < multiple.length; j++) {
				String names = new String(Messages.getString("Units."
						+ types[i] + multiple[j]));
				if (j==SINGULAR) singularNames=names;
				if (j==PLURAL) pluralNames=names;
				String[] units = names.split("\\|", -1); // index into the names list, getting string
				//split has a big memory cost so names are pre-splited 
				for (int k = 0; k < NAME_COUNT; k++) typesArray[k][j][i]=units[k];
			}
			// The pattern represents the following:
			// Singular type OR Plural type, optinally followed by white space and the estimated symbol (?)
			// Two groups are saved: Group 1 is the type, Group 2 is the estimated symbol
			pattern[i] = Pattern.compile("((?:" + singularNames+ ")"
					+ "|(?:" + pluralNames + "))?" 
					+ "(\\s*" + estimated + "?)");
		}
	}
	
	
	// NumberFormat/DecimalFormat is not thread-safe; always use a fresh instance
	// per call instead of a shared static one (issue #184).
	private static NumberFormat decimalFormat() {
		return NumberFormat.getNumberInstance();
	}
	
	public Object parseObject(String durationString, ParsePosition pos) {
		Object result = null;
		if (durationString.length() == 0)
			return null;
		
		if (durationString.charAt(pos.getIndex()) == '+') // if string begins with + sign, ignore it
			pos.setIndex(pos.getIndex()+1);
				
				
		Number numberResult = decimalFormat().parse(durationString, pos);
		if (numberResult == null)
			return null;
		String durationPart = durationString.substring(pos.getIndex());
		durationPart = durationPart.trim();
		Matcher matcher;
		for (int i = 0; i < TYPE_COUNT; i++) { // find hte appropriate units
			matcher = pattern[i].matcher(durationPart);
			if (matcher.matches()) {
				int timeUnit = (matcher.group(1) != null) ? i : TimeUnit.NONE; // first group is units.  If no units, then it will match, but should use default: NONE
				double value = numberResult.doubleValue();
				if (timeUnit == TimeUnit.PERCENT || timeUnit == TimeUnit.ELAPSED_PERCENT)
					value /= 100.0;
				if (timeUnit == TimeUnit.NONE && isWork) {
					if (canBeNonTemporal)
						timeUnit = TimeUnit.NON_TEMPORAL;
					else
						timeUnit = ScheduleOption.getInstance().getWorkUnit(); // use default work unit if work and nothing entered
				}
				if (timeUnit == TimeUnit.NONE)
					timeUnit = ScheduleOption.getInstance().getDurationEnteredIn();
				long longResult = Duration.getInstance(value,timeUnit);
				if (Duration.millis(longResult) > Duration.MAX_DURATION) // check for too big
					return null;
				if (matcher.group(2).length() != 0) { // second group is estimated				
					longResult = Duration.setAsEstimated(longResult,true);
				}

				result = new Duration(longResult);
				
				return result;
			}
		}
		
		return null;
	}
	public StringBuffer format(Object durationObject, StringBuffer toAppendTo, FieldPosition pos) {
		long duration = ((Duration)durationObject).getEncodedMillis();
		if (((Duration)durationObject).isWork() && Duration.getType(duration) != TimeUnit.NON_TEMPORAL) {
			duration = Duration.setAsTimeUnit(duration,ScheduleOption.getInstance().getWorkUnit());
		}
		
		double value = Duration.getValue(duration);
		int type = Duration.getEffectiveType(duration);
		if (value > 0D && showPlusSign)
			toAppendTo.append("+");
		boolean isPercent = Duration.isPercent(duration);
		if (isPercent)
			value *= 100.0;
		decimalFormat().format(value,toAppendTo,pos);
		
		String unit = formatTypeUnit(type
				,(Math.abs(value) == 1.0)
				,EditOption.getInstance().isAddSpaceBeforeLabel()
				,Duration.isPercent(duration)
				,Duration.isEstimated(duration)
		        ,EditOption.getInstance().getViewAs(type));
		toAppendTo.append(unit);
		return toAppendTo;
	}
	
	public String formatCompact(Object durationObject) {
		StringBuilder toAppendTo = new StringBuilder();
		long duration = ((Duration)durationObject).getEncodedMillis();
		if (((Duration)durationObject).isWork() && Duration.getType(duration) != TimeUnit.NON_TEMPORAL) {
			duration = Duration.setAsTimeUnit(duration,ScheduleOption.getInstance().getWorkUnit());
		}
		
		double value = Duration.getValue(duration);
		int type = Duration.getEffectiveType(duration);
		if (value > 0D && showPlusSign)
			toAppendTo.append("+");
		boolean isPercent = Duration.isPercent(duration);
		if (isPercent)
			value *= 100.0;
		toAppendTo.append(decimalFormat().format(value));
		
		String unit = formatTypeUnit(type
				,(Math.abs(value) == 1.0)
				,false
				,Duration.isPercent(duration)
				,Duration.isEstimated(duration)
				,3);
		toAppendTo.append(unit);
		return toAppendTo.toString();
	}

	public static String formatTypeUnit(int type, boolean isSingular, boolean addSpace, boolean isPercent, boolean isEstimated, int displayIndex) {
		StringBuilder toAppendTo = new StringBuilder();
		if (type == TimeUnit.NON_TEMPORAL)
			return "";
		if (addSpace && !isPercent) {
			toAppendTo.append(" ");
		}
		String unit = typesArray[displayIndex][isSingular ? SINGULAR : PLURAL][type]; // get either singular or plural names list
		toAppendTo.append(unit);
		if (isEstimated)
			toAppendTo.append(estimatedSymbol);
		return toAppendTo.toString();
	}	
	
	public static String formatTypeUnit(int type) {
		DurationFormat.getInstance(); // make sure it is initialized
		return formatTypeUnit(type,true,false,false,false,EditOption.getInstance().getViewAs(type));
	}
	
	public static String format(long millis) {
		return getInstance().format(new Duration(millis)).toString();
	}
	public static String formatCompact(long millis) {
		return ((DurationFormat)getInstance()).formatCompact(new Duration(millis)).toString();
	}
	public static String formatWork(long millis) {
		return getWorkInstance().format(new Work(millis)).toString();
	}
	public static String formatWork(Object millis) {
		if (millis!=null&&millis instanceof Long) return formatWork(((Long)millis).longValue());
		return getWorkInstance().format(millis);
	}
}
