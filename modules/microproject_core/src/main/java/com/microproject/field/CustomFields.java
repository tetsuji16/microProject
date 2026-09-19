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


/**
 * Interface describing custom fields.  Note that array bounds are set by the config file and stored in 
 * CustomFieldsImpl. All custom fields are access via indexed properties
 */
public interface CustomFields {
	public double getCustomCost(int i);
	public void setCustomCost(int i, double cost);
	public long getCustomDate(int i);
	public void setCustomDate(int i, long date);
	public long getCustomDuration(int i);
	public void setCustomDuration(int i, long duration);
	public long getCustomFinish(int i);
	public void setCustomFinish(int i, long finish);
	public boolean getCustomFlag(int i);
	public void setCustomFlag(int i, boolean flag);
	public double getCustomNumber(int i);
	public void setCustomNumber(int i, double number);
	public long getCustomStart(int i);
	public void setCustomStart(int i, long start);
	public String getCustomText(int i);
	public void setCustomText(int i, String text);

}
