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
package com.microproject.pm.criticalpath;

import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.field.AlgorithmFieldUpdater;
import com.microproject.pm.task.Task;
import com.microproject.transaction.MultipleTransaction;
/**
 * Interface for scheduling algorithms, such as the critical path
 */
public interface SchedulingAlgorithm extends ObjectEvent.Listener, HasSentinels, MultipleTransaction.Listener {
	/**
	 * Get the scheduling type to use for new tasks.  Forward Critical path will use ASAP, reverse
	 * scheduling will use ALAP
	 * @return ConstraintType.ALAP or ConstraintType.ASAP
	 */
	public int getDefaultTaskConstraintType();
	public void calculate(boolean update);
	/**
	 * @param project
	 */
	public void initialize(Object object);
	public void reset();
	public String getName();
	public int getCalculationStateCount();
	public boolean getMarkerStatus();
	public void addObject(Object task);
	Document getMasterDocument();
	void markBoundsAsDirty();
	public void objectChanged(ObjectEvent objectEvent);
	public void addSubproject(Task subproject);
	public void initEarliestAndLatest();
	public void setEarliestAndLatest(long earliest, long latest);
	AlgorithmFieldUpdater getFieldUpdater();
}
