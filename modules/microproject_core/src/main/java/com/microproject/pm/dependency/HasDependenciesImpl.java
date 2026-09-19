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
package com.microproject.pm.dependency;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.microproject.association.AssociationList;
import com.microproject.pm.calendar.HasCalendar;

/**
 *
 */


public class HasDependenciesImpl implements HasDependencies, Serializable {
	private static final Logger logger = Logger.getLogger(HasDependenciesImpl.class.getName());
	private HasCalendar hasCalendar;
	public HasDependenciesImpl(HasCalendar hasCalendar) {
		this.hasCalendar = hasCalendar;
		
	}
	public AssociationList getPredecessorList() {
		return predecessors;
	}
	public AssociationList getSuccessorList() {
		return successors;
	}
	private transient AssociationList predecessors = new AssociationList();
	private transient AssociationList successors = new AssociationList();

	public Consumer<Object> forAllPredecessors(Consumer<Object> visitor) {
		return value -> ((HasDependencies) value).getPredecessorList().getList().forEach(visitor);
	}
	public Consumer<Object> forAllSuccesssors(Consumer<Object> visitor) {
		return value -> ((HasDependencies) value).getSuccessorList().getList().forEach(visitor);
	}
	
	public HasCalendar getHasCalendar() {
		return hasCalendar;
	}
	
	public boolean dependsOn(HasDependencies other) {
		logger.log(Level.WARNING, "dependsOn should not be called on HasDependenciesImpl");
		return false;
	}
	
	private void writeObject(ObjectOutputStream s) throws IOException {
	    s.defaultWriteObject();
	}
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
	    s.defaultReadObject();
	    predecessors = new AssociationList();
	    successors = new AssociationList();
	}
	public AssociationList getDependencyList(boolean pred) {
		return pred ? predecessors : successors;
	}
	
}
