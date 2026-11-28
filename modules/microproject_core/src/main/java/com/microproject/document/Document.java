/*******************************************************************************
 * MIT License
 *
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

import com.microproject.model.calendar.WorkCalendar;


/**
 * A project document: the object and selection event hubs, the default
 * calendar of the project and the dirty flags that drive saving.
 */
public interface Document{
	void addObjectListener(ObjectEvent.Listener listener);
	void removeObjectListener(ObjectEvent.Listener listener);
	ObjectEventManager getObjectEventManager();
	ObjectSelectionEventManager getObjectSelectionEventManager();

	/**
	 * Announces that {@code object} changed, so views refresh it.
	 *
	 * @param source the object that reports the change, which may differ from
	 *               the changed object
	 * @param object the changed object; null is passed through unchanged
	 */
	void fireUpdateEvent(Object source, Object object);

	/**
	 * Opens or closes a transaction that groups several object changes into one
	 * notification.
	 *
	 * @param id identifier of the transaction, echoed to the listeners
	 * @param begin true to open the transaction, false to close it
	 * @return the nesting depth after the call; 0 means no transaction is open
	 */
	int fireMultipleTransaction(int id, boolean begin);

	WorkCalendar getDefaultCalendar();

	/**
	 * @return true when the document has unsaved changes
	 */
	boolean isGroupDirty();

	/**
	 * Marks the document as having or not having unsaved changes.
	 *
	 * @param dirty true when the document must be saved
	 */
	void setGroupDirty(boolean dirty);

	/**
	 * Sets the dirty flag of every child of the document, typically before a
	 * full recalculation.
	 *
	 * @param dirty true when the children must be treated as changed
	 */
	void setAllChildrenDirty(boolean dirty);


}
