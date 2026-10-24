/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class ErrorLoggerTest {
	@Test
	void formatsCausesAndSynthesizesAStackForNull() {
		IllegalStateException cause = new IllegalStateException("inner");
		RuntimeException exception = new RuntimeException("outer", cause);

		String stack = ErrorLogger.getStackTrace(exception);
		assertTrue(stack.startsWith("java.lang.RuntimeException: outer\n\tat "));
		assertTrue(stack.contains("Caused by: java.lang.IllegalStateException: inner\n\tat "));
		assertTrue(ErrorLogger.getStackTrace(null).contains("java.lang.Exception: Simulated Exception"));
	}

	@Test
	void logsOnlyTheFirstOccurrenceOfEachType() {
		Logger logger = Logger.getLogger(ErrorLogger.class.getName());
		CaptureHandler handler = new CaptureHandler();
		Level previousLevel = logger.getLevel();
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);
		try {
			String type = "error-logger-test-" + System.nanoTime();
			ErrorLogger.logOnce(type, "first", null);
			ErrorLogger.logOnce(type, "second", null);
			assertEquals(1, handler.count);
			assertTrue(handler.message.contains("first"));
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(previousLevel);
		}
	}

	private static final class CaptureHandler extends Handler {
		private int count;
		private String message;

		@Override
		public void publish(LogRecord record) {
			count++;
			message = record.getMessage();
		}

		@Override public void flush() { }
		@Override public void close() { }
	}
}
