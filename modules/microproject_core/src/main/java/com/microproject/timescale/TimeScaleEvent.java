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

/**
 * Reports that the time scale of a view changed and which part of it moved.
 */
public class TimeScaleEvent extends EventObject {
	private static final long serialVersionUID = 6776054567705472617L;

	/**
	 * The part of the time scale that changed. There is no "nothing changed"
	 * value: listeners are only notified when one of these applies.
	 */
	public enum ChangeType {
		/** The origin moved, so the visible range and its end both moved. */
		ORIGIN_AND_END,
		/** Only the end of the range moved. */
		END_ONLY,
		/** The scale itself (zoom level or minimum width) changed. */
		SCALE
	}

	protected ChangeType type;

	/**
	 * @param source the converter that reports the change
	 * @param type the part of the time scale that changed
	 */
	public TimeScaleEvent(Object source, ChangeType type) {
		super(source);
		this.type=type;
	}

	/**
	 * @return Returns the type.
	 */
	public ChangeType getType() {
		return type;
	}

	/**
	 * @param type The type to set.
	 */
	public void setType(ChangeType type) {
		this.type = type;
	}
}
