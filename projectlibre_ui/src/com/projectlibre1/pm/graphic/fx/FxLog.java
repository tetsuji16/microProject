package com.projectlibre1.pm.graphic.fx;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared logging helper for JavaFX migration work.
 *
 * Debug logging is enabled with -Dprojectlibre.fx.debug=true.
 */
public final class FxLog {
	private static final boolean DEBUG = Boolean.getBoolean("projectlibre.fx.debug");

	static {
		if (DEBUG) {
			Logger root = Logger.getLogger("");
			root.setLevel(Level.FINE);
			for (Handler handler : root.getHandlers()) {
				handler.setLevel(Level.FINE);
			}
		}
	}

	private FxLog() {
	}

	public static Logger logger(Class<?> type) {
		Logger logger = Logger.getLogger(type.getName());
		logger.setLevel(DEBUG ? Level.FINE : Level.INFO);
		return logger;
	}

	public static boolean isDebugEnabled() {
		return DEBUG;
	}
}
