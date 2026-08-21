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
	private static final Preferences STORE = Preferences.userNodeForPackage(GlobalPreferences.class).node("ui");
	protected transient boolean showAllResources = true;
	private String userName = STORE.get("userName", System.getProperty("user.name", ""));
	private boolean showRowLines = STORE.getBoolean("showRowLines", true);
	private String fontFamily = STORE.get("fontFamily", "");
	private int fontSize = clampFontSize(STORE.getInt("fontSize", 0));

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

	private static int clampFontSize(int value) { return value <= 0 ? 0 : Math.max(8, Math.min(32, value)); }

}
