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
package com.projectlibre1.server.data.mspdi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import net.sf.mpxj.TimeUnit;

/**
 * Parses and represents an xsd:duration value.
 *
 * Reimplemented to delegate all duration parsing and calendar arithmetic to the
 * JDK-standard {@link javax.xml.datatype.Duration} / {@link DatatypeFactory}
 * (a proven, specification-compliant module) instead of a hand-rolled parser.
 * The public API is preserved so existing callers (TimephasedGetter,
 * TimephasedService) are unaffected.
 */
public final class XsdDuration {

	private static final DatatypeFactory FACTORY;

	static {
		try {
			FACTORY = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final javax.xml.datatype.Duration duration;

	/**
	 * Constructor. Parses the xsd:duration value using the JDK datatype factory.
	 *
	 * @param value value formatted as an xsd:duration (may be null or "0")
	 */
	XsdDuration(String value) {
		if (value == null || value.isEmpty() || "0".equals(value)) {
			this.duration = FACTORY.newDuration(true, null, null, null, null, null, null);
		} else {
			this.duration = FACTORY.newDuration(value);
		}
	}

	/**
	 * Constructor that builds an xsd:duration from an MPXJ duration using the
	 * MPXJ public API for amount and units.
	 *
	 * @param source an MPXJ duration
	 */
	public XsdDuration(net.sf.mpxj.Duration source) {
		if (source == null) {
			this.duration = FACTORY.newDuration(true, null, null, null, null, null, null);
			return;
		}
		double amount = source.getDuration();
		boolean negative = amount < 0;
		amount = Math.abs(amount);

		BigInteger years = null;
		BigInteger months = null;
		BigInteger days = null;
		BigInteger hours = null;
		BigInteger minutes = null;
		BigDecimal seconds = null;

		switch (source.getUnits()) {
		case MINUTES:
		case ELAPSED_MINUTES:
			minutes = BigInteger.valueOf((long) amount);
			seconds = BigDecimal.valueOf((amount - minutes.intValue()) * 60.0);
			break;
		case HOURS:
		case ELAPSED_HOURS:
			hours = BigInteger.valueOf((long) amount);
			amount = (amount - hours.intValue()) * 60.0;
			minutes = BigInteger.valueOf((long) amount);
			seconds = BigDecimal.valueOf((amount - minutes.intValue()) * 60.0);
			break;
		case DAYS:
		case ELAPSED_DAYS:
			days = BigInteger.valueOf((long) amount);
			amount = (amount - days.intValue()) * 24.0;
			hours = BigInteger.valueOf((long) amount);
			amount = (amount - hours.intValue()) * 60.0;
			minutes = BigInteger.valueOf((long) amount);
			seconds = BigDecimal.valueOf((amount - minutes.intValue()) * 60.0);
			break;
		case WEEKS:
		case ELAPSED_WEEKS:
			amount *= 7.0;
			days = BigInteger.valueOf((long) amount);
			amount = (amount - days.intValue()) * 24.0;
			hours = BigInteger.valueOf((long) amount);
			amount = (amount - hours.intValue()) * 60.0;
			minutes = BigInteger.valueOf((long) amount);
			seconds = BigDecimal.valueOf((amount - minutes.intValue()) * 60.0);
			break;
		case MONTHS:
		case ELAPSED_MONTHS:
			months = BigInteger.valueOf((long) amount);
			amount = (amount - months.intValue()) * 28.0;
			days = BigInteger.valueOf((long) amount);
			amount = (amount - days.intValue()) * 24.0;
			hours = BigInteger.valueOf((long) amount);
			amount = (amount - hours.intValue()) * 60.0;
			minutes = BigInteger.valueOf((long) amount);
			seconds = BigDecimal.valueOf((amount - minutes.intValue()) * 60.0);
			break;
		case YEARS:
		case ELAPSED_YEARS:
			years = BigInteger.valueOf((long) amount);
			amount = (amount - years.intValue()) * 12.0;
			months = BigInteger.valueOf((long) amount);
			amount = (amount - months.intValue()) * 28.0;
			days = BigInteger.valueOf((long) amount);
			amount = (amount - days.intValue()) * 24.0;
			hours = BigInteger.valueOf((long) amount);
			amount = (amount - hours.intValue()) * 60.0;
			minutes = BigInteger.valueOf((long) amount);
			seconds = BigDecimal.valueOf((amount - minutes.intValue()) * 60.0);
			break;
		default:
			break;
		}
		if (seconds != null && seconds.signum() == 0) {
			seconds = null;
		}
		this.duration = FACTORY.newDuration(!negative, years, months, days, hours, minutes, seconds);
	}

	/**
	 * Convert from a string xsd duration value to a number of milliseconds.
	 *
	 * @param s xsd:duration string
	 * @return milliseconds
	 */
	public static long millis(String s) {
		if (s == null || s.equals("0") || s.equals("PT0H0M0S")) {
			return 0L;
		}
		if (s.equals("PT8H0M0S")) {
			return 8L * 60L * 60L * 1000L;
		}
		return new XsdDuration(s).getMillis();
	}

	public int getDays() {
		Integer v = duration.getDays();
		return v == null ? 0 : v.intValue();
	}

	public int getHours() {
		Integer v = duration.getHours();
		return v == null ? 0 : v.intValue();
	}

	public int getMinutes() {
		Integer v = duration.getMinutes();
		return v == null ? 0 : v.intValue();
	}

	public int getMonths() {
		Integer v = duration.getMonths();
		return v == null ? 0 : v.intValue();
	}

	public double getSeconds() {
		Number v = duration.getSeconds();
		return v == null ? 0.0 : v.doubleValue();
	}

	public int getYears() {
		Integer v = duration.getYears();
		return v == null ? 0 : v.intValue();
	}

	public long getMillis() {
		Calendar cal = new GregorianCalendar();
		return duration.getTimeInMillis(cal);
	}

	@Override
	public String toString() {
		// Preserve the full PnYnMnDTnHnMnS form expected by MSPDI writers.
		StringBuilder buffer = new StringBuilder("P");
		boolean negative = duration.getSign() < 0;
		buffer.append(getYears()).append('Y');
		buffer.append(getMonths()).append('M');
		buffer.append(getDays()).append('D');
		buffer.append('T');
		buffer.append(getHours()).append('H');
		buffer.append(getMinutes()).append('M');
		buffer.append(getSeconds()).append('S');
		if (negative) {
			buffer.insert(0, '-');
		}
		return buffer.toString();
	}
}
