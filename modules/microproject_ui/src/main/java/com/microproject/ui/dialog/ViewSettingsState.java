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
package com.microproject.ui.dialog;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

/**
 * View flags of an embedded dockable view that are saved with the workspace. The state is a plain
 * text blob so the workspace object stays a trivially serializable value.
 */
public interface ViewSettingsState {
	/** Client property of an embedded panel that carries its {@link ViewSettingsState}. */
	String PROPERTY = "microproject.viewSettingsState";

	/** Client property holding a {@link Runnable} that re-applies restored settings to the view. */
	String REFRESH_PROPERTY = "microproject.viewSettingsRefresh";

	/** The settings as text; empty when they are all defaults. */
	String saveSettings();

	/** Restores settings written by {@link #saveSettings()}; unknown or malformed input keeps the defaults. */
	void restoreSettings(String state);

	/** Helpers shared by the settings classes. */
	final class Codec {
		private Codec() {
		}

		public static String write(Properties properties) {
			if (properties.isEmpty())
				return "";
			StringWriter writer = new StringWriter();
			try {
				properties.store(writer, null);
			} catch (IOException impossible) {
				return "";
			}
			return writer.toString();
		}

		public static Properties read(String state) {
			Properties properties = new Properties();
			if (state == null || state.isBlank())
				return properties;
			try {
				properties.load(new StringReader(state));
			} catch (IOException | IllegalArgumentException malformed) {
				return new Properties();
			}
			return properties;
		}

		public static boolean bool(Properties properties, String key, boolean fallback) {
			String value = properties.getProperty(key);
			return value == null ? fallback : Boolean.parseBoolean(value);
		}

		public static int integer(Properties properties, String key, int fallback) {
			try {
				String value = properties.getProperty(key);
				return value == null ? fallback : Integer.parseInt(value.trim());
			} catch (NumberFormatException malformed) {
				return fallback;
			}
		}

		public static long number(Properties properties, String key, long fallback) {
			try {
				String value = properties.getProperty(key);
				return value == null ? fallback : Long.parseLong(value.trim());
			} catch (NumberFormatException malformed) {
				return fallback;
			}
		}
	}
}
