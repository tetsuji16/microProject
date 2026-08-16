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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.core.time.TimeInterval;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkRange;
import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.pm.calendar.WorkingHours;

/**
 * Copies a microproject WorkRange into a microproject WorkDay's working hours (the
 * .pod (de)serialization path). Both sides use the same microproject model.
 * @author Laurent Chretienneau
 */
public class OpRangeConverter {

	private static final Logger logger = Logger.getLogger(OpRangeConverter.class.getName());

	public void to(WorkDay opDay, WorkRange range) {
		if (range == null)
			return;
		WorkingHours workingHours = new WorkingHours();
		if (opDay != null) {
			try {
				workingHours.setInterval(0, range.getStart(), range.getEnd());
			} catch (WorkRangeException e) {
				logger.log(Level.WARNING, "Failed to map work range interval", e);
			}
			opDay.setWorkingHours(workingHours);
		}
	}
}
