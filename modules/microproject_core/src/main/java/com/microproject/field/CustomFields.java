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
 * Interface describing custom fields.  Note that array bounds are set by the config file and stored in 
 * CustomFieldsImpl. All custom fields are access via indexed properties
 */
public interface CustomFields {
	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @return the cost stored in that custom field
	 */
	double getCustomCost(int i);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @param cost the cost to store
	 */
	void setCustomCost(int i, double cost);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @return the date stored in that custom field, as GMT-read milliseconds
	 */
	long getCustomDate(int i);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @param date the date to store, as GMT-read milliseconds
	 */
	void setCustomDate(int i, long date);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @return the duration stored in that custom field, encoded as in {@code Duration}
	 */
	long getCustomDuration(int i);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @param duration the duration to store, encoded as in {@code Duration}
	 */
	void setCustomDuration(int i, long duration);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @return the finish date stored in that custom field, as GMT-read milliseconds
	 */
	long getCustomFinish(int i);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @param finish the finish date to store, as GMT-read milliseconds
	 */
	void setCustomFinish(int i, long finish);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @return the flag stored in that custom field
	 */
	boolean getCustomFlag(int i);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @param flag the flag to store
	 */
	void setCustomFlag(int i, boolean flag);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @return the number stored in that custom field
	 */
	double getCustomNumber(int i);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @param number the number to store
	 */
	void setCustomNumber(int i, double number);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @return the start date stored in that custom field, as GMT-read milliseconds
	 */
	long getCustomStart(int i);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @param start the start date to store, as GMT-read milliseconds
	 */
	void setCustomStart(int i, long start);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @return the text stored in that custom field, possibly null
	 */
	String getCustomText(int i);

	/**
	 * @param i zero-based custom field index, below the configured bound
	 * @param text the text to store, possibly null
	 */
	void setCustomText(int i, String text);

}
