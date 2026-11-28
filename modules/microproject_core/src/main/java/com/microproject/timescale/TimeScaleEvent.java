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
package com.microproject.timescale;

import java.util.EventObject;

/** Describes one change to the visible time scale. */
public class TimeScaleEvent extends EventObject {
	private static final long serialVersionUID = 6776054567705472617L;

	/** The kind of change represented by a time-scale notification. */
	public enum ChangeType {
		/** The visible origin changed, which may also change the visible end. */
		ORIGIN_AND_END,
		/** Only the visible end changed. */
		END_ONLY,
		/** The scale changed while the visible date range stayed the same. */
		SCALE
	}

	private final ChangeType type;

	/** Creates an event for a time-scale change. */
	public TimeScaleEvent(Object source, ChangeType type) {
		super(source);
		this.type = java.util.Objects.requireNonNull(type, "type");
	}

	/** Returns the kind of time-scale change. */
	public ChangeType getType() {
		return type;
	}
}
