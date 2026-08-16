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
package com.microproject.pm.scheduling;

import java.util.EventObject;

/**
 *
 */
public class ScheduleEvent extends EventObject {
	public static String ACTUAL = new String();
	public static String BASELINE = new String();
	public static String SCHEDULE = new String();
	
	private String type;
	private Object object;
	private Integer snapshot;
	private boolean saveSnapshot = true;
	/**
	 * @param arg0
	 */
	public ScheduleEvent(Object source, String type, Object object) {
		super(source);
		this.type = type;
		this.object = object;
	}
	public ScheduleEvent(Object source, String type) {
		this(source,type,null);
	}

	/**
	 * @return Returns the object.
	 */
	public Object getObject() {
		return object;
	}
	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return type;
	}
	/**
	 * @return Returns the snapshot.
	 */
	public Integer getSnapshot() {
		return snapshot;
	}
	/**
	 * @param snapshot The snapshot to set.
	 */
	public void setSnapshot(Integer snapshot) {
		this.snapshot = snapshot;
	}
	/**
	 * @return Returns the saveSnapshot.
	 */
	public boolean isSaveSnapshot() {
		return saveSnapshot;
	}
	/**
	 * @param saveSnapshot The saveSnapshot to set.
	 */
	public void setSaveSnapshot(boolean saveSnapshot) {
		this.saveSnapshot = saveSnapshot;
	}
}
