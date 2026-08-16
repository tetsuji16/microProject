/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.microproject.core.pm.exchange.converters.mpx;

import com.microproject.core.time.TimeUtil;
import com.microproject.exchange.ImportedCalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;

import net.sf.mpxj.Day;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectCalendarHours;

/**
 * Converts an MPXJ ProjectCalendar into a microproject WorkingCalendar.
 * Exact per-day working hours are collapsed to the standard working-day template
 * (see issue #154); calendar exceptions are carried as date-bounded WorkDay
 * entries.
 * @author Laurent Chretienneau
 */
public class MpxCalendarConverter {
	public void from(ProjectCalendar mpxCalendar, WorkingCalendar calendar, MpxImportState state){
		calendar.setName(mpxCalendar.getName());
		calendar.setId(mpxCalendar.getUniqueID());

		// base calendar
		WorkingCalendar standardCalendar = WorkingCalendar.getStandardBasedInstance();
		WorkingCalendar baseCalendar = null;
		if (mpxCalendar.isDerived()) {
			ProjectCalendar mpxBaseCalendar = mpxCalendar.getParent();
			if (mpxBaseCalendar != null) {
				baseCalendar = (WorkingCalendar) state.getImportedCalendar(mpxBaseCalendar);
			}
			if (baseCalendar == null)
				baseCalendar = standardCalendar;
			try {
				calendar.setBaseCalendar(baseCalendar);
			} catch (com.microproject.configuration.CircularDependencyException e) {
				// ignore: keep unbased calendar
			}
		}

		// work weeks
		for (int i = 0; i < 7; i++) {
			Day mpxDayId = Day.getInstance(i + 1);
			ProjectCalendarHours mpxDay = mpxCalendar.getCalendarHours(mpxDayId);
			net.sf.mpxj.DayType mpxDayType = mpxCalendar.getDayType(mpxDayId);
			WorkDay day = null;
			if (mpxDay == null) {
				if (mpxCalendar.isDerived() && baseCalendar != null) {
					if (mpxDayType == net.sf.mpxj.DayType.DEFAULT)
						day = baseCalendar.getWeekDay(i);
					else if (mpxBaseCalendarIsWorking(mpxCalendar, mpxDayId))
						day = WorkDay.getNonWorkingDay();
				}
			} else {
				if (mpxDayType == net.sf.mpxj.DayType.WORKING) {
					day = WorkDay.getDefaultWorkDay();
				} else {
					day = WorkDay.getNonWorkingDay();
				}
			}
			if (day != null)
				calendar.setWeekDay(i, day);
		}

		// exceptions
		MpxExceptionConverter exceptionConverter = new MpxExceptionConverter();
		for (ProjectCalendarException mpxException : mpxCalendar.getCalendarExceptions()) {
			long from = TimeUtil.removeTimeZoneOffset(mpxException.getFromDate().getTime());
			long to = TimeUtil.removeTimeZoneOffset(mpxException.getToDate().getTime());
			WorkDay exception = new WorkDay(from, to);
			exceptionConverter.from(mpxException, exception);
			calendar.addOrReplaceException(exception);
		}
	}

	private static boolean mpxBaseCalendarIsWorking(ProjectCalendar mpxCalendar, Day day) {
		try {
			return mpxCalendar.isWorkingDay(day);
		} catch (Exception e) {
			return false;
		}
	}
}
