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
import java.util.StringJoiner;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private final boolean showPlusSign;
	private final boolean isWork;
	private final boolean canBeNonTemporal;
	private static Format instance = null;
	public static Format getInstance() {
		if (instance == null)
			instance = new DurationFormat(false, false, false);
		return instance;
	}

	private static DurationFormat projectInstance = null;
	/** Returns the stateless formatter used with explicit per-project defaults. */
	public static DurationFormat getProjectInstance() {
		if (projectInstance == null)
			projectInstance = new DurationFormat(false, false, false);
		return projectInstance;
	}
	private static Format signedInstance = null;
	public static Format getSignedInstance() {
		if (signedInstance == null)
			signedInstance = new DurationFormat(true, false, false);
		return signedInstance;
	}
	private static Format workInstance = null;
	public static Format getWorkInstance() {
		if (workInstance == null) {
			workInstance = new DurationFormat(false, true, false);
		}
		return workInstance;
	}
	
	private static Format nonTemporalWorkInstance = null;
	public static Format getNonTemporalWorkInstance() {
		if (nonTemporalWorkInstance == null) {
			nonTemporalWorkInstance = new DurationFormat(false, true, true);
		}
		return nonTemporalWorkInstance;
	}
	
	private static final int SINGULAR = 0;
	private static final int PLURAL = 1;

	/** Unit suffix recognized after the number: a TimeUnit index (NONE when absent) and the estimated flag. */
	record UnitMatch(int timeUnit, boolean estimated) {}

	/**
	 * Localized duration unit names read from the {@code Units.<type>.singular|plural} keys.
	 * The first four '|'-separated entries are the display styles indexed by
	 * {@link EditOption#getViewAs(int)}; any further entries are input-only aliases, so a
	 * locale can display its own unit names while still accepting the English abbreviations.
	 */
	static final class UnitNames {
		// these strings are themselves parts of string ids in properties file and as such must be hard coded as below
		private static final String[] TYPES = {"minute", "hour", "day", "week", "month",
				"year", "percent", "eminute", "ehour", "eday", "eweek", "emonth",
				"eyear", "epercent"};
		private static final String[] MULTIPLE = {".singular", ".plural"};
		static final int DISPLAY_STYLE_COUNT = 4;

		private final String[][][] display = new String[DISPLAY_STYLE_COUNT][MULTIPLE.length][TYPES.length];
		private final Pattern[] patterns = new Pattern[TYPES.length];
		private final String estimatedSymbol;

		UnitNames(UnaryOperator<String> lookup) {
			estimatedSymbol = lookup.apply("Units.estimatedSymbol");
			String estimatedRegex = lookup.apply("Units.estimatedSymbolRegex"); // Like ?
			for (int type = 0; type < TYPES.length; type++) {
				StringJoiner aliases = new StringJoiner("|");
				for (int j = 0; j < MULTIPLE.length; j++) {
					String key = "Units." + TYPES[type] + MULTIPLE[j];
					String[] names = lookup.apply(key).split("\\|", -1);
					if (names.length < DISPLAY_STYLE_COUNT)
						throw new IllegalStateException(key + " needs " + DISPLAY_STYLE_COUNT + " display names");
					for (int style = 0; style < DISPLAY_STYLE_COUNT; style++)
						display[style][j][type] = names[style];
					for (String name : names)
						if (!name.isEmpty())
							aliases.add(Pattern.quote(name));
				}
				// Group 1 is the optional unit, group 2 the optional estimated symbol (?) after white space
				patterns[type] = Pattern.compile("(" + aliases + ")?(\\s*" + estimatedRegex + "?)");
			}
		}

		String display(int style, boolean singular, int type) {
			return display[style][singular ? SINGULAR : PLURAL][type];
		}

		String estimatedSymbol() {
			return estimatedSymbol;
		}

		/** Returns the unit of a trimmed suffix such as "d", "days?" or "", or null when it is not a unit. */
		UnitMatch match(String suffix) {
			for (int type = 0; type < TYPES.length; type++) {
				Matcher matcher = patterns[type].matcher(suffix);
				if (matcher.matches())
					return new UnitMatch(matcher.group(1) != null ? type : TimeUnit.NONE, !matcher.group(2).isEmpty());
			}
			return null;
		}
	}

	private static final class LocalizedUnits {
		static final UnitNames NAMES = new UnitNames(Messages::getString);
	}

	private DurationFormat(boolean showPlusSign, boolean isWork, boolean canBeNonTemporal) {
		this.showPlusSign = showPlusSign;
		this.isWork = isWork;
		this.canBeNonTemporal = canBeNonTemporal;
	}
	
	
	// NumberFormat/DecimalFormat is not thread-safe; always use a fresh instance
	// per call instead of a shared static one (issue #184).
	private static NumberFormat decimalFormat() {
		return NumberFormat.getNumberInstance();
	}
	
	public Object parseObject(String durationString, ParsePosition pos) {
		return parseObject(durationString, pos, null);
	}

	/** Parses using an explicit project's unit defaults; null preserves legacy preference behavior. */
	public Object parseObject(String durationString, ParsePosition pos, DurationDefaults defaults) {
		if (durationString.isEmpty() || pos.getIndex() < 0 || pos.getIndex() >= durationString.length())
			return null;
		
		if (durationString.charAt(pos.getIndex()) == '+') // if string begins with + sign, ignore it
			pos.setIndex(pos.getIndex()+1);
				
				
		Number numberResult = decimalFormat().parse(durationString, pos);
		if (numberResult == null)
			return null;
		String durationPart = durationString.substring(pos.getIndex());
		durationPart = durationPart.trim();
		UnitMatch unit = LocalizedUnits.NAMES.match(durationPart);
		if (unit == null)
			return null;
		int timeUnit = unit.timeUnit(); // NONE when no unit was typed: use the default below
		double value = numberResult.doubleValue();
		if (timeUnit == TimeUnit.PERCENT || timeUnit == TimeUnit.ELAPSED_PERCENT)
			value /= 100.0;
		if (timeUnit == TimeUnit.NONE && isWork) {
			if (canBeNonTemporal)
				timeUnit = TimeUnit.NON_TEMPORAL;
			else
				timeUnit = defaults == null ? ScheduleOption.getInstance().getWorkUnit()
					: defaults.workUnit(); // use project work unit when available
		}
		if (timeUnit == TimeUnit.NONE)
			timeUnit = defaults == null ? ScheduleOption.getInstance().getDurationEnteredIn()
				: defaults.durationEnteredIn();
		long longResult = defaults == null ? Duration.getInstance(value, timeUnit)
			: Duration.getInstance(value, timeUnit, defaults);
		if (Duration.millis(longResult) > Duration.MAX_DURATION) // check for too big
			return null;
		if (unit.estimated())
			longResult = Duration.setAsEstimated(longResult,true);
		pos.setIndex(durationString.length());
		return new Duration(longResult);
	}
	/** Duration in its display unit, the number to print (already scaled for a percent) and the unit type. */
	private record FormattedValue(long duration, double value, int type) { }

	private FormattedValue formattedValue(Object durationObject, DurationDefaults defaults) {
		long duration = ((Duration)durationObject).getEncodedMillis();
		if (((Duration)durationObject).isWork() && Duration.getType(duration) != TimeUnit.NON_TEMPORAL) {
			int workUnit = defaults == null ? ScheduleOption.getInstance().getWorkUnit() : defaults.workUnit();
			duration = Duration.setAsTimeUnit(duration, workUnit);
		}
		double value = defaults == null ? Duration.getValue(duration) : Duration.getValue(duration, defaults);
		int type = effectiveType(duration, defaults);
		if (Duration.isPercent(duration))
			value *= 100.0;
		return new FormattedValue(duration, value, type);
	}

	public StringBuffer format(Object durationObject, StringBuffer toAppendTo, FieldPosition pos) {
		return format(durationObject, toAppendTo, pos, null);
	}

	/** Formats using an explicit project's unit defaults; null preserves legacy preference behavior. */
	public StringBuffer format(Object durationObject, StringBuffer toAppendTo, FieldPosition pos,
		DurationDefaults defaults) {
		FormattedValue shown = formattedValue(durationObject, defaults);
		long duration = shown.duration();
		double value = shown.value();
		int type = shown.type();
		if (value > 0D && showPlusSign)
			toAppendTo.append("+");
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

	public String format(Duration duration, DurationDefaults defaults) {
		return format(duration, new StringBuffer(), new FieldPosition(0), defaults).toString();
	}
	
	public String formatCompact(Object durationObject) {
		return formatCompact(durationObject, null);
	}

	public String formatCompact(Object durationObject, DurationDefaults defaults) {
		StringBuilder toAppendTo = new StringBuilder();
		FormattedValue shown = formattedValue(durationObject, defaults);
		long duration = shown.duration();
		double value = shown.value();
		int type = shown.type();
		if (value > 0D && showPlusSign)
			toAppendTo.append("+");
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

	private static int effectiveType(long duration, DurationDefaults defaults) {
		int type = Duration.getType(duration);
		if (type != TimeUnit.NONE)
			return type;
		return defaults == null ? ScheduleOption.getInstance().getDurationEnteredIn() : defaults.durationEnteredIn();
	}

	public static String formatTypeUnit(int type, boolean isSingular, boolean addSpace, boolean isPercent, boolean isEstimated, int displayIndex) {
		StringBuilder toAppendTo = new StringBuilder();
		if (type == TimeUnit.NON_TEMPORAL)
			return "";
		if (addSpace && !isPercent) {
			toAppendTo.append(" ");
		}
		toAppendTo.append(LocalizedUnits.NAMES.display(displayIndex, isSingular, type));
		if (isEstimated)
			toAppendTo.append(LocalizedUnits.NAMES.estimatedSymbol());
		return toAppendTo.toString();
	}	
	
	public static String formatTypeUnit(int type) {
		return formatTypeUnit(type,true,false,false,false,EditOption.getInstance().getViewAs(type));
	}
	
	public static String format(long millis) {
		return getInstance().format(new Duration(millis)).toString();
	}
	public static String format(long millis, DurationDefaults defaults) {
		return getProjectInstance().format(new Duration(millis), defaults);
	}
	public static String formatCompact(long millis) {
		return ((DurationFormat)getInstance()).formatCompact(new Duration(millis)).toString();
	}
	public static String formatWork(long millis) {
		return getWorkInstance().format(new Work(millis)).toString();
	}
	public static String formatWork(long millis, DurationDefaults defaults) {
		return ((DurationFormat) getWorkInstance()).format(new Work(millis), defaults);
	}
	public static String formatWork(Object millis) {
		if (millis instanceof Long longMillis) return formatWork(longMillis);
		return getWorkInstance().format(millis);
	}
}
