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



/**
 * Used to notify that the current document selection has changed
 */
public class ScheduleEventManager {

    protected javax.swing.event.EventListenerList listenerList =
        new javax.swing.event.EventListenerList();

    // This methods allows classes to register for ObjectEvents
    public void addListener(ScheduleEventListener listener) {
        listenerList.add(ScheduleEventListener.class, listener);
    }

    // This methods allows classes to unregister for ObjectEvents
    public void removeListener(ScheduleEventListener listener) {
        listenerList.remove(ScheduleEventListener.class, listener);
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
        Object[] listeners = listenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
//        long t0=System.currentTimeMillis(),t1;
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i]==ScheduleEventListener.class) {
                ((ScheduleEventListener)listeners[i+1]).scheduleChanged(evt);
//                t1=System.currentTimeMillis();
//                System.out.println("\tSchedule events ran in "+(t1-t0)+"ms"+" ("+listeners[i+1].getClass()+")");
//                t0=t1;
            }
        }
    }
    

    
}
