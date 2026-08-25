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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import javax.swing.undo.CompoundEdit;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.util.Environment;

/**
 *
 */
public class UndoController extends UndoManager implements UndoableEditListener{

	protected transient UndoableEditSupport editSupport;
	private transient CopyOnWriteArrayList<UndoStateListener> stateListeners = new CopyOnWriteArrayList<>();
	private transient int updateDepth;
	private transient int stateNotificationDepth;
	private transient UndoStateEvent.Cause pendingStateCause;
	private transient ThreadLocal<ArrayDeque<EditCapture>> editCaptures = ThreadLocal.withInitial(ArrayDeque::new);
	//protected transient UndoManager undoManager;
	/**
	 *
	 */
	public UndoController() {
		editSupport=new UndoableEditSupport() {
			@Override protected CompoundEdit createCompoundEdit() { return new AtomicCompoundEdit(); }
		};
		//undoManager=new UndoManager();
		editSupport.addUndoableEditListener(this);
	}
	public void undoableEditHappened(UndoableEditEvent e){
		//System.out.println("undoableEditHappened");
		UndoableEdit edit=e.getEdit();
//		undoManager.addEdit(edit);
		EditCapture capture = currentCapture();
		if (capture != null) {
			capture.add(edit);
			return;
		}
		super.addEdit(edit);
		fireStateChanged(UndoStateEvent.Cause.EDIT_ADDED);
	}

	public void clear(){
//		undoManager.discardAllEdits();
		super.discardAllEdits();
		nodeMapping.clear();
		fireStateChanged(UndoStateEvent.Cause.CLEARED);
	}

	public UndoableEditSupport getEditSupport() {
		return editSupport;
	}

	/** Adds a command's already-built edit at the transaction commit point. */
	public synchronized void commitEdit(UndoableEdit edit) {
		if (edit == null)
			throw new IllegalArgumentException("edit is required");
		if (updateDepth > 0) {
			editSupport.postEdit(edit);
			return;
		}
		EditCapture capture = currentCapture();
		if (capture != null) {
			capture.add(edit);
			return;
		}
		if (!super.addEdit(edit))
			throw new IllegalStateException("Undo manager rejected committed edit");
		fireStateChanged(UndoStateEvent.Cause.EDIT_ADDED);
	}

	/** Captures legacy-posted edits so a ModelTransaction can commit or compensate them atomically. */
	public EditCapture captureEdits() {
		EditCapture capture = new EditCapture(this);
		captures().push(capture);
		return capture;
	}

	private EditCapture currentCapture() { return captures().peek(); }
	private ArrayDeque<EditCapture> captures() {
		if (editCaptures == null) editCaptures = ThreadLocal.withInitial(ArrayDeque::new);
		return editCaptures.get();
	}

	public static final class EditCapture implements AutoCloseable {
		private final UndoController owner;
		private final List<UndoableEdit> edits = new ArrayList<>();
		private boolean closed;
		private EditCapture(UndoController owner) { this.owner = owner; }
		private void add(UndoableEdit edit) { edits.add(edit); }
		public UndoableEdit edit() {
			if (!closed) throw new IllegalStateException("capture must be closed first");
			if (edits.isEmpty()) return null;
			if (edits.size() == 1) return edits.get(0);
			AtomicCompoundEdit compound = new AtomicCompoundEdit();
			for (UndoableEdit edit : edits) compound.addEdit(edit);
			compound.end();
			return compound;
		}
		@Override public void close() {
			if (closed) return;
			if (owner.captures().peek() != this) throw new IllegalStateException("edit captures must close LIFO");
			owner.captures().pop();
			closed = true;
		}
	}

	public void undo() {
		if (canUndo()) {
			boolean previousBatchMode = Environment.isBatchMode();
			Environment.setBatchMode(true);
			try {
				super.undo();
//				undoManager.undo();
			} finally {
				Environment.setBatchMode(previousBatchMode);
			}
			fireStateChanged(UndoStateEvent.Cause.UNDO);
		}
	}

	public void redo() {
		if (canRedo()) {
			boolean previousBatchMode = Environment.isBatchMode();
			Environment.setBatchMode(true);
			try {
//				undoManager.redo();
				super.redo();
			} finally {
				Environment.setBatchMode(previousBatchMode);
			}
			fireStateChanged(UndoStateEvent.Cause.REDO);
		}
	}
	public boolean canUndo() {
		return super.canUndo();//undoManager.canUndo();
	}
	public boolean canRedo() {
		return super.canRedo();//undoManager.canRedo();
	}


	protected Map nodeMapping=new HashMap();
	public void store(Node node,NodeModel model){
		HashMap modelMap =(HashMap) nodeMapping.get(model);
		if (modelMap==null){
			modelMap=new HashMap();
			nodeMapping.put(model, modelMap);
		}
		modelMap.put(node.getImpl(), node);
	}
	public Node retrieve(Object impl,NodeModel model){
		HashMap modelMap =(HashMap) nodeMapping.get(model);
		if (modelMap==null) return null;
		return (Node)modelMap.get(impl);
	}

	public synchronized void beginUpdate(){
		if (editSupport!=null) {
			editSupport.beginUpdate();
			updateDepth++;
		}
	}
	public synchronized void endUpdate(){
		if (editSupport!=null) {
			if (updateDepth <= 0)
				throw new IllegalStateException("endUpdate without beginUpdate");
			try {
				editSupport.endUpdate();
			} finally {
				updateDepth--;
			}
		}
	}

	public void addUndoStateListener(UndoStateListener listener) {
		if (listener != null)
			stateListeners().addIfAbsent(listener);
	}

	public void removeUndoStateListener(UndoStateListener listener) {
		stateListeners().remove(listener);
	}

	/** Defers observer callbacks while a transaction installs Undo and its revision. */
	public StateNotificationScope deferStateNotifications() {
		stateNotificationDepth++;
		return new StateNotificationScope(this);
	}

	public static final class StateNotificationScope implements AutoCloseable {
		private UndoController owner;
		private StateNotificationScope(UndoController owner) { this.owner = owner; }
		@Override public void close() {
			if (owner == null) return;
			UndoController value = owner;
			owner = null;
			if (--value.stateNotificationDepth == 0 && value.pendingStateCause != null) {
				UndoStateEvent.Cause cause = value.pendingStateCause;
				value.pendingStateCause = null;
				value.fireStateChanged(cause);
			}
		}
	}

	private void fireStateChanged(UndoStateEvent.Cause cause) {
		if (stateNotificationDepth > 0) {
			pendingStateCause = cause;
			return;
		}
		UndoStateEvent event = new UndoStateEvent(this, cause, canUndo(), canRedo(), getUndoName(), getRedoName());
		for (UndoStateListener listener : stateListeners())
			try {
				listener.undoStateChanged(event);
			} catch (Throwable ignored) {
				// UI observers cannot invalidate an edit already added to the stack.
			}
	}

	private CopyOnWriteArrayList<UndoStateListener> stateListeners() {
		if (stateListeners == null)
			stateListeners = new CopyOnWriteArrayList<>();
		return stateListeners;
	}

	public List<String> getEditNames(){
		if (edits==null) return null;
		else{
			int nb=edits.size()>=50?50:edits.size();
			List<String> r=new ArrayList<String>(nb);
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
