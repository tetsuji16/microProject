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
package com.microproject.pm.scheduling;

import com.microproject.util.DeferredEventBatch;
import com.microproject.util.ListenerRegistry;
import java.util.List;


/**
 * Used to notify that the current document selection has changed
 */
public class ScheduleEventManager {

    private final ListenerRegistry<ScheduleEventListener> listeners = new ListenerRegistry<>();
    private final DeferredEventBatch<ScheduleEvent> batch = new DeferredEventBatch<>(
        "No schedule-event batch is active", "Invalid schedule-event checkpoint");

    /** Defers schedule observer callbacks until a surrounding model transaction commits. */
    public void beginBatch() {
        batch.begin();
    }

    /** Number of deferred facts currently waiting in this nested batch stack. */
    public int pendingEventCount() {
        return batch.pendingCount();
    }

    /** Discards facts added after an atomic command's checkpoint without affecting its parent batch. */
    public void discardPendingEventsAfter(int checkpoint) {
        batch.discardAfter(checkpoint);
    }

    /** Publishes deferred events on commit, or drops them after rollback. */
    public void endBatch(boolean publish) {
        publishBatch(detachBatch(publish));
    }

    /** Detaches one completed outer batch without invoking observer callbacks. */
    public PendingBatch detachBatch(boolean publish) {
        DeferredEventBatch.Detached<EventFacts> detached = batch.detach(publish,
            event -> new EventFacts(event.getSource(), event.getType(), event.getObject(),
                event.getSnapshot(), event.isSaveSnapshot()));
        return detached == null ? null : new PendingBatch(detached.events(), detached.publish());
    }

    /** Publishes a batch detached after its model revision was committed. */
    public void publishBatch(PendingBatch batch) {
        publishBatch(batch, -1L);
    }

    /** Publishes detached facts with the exact revision committed with their event batch. */
    public void publishBatch(PendingBatch batch, long publicationRevision) {
        if (batch == null || !batch.publish) return;
        Throwable failure = null;
        for (EventFacts facts : batch.events) {
            try {
                ScheduleEvent event = new ScheduleEvent(facts.source, facts.type, facts.object);
                event.setSnapshot(facts.snapshot);
                event.setSaveSnapshot(facts.saveSnapshot);
                event.setPublicationRevision(publicationRevision);
                dispatch(event);
            } catch (Throwable current) {
                if (failure == null) failure = current;
                else failure.addSuppressed(current);
            }
        }
        if (failure instanceof Error error) throw error;
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure != null) throw new IllegalStateException("Deferred schedule-event publication failed", failure);
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

    private record EventFacts(Object source, String type, Object object, Integer snapshot,
        boolean saveSnapshot) { }

    // This methods allows classes to register for ObjectEvents
    public void addListener(ScheduleEventListener listener) {
        listeners.add(listener);
    }

    // This methods allows classes to unregister for ObjectEvents
    public void removeListener(ScheduleEventListener listener) {
        listeners.remove(listener);
    }
    public void fireBaselineChanged(Object source, Object object, Integer snapshot, boolean save) {
    	ScheduleEvent evt = new ScheduleEvent(source,ScheduleEvent.BASELINE,object);
    	evt.setSnapshot(snapshot);
    	evt.setSaveSnapshot(save);
    	fire(evt);
    }
    
    public void fire(Object source, String type) {
    	fire(source,type,null);
    }
    public void fire(Object source, String type, Object object) {
    	ScheduleEvent evt = new ScheduleEvent(source,type,object);
    	fire (evt);
    }
    private void fire(ScheduleEvent evt) {
		if (batch.tryDefer(() -> evt)) return;
		dispatch(evt);
	}
	private void dispatch(ScheduleEvent evt) {
		for (ScheduleEventListener listener : listeners.snapshot())
			listener.scheduleChanged(evt);
    }
    

    
}
