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
package com.microproject.core.pm.exchange.converters.mpx;

import java.util.Date;

import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.Rate;
import com.microproject.datatype.TimeUnit;
import com.microproject.interval.ValueObjectForInterval;
import com.microproject.pm.costing.CostRate;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.Resource;

import net.sf.mpxj.ProjectCalendar;

/**
 * Converts an MPXJ Resource into a microproject Resource.
 * Resource values represented by the local model are mapped, including
 * time-phased availability periods.
 * @author Laurent Chretienneau
 */
public class MpxResourceConverter {
	private MpxCalendarConverter calendarConverter = new MpxCalendarConverter();

	public void from(net.sf.mpxj.Resource mpxResource, Resource resource, MpxImportState state) {
		DurationDefaults defaults = MpxProjectConverter.durationDefaults(state.getMpxProjectFile() == null ? null
			: state.getMpxProjectFile().getProjectProperties());
		// Parse and validate interval and scalar availability before changing any target fields.
		MpxResourceAvailabilityConverter.from(mpxResource, resource.getAvailabilityTable());
		if (mpxResource.getName() != null)
			resource.setName(mpxResource.getName());
		if (mpxResource.getNotes() != null)
			resource.setNotes(mpxResource.getNotes());
		resource.setGeneric(mpxResource.getGeneric());
		if (mpxResource.getGroup() != null)
			resource.setGroup(mpxResource.getGroup());
		if (mpxResource.getInitials() != null)
			resource.setInitials(mpxResource.getInitials());
		if (mpxResource.getEmailAddress() != null)
			resource.setEmailAddress(mpxResource.getEmailAddress());
		if (mpxResource.getID() != null)
			resource.setId(mpxResource.getID().longValue());
		if (mpxResource.getUniqueID() != null)
			resource.setUniqueId(mpxResource.getUniqueID().longValue());
		if (mpxResource.getAccrueAt() != null)
			resource.setAccrueAt(mpxResource.getAccrueAt().getValue());
		for (int tableIndex = 0; tableIndex < net.sf.mpxj.CostRateTable.MAX_TABLES; tableIndex++) {
			// MPXJ and the domain both number tables A-E as 0-4.
			net.sf.mpxj.CostRateTable sourceTable = mpxResource.getCostRateTable(tableIndex);
			if (sourceTable == null || sourceTable.isEmpty()) continue;
			com.microproject.pm.costing.CostRateTable targetTable = resource.getCostRateTable(tableIndex);
			for (net.sf.mpxj.CostRateTableEntry sourceEntry : sourceTable) {
				Date startDate = sourceEntry.getStartDate();
				CostRate targetEntry;
				if (startDate == null || net.sf.mpxj.common.DateHelper.START_DATE_NA.equals(startDate)) {
					targetEntry = (CostRate) targetTable.getList().getFirst();
				} else {
					long start = startDate.getTime();
					ValueObjectForInterval existing = targetTable.findActive(start);
					if (existing != null && existing.getStart() == start) targetEntry = (CostRate) existing;
					else {
						try { targetEntry = (CostRate) targetTable.newValueObject(start); }
						catch (com.microproject.interval.InvalidValueObjectForIntervalException invalid) {
							throw new IllegalArgumentException("Invalid resource cost-rate effective date", invalid);
						}
					}
				}
				targetEntry.setStandardRate(fromMpxRate(sourceEntry.getStandardRate(), defaults));
				targetEntry.setOvertimeRate(fromMpxRate(sourceEntry.getOvertimeRate(), defaults));
				if (sourceEntry.getCostPerUse() != null)
					targetEntry.setCostPerUse(sourceEntry.getCostPerUse().doubleValue());
				Date endDate = sourceEntry.getEndDate();
				if (endDate != null && !net.sf.mpxj.common.DateHelper.END_DATE_NA.equals(endDate)
						&& !targetEntry.isFirst()) targetEntry.setEnd(endDate.getTime());
			}
		}
		// MSPDI represents table A's current value as Resource scalar fields. MPXJ's
		// reader ignores those scalar defaults when a Rates collection is present,
		// so prefer the values captured by MspImporter from the source XML.
		Integer sourceUniqueId = mpxResource.getUniqueID();
		MpxImportState.RawMspdiResourceRates rawRates = state.getRawMspdiResourceRates(
			sourceUniqueId == null ? null : Long.valueOf(sourceUniqueId.longValue()));
		if (rawRates != null) {
			if (rawRates.standardRate() != null) resource.setStandardRate(rawRates.standardRate());
			if (rawRates.overtimeRate() != null) resource.setOvertimeRate(rawRates.overtimeRate());
			if (rawRates.costPerUse() != null) resource.setCostPerUse(rawRates.costPerUse().doubleValue());
		} else {
			if (mpxResource.getStandardRate() != null)
				resource.setStandardRate(fromMpxRate(mpxResource.getStandardRate(), defaults));
			if (mpxResource.getOvertimeRate() != null)
				resource.setOvertimeRate(fromMpxRate(mpxResource.getOvertimeRate(), defaults));
			if (mpxResource.getCostPerUse() != null)
				resource.setCostPerUse(mpxResource.getCostPerUse().doubleValue());
		}

		// convert calendar
		WorkCalendar calendar;
		ProjectCalendar mpxCalendar = mpxResource.getCalendar();
		if (mpxCalendar == null) {
			calendar = state.getProjectBaseCalendar();
		} else {
			calendar = state.getImportedCalendar(mpxCalendar);
			if (calendar == null) {
				calendar = WorkingCalendar.getStandardBasedInstance();
				calendar.setName(mpxResource.getName());
				calendarConverter.from(mpxCalendar, (WorkingCalendar) calendar, state);
				state.registerImportedCalendar(calendar, mpxCalendar);
			}
		}
		resource.setWorkCalendar(calendar);
	}

	private static Rate fromMpxRate(net.sf.mpxj.Rate rate, DurationDefaults defaults) {
		if (rate == null) return new Rate();
		int unit = switch (rate.getUnits()) {
			case MINUTES -> TimeUnit.MINUTES;
			case HOURS -> TimeUnit.HOURS;
			case DAYS -> TimeUnit.DAYS;
			case WEEKS -> TimeUnit.WEEKS;
			case MONTHS -> TimeUnit.MONTHS;
			case YEARS -> TimeUnit.YEARS;
			case PERCENT -> TimeUnit.PERCENT;
			case ELAPSED_MINUTES -> TimeUnit.ELAPSED_MINUTES;
			case ELAPSED_HOURS -> TimeUnit.ELAPSED_HOURS;
			case ELAPSED_DAYS -> TimeUnit.ELAPSED_DAYS;
			case ELAPSED_WEEKS -> TimeUnit.ELAPSED_WEEKS;
			case ELAPSED_MONTHS -> TimeUnit.ELAPSED_MONTHS;
			case ELAPSED_YEARS -> TimeUnit.ELAPSED_YEARS;
			case ELAPSED_PERCENT -> TimeUnit.ELAPSED_PERCENT;
		};
		return new Rate(rate.getAmount() / Duration.timeUnitFactor(unit, defaults), unit);
	}
}
