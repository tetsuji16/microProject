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

import com.microproject.configuration.CalculationPreference;
import com.microproject.datatype.ImageLink;
import com.microproject.pm.calendar.HasCalendar;
import com.microproject.pm.snapshot.BaselineScheduleFields;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;

/**
 * Implements the earned value calculation algorithms.  Currently, standard calculations
 * are implemented, with TCPI being calculated like Project 2003 (it was different in earlier versions).
 * Other instances can be created to implement variaions, such as Primavera's
 */
public class EarnedValueCalculator {
	private static final long defaultStart = 0;
	private static final long defaultEnd = DateTime.getMaxDate().getTime();
	private static EarnedValueCalculator instance = null;
	
	private double getDivideByZeroValue() {
		return CalculationPreference.getActive().getEarnedValueDivideByZeroValue();
	}
	public double acwp(EarnedValueValues ev) {
		return ev.acwp(defaultStart,defaultEnd); 
	}
	public double bac(EarnedValueValues ev) {
		return ev.bac(defaultStart,defaultEnd); 
	}
	public double bcwp(EarnedValueValues ev) {
		return ev.acwp(defaultStart,defaultEnd); 
	}
	public double bcws(EarnedValueValues ev) {
		return ev.bcws(defaultStart,defaultEnd); 
	}
	
	public static EarnedValueCalculator getInstance() {
		if (instance == null)
			instance = new EarnedValueCalculator();
		return instance;
	}
	private EarnedValueCalculator() {}
	
	public double cv(EarnedValueValues ev, long start, long end) {
//		return ev.acwp(start,end) - ev.bcwp(start,end);
		return ev.bcwp(start,end) - ev.acwp(start,end);
	}
	
	public double cv(EarnedValueValues ev) {
		return cv(ev,defaultStart,defaultEnd); 
	}
	
	public double sv(EarnedValueValues ev, long start, long end) {
		return ev.bcwp(start,end) - ev.bcws(start,end);
	}

	public double sv(EarnedValueValues ev) {
		return sv(ev,defaultStart,defaultEnd); 
	}
	
	public double eac(EarnedValueValues ev, long start, long end) {
		double bcwp = ev.bcwp(start,end);
		if (bcwp == 0.0D) {
			if (getDivideByZeroValue() == 0)// prevent divide by 0
				return 0;
			bcwp = ev.cost(start,end); // use cost for bcwp in case no bcwp
		}
		double acwp = ev.acwp(start,end);
		if (acwp == 0) {
			if (getDivideByZeroValue() != 0)
				return ev.cost(start,end); // use cost eac
		}
		return acwp + acwp * (ev.bac(start,end) - bcwp) / bcwp;
	}

	public double eac(EarnedValueValues ev) {
		return eac(ev,defaultStart,defaultEnd); 
	}
	
	public double vac(EarnedValueValues ev, long start, long end) {
		return ev.bac(start,end) - eac(ev,start,end);
	}

	public double vac(EarnedValueValues ev) {
		return vac(ev,defaultStart,defaultEnd); 
	}

	public double cpi(EarnedValueValues ev, long start, long end) {
		double acwp = ev.acwp(start,end);
		if (acwp == 0.0D) // prevent divide by 0
			return getDivideByZeroValue();
		return ev.bcwp(start,end) / acwp;
	}

	public double cpi(EarnedValueValues ev) {
		return cpi(ev,defaultStart,defaultEnd); 
	}

	public double spi(EarnedValueValues ev, long start, long end) {
		double bcws = ev.bcws(start,end);
		if (bcws == 0.0D) // prevent divide by 0
			return getDivideByZeroValue();
		return ev.bcwp(start,end) / bcws;
	}

	public double spi(EarnedValueValues ev) {
		return spi(ev,defaultStart,defaultEnd); 
	}
	public double csi(EarnedValueValues ev, long start, long end) {
		return spi(ev,start,end) * cpi(ev,start,end);
	}
	public double csi(EarnedValueValues ev) {
		return spi(ev) * cpi(ev);
	}
	public double cvPercent(EarnedValueValues ev, long start, long end) {
		double bcwp = ev.bcwp(start,end);
		if (bcwp == 0.0D) // prevent divide by 0
			return getDivideByZeroValue();

		return (bcwp - ev.acwp(start,end)) / bcwp;
	}
		
	public double cvPercent(EarnedValueValues ev) {
		return cvPercent(ev,defaultStart,defaultEnd); 
	}
	
	public double svPercent(EarnedValueValues ev, long start, long end) {
		double bcws = ev.bcws(start,end);
		if (bcws == 0.0D) // prevent divide by 0
			return getDivideByZeroValue();
		return (ev.bcwp(start,end) - bcws) / bcws;
	}	

	public double svPercent(EarnedValueValues ev) {
		return svPercent(ev,defaultStart,defaultEnd); 
	}
	
	public double tcpi(EarnedValueValues ev, long start, long end) {
		double bac = ev.bac(start,end);
		double acwp = ev.acwp(start,end);
		if (bac == acwp) // prevent divide by 0
			return getDivideByZeroValue();
		
		return (bac - ev.bcwp(start,end)) / (bac - acwp);
	}	

	public double tcpi(EarnedValueValues ev) {
		return tcpi(ev,defaultStart,defaultEnd); 
	}

	public double bcwr(EarnedValueValues ev, long start, long end) {
		return ev.bac(start,end) - ev.bcwp(start,end);
	}

	public double bcwr(EarnedValueValues ev) {
		return bcwr(ev,defaultStart,defaultEnd); 
	}
	public long getStartOffset(EarnedValueValues ev) {
		int numBaseline = Snapshottable.BASELINE.intValue(); // TODO use EV baseline?
		if (!(ev instanceof HasStartAndEnd))
			return 0L;
		if (!(ev instanceof BaselineScheduleFields))
			return 0L;
		if (!(ev instanceof HasCalendar))
			return 0L;
		long baselineStart = ((BaselineScheduleFields)ev).getBaselineStart(numBaseline);
		if (baselineStart == 0)
			return 0L;
		long start = ((HasStartAndEnd)ev).getStart();
		return ((HasCalendar)ev).getEffectiveWorkCalendar().compare(start,baselineStart, false);
	}
	public long getFinishOffset(EarnedValueValues ev) {
		int numBaseline = Snapshottable.BASELINE.intValue(); // TODO use EV baseline?
		if (!(ev instanceof HasStartAndEnd))
			return 0L;
		if (!(ev instanceof BaselineScheduleFields))
			return 0L;
		if (!(ev instanceof HasCalendar))
			return 0L;
		long baselineFinish = ((BaselineScheduleFields)ev).getBaselineFinish(numBaseline);
		if (baselineFinish == 0)
			return 0L;
		long finish = ((HasStartAndEnd)ev).getEnd();
		return ((HasCalendar)ev).getEffectiveWorkCalendar().compare(finish,baselineFinish, false);
		
	}
	private static final String NO_BASELINE = "There is no Earned Value data"; //$NON-NLS-1$
	private static String metricLabel(String key, double value) {
		return java.text.MessageFormat.format("{0}={1}", Messages.getString(key), value);
	}
	public ImageLink getScheduleStatusIndicator(double spi) {
		
		return ImageLink.trafficLight(spi == 0.0D ? NO_BASELINE : metricLabel("EarnedValueCalculator.SPI", spi),spi, 1.0D, 0.9D); //$NON-NLS-1$
	}
	public ImageLink getBudgetStatusIndicator(double cpi) {
		return ImageLink.trafficLight(cpi == 0.0D ? NO_BASELINE : metricLabel("EarnedValueCalculator.CPI", cpi),cpi, 1.0D, 0.9D); //$NON-NLS-1$
	}
	public ImageLink getStatusIndicator(double csi) {
		return ImageLink.trafficLight(csi == 0.0D ? NO_BASELINE : metricLabel("EarnedValueCalculator.CSI", csi),csi, 1.0D, 0.81D); //$NON-NLS-1$
	}
	
}
