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

import java.util.Calendar;

import com.projectlibre1.algorithm.Query;
import com.projectlibre1.algorithm.RangeIntervalGenerator;
import com.projectlibre1.algorithm.SelectFrom;
import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.assignment.HasTimeDistributedData;
import com.projectlibre1.pm.assignment.TimeDistributedHelper;
import com.projectlibre1.pm.assignment.functor.AssignmentFieldFunctor;
import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.util.DateTime;

import net.sf.mpxj.mspdi.schema.ObjectFactory;
import net.sf.mpxj.mspdi.schema.TimephasedDataType;

/**
 *
 */
public class TimephasedService {
	protected static TimephasedService instance=null;
	protected TimephasedService() {
	}

	public static TimephasedService getInstance(){
		if (instance==null) instance=new TimephasedService();
		return instance;
	}

	private void doQuery(Assignment assignment, ObjectFactory factory, TimephasedConsumer consumer,Object fieldType, int type, long id) {
		SelectFrom clause = SelectFrom.getInstance();
		AssignmentFieldFunctor dataFunctor = assignment.getDataSelect(fieldType,clause,false);
		TimephasedGetter getter = TimephasedGetter.getInstance(factory,consumer,dataFunctor,type,id);
		long end = assignment.getEnd();
		long start = assignment.getStart();

		if (fieldType == HasTimeDistributedData.ACTUAL_WORK)
			end = assignment.getStop();
		else if (fieldType == HasTimeDistributedData.REMAINING_WORK)
			start = assignment.getStop();
		RangeIntervalGenerator dailyInRange = RangeIntervalGenerator.getInstance(start, end, Calendar.DATE);

		Query.getInstance().selectFrom(clause)
		.groupBy(dailyInRange)
		.action(getter)
		.execute();
	}
	public void consumeTimephased(Schedule schedule,TimephasedConsumer consumer,Object factory){ //claur removed exception
		ObjectFactory mspdiTimephasedFactory=(ObjectFactory)factory;

		if (!(schedule instanceof Assignment))
			return; // only do assignments
		Assignment assignment = (Assignment)schedule;

		long id = 0;

		if ( assignment.getPercentComplete() > 0) {
			doQuery(assignment,mspdiTimephasedFactory, consumer,HasTimeDistributedData.ACTUAL_WORK, TimeDistributedTypeMapper.ASSIGNMENT_ACTUAL_WORK, id++);
		}
		doQuery(assignment,mspdiTimephasedFactory, consumer,HasTimeDistributedData.REMAINING_WORK, TimeDistributedTypeMapper.ASSIGNMENT_REMAINING_WORK, id++);


		Object fields[] = HasTimeDistributedData.baselineWorkTypes;
		Assignment baselineAssignment;
		for (int i = 0; i < fields.length; i++) {
			baselineAssignment = assignment.getBaselineAssignment(Integer.valueOf(i), false);
			if (baselineAssignment == null)
				continue;
			int mapType = TimeDistributedTypeMapper.getTimeDistributedType(i,false,baselineAssignment);
			doQuery(baselineAssignment,mspdiTimephasedFactory, consumer,HasTimeDistributedData.WORK, mapType, id++);
		}
	}

	public void readTimephased (Assignment assignment,TimephasedDataType t) {
		// if reading current info, do not bother unless the contour is nonstandard
//		if (TimeDistributedTypeMapper.isCurrent(t.getType().intValue()))// && assignment.getWorkContourType() != ContourTypes.CONTOURED)
//			return;

		if (!TimeDistributedHelper.isWork(t.getType()))
			return;

		Object type = TimeDistributedTypeMapper.getOPPrField(t.getType());

		// do not treat current values for non contoured assignments
//		if (TimeDistributedTypeMapper.isCurrent(t.getType().intValue()) && assignment.getWorkContourType() != ContourTypes.CONTOURED)
//			return;
		long duration = XsdDuration.millis(t.getValue());
		assignment.setInterval(type, t.getStart().getTime(), t.getFinish().getTime(), duration);
	}
}
