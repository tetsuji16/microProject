/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Local Java util logging for recoverable application errors. */
public class ErrorLogger {
	private static final Logger logger = Logger.getLogger(ErrorLogger.class.getName());
	private static final Set<String> loggedErrors = ConcurrentHashMap.newKeySet();

	public static void log(Exception exception) {
		logger.log(Level.SEVERE, getStackTrace(exception));
	}

	public static void log(String message, Exception exception) {
		log(message + "\n" + getStackTrace(exception));
	}

	public static void log(String message) {
		logger.log(Level.SEVERE, message);
	}

	public static String getStackTrace(Throwable throwable) {
		if (throwable == null) {
			try {
				throw new Exception("Simulated Exception");
			} catch (Exception exception) {
				throwable = exception;
			}
		}
		StringBuilder result = new StringBuilder();
		result.append(throwable).append('\n');
		for (StackTraceElement element : throwable.getStackTrace())
			result.append("\tat ").append(element).append('\n');
		Throwable cause = throwable.getCause();
		while (cause != null) {
			result.append("Caused by: ").append(cause).append('\n');
			for (StackTraceElement element : cause.getStackTrace())
				result.append("\tat ").append(element).append('\n');
			cause = cause.getCause();
		}
		return result.toString();
	}

	public static synchronized void logOnce(String type, String message, Exception exception) {
		if (loggedErrors.contains(type))
			return;
		loggedErrors.add(type);
		log(message, exception);
	}
}
