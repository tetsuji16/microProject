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
package com.microproject.pm.graphic.frames;

import java.util.EventListener;
import java.util.EventObject;


/**
 * Used to notify that the current document selection has changed
 */
public class DocumentSelectedEvent extends EventObject {
	private static final long serialVersionUID = -1882234298777797617L;

	private static DocumentFrame cachedPrevious = null;
	
	private DocumentFrame current;
	private DocumentFrame previous;
	
	/**
	 * @param arg0
	 */
	public DocumentSelectedEvent(Object source, DocumentFrame current) {
		super(source);
		this.current = current;
		this.previous = cachedPrevious;
		cachedPrevious = current;
	}

    protected static javax.swing.event.EventListenerList listenerList =
        new javax.swing.event.EventListenerList();

    // This methods allows classes to register for ObjectEvents
    public static void addListener(Listener listener) {
        listenerList.add(Listener.class, listener);
    }

    // This methods allows classes to unregister for ObjectEvents
    public static void removeListener(Listener listener) {
        listenerList.remove(Listener.class, listener);
    }

	public interface Listener extends EventListener {
		public void documentSelected(DocumentSelectedEvent evt);
	}	
    public static void fire(Object source, DocumentFrame current) {
//    	if (current == cachedPrevious)
//    		return;
    	DocumentSelectedEvent evt = new DocumentSelectedEvent(source,current);
        Object[] listeners = listenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i]==Listener.class) {
                ((Listener)listeners[i+1]).documentSelected(evt);
            }
        }
    }
    
    

	/**
	 * @return Returns the current.
	 */
	public DocumentFrame getCurrent() {
		return current;
	}
	/**
	 * @return Returns the previous.
	 */
	public DocumentFrame getPrevious() {
		return previous;
	}
}

