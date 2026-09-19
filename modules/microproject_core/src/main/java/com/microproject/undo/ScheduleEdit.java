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
package com.microproject.undo;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.server.data.DataObject;

/**
 *
 */
public class ScheduleEdit extends AbstractUndoableEdit{
	protected Schedule schedule;
	protected ScheduleInterval interval;
	protected long start,end;
	protected boolean isChild;
	protected Object source,detailBackup;
	
	/**
	 * @param interval
	 * @param oldInterval
	 * @param source
	 */
	public ScheduleEdit(Schedule schedule, Object detailBackup,long start,long end, ScheduleInterval interval, boolean isChild,
			Object source) {
		super();
		this.schedule=schedule;
		this.start=start;
		this.end=end;
		this.interval = interval;
		this.isChild=isChild;
		this.source = source;
		this.detailBackup=detailBackup;
	}
	public boolean canRedo() {
		return super.canRedo();
	}
	public boolean canUndo() {
		return super.canUndo();
	}
	public String getPresentationName() {
		String s="Schedule";
		if (schedule!=null&&schedule instanceof DataObject){
			DataObject data=(DataObject)schedule;
			String cn=schedule.getClass().getName();
			cn=cn.substring(cn.lastIndexOf('.')+1);
			s+=": "+cn+" "+data.getName()+"("+data.getUniqueId()+")";
		}
		return s;
	}
	public void redo() throws CannotRedoException {
		super.redo();
		ScheduleService.getInstance().setInterval(this, schedule, start, end, interval, null);
		//schedule.moveInterval(this,start,end,interval,isChild);
	}
	public void undo() throws CannotUndoException {
		super.undo();
		schedule.restoreDetail(this,detailBackup,false);
		
	}
}
