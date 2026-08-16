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
package com.microproject.pm.scheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Laurent Chretienneau
 *
 */
public enum ScheduleFrom {
	//same constants as mpxj to simplify
	   START(0),
	   FINISH(1);

	protected int id;
	protected static Map<Integer,ScheduleFrom> reverseMap;

	private ScheduleFrom(int id){
		this.id=id;
	}
	public int getId() {
		return id;
	}
	public static ScheduleFrom getInstance(int id){
		if (reverseMap==null){
			reverseMap=new HashMap<Integer, ScheduleFrom>();
			for (ScheduleFrom ct : values())
				reverseMap.put(ct.getId(),ct);
		}
		return reverseMap.get(id);
	}

}
