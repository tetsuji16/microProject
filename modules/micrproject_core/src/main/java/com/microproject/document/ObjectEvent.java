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

import java.util.EventListener;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.microproject.field.Field;
import com.microproject.undo.NodeUndoInfo;

/**
 *
 */
public class ObjectEvent extends EventObject {
	private static final Logger logger = Logger.getLogger(ObjectEvent.class.getName());
    
	public Object getObject() {
		return object;
	}

	public boolean isUpdate() {
		return (eventType == UPDATE);
	}
	
	public boolean isDelete() {
		return (eventType == DELETE);
	}
	public boolean isCreate() {
		return (eventType == CREATE);
	}
	public int getType(){
		return eventType;
	}
	
    public NodeUndoInfo getInfo() {
        return info;
    }
	
	/**
	 * @param object The object to set.
	 */
	public void setObject(Object object) {
		this.object = object;
	}

    private static Object NULL_SOURCE = new Object();
	public static final int CREATE = 1;
	public static final int UPDATE = 0;
	public static final int DELETE = -1;
	
	private Object object;
	private int eventType;
	private NodeUndoInfo info;
	private Field field = null;
	
	public static ObjectEvent getInstance(Object source) {
		ObjectEvent objectEvent;
		try {
			objectEvent = (ObjectEvent) pool.borrowObject();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error", e);
			return null;
		}
		objectEvent.source = source;
		return objectEvent;
	}
	
	public static ObjectEvent getInstance(Object source, Object object, int eventType, NodeUndoInfo info) {
			ObjectEvent objectEvent = getInstance(source);
			objectEvent.object = object;
			objectEvent.eventType = eventType;
			objectEvent.info = info;
			return objectEvent;
	}
	
	public void recycle() {
		try {
			pool.returnObject(this);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error", e);
		}
	}
	
	private void reset() {
		source = null;
		eventType = 0;
		object = null;	
	}
	
	private ObjectEvent() {
		super(NULL_SOURCE); // it needs a source.  Will be modified later
		reset();
	}
	
	
	
	private static GenericObjectPool pool = new GenericObjectPool(new ObjectEventFactory());
	
	private static class ObjectEventFactory extends BasePoolableObjectFactory {
		public Object makeObject() { //claur
			return new ObjectEvent();
		}

		public void activateObject(Object arg0){ //claur
			ObjectEvent objectEvent = (ObjectEvent)arg0;
			objectEvent.reset();

		}
	}
	public interface Listener extends EventListener {
		public void objectChanged(ObjectEvent objectEvent);
	}	
	/**
	 * @return Returns the field.
	 */
	public Field getField() {
		return field;
	}
	/**
	 * @param field The field to set.
	 */
	public void setField(Field field) {
		this.field = field;
	}
}
