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
package com.microproject.transaction;

import java.util.ArrayList;
import java.util.List;

import com.microproject.document.Document;


/**
 * Takes care of notifying the creation or deletion of objects
 */
public class MultipleTransactionManager {
	private static int counter=0;
	public static int depth = 0;
    private final Object listenerLock = new Object();
    private final List<MultipleTransaction.Listener> listeners = new ArrayList<>();

    // This methods allows classes to register for MultipleTransactions
    public void addListener(MultipleTransaction.Listener listener) {
        if (listener == null)
            return;
        synchronized (listenerLock) {
            listeners.add(listener);
        }
    }

    // This methods allows classes to unregister for MultipleTransactions
    public void removeListener(MultipleTransaction.Listener listener) {
        if (listener == null)
            return;
        synchronized (listenerLock) {
            int index = listeners.lastIndexOf(listener);
            if (index >= 0)
                listeners.remove(index);
        }
    }
    
/**
 * Fire either the beginning or the end of a long transaction
 * @param source the source of the event
 * @param id if 0 an new one will be generated and returned
 * @param begin true if transaction start
 * @return id generated if begin, or same is input
 */    
    public int fire(Document source, int id, boolean begin) {
    	if (id == 0)
    		id = ++counter;
    	if (begin)
    		depth++;
    	else
    		depth--;
    	MultipleTransaction evt = MultipleTransaction.getInstance(source,id, begin, depth);
        List<MultipleTransaction.Listener> listenersSnapshot;
        synchronized (listenerLock) {
            listenersSnapshot = List.copyOf(listeners);
        }
        for (MultipleTransaction.Listener listener : listenersSnapshot)
            listener.multipleTransaction(evt);
        return id;
    }
}
