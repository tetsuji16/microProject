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
package com.microproject.server.access;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.util.Environment;


public class ErrorLogger {
	public static boolean disabled = Environment.getStandAlone();
	private static final Logger logger = Logger.getLogger(ErrorLogger.class.getName());
	private static final Set<String> loggedErrors = ConcurrentHashMap.newKeySet();

	public static void log(final Exception e) {
		if (disabled)
			return;
		new Thread() {
			public void run() {
				Session session = SessionFactory.getInstance().getSession(false);
				if (session != null)
					session.logException(e);
			}}.start();
	}
	public static void log(String s,Exception e) {
		String result = s + "\n" + getStackTrace(e);
		log(result);
	}

	
	public static void log(final String s) {
		logger.log(Level.SEVERE, s);
		if (disabled)
			return;
		new Thread() {
			public void run() {
				Session session = SessionFactory.getInstance().getSession(false);
				if (session != null)
					session.logString(s);
			}}.start();
	}
	public static String getStackTrace(Throwable aThrowable) {
		if (aThrowable == null) {
			try {
				throw new Exception("Simulated Exception");
			} catch (Exception e) {
				aThrowable = e; // this will give a meaningful stack if none given
			}
		}
		StringBuilder result = new StringBuilder();
		result.append(aThrowable).append('\n');
		for (StackTraceElement element : aThrowable.getStackTrace()) {
			result.append("\tat ").append(element).append('\n');
		}
		Throwable cause = aThrowable.getCause();
		while (cause != null) {
			result.append("Caused by: ").append(cause).append('\n');
			for (StackTraceElement element : cause.getStackTrace()) {
				result.append("\tat ").append(element).append('\n');
			}
			cause = cause.getCause();
		}
		return result.toString();
	}
	public static synchronized void logOnce(String type, String s, Exception e) {
		if (loggedErrors.contains(type)) // if have already added it, do nothing
			return;
		loggedErrors.add(type);
		log(s,e);
	}

}
