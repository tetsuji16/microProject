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
package com.microproject.field;

import java.util.HashSet;
import java.util.Iterator;

import com.microproject.configuration.Configuration;
import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;


/**
 * This abstract class manages a list of input fields and output fields for an algorithm as well as field updating
 */
public abstract class AlgorithmFieldUpdater extends Thread {
	Object eventSource;
	Document document;
	public AlgorithmFieldUpdater(Object eventSource, Document document) {
		super();
		this.eventSource = eventSource;
		this.document = document;
	}

	public void run() {
		fireOutputEvents(eventSource,null);
	}	
/**
 * Fire a field event for each output field
 * @param algo
 * @param object
 */	public void fireOutputEvents(Object algo, Object object) {
		Iterator i = outputFields.iterator();
	
		ObjectEvent objectEvent = ObjectEvent.getInstance(algo);
		objectEvent.setObject(object);
		Field field;
		while (i.hasNext()) {
			if (isInterrupted()) // if interrupted, don't keep going
				break;
			field = (Field)i.next();
			objectEvent.setField(field);
			document.getObjectEventManager().fire(objectEvent);
		}
		objectEvent.recycle();
	}

	public boolean inputContains(Field field) {
		return inputFields.contains(field);
	}

	public boolean outputContains(Field field) {
		return outputFields.contains(field);
	}

	protected HashSet inputFields = new HashSet();
	protected HashSet outputFields = new HashSet();
	
	protected void addInputField(String fieldId) {
		Field field = Configuration.getFieldFromId(fieldId);
		if (field == null) {
			Field.log.error("could not add input field (probably not yet implemented)" + fieldId);
		} else {
			inputFields.add(field);
		}
	}
	protected void addOutputField(String fieldId) {
		Field field = Configuration.getFieldFromId(fieldId);
		if (field == null) {
			Field.log.error("could not add output field (probably not yet implemented)" + fieldId);
		} else {
			outputFields.add(field);
		}
	}
	
}
