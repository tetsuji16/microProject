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

import java.util.Vector;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

/** Core-owned publisher for undo edits, including nested compound batches. */
public final class EditSupport {
	private final Vector<EditListener> listeners = new Vector<>();
	private final ThreadLocal<Activation> active = new ThreadLocal<>();
	private final ThreadLocal<Boolean> recordingSuppressed = new ThreadLocal<>();
	private final ArrayDeque<Group> committed = new ArrayDeque<>();
	private boolean draining;
	private boolean deliveryPending;
	private UndoableEdit deliveringEdit;

	/** One logical edit, retained independently while its model notifications publish. */
	public static final class Group {
		private final List<UndoableEdit> edits = new ArrayList<>();
		private int updates;
		private int publications;
		private boolean reserved;
		private boolean complete;
		private boolean recording = true;
		private boolean compound;
	}

	public final class RecordingScope implements AutoCloseable {
		private final Boolean previous = recordingSuppressed.get();
		private RecordingScope() {
			recordingSuppressed.set(Boolean.TRUE);
			Activation current = active.get();
			if (current != null) current.group.recording = false;
		}
		@Override public void close() {
			if (previous == null) recordingSuppressed.remove(); else recordingSuppressed.set(previous);
		}
	}

	/** Replayed events retain the command's suppression through delayed publication. */
	public RecordingScope suppressRecording() { return new RecordingScope(); }

	/** A thread-local binding only; closing it never calls observers. */
	public final class Activation implements AutoCloseable {
		private final Group group;
		private final Activation previous;
		private final boolean publication;
		private final boolean legacy;
		private Activation(Group group, boolean publication, boolean legacy) {
			this.group = group;
			this.publication = publication;
			this.legacy = legacy;
			previous = active.get();
			active.set(this);
		}
		public Group group() { return group; }
		@Override public void close() {
			if (active.get() != this) throw new IllegalStateException("Undo scopes closed out of order");
			if (previous == null) active.remove(); else active.set(previous);
		}
	}

	public synchronized Activation beginCommand() {
		Activation current = active.get();
		Group group = current != null && !current.publication ? current.group : new Group();
		group.compound = true;
		group.publications++;
		return new Activation(group, false, false);
	}

	public Activation activatePublication(Group group) {
		return new Activation(group, true, false);
	}

	/** Called before releasing the model barrier, so queue order is commit order. */
	public synchronized void reserveCommit(Group group) {
		if (group != null && !group.reserved) {
			group.reserved = true;
			committed.addLast(group);
		}
	}

	public synchronized int checkpoint(Group group) { return group.edits.size(); }

	public synchronized void discardSince(Group group, int checkpoint) {
		group.edits.subList(checkpoint, group.edits.size()).clear();
	}

	public void completePublication(Group group) {
		boolean ready;
		synchronized (this) {
			if (--group.publications < 0) throw new IllegalStateException("Undo publication already completed");
			finish(group);
			ready = group.complete && group.reserved;
		}
		if (ready) drain();
	}

	/** A pending commit must not allow undo of an older model state. */
	public synchronized boolean hasPendingHistory() { return !committed.isEmpty() || deliveryPending; }

	/** The owning controller acknowledges registration before later UI observers run. */
	public synchronized void historyRecorded(UndoableEdit edit) {
		if (edit == deliveringEdit) deliveryPending = false;
	}

	public synchronized void addEditListener(EditListener listener) {
		listeners.addElement(listener);
	}

	public synchronized void removeEditListener(EditListener listener) {
		listeners.removeElement(listener);
	}

	public void postEdit(UndoableEdit edit) {
		synchronized (this) {
			Activation current = active.get();
			if (Boolean.TRUE.equals(recordingSuppressed.get()) || (current != null && !current.group.recording)) return;
			if (current != null) {
				current.group.edits.add(edit);
				return;
			}
			Group group = new Group();
			group.edits.add(edit);
			reserveCommit(group);
			group.complete = true;
		}
		drain();
	}

	public synchronized void beginUpdate() {
		Activation current = active.get();
		if (current == null) current = new Activation(new Group(), false, true);
		current.group.compound = true;
		current.group.updates++;
	}

	public void endUpdate() {
		boolean ready;
		synchronized (this) {
			Activation current = active.get();
			if (current == null || current.group.updates == 0)
				throw new IllegalStateException("No undo update to end");
			current.group.updates--;
			if (current.legacy && current.group.updates == 0) current.close();
			finish(current.group);
			ready = current.group.complete && current.group.reserved;
		}
		if (ready) drain();
	}

	private void finish(Group group) {
		if (group.updates != 0 || group.publications != 0) return;
		group.complete = true;
		if (!group.edits.isEmpty()) reserveCommit(group);
	}

	/** Never hold this monitor or a model barrier while calling edit observers. */
	private void drain() {
		synchronized (this) {
			if (draining) return;
			draining = true;
		}
		Throwable failure = null;
		boolean ownsDrain = true;
		boolean notifySettled = false;
		try {
			while (true) {
				Group group;
				synchronized (this) {
					group = committed.peekFirst();
					if (group == null || !group.complete) {
						draining = false;
						ownsDrain = false;
						notifySettled = group == null;
						break;
					}
					committed.removeFirst();
					deliveryPending = !group.edits.isEmpty();
				}
				if (group.edits.isEmpty()) continue;
				UndoableEdit edit;
				if (!group.compound && group.edits.size() == 1) edit = group.edits.get(0);
				else {
					CompoundEdit compound = new CompoundEdit();
					for (UndoableEdit child : group.edits) compound.addEdit(child);
					compound.end();
					edit = compound;
				}
				synchronized (this) { deliveringEdit = edit; }
				try { fire(edit); }
				catch (Throwable current) {
					if (failure == null) failure = current; else failure.addSuppressed(current);
				}
				finally {
					synchronized (this) {
						deliveryPending = false;
						deliveringEdit = null;
					}
				}
			}
		} finally {
			if (ownsDrain) synchronized (this) {
				draining = false;
				deliveryPending = false;
				deliveringEdit = null;
			}
		}
		if (notifySettled) {
			try { fireHistorySettled(); }
			catch (Throwable current) {
				if (failure == null) failure = current; else failure.addSuppressed(current);
			}
		}
		if (failure instanceof Error error) throw error;
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure != null) throw new IllegalStateException("Undo publication failed", failure);
	}

	private void fire(UndoableEdit edit) {
		EditListener[] snapshot = listeners.toArray(new EditListener[0]);
		Throwable failure = null;
		for (EditListener listener : snapshot) {
			try { listener.editPosted(edit); }
			catch (Throwable current) {
				if (failure == null) failure = current; else failure.addSuppressed(current);
			}
		}
		if (failure instanceof Error error) throw error;
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure != null) throw new IllegalStateException("Undo observer failed", failure);
	}

	private void fireHistorySettled() {
		EditListener[] snapshot = listeners.toArray(new EditListener[0]);
		Throwable failure = null;
		for (EditListener listener : snapshot) {
			try { listener.historySettled(); }
			catch (Throwable current) {
				if (failure == null) failure = current; else failure.addSuppressed(current);
			}
		}
		if (failure instanceof Error error) throw error;
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure != null) throw new IllegalStateException("Undo history observer failed", failure);
	}
}
