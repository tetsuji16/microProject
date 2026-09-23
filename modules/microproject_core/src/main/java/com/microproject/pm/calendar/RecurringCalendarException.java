/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.pm.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.microproject.util.DateTime;

/** A named exception whose date span repeats according to a retained recurrence rule. */
public final class RecurringCalendarException implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	private WorkDay template;
	private CalendarRecurrence recurrence;

	public RecurringCalendarException(WorkDay template, CalendarRecurrence recurrence) {
		this.template = Objects.requireNonNull(template, "template").clone();
		this.recurrence = Objects.requireNonNull(recurrence, "recurrence").clone();
		if (DateTime.dayFloor(template.getStart()) != recurrence.getStartDate())
			throw new IllegalArgumentException("Exception start must match the recurrence start date");
	}

	public WorkDay getTemplate() { return template.clone(); }
	public CalendarRecurrence getRecurrence() { return recurrence.clone(); }
	public List<WorkDay> getOccurrences() {
		long spanDays = (DateTime.dayFloor(template.getEnd()) - DateTime.dayFloor(template.getStart()))
			/ WorkCalendar.MILLIS_IN_DAY;
		ArrayList<WorkDay> result = new ArrayList<>();
		for (long start : recurrence.occurrenceDates()) {
			WorkDay occurrence = new WorkDay(start, DateTime.dayFloor(start) + spanDays * WorkCalendar.MILLIS_IN_DAY,
				template.getDescription());
			occurrence.setWorkingHours(template.getWorkingHours().clone());
			result.add(occurrence);
		}
		return List.copyOf(result);
	}

	@Override public RecurringCalendarException clone() {
		try {
			RecurringCalendarException result = (RecurringCalendarException) super.clone();
			result.template = template.clone();
			result.recurrence = recurrence.clone();
			return result;
		} catch (CloneNotSupportedException impossible) {
			throw new IllegalStateException("RecurringCalendarException must be cloneable", impossible);
		}
	}
}
