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
package com.microproject.util;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.Iterator;


import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.TypedNodeIterator;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.key.HasKey;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Task;

/**
 * Utility functions for data manipulation
 */
public class DataUtils {
	
	public static Object extractObjectOfClass(Object object, Class objectClass) {
		if (object instanceof Assignment) {// if clicked on an assignment, set task
			if (objectClass == Task.class)
				object = ((Assignment)object).getTask();
			else if (objectClass == Resource.class)
				object = ((Assignment)object).getResource();
		}
		// assure type is treated by this dialog
		if (objectClass == Task.class  && !(object instanceof Task))
			return null;
		if (objectClass == Resource.class  && !(object instanceof Resource))
			return null;
		return object;
	}
	
	public static void extractObjectsOfClassFromNodeList(Collection<Object> result, Collection<?> nodeList, Class<?> objectClass) {
		result.clear();
		Iterator<?> i = nodeList.iterator();
		Object nodeObject;
		
		while (i.hasNext()) {
			nodeObject = ((Node)i.next()).getImpl();
			nodeObject = DataUtils.extractObjectOfClass(nodeObject,objectClass);
			if (nodeObject != null) {
				if (!result.contains(nodeObject)) // only add if not already in there
					result.add(nodeObject);
			}
				
		}
	}
	
	public static void forAllDo(Iterator<?> i, Consumer<Object> closure) {
		while (i.hasNext())
			closure.accept(i.next());
	}

	/**
	 * Apply a closure to one of the collections: if all is true, then use the allList, otherwise
	 * use the nodeList, and extract only the impls of the type clazz (or subclasses of clazz)
	 * @param closure
	 * @param all
	 * @param allList
	 * @param nodeList
	 * @param clazz
	 */
	public static void forAllDo(Consumer<Object> closure, boolean all, Collection<?> allList, Collection<?> nodeList, Class<?> clazz) {
		forAllDo(closure, all, allList.iterator(), nodeList, clazz);
	}
	public static void forAllDo(Consumer<Object> closure, boolean all, Iterator<?> allIterator, Collection<?> nodeList, Class<?> clazz) {
		Iterator<?> i = all ? allIterator : TypedNodeIterator.getInstance(nodeList,clazz);
		forAllDo(i,closure);
	}
	public static boolean nodeListContainsImplOfType(Collection<?> nodeList, Class<?> clazz) {
		if (nodeList == null)
			return false;
		Iterator<?> i = TypedNodeIterator.getInstance(nodeList,clazz);
		return i.hasNext();
	}

	public static String stringList(Collection<?> collection) {
		return collection.stream().map(value -> "" + ((HasKey) value).getId())
				.collect(java.util.stream.Collectors.joining(com.microproject.configuration.Settings.LIST_SEPARATOR));
	}

	public static String stringListWithMaxAndMessage(Collection<?> collection, int maxInList, String message) {
		if (collection.size() > maxInList)
			return java.text.MessageFormat.format(message, Integer.valueOf(collection.size()));
		return stringList(collection);
	}

}
