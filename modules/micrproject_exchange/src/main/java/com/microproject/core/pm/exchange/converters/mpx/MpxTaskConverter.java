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

import java.util.Date;

import com.microproject.core.time.TimeUtil;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.task.Task;

import net.sf.mpxj.Duration;
import net.sf.mpxj.TaskMode;

/**
 * Converts an MPXJ Task into a microproject Task.
 * Only fields that exist on the microproject Task model are mapped; fields that
 * the microproject model does not carry (estimated, effortDriven, schedulingType,
 * priority, cost, fixedCost, fixedCostAccrual, external, ...) are intentionally
 * skipped. See issue #154 for the model-extension discussion.
 * @author Laurent Chretienneau
 */
public class MpxTaskConverter {
	private MpxCalendarConverter calendarConverter = new MpxCalendarConverter();

	public void from(net.sf.mpxj.Task mpxTask, Task task, MpxImportState state) {
		if (mpxTask.getName() != null)
			task.setName(mpxTask.getName());
		if (mpxTask.getWBS() != null)
			task.setWbs(mpxTask.getWBS());
		if (mpxTask.getNotes() != null)
			task.setNotes(mpxTask.getNotes());
		if (mpxTask.getID() != null)
			task.setId(mpxTask.getID().longValue());
		if (mpxTask.getUniqueID() != null)
			task.setUniqueId(mpxTask.getUniqueID().longValue());
		if (mpxTask.getConstraintType() != null) {
			try {
				task.setConstraintType(mpxTask.getConstraintType().getValue());
			} catch (com.microproject.field.FieldParseException e) {
				// leave default constraint type
			}
		}
		if (mpxTask.getEarnedValueMethod() != null)
			task.setEarnedValueMethod(mpxTask.getEarnedValueMethod().getValue());
		if (mpxTask.getMilestone())
			task.setMarkTaskAsMilestone(true);

		task.setCreated(mpxTask.getCreateDate());
		task.setDeadline(toLong(mpxTask.getDeadline()));
		task.setConstraintDate(toLong(mpxTask.getConstraintDate()));
		task.setLevelingDelay(toLong(mpxTask.getLevelingDelay()));

		task.setStart(toLong(mpxTask.getStart()));
		task.setEnd(toLong(mpxTask.getFinish()));
		task.setActualStart(toLong(mpxTask.getActualStart()));
		task.setActualFinish(toLong(mpxTask.getActualFinish()));
		task.setActualDuration(toLong(mpxTask.getActualDuration()));
		task.setRemainingDuration(toLong(mpxTask.getRemainingDuration()));
		task.setDuration(toLong(mpxTask.getDuration()));
		task.setPercentComplete(toRatio(mpxTask.getPercentageComplete()));
		if (mpxTask.getPhysicalPercentComplete() != null)
			task.setPhysicalPercentComplete(toRatio(mpxTask.getPhysicalPercentComplete()));

		task.setInactiveTask(!mpxTask.getActive());
		task.setManuallyScheduled(mpxTask.getTaskMode() == TaskMode.MANUALLY_SCHEDULED);

		// convert calendar
		WorkCalendar calendar;
		if (mpxTask.getCalendar() == null) {
			calendar = state.getProjectBaseCalendar();
		} else {
			calendar = state.getImportedCalendar(mpxTask.getCalendar());
			if (calendar == null) {
				calendar = WorkingCalendar.getStandardBasedInstance();
				calendar.setName(mpxTask.getName());
				calendarConverter.from(mpxTask.getCalendar(), (WorkingCalendar) calendar, state);
				state.registerImportedCalendar(calendar, mpxTask.getCalendar());
			}
		}
		task.setWorkCalendar(calendar);
	}

	private static long toLong(Date d) {
		if (d == null)
			return 0L;
		return TimeUtil.addTimeZoneOffset(d.getTime());
	}

	private static double toRatio(Number percentage) {
		return percentage == null ? 0.0 : percentage.doubleValue() / 100.0;
	}

	private static long toLong(Duration d) {
		return MpxUtils.toMillis(d);
	}
}
