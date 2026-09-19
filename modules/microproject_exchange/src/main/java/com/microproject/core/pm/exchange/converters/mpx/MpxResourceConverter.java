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

import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.Resource;

import net.sf.mpxj.ProjectCalendar;

/**
 * Converts an MPXJ Resource into a microproject Resource.
 * Only fields carried by the microproject Resource interface are mapped. Rates,
 * cost, maximum units, start/finish and availability are intentionally skipped
 * (see issue #154).
 * @author Laurent Chretienneau
 */
public class MpxResourceConverter {
	private MpxCalendarConverter calendarConverter = new MpxCalendarConverter();

	public void from(net.sf.mpxj.Resource mpxResource, Resource resource, MpxImportState state) {
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
		if (mpxResource.getCostPerUse() != null)
			resource.setCostPerUse(mpxResource.getCostPerUse().doubleValue());

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
}
