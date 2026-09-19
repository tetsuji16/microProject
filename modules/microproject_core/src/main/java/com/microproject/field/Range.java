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
package com.microproject.field;

import java.text.MessageFormat;

import com.microproject.strings.Messages;

/**
 * Stores min and max values as well as optional error message
 * You can set min, max or both
 */
public class Range {
	private static final String messageErrorRangeMinimum = Messages.getString("Message.errorRangeMinimum.mf");
	private static final String messageErrorRangeMaximum = Messages.getString("Message.errorRangeMaximum.mf");
	private static final String messagePercentErrorRangeMinimum = Messages.getString("Message.errorPercentRangeMinimum.mf");
	private static final String messagePercentErrorRangeMaximum = Messages.getString("Message.errorPercentRangeMaximum.mf");
	

	double minimum = 0.0;
	double maximum = Double.MAX_VALUE;
	double step = 0;
	String errorMessage = null;
	
		/**
	 * @return Returns the maximum.
	 */
	public double getMaximum() {
		return maximum;
	}
	/**
	 * @param maximum The maximum to set.
	 */
	public void setMaximum(double maximum) {
		this.maximum = maximum;
	}
	/**
	 * @return Returns the minimum.
	 */
	public double getMinimum() {
		return minimum;
	}
	/**
	 * @param minimum The minimum to set.
	 */
	public void setMinimum(double minimum) {
		this.minimum = minimum;
	}
	public void validate(Object objectValue, Field field) throws FieldParseException {
		if (!(objectValue instanceof Number))
			return;
		Number value = (Number)objectValue;
		String error = null;
		if (minimum > ((Number)value).doubleValue()) {
			error = field.isPercent() ? messagePercentErrorRangeMinimum : messageErrorRangeMinimum;
		} else if (maximum < ((Number)value).doubleValue()) {
			error = field.isPercent() ? messagePercentErrorRangeMaximum : messageErrorRangeMaximum;
		}
		if (error != null) {
			throw new FieldParseException(getFormattedError(value,field, error));
		}
	}

	private String getFormattedError(Number value, Field field, String defaultMessage) {
		String message = (errorMessage == null) ? defaultMessage : errorMessage;
		return MessageFormat.format( message, new Object[] { Double.valueOf(minimum), Double.valueOf(maximum), value, field.getName()});
	}	
	
	/**
	 * @return Returns the errorMessage.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	/**
	 * @param errorMessage The errorMessage to set. The message must be formated.
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	/**
	 * @return Returns the step.
	 */
	public final double getStep() {
		return step;
	}
	/**
	 * @param step The step to set.
	 */
	public final void setStep(double step) {
		this.step = step;
	}
}

