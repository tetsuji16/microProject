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

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.key.HasId;
import com.microproject.server.data.DataObject;
import com.microproject.strings.Messages;

/**
 *
 */
public class DebugUtils {
	private static final Logger logger = Logger.getLogger(DebugUtils.class.getName());

	/**
	 * 
	 */
	public DebugUtils() {
		super();
	}

	public static void dumpStack(String text) {
		try {
			throw new Exception(text);
		} catch (Exception e) {
			logger.log(Level.INFO, text, e);
		}
	}
	
	public static void dumpMapOfHasIdKeys(Map<?, ?> map) {
		Iterator<?> i = map.keySet().iterator();
		while (i.hasNext()) {
			Object key = i.next();
			String keyString =""+key;
			if (key instanceof DataObject)
				keyString = ""+((DataObject)key).getUniqueId();
			else if (key instanceof HasId)
				keyString = ""+((HasId)key).getId();
			logger.info("key=" + keyString + " value=" + map.get(key));
		}
	}
private static long LOW_MEMORY_LIMIT = 1000000L;
	public static boolean isMemoryOk(boolean popup) {
		long mem = Runtime.getRuntime().freeMemory();
		logger.info("Free Memory" + mem);
		if (mem < LOW_MEMORY_LIMIT) {
			System.gc();
			mem = Runtime.getRuntime().freeMemory();
			if (mem < LOW_MEMORY_LIMIT) {
				Environment.setOutOfMemory(true);
				if (popup)
					Alert.error(Messages.getString("Error.OutOfMemory"));
				return false;
			}
		}
		return true;
	}

}
