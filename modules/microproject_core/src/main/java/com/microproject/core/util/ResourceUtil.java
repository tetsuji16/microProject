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
package com.microproject.core.util;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Laurent Chretienneau
 *
 */
public class ResourceUtil {
	private static final Logger logger = Logger.getLogger(ResourceUtil.class.getName());

	public static ClassLoader getClassLoader(){
		return ResourceUtil.class.getClassLoader();
	}

	public static Object createObject(String simpleClassName, String...packages){
		for (String p : packages){
			try {
				Class<?> c=Class.forName(p+"."+simpleClassName, true, getClassLoader());
				return c.getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException e) {
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
				logger.log(Level.WARNING, "Failed to instantiate " + p + "." + simpleClassName, e);
			} catch (InvocationTargetException e) {
				return ResourceUtil.<RuntimeException>rethrow(e.getCause());
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> Object rethrow(Throwable cause) throws T {
		throw (T) cause;
	}
}
