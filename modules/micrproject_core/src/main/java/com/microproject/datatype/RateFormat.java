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
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;

import com.microproject.configuration.Settings;
import com.microproject.options.ScheduleOption;
import com.microproject.util.ClassUtils;

/**
 *
 */
public class RateFormat extends Format implements TimeUnit {
	// NumberFormat/DecimalFormat is not thread-safe; always use a fresh instance
	// per call instead of a shared static one (issue #184).
	private static NumberFormat moneyFormat() {
		return Money.getMoneyFormatInstance(); // creates a new instance per call
	}
	private static NumberFormat numberFormat() {
		return NumberFormat.getNumberInstance();
	}
	private static NumberFormat percentFormat() {
		return NumberFormat.getPercentInstance();
	}

	private static RateFormat moneyInstance = null;
	private static RateFormat instance = null;
	private static RateFormat percentInstance = null;
	private static RateFormat nonTemporalInstance = null;
	private String timeUnitLabel;
	private boolean money;
	public boolean percent;
	private boolean temporal;
	public static RateFormat getInstance(Object object, boolean money, boolean percent, boolean temporal) {
		if (instance == null) {
			instance = new RateFormat(null,false,false,true);
			moneyInstance = new RateFormat(null,true,false,true);
			percentInstance = new RateFormat(null,false,true,true);
			nonTemporalInstance = new RateFormat(null,false,false,false);
		}
		String timeUnit = unitLabelOfObject(object);
		if (percent)
			return percentInstance;
		if (timeUnit != null)
			return new RateFormat(timeUnit, money,false,temporal);
		if (money)
			return moneyInstance;
		else if (temporal)
			return instance;
		else
			return nonTemporalInstance;
	}
	
	public static String unitLabelOfObject(Object object) {
		String result = null;
		if (object != null) {
			if (object instanceof String)
				result = (String)object;
			else if (object instanceof CanSupplyRateUnit) {
				result = ((CanSupplyRateUnit)object).getTimeUnitLabel();
				if (result == null)
					result = "";
			}
		}
		return result;
	}
	/**
	 * 
	 */
	private RateFormat(String timeUnitLabel, boolean money, boolean percent, boolean temporal) {
		super();
		this.money = money;
		this.percent = percent;
		this.timeUnitLabel = timeUnitLabel;
		this.temporal = temporal;
	}
	public Object parseObject(String rateString, ParsePosition pos) {
		if (rateString.length() == 0)
			return null;
		
		if (rateString.charAt(pos.getIndex()) == '+') // if string begins with + sign, ignore it
			pos.setIndex(pos.getIndex()+1);
				
		Number numberResult = null;
		if (percent) {
			numberResult = percentFormat().parse(rateString,pos);
			if (numberResult == null) {
				numberResult = numberFormat().parse(rateString,pos);
				if (numberResult != null)
					numberResult = Double.valueOf(numberResult.doubleValue() / 100.0D);
			}
			if (numberResult == null)
				return null;
			return new Rate(numberResult.doubleValue(),TimeUnit.PERCENT);
		}

		if (money) 
			numberResult = moneyFormat().parse(rateString, pos);
		if (numberResult == null)
			numberResult = numberFormat().parse(rateString, pos);
		if (numberResult == null)
			return null;
		double rate = numberResult.doubleValue();
		String durationPart = rateString.substring(pos.getIndex());
		durationPart = durationPart.trim();
		
		// at this point, we have the number and are now focusing on the suffix
		int type = TimeUnit.NON_TEMPORAL;
		int slashIndex = durationPart.indexOf(Settings.SLASH);
		if (slashIndex == -1) { // if no slash
			if (timeUnitLabel == null && temporal) // temporal types use default
				type = getDefaultType();
		} else {
			durationPart = "1" + durationPart.substring(slashIndex+1,durationPart.length());// replace the slash with a 1
			Duration duration = (Duration) DurationFormat.getInstance().parseObject(durationPart, new ParsePosition(0));
			if (duration == null)
				return null;
			type = Duration.getEffectiveType(duration.getEncodedMillis());
		}
		rate /= Duration.timeUnitFactor(type);
		return new Rate(rate,type);
	}
	public StringBuffer format(Object rateObject, StringBuffer toAppendTo, FieldPosition pos) {

		if (rateObject == null)
			return toAppendTo;
		else if (rateObject == ClassUtils.defaultRate)
			return toAppendTo; // do nothing
		else if (rateObject == ClassUtils.defaultUnitlessRate)
			return toAppendTo;
		
		Rate rate = (Rate)rateObject;
		double rateValue = rate.getValue();
		int type = rate.getTimeUnit();
		if (type == TimeUnit.NONE && temporal) // if no unit, use default
			type = getDefaultType();

		if (percent) {
			percentFormat().format(Double.valueOf(rateValue),toAppendTo,pos);
		} else {
			rateValue *= Duration.timeUnitFactor(type);
			if (money) {
				moneyFormat().format(Double.valueOf(rateValue),toAppendTo,pos);
			} else {
				NumberFormat.getInstance().format(Double.valueOf(rateValue),toAppendTo,pos);
				if (timeUnitLabel != null && !timeUnitLabel.equals(""))
					toAppendTo.append(" " + timeUnitLabel);
			}
	
			if (type != TimeUnit.NON_TEMPORAL) { // if value is expressed in duration
				toAppendTo.append(Settings.SLASH);
				String unit = DurationFormat.formatTypeUnit(type);
				toAppendTo.append(unit);
			} 
		}
		return toAppendTo;
	}
	
	private int getDefaultType() {
		return ScheduleOption.getInstance().getRateEnteredIn();
	}
}
