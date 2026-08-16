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
package com.microproject.functor;

import org.apache.commons.collections.Predicate;
import java.lang.reflect.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ReflectionPredicate implements Predicate {
	private static final Logger logger = Logger.getLogger(ReflectionPredicate.class.getName());

	Method method;
	/**
	 * 
	 */
	private ReflectionPredicate(Method method) {
		this.method = method;
	}

	public boolean evaluate(Object arg0) {
		try {
			return ((Boolean)method.invoke(arg0, new Object[0])).booleanValue();
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, "Reflection error", e);
		} catch (IllegalAccessException e) {
			logger.log(Level.WARNING, "Reflection error", e);
		} catch (InvocationTargetException e) {
			logger.log(Level.WARNING, "Reflection error", e);
		}
		return false;
	}

	public static ReflectionPredicate getInstance(Method method) {
		return new ReflectionPredicate(method);
	}

}
