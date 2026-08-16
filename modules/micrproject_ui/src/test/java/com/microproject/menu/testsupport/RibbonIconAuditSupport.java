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
package com.microproject.menu.testsupport;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import com.microproject.pm.graphic.IconManager;

public final class RibbonIconAuditSupport {
	private RibbonIconAuditSupport() {
	}

	public static Map<String, String> missingIconMappings(RibbonInventory inventory) {
		Map<String, String> missing = new LinkedHashMap<>();
		for (RibbonInventory.ButtonSpec spec : inventory.buttons().values()) {
			if (spec.requiresIcon() && (spec.iconFileName() == null || spec.iconFileName().isBlank())) {
				missing.put(spec.id(), spec.iconKey());
			}
		}
		return missing;
	}

	public static Map<String, URL> missingResources(RibbonInventory inventory) {
		Map<String, URL> missing = new LinkedHashMap<>();
		for (RibbonInventory.ButtonSpec spec : inventory.buttons().values()) {
			if (!spec.requiresIcon()) {
				continue;
			}
			URL resource = IconManager.resolveIconResource(spec.iconKey());
			if (resource == null) {
				missing.put(spec.id(), null);
			}
		}
		return missing;
	}

	public static Map<String, String> missingSvgResources(RibbonInventory inventory) {
		Map<String, String> missing = new LinkedHashMap<>();
		for (RibbonInventory.ButtonSpec spec : inventory.buttons().values()) {
			if (spec.requiresIcon() && !IconManager.hasSvgResource(spec.iconKey())) {
				missing.put(spec.id(), spec.iconKey());
			}
		}
		return missing;
	}
}
