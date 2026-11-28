/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.assignment;

import com.microproject.datatype.Rate;
import com.microproject.model.task.Project;

/**
 * Assignment projection of task, resource, project, rate, contour, and timesheet fields.
 * Implementations expose these values to assignment-specific field and report consumers.
 */
public interface AssignmentSpecificFields {
	/** Returns the persisted work contour type code. */
	int getWorkContourType();
	/** Sets the persisted work contour type code. */
	void setWorkContourType(int workContourType);
	/** Returns the resource availability applicable to this assignment. */
	long getResourceAvailability();
	/** Returns the assigned task's display name. */
	String getTaskName();
	/** Returns the assigned task's identifier. */
	String getTaskId();
	/** Returns the assigned resource's display name. */
	String getResourceName();
	/** Returns the assigned resource's identifier. */
	String getResourceId();
	/** Returns the rate applied to this assignment. */
	Rate getRate();
	/** Replaces the rate applied to this assignment. */
	void setRate(Rate rate);
	/** Returns the owning project's display name. */
	String getProjectName();
	/** Returns the owning project. */
	Project getProject();
	/** Returns the start of the timesheet interval in milliseconds. */
	long getTimesheetStart();
	/** Returns the finish of the timesheet interval in milliseconds. */
	long getTimesheetFinish();
}
