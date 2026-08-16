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

import com.microproject.core.time.TimeUtil;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.task.Project;

import net.sf.mpxj.ProjectProperties;

/**
 * Converts MPXJ ProjectProperties into a microproject Project header.
 * Only fields carried by the microproject Project model are mapped; fields the
 * model does not expose are intentionally skipped (see issue #154).
 * @author Laurent Chretienneau
 */
public class MpxProjectConverter {

	public void from(ProjectProperties mpxProjectHeader, Project project, MpxImportState state) {
		if (mpxProjectHeader.getName() != null)
			project.setName(mpxProjectHeader.getName());
		if (mpxProjectHeader.getManager() != null)
			project.setManager(mpxProjectHeader.getManager());
		if (mpxProjectHeader.getComments() != null)
			project.setNotes(mpxProjectHeader.getComments());
		project.setStartDate(toLong(mpxProjectHeader.getStartDate()));
		project.setStatusDate(toLong(mpxProjectHeader.getStatusDate()));

		WorkCalendar calendar = null;
		if (mpxProjectHeader.getDefaultCalendar() != null)
			calendar = state.getMappedBaseCalendar(mpxProjectHeader.getDefaultCalendar().getName());
		if (calendar == null)
			calendar = state.getProjectBaseCalendar();
		try {
			project.setBaseCalendar(calendar);
		} catch (com.microproject.configuration.CircularDependencyException e) {
			// ignore: a self-referential base calendar is not expected here
		}
	}

	private static long toLong(Date d) {
		if (d == null)
			return 0L;
		return TimeUtil.addTimeZoneOffset(d.getTime());
	}
}
