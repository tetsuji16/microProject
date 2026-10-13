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

import javax.swing.undo.UndoableEdit;

import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.task.Project;
import com.microproject.transaction.ProjectMutationState;

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
	
	public void editPosted(UndoableEdit edit){
		super.editPosted(edit);
		dataFactory.setGroupDirty(true);
	}

	@Override
	public void undo() {
		mutateProjectHistory(true);
	}

	@Override
	public void redo() {
		mutateProjectHistory(false);
	}

	private void mutateProjectHistory(boolean undo) {
		if (!(dataFactory instanceof Project project)) {
			if (undo) super.undo();
			else super.redo();
			return;
		}

		ProjectMutationState.Scope mutation = project.getMutationState().begin();
		project.beginEventBatch();
		int initialPendingEvents = project.pendingEventCount();
		Project.PendingEventBatch pending;
		boolean applying = false;
		try {
			boolean available = undo ? canUndo() : canRedo();
			if (!available) {
				pending = project.commitEventBatch(mutation, false);
				project.publishEventBatch(pending);
				return;
			}
			applying = true;
			if (undo) super.undo();
			else super.redo();
		} catch (RuntimeException | Error failure) {
			pending = finishHistoryMutation(project, mutation, applying, initialPendingEvents, failure);
			try {
				project.publishEventBatch(pending);
			} catch (Throwable publicationFailure) {
				failure.addSuppressed(publicationFailure);
			}
			throw failure;
		}
		if (project.pendingEventCount() == initialPendingEvents)
			project.fireScheduleChanged(project, ScheduleEvent.SCHEDULE);
		pending = project.commitEventBatch(mutation, true);
		project.publishEventBatch(pending);
	}

	private Project.PendingEventBatch finishHistoryMutation(Project project, ProjectMutationState.Scope mutation,
			boolean applying, int initialPendingEvents, Throwable failure) {
		try {
			boolean emittedFacts = project.pendingEventCount() > initialPendingEvents;
			if (applying && !emittedFacts)
				project.fireScheduleChanged(project, ScheduleEvent.SCHEDULE);
			return project.commitEventBatch(mutation, applying || emittedFacts);
		} catch (Throwable commitFailure) {
			failure.addSuppressed(commitFailure);
			return null;
		}
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


}
