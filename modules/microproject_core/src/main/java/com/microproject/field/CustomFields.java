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
package com.microproject.field;


/**
 * Provides indexed values for the configured custom project fields.
 *
 * <p>Each index identifies one custom field of the corresponding type. The
 * available index range is defined by the field configuration. Callers must
 * use an index in that range; no checked exceptions are declared.</p>
 */
public interface CustomFields {
	/**
	 * Returns the cost stored at the selected custom cost field.
	 * @param i zero-based custom cost field index
	 * @return the value at {@code i}
	 */
	double getCustomCost(int i);
	/**
	 * Stores a value in the selected custom cost field.
	 * @param i zero-based custom cost field index
	 * @param cost the value to store
	 */
	void setCustomCost(int i, double cost);
	/**
	 * Returns the value stored at the selected custom date field.
	 * @param i zero-based custom date field index
	 * @return the value at {@code i}
	 */
	long getCustomDate(int i);
	/**
	 * Stores a value in the selected custom date field.
	 * @param i zero-based custom date field index
	 * @param date the value to store
	 */
	void setCustomDate(int i, long date);
	/**
	 * Returns the value stored at the selected custom duration field.
	 * @param i zero-based custom duration field index
	 * @return the value at {@code i}
	 */
	long getCustomDuration(int i);
	/**
	 * Stores a value in the selected custom duration field.
	 * @param i zero-based custom duration field index
	 * @param duration the value to store
	 */
	void setCustomDuration(int i, long duration);
	/**
	 * Returns the value stored at the selected custom finish field.
	 * @param i zero-based custom finish field index
	 * @return the value at {@code i}
	 */
	long getCustomFinish(int i);
	/**
	 * Stores a value in the selected custom finish field.
	 * @param i zero-based custom finish field index
	 * @param finish the value to store
	 */
	void setCustomFinish(int i, long finish);
	/**
	 * Returns the value stored at the selected custom flag field.
	 * @param i zero-based custom flag field index
	 * @return the value at {@code i}
	 */
	boolean getCustomFlag(int i);
	/**
	 * Stores a value in the selected custom flag field.
	 * @param i zero-based custom flag field index
	 * @param flag the value to store
	 */
	void setCustomFlag(int i, boolean flag);
	/**
	 * Returns the value stored at the selected custom number field.
	 * @param i zero-based custom number field index
	 * @return the value at {@code i}
	 */
	double getCustomNumber(int i);
	/**
	 * Stores a value in the selected custom number field.
	 * @param i zero-based custom number field index
	 * @param number the value to store
	 */
	void setCustomNumber(int i, double number);
	/**
	 * Returns the value stored at the selected custom start field.
	 * @param i zero-based custom start field index
	 * @return the value at {@code i}
	 */
	long getCustomStart(int i);
	/**
	 * Stores a value in the selected custom start field.
	 * @param i zero-based custom start field index
	 * @param start the value to store
	 */
	void setCustomStart(int i, long start);
	/**
	 * Returns the value stored at the selected custom text field.
	 * @param i zero-based custom text field index
	 * @return the value at {@code i}
	 */
	String getCustomText(int i);
	/**
	 * Stores a value in the selected custom text field.
	 * @param i zero-based custom text field index
	 * @param text the value to store
	 */
	void setCustomText(int i, String text);

}
