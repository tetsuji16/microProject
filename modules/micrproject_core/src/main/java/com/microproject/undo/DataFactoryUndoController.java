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

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoableEdit;

import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.pm.task.Project;
import com.microproject.transaction.DomainChangeSet;
import com.microproject.transaction.DomainChangeJournal;

/**
 *
 */
public class DataFactoryUndoController extends UndoController {
	protected NodeModelDataFactory dataFactory;
	public DataFactoryUndoController() {
		super();
	}
	public DataFactoryUndoController(NodeModelDataFactory dataFactory) {
		super();
		this.dataFactory=dataFactory;
	}
	
	public void undoableEditHappened(UndoableEditEvent e){
		withProjectWrite(() -> {
			super.undoableEditHappened(e);
			dataFactory.setGroupDirty(true);
		});
	}

	@Override
	public void commitEdit(UndoableEdit edit) {
		withProjectWrite(() -> super.commitEdit(edit));
	}

	@Override
	public void clear() {
		withProjectWrite(super::clear);
	}
	public NodeModelDataFactory getDataFactory() {
		return dataFactory;
	}
	public void setDataFactory(NodeModelDataFactory dataFactory) {
		this.dataFactory = dataFactory;
	}
	
	public String getPresentationName() {
		return "NodePaste";
	}

	@Override
	public void undo() {
		if (!(dataFactory instanceof Project project)) {
			super.undo();
			return;
		}
		DomainChangeJournal journal = project.getDomainChangeJournal();
		journal.write(() -> {
			if (!canUndo()) return null;
			try (DomainChangeJournal.Scope ignored = journal.suppressLegacyEvents()) {
				super.undo();
			}
			journal.recordLegacy(DomainChangeSet.Origin.UNDO);
			return null;
		});
	}

	@Override
	public void redo() {
		if (!(dataFactory instanceof Project project)) {
			super.redo();
			return;
		}
		DomainChangeJournal journal = project.getDomainChangeJournal();
		journal.write(() -> {
			if (!canRedo()) return null;
			try (DomainChangeJournal.Scope ignored = journal.suppressLegacyEvents()) {
				super.redo();
			}
			journal.recordLegacy(DomainChangeSet.Origin.REDO);
			return null;
		});
	}

	private void withProjectWrite(Runnable action) {
		if (dataFactory instanceof Project project)
			project.getDomainChangeJournal().write(() -> { action.run(); return null; });
		else
			action.run();
	}


}
