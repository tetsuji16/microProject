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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Static class holding common object pools.
 * Currently I am not using the KeyedObjectPool.  Using specific versions is no doubt a little faster.
 */
public class Factory {
	private static final Logger logger = Logger.getLogger(Factory.class.getName());
	// for easy naming, the factory is named GregorianCalendarFactory
	public static class GregorianCalendarPool extends BasePoolableObjectFactory {
		private static GenericObjectPool pool =  new GenericObjectPool(new GregorianCalendarPool());
		public Object makeObject() { //claur
			return DateTime.calendarInstance();
		}
		public static GregorianCalendar getInstance() {
			try {
				return (GregorianCalendar) pool.borrowObject();
			} catch (Exception e) {
				logger.log(Level.WARNING, "Failed to borrow GregorianCalendar from pool", e);
				return null;
			}
		}
		
		public static void recycle(GregorianCalendar object) {
			try {
				pool.returnObject(object);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Failed to recycle GregorianCalendar into pool", e);
			}
		}
	}

	public static class DatePool extends BasePoolableObjectFactory {
		private static GenericObjectPool pool =  new GenericObjectPool(new DatePool());
		public Object makeObject() { //claur
			return new Date();
		}
		
		public static Date getInstance(long millis) {
			Date date = getInstance();
			date.setTime(millis);
			return date;
		}
		public static Date getInstance() {
			try {
				return (Date) pool.borrowObject();
			} catch (Exception e) {
				logger.log(Level.WARNING, "Failed to borrow Date from pool", e);
				return null;
			}
		}
		
		public static void recycle(Date object) {
			try {
				pool.returnObject(object);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Failed to recycle Date into pool", e);
			}
		}
	}	


	
}
