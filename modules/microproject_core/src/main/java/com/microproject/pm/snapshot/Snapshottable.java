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
package com.microproject.pm.snapshot;


/**
 * Snapshottable is an interface for objects which have current values and baselines - specifically tasks and assignments.
 * Also defined are constants which conform to 11 baselines. 
 * The snapshot data is indexed via an object. This permits easily moving to a non-integer-array based storage scheme in the future, such as
 * having a hashtable of named baselines.
 */
public interface Snapshottable extends Cloneable{
	public static final Integer CURRENT = Integer.valueOf(11);
	public static final Integer BASELINE = Integer.valueOf(0);
	public static final Integer BASELINE_1 = Integer.valueOf(1);
	public static final Integer BASELINE_2 = Integer.valueOf(2);
	public static final Integer BASELINE_3 = Integer.valueOf(3);
	public static final Integer BASELINE_4 = Integer.valueOf(4);
	public static final Integer BASELINE_5 = Integer.valueOf(5);
	public static final Integer BASELINE_6 = Integer.valueOf(6);
	public static final Integer BASELINE_7 = Integer.valueOf(7);
	public static final Integer BASELINE_8 = Integer.valueOf(8);
	public static final Integer BASELINE_9 = Integer.valueOf(9);
	public static final Integer BASELINE_10 = Integer.valueOf(10);
	public static final Integer TIMESHEET = Integer.valueOf(12);
	
	public DataSnapshot getSnapshot(Object snapshotId);
	public void setSnapshot(Object snapshotId, DataSnapshot snapshot);
    public void saveCurrentToSnapshot(Object snapshotId);
	public DataSnapshot getCurrentSnapshot();
	public void setCurrentSnapshot(DataSnapshot snapshot);
	public void clearSnapshot(Object snapshotId);
	public DataSnapshot cloneSnapshot(DataSnapshot snapshot);
}
