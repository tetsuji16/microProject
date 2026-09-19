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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.pm.scheduling.Schedule;

public class ScheduleBackupEdit  extends AbstractUndoableEdit{
	Map backups;
	protected Object source;
	
	/**
	 * @param interval
	 * @param oldInterval
	 * @param source
	 */
	public ScheduleBackupEdit(Object schedule, Object source) {
		super();
		backups=new HashMap();
		Collection collection;
		if (schedule instanceof Collection){
			collection=(Collection)schedule;
		}else{
			collection=new ArrayList(1);
			collection.add(schedule);
		}
		for (Iterator i=collection.iterator();i.hasNext();){
			Schedule s=(Schedule)i.next();
			backups.put(s,s.backupDetail());
		}
		this.source=source;
	}
	public boolean canRedo() {
		return super.canRedo();
	}
	public boolean canUndo() {
		return super.canUndo();
	}
	public String getPresentationName() {
		return "ScheduleBackup";
	}
	public void redo() throws CannotRedoException {
		super.redo();
	}
	public void undo() throws CannotUndoException {
		super.undo();
		for (Iterator i=backups.keySet().iterator();i.hasNext();){
			Schedule s=(Schedule)i.next();
			s.restoreDetail(source, backups.get(s), false);
		}
	}

}
