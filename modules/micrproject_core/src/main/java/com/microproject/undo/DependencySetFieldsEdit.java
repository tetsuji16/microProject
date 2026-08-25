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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.association.InvalidAssociationException;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;

/**
 *
 */
public class DependencySetFieldsEdit extends AbstractUndoableEdit{
	private static final Logger logger = Logger.getLogger(DependencySetFieldsEdit.class.getName());
	protected Dependency dependency;
	protected long lag;
	protected int type;
	protected Object source;
	
	
	/**
	 * @param dependency
	 * @param lag
	 * @param type
	 * @param source
	 */
	public DependencySetFieldsEdit(Dependency dependency, long lag, int type,
			Object source) {
		super();
		this.dependency = dependency;
		this.lag = lag;
		this.type = type;
		this.source = source;
	}
	public boolean canRedo() {
		return super.canRedo();
	}
	public boolean canUndo() {
		return super.canUndo();
	}
	public String getPresentationName() {
		return "DependencySetFields";
	}
	public void redo() throws CannotRedoException {
		if (!canRedo()) throw new CannotRedoException();
		changeFields(false);
		super.redo();
	}
	public void undo() throws CannotUndoException {
		if (!canUndo()) throw new CannotUndoException();
		changeFields(true);
		super.undo();
	}
	
	public void changeFields(boolean undo) throws CannotUndoException, CannotRedoException {
		try {
			long oldLag=dependency.getLag();
			int oldType=dependency.getDependencyType();
			DependencyService.getInstance().setFields(dependency,lag,type,this);
			lag=oldLag;
			type=oldType;
			DependencyService.getInstance().update(dependency,this);
		} catch (InvalidAssociationException e) {
			logger.log(Level.WARNING, "Failed to change dependency fields", e);
			if (undo) {
				CannotUndoException failure = new CannotUndoException();
				failure.initCause(e);
				throw failure;
			}
			CannotRedoException failure = new CannotRedoException();
			failure.initCause(e);
			throw failure;
		}
	}
}
