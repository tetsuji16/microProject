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
package com.microproject.undo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.util.Environment;

/**
 *
 */
public class UndoController extends UndoManager implements EditListener{

	protected transient EditSupport editSupport;
	/**
	 *
	 */
	public UndoController() {
		editSupport=new EditSupport();
		editSupport.addEditListener(this);
	}
	public void editPosted(UndoableEdit edit){
		super.addEdit(edit);
		editSupport.historyRecorded(edit);
	}

	public void clear(){
		super.discardAllEdits();
		nodeMapping.clear();
	}

	public EditSupport getEditSupport() {
		return editSupport;
	}

	public void undo() {
		if (canUndo()) {
			boolean previousBatchMode = Environment.isBatchMode();
			Environment.setBatchMode(true);
			try (EditSupport.RecordingScope _ = editSupport.suppressRecording()) {
				super.undo();
//				undoManager.undo();
			} finally {
				Environment.setBatchMode(previousBatchMode);
			}
		}
	}

	public void redo() {
		if (canRedo()) {
			boolean previousBatchMode = Environment.isBatchMode();
			Environment.setBatchMode(true);
			try (EditSupport.RecordingScope _ = editSupport.suppressRecording()) {
//				undoManager.redo();
				super.redo();
			} finally {
				Environment.setBatchMode(previousBatchMode);
			}
		}
	}
	public boolean canUndo() {
		return !editSupport.hasPendingHistory() && super.canUndo();
	}
	public boolean canRedo() {
		return !editSupport.hasPendingHistory() && super.canRedo();
	}


	protected Map<NodeModel, Map<Object, Node>> nodeMapping = new HashMap<>();
	public void store(Node node,NodeModel model){
		Map<Object, Node> modelMap = nodeMapping.get(model);
		if (modelMap==null){
			modelMap=new HashMap<>();
			nodeMapping.put(model, modelMap);
		}
		modelMap.put(node.getImpl(), node);
	}
	public Node retrieve(Object impl,NodeModel model){
		Map<Object, Node> modelMap = nodeMapping.get(model);
		if (modelMap==null) return null;
		return modelMap.get(impl);
	}

	/**
	 * Collects every edit posted while a dialog is open so that the dialog can be accepted as one undoable
	 * action (OK) or discarded as a whole (Cancel).
	 */
	public final class Session implements EditListener {
		private final List<UndoableEdit> recorded = new ArrayList<>();
		private boolean open = true;

		private Session() {
		}

		@Override
		public synchronized void editPosted(UndoableEdit edit) {
			if (open && isDialogEditScope()) recorded.add(edit);
		}

		public synchronized boolean hasEdits() {
			return !recorded.isEmpty();
		}

		/** Keeps the edits and merges them into a single undo step. */
		public void commit() {
			List<UndoableEdit> mine = close();
			if (mine.size() < 2) return;
			synchronized (UndoController.this) {
				int size = edits.size();
				if (canRedo() || size < mine.size()) return;
				List<UndoableEdit> tail = edits.subList(size - mine.size(), size);
				for (int i = 0; i < mine.size(); i++)
					if (tail.get(i) != mine.get(i)) return;
				tail.clear();
				final String name = mine.get(mine.size() - 1).getPresentationName();
				javax.swing.undo.CompoundEdit compound = new javax.swing.undo.CompoundEdit() {
					@Override public String getPresentationName() { return name; }
				};
				for (UndoableEdit edit : mine) compound.addEdit(edit);
				compound.end();
				UndoController.super.addEdit(compound);
			}
		}

		/** Undoes every edit of the session and leaves nothing to redo. */
		public void rollback() {
			List<UndoableEdit> mine = close();
			int undone = 0;
			for (int i = mine.size() - 1; i >= 0; i--) {
				if (editToBeUndone() != mine.get(i) || !canUndo()) break;
				UndoController.this.undo();
				undone++;
			}
			synchronized (UndoController.this) {
				// the undone session edits are no longer redoable
				if (undone > 0 && canRedo())
					trimEdits(edits.size() - undone, edits.size() - 1);
			}
		}

		private synchronized List<UndoableEdit> close() {
			open = false;
			editSupport.removeEditListener(this);
			return new ArrayList<>(recorded);
		}
	}

	private static final ThreadLocal<int[]> DIALOG_EDIT_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

	/** Marks the current thread as applying an edit on behalf of an information dialog; pair with {@link #exitDialogEditScope()}. */
	public static void enterDialogEditScope() {
		DIALOG_EDIT_DEPTH.get()[0]++;
	}

	public static void exitDialogEditScope() {
		int[] depth = DIALOG_EDIT_DEPTH.get();
		if (depth[0] > 0) depth[0]--;
	}

	static boolean isDialogEditScope() {
		return DIALOG_EDIT_DEPTH.get()[0] > 0;
	}

	public Session beginSession() {
		Session session = new Session();
		editSupport.addEditListener(session);
		return session;
	}

	public void beginUpdate(){
		if (editSupport!=null) 	editSupport.beginUpdate();
	}
	public void endUpdate(){
		if (editSupport!=null) editSupport.endUpdate();
	}

	public List<String> getEditNames(){
		if (edits==null) return null;
		else{
			int nb=edits.size()>=50?50:edits.size();
			List<String> r=new ArrayList<>(nb);
			for (ListIterator<UndoableEdit> i=edits.listIterator(edits.size()-nb);i.hasNext();){
				UndoableEdit edit=i.next();
				if (edit!=null) r.add(edit.getPresentationName());
			}
			return r;
		}
	}

    public String getRedoName() {
    	UndoableEdit edit=editToBeRedone();
    	if (edit!=null) return edit.getPresentationName();
    	else return null;
    }
    public String getUndoName() {
    	UndoableEdit edit=editToBeUndone();
    	if (edit!=null) return edit.getPresentationName();
    	else return null;
    }


}
