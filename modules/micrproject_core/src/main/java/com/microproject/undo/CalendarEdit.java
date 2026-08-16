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

import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;

/**
 *
 */
public class CalendarEdit extends AbstractUndoableEdit{
	protected WorkingCalendar oldValue;
	protected WorkingCalendar dest;
	protected WorkingCalendar newValue;
	
	public CalendarEdit(WorkingCalendar dest, WorkingCalendar newValue) {
		super();
		this.dest = dest;
		this.newValue = newValue.makeScratchCopy();
		this.oldValue = dest.makeScratchCopy();
	}
	public boolean canRedo() {
		return super.canRedo();
	}
	public boolean canUndo() {
		return super.canUndo();
	}
	public String getPresentationName() {
		return "Calendar";
	}
	public void redo() throws CannotRedoException {
		super.redo();
		dest.assignFrom(newValue);
	    CalendarService service=CalendarService.getInstance();
	    service.saveAndUpdate(dest);
	}
	public void undo() throws CannotUndoException {
		super.undo();
		dest.assignFrom(oldValue);
	    CalendarService service=CalendarService.getInstance();
	    service.saveAndUpdate(dest);
	}
}
