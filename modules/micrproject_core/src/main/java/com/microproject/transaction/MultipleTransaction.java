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

import java.util.EventListener;
import java.util.EventObject;

import com.microproject.document.Document;

/**
 * Takes care of notifying the creation or deletion of objects
 */
public class MultipleTransaction extends EventObject {
    private boolean begin;
    private int id;
    private int depth;
	public boolean isBegin() {
		return begin;
	}
	
	public Document getDocument() {
		return (Document)getSource();
	}
	
	
	public static MultipleTransaction getInstance(Document source, int id, boolean begin, int depth) {
		return new MultipleTransaction(source, id, begin, depth);
	}
	
	private MultipleTransaction(Document source, int id, boolean begin, int depth) {
		super(source);
		this.id = id;
		this.begin = begin;
		this.depth = depth;
	}
	
	
	
	public interface Listener extends EventListener {
		public void multipleTransaction(MultipleTransaction objectEvent);
	}	
	/**
	 * @return Returns the depth.
	 */
	public final int getDepth() {
		return depth;
	}
	/**
	 * @return Returns the id.
	 */
	public final int getId() {
		return id;
	}
	
	public boolean isFinalEnd() {
		return depth == 0;
	}
}
