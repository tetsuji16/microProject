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
package com.microproject.pm.assignment;

import com.microproject.field.FieldContext;
import com.microproject.pm.resource.Resource;

/**
 * Computed per-period fields of the usage views.  Cumulative work applies to every
 * time distributed row; overallocation and remaining availability apply to resources
 * (other rows have no availability and report zero).
 */
public interface UsageDetailFields extends HasTimeDistributedData {
	default long getCumulativeWork(FieldContext fieldContext) {
		return UsageDetailValues.cumulativeWork(this, FieldContext.end(fieldContext));
	}

	default long getOverallocation(FieldContext fieldContext) {
		if (!(this instanceof Resource resource) || !FieldContext.hasInterval(fieldContext))
			return 0L;
		long start = FieldContext.start(fieldContext);
		long end = FieldContext.end(fieldContext);
		return UsageDetailValues.overallocation(work(start, end), UsageDetailValues.availableWork(resource, start, end));
	}

	default long getRemainingAvailability(FieldContext fieldContext) {
		if (!(this instanceof Resource resource) || !FieldContext.hasInterval(fieldContext))
			return 0L;
		long start = FieldContext.start(fieldContext);
		long end = FieldContext.end(fieldContext);
		return UsageDetailValues.remainingAvailability(work(start, end), UsageDetailValues.availableWork(resource, start, end));
	}
}
