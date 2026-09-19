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
package com.microproject.pm.assignment;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.pm.snapshot.Snapshottable;

public class TimeDistributedHelper {
	private static final Logger logger = Logger.getLogger(TimeDistributedHelper.class.getName());
	private static final Map<Object, Object> baselineMapper;
	static {
		Map<Object, Object> m = new HashMap<>();
		m.put(HasTimeDistributedData.WORK, Snapshottable.CURRENT);
		m.put(HasTimeDistributedData.ACTUAL_WORK, Snapshottable.CURRENT);
		m.put(HasTimeDistributedData.REMAINING_WORK, Snapshottable.CURRENT);
		m.put(HasTimeDistributedData.BASELINE_WORK, Snapshottable.BASELINE);
		m.put(HasTimeDistributedData.BASELINE1_WORK, Snapshottable.BASELINE_1);
		m.put(HasTimeDistributedData.BASELINE2_WORK, Snapshottable.BASELINE_2);
		m.put(HasTimeDistributedData.BASELINE3_WORK, Snapshottable.BASELINE_3);
		m.put(HasTimeDistributedData.BASELINE4_WORK, Snapshottable.BASELINE_4);
		m.put(HasTimeDistributedData.BASELINE5_WORK, Snapshottable.BASELINE_5);
		m.put(HasTimeDistributedData.BASELINE6_WORK, Snapshottable.BASELINE_6);
		m.put(HasTimeDistributedData.BASELINE7_WORK, Snapshottable.BASELINE_7);
		m.put(HasTimeDistributedData.BASELINE8_WORK, Snapshottable.BASELINE_8);
		m.put(HasTimeDistributedData.BASELINE9_WORK, Snapshottable.BASELINE_9);
		m.put(HasTimeDistributedData.BASELINE10_WORK, Snapshottable.BASELINE_10);

		m.put(HasTimeDistributedData.COST, Snapshottable.CURRENT);
		m.put(HasTimeDistributedData.ACTUAL_COST, Snapshottable.CURRENT);
		m.put(HasTimeDistributedData.REMAINING_COST, Snapshottable.CURRENT);
		m.put(HasTimeDistributedData.BASELINE_COST, Snapshottable.BASELINE);
		m.put(HasTimeDistributedData.BASELINE1_COST, Snapshottable.BASELINE_1);
		m.put(HasTimeDistributedData.BASELINE2_COST, Snapshottable.BASELINE_2);
		m.put(HasTimeDistributedData.BASELINE3_COST, Snapshottable.BASELINE_3);
		m.put(HasTimeDistributedData.BASELINE4_COST, Snapshottable.BASELINE_4);
		m.put(HasTimeDistributedData.BASELINE5_COST, Snapshottable.BASELINE_5);
		m.put(HasTimeDistributedData.BASELINE6_COST, Snapshottable.BASELINE_6);
		m.put(HasTimeDistributedData.BASELINE7_COST, Snapshottable.BASELINE_7);
		m.put(HasTimeDistributedData.BASELINE8_COST, Snapshottable.BASELINE_8);
		m.put(HasTimeDistributedData.BASELINE9_COST, Snapshottable.BASELINE_9);
		m.put(HasTimeDistributedData.BASELINE10_COST, Snapshottable.BASELINE_10);

		baselineMapper = Map.copyOf(m);
	}
	public static Object baselineForData(Object data) {
		return baselineMapper.get(data);
	}
	public static boolean isWork(Object data) {
		if (data instanceof Field) {
			return ((Field)data).isWork();
		}
		if (data instanceof Number) {
			int type = ((Number)data).intValue();
			if (type > 0 &&  type <= 4)
				return true;
			return (type - 16) %6 == 0; // See TimeDistributedTypeMapper
		}
		return false;
	}
	public static boolean isCost(Object data) {
		if (data instanceof Field) {
			return ((Field)data).isMoney();
		}
		return false;
	}
	public static String getIdForObject(Object obj) {
		if (obj instanceof String)
			return (String) obj;
		else
			return ((Field)obj).getId();
	}
	public static Object getObjectFromId(String id) {
		Object result = null;
		if (id.equals(TimeDistributedConstants.REMAINING_WORK))	
			result = TimeDistributedConstants.REMAINING_WORK;
		else if (id.equals(TimeDistributedConstants.REMAINING_COST))	
			result = TimeDistributedConstants.REMAINING_COST;
		else 
			result =Configuration.getFieldFromId(id);
		if (result == null)
			logger.warning("error no object for id " + id);
		return result;
	}
}
