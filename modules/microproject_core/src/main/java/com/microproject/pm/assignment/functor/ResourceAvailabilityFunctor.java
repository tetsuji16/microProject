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
package com.microproject.pm.assignment.functor;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.availability.Availability;
import com.microproject.pm.time.HasStartAndEnd;

/**
 * A functor which cumulates resource availability
 */
public class ResourceAvailabilityFunctor extends AssignmentFieldFunctor {
	double maxUnits;
	private Resource resource;
	private ResourceAvailabilityFunctor(Assignment assignment) {
		super(assignment,assignment.getResource().getEffectiveWorkCalendar(), null);
		if (workCalendar == null) {
			// if no work calendar for resource, then use project's calendar
			workCalendar = assignment.getTask().getProject().getEffectiveWorkCalendar();
		}
		resource = assignment.getResource();
		maxUnits = resource.getMaximumUnits();
	}
	private ResourceAvailabilityFunctor(Resource resource) {
		super(null,resource.getEffectiveWorkCalendar(), null);
		this.resource = resource;
		maxUnits = resource.getMaximumUnits();
	}
	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd)object;
		Availability availability = (Availability) resource.getAvailabilityTable().findActive(interval.getStart());
		double intervalUnits = (availability == null) ? maxUnits : availability.getMaximumUnits();
		value += intervalUnits * workCalendar.compare(interval.getEnd(),interval.getStart(), false);
	}
	
	
	public void initialize() {
		super.initialize();
	}
	public static ResourceAvailabilityFunctor getInstance(Assignment assignment) {
		return new ResourceAvailabilityFunctor(assignment);
	}	

	public static ResourceAvailabilityFunctor getInstance(Resource resource) {
		return new ResourceAvailabilityFunctor(resource);
	}	
}

