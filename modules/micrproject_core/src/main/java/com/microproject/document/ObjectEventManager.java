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

import java.util.Iterator;

import com.microproject.field.Field;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.NormalTask;
import com.microproject.undo.NodeUndoInfo;


/**
 *
 */
public class ObjectEventManager {
//	 Create the listener list
    protected javax.swing.event.EventListenerList listenerList =
        new javax.swing.event.EventListenerList();

    // This methods allows classes to register for ObjectEvents
    public void addListener(ObjectEvent.Listener listener) {
        listenerList.add(ObjectEvent.Listener.class, listener);
    }

    // This methods allows classes to unregister for ObjectEvents
    public void removeListener(ObjectEvent.Listener listener) {
        listenerList.remove(ObjectEvent.Listener.class, listener);
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
    	
    	if (object instanceof NormalTask && field.isApplicable(Assignment.class)) { // fix for bug 258
    		Iterator i = ((NormalTask)object).getAssignments().iterator();
    		while (i.hasNext()) {
    			fireUpdateEvent(source,i.next(),field);
    		}
    	}
    }
	
	
    
    private void fire(Object source, Object object, int eventType, NodeUndoInfo info) {
    	ObjectEvent evt = ObjectEvent.getInstance(source,object,eventType,info);
    	fire(evt);
    }
    
    public void fire(ObjectEvent evt) {    	
        Object[] listeners = listenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i]==ObjectEvent.Listener.class) {
//            	if (evt.isUpdate()) System.out.println("ObjectEvent update: object="+evt.getObject()+", field="+evt.getField()+", source="+evt.getSource()+", info="+evt.getInfo()+", listener="+listeners[i+1]);
//            	else if (evt.isCreate()) System.out.println("ObjectEvent create: object="+evt.getObject()+", field="+evt.getField()+", source="+evt.getSource()+", info="+evt.getInfo()+", listener="+listeners[i+1]);
//            	else if (evt.isDelete()) System.out.println("ObjectEvent delete: object="+evt.getObject()+", field="+evt.getField()+", source="+evt.getSource()+", info="+evt.getInfo()+", listener="+listeners[i+1]);
//            	else System.out.println("ObjectEvent: object="+evt.getObject()+", field="+evt.getField()+", source="+evt.getSource()+", info="+evt.getInfo()+", listener="+listeners[i+1]);
                ((ObjectEvent.Listener)listeners[i+1]).objectChanged(evt);
            }
        }
        evt.recycle();
    }
}
