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
package com.microproject.preference;

import java.util.prefs.Preferences;

import com.microproject.document.ObjectEvent;
import com.microproject.document.ObjectEventManager;

public class GlobalPreferences {
	public static final String GANTT_BAR_TEXT_RESOURCE_NAMES = "Field.resourceNames";
	public static final String GANTT_BAR_TEXT_TASK_NAME = "Field.name";
	public static final String GANTT_BAR_TEXT_POSITION_AUTO = "auto";
	public static final String GANTT_BAR_TEXT_POSITION_RIGHT = "right";
	public static final String GANTT_BAR_TEXT_POSITION_LEFT = "left";
	private static final Preferences STORE = Preferences.userNodeForPackage(GlobalPreferences.class).node("ui");
	protected transient boolean showAllResources = true;
	private String userName = STORE.get("userName", System.getProperty("user.name", ""));
	/** MS Project-compatible default: Gantt gridlines are hidden until enabled. */
	private boolean showRowLines = STORE.getBoolean("showRowLines", false);
	/** Null keeps grid lines synchronized with the active UI theme. */
	private Integer gridLineColor = readColor("gridLineColor");
	/** Null keeps the MS Project-compatible palette default for ordinary task bars. */
	private Integer defaultGanttBarColor = readColor("defaultGanttBarColor");
	private String fontFamily = STORE.get("fontFamily", "");
	private int fontSize = clampFontSize(STORE.getInt("fontSize", 0));
	private String defaultGanttBarText = normalizeGanttBarText(
			STORE.get("defaultGanttBarText", GANTT_BAR_TEXT_RESOURCE_NAMES));
	private String defaultGanttBarTextPosition = normalizeGanttBarTextPosition(
			STORE.get("defaultGanttBarTextPosition", GANTT_BAR_TEXT_POSITION_AUTO));

	public boolean isShowProjectResourcesOnly() {
		return !showAllResources;
	}

	public void setShowProjectResourcesOnly(boolean showProjectResourcesOnly) {
		if (showProjectResourcesOnly == !showAllResources) return;
		this.showAllResources = !showProjectResourcesOnly;
		fireUpdateEvent(this, this);
	}
	
	private transient ObjectEventManager objectEventManager = new ObjectEventManager();
	/**
	 * @param listener
	 */
	public void addObjectListener(ObjectEvent.Listener listener) {
		objectEventManager.addListener(listener);
	}
	/**
	 * @param listener
	 */
	public void removeObjectListener(ObjectEvent.Listener listener) {
		objectEventManager.removeListener(listener);
	}	

	public ObjectEventManager getObjectEventManager() {
		return objectEventManager;
	}

	public void fireUpdateEvent(Object source, Object object) {
		objectEventManager.fireUpdateEvent(source,object);
	}

	public String getUserName() { return userName; }
	public void setUserName(String value) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.equals(userName)) return;
		userName = normalized;
		STORE.put("userName", normalized);
		fireUpdateEvent(this, this);
	}

	public boolean isShowRowLines() { return showRowLines; }
	public void setShowRowLines(boolean value) {
		if (showRowLines == value) return;
		showRowLines = value;
		STORE.putBoolean("showRowLines", value);
		fireUpdateEvent(this, this);
	}

	public Integer getGridLineColor() { return gridLineColor; }
	public void setGridLineColor(Integer value) {
		Integer normalized = value == null ? null : Integer.valueOf(value.intValue() & 0x00ffffff);
		if (java.util.Objects.equals(gridLineColor, normalized)) return;
		gridLineColor = normalized;
		if (normalized == null) STORE.remove("gridLineColor");
		else STORE.putInt("gridLineColor", normalized.intValue());
		fireUpdateEvent(this, this);
	}

	public Integer getDefaultGanttBarColor() { return defaultGanttBarColor; }
	public void setDefaultGanttBarColor(Integer value) {
		Integer normalized = value == null ? null : Integer.valueOf(value.intValue() & 0x00ffffff);
		if (java.util.Objects.equals(defaultGanttBarColor, normalized)) return;
		defaultGanttBarColor = normalized;
		if (normalized == null) STORE.remove("defaultGanttBarColor");
		else STORE.putInt("defaultGanttBarColor", normalized.intValue());
		fireUpdateEvent(this, this);
	}

	/** Empty family/zero size means use the platform theme default. */
	public String getFontFamily() { return fontFamily; }
	public void setFontFamily(String value) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.equals(fontFamily)) return;
		fontFamily = normalized;
		STORE.put("fontFamily", normalized);
		fireUpdateEvent(this, this);
	}

	public int getFontSize() { return fontSize; }
	public void setFontSize(int value) {
		int normalized = clampFontSize(value);
		if (fontSize == normalized) return;
		fontSize = normalized;
		STORE.putInt("fontSize", normalized);
		fireUpdateEvent(this, this);
	}

	/** Default annotation for newly opened Gantt views; saved view settings still take precedence. */
	public String getDefaultGanttBarText() { return defaultGanttBarText; }
	public void setDefaultGanttBarText(String value) {
		String normalized = normalizeGanttBarText(value);
		if (normalized.equals(defaultGanttBarText)) return;
		defaultGanttBarText = normalized;
		STORE.put("defaultGanttBarText", normalized);
		fireUpdateEvent(this, this);
	}

	/** Default label position for newly opened Gantt views. */
	public String getDefaultGanttBarTextPosition() { return defaultGanttBarTextPosition; }
	public void setDefaultGanttBarTextPosition(String value) {
		String normalized = normalizeGanttBarTextPosition(value);
		if (normalized.equals(defaultGanttBarTextPosition)) return;
		defaultGanttBarTextPosition = normalized;
		STORE.put("defaultGanttBarTextPosition", normalized);
		fireUpdateEvent(this, this);
	}

	private static int clampFontSize(int value) { return value <= 0 ? 0 : Math.max(8, Math.min(32, value)); }
	private static String normalizeGanttBarText(String value) {
		return GANTT_BAR_TEXT_TASK_NAME.equals(value) ? GANTT_BAR_TEXT_TASK_NAME : GANTT_BAR_TEXT_RESOURCE_NAMES;
	}
	private static String normalizeGanttBarTextPosition(String value) {
		if (GANTT_BAR_TEXT_POSITION_LEFT.equals(value) || GANTT_BAR_TEXT_POSITION_RIGHT.equals(value))
			return value;
		return GANTT_BAR_TEXT_POSITION_AUTO;
	}
	private static Integer readColor(String key) {
		int value = STORE.getInt(key, -1);
		return value < 0 ? null : Integer.valueOf(value & 0x00ffffff);
	}

	/** Check GitHub Releases at startup for a newer version (#338 plan D). */
	public boolean isCheckForUpdates() { return STORE.getBoolean("checkForUpdates", true); }
	public void setCheckForUpdates(boolean value) {
		if (isCheckForUpdates() == value) return;
		STORE.putBoolean("checkForUpdates", value);
		fireUpdateEvent(this, this);
	}

}
