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
package com.microproject.core.pm.exchange.converters.op;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.microproject.core.fields.FieldUtil;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.task.Project;
import com.microproject.configuration.CircularDependencyException;

/**
 * @author Laurent Chretienneau
 *
 */
public class OpProjectConverter {
	protected static Logger log = Logger.getLogger("OpTaskConverter");
	protected String[] fieldsToConvert=new String[]{
			//ProjectLibre, mpx, converter (mpx-> ProjectLibre
		"name", "name", null,
		"mamager", "manager", null,
		"notes", "notes", null,
		"start", "startDate", "com.microproject.core.pm.exchange.converters.type.LongDateConverter",
		"statusDate", "statusDate", "com.microproject.core.pm.exchange.converters.type.LongDateConverter",
	};
	public void to(com.microproject.pm.task.Project opProject, Project project, OpImportState state) {
		if (project.getName() != null)
			opProject.setName(project.getName());
		if (project.getManager() != null)
			opProject.setManager(project.getManager());
		if (project.getNotes() != null)
			opProject.setNotes(project.getNotes());
		opProject.setStartDate(project.getStart());
		opProject.setStatusDate(project.getStatusDate());

		//copy base calendar
		WorkCalendar calendar = project.getBaseCalendar();
		if (calendar != null) {
			try {
				opProject.setBaseCalendar(calendar);
			} catch (CircularDependencyException e) {
				log.log(Level.WARNING, "Failed to set base calendar", e);
			}
		}
	}

}
