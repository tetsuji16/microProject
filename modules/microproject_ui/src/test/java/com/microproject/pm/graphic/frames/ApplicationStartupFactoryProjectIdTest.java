package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Issue #186: the --projectId option was parsed with an unguarded
 * Long.parseLong, so a non-numeric value crashed startup with
 * NumberFormatException.
 */
class ApplicationStartupFactoryProjectIdTest {
	@Test
	void startupOptionLoggingPreservesMapAndArgumentOrder() {
		HashMap<String, Object> opts = new LinkedHashMap<>();
		opts.put("first", List.of("a", "b"));
		opts.put("second", "c");
		Logger logger = Logger.getLogger(ApplicationStartupFactory.class.getName());
		Level previousLevel = logger.getLevel();
		boolean previousUseParentHandlers = logger.getUseParentHandlers();
		List<String> messages = new ArrayList<>();
		Handler handler = new Handler() {
			@Override
			public void publish(LogRecord record) {
				messages.add(record.getMessage());
			}

			@Override
			public void flush() { }

			@Override
			public void close() { }
		};
		logger.addHandler(handler);
		logger.setLevel(Level.INFO);
		logger.setUseParentHandlers(false);
		try {
			new ApplicationStartupFactory(opts);
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(previousLevel);
			logger.setUseParentHandlers(previousUseParentHandlers);
		}

		assertEquals(List.of("opts:", "first:", "\ta", "\tb", "second:", "\tc"), messages);
	}

	@Test
	void malformedProjectIdOptionIsIgnoredInsteadOfCrashingStartup() {
		HashMap<String, Object> opts = new HashMap<>();
		opts.put("projectId", "abc");
		ApplicationStartupFactory factory = assertDoesNotThrow(() -> new ApplicationStartupFactory(opts));
		assertEquals(0L, factory.projectId);
	}

	@Test
	void validProjectIdOptionIsParsed() {
		HashMap<String, Object> opts = new HashMap<>();
		opts.put("projectId", "42");
		ApplicationStartupFactory factory = new ApplicationStartupFactory(opts);
		assertEquals(42L, factory.projectId);
	}
}
