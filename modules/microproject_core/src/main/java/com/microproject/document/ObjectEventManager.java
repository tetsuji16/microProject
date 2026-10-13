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
package com.microproject.document;

import com.microproject.association.Association;
import com.microproject.field.Field;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.NormalTask;
import com.microproject.undo.NodeUndoInfo;
import com.microproject.util.ListenerRegistry;
import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class ObjectEventManager {
    private final ListenerRegistry<ObjectEvent.Listener> listeners = new ListenerRegistry<>();
    private final List<ObjectEvent> deferredEvents = new ArrayList<>();
    private int batchDepth;
    private boolean rollbackOnly;

    /** Defers observer callbacks until a surrounding model transaction commits. */
    public synchronized void beginBatch() {
        batchDepth++;
    }

    /** Number of deferred facts currently waiting in this nested batch stack. */
    public synchronized int pendingEventCount() {
        return deferredEvents.size();
    }

    /** Publishes deferred events on commit, or releases them without notifying observers on rollback. */
    public void endBatch(boolean publish) {
        publishBatch(detachBatch(publish));
    }

    /** Detaches one completed outer batch without invoking observer callbacks. */
    public PendingBatch detachBatch(boolean publish) {
        List<EventFacts> events = null;
        synchronized (this) {
            if (batchDepth == 0) throw new IllegalStateException("No object-event batch is active");
            if (!publish) rollbackOnly = true;
            if (--batchDepth == 0) {
                events = new ArrayList<>(deferredEvents.size());
                for (ObjectEvent event : deferredEvents) {
                    events.add(new EventFacts(event.getSource(), event.getObject(), event.getType(),
                        event.getInfo(), event.getField()));
                    event.recycle();
                }
                deferredEvents.clear();
                publish = !rollbackOnly;
                rollbackOnly = false;
            }
        }
        return events == null ? null : new PendingBatch(List.copyOf(events), publish);
    }

    /** Publishes a batch detached after its model revision was committed. */
    public void publishBatch(PendingBatch batch) {
        publishBatch(batch, -1L);
    }

    /** Publishes detached facts with the exact revision committed with their event batch. */
    public void publishBatch(PendingBatch batch, long publicationRevision) {
        if (batch == null) return;
        if (!batch.publish) return;
        Throwable failure = null;
        for (EventFacts facts : batch.events) {
            try {
                ObjectEvent event = ObjectEvent.getInstance(facts.source, facts.object,
                    facts.eventType, facts.info);
                if (event == null) throw new IllegalStateException("Could not allocate an object event");
                event.setField(facts.field);
                event.setPublicationRevision(publicationRevision);
                dispatch(event);
            } catch (Throwable current) {
                if (failure == null) failure = current;
                else failure.addSuppressed(current);
            }
        }
        if (failure instanceof Error error) throw error;
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure != null) throw new IllegalStateException("Deferred object-event publication failed", failure);
    }

    public static final class PendingBatch {
        private final List<EventFacts> events;
        private final boolean publish;

        private PendingBatch(List<EventFacts> events, boolean publish) {
            this.events = events;
            this.publish = publish;
        }

        public boolean isPublishable() {
            return publish;
        }
    }

    private record EventFacts(Object source, Object object, int eventType,
        NodeUndoInfo info, Field field) { }

    // This methods allows classes to register for ObjectEvents
    public void addListener(ObjectEvent.Listener listener) {
        listeners.add(listener);
    }

    // This methods allows classes to unregister for ObjectEvents
    public void removeListener(ObjectEvent.Listener listener) {
        listeners.remove(listener);
    }



    public void fireCreateEvent(Object source, Object object) {
    	fire(source,object,ObjectEvent.CREATE,null);
    }
	
    public void fireCreateEvent(Object source, Object object, NodeUndoInfo info) {
    	fire(source,object,ObjectEvent.CREATE,info);
    }
    
    public void fireDeleteEvent(Object source, Object object) {
    	fire(source,object,ObjectEvent.DELETE,null);
    }

    public void fireDeleteEvent(Object source, Object object, NodeUndoInfo info) {
    	fire(source,object,ObjectEvent.DELETE,info);
    }

    
    public void fireUpdateEvent(Object source, Object object) {
    	fire(source,object,ObjectEvent.UPDATE,null);
    }
    
    public void fireUpdateEvent(Object source, Object object, NodeUndoInfo info) {
    	fire(source,object,ObjectEvent.UPDATE,info);
    }

    public void fireUpdateEvent(Object source, Object object, Field field) {
    	ObjectEvent evt = ObjectEvent.getInstance(source,object,ObjectEvent.UPDATE,null);
    	evt.setField(field);
    	fire(evt);
    	
		if (object instanceof NormalTask task && field.isApplicable(Assignment.class)) { // fix for bug 258
			for (Association assignment : task.getAssignments()) {
				fireUpdateEvent(source, assignment, field);
			}
		}
    }
	
	
    
    private void fire(Object source, Object object, int eventType, NodeUndoInfo info) {
    	ObjectEvent evt = ObjectEvent.getInstance(source,object,eventType,info);
    	fire(evt);
    }
    
    public void fire(ObjectEvent evt) {
        synchronized (this) {
            if (batchDepth > 0) {
                deferredEvents.add(evt);
                return;
            }
        }
        dispatch(evt);
    }

    private void dispatch(ObjectEvent evt) {
        try {
            for (ObjectEvent.Listener listener : listeners.snapshot())
                listener.objectChanged(evt);
        } finally {
            evt.recycle();
        }
    }
}
